USE yunshang_market;

UPDATE orders
SET tracking_no = CONCAT(
    CASE shipping_company
        WHEN '顺丰速运' THEN 'SF'
        WHEN '圆通速递' THEN 'YT'
        WHEN '中通快递' THEN 'ZT'
        WHEN '韵达快递' THEN 'YD'
        WHEN '京东物流' THEN 'JD'
        WHEN '邮政快递' THEN 'YZ'
        ELSE 'EX'
    END,
    UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 22))
)
WHERE tracking_no IS NOT NULL
  AND CHAR_LENGTH(tracking_no) <> 24;

ALTER TABLE orders
    ADD CONSTRAINT chk_order_tracking_no
    CHECK (tracking_no IS NULL OR CHAR_LENGTH(tracking_no) = 24);
