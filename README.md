# invoice-crud-system
## Overview
Console-based Java CRUD application to manage invoices and their line items.

Features:
- Create invoices with multiple line items (description and price)
- Read/list and search invoices (by customer name or item description)
- Update by adding new line items to an existing invoice
- Delete is intentionally omitted

## Requirements
- Java 17+
- Gradle 7.6+ (or any recent Gradle installation)

## Build
```bash
gradle build
```

## Run
```bash
gradle run
```

## Run (via JAR)
```bash
java -jar build/libs/invoice-crud-system-1.0.0.jar
```

## Usage
When running, you’ll see a menu:
```
1) Create invoice
2) List all invoices
3) Search invoices
4) Add line item to existing invoice
0) Exit
```

Notes:
- Invoice IDs are auto-generated (UUID). Copy an ID from list/search to add new items to that invoice.
- Prices use decimal numbers (e.g., 100, 99.95).

## Project Structure
```
src/main/java/com/voris/invoice/
  App.java                         # Console entry point
  model/                           # Domain models
    Invoice.java
    LineItem.java
  repo/                            # Repository interfaces/impls
    InvoiceRepository.java
    InMemoryInvoiceRepository.java
  service/                         # Business logic
    InvoiceService.java
```

## License
MIT