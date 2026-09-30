package PayFlow;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    int countByPaymentId(Long paymentId);
}