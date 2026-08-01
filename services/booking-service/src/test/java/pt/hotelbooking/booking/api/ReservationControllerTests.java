package pt.hotelbooking.booking.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.hotelbooking.booking.service.ReservationService;
import pt.hotelbooking.booking.model.dto.ReservationResponse;
import pt.hotelbooking.booking.model.dto.ReservationItemResponse;
import pt.hotelbooking.booking.model.entity.ReservationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void rejectsInvalidPublicReservationRequest() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":\"\",\"guestName\":\"\",\"guestPhone\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsGuestReservationFromAccessToken() throws Exception {
        when(reservationService.findByGuestAccessToken("token"))
                .thenReturn(response());

        mockMvc.perform(get("/api/reservations/guest/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestName").value("Guest"));
    }

    @Test
    @WithMockUser(username = "customer")
    void returnsCustomerHistory() throws Exception {
        when(reservationService.findMyReservations("customer"))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/reservations/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guestName").value("Guest"));

        verify(reservationService).findMyReservations(eq("customer"));
    }

    private ReservationResponse response() {
        return new ReservationResponse(
                UUID.randomUUID(), "hotel-1", "Guest", "+351000000000", null, 1,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12), null,
                ReservationStatus.PENDING, List.of(new ReservationItemResponse(
                UUID.randomUUID(), "room-type-1", null)), null, null, null,
                false, null, null, null);
    }
}
