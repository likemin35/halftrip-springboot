ALTER TABLE receipts
    ADD COLUMN payment_date_time DATETIME NULL AFTER amount;
