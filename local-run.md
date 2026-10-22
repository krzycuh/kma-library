# Local Startup Instructions

### 1. Stop any running containers:
   `docker-compose down -v`

### 2. Build and start everything:
  `docker-compose up --build`

This command will:
  - Build the backend image with current code
  - Start PostgreSQL 16
  - Start the Spring Boot application
  - Wait for the database to be ready before starting the application

### 3. Check logs (in another terminal):

   - All services: `docker-compose logs -f`
   - Backend only: `docker-compose logs -f backend`

### 4. Check if it's working:
   - Backend: http://localhost:8080/actuator/health
   - OpenAPI/Swagger: http://localhost:8080/swagger-ui.html

### 5. To stop:
   - Stop (preserve data): `docker-compose down`
   - Stop + remove data: `docker-compose down -v`

### 6. Useful commands

   - Run in background
   `docker-compose up -d --build`

   - Rebuild backend only
   `docker-compose build backend`
   `docker-compose up -d backend`

   - Restart backend
   `docker-compose restart backend`

   - View status
   `docker-compose ps`



