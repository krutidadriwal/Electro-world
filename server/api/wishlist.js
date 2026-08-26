const { getPool } = require('../lib/db');
const { normalizePhone } = require('../lib/phone');

module.exports = async function handler(req, res) {
  if (req.method === 'GET') {
    return handleGet(req, res);
  }
  if (req.method === 'POST') {
    return handlePost(req, res);
  }
  res.setHeader('Allow', 'GET, POST');
  return res.status(405).json({ error: 'Method not allowed' });
};

async function handleGet(req, res) {
  const { phone, countryCode } = req.query;
  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone query param must be a 10-digit number' });
  }

  try {
    const pool = getPool();
    const result = await pool.query(
      `select id, category_icon_key, category_name, subcategory_id, subcategory_name, created_at
       from public.wishlist_items
       where phone = $1
       order by created_at desc`,
      [normalizedPhone]
    );
    return res.status(200).json({ items: result.rows });
  } catch (err) {
    console.error('list wishlist error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}

// Reconciles the wishlist state for a single category: replaces whatever
// rows currently exist for (phone, categoryIconKey) with the desired set
// derived from categoryStarred + subcategoryIds.
//   - subcategoryIds non-empty  -> one row per subcategory (category alone
//     is dropped even if categoryStarred was also true).
//   - subcategoryIds empty and categoryStarred -> a single category-only row.
//   - neither -> nothing (clears any prior entries for this category).
async function handlePost(req, res) {
  const { phone, countryCode, categoryIconKey, categoryStarred, subcategoryIds } = req.body ?? {};

  const normalizedPhone = normalizePhone(phone, countryCode);
  if (!normalizedPhone) {
    return res.status(400).json({ error: 'phone must be a 10-digit number' });
  }
  if (typeof categoryIconKey !== 'string' || categoryIconKey.trim().length === 0) {
    return res.status(400).json({ error: 'categoryIconKey is required' });
  }
  const requestedSubcategoryIds = Array.isArray(subcategoryIds)
    ? subcategoryIds.filter((id) => typeof id === 'string' && id.trim().length > 0)
    : [];

  const pool = getPool();
  try {
    const userResult = await pool.query(`select 1 from public.users where phone = $1`, [normalizedPhone]);
    if (userResult.rowCount === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const categoryResult = await pool.query(
      `select icon_key, name from public.categories where icon_key = $1`,
      [categoryIconKey]
    );
    if (categoryResult.rowCount === 0) {
      return res.status(400).json({ error: 'categoryIconKey does not match a known category' });
    }
    const category = categoryResult.rows[0];

    let subcategories = [];
    if (requestedSubcategoryIds.length > 0) {
      const subcategoryResult = await pool.query(
        `select id, name from public.subcategories where category_icon_key = $1 and id = any($2::uuid[])`,
        [categoryIconKey, requestedSubcategoryIds]
      );
      if (subcategoryResult.rowCount !== requestedSubcategoryIds.length) {
        return res.status(400).json({ error: 'One or more subcategoryIds do not belong to categoryIconKey' });
      }
      subcategories = subcategoryResult.rows;
    }

    await pool.query('begin');
    try {
      await pool.query(
        `delete from public.wishlist_items where phone = $1 and category_icon_key = $2`,
        [normalizedPhone, categoryIconKey]
      );

      if (subcategories.length > 0) {
        for (const sub of subcategories) {
          await pool.query(
            `insert into public.wishlist_items (phone, category_icon_key, category_name, subcategory_id, subcategory_name)
             values ($1, $2, $3, $4, $5)`,
            [normalizedPhone, category.icon_key, category.name, sub.id, sub.name]
          );
        }
      } else if (categoryStarred === true) {
        await pool.query(
          `insert into public.wishlist_items (phone, category_icon_key, category_name)
           values ($1, $2, $3)`,
          [normalizedPhone, category.icon_key, category.name]
        );
      }

      await pool.query('commit');
    } catch (err) {
      await pool.query('rollback');
      throw err;
    }

    const result = await pool.query(
      `select id, category_icon_key, category_name, subcategory_id, subcategory_name, created_at
       from public.wishlist_items
       where phone = $1 and category_icon_key = $2
       order by created_at desc`,
      [normalizedPhone, categoryIconKey]
    );
    return res.status(200).json({ items: result.rows });
  } catch (err) {
    console.error('confirm wishlist error', err);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
