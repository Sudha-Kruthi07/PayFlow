package PayFlow;

public class PaymentProviderResponse {

    private final boolean successful;
    private final String transactionId;
    private final String failureCode;
    private final String failureMessage;

    public PaymentProviderResponse(
            boolean successful,
            String transactionId,
            String failureCode,
            String failureMessage) {

        this.successful = successful;
        this.transactionId = transactionId;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
}