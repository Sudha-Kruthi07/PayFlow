package PayFlow;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;

    public PaymentService(PaymentRepository paymentRepository,PaymentAttemptRepository paymentAttemptRepository) {

            this.paymentRepository = paymentRepository;
            this.paymentAttemptRepository = paymentAttemptRepository;
    }

    public Payment createPayment(Payment payment) {

        Payment existingPayment =
                paymentRepository.findByIdempotencyKey(payment.getIdempotencyKey())
                        .orElse(null);

        if (existingPayment != null) {
            return existingPayment;
        }

        return paymentRepository.save(payment);
    }

    public Payment processPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElse(null);

        if (payment == null) {
            return null;
        }

        int nextAttemptNumber =
                paymentAttemptRepository.countByPaymentId(paymentId) + 1;

        PaymentAttempt attempt = new PaymentAttempt();

        attempt.setPayment(payment);
        attempt.setAttemptNumber(nextAttemptNumber);
        attempt.setStatus("SUCCESS");
        attempt.setProvider("PayFlow-Simulator");
        attempt.setProviderTransactionId(
                "txn_" + paymentId + "_" + nextAttemptNumber
        );

        paymentAttemptRepository.save(attempt);

        payment.setStatus("SUCCESS");

        return paymentRepository.save(payment);
    }
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id).orElse(null);
    }
}