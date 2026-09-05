# Public website hotel selection

The public website never selects the first hotel returned by the API. When there
is exactly one hotel, it is selected automatically. With multiple hotels, configure
the property this website represents using its hotel UUID from the admin/API.

For Docker Compose, set `HB_PUBLIC_HOTEL_ID` in your local `.env`, then rebuild:

```sh
docker compose up -d --build public-web
```

For Vite development, set `VITE_PUBLIC_HOTEL_ID` in
`frontend/public-web/.env.local` and restart Vite. For a direct Docker build, pass
`--build-arg VITE_PUBLIC_HOTEL_ID=<hotel-uuid>`.

This is public, build-time configuration, not a secret. Changing it requires a
frontend rebuild, not merely a container restart. Each property's deployment can
build the same source with a different ID; DNS alone does not select a hotel.

A missing configured hotel, an empty list, or an ambiguous unconfigured list
shows the hotel-loading error and leaves booking unavailable instead of falling
back to another property. It does not replace backend authorization or isolate
the public hotel catalog. The disposable full-stack test still uses its single
test hotel and does not load the developer's `.env`.
