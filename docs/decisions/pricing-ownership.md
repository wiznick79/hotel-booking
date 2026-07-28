# Pricing Ownership

Room rates and pricing periods belong to `hotel-service`, alongside hotels and room types. The service owns seasonal, weekend, and holiday pricing rules.

`booking-service` requests a price calculation during reservation creation and stores the result as a snapshot. Later changes to rates must not alter existing reservations.

This keeps pricing rules in one service while keeping reservation history financially stable.
