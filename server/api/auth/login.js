const { getPool } = require('../../lib/db');
const { normalizePhone } = require('../../lib/phone');
const { signSession } = require('../../lib/jwt');
const bcrypt = require('bcryptjs');

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { phone, countryCode, pin } = req.body ?? {};
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof pin !== 'string' || pin.length === 0) {
    return res.status(400).json({ error: 'pin is required' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      'select phone, name, pin_hash from public.users where phone = $1',
      [normalizedPhone]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'No account found with this number' });
    }

    const user = result.rows[0];
    if (!user.pin_hash) {
      return res.status(400).json({ error: 'This account has not set up a PIN yet' });
    }

    const pinMatches = await bcrypt.compare(pin, user.pin_hash);
    if (!pinMatches) {
      return res.status(401).json({ error: 'Incorrect PIN' });
    }

    await pool.query('update public.users set last_login_at = now() where phone = $1', [normalizedPhone]);

    return res.status(200).json({ token: signSession(user.phone), phone: user.phone, name: user.name });
  } catch (err) {
    console.error('login error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
};
