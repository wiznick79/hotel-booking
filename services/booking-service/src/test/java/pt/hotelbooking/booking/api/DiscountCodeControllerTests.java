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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
