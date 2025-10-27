package com.firefly.psps.exceptions;
public class PaymentNotFoundException extends PspException {
    public PaymentNotFoundException(String paymentId) { super("Payment not found: " + paymentId); }
}
