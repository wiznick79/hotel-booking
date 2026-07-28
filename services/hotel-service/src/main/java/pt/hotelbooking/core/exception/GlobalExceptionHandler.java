package pt.hotelbooking.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pt.hotelbooking.hotel.exception.HotelNotFoundException;
import pt.hotelbooking.hotel.exception.RoomTypeNotFoundException;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(HotelNotFoundException.class)
    public ProblemDetail handleHotelNotFound(HotelNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Hotel not found");
        problem.setType(URI.create("https://hotel-booking.local/problems/hotel-not-found"));
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(RoomTypeNotFoundException.class)
    public ProblemDetail handleRoomTypeNotFound(RoomTypeNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Room type not found");
        problem.setType(URI.create("https://hotel-booking.local/problems/room-type-not-found"));
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage())).toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more request fields are invalid.");
        problem.setTitle("Validation failed");
        problem.setType(URI.create("https://hotel-booking.local/problems/validation-failed"));
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("errors", errors);
        return problem;
    }

    private record ValidationError(String field, String message) {}
}
