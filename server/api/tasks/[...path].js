// EXPERIMENTAL -- Task Tracker. Admin-only internal board for posting and
// tracking development work (owner posts what's needed, developer moves it
// through statuses). Intended to grow into a task tracker for all employees,
// or to be deleted outright.
//
// Gated by the TASK_TRACKER_MODE env var: unless it's exactly "true", every
// route here answers 404 as if the feature didn't exist. The staff app has
// its own matching TASK_TRACKER_MODE build flag (see TaskTrackerScreen.kt).
//
// Kept fully self-contained so removing the feature is just:
//   1. delete this file, server/test/taskTracker.test.js, and the
//      '/api/tasks' line in scripts/dev-server.js
//   2. delete the TASK TRACKER block at the end of schema.sql and run
//      `drop table if exists public.tracker_tasks;`
//   3. the staff app's removal steps, listed in TaskTrackerScreen.kt
//
// Lives in its own catch-all function rather than inside
// api/staff/[...path].js (counts toward Vercel Hobby's 12-function cap --
// 8 of 12 with this one). Routes are single-segment for the same reason as
// there: /api/tasks/list, /api/tasks/staff, /api/tasks/create,
// /api/tasks/update, /api/tasks/delete.

const { getPool } = require('../../lib/db');
const { requireAdmin, StaffAuthError } = require('../../lib/staffAuth');
const { routeSegmentsAfter } = require('../../lib/routeSegments');

const STATUSES = ['todo', 'in_progress', 'testing', 'done', 'wont_do'];
const PRIORITIES = ['low', 'normal', 'high', 'urgent'];
const MAX_TITLE_LENGTH = 200;
const MAX_DESCRIPTION_LENGTH = 5000;
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const TASK_COLUMNS = `
  t.id, t.title, t.description, t.status, t.priority,
  t.assigned_to, a.email as assigned_to_email,
  t.created_by, c.email as created_by_email,
  t.updated_by, u.email as updated_by_email,
  t.created_at, t.updated_at, t.completed_at`;

const TASK_JOINS = `
  left join public.staff_profiles a on a.id = t.assigned_to
  left join public.staff_profiles c on c.id = t.created_by
  left join public.staff_profiles u on u.id = t.updated_by`;

function isTaskTrackerEnabled() {
  return process.env.TASK_TRACKER_MODE === 'true';
}

module.exports = async function handler(req, res) {
  if (!isTaskTrackerEnabled()) {
    return res.status(404).json({ error: 'Not found' });
  }

  // .../api/tasks/<...> -- drop "api", "tasks".
  const route = routeSegmentsAfter(req, 2).join('/');

  switch (route) {
    case 'list':
      return withAdmin(req, res, 'GET', listTasks);
    case 'staff':
      return withAdmin(req, res, 'GET', listAssignees);
    case 'create':
      return withAdmin(req, res, 'POST', createTask);
    case 'update':
      return withAdmin(req, res, 'PATCH', updateTask);
    case 'delete':
      return withAdmin(req, res, 'DELETE', deleteTask);
    default:
      return res.status(404).json({ error: 'Not found' });
  }
};

