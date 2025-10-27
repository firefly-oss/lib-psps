package com.firefly.psps.dtos.subscriptions;
import com.firefly.psps.domain.Money;
import lombok.*;
import java.time.Instant;
import java.util.List;
@Data @Builder public class InvoiceResponse {
    private String invoiceId;
    private String subscriptionId;
    private String customerId;
    private Money amountDue;
    private Money amountPaid;
    private String status;
    private Instant dueDate;
    private Instant paidAt;
    private List<InvoiceLineItem> lineItems;
    private Instant createdAt;
}
