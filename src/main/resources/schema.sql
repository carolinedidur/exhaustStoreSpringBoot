CREATE TABLE IF NOT EXISTS products (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sku TEXT NOT NULL UNIQUE,
  title TEXT NOT NULL,
  brand TEXT NOT NULL,
  category TEXT NOT NULL,
  description TEXT NOT NULL,
  material TEXT NOT NULL,
  price_cents INTEGER NOT NULL CHECK(price_cents > 0),
  image_path TEXT NOT NULL,
  source_url TEXT NOT NULL DEFAULT '',
  image_credit TEXT NOT NULL DEFAULT '',
  stock_goodwill INTEGER NOT NULL DEFAULT 0 CHECK(stock_goodwill >= 0),
  stock_grmagele INTEGER NOT NULL DEFAULT 0 CHECK(stock_grmagele >= 0),
  published INTEGER NOT NULL DEFAULT 1,
  featured INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS fitments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  make TEXT NOT NULL,
  model TEXT NOT NULL,
  year_from INTEGER NOT NULL,
  year_to INTEGER NOT NULL,
  engine TEXT NOT NULL DEFAULT ''
);
CREATE INDEX IF NOT EXISTS idx_fitment_vehicle ON fitments(make, model, year_from, year_to);
CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL);
