# campus-room-booking

A room-booking system for a college campus. Students book rooms for club events. A booking is
confirmed only after it is approved by five roles: the club's faculty head, the student council, a
cultural professor, the room's floor manager and security.

Forked from [Jawwad2005/DBSProj](https://github.com/Jawwad2005/DBSProj), originally built by
[Jawwad2005](https://github.com/Jawwad2005) and [ParallelParking](https://github.com/ParallelParking)
as a database systems course project (April 2025). This README describes the code as it is in the
fork today. See [Known issues](#known-issues) for what is being fixed.

## Stack

| Part | Tech |
|---|---|
| Backend | Java 21, Spring Boot 3.4.4 (Web, Data JPA, Security, Validation), jjwt 0.11.5 |
| Database | PostgreSQL, schema in hand-written SQL with PL/pgSQL trigger functions |
| Frontend | React 19, Vite 6, React Router 7, react-datepicker |
| Build | Maven wrapper (Maven 3.9.9), npm |

## Repository layout

```
campus-room-booking/
├── backend/
│   ├── pom.xml, mvnw, .mvn/
│   ├── test.http                      # sample requests (VS Code REST Client / IntelliJ HTTP client)
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
or a local PostgreSQL, and Node.js 18+.

1. **Database.** The backend expects PostgreSQL on `localhost:5432`, database `roombooking`, user
   `myuser`, password `mypassword` (see `backend/src/main/resources/application.properties`):
   ```sh
   docker run -d --name roombooking-db -p 5432:5432 \
     -e POSTGRES_DB=roombooking -e POSTGRES_USER=myuser -e POSTGRES_PASSWORD=mypassword \
     postgres:16
   ```
2. **Backend** (port 8080):
   ```sh
   cd backend
   JWT_SECRET_KEY='<a long random string>' ./mvnw spring-boot:run
   ```
   Without `JWT_SECRET_KEY` it falls back to a development-only default. On startup the schema
   scripts run and `DataLoader` inserts the seed data below.
3. **Frontend** (port 5173):
   ```sh
   cd frontend
   npm install
   npm run dev
   ```
   API calls go to `http://localhost:8080`, which is hard-coded in the components. CORS allows
   `localhost:5173`, `localhost:3000` and `localhost:8080`.

> **Warning:** every backend start drops and recreates all tables, so data does not survive a
> restart. See [Known issues](#known-issues).

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

### Booking lifecycle

1. A student who is a member of a club books a room for a time range (`POST /api/bookings`).
   The service checks that the room, student and club exist, that the student is in the club, and
   that no other booking overlaps the range (rejected and cancelled bookings are ignored). The
   booking starts as `PENDING_APPROVAL`.
2. Each approver submits `APPROVED` or `REJECTED`
   (`POST /api/bookings/{block}/{roomNo}/{startTime}/approvals`). The backend works out the
   approver's role for that booking from their email: floor manager of the room, faculty head of the
   club, cultural professor, student council, or security. Each role can respond once per booking.
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
  "studentEmail": "poc.student1@example.com",
  "clubName": "Tech Club"
}
```

Example approval request body:

```json
{ "approverEmail": "floor.manager1@example.com", "status": "APPROVED", "comments": "ok" }
```

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

The only test is `CampusRoomBookingApplicationTests`, and it only checks that the Spring context
starts. It needs the database running.

## Known issues

These were found by reading the code. They are being confirmed with tests and fixed in this fork.

1. **Double booking under concurrent requests.** The overlap check is a `SELECT` followed by an
   `INSERT`, so two overlapping requests arriving together can both pass. The primary key only
   blocks an identical start time.
2. **Caller identity is taken from the request body.** `approverEmail` (approvals) and
   `studentEmail` (bookings) come from the JSON, not the JWT, so a logged-in user can act as
   someone else.
3. **Approver roles are too broad.** Any floor manager is accepted as `FLOOR_MANAGER`, even for
   rooms they do not manage. Any student council member, cultural professor or security user can
   approve any booking.
4. **Data is wiped on every restart.** `spring.sql.init.mode=always` runs `schema-tables.sql`,
   which drops all tables first.
5. **Anyone can register as any role**, including `SECURITY` and `FLOOR_MANAGER`, through the public
   register endpoint.
6. **Access rules that don't match the controllers.**
   - `SecurityConfig` protects `/api/student-council/**`, but the controller is at
     `/api/studentcouncil`, so that rule never matches.
   - Student council members get only `ROLE_STUDENT_COUNCIL`, so they are refused on
     `GET /api/rooms` and `GET /api/memberships/student/**`, which require `STUDENT`.
7. **Smaller issues:**
   - Any authenticated user can delete any booking.
   - A rejected or cancelled booking keeps its primary key, so the same room can't be booked again
     at the exact same start time.
   - The service allows a booking without a club, but the database trigger rejects it.
