-- No two active bookings of the same room may overlap. The app-level conflict SELECT in
-- BookingService is racy under READ COMMITTED; this constraint is the actual guarantee.
-- btree_gist lets the GiST index compare the plain equality columns (block, room_no).
-- tsrange defaults to [start, end), so back-to-back bookings (10-11, 11-12) are allowed.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE booking ADD CONSTRAINT no_overlapping_active_booking
    EXCLUDE USING gist (
        block WITH =,
        room_no WITH =,
        tsrange(start_time, end_time) WITH &&
    ) WHERE (overall_status IN ('PENDING_APPROVAL', 'APPROVED'));
