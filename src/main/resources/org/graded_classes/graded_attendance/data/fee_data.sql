-- Make sure the fee_payments table and index exist (yours as given)
CREATE TABLE IF NOT EXISTS fee_payments
(
    payment_id        INTEGER
        primary key autoincrement,
    ed_no             TEXT
                                                references StudentData
                                                    on delete set null,
    month             TEXT                      not null,
    amount            REAL                      not null,
    paid_on           DATE default CURRENT_DATE not null,
    next_fee_date     TEXT,
    collected_by_name TEXT                      not null,
    payment_mode      TEXT                      not null,
    gateway           TEXT,
    reference_no      TEXT,
    due_amount        TEXT,
    student_name      TEXT,
    check (amount >= 0),
    check (month IN ('Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                     'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'))
);
CREATE INDEX IF NOT EXISTS idx_fee_payments_mode ON fee_payments (payment_mode, paid_on);
CREATE TRIGGER IF NOT EXISTS fee_payments_set_name
    AFTER INSERT ON fee_payments
    FOR EACH ROW
BEGIN
    UPDATE fee_payments
    SET student_name = (
        SELECT name FROM StudentData s WHERE s.ed_no = NEW.ed_no
    )
    WHERE payment_id = NEW.payment_id;
END;
-- Clear for repeatable test (optional in dev)
