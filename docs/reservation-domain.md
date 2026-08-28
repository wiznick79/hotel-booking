# Reservation Domain

## Core model

```text
Reservation
├── Guest details
├── One or more ReservationItems
├── Status
├── Date range
├── Frozen price summary
├── Payment/confirmation policy
└── Notes

ReservationItem
└── Physical Room
```

Public bookings select a room type rather than a room number. A reservation item records that booked type immediately, and the system automatically assigns a suitable physical room internally at creation. Staff may later change that assignment if the replacement is suitable and free for the full stay.

Physical-room assignments remain necessary for the second hotel, where room access will eventually depend on a room-specific PIN. PIN generation and door integration are outside the current scope.

The checkout date is exclusive. A reservation from July 10 through July 14 occupies the nights of July 10, 11, 12, and 13.

Discount codes are a planned feature. Hotel managers will be able to configure codes with validity rules and discount values. A successfully applied discount must be stored in the reservation price snapshot so later changes to the code do not alter existing reservations.

## Room unavailability and maintenance

A room may be unavailable for a future period because of renovation, maintenance, damage, or another operational reason. This should be represented as a dated unavailability period rather than only changing the room's current active flag.

Creating an unavailability period prevents new reservations from using the room during the affected dates. Existing future reservations are not cancelled automatically; the system identifies them and warns staff so they can reassign the reservations. An emergency override is allowed for unsafe rooms and must record the reason and responsible staff member.

## Availability

Availability must account for:

- Confirmed and pending reservations
- Temporary staff holds
- Rooms marked unavailable for maintenance or other operational reasons
- Multiple room-type items within one reservation
- Unassigned reservation items that have already reserved a room type's capacity

The inventory invariant is that active reservations and holds must not exceed the number of eligible physical rooms for a room type and date range. Once a room is assigned, the additional invariant is that one physical room cannot have overlapping active reservations or holds.

## Payment and confirmation

Hotels may configure whether pay-later bookings are accepted. A future policy model can support:

- Pay at reception
- Payment-required bookings
- A maximum number of unpaid bookings
- A deadline by which unpaid bookings must be manually confirmed
- Automatic expiry of unconfirmed holds

This avoids making every unpaid reservation permanently confirmed while keeping payment integration out of the first slice.

## Pricing

Pricing is calculated during booking and stored as a snapshot on the reservation. Future changes to rates must not change existing reservations.

The pricing model supports hotel-level rules with room-type-specific prices. A rule can be either:

- A recurring season expressed as month/day boundaries, including periods that cross New Year (for example 20 December to 5 January).
- A one-off date override for a specific date range, including moving holidays such as Easter.

When rules overlap, the highest priority wins. At the same priority, a one-off date override wins over a recurring season. A rule applies only to the room types for which it has a price; all other room types fall back to their base price.

The initial weekend convention is Friday and Saturday nights. Sunday uses the normal nightly price. A missing weekend price also uses the normal nightly price.

## Communication

Email is the initial confirmation and secure-link channel. Guest access tokens are stored only as hashes and expire after the reservation checkout date plus a configurable grace period. SMS can be added later for phone-only bookings, but it requires an external SMS provider, delivery-status handling, costs, rate limiting, and privacy controls.

Public bookings require affirmative acceptance of the privacy notice. The booking service stores the acceptance flag and timestamp with the reservation; this is an operational audit record, not a substitute for a legally reviewed privacy programme.
