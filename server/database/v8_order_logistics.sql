USE yunshang_market;

ALTER TABLE orders
    ADD COLUMN shipping_company VARCHAR(80) NULL AFTER paid_at,
    ADD COLUMN tracking_no VARCHAR(80) NULL AFTER shipping_company,
    ADD COLUMN shipped_at DATETIME NULL AFTER tracking_no;
