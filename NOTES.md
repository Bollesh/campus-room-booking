# Notes

## B1: double-booking race

Test: `backend/src/test/java/com/campusbooking/service/BookingConcurrencyTest.java`. 20 threads
behind a `CountDownLatch` start gate each call `BookingService.createBooking` for room AB1/101 with
overlapping ranges (start 10:00 + i min, end 13:00) and distinct start times, so the
`(start_time, block, room_no)` primary key can't reject them. Runs on Postgres 16 via Testcontainers.

Before the fix (3 runs, 2026-09-29): **10 of 20 succeeded every time**, leaving 10 overlapping
rows in `booking`. The other 10 got the service's `IllegalStateException` from the conflict SELECT.

Why 10: HikariCP's default pool is 10 connections. The first 10 threads each get a connection and
run the conflict SELECT before any of them commits. Under READ COMMITTED none of them sees the
others' uncommitted INSERTs, so all 10 pass the check. The other 10 wait for a free connection,
start after those commits, and see the conflicts.

After the fix (`no_overlapping_active_booking` GiST exclusion constraint + `saveAndFlush` +
409 mapping, 4 runs): **1 of 20 succeeded every time**, 1 row in `booking`. All 19 losers got
`BookingConflictException` (409): some from the conflict SELECT, the rest from the constraint
(SQLState `23P01`), translated in `BookingService.createBooking`.

`BookingOverlapTest` also covers: sequential overlap is rejected, overlap with a REJECTED or
CANCELLED booking is allowed (the constraint's `WHERE`), back-to-back bookings are allowed
(`tsrange` is `[start, end)`), a raw SQL insert that skips the app check is still rejected with
`23P01`, and an overlapping `POST /api/bookings` returns 409.

Not covered: two requests with the *same* start time racing still collide on the primary key
(SQLState `23505`), which is not mapped and comes back as a 500.

**Correction (found during Step 3):** the fix is not complete. Repeating the race test 50 times
in one JVM, 31 of 50 runs had all 19 losers fail with `40P01 deadlock detected` ("while checking
exclusion constraint on tuple ... in relation booking"), surfacing as `CannotAcquireLockException`
(a 500 over HTTP) after up to ~20 s of deadlock-timeout waits. The other 19 runs behaved as above.
Every run still left exactly 1 row, so the constraint holds; what breaks is the losers' error and
latency. Cause: an exclusion constraint is checked after the index entry is inserted, so two
overlapping concurrent inserts can each wait on the other's transaction. The four clean runs
reported above were not enough to catch it.

## B2: approver / booking owner taken from the request body

Test: `backend/src/test/java/com/campusbooking/controller/BookingApprovalAuthTest.java` (MockMvc,
`@WithUserDetails` for seeded users, real `/api/auth/login` JWTs for the approval flow).

Before the fix, 4 of 7 tests failed, each an impersonation that worked:
- `poc.student1` posted an approval with `approverEmail=floor.manager1`: **200**, recorded as the
  floor manager.
- `floor.manager1` posted `approverEmail=security1`: recorded as `security1` / `SECURITY`.
- `poc.student2` (not in Tech Club) booked for Tech Club as `poc.student1`: **201**.
- `poc.student1` with `studentEmail=poc.student2` in the body: rejected, because the booking was
  attributed to `poc.student2`.

Fix: `BookingController` takes both identities from `@AuthenticationPrincipal UserDetails`
(username = email, set by `JwtRequestFilter`). The body fields are ignored.

## B3: any floor manager could approve any room

Before the fix, `floor.manager2` (manages AB2/101 only) approved an AB1/101 booking: **200**.
Fix: removed the `FLOOR_MANAGER` fallback in `BookingService.determineApproverRole`. Now 403.

Still by design (not changed): any student-council member, any cultural professor and any
security user can approve any booking, because those roles are campus-wide in this app.
