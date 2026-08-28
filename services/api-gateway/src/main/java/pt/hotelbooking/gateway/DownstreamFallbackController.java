package pt.hotelbooking.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DownstreamFallbackController {

    @RequestMapping(value = "/fallback/{serviceName}", produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    public ResponseEntity<ProblemDetail> unavailable(@PathVariable String serviceName) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The requested service is temporarily unavailable. Please try again shortly.");
        problem.setTitle("Service temporarily unavailable");
        problem.setProperty("service", serviceName);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}