// Shared method check + input validation + admin auth + error mapping, so
// each route below only holds its own logic. `validate` runs before auth
// so bad input gets a 400 without a token/DB round trip.
async function withAdmin(req, res, method, route) {
  if (req.method !== method) {
    res.setHeader('Allow', method);
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const validationError = route.validate?.(req);
  if (validationError) {
    return res.status(400).json({ error: validationError });
  }

  try {
    const admin = await requireAdmin(req);
    return await route(req, res, admin);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    // foreign_key_violation -- the only FK a caller controls is assigned_to.
    if (err.code === '23503') {
      return res.status(400).json({ error: 'assignedTo is not a staff member' });
    }
    console.error(`task tracker ${req.method} ${req.url} error`, err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function listTasks(req, res) {
  const pool = getPool();
  const tasks = await pool.query(
    `select ${TASK_COLUMNS} from public.tracker_tasks t ${TASK_JOINS} order by t.created_at desc`
  );
  return res.status(200).json({ items: tasks.rows });
}

// Who a task can be assigned to. Its own route (not folded into list) so the
// picker still fills even if loading tasks fails. Admin-only for now; drop
// the role filter when the tracker opens up to all employees.
async function listAssignees(req, res) {
  const pool = getPool();
  const staff = await pool.query(
    `select id, email, role from public.staff_profiles where role = 'admin' order by email`
  );
  return res.status(200).json({ staff: staff.rows });
}

async function createTask(req, res, admin) {
  const { title, description, priority, assignedTo } = req.body ?? {};
  const pool = getPool();
  const inserted = await pool.query(
    `insert into public.tracker_tasks (title, description, priority, assigned_to, created_by, updated_by)
     values ($1, $2, $3, $4, $5, $5)
     returning id`,
    [title.trim(), (description ?? '').trim(), priority ?? 'normal', assignedTo || null, admin.id]
  );
  return res.status(201).json(await fetchTask(pool, inserted.rows[0].id));
}
createTask.validate = (req) => {
  const body = req.body ?? {};
  return validateTitle(body.title, { required: true }) ?? validateCommonFields(body);
};

// Partial update -- only the fields present in the body are changed.
// assignedTo: null or "" unassigns ("" because the app's Moshi client drops
// null fields rather than sending them).
async function updateTask(req, res, admin) {
  const body = req.body ?? {};
  const sets = [];
  const values = [];
  const add = (sql, value) => {
    values.push(value);
    sets.push(sql.replace('?', `$${values.length}`));
  };

  if (body.title !== undefined) add('title = ?', body.title.trim());
  if (body.description !== undefined) add('description = ?', body.description.trim());
  if (body.priority !== undefined) add('priority = ?', body.priority);
  if (body.assignedTo !== undefined) add('assigned_to = ?', body.assignedTo || null);
  if (body.status !== undefined) {
    add('status = ?', body.status);
    // completed_at tracks when it first reached done; leaving done clears it.
    sets.push(`completed_at = case when $${values.length} = 'done' then coalesce(completed_at, now()) else null end`);
  }
  add('updated_by = ?', admin.id);
  sets.push('updated_at = now()');
  values.push(body.id);

  const pool = getPool();
  const updated = await pool.query(
    `update public.tracker_tasks set ${sets.join(', ')} where id = $${values.length} returning id`,
    values
  );
  if (updated.rowCount === 0) {
    return res.status(404).json({ error: 'Task not found' });
  }
  return res.status(200).json(await fetchTask(pool, body.id));
}
updateTask.validate = (req) => {
  const body = req.body ?? {};
  if (typeof body.id !== 'string' || !UUID_RE.test(body.id)) {
    return 'id is required';
  }
  const fields = ['title', 'description', 'status', 'priority', 'assignedTo'];
  if (!fields.some((f) => body[f] !== undefined)) {
    return `Nothing to update -- send at least one of: ${fields.join(', ')}`;
  }
  if (body.status !== undefined && !STATUSES.includes(body.status)) {
    return `status must be one of: ${STATUSES.join(', ')}`;
  }
  return validateTitle(body.title, { required: false }) ?? validateCommonFields(body);
};

async function deleteTask(req, res) {
  const pool = getPool();
  const deleted = await pool.query('delete from public.tracker_tasks where id = $1', [req.query.id]);
  if (deleted.rowCount === 0) {
    return res.status(404).json({ error: 'Task not found' });
  }
  return res.status(200).json({ id: req.query.id, deleted: true });
}
deleteTask.validate = (req) => {
  const id = req.query?.id;
  return typeof id === 'string' && UUID_RE.test(id) ? null : 'id query parameter is required';
};

function validateTitle(title, { required }) {
  if (title === undefined && !required) return null;
  if (typeof title !== 'string' || !title.trim()) return 'title is required';
  if (title.trim().length > MAX_TITLE_LENGTH) return `title must be at most ${MAX_TITLE_LENGTH} characters`;
  return null;
}

function validateCommonFields({ description, priority, assignedTo }) {
  if (description !== undefined) {
    if (typeof description !== 'string') return 'description must be a string';
    if (description.length > MAX_DESCRIPTION_LENGTH) {
      return `description must be at most ${MAX_DESCRIPTION_LENGTH} characters`;
    }
  }
  if (priority !== undefined && !PRIORITIES.includes(priority)) {
    return `priority must be one of: ${PRIORITIES.join(', ')}`;
  }
  if (assignedTo !== undefined && assignedTo !== null && assignedTo !== '' && (typeof assignedTo !== 'string' || !UUID_RE.test(assignedTo))) {
    return 'assignedTo must be a staff id, or null/"" to unassign';
  }
  return null;
}

async function fetchTask(pool, id) {
  const result = await pool.query(`select ${TASK_COLUMNS} from public.tracker_tasks t ${TASK_JOINS} where t.id = $1`, [id]);
  return result.rows[0];
}
