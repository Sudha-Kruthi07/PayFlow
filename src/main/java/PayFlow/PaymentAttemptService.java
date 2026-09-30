package PayFlow;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentAttemptService {

    private final PaymentAttemptRepository paymentAttemptRepository;

    public PaymentAttemptService(PaymentAttemptRepository paymentAttemptRepository) {
        this.paymentAttemptRepository = paymentAttemptRepository;
    }

    public PaymentAttempt createAttempt(PaymentAttempt attempt) {

        int existingAttempts =
                paymentAttemptRepository.countByPaymentId(attempt.getPayment().getId());

        attempt.setAttemptNumber(existingAttempts + 1);

        return paymentAttemptRepository.save(attempt);
    }

    public List<PaymentAttempt> getAllAttempts() {
        return paymentAttemptRepository.findAll();
    }

    public PaymentAttempt getAttemptById(Long id) {
        return paymentAttemptRepository.findById(id).orElse(null);
    }
}