# UriPET — Backend para gestión y cuidado colaborativo de mascotas

## Curso

CS2031 — Desarrollo Basado en Plataforma

## Integrantes

* [mauricio.galindo@utec.edu.pe](mailto:mauricio.galindo@utec.edu.pe)
* [adriana.matumay@utec.edu.pe](mailto:adriana.matumay@utec.edu.pe)

---

## 1. Introducción

UriPET es un backend desarrollado con Spring Boot que centraliza la gestión de mascotas, responsables y registros de salud. Permite autenticación segura mediante JWT, acceso mediante QR y almacenamiento de imágenes en la nube.

---

## 2. Problema

La información de mascotas suele estar dispersa (WhatsApp, fotos, notas), dificultando la coordinación entre responsables y el acceso rápido a datos en emergencias.

---

## 3. Solución

UriPET implementa una API REST que permite:

* Registro e inicio de sesión con JWT
* Verificación por correo
* Gestión de mascotas
* Gestión de responsables
* Registros de salud
* Login mediante QR
* Almacenamiento de imágenes (AWS S3)
* Notificaciones en tiempo real (WebSockets)

---

## 4. Tecnologías

* Java 21
* Spring Boot 4
* Spring Security
* Spring Data JPA
* PostgreSQL
* JWT
* Swagger (Springdoc OpenAPI)
* AWS S3
* Docker & Docker Compose

---

## 5. Arquitectura

El backend sigue arquitectura por capas:

* Controller
* Service
* Repository
* DTO
* Domain
* Config
* Events

---

## 6. API REST

### Autenticación

* POST `/auth/register`
* POST `/auth/login`
* POST `/auth/verify`
* POST `/auth/resend-verification`

### Usuarios

* GET `/user/me`
* PATCH `/user/me`
* DELETE `/user/me`

### Mascotas

* POST `/pets`
* GET `/pets/{pid}`
* PATCH `/pets/{pid}`
* GET `/pets/me`

---

## 7. Seguridad

* Autenticación con JWT
* Contraseñas cifradas con BCrypt
* Protección de endpoints con Spring Security
* Uso de variables de entorno (`.env`)

---

## 8. Ejecución local

### Requisitos

* Docker y Docker Compose

---

### Variables de entorno

Crear un archivo `.env` basado en `.env.example`:

```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=uripet

SPRING_DATASOURCE_URL=jdbc:postgresql://uripet-db:5432/uripet
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

JWT_SECRET=change_me
JWT_EXPIRATION=3600

MAIL_USERNAME=example@mail.com
MAIL_PASSWORD=change_me

BUCKET_NAME=example-bucket
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=change_me
AWS_SECRET_ACCESS_KEY=change_me
```

---

### Levantar el proyecto

```bash
cd backend/uripet
docker compose up --build -d
```

---

## 9. Documentación (Swagger)

Accede a la documentación en:

```text
http://localhost:8080/swagger-ui/index.html
```

---

## 10. Cómo probar la API

1. Registrar usuario
   `POST /auth/register`

2. Iniciar sesión
   `POST /auth/login`

3. Copiar el token JWT

4. Autorizar en Swagger:

```
Bearer TU_TOKEN
```

5. Probar endpoints protegidos (ej: `/pets`)

---

## 11. Postman

El repositorio incluye:

```text
postman_collection.json
```

Importar este archivo en Postman para probar los endpoints.

---

## 12. Buenas prácticas

* `.env` NO se incluye en el repositorio
* `.env.example` se usa como referencia
* Separación por capas
* Uso de DTOs
* Documentación con Swagger

---

## 13. Conclusión

UriPET implementa un backend completo con autenticación, persistencia, eventos, almacenamiento externo y comunicación en tiempo real, resolviendo el problema de gestión colaborativa de mascotas.

---

## 14. Trabajo futuro

* Frontend
* Refresh tokens
* Paginación
* Deploy en la nube

---
