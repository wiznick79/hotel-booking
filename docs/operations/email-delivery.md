# Transactional email delivery

The notification service always uses the `EmailSender` abstraction. Local Docker
development defaults to Mailpit, which safely captures emails without delivering them
to external recipients. Staging can switch to a real SMTP provider without any code
change by supplying SMTP settings through AWS Systems Manager Parameter Store.

## Recommended staging provider: Amazon SES

Use Amazon SES in the same `eu-west-3` Region as the staging host. Verify the
`wiznick.net` domain as an SES identity and enable DKIM. SES will provide DNS records;
add those records at the domain's DNS provider before configuring the application.

New SES accounts begin in the SES sandbox. While in that sandbox, both the sender and
every recipient must be verified. This is enough to test the booking-confirmation flow
with your own email address. Request production access only when the hotel is ready to
send to arbitrary guest addresses. [AWS SES identity verification](https://docs.aws.amazon.com/ses/latest/dg/verify-addresses-and-domains.html)
and [sandbox guidance](https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html)
describe these requirements.

Create SES SMTP credentials, not an IAM access key. Store the generated SMTP password
only in Parameter Store as a `SecureString`; it cannot be retrieved from SES later.

## Staging parameter values for SES

Create the following parameters under `/hotel-booking/staging/`:

| Parameter suffix | Type | SES value |
| --- | --- | --- |
| `notification-email-from` | `String` | A verified address, for example `bookings@wiznick.net` |
| `notification-email-smtp-host` | `String` | `email-smtp.eu-west-3.amazonaws.com` |
| `notification-email-smtp-port` | `String` | `587` |
| `notification-email-smtp-username` | `String` | Generated SES SMTP username |
| `notification-email-smtp-password` | `SecureString` | Generated SES SMTP password |
| `notification-email-smtp-auth` | `String` | `true` |
| `notification-email-smtp-starttls-enable` | `String` | `true` |
| `notification-email-smtp-starttls-required` | `String` | `true` |

Then run the normal staging deployment workflow. `start-stack.sh` reads the values into
a root-only runtime environment file, and Compose passes them only to
`notification-service`. Do not place the SMTP password in GitHub Actions secrets,
Docker Compose files, source code, or `.env.example`.

## Verification

1. In SES sandbox mode, verify the sender identity and the test recipient.
2. Create a public booking using that test recipient address.
3. Confirm that the notification is marked `SENT` in the admin panel and that the email
   contains the guest-access link.
4. Open the link in a private browser window and confirm that it reaches only that
   reservation.

If SMTP is unavailable, the notification service keeps the notification for retry and
eventually marks it `FAILED`; staff can inspect and replay terminal failures from the
admin panel.
