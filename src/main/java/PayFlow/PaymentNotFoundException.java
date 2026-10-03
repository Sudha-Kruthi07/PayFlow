package PayFlow;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long paymentId) {
        super("Payment with id " + paymentId + " was not found");
    }
}