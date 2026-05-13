-- Wipes everything except the seeded demo data.
-- Run in Railway Data tab (or any MySQL client) when you want a clean slate
-- for a demo video / submission screenshot.
--
-- After running this, the database looks exactly like the very first launch:
--   - 3 default users (admin / employee / seller)
--   - 10 suppliers, 30 parts (seeded)
--   - 30 days of historical sales (seeded)
--   - 4 sample purchases (seeded)

DELETE FROM sale_items     WHERE sale_id     > 30;
DELETE FROM sales          WHERE id          > 30;
DELETE FROM purchase_items WHERE purchase_id > 4;
DELETE FROM purchases      WHERE id          > 4;

-- Pretend the seeded historical sales were already approved, so reports keep
-- showing meaningful data and the approval queue starts empty.
UPDATE sales SET status = 'APPROVED', approver_id = 1, approved_at = created_at
  WHERE id <= 30;

-- Restart auto-increment counters so new sales start at id 31, not 9999.
ALTER TABLE sales     AUTO_INCREMENT = 31;
ALTER TABLE purchases AUTO_INCREMENT = 5;
