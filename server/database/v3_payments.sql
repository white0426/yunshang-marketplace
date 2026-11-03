USE yunshang_market;

ALTER TABLE orders
    ADD COLUMN payment_no VARCHAR(40) NULL AFTER status,
    ADD COLUMN payment_expires_at DATETIME NULL AFTER payment_no,
    ADD COLUMN paid_at DATETIME NULL AFTER payment_expires_at,
    ADD INDEX idx_order_payment_no (payment_no);

ALTER TABLE orders DROP CHECK chk_order_status;
ALTER TABLE orders
    ADD CONSTRAINT chk_order_status
    CHECK (status IN ('待支付', '待发货', '待收货', '已完成', '已取消'));
