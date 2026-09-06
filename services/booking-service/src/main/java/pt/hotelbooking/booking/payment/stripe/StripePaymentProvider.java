package pt.hotelbooking.booking.payment.stripe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pt.hotelbooking.booking.model.entity.PaymentMethod;
import pt.hotelbooking.booking.payment.PaymentInitiation;
import pt.hotelbooking.booking.payment.PaymentInitiationRequest;
import pt.hotelbooking.booking.payment.PaymentInstructions;
import pt.hotelbooking.booking.payment.PaymentProvider;
import pt.hotelbooking.booking.payment.PaymentProviderType;
import pt.hotelbooking.booking.payment.PaymentWebhook;
import pt.hotelbooking.booking.payment.PaymentWebhookResult;

@Component
@ConditionalOnProperty(name = "payment.stripe.enabled", havingValue = "true")
public class StripePaymentProvider implements PaymentProvider {
    private static final EnumSet<PaymentMethod> SUPPORTED_METHODS = EnumSet.of(
            PaymentMethod.CARD,
            PaymentMethod.MULTIBANCO,
            PaymentMethod.MB_WAY);

    private final StripeClient stripeClient;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;
    private final String publicFrontendBaseUrl;
    private final StripeResilience stripeResilience;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public StripePaymentProvider(
            ObjectMapper objectMapper,
            @Value("${payment.stripe.secret-key}") String secretKey,
            @Value("${payment.stripe.webhook-secret}") String webhookSecret,
            @Value("${payment.stripe.public-frontend-base-url}") String publicFrontendBaseUrl,
            @Value("${payment.stripe.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${payment.stripe.read-timeout-ms:5000}") int readTimeoutMs,
            StripeResilience stripeResilience) {
        if (secretKey.isBlank() || webhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe requires a secret key and webhook signing secret.");
        }

        this.stripeClient = new StripeClient(secretKey);
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
        this.publicFrontendBaseUrl = publicFrontendBaseUrl.replaceAll("/$", "");
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.stripeResilience = stripeResilience;
    }

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.STRIPE;
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return SUPPORTED_METHODS.contains(paymentMethod);
    }

    @Override
    public PaymentInitiation initiate(PaymentInitiationRequest request) {
        try {
            SessionCreateParams.Builder parameters = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setIntegrationIdentifier("hotel_booking_qmxnrvta")
                    .setSuccessUrl(publicFrontendBaseUrl + "/#/payment/stripe/success")
                    .setCancelUrl(publicFrontendBaseUrl + "/#/book")
                    .addPaymentMethodType(toStripePaymentMethod(request.paymentMethod()))
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(request.currency().toLowerCase())
                                    .setUnitAmount(toMinorUnitAmount(request.amount()))
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Hotel stay")
                                            .build())
                                    .build())
                            .build())
                    .putMetadata("paymentAttemptId", request.paymentAttemptId().toString())
                    .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                            .putMetadata("paymentAttemptId", request.paymentAttemptId().toString())
                            .build());

            if (request.guestEmail() != null && !request.guestEmail().isBlank()) {
                parameters.setCustomerEmail(request.guestEmail());
            }

            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey("hotel-booking-checkout-" + request.paymentAttemptId())
                    .setConnectTimeout(connectTimeoutMs)
                    .setReadTimeout(readTimeoutMs)
                    .setMaxNetworkRetries(0)
                    .build();
            Session session = stripeResilience.execute(() -> {
                try {
                    return stripeClient.v1().checkout().sessions().create(parameters.build(), requestOptions);
                } catch (Exception exception) {
                    throw new StripeServiceUnavailableException("Stripe checkout session creation failed.", exception);
                }
            });

            return new PaymentInitiation(
                    providerType(), session.getId(), session.getPaymentIntent(), session.getUrl(), session.getMetadata());
        } catch (Exception exception) {
            throw new IllegalStateException("Stripe could not create the checkout session.", exception);
        }
    }

    @Override
    public PaymentWebhookResult verifyWebhook(PaymentWebhook webhook) {
        try {
            Event event = Webhook.constructEvent(webhook.payload(), webhook.signature(), webhookSecret);
            JsonNode payload = objectMapper.readTree(webhook.payload());
            JsonNode paymentObject = payload.path("data").path("object");

            if (event.getType().startsWith("payment_intent.")) {
                return new PaymentWebhookResult(
                        null,
                        paymentObject.path("id").asText(),
                        readPaymentAttemptId(paymentObject),
                        toPaymentOutcome(event.getType(), paymentObject.path("status").asText()),
                        toMultibancoInstructions(paymentObject));
            }

            return new PaymentWebhookResult(
                    paymentObject.path("id").asText(),
                    paymentObject.path("payment_intent").asText(null),
                    readPaymentAttemptId(paymentObject),
                    toPaymentOutcome(event.getType(), paymentObject.path("payment_status").asText()),
                    null);
        } catch (SignatureVerificationException exception) {
            throw new IllegalArgumentException("Stripe webhook signature is invalid.", exception);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Stripe webhook could not be processed.", exception);
        }
    }

    private SessionCreateParams.PaymentMethodType toStripePaymentMethod(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case CARD -> SessionCreateParams.PaymentMethodType.CARD;
            case MULTIBANCO -> SessionCreateParams.PaymentMethodType.MULTIBANCO;
            case MB_WAY -> SessionCreateParams.PaymentMethodType.MB_WAY;
            default -> throw new IllegalArgumentException("Stripe does not support " + paymentMethod + ".");
        };
    }

    private long toMinorUnitAmount(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    private PaymentWebhookResult.PaymentOutcome toPaymentOutcome(String eventType, String paymentStatus) {
        return switch (eventType) {
            case "checkout.session.completed" -> "paid".equals(paymentStatus)
                    ? PaymentWebhookResult.PaymentOutcome.SUCCEEDED
                    : PaymentWebhookResult.PaymentOutcome.PENDING;
            case "checkout.session.async_payment_succeeded", "payment_intent.succeeded" ->
                    PaymentWebhookResult.PaymentOutcome.SUCCEEDED;
            case "checkout.session.async_payment_failed", "payment_intent.payment_failed" ->
                    PaymentWebhookResult.PaymentOutcome.FAILED;
            default -> PaymentWebhookResult.PaymentOutcome.PENDING;
        };
    }

    private PaymentInstructions toMultibancoInstructions(JsonNode paymentIntent) {
        JsonNode details = paymentIntent.path("next_action").path("multibanco_display_details");

        if (details.isMissingNode()) {
            return null;
        }

        long expiresAt = details.path("expires_at").asLong(0);
        return new PaymentInstructions(
                details.path("entity").asText(null),
                details.path("reference").asText(null),
                details.path("hosted_voucher_url").asText(null),
                expiresAt == 0 ? null : Instant.ofEpochSecond(expiresAt));
    }

    private UUID readPaymentAttemptId(JsonNode paymentObject) {
        String paymentAttemptId = paymentObject.path("metadata").path("paymentAttemptId").asText(null);

        if (paymentAttemptId == null || paymentAttemptId.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(paymentAttemptId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
