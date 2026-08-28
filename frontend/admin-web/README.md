# Hotel Booking Admin Web

The management portal is a React and TypeScript single-page application. It is
kept in the main repository because it evolves with the API contracts,
infrastructure, and deployment configuration.

## Run locally

Start the backend stack first from the repository root:

```powershell
docker compose up --build
```

Then run the frontend:

```powershell
cd frontend/admin-web
npm install
npm run dev
```

The Vite development server runs on `http://localhost:3000` and proxies `/api`
requests to the Compose API gateway at `http://localhost:18080` by default. Set
`VITE_API_PROXY_TARGET` to use another gateway address.

## Authentication

The portal signs in through `POST /api/auth/login`. The resulting access token
is held in `sessionStorage`, so it is cleared when the browser session ends.
The UI uses the JWT's permissions and hotel assignments to hide navigation
items that are not relevant to the signed-in staff member. The backend remains
the authorization authority; hiding a UI item never replaces API authorization.

## Current scope

- Login/logout and expired-session handling.
- Protected admin shell.
- Permission-aware navigation.
- Selected-hotel context.
- Placeholder pages for the planned management areas.

The reservations list and other management operations are the next increments.
