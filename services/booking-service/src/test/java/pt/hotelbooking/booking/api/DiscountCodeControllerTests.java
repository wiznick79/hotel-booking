package pt.hotelbooking.booking.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.hotelbooking.booking.service.DiscountCodeService;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiscountCodeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiscountCodeService discountCodeService;

    @Test
    void rejectsDiscountCodeWithoutDiscountValue() throws Exception {
        mockMvc.perform(post("/api/discount-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hotelId": "hotel-1",
                                  "code": "SUMMER",
                                  "validFrom": "2026-07-01",
                                  "validUntil": "2026-08-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void rejectsDiscountCodeWithTwoDiscountValues() throws Exception {
        mockMvc.perform(post("/api/discount-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hotelId": "hotel-1",
                                  "code": "SUMMER",
                                  "percentage": 10,
                                  "fixedAmount": 20,
                                  "validFrom": "2026-07-01",
                                  "validUntil": "2026-08-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void previewsAValidDiscountWithoutAuthentication() throws Exception {
        when(discountCodeService.preview(
                "hotel-1", "WELCOME10", BigDecimal.valueOf(200).setScale(2),
                java.time.LocalDate.of(2026, 10, 10)))
                .thenReturn(new DiscountCodeService.DiscountResult(
                        "WELCOME10", BigDecimal.valueOf(20), BigDecimal.valueOf(180)));

        mockMvc.perform(get("/api/discount-codes/validate")
                        .param("hotelId", "hotel-1")
                        .param("code", "WELCOME10")
                        .param("total", "200.00")
                        .param("stayDate", "2026-10-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WELCOME10"))
                .andExpect(jsonPath("$.originalTotal").value(200.0))
                .andExpect(jsonPath("$.discountAmount").value(20.0))
                .andExpect(jsonPath("$.finalTotal").value(180.0));
    }
}
