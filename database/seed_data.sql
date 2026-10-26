-- Initial Seed Roles, Users, and Sample Inventory for POS Application

-- Insert Roles
INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_ADMIN', 'Full Administrative Access'),
(2, 'ROLE_CASHIER', 'Point of Sale Checkout & Cash Register Operations'),
(3, 'ROLE_INVENTORY_MANAGER', 'Product and Stock Management Access')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Insert Users (Default Password for all: password123)
-- BCrypt Hash: $2a$10$r6qqBGFhEChQ.cBuYm/AvuvScnkHFG4nhlqG4vfzcIbqi26mp6nzm
INSERT INTO users (id, username, email, password, full_name, is_active, created_at, updated_at) VALUES
(1, 'admin', 'admin@pos.local', '$2a$10$r6qqBGFhEChQ.cBuYm/AvuvScnkHFG4nhlqG4vfzcIbqi26mp6nzm', 'System Administrator', TRUE, NOW(), NOW()),
(2, 'cashier', 'cashier@pos.local', '$2a$10$r6qqBGFhEChQ.cBuYm/AvuvScnkHFG4nhlqG4vfzcIbqi26mp6nzm', 'Jane Cashier', TRUE, NOW(), NOW()),
(3, 'manager', 'manager@pos.local', '$2a$10$r6qqBGFhEChQ.cBuYm/AvuvScnkHFG4nhlqG4vfzcIbqi26mp6nzm', 'Bob Stock Manager', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- Map User Roles
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), -- admin -> ROLE_ADMIN
(2, 2), -- cashier -> ROLE_CASHIER
(3, 3)  -- manager -> ROLE_INVENTORY_MANAGER
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id);

-- Insert Sample Products
INSERT INTO products (id, sku, barcode, name, category, price, cost_price, is_active, created_at, updated_at) VALUES
(1, 'BEV-COF-001', '8901234567890', 'Espresso Dark Roast 250g', 'Beverages', 12.50, 6.00, TRUE, NOW(), NOW()),
(2, 'BEV-TEA-002', '8901234567891', 'Organic Green Tea 50 Bags', 'Beverages', 8.99, 4.20, TRUE, NOW(), NOW()),
(3, 'SNK-CHIP-003', '8901234567892', 'Artisan Sea Salt Chips', 'Snacks', 3.75, 1.80, TRUE, NOW(), NOW()),
(4, 'SNK-CHOC-004', '8901234567893', 'Belgian Dark Chocolate 100g', 'Snacks', 4.50, 2.10, TRUE, NOW(), NOW()),
(5, 'STA-NOTE-005', '8901234567894', 'Hardcover Journal A5', 'Stationery', 9.99, 4.50, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE sku=VALUES(sku);

-- Insert Initial Inventory
INSERT INTO inventory (id, product_id, quantity, low_stock_threshold, last_restocked_at, created_at, updated_at) VALUES
(1, 1, 50, 10, CURRENT_TIMESTAMP, NOW(), NOW()),
(2, 2, 35, 10, CURRENT_TIMESTAMP, NOW(), NOW()),
(3, 3, 5, 10, CURRENT_TIMESTAMP, NOW(), NOW()),   -- Low Stock Alert item!
(4, 4, 100, 15, CURRENT_TIMESTAMP, NOW(), NOW()),
(5, 5, 8, 10, CURRENT_TIMESTAMP, NOW(), NOW())    -- Low Stock Alert item!
ON DUPLICATE KEY UPDATE product_id=VALUES(product_id);
