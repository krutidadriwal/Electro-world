// Combined into one function for the same reason as api/auth/[...path].js --
// staying under Vercel Hobby's 12-Serverless-Function cap. External URLs are
// unchanged (/api/staff/me, /api/staff/list, etc).

const { getPool } = require('../../lib/db');
const { getSupabaseAdmin } = require('../../lib/supabaseAdmin');
const { requireStaff, requireAdmin, StaffAuthError } = require('../../lib/staffAuth');
const { routeSegmentsAfter } = require('../../lib/routeSegments');
const { syncPriceList } = require('../../lib/priceList');
const { normalizePhone } = require('../../lib/phone');
const { sendToTokens } = require('../../lib/fcm');

const VALID_ROLES = ['admin', 'employee'];
const VALID_WISHLIST_STATUSES = [
  'not_contacted',
  'unreachable',
  'invalid_number',
  'call_back_later',
  'sent_details_whatsapp',
  'store_visit_scheduled',
  'interested_comparing_prices',
  'interested_waiting_emi',
  'interested_waiting_stock',
  'interested_waiting_offer',
  'purchased',
  'pending_delivery',
  'bought_elsewhere',
  'dropped_plan',
  'not_interested'
];
const VALID_SERVICE_REQUEST_STATUSES = ['open', 'in_progress', 'resolved', 'closed'];

module.exports = async function handler(req, res) {
  // .../api/staff/<...> -- drop "api", "staff".
  const route = routeSegmentsAfter(req, 2).join('/');

  switch (route) {
    case 'me':
      return me(req, res);
    case 'list':
      return list(req, res);
    case 'create':
      return create(req, res);
    case 'role':
      return role(req, res);
    // Single hyphenated segments, not nested ("price-list/list") -- Vercel's
    // [...path].js catch-all only ever routes a *1-segment* remainder to
    // this function in production (confirmed by testing against the
    // deployed server: a 2+-segment path under /api/staff/ never reaches
    // this handler at all, returning Vercel's own platform 404 instead of
    // ours). Every other route above is single-segment for the same reason.
    case 'price-list-list':
      return priceListList(req, res);
    case 'price-list-sync':
      return priceListSync(req, res);
    case 'wishlist-list':
      return wishlistList(req, res);
    case 'wishlist-add':
      return wishlistAdd(req, res);
    case 'wishlist-status':
      return wishlistStatus(req, res);
    case 'wishlist-call':
      return wishlistCall(req, res);
    case 'complaints-list':
      return complaintsList(req, res);
    case 'complaints-status':
      return complaintsStatus(req, res);
    case 'installations-list':
      return installationsList(req, res);
    case 'installations-status':
      return installationsStatus(req, res);
    case 'invoice-requests-list':
      return invoiceRequestsList(req, res);
    case 'invoice-requests-status':
      return invoiceRequestsStatus(req, res);
    case 'device-token':
      return staffDeviceToken(req, res);
    default:
      return res.status(404).json({ error: 'Not found' });
  }
};

