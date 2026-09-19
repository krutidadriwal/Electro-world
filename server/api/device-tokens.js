const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { phone, countryCode, fcmToken } = req.body ?? {};
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof fcmToken !== 'string' || fcmToken.trim().length === 0) {
    return res.status(400).json({ error: 'fcmToken is required' });
  }

  try {
    const pool = getPool();
    const userResult = await pool.query('select 1 from public.users where phone = $1', [normalizedPhone]);
    if (userResult.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    await pool.query(
      `insert into public.device_tokens (phone, fcm_token)
       values ($1, $2)
       on conflict (fcm_token) do update set phone = excluded.phone, updated_at = now()`,
      [normalizedPhone, fcmToken.trim()]
    );
    return res.status(204).end();
  } catch (err) {
    console.error('register device token error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
};
