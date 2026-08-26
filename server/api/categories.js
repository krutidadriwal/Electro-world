const { getPool } = require('../lib/db');

module.exports = async function handler(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      `select c.icon_key, c.name as category_name,
              s.id as subcategory_id, s.name as subcategory_name
       from public.categories c
       left join public.subcategories s on s.category_icon_key = c.icon_key
       order by c.sort_order, c.name, s.sort_order, s.name`
    );

    const categoriesByIconKey = new Map();
    for (const row of result.rows) {
      let category = categoriesByIconKey.get(row.icon_key);
      if (!category) {
        category = {
          iconKey: row.icon_key,
          name: row.category_name,
          subcategories: []
        };
        categoriesByIconKey.set(row.icon_key, category);
      }
      if (row.subcategory_id) {
        category.subcategories.push({ id: row.subcategory_id, name: row.subcategory_name });
      }
    }

    return res.status(200).json({ categories: Array.from(categoriesByIconKey.values()) });
  } catch (err) {
    console.error('list categories error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
};
