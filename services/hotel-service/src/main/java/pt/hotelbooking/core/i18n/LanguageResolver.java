package pt.hotelbooking.core.i18n;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Locale;

@Component
public class LanguageResolver {

    private static final List<String> SUPPORTED_LANGUAGES = List.of(
            "en", "pt-PT", "es", "fr", "de"
    );

    @Value("${hotel.i18n.default-language:en}")
    private String defaultLanguage;

    public String resolve(String explicitLanguage, String acceptLanguage) {
        if (isSupported(explicitLanguage)) {
            return normalize(explicitLanguage);
        }

        if (acceptLanguage != null) {
            for (String requestedLanguage : acceptLanguage.split(",")) {
                String language = requestedLanguage.split(";", 2)[0].trim();

                if (isSupported(language)) {
                    return normalize(language);
                }
            }
        }

        return defaultLanguage;
    }

    private boolean isSupported(String language) {
        if (language == null || language.isBlank()) {
            return false;
        }

        return SUPPORTED_LANGUAGES.stream()
                .anyMatch(supported -> supported.equalsIgnoreCase(language.trim()));
    }

    private String normalize(String language) {
        return Locale.forLanguageTag(language.trim()).toLanguageTag();
    }
}
