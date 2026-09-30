package PayFlow;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment-attempts")
public class PaymentAttemptController {

    private final PaymentAttemptService paymentAttemptService;

    public PaymentAttemptController(PaymentAttemptService paymentAttemptService) {
        this.paymentAttemptService = paymentAttemptService;
    }

    @PostMapping
    public PaymentAttempt createAttempt(@RequestBody PaymentAttempt attempt) {
        return paymentAttemptService.createAttempt(attempt);
    }

    @GetMapping
    public List<PaymentAttempt> getAllAttempts() {
        return paymentAttemptService.getAllAttempts();
    }

    @GetMapping("/{id}")
    public PaymentAttempt getAttemptById(@PathVariable Long id) {
        return paymentAttemptService.getAttemptById(id);
    }
}