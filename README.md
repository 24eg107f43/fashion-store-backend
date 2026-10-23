# StyleHub — Basic Fashion Store

Beginner-friendly Java + Spring Boot + Thymeleaf + MySQL project. The pages use HTML/CSS templates (no React and no custom JavaScript).

## Features
- Six sample products inserted automatically on first run
- Sign up and log in (passwords stored as BCrypt hashes)
- Shopping cart: add, change quantity, remove items
- Checkout with delivery address (demo only; no real payment)
- Order history saved in MySQL
- Stock reduces when an order is placed

## Requirements
- JDK 21
- Maven 3.9+
- MySQL Server 8.x
- VS Code extensions: Extension Pack for Java, Spring Boot Extension Pack, Extension Pack for HTML. Optional: Database Client/JDBC.

## 1. Create the database
In MySQL Command Line Client, run:
```sql
CREATE DATABASE IF NOT EXISTS fashion_store;
```
Tables are created automatically by Spring Boot.

## 2. Set your MySQL password
Open `src/main/resources/application.properties` and replace `CHANGE_ME` with your actual MySQL root password. If the MySQL user has no password, use `spring.datasource.password=`.
Do not share your password or commit it to a public repository.

## 3. Run the backend
Open the `fashion-store-backend` folder in VS Code. Open Terminal > New Terminal and run:
```bash
mvn spring-boot:run
```
Wait for `Started FashionStoreApplication`, then open http://localhost:8080. Stop with Ctrl+C.

## Frontend folder
The separate `fashion-store-frontend` ZIP contains the same templates and CSS for viewing/editing. The running app serves pages from `src/main/resources/templates` and `src/main/resources/static` inside the backend ZIP. Edit those backend copies to see changes immediately.

## Important
This is a learning/demo project, not production-ready. Checkout is simulated and does not take payment.
