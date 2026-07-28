# Room Type Ownership

## Decision

Room types belong to a hotel in the initial system.

## Reasoning

Although room types such as single, double, and suite appear standard, their names, descriptions, occupancy, amenities, pricing, and translations can differ between hotels. Hotel ownership keeps the model simple and makes configuration explicit.

## Future evolution

If the system later supports many hotels, a global catalog can be introduced:

```text
room_type_catalog
hotel_room_type
```

The catalog would contain reusable defaults, while the hotel-specific association would control availability, naming, pricing, translations, and whether that type is offered by a particular hotel.

That model is more scalable but is not justified for the current two-hotel scope.
