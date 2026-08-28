package pt.hotelbooking.booking.model.entity;

public enum PaymentMethod {
    PAY_AT_RECEPTION,
    CARD,
    PAYPAL,
    MULTIBANCO,
    MB_WAY;

    public boolean requiresOnlineProvider() {
        return this != PAY_AT_RECEPTION;
    }
}
