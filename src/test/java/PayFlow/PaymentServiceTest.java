package PayFlow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentProviderSimulator paymentProviderSimulator;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                paymentAttemptRepository,
                userRepository,
                paymentProviderSimulator
        );
    }

    @Test
    void createPaymentShouldCreatePendingPayment() {

        PaymentRequest request = new PaymentRequest();

        request.setUserId(1L);
        request.setAmount(new BigDecimal("1000.00"));
        request.setCurrency("INR");
        request.setDescription("Test payment");
        request.setIdempotencyKey("test_key_001");

        User user = new User();

        when(paymentRepository.findByIdempotencyKey("test_key_001"))
                .thenReturn(Optional.empty());

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Payment savedPayment = new Payment();

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        Payment result = paymentService.createPayment(request);

        assertSame(savedPayment, result);

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository).save(paymentCaptor.capture());

        Payment capturedPayment = paymentCaptor.getValue();

        assertEquals(user, capturedPayment.getUser());
        assertEquals(
                new BigDecimal("1000.00"),
                capturedPayment.getAmount()
        );
        assertEquals("INR", capturedPayment.getCurrency());
        assertEquals("Test payment", capturedPayment.getDescription());
        assertEquals(
                "test_key_001",
                capturedPayment.getIdempotencyKey()
        );
        assertEquals(
                PaymentStatus.PENDING,
                capturedPayment.getStatus()
        );
    }

    @Test
    void createPaymentShouldReturnExistingPaymentForDuplicateIdempotencyKey() {

        Payment existingPayment = new Payment();

        when(paymentRepository.findByIdempotencyKey("duplicate_key"))
                .thenReturn(Optional.of(existingPayment));

        PaymentRequest request = new PaymentRequest();

        request.setUserId(1L);
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("INR");
        request.setDescription("Duplicate test");
        request.setIdempotencyKey("duplicate_key");

        Payment result = paymentService.createPayment(request);

        assertSame(existingPayment, result);

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(userRepository, never())
                .findById(anyLong());
    }

    @Test
    void processPaymentShouldMarkPaymentAsSuccessful() {

        Payment payment = new Payment();

        payment.setAmount(new BigDecimal("1000.00"));
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        when(paymentAttemptRepository.countByPaymentId(1L))
                .thenReturn(0);

        PaymentProviderResponse providerResponse =
                new PaymentProviderResponse(
                        true,
                        "txn_1_1",
                        null,
                        null
                );

        when(paymentProviderSimulator.processPayment(
                1L,
                1,
                null
        )).thenReturn(providerResponse);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        Payment result = paymentService.processPayment(1L);

        assertEquals(
                PaymentStatus.SUCCESS,
                result.getStatus()
        );

        verify(paymentAttemptRepository)
                .save(any(PaymentAttempt.class));

        verify(paymentRepository)
                .save(payment);
    }

    @Test
    void processPaymentShouldBecomePermanentlyFailedAfterThreeFailedAttempts() {

        Payment payment = new Payment();

        payment.setAmount(new BigDecimal("3000.00"));
        payment.setCurrency("INR");
        payment.setDescription("FORCE_FAILURE");
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findById(8L))
                .thenReturn(Optional.of(payment));

        when(paymentAttemptRepository.countByPaymentId(8L))
                .thenReturn(0, 1, 2);

        PaymentProviderResponse failedResponse =
                new PaymentProviderResponse(
                        false,
                        null,
                        "SIMULATED_FAILURE",
                        "Payment provider rejected the payment"
                );

        when(paymentProviderSimulator.processPayment(
                eq(8L),
                anyInt(),
                eq("FORCE_FAILURE")
        )).thenReturn(failedResponse);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        Payment firstResult =
                paymentService.processPayment(8L);

        assertEquals(
                PaymentStatus.FAILED,
                firstResult.getStatus()
        );

        Payment secondResult =
                paymentService.processPayment(8L);

        assertEquals(
                PaymentStatus.FAILED,
                secondResult.getStatus()
        );

        Payment thirdResult =
                paymentService.processPayment(8L);

        assertEquals(
                PaymentStatus.PERMANENTLY_FAILED,
                thirdResult.getStatus()
        );

        verify(paymentAttemptRepository, times(3))
                .save(any(PaymentAttempt.class));

        verify(paymentProviderSimulator, times(3))
                .processPayment(
                        eq(8L),
                        anyInt(),
                        eq("FORCE_FAILURE")
                );
    }

    @Test
    void permanentlyFailedPaymentShouldNotBeProcessedAgain() {

        Payment payment = new Payment();

        payment.setStatus(PaymentStatus.PERMANENTLY_FAILED);

        when(paymentRepository.findById(8L))
                .thenReturn(Optional.of(payment));

        Payment result =
                paymentService.processPayment(8L);

        assertEquals(
                PaymentStatus.PERMANENTLY_FAILED,
                result.getStatus()
        );

        verify(paymentProviderSimulator, never())
                .processPayment(
                        anyLong(),
                        anyInt(),
                        anyString()
                );

        verify(paymentAttemptRepository, never())
                .save(any(PaymentAttempt.class));

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void processPaymentShouldThrowExceptionWhenPaymentDoesNotExist() {

        when(paymentRepository.findById(9999L))
                .thenReturn(Optional.empty());

        assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.processPayment(9999L)
        );

        verify(paymentAttemptRepository, never())
                .save(any(PaymentAttempt.class));

        verify(paymentProviderSimulator, never())
                .processPayment(
                        anyLong(),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void createPaymentShouldThrowExceptionWhenUserDoesNotExist() {

        PaymentRequest request = new PaymentRequest();

        request.setUserId(9999L);
        request.setAmount(new BigDecimal("1000.00"));
        request.setCurrency("INR");
        request.setDescription("User test");
        request.setIdempotencyKey("user_test_001");

        when(paymentRepository.findByIdempotencyKey("user_test_001"))
                .thenReturn(Optional.empty());

        when(userRepository.findById(9999L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> paymentService.createPayment(request)
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }
}