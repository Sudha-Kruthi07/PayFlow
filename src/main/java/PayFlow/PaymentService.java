package PayFlow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentService {

    private static final int MAX_ATTEMPTS = 3;

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final UserRepository userRepository;
    private final PaymentProviderSimulator paymentProviderSimulator;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            UserRepository userRepository,
            PaymentProviderSimulator paymentProviderSimulator) {

        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.userRepository = userRepository;
        this.paymentProviderSimulator = paymentProviderSimulator;
    }

    public Payment createPayment(PaymentRequest request) {

        Payment existingPayment =
                paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                        .orElse(null);

        if (existingPayment != null) {
            return existingPayment;
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new UserNotFoundException(request.getUserId()));

        Payment payment = new Payment();

        payment.setUser(user);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setDescription(request.getDescription());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setStatus(PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment processPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId));

        // A successful payment must not be processed again
        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            return payment;
        }

        // A permanently failed payment must not be processed again
        if (PaymentStatus.PERMANENTLY_FAILED.equals(payment.getStatus())) {
            return payment;
        }

        int nextAttemptNumber =
                paymentAttemptRepository.countByPaymentId(paymentId) + 1;

        // Do not allow more than the maximum number of attempts
        if (nextAttemptNumber > MAX_ATTEMPTS) {
            return payment;
        }

        PaymentProviderResponse providerResponse =
                paymentProviderSimulator.processPayment(
                        paymentId,
                        nextAttemptNumber,
                        payment.getDescription()
                );

        PaymentAttempt attempt = new PaymentAttempt();

        attempt.setPayment(payment);
        attempt.setAttemptNumber(nextAttemptNumber);
        attempt.setProvider("PayFlow-Simulator");

        if (providerResponse.isSuccessful()) {

            attempt.setStatus("SUCCESS");

            attempt.setProviderTransactionId(
                    providerResponse.getTransactionId()
            );

            payment.setStatus(PaymentStatus.SUCCESS);

        } else {

            attempt.setStatus("FAILED");

            attempt.setFailureCode(
                    providerResponse.getFailureCode()
            );

            attempt.setFailureMessage(
                    providerResponse.getFailureMessage()
            );

            // If this was the final allowed attempt,
            // mark the payment as permanently failed.
            if (nextAttemptNumber == MAX_ATTEMPTS) {
                payment.setStatus(PaymentStatus.PERMANENTLY_FAILED);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
        }

        paymentAttemptRepository.save(attempt);

        return paymentRepository.save(payment);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new PaymentNotFoundException(id));
    }
}