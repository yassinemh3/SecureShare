# 📦 SecureShare

> A secure, encrypted file-sharing platform with authentication and link-based sharing.

---

## 📄 Overview

**SecureShare** is a secure file upload, storage, and sharing web application built with **Spring Boot (Java)** for the backend and **React** for the frontend. It supports:

- User authentication (JWT)
- Encrypted file storage using AES
- File upload/download/delete functionality
- Secure shareable download links (with optional password and expiry)
- Role-based access (Admin/User)

---

## 🚀 Features

- 🔐 **User Authentication** (JWT with BCrypt hashing)
- 📤 **File Upload & Download**
- 🧹 **File Deletion with Authorization**
- 🗃️ **Encrypted File Storage (AES)**
- 🔗 **Token-based File Sharing with Optional Password**
- 👮‍♂️ **Role-based Access Control**

---

## 🧱 Tech Stack

- **Backend**: Java, Spring Boot, Spring Security, JPA, JWT, AES Encryption  
- **Frontend**: React, TailwindCSS, TypeScript, shadcn/ui
- **Database**: PostgreSQL


## ⚙️ Setup Instructions

### Backend (Spring Boot)

1. Clone repo and `cd src/`
2. Configure `application.yaml`:
3. Run the backend:

```bash
./mvnw spring-boot:run
```

### Frontend (React)

1. `cd secure-share-frontend/`
2. Install dependencies:

```bash
npm install
```

3. Run development server:

```bash
npm run dev
```

---

## 🔐 Authentication Flow

### User Entity

```java
public class User {
  private Long id;
  private String username;
  private String password; // BCrypt hash
  private String role; // ADMIN or USER
}
```

- JWT authentication used for protected endpoints.
- Passwords are hashed with BCrypt.
- Role-based restrictions using `@PreAuthorize` or config.

---

## 📁 File Management

- Files stored in `./uploads/` (or configured path).
- Metadata (filename, path, owner) saved in DB.
- AES used to encrypt before storing.
- Decryption on download.

### API Endpoints

| Method | Endpoint          | Description              |
|--------|-------------------|--------------------------|
| POST   | `/api/v1/files/upload`         | Upload a file            |
| GET    | `/api/v1/files/{id}`     | Download file            |
| DELETE | `/api/v1/files/{id}`     | Delete (if owner/admin)  |

---

## 🔗 Sharing Files

- Generates a UUID-based token and optional password
- Token expires after `X` minutes
- Download via:

```
/api/v1/share/access/{token}?password=optional
```

- Shared link shown in frontend like:

```
https://yourdomain.com/share/access/{token}
```

---
## 📸 Screenshots

### 🔐 Login Page

![Login Page](./assets/login.png)

---

### 🏠 Home Page

![Home Page](./assets/home.png)


## 🛡️ Security Notes

- AES encryption secures files at rest
- Only authenticated users can upload or manage files
- Shared links can be:
  - Expiry-based
  - Password-protected
- JWT secures user sessions

---

## 📄 License

This project is licensed under the MIT License. Feel free to use, fork, and enhance.
