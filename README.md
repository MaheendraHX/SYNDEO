# SYNDEO

SYNDEO is a student project exploring how people can find collaborators through shared skills, interests, goals, and availability. The project aims to support purpose-driven circles and collaborative projects with task tracking.

## Current Status

SYNDEO is a student collaboration platform with a vanilla JavaScript frontend and a Phase 1 Spring Boot backend.

The frontend uses the backend API for registration, login, profile management, collaborator discovery, circles, projects, and tasks. These features require a configured backend and MySQL database; the GitHub Pages frontend alone cannot provide the full server-backed experience.

The project is under development and is not yet a production deployment. Review the database migration safety notes before starting the backend against any existing database.

## Frontend Capabilities

- Landing page describing the SYNDEO concept.
- Login and registration forms with client-side validation and backend authentication.
- Dashboard navigation to profile, discovery, circles, and projects.
- Account profile details: skills, interests, experience, and availability are loaded and saved through the backend; the optional profile picture is browser-local.
- Search and filters for active account profiles. Results are not ranked recommendations.
- Server-backed circle listings, details, creation, and membership.
- Server-backed projects, membership, project tasks, assignments, and task status updates.
- About page with project abstract, KTU academic context, guide, and team credits.

## Technology

- **Frontend:** HTML, CSS, and vanilla JavaScript.
- **Backend:** Java 25, Spring Boot 3.5.6, Maven Wrapper, Spring JDBC, and REST/JSON.
- **Database:** MySQL 8.0.16+ with versioned Flyway migrations.
- **Password hashing:** Spring Security Crypto (BCrypt), used by the backend authentication implementation.

The frontend has no build step, package manifest, or third-party runtime dependency. The backend builds independently from `backend/`.

## Repository Structure

```text
SYNDEO/
├── README.md
├── .gitignore
├── backend/
│   ├── .mvn/wrapper/
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/syndeo/backend/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── dto/
│       │   ├── exception/
│       │   ├── model/
│       │   ├── repository/
│       │   └── service/
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── db/migration/
│       └── test/java/com/syndeo/backend/
└── frontend/
    ├── index.html
    ├── css/
    │   ├── app.css
    │   ├── auth.css
    │   ├── landing.css
    │   └── style.css
    ├── js/
    │   ├── api.js
    │   ├── app.js
    │   ├── auth.js
    │   ├── landing.js
    │   └── page-transitions.js
    └── pages/
        ├── about.html
        ├── circles.html
        ├── dashboard.html
        ├── discover.html
        ├── login.html
        ├── profile.html
        ├── projects.html
        └── register.html
```

## Run Locally

### Option 1: VS Code Live Server

1. Open the repository folder in VS Code.
2. Open `frontend/index.html`.
3. If the Live Server extension is installed, choose **Open with Live Server**.

### Option 2: Python Static Server

From the repository root, run:

```powershell
python -m http.server 8000 --directory .\frontend
```

Then open [http://localhost:8000/](http://localhost:8000/) in a browser. Stop the server with `Ctrl+C`.

## Run the Backend Locally

Requirements: Java 25 and a MySQL 8.0.16+ server. Maven is not required because the Maven Wrapper downloads the configured Maven distribution when needed.

**Database safety:** Do not start the application against an existing `syndeo` database until its migration state has been reviewed and a safe recovery plan approved. Flyway is enabled, automatic baselining is disabled, and the migrations are intended for a new, empty schema. Do not use these instructions to repair or overwrite an existing database.

For a new, dedicated, empty development database only, first review the migration files under `backend/src/main/resources/db/migration/` and arrange an appropriate backup before making database changes. The database must be created separately; the application does not create the database itself.

Set the environment variables in PowerShell for the current terminal. Use credentials configured on your own MySQL server:

```powershell
$env:SYNDEO_DB_USERNAME = "your_local_mysql_username"
$env:SYNDEO_DB_PASSWORD = "your_local_mysql_password"
$env:SYNDEO_JWT_SECRET = [Convert]::ToBase64String(
    [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
)
```

Keep the generated JWT secret private. Generate and configure a stable secret for each environment; changing it invalidates tokens signed with the previous secret.

Optional settings:

| Variable | Purpose |
| --- | --- |
| `SYNDEO_DB_URL` | Database JDBC URL; defaults to the local `syndeo` database |
| `SERVER_PORT` | HTTP port; defaults to `8080` |

From the repository root, run the backend tests without starting the application:

```powershell
Set-Location .\backend
.\mvnw.cmd test
```

Only start the application after confirming that the configured database is a suitable, dedicated development schema and that its migration state is safe. Starting Spring Boot applies pending Flyway migrations.

The health endpoints, when the backend is running, are:

- `GET http://localhost:8080/api/health`
- `GET http://localhost:8080/api/health/database`

Stop a running development server with `Ctrl+C`.

## Use the Frontend

The landing page links to Sign in, Join SYNDEO, and About SYNDEO. Signed-in application pages include Dashboard, Profile, Discover, Circles, and Projects.

Registration, login, account profile fields, discovery, circles, projects, and tasks use the backend API. Profile photos are stored in browser local storage per account and are not uploaded. Discovery applies profile search and filters; it does not run a recommendation or matching algorithm. Circle tags are not stored because the database schema does not define a tags field.

The GitHub Pages site serves the frontend only. Full server-backed functionality requires a separately running and configured backend and database. Feature pages require a valid sign-in; API errors are shown rather than replaced with local sample records.

## Backend Integration Notes

The Phase 1 Spring Boot backend includes:

- Registration and login with password hashing and JWT-based authentication.
- Profile retrieval and updates.
- Authenticated collaborator discovery.
- Circle and project creation, membership operations, and detail views.
- Project task listing, creation, and updates.
- Application and database health endpoints.
- Versioned Flyway migrations, including recommendation-log schema support.

The recommendation-log schema does not represent an implemented recommendation engine or recommendation API.

The frontend API client is in `frontend/js/api.js`. The frontend is hosted separately from the backend; GitHub Pages does not host the Spring Boot service or MySQL database.

**Migration warning:** Flyway is enabled, automatic baselining is disabled, and Flyway clean is disabled. The migrations are intended for an empty schema. Do not start the application against an existing or partially migrated database until its state has been reviewed and a backup-backed recovery plan approved. Automated tests do not replace validation against a safely isolated MySQL instance.

This project is still under development; API integration does not by itself establish production readiness or a public backend deployment.
