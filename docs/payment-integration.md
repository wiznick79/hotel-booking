# Future Payment Integration

The current booking flow deliberately supports **pay at reception**. It does not collect card data and no payment
provider credentials are configured. This keeps the initial project secure and usable while avoiding a misleading
fake payment integration.

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

`PaymentProvider` supports initiation and signed-webhook verification. `PaymentProviderRegistry` chooses a provider
according to the requested method. No concrete provider is configured yet, so the system cannot accidentally treat a
real online payment as complete.

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

A local form that pretends to charge a card teaches the wrong security boundary. Local tests can instead use a fake
implementation of the future provider interface, while production uses a provider-hosted checkout page and signed
webhooks.
