# SYNDEO

SYNDEO is a student project exploring how people can find collaborators through shared skills, interests, goals, and availability. The project aims to support purpose-driven circles and collaborative projects with task tracking.

## Current Status

This repository currently contains the frontend only. It is a browser-based demonstration built with HTML, CSS, and vanilla JavaScript. The Spring Boot backend and MySQL integration are planned work and are not included here.

## Frontend Capabilities

- Landing page describing the SYNDEO concept.
- Login and registration forms with client-side validation. Authentication and account creation are not connected.
- An explicit demo entry point that does not require signing in.
- Dashboard navigation to profile, discovery, circles, and projects.
- Editable profile details: skills, interests, experience, availability, and an optional profile picture.
- Search and filters for a small set of fictional collaborator examples.
- Sample circles with local demo creation and membership interactions.
- Sample projects with optional circle associations, task creation, fictional sample assignees, task statuses, and progress updates.

## Technology

- **Frontend:** HTML, CSS, and vanilla JavaScript.
- **Backend (planned):** Java and Spring Boot.
- **Database (planned):** MySQL.

There is no frontend build step, package manifest, or third-party runtime dependency in the current repository. The landing-page artwork is inline SVG.

## Repository Structure

```text
SYNDEO/
├── README.md
├── .gitignore
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

## Explore the Frontend Demo

From the landing page, select **Sign in** or **Join SYNDEO**, then choose **Explore demo**. This opens the dashboard without creating an account. The app pages also link between Dashboard, Profile, Discover, Circles, and Projects.

The demo uses fictional collaborator, circle, project, task, and assignee examples. Profile edits (including a selected picture), circle interactions, and project/task changes are frontend-only and are stored in the browser's local storage where applicable. They are not shared with other users, sent to a server, or persisted in MySQL. The discovery examples are illustrative and are not results from an integrated matching engine.

## Backend Integration Notes

Real authentication, account creation, matching-engine integration, and server-side persistence await backend implementation. Backend APIs and their request/response contracts have not yet been integrated or defined in this repository. `frontend/js/api.js` is currently an empty placeholder; no API endpoints should be assumed from the frontend demo.

When backend work is ready, agree on API contracts with the team before connecting the forms and demo data to server operations. Keep demo actions clearly distinguished from real server-side behavior until that integration is complete.
