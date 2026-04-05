# Finance Dashboard Backend

This project is a backend system built for managing financial data with role-based access control. It supports different types of users who can interact with financial records based on their permissions.

---

## Tech Stack

* Java 17
* Spring Boot
* Spring Security (JWT Authentication)
* MySQL
* Maven

---

## Features

* User authentication using JWT
* Role-based access control (ADMIN, MANAGER, EMPLOYEE)
* Expense and category management
* Create, update, delete, and view financial records
* Dashboard APIs for summary data
* Input validation and error handling

---

## User Roles

* **ADMIN** → Full access to all operations
* **MANAGER** → Can review and access financial data
* **EMPLOYEE** → Can create and view records

---

## Authentication Flow

1. User logs in using email and password
2. Backend generates a JWT token
3. Token is sent in the Authorization header
4. Backend validates the token before processing requests

---

## API Endpoints

### Auth

* POST /api/auth/login

### Categories

* POST /api/categories
* GET /api/categories

### Expenses

* POST /api/expenses
* GET /api/expenses

### Dashboard

* APIs for total income, expenses, and summaries

---

## API Testing

You can test the APIs using:

* Postman
* Swagger UI

Swagger URL (local):
http://localhost:8081/swagger-ui/index.html

---

## How to Run

1. Clone the repository

2. Configure MySQL in application.properties

3. Create database:

   CREATE DATABASE finance_dashboard;

4. Run the application:

   mvn spring-boot:run

---

## Notes

* Default users and categories are automatically created when the application starts
* Passwords are stored securely using encryption
* Access control is enforced based on user roles

---

## Assumptions

* Each user has a fixed role
* Only authenticated users can access protected APIs
* Data is stored in MySQL

---
