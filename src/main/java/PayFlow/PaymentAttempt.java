package PayFlow;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "payment_attempts",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"payment_id", "attempt_number"})
    }
)
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 50)
    private String provider;

    @Column(length = 100)
    private String providerTransactionId;

    @Column(length = 50)
    private String failureCode;

    @Column(length = 255)
    private String failureMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public PaymentAttempt() {
    }

    public Long getId() {
        return id;
    }

    public Payment getPayment() {
        return payment;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setProviderTransactionId(String providerTransactionId) {
        this.providerTransactionId = providerTransactionId;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}