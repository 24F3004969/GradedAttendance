CREATE TABLE IF NOT EXISTS fee_payments
(
    payment_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    ed_no             TEXT    NOT NULL,
    month             TEXT    NOT NULL CHECK (
        month IN ('Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec')
        ),
    amount            REAL    NOT NULL CHECK (amount >= 0),
    paid_on           DATE    NOT NULL DEFAULT CURRENT_DATE,
    next_fee_date     TEXT,
    collected_by_name TEXT    NOT NULL,
    payment_mode      TEXT    NOT NULL CHECK (payment_mode IN ('Online', 'Offline')),
    gateway           TEXT,  -- 'UPI','Card','NetBanking','Cash','Cheque'
    reference_no      TEXT,  -- txn id, cheque no, receipt no
    due_amount        TEXT,

    FOREIGN KEY (ed_no) REFERENCES StudentData (ed_no) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_fee_payments_mode ON fee_payments (payment_mode, paid_on);