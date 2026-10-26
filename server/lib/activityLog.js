// Staff activity log -- one plain-language row per thing a staff member did
// ("changed Washing Machine complaint to Closed on 2026-10-26"), shown to
// admins on the staff app's Activity Log tab. Written from each staff
// endpoint right after its change succeeds; see staff_activity_log in
// schema.sql and scripts/backfill-activity-log.js for older history.

const { getPool } = require('./db');

// The Activity Log tab's "Work" filter. Keep in sync with WORK_TYPES in the
// staff app's ActivityLogScreen.kt and the check constraint in schema.sql.
const WORK_TYPES = [
  'wishlist',
  'complaint',
  'installation',
  'invoice',
  'price_share',
  'spin',
  'broadcast',
  'staff',
  'price_list'
];

// Same wording the staff app shows in its status dropdowns.
const WISHLIST_STATUS_LABELS = {
  not_contacted: 'Not Contacted',
  unreachable: 'Unreachable (No answer/Switched off)',
  invalid_number: 'Invalid/Wrong Number',
  call_back_later: 'Call Back Later',
  sent_details_whatsapp: 'Sent Details (WhatsApp)',
  store_visit_scheduled: 'Store Visit Scheduled',
  interested_comparing_prices: 'Interested - Comparing Prices',
  interested_waiting_emi: 'Interested - Waiting for EMI/Finance',
  interested_waiting_stock: 'Interested - Waiting for Stock',
  interested_waiting_offer: 'Interested - Waiting for Sale/Offer',
  purchased: 'Purchased (Deal Closed)',
  pending_delivery: 'Pending Delivery/Installation',
  bought_elsewhere: 'Bought Elsewhere (Competitor/Online)',
  dropped_plan: 'Dropped Plan (Postponed/Cancelled)',
  not_interested: 'Not Interested'
};

const SERVICE_REQUEST_STATUS_LABELS = {
  open: 'Open',
  assigned_to_service_center: 'Assigned to Service Center',
  assigned_to_technician: 'Assigned to Technician',
  blocked: 'Blocked',
  cancelled_by_staff: 'Cancelled by Staff',
  cancelled_by_customer: 'Cancelled by Customer',
  closed: 'Closed'
};

// "Assigned to Technician (Suresh, 9876543210)" -- the status plus whatever
// was entered in the popup for it (see statusChangeFieldsError).
function serviceRequestStatusText(status, body = {}) {
  const label = SERVICE_REQUEST_STATUS_LABELS[status] ?? status;
  switch (status) {
    case 'assigned_to_service_center':
      return `${label} (ref ${body.referenceNumber})`;
    case 'assigned_to_technician':
      return `${label} (${body.technicianName}, ${body.technicianMobile})`;
    case 'blocked':
    case 'cancelled_by_staff':
    case 'cancelled_by_customer':
      return `${label}: ${body.reason}`;
    case 'closed':
      return `${label} on ${body.closedDate}`;
    default:
      return label;
  }
}

// "Washing Machine" / "Washing Machine - Front Load".
function categoryText(categoryName, subcategoryName) {
  return subcategoryName ? `${categoryName} - ${subcategoryName}` : categoryName;
}

// 'sent a broadcast: "Diwali sale starts today..."' -- long messages are cut
// short, the full text is on the Broadcast tab.
const BROADCAST_PREVIEW_LENGTH = 80;

function broadcastDescription(message) {
  const preview = message.length > BROADCAST_PREVIEW_LENGTH
    ? `${message.slice(0, BROADCAST_PREVIEW_LENGTH).trimEnd()}...`
    : message;
  return `sent a broadcast: "${preview}"`;
}

// Best-effort: the action has already happened, so a logging failure is
// only reported, never passed on to fail the request. Awaited by callers
// anyway, since Vercel can freeze the function as soon as it responds.
// The customer's name defaults to the one on their account.
async function logActivity(staff, { workType, description, customerPhone = null, customerName = null, at = null }) {
  try {
    await getPool().query(
      `insert into public.staff_activity_log
         (staff_id, staff_name, work_type, description, customer_phone, customer_name, created_at)
       values ($1, $2, $3, $4, $5::text,
               coalesce($6, (select name from public.users where phone = $5::text)),
               coalesce($7::timestamptz, now()))`,
      [staff.id, staff.name || staff.email, workType, description, customerPhone, customerName, at]
    );
  } catch (err) {
    console.error('activity log write error', err);
  }
}

module.exports = {
  WORK_TYPES,
  WISHLIST_STATUS_LABELS,
  serviceRequestStatusText,
  categoryText,
  broadcastDescription,
  logActivity
};
