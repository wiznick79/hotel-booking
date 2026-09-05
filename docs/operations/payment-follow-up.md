# Payment follow-up

Payment status records the provider's result independently of reservation status.
A successful payment received after cancellation or hold expiry is retained as
`SUCCEEDED`; the reservation remains `CANCELLED` because its room may have been sold.
The reservation API exposes `paymentReviewRequired`, and the admin reservation
list/details show **Paid · [method] · Review required (cancelled booking)**.

Staff should inspect the provider payment and contact the guest to agree on a
refund or a new booking after checking availability. A refund is not automatically
issued by this workflow. Refund execution and reconciliation are still pending.

Success supersedes a local expiry or earlier failure. Duplicate success and late
failure notifications do not undo a recorded payment. Payment for an already
confirmed, checked-in or completed reservation does not repeat confirmation.
The payment handler checks the hold deadline even before the expiry job runs.
A successful older attempt takes precedence over a newer unpaid retry in the
reservation payment summary.

The local payment simulator follows the same reservation rules. Regression tests
in `PaymentServiceTests` cover late success, cancellation, expired holds, manual
confirmation, event repetition/order, and simulator behavior.