async function me(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const staff = await requireStaff(req);
    return res.status(200).json({ id: staff.id, email: staff.email, role: staff.role });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('get staff me error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function list(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    await requireAdmin(req);

    const pool = getPool();
    const result = await pool.query(
      `select id, email, role, created_at from public.staff_profiles order by created_at desc`
    );
    return res.status(200).json({ staff: result.rows });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('list staff error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function create(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { email, password, role: newRole } = req.body ?? {};
  if (typeof email !== 'string' || !email.includes('@')) {
    return res.status(400).json({ error: 'A valid email is required' });
  }
  if (typeof password !== 'string' || password.length < 8) {
    return res.status(400).json({ error: 'password must be at least 8 characters' });
  }
  if (!VALID_ROLES.includes(newRole)) {
    return res.status(400).json({ error: `role must be one of: ${VALID_ROLES.join(', ')}` });
  }

  try {
    const admin = await requireAdmin(req);

    const supabase = getSupabaseAdmin();
    const { data, error } = await supabase.auth.admin.createUser({
      email: email.trim(),
      password,
      email_confirm: true
    });
    if (error) {
      return res.status(400).json({ error: error.message });
    }

    const pool = getPool();
    const result = await pool.query(
      `insert into public.staff_profiles (id, email, role, invited_by)
       values ($1, $2, $3, $4)
       returning id, email, role, created_at`,
      [data.user.id, email.trim(), newRole, admin.id]
    );
    return res.status(201).json(result.rows[0]);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('create staff error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function role(req, res) {
  if (req.method !== 'PATCH') {
    res.setHeader('Allow', 'PATCH');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { staffId, role: newRole } = req.body ?? {};
  if (typeof staffId !== 'string' || staffId.trim().length === 0) {
    return res.status(400).json({ error: 'staffId is required' });
  }
  if (!VALID_ROLES.includes(newRole)) {
    return res.status(400).json({ error: `role must be one of: ${VALID_ROLES.join(', ')}` });
  }

  try {
    const admin = await requireAdmin(req);
    if (staffId === admin.id) {
      return res.status(400).json({ error: 'You cannot change your own role' });
    }

    const pool = getPool();
    const result = await pool.query(
      `update public.staff_profiles set role = $1 where id = $2
       returning id, email, role, created_at`,
      [newRole, staffId]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Staff member not found' });
    }
    return res.status(200).json(result.rows[0]);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('update staff role error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Any signed-in staff member (admin or employee) can browse the price list --
// it's read-only reference data, not a role-restricted action.
async function priceListList(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const staff = await requireStaff(req);

    const pool = getPool();
    // cost_price is admin-only -- employees browsing the price list shouldn't
    // see what items cost the business.
    const columns = staff.role === 'admin'
      ? 'category, group_name, item_name, final_price, mrp, cost_price, stock_label'
      : 'category, group_name, item_name, final_price, mrp, stock_label';
    const result = await pool.query(
      `select ${columns}
       from public.price_list_items
       order by category, group_name, item_name`
    );
    return res.status(200).json({ items: result.rows });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('list price list error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function priceListSync(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    await requireAdmin(req);

    const result = await syncPriceList();
    return res.status(200).json(result);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('sync price list error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Any signed-in staff member can browse the wishlist -- calling customers
// about it is a sales-floor task, not an admin-only one.
async function wishlistList(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    await requireStaff(req);

    const pool = getPool();
    const result = await pool.query(
      `select
         wi.id,
         wi.phone as customer_phone,
         u.name as customer_name,
         wi.category_name,
         wi.subcategory_name,
         wi.status,
         wi.created_at,
         wi.status_updated_at,
         (
           select wcl.called_at from public.wishlist_call_logs wcl
           where wcl.wishlist_item_id = wi.id
           order by wcl.called_at desc
           limit 1
         ) as last_called_at
       from public.wishlist_items wi
       join public.users u on u.phone = wi.phone
       order by wi.created_at desc`
    );
    return res.status(200).json({ items: result.rows });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('list wishlist error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Lets a salesperson log a walk-in customer's interest directly, without the
// customer needing the EW app (or even an account) yet. If the phone isn't
// in public.users at all, a bare placeholder row is created for it (name
// only, no PIN) -- wishlist_items.phone has a not-null FK to users, so this
// is required either way. That placeholder is exactly what /api/auth's
// "forgot PIN" flow already expects for a phone with no PIN set yet, so once
// this customer verifies that phone number in the app, they can set a PIN
// on this same row and immediately see the wishlist entry staff added.
async function wishlistAdd(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { phone, countryCode, customerName, categoryIconKey, subcategoryId } = req.body ?? {};
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof categoryIconKey !== 'string' || categoryIconKey.trim().length === 0) {
    return res.status(400).json({ error: 'categoryIconKey is required' });
  }
  const trimmedName = typeof customerName === 'string' ? customerName.trim() : '';

  try {
    await requireStaff(req);

    const pool = getPool();

    const categoryResult = await pool.query(
      `select icon_key, name from public.categories where icon_key = $1`,
      [categoryIconKey]
    );
    if (categoryResult.rowCount === 0) {
      return res.status(400).json({ error: 'categoryIconKey does not match a known category' });
    }
    const category = categoryResult.rows[0];

    let subcategory = null;
    if (typeof subcategoryId === 'string' && subcategoryId.trim().length > 0) {
      const subcategoryResult = await pool.query(
        `select id, name from public.subcategories where id = $1 and category_icon_key = $2`,
        [subcategoryId, categoryIconKey]
      );
      if (subcategoryResult.rowCount === 0) {
        return res.status(400).json({ error: 'subcategoryId does not belong to categoryIconKey' });
      }
      subcategory = subcategoryResult.rows[0];
    }

    const existingUser = await pool.query('select phone from public.users where phone = $1', [normalizedPhone]);
    if (existingUser.rowCount === 0) {
      if (!trimmedName) {
        return res.status(400).json({ error: 'customerName is required for a phone with no existing account' });
      }
      await pool.query(
        `insert into public.users (phone, name) values ($1, $2)`,
        [normalizedPhone, trimmedName]
      );
    }

    const insertResult = subcategory
      ? await pool.query(
          `insert into public.wishlist_items (phone, category_icon_key, category_name, subcategory_id, subcategory_name)
           values ($1, $2, $3, $4, $5)
           on conflict (phone, category_icon_key, subcategory_id) where subcategory_id is not null do nothing
           returning id`,
          [normalizedPhone, category.icon_key, category.name, subcategory.id, subcategory.name]
        )
      : await pool.query(
          `insert into public.wishlist_items (phone, category_icon_key, category_name)
           values ($1, $2, $3)
           on conflict (phone, category_icon_key) where subcategory_id is null do nothing
           returning id`,
          [normalizedPhone, category.icon_key, category.name]
        );

    return res.status(200).json({ added: insertResult.rowCount > 0 });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('add wishlist error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function wishlistStatus(req, res) {
  if (req.method !== 'PATCH') {
    res.setHeader('Allow', 'PATCH');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { wishlistItemId, status } = req.body ?? {};
  if (typeof wishlistItemId !== 'string' || wishlistItemId.trim().length === 0) {
    return res.status(400).json({ error: 'wishlistItemId is required' });
  }
  if (!VALID_WISHLIST_STATUSES.includes(status)) {
    return res.status(400).json({ error: `status must be one of: ${VALID_WISHLIST_STATUSES.join(', ')}` });
  }

  try {
    const staff = await requireStaff(req);

    const pool = getPool();

    // A purchase ends the wishlist entry's reason for existing -- rather than
    // leave a "purchased" row behind, it's deleted outright, both as a
    // signal to staff (gone from the list = done) and to the customer (this
    // table is also what their own app's wishlist reads from).
    if (status === 'purchased') {
      const result = await pool.query(
        `delete from public.wishlist_items where id = $1 returning id`,
        [wishlistItemId]
      );
      if (result.rowCount === 0) {
        return res.status(404).json({ error: 'Wishlist item not found' });
      }
      return res.status(200).json({ id: result.rows[0].id, status: 'purchased', deleted: true });
    }

    const result = await pool.query(
      `update public.wishlist_items
       set status = $1, status_updated_at = now(), status_updated_by = $2
       where id = $3
       returning id, status, status_updated_at`,
      [status, staff.id, wishlistItemId]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Wishlist item not found' });
    }
    return res.status(200).json(result.rows[0]);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('update wishlist status error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Logs a tap of the Call button. The customer's phone/name are looked up
// server-side from the wishlist item rather than trusted from the request,
// so the log can't be spoofed with an arbitrary number/name pair.
async function wishlistCall(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { wishlistItemId } = req.body ?? {};
  if (typeof wishlistItemId !== 'string' || wishlistItemId.trim().length === 0) {
    return res.status(400).json({ error: 'wishlistItemId is required' });
  }

  try {
    const staff = await requireStaff(req);

    const pool = getPool();
    const itemResult = await pool.query(
      `select wi.phone as customer_phone, u.name as customer_name
       from public.wishlist_items wi
       join public.users u on u.phone = wi.phone
       where wi.id = $1`,
      [wishlistItemId]
    );
    if (itemResult.rowCount === 0) {
      return res.status(404).json({ error: 'Wishlist item not found' });
    }
    const { customer_phone: customerPhone, customer_name: customerName } = itemResult.rows[0];

    const logResult = await pool.query(
      `insert into public.wishlist_call_logs
         (wishlist_item_id, staff_id, staff_email, customer_phone, customer_name)
       values ($1, $2, $3, $4, $5)
       returning id, called_at`,
      [wishlistItemId, staff.id, staff.email, customerPhone, customerName]
    );
    return res.status(201).json(logResult.rows[0]);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('log wishlist call error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Any signed-in staff member can review/triage complaints -- not an
// admin-only action, same reasoning as wishlistList above.
async function complaintsList(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    await requireStaff(req);

    const pool = getPool();
    const result = await pool.query(
      `select
         c.id,
         c.phone as customer_phone,
         u.name as customer_name,
         c.category_name,
         c.subcategory_name,
         c.description,
         c.address,
         c.contact_phone,
         c.invoice_file_name,
         c.status,
         c.created_at,
         c.updated_at,
         c.resolved_at
       from public.complaints c
       join public.users u on u.phone = c.phone
       order by c.created_at desc`
    );
    return res.status(200).json({ items: result.rows });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('list complaints error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function complaintsStatus(req, res) {
  if (req.method !== 'PATCH') {
    res.setHeader('Allow', 'PATCH');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { complaintId, status } = req.body ?? {};
  if (typeof complaintId !== 'string' || complaintId.trim().length === 0) {
    return res.status(400).json({ error: 'complaintId is required' });
  }
  if (!VALID_SERVICE_REQUEST_STATUSES.includes(status)) {
    return res.status(400).json({ error: `status must be one of: ${VALID_SERVICE_REQUEST_STATUSES.join(', ')}` });
  }

  try {
    await requireStaff(req);

    const pool = getPool();
    const resolved = status === 'resolved' || status === 'closed';
    const result = await pool.query(
      `update public.complaints
       set status = $1, updated_at = now(), resolved_at = case when $2 then now() else null end
       where id = $3
       returning id, status, updated_at, resolved_at`,
      [status, resolved, complaintId]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Complaint not found' });
    }
    return res.status(200).json(result.rows[0]);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('update complaint status error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Any signed-in staff member can review/triage installation & demo
// requests -- not an admin-only action, same reasoning as wishlistList above.
async function installationsList(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    await requireStaff(req);

    const pool = getPool();
    const result = await pool.query(
      `select
         i.id,
         i.phone as customer_phone,
         u.name as customer_name,
         i.category_name,
         i.item_name,
         i.wants_demo,
         i.wants_installation,
         i.address,
         i.contact_phone,
         i.invoice_file_name,
         i.status,
         i.created_at,
         i.updated_at,
         i.resolved_at
       from public.installations i
       join public.users u on u.phone = i.phone
       order by i.created_at desc`
    );
    return res.status(200).json({ items: result.rows });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('list installations error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

async function installationsStatus(req, res) {
  if (req.method !== 'PATCH') {
    res.setHeader('Allow', 'PATCH');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { installationId, status } = req.body ?? {};
  if (typeof installationId !== 'string' || installationId.trim().length === 0) {
    return res.status(400).json({ error: 'installationId is required' });
  }
  if (!VALID_SERVICE_REQUEST_STATUSES.includes(status)) {
    return res.status(400).json({ error: `status must be one of: ${VALID_SERVICE_REQUEST_STATUSES.join(', ')}` });
  }

  try {
    await requireStaff(req);

    const pool = getPool();
    const resolved = status === 'resolved' || status === 'closed';
    const result = await pool.query(
      `update public.installations
       set status = $1, updated_at = now(), resolved_at = case when $2 then now() else null end
       where id = $3
       returning id, status, updated_at, resolved_at`,
      [status, resolved, installationId]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Installation request not found' });
    }
    return res.status(200).json(result.rows[0]);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('update installation status error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Admin-only, unlike complaintsList/installationsList/wishlistList -- a
// request to re-send an invoice touches billing/accounts territory, not
// general sales-floor triage.
async function invoiceRequestsList(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    await requireAdmin(req);

    const pool = getPool();
    const result = await pool.query(
      `select
         ir.id,
         ir.phone as customer_phone,
         u.name as customer_name,
         ir.description,
         ir.status,
         ir.created_at,
         ir.updated_at,
         ir.resolved_at
       from public.invoice_requests ir
       join public.users u on u.phone = ir.phone
       order by ir.created_at desc`
    );
    return res.status(200).json({ items: result.rows });
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('list invoice requests error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Admin-only, like invoiceRequestsList. "Closed" isn't a status that sticks
// around to be filtered on later -- it means the request is done, so the row
// is deleted outright (same pattern as wishlist's "purchased"), and the
// customer who asked for it is pushed a heads-up that their invoice is
// ready, via the CUSTOMER app's FCM project (lib/fcm.js), not the staff
// app's (lib/staffFcm.js) -- this notification is for them, not staff.
// "Open" is the only other option exposed in the staff app's dropdown, which
// just reverts status/resolved_at without deleting anything.
async function invoiceRequestsStatus(req, res) {
  if (req.method !== 'PATCH') {
    res.setHeader('Allow', 'PATCH');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { invoiceRequestId, status } = req.body ?? {};
  if (typeof invoiceRequestId !== 'string' || invoiceRequestId.trim().length === 0) {
    return res.status(400).json({ error: 'invoiceRequestId is required' });
  }
  if (status !== 'open' && status !== 'closed') {
    return res.status(400).json({ error: 'status must be one of: open, closed' });
  }

  try {
    await requireAdmin(req);

    const pool = getPool();

    if (status === 'closed') {
      const result = await pool.query(
        `delete from public.invoice_requests where id = $1 returning phone`,
        [invoiceRequestId]
      );
      if (result.rowCount === 0) {
        return res.status(404).json({ error: 'Invoice request not found' });
      }
      const { phone } = result.rows[0];

      try {
        const tokensResult = await pool.query('select fcm_token from public.device_tokens where phone = $1', [phone]);
        const tokens = tokensResult.rows.map((row) => row.fcm_token);
        if (tokens.length > 0) {
          const { deadTokens } = await sendToTokens(tokens, {
            title: 'Electro World',
            body: 'Your requested invoice has been uploaded. Check the app to view/download it.'
          });
          if (deadTokens.length > 0) {
            await pool.query('delete from public.device_tokens where fcm_token = any($1)', [deadTokens]);
          }
        }
      } catch (pushErr) {
        // The request is already deleted -- a push failure shouldn't fail
        // the whole action, just get logged for follow-up.
        console.error('invoice ready push error', pushErr);
      }

      return res.status(200).json({ id: invoiceRequestId, status: 'closed', deleted: true });
    }

    const result = await pool.query(
      `update public.invoice_requests
       set status = 'open', updated_at = now(), resolved_at = null
       where id = $1
       returning id, status, updated_at, resolved_at`,
      [invoiceRequestId]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Invoice request not found' });
    }
    return res.status(200).json(result.rows[0]);
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('update invoice request status error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Registers/refreshes this device's FCM token for the staff app's own alert
// channel (new wishlist/complaint/installation/invoice-request pushes) --
// separate from the customer app's public.device_tokens.
async function staffDeviceToken(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { fcmToken } = req.body ?? {};
  if (typeof fcmToken !== 'string' || fcmToken.trim().length === 0) {
    return res.status(400).json({ error: 'fcmToken is required' });
  }

  try {
    const staff = await requireStaff(req);

    const pool = getPool();
    await pool.query(
      `insert into public.staff_device_tokens (staff_id, fcm_token)
       values ($1, $2)
       on conflict (fcm_token) do update set staff_id = excluded.staff_id, updated_at = now()`,
      [staff.id, fcmToken.trim()]
    );
    return res.status(204).end();
  } catch (err) {
    if (err instanceof StaffAuthError) {
      return res.status(err.status).json({ error: err.message });
    }
    console.error('register staff device token error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
