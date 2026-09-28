# campus-room-booking

A room-booking system for a college campus. Students book rooms for club events. A booking is
confirmed only after it is approved by five roles: the club's faculty head, the student council, a
cultural professor, the room's floor manager and security.

Forked from [Jawwad2005/DBSProj](https://github.com/Jawwad2005/DBSProj), originally built by
[Jawwad2005](https://github.com/Jawwad2005) and [ParallelParking](https://github.com/ParallelParking)
as a database systems course project (April 2025). This README describes the code as it is in the
fork today. [What I changed](#what-i-changed) lists the bugs fixed in this fork and the tests that
prove them; [Known issues](#known-issues) lists what is still open.

## Stack

| Part | Tech |
|---|---|
| Backend | Java 21, Spring Boot 3.4.4 (Web, Data JPA, Security, Validation), jjwt 0.11.5 |
| Database | PostgreSQL 16, schema managed by Flyway migrations (SQL with PL/pgSQL trigger functions) |
| Frontend | React 19, Vite 6, React Router 7, react-datepicker |
| Build | Maven wrapper (Maven 3.9.9), npm |
| Tests | JUnit 5, MockMvc, spring-security-test, Testcontainers (PostgreSQL) |
| CI | GitHub Actions: `./mvnw -B verify` on every push and pull request |

## Repository layout

```
campus-room-booking/
├── .github/workflows/ci.yml           # CI: ./mvnw -B verify
├── NOTES.md                           # measured before/after results for each fix
├── backend/
│   ├── pom.xml, mvnw, .mvn/
│   ├── compose.yaml                   # local PostgreSQL 16
│   ├── test.http                      # sample requests (VS Code REST Client / IntelliJ HTTP client)
│   ├── src/test/java/com/campusbooking/  # Testcontainers-based tests (see Tests)
│   └── src/main/
│       ├── java/com/campusbooking/
│       │   ├── CampusRoomBookingApplication.java
│       │   ├── config/                # SecurityConfig, DataLoader (seed data)
│       │   ├── controller/            # REST controllers; controller/security/AuthController
│       │   ├── model/                 # JPA entities + composite-key classes
│       │   ├── repository/            # Spring Data repositories
│       │   ├── security/              # JwtUtil, JwtRequestFilter
│       │   ├── service/               # business logic; service/security/UserDetailsServiceImpl
│       │   └── types/                 # request/response DTOs and enums
│       └── resources/
│           ├── application.properties
│           └── db/migration/          # Flyway migrations: V1 baseline (tables, functions,
│                                      #   triggers), V2 no-overlap constraint
└── frontend/
    └── src/
        ├── App.jsx                    # routes
        ├── home.jsx, landing.jsx, login.jsx, register.jsx
        ├── dashboards/                # one dashboard per role
        ├── components/
        └── styles/
```

## Running it locally

**Requirements:** a full JDK 21 or newer (a JRE is not enough, because `javac` must exist), Docker
(for the database and the tests), and Node.js 18+. If `./mvnw -v` reports a JRE or a different
Java, point `JAVA_HOME` at the JDK, e.g. `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./mvnw ...`.

1. **Database.** The backend expects PostgreSQL on `localhost:5432`, database `roombooking`, user
   `myuser`, password `mypassword` (see `backend/src/main/resources/application.properties`).
   `backend/compose.yaml` starts exactly that, with a named volume for the data:
   ```sh
   cd backend
   docker compose up -d
   ```
2. **Backend** (port 8080):
   ```sh
   cd backend
   JWT_SECRET_KEY='<a long random string>' ./mvnw spring-boot:run
   ```
   Without `JWT_SECRET_KEY` it falls back to a development-only default. On startup Flyway applies
   any pending migrations from `src/main/resources/db/migration`, and `DataLoader` inserts the seed
   data below if the `users` table is empty. Data survives restarts.
3. **Frontend** (port 5173):
   ```sh
   cd frontend
   npm install
   npm run dev
   ```
   API calls go to `http://localhost:8080`, which is hard-coded in the components. CORS allows
   `localhost:5173`, `localhost:3000` and `localhost:8080`.

> **Upgrading an old local database:** a database created before the switch to Flyway has tables
> but no Flyway history, and Flyway refuses to start on it. Recreate it once with
> `docker compose down -v && docker compose up -d` (the old setup wiped its data on every start
> anyway).

### Seed accounts

| Role | Email | Password |
|---|---|---|
| Student (Tech Club point of contact) | `poc.student1@example.com` | `studentpass1` |
| Student (Music Club point of contact) | `poc.student2@example.com` | `studentpass2` |
| Professor, faculty head of Tech Club | `faculty.head1@example.com` | `profpass1` |
| Professor, faculty head of Music Club | `faculty.head2@example.com` | `profpass2` |
| Cultural professor | `cultural.prof@example.com` | `profpass3` |
| Floor manager of room AB1/101 | `floor.manager1@example.com` | `managerpass1` |
| Floor manager of room AB2/101 | `floor.manager2@example.com` | `managerpass2` |
| Student council (President) | `sc.member1@example.com` | `scpass1` |
| Security | `security1@example.com` | `securitypass1` |

The seeded rooms are `AB1/101` and `AB2/101`. The seeded clubs are Tech Club and Music Club, each
with its point-of-contact student as a member.

## How it works

### Roles

Every account is a row in `users` plus a row in exactly one role table: `student`, `professor`
(with an `is_cultural` flag), `floor_manager`, `security`, or `student_council`. A student council
member is also a `student`. At login, `UserDetailsServiceImpl` maps the account to one Spring
Security role: `ROLE_STUDENT`, `ROLE_PROFESSOR`, `ROLE_FLOOR_MANAGER`, `ROLE_SECURITY` or
`ROLE_STUDENT_COUNCIL`.

Who may approve a booking:

- **Floor manager:** only the manager of the booked room.
- **Faculty head:** only the faculty head of the booking's club.
- **Cultural professor, student council, security:** any holder of the role, for any booking. This
  is intended. These are campus-wide offices, and the data model doesn't tie them to a room, block
  or club. Each role still responds only once per booking.

Students see only their own bookings (`GET /api/bookings?studentEmail=<own email>`). The full list
(`GET /api/bookings`) is for the approver roles.

### Booking lifecycle

1. A student who is a member of a club books a room for a time range (`POST /api/bookings`).
   The booking is made in the name of the logged-in student (from the JWT). The service locks the
   room row, checks that the room, student and club exist, that the student is in the club, and
   that no other active booking overlaps the range (rejected and cancelled bookings are ignored).
   An overlap returns **409 Conflict**. The database enforces the same rule with an exclusion
   constraint. The booking starts as `PENDING_APPROVAL`.
2. Each approver submits `APPROVED` or `REJECTED`
   (`POST /api/bookings/{block}/{roomNo}/{startTime}/approvals`). The approver is the logged-in
   user (from the JWT). The backend works out their role for that booking: floor manager of that
   room, faculty head of the club, cultural professor, student council, or security. Anyone else
   gets **403**. Each role can respond once per booking.
3. After every response the overall status is recomputed:
   - any rejection -> `REJECTED`
   - approvals from all of `FACULTY_HEAD`, `STUDENT_COUNCIL`, `CULTURAL_PROF`, `FLOOR_MANAGER` and
     `SECURITY` -> `APPROVED` (the service skips the faculty head for bookings with no club)
   - otherwise it stays `PENDING_APPROVAL`

Booking statuses are `PENDING_APPROVAL`, `APPROVED`, `REJECTED` and `CANCELLED`. Approval statuses
are `PENDING`, `APPROVED` and `REJECTED`.

### Data model

| Table | Primary key | Notes |
|---|---|---|
| `users` | `email` | name, phone, bcrypt password |
| `student` | `email` -> users | `regno` unique |
| `professor` | `email` -> users | `is_cultural` |
| `floor_manager`, `security` | `email` -> users | |
| `student_council` | `email` -> student | `position` |
| `room` | `(block, room)` | `manager_email` -> floor_manager |
| `club` | `name` | `faculty_head_email` -> professor, `poc_student_email` -> student (both unique) |
| `club_membership` | `(club_name, stu_email)` | |
| `booking` | `(start_time, block, room_no)` | `end_time`, `purpose`, `student_email`, `club_name`, `overall_status` |
| `booking_approval` | `id` (identity) | FK to booking's composite key; `approver_role`, `approver_email`, `approval_status`, `approval_time`, `comments` |

Every table has a `BEFORE INSERT` trigger (defined in `V1__baseline.sql`) that raises an error when a
required column is null.

### Authentication

- `POST /api/auth/register` creates a user of any `userType` (`STUDENT`, `PROFESSOR`,
  `FLOOR_MANAGER`, `SECURITY`, `STUDENT_COUNCIL`). Passwords are bcrypt-hashed and must be at least
  6 characters.
- `POST /api/auth/login` returns a JWT and the user's role (e.g. `ROLE_STUDENT`). The JWT is HS256,
  its subject is the email, it has a `roles` claim, and it expires after 10 hours. The frontend stores
  `jwt`, `role` and `email` in `localStorage` and sends `Authorization: Bearer <jwt>`.

The access rules are set in `SecurityConfig`:

- **Public:** `/api/auth/**`, `GET /api/clubs/**`, `POST /api/memberships`.
- **`GET /api/memberships/student/**`** requires `STUDENT`.
- **`GET /api/rooms`** requires `STUDENT` or `FLOOR_MANAGER`.
- **Every other `/api/**` route** accepts any valid JWT.

## API

All paths are under `http://localhost:8080`. `{startTime}` is an ISO date-time, e.g.
`2026-10-01T10:00:00`.

| Resource | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Bookings | `GET /api/bookings`, `GET /api/bookings?studentEmail=`, `GET /api/bookings/{block}/{roomNo}/{startTime}`, `POST /api/bookings`, `PUT /api/bookings/{block}/{roomNo}/{startTime}/purpose`, `DELETE /api/bookings/{block}/{roomNo}/{startTime}`, `POST /api/bookings/{block}/{roomNo}/{startTime}/approvals` |
| Rooms | `GET /api/rooms`, `GET /api/rooms/{block}/{room}`, `POST /api/rooms`, `PUT /api/rooms/{block}/{room}/manager`, `DELETE /api/rooms/{block}/{room}` |
| Clubs | `GET /api/clubs`, `GET /api/clubs/{name}`, `POST /api/clubs`, `PUT /api/clubs/{name}`, `DELETE /api/clubs/{name}` |
| Memberships | `GET /api/memberships`, `POST /api/memberships`, `GET /api/memberships/student/{email}`, `GET /api/memberships/club/{clubName}`, `GET /api/memberships/student/{email}/club/{clubName}`, `DELETE /api/memberships/student/{email}/club/{clubName}` |
| Users | `GET /api/user/all`, `POST /api/user`, `GET/PUT/DELETE /api/user/{email}` |
| Per role | `/api/students`, `/api/professors`, `/api/floormanagers`, `/api/security`, `/api/studentcouncil`: each has `GET` (list), `GET /{email}`, `POST`, `PUT /{email}`, `DELETE /{email}` |

Example booking request body:

```json
{
  "block": "AB1",
  "roomNo": "101",
  "startTime": "2026-10-01T10:00:00",
  "endTime": "2026-10-01T12:00:00",
  "purpose": "Tech Club meetup",
  "clubName": "Tech Club"
}
```

Example approval request body:

```json
{ "status": "APPROVED", "comments": "ok" }
```

Both requests need an `Authorization: Bearer <jwt>` header from `POST /api/auth/login`. The student
and approver come from that token. A `studentEmail` or `approverEmail` field in the body is ignored
(the frontend still sends them).

There are more example requests in `backend/test.http`.

## Frontend

| Route | Page |
|---|---|
| `/` | Home |
| `/register` | Student sign-up, then pick clubs to join |
| `/login` | Login; redirects to the dashboard for the returned role |
| `/student-dashboard` | Student home, links to booking and viewing |
| `/student-book` | Book a room: pick a room, one of the student's clubs, and a start and end time |
| `/student-view` | The student's own bookings |
| `/professor-dashboard` | Bookings to approve or reject. A faculty head sees bookings for the clubs they head; the cultural professor sees all bookings |
| `/floor-manager-dashboard` | Bookings for the rooms this manager manages |
| `/student-council-dashboard`, `/security-dashboard` | All bookings |

Each approver dashboard hides bookings that are already `REJECTED` and bookings this user has
already responded to. Dashboards fetch every booking and filter in the browser, because the backend
does not filter bookings by role.

## Tests

```sh
cd backend
./mvnw verify
```

The tests need Docker: each Spring test context starts its own PostgreSQL 16 container
(Testcontainers) and Flyway builds the schema in it, so they run against the real triggers and
constraints, not H2. They do not use the database from `compose.yaml`.

| Test | What it checks |
|---|---|
| `BookingConcurrencyTest` | 20 threads book the same room with overlapping times at once (different start times, then the same start time); exactly 1 succeeds and the other 19 get a conflict |
| `BookingReadAccessTest` | booking responses contain no password hashes; students get 403 for the full list and for another student's bookings; approvers see all; registration and login still work |
| `BookingOverlapTest` | overlaps rejected (409 over HTTP); overlap with a rejected or cancelled booking allowed; back-to-back allowed; the database rejects an overlapping raw `INSERT` |
| `BookingApprovalAuthTest` | approver and booking owner come from the JWT; students and floor managers of other rooms get 403; all five approvals -> `APPROVED`, one rejection -> `REJECTED` |
| `CampusRoomBookingApplicationTests` | the application context starts |

CI (`.github/workflows/ci.yml`) runs `./mvnw -B verify` on every push and pull request.

## What I changed

The original project is by [Jawwad2005](https://github.com/Jawwad2005) and
[ParallelParking](https://github.com/ParallelParking)
([upstream repo](https://github.com/Jawwad2005/DBSProj)). In this fork I found six bugs (four by
reading the code, two more while testing those fixes), confirmed each one with a test that failed
before the fix, and then fixed it. Measured numbers and details are in [`NOTES.md`](NOTES.md).

### 1. Double booking under concurrent requests

- **Found:** `BookingService.createBooking` runs a conflict `SELECT` and then an `INSERT`. Under
  PostgreSQL's default READ COMMITTED isolation, concurrent transactions don't see each other's
  uncommitted rows, so several overlapping requests can all pass the check. The primary key
  `(start_time, block, room_no)` only blocks an identical start time.
- **Proved:** `BookingConcurrencyTest` sends 20 overlapping requests for one room at once, with
  different start times so the primary key can't help. Before the fix, **10 of 20 succeeded** on
  every run (10 is the connection pool size: those 10 all ran the check before any committed).
- **Fixed:**
  - A PostgreSQL exclusion constraint, `no_overlapping_active_booking`
    (`EXCLUDE USING gist (block WITH =, room_no WITH =, tsrange(start_time, end_time) WITH &&)` for
    `PENDING_APPROVAL` and `APPROVED` bookings), so the database itself rejects overlaps.
  - `createBooking` locks the room row (`SELECT ... FOR NO KEY UPDATE`) before the check, so
    bookings of the same room are checked one at a time. Without the lock, concurrent overlapping
    inserts could deadlock inside the constraint check: in 31 of 50 repeated runs the losers got a
    deadlock error (a 500) after up to ~20 s.
  - Overlaps return **409 Conflict** (`BookingConflictException`), whether caught by the check or by
    the constraint (`saveAndFlush` raises the violation inside the method, where it's translated).
- **Result:** 1 of 20 succeeds and the other 19 get a 409, on 50 of 50 runs.

### 2. Caller identity taken from the request body

- **Found:** `approverEmail` (approvals) and `studentEmail` (bookings) were read from the JSON body.
- **Proved:** `BookingApprovalAuthTest`. Before the fix, a student approved a booking as the floor
  manager (200), an approval was recorded under the email in the body instead of the caller's, and a
  student booked a room in another student's name (201).
- **Fixed:** `BookingController` takes both from the authenticated user (`@AuthenticationPrincipal`,
  set from the JWT) and ignores the body fields. The frontend didn't need changes.

### 3. Floor managers could approve any room

- **Found:** `determineApproverRole` fell back to `FLOOR_MANAGER` for any floor manager.
- **Proved:** the floor manager of AB2/101 approved an AB1/101 booking (200).
- **Fixed:** removed the fallback. Only the manager of the booked room gets `FLOOR_MANAGER`; others
  get 403.

### 4. Every restart wiped the database

- **Found:** `spring.sql.init.mode=always` ran `schema-tables.sql` on every start, and it began with
  `DROP TABLE ... CASCADE`.
- **Fixed:** the schema is managed by Flyway. `V1__baseline.sql` has the original tables, functions
  and triggers; `V2__no_overlapping_bookings.sql` adds the constraint from fix 1.
- **Verified:** created a booking, restarted the app, and the booking was still there.

### 5. Password hashes in API responses

- **Found (while testing fix 2):** approval responses included bcrypt `password` hashes. Every
  response that embeds a user (the student, the room's manager, the club's faculty head and point of
  contact) serialized `Users.password`, and `GET /api/bookings` returns these to any logged-in user.
- **Proved:** `BookingReadAccessTest`: `GET /api/bookings` and `GET /api/bookings/{...}` bodies
  contained `password` and `$2a$...` hashes.
- **Fixed:** `Users.password` is `@JsonProperty(access = WRITE_ONLY)`: accepted in request bodies,
  never written out. Registration, login and the entity create endpoints still work (tested).

### 6. Any user could list anyone's bookings

- **Found:** `GET /api/bookings?studentEmail=` trusted the query parameter (same pattern as fix 2),
  and plain `GET /api/bookings` returned every booking to every user.
- **Proved:** a student got 200 for another student's bookings and for the full list.
- **Fixed:** students get 403 for both; they can list only their own bookings. Approver roles keep
  the full list, which their dashboards use.

### Checked, not a bug

- **Same-start-time race:** 20 concurrent requests with the *same* start time also collide on the
  primary key, which would surface as a 500. With the room lock from fix 1, 1 of 20 succeeds and the
  other 19 get a 409 (`BookingConcurrencyTest`), so no separate fix was needed.

### Also added

- A test suite on real PostgreSQL via Testcontainers (see [Tests](#tests)); before, the only test
  checked that the context starts.
- `backend/compose.yaml` for the local database, and CI on GitHub Actions.

## Known issues

Still open.

1. **Anyone can register as any role**, including `SECURITY` and `FLOOR_MANAGER`, through the public
   register endpoint.
2. **The per-role create endpoints store passwords unhashed, and any logged-in user can call them.**
   `POST /api/students` (and `/api/professors`, `/api/floormanagers`, `/api/security`,
   `/api/studentcouncil`) saves the `password` from the body as is, without hashing, and nothing
   restricts who can create accounts this way. Found while testing fix 5. `/api/auth/register`
   hashes correctly.
3. **Single bookings are readable by anyone logged in.** `GET /api/bookings/{block}/{roomNo}/{startTime}`
   is not restricted to the owner and approvers, unlike the lists (fix 6).
4. **Access rules that don't match the controllers.**
   - `SecurityConfig` protects `/api/student-council/**`, but the controller is at
     `/api/studentcouncil`, so that rule never matches.
   - Student council members get only `ROLE_STUDENT_COUNCIL`, so they are refused on
     `GET /api/rooms` and `GET /api/memberships/student/**`, which require `STUDENT`.
5. **Smaller issues:**
   - Any authenticated user can delete any booking.
   - A rejected or cancelled booking keeps its primary key, so the same room can't be booked again
     at the exact same start time.
   - The service allows a booking without a club, but the database trigger rejects it.
