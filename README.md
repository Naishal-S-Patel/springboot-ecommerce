# springboot-ecommerce

Full-stack e-commerce project pairing a React/Vite storefront & admin UI with a Spring Boot API (PostgreSQL, JWT auth, Stripe payments).

## What this project is about
- Modern React frontend (Vite) for shopping experience and admin management
- Spring Boot backend with secure JWT-based auth and role-based access
- PostgreSQL persistence and Stripe integration for payments
- Example config provided; real secrets stay local and ignored

## Structure
- [ecom-frontend](ecom-frontend): React + Vite storefront/admin UI.
- [sb-ecom](sb-ecom): Spring Boot API with PostgreSQL, JWT auth, Stripe integration.

## Prerequisites
- Node.js (18+ recommended) and npm
- Java 17+
- PostgreSQL running locally

## Frontend (React/Vite)
```bash
cd ecom-frontend
npm install
npm run dev    # starts on http://localhost:5173
npm run build  # production build
```

## Backend (Spring Boot)
1) Copy the example config and fill in secrets:
```bash
cd sb-ecom
cp src/main/resources/application.example.properties src/main/resources/application.properties
```
Set values for DB credentials, JWT secret, and `stripe.secret.key`.

2) Run the API:
```bash
# from sb-ecom
./mvnw spring-boot:run    # or mvnw.cmd on Windows
```
API default port: http://localhost:8080

## Environment Notes
- Real config files are git-ignored (see [.gitignore](.gitignore)). Keep secrets in `application.properties` (not committed).
- Example config: [sb-ecom/src/main/resources/application.example.properties](sb-ecom/src/main/resources/application.example.properties).

## Testing
- Frontend: `npm test` (add tests as needed).
- Backend: `./mvnw test` (or `mvnw.cmd test` on Windows).

## Production Builds
- Frontend: `npm run build` produces `dist/` (git-ignored).
- Backend: `./mvnw clean package` produces JAR in `target/` (git-ignored).

## Common Ports
- Frontend: 5173
- Backend/API: 8080
- Postgres default: 5432
