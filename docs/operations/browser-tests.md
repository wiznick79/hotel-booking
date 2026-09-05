# Browser regression tests

From `frontend/public-web`:

```sh
npm ci
npx playwright install chromium
npm run test:browser
```

The suite runs the actual React public website in Chromium at desktop and mobile
sizes. Playwright starts and stops a dedicated Vite server on port 14327. It fails
if that port is occupied rather than reusing another project's server.

All API calls use explicit test fixtures. Unknown API calls fail the test, and
external requests are blocked. The tests do not create users or reservations in
PostgreSQL, send email, or contact payment providers. They verify browser behavior
and request payloads, not backend integration or real payment completion.

Coverage includes guest booking, unavailable inventory, password confirmation,
email verification success/failure, contact validation, paid reservations,
Multibanco voucher visibility, session refresh and profile updates.

CI runs these tests independently of Maven and saves failure screenshots/traces
for seven days. Run `npm run test:browser:report` to inspect the local HTML report.
Reports and test results are ignored by Git.

## Admin regressions

Install `frontend/admin-web` dependencies once with `npm ci`, then from
`frontend/public-web` run:

```sh
npx playwright test -c playwright.admin.config.ts
```

This suite uses port 14328 and controlled API fixtures. It verifies row ordering
after no-show, refreshable date filters without history pollution, and the
cancelled/paid review message.

## Full-stack booking

With Docker running, from `frontend/public-web`:

```sh
node tests/full-stack/run.mjs
```

The runner derives a disposable stack from the repository Compose definition.
It uses a random `hb-e2e-*` project name, random loopback host ports, an in-memory
PostgreSQL data directory and fresh test signing keys. It never loads the local
`.env`, and no application data volumes or fixed container names are reused.
Stripe is disabled; email goes to the test stack's Mailpit.

The test seeds one hotel and room via authenticated APIs, books through the real
public website, checks the stored reservation via the API, then waits for email
delivery through the outbox and Kafka. This is a booking smoke test; full identity
login and real provider callbacks remain separate coverage areas.

The runner removes its own containers, volumes and image tags in `finally` on
success or test failure. Forced process termination can prevent cleanup; any
remaining `hb-e2e-*` stack is disposable. Existing application containers are not
touched. CI runs this after the regular build. Full-stack traces are disabled to
avoid recording the temporary access tokens.
