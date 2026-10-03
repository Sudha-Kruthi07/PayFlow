package PayFlow;

import org.springframework.stereotype.Component;

@Component
public class PaymentProviderSimulator {

    public PaymentProviderResponse processPayment(
            Long paymentId,
            int attemptNumber,
            String description) {

        // Used only for testing failure scenarios.
        if ("FORCE_FAILURE".equalsIgnoreCase(description)) {

            return new PaymentProviderResponse(
                    false,
                    null,
                    "SIMULATED_FAILURE",
                    "Payment provider rejected the payment"
            );
        }

        // Normal simulated successful payment.
        return new PaymentProviderResponse(
                true,
                "txn_" + paymentId + "_" + attemptNumber,
                null,
                null
        );
    }
}