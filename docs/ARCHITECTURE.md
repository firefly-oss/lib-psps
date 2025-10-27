# Architecture

This document describes the hexagonal architecture of the Firefly PSP library.

## Hexagonal Architecture

The library implements **Ports and Adapters** pattern to separate business logic from external PSP integrations.

### Core Concepts

**Ports**: Interfaces defining capabilities  
**Adapters**: PSP-specific implementations  
**Domain**: Business logic independent of external systems

```
┌─────────────────────────────────────────┐
│       Application Layer                 │
│  (Business Logic & Use Cases)           │
└──────────────┬──────────────────────────┘
               │ depends on
               ▼
┌─────────────────────────────────────────┐
│       Domain Layer (lib-psps)           │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │ Ports (Interfaces)               │  │
│  │  • PspAdapter                    │  │
│  │  • PaymentPort, RefundPort       │  │
│  │  • SubscriptionPort, etc.        │  │
│  └──────────────────────────────────┘  │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │ Domain Models                    │  │
│  │  • Money, Currency               │  │
│  │  • PaymentStatus, etc.           │  │
│  └──────────────────────────────────┘  │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │ DTOs                             │  │
│  │  • Request/Response objects      │  │
│  └──────────────────────────────────┘  │
└──────────────┬──────────────────────────┘
               │ implemented by
               ▼
┌─────────────────────────────────────────┐
│    Infrastructure (Implementations)     │
│                                         │
│  ┌───────────┐  ┌───────────┐         │
│  │  Stripe   │  │   Adyen   │  ...    │
│  │  Adapter  │  │  Adapter  │         │
│  └───────────┘  └───────────┘         │
└─────────────────────────────────────────┘
```

## Port Interfaces

### 1. PspAdapter
Main entry point providing access to all ports.

### 2. PaymentPort
Direct payment processing: create, confirm, capture, cancel.

### 3. RefundPort
Refund management: create, get, list refunds.

### 4. PayoutPort
Fund transfers to external accounts.

### 5. CustomerPort
Customer CRUD and payment method management.

### 6. SubscriptionPort
Recurring billing: plans, subscriptions, invoices.

### 7. CheckoutPort
Hosted checkout sessions and payment intents for redirect/client-side flows.

### 8. WebhookPort
Webhook signature verification and event parsing.

### 9. DisputePort
Chargeback and dispute handling.

### 10. ProviderSpecificPort
Extensibility for PSP-unique features (e.g., Stripe Connect, Adyen split payments).

## Design Patterns

### Hexagonal Architecture (Ports & Adapters)
- **Core domain** independent of PSPs
- **Ports** define contracts
- **Adapters** implement for specific PSPs

### Repository Pattern
Each port acts as a repository for its domain.

### Strategy Pattern
Different PSP implementations are swappable strategies.

### Registry Pattern
Provider-specific operations use a registry for dynamic discovery.

## Package Structure

```
com.firefly.psps
├── adapter/
│   ├── PspAdapter.java
│   └── ports/
│       ├── PaymentPort.java
│       ├── SubscriptionPort.java
│       ├── CheckoutPort.java
│       └── ...
├── services/
│   └── AbstractPspService.java
├── controllers/
│   ├── AbstractPaymentController.java
│   ├── AbstractSubscriptionController.java
│   └── AbstractWebhookController.java
├── domain/
│   ├── Money.java
│   ├── Currency.java
│   └── ...
├── dtos/
│   ├── payments/
│   ├── subscriptions/
│   ├── checkout/
│   └── ...
├── exceptions/
│   └── PspException.java
└── config/
    └── PspProperties.java
```

## Data Flow Example

### Payment Creation

```
1. HTTP Request → Controller
2. Controller → AbstractPspService
3. Service → PspAdapter.payments()
4. PaymentPort → Implementation (e.g., StripePaymentPort)
5. Implementation maps request → Calls Stripe API
6. Maps response back → Returns PaymentResponse
7. Response → Client
```

## Benefits

### Testability
Easy to mock PSP interactions in tests.

### Flexibility
Switch PSPs without code changes.

### Maintainability
Clear separation of concerns.

### Scalability
Add new PSPs independently.

### Consistency
Unified API regardless of PSP.

## Dependency Rules

**Application → Domain (Ports) ← Infrastructure (Adapters)**

Rules:
1. ✅ Application depends on Domain
2. ✅ Infrastructure depends on Domain
3. ❌ Domain NEVER depends on Infrastructure
4. ❌ Domain NEVER depends on Application

This ensures the domain remains independent and portable.
