# Future Payment Integration

The current booking flow supports **pay at reception** and a development-only hosted-checkout simulator. It never
collects card data. The simulator exercises the same redirect and completion boundary that a real provider will use,
without pretending to process payment details.

## Current boundary

`booking-service` records the selected `PaymentMode`, `PaymentMethod`, frozen reservation total, and whether manual
confirmation is required. The payment adapter contract supports several providers without coupling reservations to
their SDKs.

Supported business methods are:

- `CARD` — credit/debit card
- `PAYPAL`
- `MULTIBANCO` — entity/reference voucher
- `MB_WAY`
- `PAY_AT_RECEPTION`

Each hotel configures its available online methods in the admin panel. `PaymentProvider` supports initiation and
signed-webhook verification, while `PaymentProviderRegistry` chooses the matching adapter. A `PaymentAttempt` persists
the provider reference and lifecycle (`PENDING`, `SUCCEEDED`, `FAILED`, `EXPIRED`, or `REFUNDED`). Online reservations
remain held until a successful provider callback confirms them.

For online payments, reservation creation and provider-checkout initiation are deliberately separate HTTP requests.
The first request commits the reservation, hold, and guest-access token. It returns the token only in the
`X-Guest-Access-Token` response header; the public site then uses it to initiate checkout in a second request. This
prevents a fast asynchronous provider webhook (notably
Multibanco's `payment_intent.requires_action`) from arriving before the local payment attempt has committed. Repeating
the second request while an attempt is pending returns the existing checkout URL rather than creating another payment.

`LocalSimulationPaymentProvider` is enabled only with `PAYMENT_LOCAL_SIMULATION_ENABLED=true` (local Docker Compose).
Its `/api/payments/local/**` callback exists solely for local development and must remain disabled in staging and
production. It is not a real payment provider and must never be exposed as one.

## Stripe test mode

Stripe uses its hosted Checkout page, so this application never receives card details. Configure test mode locally in
the ignored `.env` file:

```dotenv
PAYMENT_LOCAL_SIMULATION_ENABLED=false
PAYMENT_STRIPE_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

Only one online provider should be enabled at a time. For local webhook forwarding, run:

```powershell
stripe listen --forward-to http://localhost:18080/api/payments/stripe/webhook
```

The CLI prints a local `whsec_...` signing secret. It is distinct from any signing secret shown for a Stripe Dashboard
webhook endpoint and must be used while the CLI is forwarding events. The webhook verifies Stripe's signature against
the raw request body before it updates a payment attempt. Successful Checkout events confirm the reservation; the
browser return page is informational only.

### Stripe sandbox verification

Enable the relevant method for the hotel in the admin panel before testing it. The checkout session exposes only the
method selected by the guest.

- **Card:** `4242 4242 4242 4242`, any future expiry date and any three-digit CVC succeeds immediately.
- **MB WAY:** use the sandbox phone number `+351911111112` to simulate a successful approval after approximately
  15 seconds. The payment attempt remains pending until Stripe's signed webhook is received.
- **Multibanco:** completing Checkout produces a voucher rather than a normal browser return. In Stripe sandbox, an
  email such as `guest@example.com` simulates payment and the success webhook arrives after approximately three
  minutes. This mirrors the delayed confirmation of a real bank-transfer payment.

The Stripe CLI should listen for `checkout.session.completed`,
`checkout.session.async_payment_succeeded`, `checkout.session.async_payment_failed`,
`payment_intent.requires_action`, `payment_intent.succeeded`, and `payment_intent.payment_failed`.
The `payment_intent.requires_action` event supplies the Multibanco entity, reference, expiry, and hosted voucher URL.
Those details are persisted on the payment attempt and shown through the guest-access reservation page.

Stripe receives a randomly generated payment-attempt UUID in signed metadata solely for webhook correlation. It contains
no reservation ID, guest information, or payment details. The payment attempt is committed before Checkout is created,
so that metadata lets an immediate asynchronous event be matched safely.

1. Create a provider payment intent for a specific reservation and frozen amount.
2. Redirect or present the provider-hosted payment experience; card data must never enter this application.
3. Receive a signed provider webhook.
4. Verify webhook signature, make processing idempotent, and record the provider transaction reference.
5. Publish a reservation payment event through the existing transactional outbox.

## Provider decision deferred

Stripe is a practical primary choice because it supports cards, Multibanco, and MB WAY in Portugal. PayPal should be a
separate adapter. A Portuguese gateway such as Eupago or Ifthenpay can be added as a third adapter if its commercial
terms or local support are preferable. Provider API keys and webhook secrets belong in environment/secret storage,
never Git.

## Sandbox credentials needed later

- **Stripe:** publishable test key, secret test key, and webhook signing secret. Stripe can be the first adapter for
  cards, Multibanco, and MB WAY.
- **PayPal:** sandbox client ID, client secret, and its generated sandbox buyer and business accounts.
- **Portuguese gateway (optional):** test/API key and webhook/callback secret. Choose Eupago, Ifthenpay, or another
  provider only after comparing commercial terms and the hotel’s preferred settlement arrangement.

## Why no simulated card form

A local form that pretends to charge a card teaches the wrong security boundary. The simulator therefore uses a
separate development checkout page and callback; production uses a provider-hosted checkout page and signed webhooks.
