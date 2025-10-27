package com.firefly.psps.exceptions;
public class PaymentFailedException extends PspException {
    public PaymentFailedException(String message) { super(message); }
    public PaymentFailedException(String message, String provider, String code) { super(message, provider, code); }
    public PaymentFailedException(String message, Throwable cause) { super(message, cause); }
}
