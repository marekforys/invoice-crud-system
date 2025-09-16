# Invoice Management System

A full-stack application for managing invoices with a Java/Spring Boot backend and React frontend.

## Features

### Backend (Java/Spring Boot)
- RESTful API for invoice management
- In-memory data storage (can be extended to use a database)
- Support for:
  - Creating and managing invoices
  - Adding line items to invoices
  - Recording payments
  - Viewing payment history
  - Searching invoices by customer name or item description

### Frontend (React)
- Modern, responsive user interface
- Real-time updates
- Intuitive invoice management
- Payment tracking and history

## Prerequisites

### Backend
- Java 17 or higher
- Gradle 7.6 or higher

### Frontend
- Node.js 16.x or higher
- npm 8.x or higher

## Getting Started

### Backend Setup

1. **Build the project**:
   ```bash
   ./gradlew build
   ```

2. **Run the backend server**:
   ```bash
   ./gradlew bootRun
   ```
   The API will be available at `http://localhost:8080`

### Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```
   The frontend will be available at `http://localhost:5173`

## API Endpoints

- `GET /api/invoices` - List all invoices
- `POST /api/invoices` - Create a new invoice
- `GET /api/invoices/{id}` - Get invoice by ID
- `POST /api/invoices/{id}/items` - Add item to invoice
- `POST /api/invoices/{id}/payments` - Record a payment
- `GET /api/invoices/search?q={query}` - Search invoices

## Project Structure

```
src/
  main/
    java/com/voris/invoice/
      model/                      # Domain models
        Invoice.java
        LineItem.java
        Payment.java
      repo/                       # Data access
        InvoiceRepository.java
        InMemoryInvoiceRepository.java
      service/                    # Business logic
        InvoiceService.java
      web/                        # Web layer
        InvoiceController.java
      InvoiceApplication.java     # Spring Boot application

frontend/
  public/                        # Static files
  src/
    components/                  # React components
    pages/                       # Page components
    services/                    # API service layer
    App.jsx                      # Main application component
    main.jsx                     # Entry point
```

## Development

### Running Tests

Backend tests:
```bash
./gradlew test
```

Frontend tests:
```bash
cd frontend
npm test
```

### Building for Production

Backend:
```bash
./gradlew bootJar
```

Frontend:
```bash
cd frontend
npm run build
```

## License

MIT