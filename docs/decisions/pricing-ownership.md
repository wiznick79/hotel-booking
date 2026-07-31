# Pricing Ownership

Room rates and pricing rules belong to `hotel-service`, alongside hotels and room types. Rules are owned by a hotel and may define prices for multiple room types. The service owns recurring seasonal, weekend, and one-off holiday override rules.

`booking-service` requests a price calculation during reservation creation and stores the result as a snapshot. Later changes to rates must not alter existing reservations.

This keeps pricing rules in one service while keeping reservation history financially stable.
