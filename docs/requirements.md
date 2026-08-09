# Hotel Booking System Requirements

## Purpose

Build a small, secure hotel-management and booking system for the second family hotel, while providing practical experience with Java, Spring Boot, SQL, microservices, containers, and AWS.

The system should be suitable for a small real-world deployment without attempting to become a general-purpose hotel platform.

## Scope and users

The initial release targets one hotel. The data model must support a second hotel later.

The main users are trusted owners and managers. Existing staff may continue using paper processes and are not assumed to be comfortable with computers.

### User types

- Guest: can make a reservation without creating an account.
- Registered customer: can manage eligible reservations and view booking history.
- Staff member: can manage reservations and hotel information according to permissions.
- Owner/administrator: can manage hotels, rooms, room types, users, and configuration.

## Functional requirements

### Identity and access

- Users must not be required to create an account to make a booking.
- Registered customers must be able to authenticate securely.
- Staff users must have role-based permissions.
- Administrators have full system access and are the only users allowed to create or promote Managers.
- Managers can manage their assigned hotels and create Staff accounts, but cannot create or promote Administrators or Managers.
- Staff members are assigned to at least one hotel and have operational access only to their assigned hotels.
- A guest must be able to access a booking through a secure, expiring email link.
- Guest access must be limited to the specific booking represented by the link.
- Guest access links must remain valid for early bookings until the checkout date, followed by a configurable post-checkout grace period.
- Guest access tokens must be cryptographically random, stored only as hashes, and invalidated when the reservation is no longer valid.

### Hotel and inventory management

- The system must model hotels explicitly, even when only one hotel is initially active.
- Administrators must be able to create and manage hotels.
- Each hotel must have a configurable default content language.
- Hotels must contain room types.
- Room types must support configurable names and descriptions in multiple languages.
- The initial supported languages are English, Portuguese, Spanish, French, and German.
- Each room type must define at least maximum occupancy and base price.
- Individual physical rooms must belong to a hotel and room type.
- Rooms must have a number or other operational identifier.
- Inventory must support active/inactive lifecycle states.
- Staff must be able to inspect room availability in a weekly, all-rooms calendar. The calendar must distinguish free rooms, assigned reservations, scheduled maintenance blocks, and out-of-service rooms. The booking checkout date is exclusive in this view.

### Booking

- Guests must be able to search room-type availability for a date range and guest count. Search results must show date-specific prices but never physical room numbers.
- Guests select one or more room types, never a specific physical room number.
- A reservation reserves capacity in its selected room types for the stay; the system automatically assigns a suitable physical room for each item as an internal operational concern.
- Staff must be able to assign or change the physical room for each reservation item, subject to hotel, room-type, status, maintenance, and overlapping-reservation validation.
- One reservation may contain multiple rooms.
- Guests must be able to create a reservation without an account.
- The system must prevent overbooking a room type and prevent conflicting reservations for the same assigned room and dates.
- Guests must receive a booking confirmation by email.
- Guests must be able to view or manage eligible bookings through a secure link.
- Guest access is initially read-only; unauthenticated guests must contact staff to request booking changes.
- Registered customers must be able to view booking history.
- Registered customers may modify their own reservation before the cancellation deadline, subject to room availability and updated pricing validation.
- Reservations created by authenticated customers should be associated with the customer identity and visible through a personal reservation-history endpoint.
- Staff must be able to search, view, modify, and cancel reservations according to permissions.
- Initial reservation statuses are `PENDING`, `CONFIRMED`, `CANCELLED`, `CHECKED_IN`, `CHECKED_OUT`, and `NO_SHOW`.
- Check-in and check-out are date-only business values; the checkout date is exclusive.
- Hotel check-in/check-out hours are hotel configuration and are not part of the reservation date values.
- Staff must be able to check in a confirmed guest during the booked stay, check out a checked-in guest, and mark an unarrived past reservation as a no-show. `CHECKED_OUT` is displayed as **Completed** in the management interface.
- Staff must be able to view reservation details, including guest contact information, notes, payment and discount snapshots, and booked room-type/physical-room assignments.
- Reservations whose checkout date has passed cannot be cancelled, confirmed, or reassigned. A past reservation that was never checked in must be resolved explicitly as a no-show rather than being automatically marked completed.
- Guests may provide arrival details and special requests in a free-text notes field.
- Guest name, phone number, and guest count are required for an unauthenticated booking.
- Guest email is optional for unauthenticated bookings but recommended for confirmations and management information.
- A public booking must require affirmative acceptance of the privacy notice. The booking service records the
  acceptance timestamp for operational auditability; the final notice wording, retention policy, and contact details
  require review for the hotel’s real legal context.
- A reservation can be created without payment when the hotel permits pay-later bookings.
- Online payment must support credit/debit cards, PayPal, Multibanco references, and MB WAY. `PaymentMode`
  distinguishes online payment from pay-at-reception; a separate `PaymentMethod` records the selected channel.
