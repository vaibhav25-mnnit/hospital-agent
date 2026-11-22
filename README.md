# Hospital Agent

A Spring Boot backend for managing patients and their medical reports. It is being extended into an **agentic AI assistant** (Spring AI + AWS Bedrock) that lets users search patients and fetch reports using natural language.

> **Status:** Phase 1 in progress. The database and CRUD REST API are done. The AI agent and S3 storage are next.

## Tech stack

- Java 21
- Spring Boot 4 (Web MVC, Data JPA, Validation)
- PostgreSQL 17 (Docker)
- Maven
- Planned: Spring AI with AWS Bedrock (Claude), AWS S3

## Architecture

```
Client -> Controller -> Service -> Repository -> PostgreSQL
```

Planned agent flow:

```
User -> ChatController -> ChatClient (Spring AI) -> Bedrock (Claude)
                               | tool calls
                     Services -> PostgreSQL / S3 (presigned URLs)
```

The LLM never touches the database. It only asks for a tool to be called, and the Java code does the work.

## Database

Two tables: `patients` and `reports` (one patient has many reports). Report files will live in S3, and the table stores only the `s3_key`.

```sql
CREATE TABLE patients (
  id          BIGSERIAL PRIMARY KEY,
  full_name   VARCHAR(150) NOT NULL,
  dob         DATE,
  gender      VARCHAR(10),
  phone       VARCHAR(20),
  created_at  TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_patients_name ON patients (lower(full_name));

CREATE TABLE reports (
  id          BIGSERIAL PRIMARY KEY,
  patient_id  BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  report_type VARCHAR(80) NOT NULL,
  report_date DATE NOT NULL,
  s3_key      VARCHAR(300) NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_reports_patient ON reports (patient_id);
```

## Getting started

### Prerequisites

- JDK 21+
- Maven
- Docker

### 1. Start PostgreSQL

```bash
docker run --name hospital-db -e POSTGRES_USER=admin_user -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=hospital -p 5000:5432 -d postgres:17
```

Then run the SQL above against the `hospital` database.

### 2. Configure environment variables

Copy `.env.example` to `.env` and fill in your values:

```
DB_URL=jdbc:postgresql://localhost:5000/hospital
DB_USERNAME=admin_user
DB_PASSWORD=password
```

### 3. Run

```bash
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

## REST API

| Method | URL | Description |
|---|---|---|
| GET | `/api/patients?name=rahul` | Search patients by name (all if no name) |
| GET | `/api/patients/{id}` | Get one patient |
| POST | `/api/patients` | Create a patient |
| PUT | `/api/patients/{id}` | Update a patient |
| DELETE | `/api/patients/{id}` | Delete a patient and their reports |
| GET | `/api/patients/{id}/reports` | List a patient's reports |
| POST | `/api/patients/{id}/reports` | Add a report |
| GET | `/api/reports/{id}` | Get one report |
| PUT | `/api/reports/{id}` | Update a report |
| DELETE | `/api/reports/{id}` | Delete a report |

Example:

```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Amit Verma","dob":"1992-03-05","gender":"M","phone":"9000000004"}'
```

## Roadmap

- [x] Database design (PostgreSQL)
- [x] Spring Boot project, entities, repositories
- [x] CRUD APIs for patients and reports
- [ ] Spring AI + AWS Bedrock chat integration
- [ ] Agent tools: `findPatients`, `getReports`
- [ ] Chat memory (follow-up questions like "fetch that patient's reports")
- [ ] S3 storage with presigned URLs
- [ ] Authentication and access control
- [ ] Report summarization and RAG (pgvector)

## Important notes

- Use **synthetic data only**. Real patient data carries legal obligations (HIPAA or local equivalents).
- Never commit `.env` or AWS credentials.

## License

Add a license of your choice (for example MIT).