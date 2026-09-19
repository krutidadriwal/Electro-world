const { requireStaff, StaffAuthError } = require('../../lib/staffAuth');

module.exports = async function handler(req, res) {
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
};
