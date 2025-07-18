# 📦 SecureShare

> A secure, encrypted file-sharing platform with authentication and link-based sharing.

---

## 📄 Overview

**SecureShare** is a secure file upload, storage, and sharing web application built with **Spring Boot (Java)** for the backend and **React** for the frontend. It supports:

- User authentication (JWT)
- Encrypted file storage using AES-GCM encryption
- File upload/download/delete functionality
- Secure shareable download links and a QR Code (with optional password and expiry)
- Role-based access (Admin/User)
- Managing The Shared Links
- Zero-Knowledge Encryption for files before upload

---

## 🚀 Features

- 🔐 **User Authentication** (JWT with BCrypt hashing)
- 📤 **File Upload & Download**
- 🧹 **File Deletion with Authorization**
- 🗃️ **Encrypted File Storage (AES-GCM)**
- 🔗 **Token-based File Sharing with Optional Password**
- 👮‍♂️ **Role-based Access Control**
- 🔗 **QR Code Sharing**
- 🔗 **Shared Links Management**
- 🔐 **Zero-Knowledge Encryption**

---

## 🧱 Tech Stack

- **Backend**: Java, Spring Boot, Spring Security, JPA, JWT, AES-GCM Encryption, Zero-Knowledge Encryption
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
## 🔗 QR Code Sharing

SecureShare now supports **QR code-based file sharing**! 🚀

After generating a shareable link for your file, a **QR code** will also be displayed. This makes it easy to:

- Share files via mobile devices
- Print the QR code for physical distribution
- Use in presentations or offline scenarios

### 🛠️ How It Works

- Backend provides a public endpoint:  
  `GET /api/v1/share/qr/{token}`  
  Returns a PNG QR code image for the share token.

- Frontend fetches and displays the QR code next to the share link.

---
## 📜 View Shared Links
- List all active shared links with metadata:
  - File name
  - Expiration time
  - Password protection status
  - Share URL

### 🗑️ Revoke Access
- Instantly revoke any shared link
- Works for both password-protected and open links
- Automatic removal from the shared links list

---
## 🔐 Zero-Knowledge Encryption (ZKE)

SecureShare supports **Zero-Knowledge Encryption** for files before upload. This ensures files are encrypted entirely on the client side and the server never sees the original content or the passphrase.

### ✅ How to Use ZKE

1. Select a file using the upload form.
2. Toggle the **"Enable ZK Encryption"** switch.
3. Enter your **passphrase** in the input field.
4. Click **Upload**.

The selected file will be encrypted in your browser and uploaded with a `.enc` extension.

> ⚠️ **Important:** Keep your passphrase safe! Without it, the encrypted file **cannot** be decrypted — not even by the server.

---

### 🔓 Decryption

When downloading an encrypted file:

- You will be prompted to enter the decryption passphrase.
- If correct, the file will be decrypted locally in the browser.
- The decrypted version of the file will then be downloaded automatically.

---

## 📸 Screenshots

### 🔐 Login Page

![Login Page](./assets/login.png)

---

### 🏠 Home Page

![Home Page](./assets/home_pg.png)

---

### Share Link and QR Code

![Sharepage](./assets/Sharepage.png)

---

## 🛡️ Security Notes

- AES-GCM encryption secures files at rest
- Only authenticated users can upload or manage files
- Shared links can be:
  - Expiry-based
  - Password-protected
- JWT secures user sessions
- Zero-Knowledge Encryption for files before upload

---

## 📄 License

This project is licensed under the MIT License. Feel free to use, fork, and enhance.