- Staff must be able to manually confirm eligible pending or held reservations.
- Temporary holds must expire automatically and release their rooms when their expiration time is reached.
- Reservation prices are frozen when the reservation is created.
- Hotel managers should be able to create and manage discount codes.
- Guests and registered customers should be able to apply valid discount codes during booking.
- Discount codes should support configurable validity periods, usage limits, and discount values or percentages.
- The applied discount and its resulting price must be frozen as part of the reservation price snapshot.
- Room availability must support temporary staff holds and rooms marked unavailable for operational reasons.
- Hotel staff must be able to schedule a room as unavailable for a future period, including a reason such as renovation, maintenance, or damage repair.
- A future room-unavailability period must prevent new bookings that overlap the period.
- When an unavailable room has future reservations, the system must identify and warn about the affected reservations rather than silently cancelling them.
- Staff must be able to reassign affected reservations to other suitable rooms.
- Emergency unavailability must support an override for unsafe rooms, with the impact recorded for staff follow-up.
- Room unavailability changes and reservation reassignments must be auditable.
- The current implementation provides scheduled room-unavailability periods, overlap validation, a public availability check for booking-service integration, a staff-only affected-reservations query, emergency blocks with mandatory reasons, and audit records for unavailability creation and reservation room reassignment. Broader audit history remains future work.
- Booking dates are business dates and must be represented separately from audit timestamps.

### Notifications

- Booking confirmation emails must be sent asynchronously where practical.
- Notifications must be retryable and idempotent.
- Failed notifications must be observable and recoverable.
- Reservation notification events must use a transactional outbox. Failed delivery attempts use exponential backoff,
  become terminal after a configured maximum, and can be inspected and manually replayed by staff assigned to the
  relevant hotel.
- Email is the first notification channel. SMS delivery for phone-only bookings is a later provider integration.
- Local development uses Mailpit as a safe SMTP inbox. Staging and production support a real SMTP delivery provider with authenticated STARTTLS settings supplied through environment variables and AWS Parameter Store. Amazon SES is the initial recommended provider; raw credentials must never be logged or committed.
- Each hotel may configure its own email display name, from address, and reply-to address. These values are captured in the reservation notification event so notification-service does not synchronously depend on hotel-service while delivering email. Blank hotel-specific values fall back to the deployment-wide sender.

### Internationalization

- Hotel-managed content must support translations without language-specific columns.
- The requested language must be resolved using this precedence:
  1. Explicit request language
  2. Authenticated user preference
  3. `Accept-Language` request header
  4. Hotel default language
  5. System default language
- The system default language is English.
- Frontend interface translations are separate from hotel content translations.
- The public site and admin panel initially support English and Portuguese interface text. The public site stores the
  visitor’s interface choice locally and forwards it as `Accept-Language` when retrieving translated hotel content.

## Non-functional requirements

- Backend services must use Java, Spring Boot, SQL, and REST APIs.
- The system must use a small microservices architecture from the beginning.
- Services must own their data and must not access another service's database directly.
- The system must be runnable locally with Docker-supported dependencies.
- The system must be deployable to AWS in a low-cost development environment.
- Secrets must not be committed to source control.
- Passwords and sensitive tokens must never be stored or logged in plain text.
- Important actions must be auditable.
- APIs must validate input and return consistent error responses.
- The system must provide health checks, structured logs, and basic observability.
- Personal data must be handled according to applicable GDPR principles.

## Client applications

- A public booking website will be developed after the initial backend workflow is stable.
- An authenticated admin panel will support hotel and reservation management.
- The public website and admin panel are clients of the backend, not backend microservices.
- Development should use vertical slices so backend functionality can be tested through a simple client before visual polish.

## Initial architecture direction

- `hotel-service`: hotels, room types, translations, and rooms.
- `booking-service`: availability and reservation lifecycle.
- `identity-service`: registered customers, staff users, roles, and authentication.
- `notification-service`: email delivery and notification processing.

The exact extraction boundaries may evolve as the domain becomes clearer.

## Deliberately out of scope for the first release

- Kubernetes
- Multi-region deployment
- Event sourcing and CQRS
- Live payment-provider implementation, refunds, chargebacks, and reconciliation workflows
- Full hotel accounting
- Channel-manager integrations with booking platforms
- Housekeeping and maintenance workflows
- Supporting every hotel employee as a system user

## Open questions

- Whether payments are required in the first usable release.
- Cancellation and modification policies.
- Seasonal pricing and minimum-stay rules.
- Email provider selection.
- Exact staff roles and permissions.
- Payment provider selection and the legal/business rules for refunds, chargebacks, and payment reconciliation.

## Reservation policy decisions

- Registered customers may modify eligible bookings before the cancellation deadline.
- Guests without accounts cannot modify bookings through the secure link initially; they must contact staff.
- Late cancellation fees are deferred to a later phase.
- Hotels may configure whether pay-later bookings are accepted.
- The system should support hotel-configured limits or confirmation deadlines for unpaid bookings rather than treating every unpaid booking as permanently confirmed.
- Overlapping bookings for the same physical room are forbidden.
- Public bookings reserve room-type inventory. The system automatically allocates a suitable physical room without exposing its number to the guest during selection; staff may subsequently change that assignment without changing the booked room type or frozen price.
- Overlapping bookings associated with the same guest should normally produce a warning rather than an absolute rejection, because groups may legitimately have multiple rooms or reservations. Exact duplicate/abusive patterns can be restricted later.
- Seasonal, weekend, and holiday pricing rules are configurable per hotel. A rule can set prices for one or more room types, while any omitted room type keeps its base price.
- Weekend pricing applies to Friday and Saturday nights by default; Sunday is treated as a normal night.
- Weekend pricing is optional; when absent, the normal nightly price applies.
