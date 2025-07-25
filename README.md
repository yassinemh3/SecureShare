# 📦 SecureShare

> A secure, encrypted file-sharing platform with authentication, link-based sharing, and AI-powered file summarization.

---

## 📄 Overview

**SecureShare** is a secure file upload, storage, and sharing web application built with **Spring Boot (Java)** and **React**. It emphasizes end-to-end security and user convenience, featuring:

- 🔐 **User authentication (JWT)**
- 🔒 **Encrypted file storage using AES-GCM**
- 📤 **File upload, download, and deletion**
- 🔗 **Secure shareable links with optional password and expiry**
- 📱 **QR Code generation for shared links**
- 🧠 **AI-Powered file summarization (via Hugging Face model: `facebook/bart-large-cnn`)**
- 🔎 **Search files by name or metadata**
- 🧑‍💼 **Role-based access control (Admin/User)**
- 💡 **Zero-Knowledge Encryption (ZKE) on client side**
- 🔧 **Shared link management and revocation**

## 🚀 Key Features

### 🔐 Authentication & Roles

- Secure **JWT**-based login with **BCrypt**-hashed passwords.
- **Admin/User** role-based access control using `@PreAuthorize`.

### 📁 File Management

- Upload, download, and delete files.
- Stored securely on the server with **AES-GCM** encryption.
- Metadata (e.g. filename, user, encrypted status) stored in **PostgreSQL**.
- **Search files** by name or metadata in the dashboard.

### 🧠 AI Summary (NEW)

- Uses Hugging Face's `facebook/bart-large-cnn` for file summarization.
- Automatically generates summaries for supported files (e.g., `.txt`, `.pdf`).
- Summaries are displayed in the frontend after file upload or on request.

> ✨ This enables quick insights into large documents without needing to download and read the entire content.

### 🔗 Secure Sharing

- Create unique, tokenized share links (UUID-based).
- Optional:
  - Password protection
  - Expiry time
- API Access:  
  `GET /api/v1/share/access/{token}?password=optional`

### 📱 QR Code Sharing

- Automatically generates a **QR code** for each shared file link.
- Endpoint:  
  `GET /api/v1/share/qr/{token}`
- Ideal for mobile access, print materials, and presentations.

### 🔐 Zero-Knowledge Encryption (ZKE)

- Encrypts files **client-side** before upload.
- The server **never** sees the original file content or passphrase.
- Files are uploaded with a `.enc` extension.

> ⚠️ Keep your passphrase secure — it is **required** for decryption and cannot be recovered.

---

## 🧱 Tech Stack

| Layer        | Tech                                                   |
|--------------|--------------------------------------------------------|
| **Frontend** | React, TailwindCSS, TypeScript, shadcn/ui              |
| **Backend**  | Java, Spring Boot, Spring Security, JWT, JPA           |
| **Database** | PostgreSQL                                             |
| **Security** | AES-GCM, BCrypt, Zero-Knowledge Encryption             |
| **AI**       | Hugging Face Transformers: `facebook/bart-large-cnn`  |

---

Here's that entire section rewritten in proper `README.md` Markdown format, ready to paste directly into your `README.md` file:

---

## ⚙️ Setup Instructions

### 🔧 Backend (Spring Boot)

1. Configure your `application.yaml` file with:
   - PostgreSQL database connection
   - JWT secret and expiration settings
   - Encryption key (for AES)

2. Start the backend server:

```bash
./mvnw spring-boot:run
````

---

### 💻 Frontend (React)

```bash
cd secure-share-frontend/
npm install
npm run dev
```

---

## 🔎 File Search

* Quickly search files uploaded by the user.
* Search is **real-time** and supports **partial matches**.
* Admins can search across **all users' files**.

---

## 🧠 AI-Powered Summary

* After uploading a file (`.txt`, `.pdf`), click **"Generate Summary"**.
* Summary is powered by a Hugging Face model: [`facebook/bart-large-cnn`](https://huggingface.co/facebook/bart-large-cnn)
* Helps users understand long content **at a glance**.

---

## 🔗 API Endpoints (Core)

| Method | Endpoint                       | Description                      |
| ------ | ------------------------------ | -------------------------------- |
| POST   | `/api/v1/files/upload`         | Upload a file                    |
| GET    | `/api/v1/files/{id}`           | Download a file                  |
| DELETE | `/api/v1/files/{id}`           | Delete (owner/admin only)        |
| GET    | `/api/v1/share/access/{token}` | Access shared file via token     |
| GET    | `/api/v1/share/qr/{token}`     | Get QR code for shared file link |
| POST   | `/api/v1/ai/summary`           | Get AI-generated file summary    |

---

## 🗂️ Manage Shared Links

* View all your **active shareable links**:

  * ✅ Filename
  * ⏱️ Expiry date
  * 🔐 Password protection status
  * 🔗 Direct access URL
* Instantly **revoke any shared link** with one click.

---

## 🔓 Decryption (ZKE)

* When downloading a `.enc` file, the user is prompted for a **passphrase**.
* If the passphrase is correct:

  * File is **decrypted in-browser**
  * Then **automatically downloaded**

> ⚠️ Without the correct passphrase, decryption is not possible — even by the server.

---

## 📸 Screenshots

### 🔐 Login Page

![Login Page](./assets/login.png)

### 🏠 Home Page

![Home Page](./assets/home_pg.png)

### 📤 Share Link & QR Code

![Sharepage](./assets/Sharepage.png)

---

## 🛡️ Security Notes

* 🔐 **AES-GCM** encryption secures files at rest
* 🔑 **BCrypt** password hashing for safe credential storage
* 🧾 **JWT** secures user authentication
* 👥 **Role-based access controls** (Admin/User)
* 📦 **Optional password + expiration** on shared links
* 🧠 **Zero-Knowledge Encryption** ensures true client-side privacy

