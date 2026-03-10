-- Ensure table exists
CREATE TABLE IF NOT EXISTS DueDate
(
    ed_no         TEXT PRIMARY KEY,
    last_due_date TEXT NOT NULL,
    FOREIGN KEY (ed_no) REFERENCES StudentData(ed_no) ON DELETE CASCADE
);

CREATE TRIGGER IF NOT EXISTS trg_fee_payments_after_insert
    AFTER INSERT ON fee_payments
    FOR EACH ROW
    WHEN NEW.next_fee_date IS NOT NULL
BEGIN
    INSERT INTO DueDate (ed_no, last_due_date)
    SELECT NEW.ed_no, MAX(next_fee_date)
    FROM fee_payments
    WHERE ed_no = NEW.ed_no
    ON CONFLICT(ed_no) DO UPDATE SET
        last_due_date = excluded.last_due_date;
END;


