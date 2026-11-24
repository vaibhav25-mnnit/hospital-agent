# Hospital Agent

A Spring Boot backend where staff can **chat in plain English** to find patients and look inside their medical reports.

> **Status:** Working locally. The chat agent, patient and report lookups, and report search (RAG) are done. Report file storage through S3 (LocalStack for local testing, via a separate document service) is in progress.

> **Use fake data only.** Real patient data carries legal duties (HIPAA or local equivalents). This project has no login yet, so it is a learning and portfolio project, not production software.

## What it does

```
You:    Is there a patient named Priya Patel?
Agent:  Yes: Priya Patel (id 3, born 1995-01-20).

You:    What was her hemoglobin?
Agent:  10.9 g/dL, flagged low (Blood Test, 2026-09-01).

You:    What was her cholesterol?
Agent:  I couldn't find cholesterol in this patient's reports.
```

The last answer matters as much as the first two. The agent must say "not found" instead of making something up.

## Ideas you need (for beginners)

**LLM (large language model).** A program that writes text. It knows nothing about your hospital, and it forgets everything between requests.

**Tool calling.** The LLM can't touch your database. Instead, it can *ask* your Java code to run a small function, called a tool (for example "find patients named Rahul"). Your code runs it and hands the result back. The LLM then writes the answer from that result.

**Agent.** An LLM that chooses which tool to use, uses the result, and decides what to do next.

**Chunking.** A report can be long, and a search works best on small pieces. So each report is cut into short passages called *chunks*. Our sample reports are tiny, so most become a single chunk.

**Embedding.** A list of numbers that captures the *meaning* of a text. Texts with similar meaning get similar numbers. This lets us search by meaning, not just by matching words. Searching "iron deficiency" finds a sentence about "low ferritin and anemia" even though they share no words.

**Vector store.** A database that stores chunks with their embeddings and finds the closest ones to a question. We use **pgvector**, an add-on that gives PostgreSQL this ability, so one database holds everything.

**RAG (retrieval-augmented generation).** First *retrieve* the relevant chunks, then let the LLM *write an answer* using only those chunks. It's like an open-book exam: the model reads the passage before answering.

## How it works

The agent has two kinds of tools, because there are two kinds of questions.

| Question type | Example | How it's answered |
|---|---|---|
| **Exact records** | "Find Rahul", "list her reports", "his phone number" | SQL tools query PostgreSQL directly. Results are exact. |
| **What's inside a report** | "What was the hemoglobin?", "Does she have anemia?" | Search tool looks through report text (RAG). |

Why two paths? Exact facts like ids, dates and phone numbers must never be "close enough". A similarity search can return a near match, which is wrong for these. Vector search is only used where meaning matters.

### Flow 1: Indexing (happens when reports are loaded)

```
Report file (text) -> add a header (type + date) -> cut into chunks
   -> turn each chunk into an embedding -> save in the vector store
      together with metadata: patient id, report id, type, date
```

Each chunk is saved with its **patient id**. That label is what keeps one patient's data separate from another's.

### Flow 2: Asking (happens on every chat message)

```
Your question
   -> the LLM decides which tool to call
   -> tool runs (SQL query, or search of the vector store)
   -> result goes back to the LLM
   -> the LLM writes a short answer
```

For a report question, the agent first finds the patient (SQL), then searches only that patient's chunks (vector store).

### Keeping answers honest

Small local models can invent plausible-looking facts (called *hallucination*). During testing, the agent once described a "Lipid Panel" that did not exist. These safeguards were added:

- **The patient filter lives in code, not in the prompt.** The model can't search across other patients' reports.
- **Exact data bypasses the model's wording.** Tools for patient lists and reports return the database result directly.
- **"Not found" is explicit.** If no retrieved passage mentions the item asked about, the search tool says so, and the model is told not to guess.
- **Prompt rules:** answer only from retrieved text, always name the source report, quote numbers exactly.
- **Short chat memory,** so one wrong answer doesn't snowball into later ones.

These reduce mistakes but don't eliminate them. Always check important values against the source report.

## Tech stack

- Java 21, Spring Boot 4 (Web MVC, Data JPA, Validation)
- Spring AI 2.x
- PostgreSQL 17 with the **pgvector** extension (Docker)
- **Ollama** for local models, with no cost and no data leaving your machine:
    - chat model: `gemma4:e2b`
    - embedding model: `nomic-embed-text` (768 numbers per embedding)
- Maven
- In progress: S3-compatible storage (LocalStack for local testing) through a separate document service

The model is a setting, so you can change it. This project started on AWS Bedrock and was moved to Ollama to avoid cost. Spring AI makes switching providers mostly a dependency and configuration change.

## Architecture

```
Browser (chat page)
   |
ChatController -> ChatService -> ChatClient (Spring AI) -> Ollama (local model)
                                      |  tool calls
              +-----------------------+----------------------+
              |                                              |
        Patient / report tools                     Report search tool
        (SQL: patients, reports)                   (vector search, filtered by patient)
              |                                              |
              +-------------- PostgreSQL + pgvector ---------+
                       tables: patients, reports,
                       vector_store, chat memory

Report files (S3 / LocalStack)  --read once when indexing-->  chunks in vector_store
```

The report files are read from storage **once, at indexing time**. Chat questions are answered from the stored chunks, so S3 isn't touched while chatting.

## Database

One PostgreSQL database (`hospital`) holds four things:

| Table | Purpose |
|---|---|
| `patients` | One row per patient |
| `reports` | One row per report, linked to a patient. Stores the file's storage key (`s3_key`), not the file itself. |
| `vector_store` | Report chunks and their embeddings (created automatically by Spring AI) |
| `spring_ai_chat_memory` | Chat history (created automatically) |

You create `patients` and `reports` yourself. Their SQL is in the project's `db` folder or in the setup notes. The other two tables are created at startup.

## Getting started

### Prerequisites

JDK 21+, Maven, Docker, and [Ollama](https://ollama.com).

### 1. Start PostgreSQL with pgvector

A normal Postgres image does **not** include pgvector, so use the pgvector image:

```bash
docker run --name hospital-pgvector -e POSTGRES_USER=admin_user -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=hospital -p 5001:5432 -d pgvector/pgvector:pg17
```

Then create the `patients` and `reports` tables and add some fake rows.

### 2. Download the AI models

```bash
ollama pull gemma4:e2b
ollama pull nomic-embed-text
```

The chat model must support **tool calling**. Check with `ollama show <model>` and look for `tools` under Capabilities. A 4.6B model like this one is small and can be slow on a CPU-only computer. A larger model gives better answers but needs more memory.

### 3. Configure environment variables

Copy `.env.example` to `.env` and fill in your values:

```
DB_URL=jdbc:postgresql://localhost:5001/hospital
DB_USERNAME=admin_user
DB_PASSWORD=password
```

Never commit `.env`.

### 4. Run

```bash
mvn spring-boot:run
```

Open the chat page at `http://localhost:<port>/` (the port is `server.port` in `application.properties`).

### 5. Index the reports

After loading report files, build the searchable chunks:

```bash
curl -X POST http://localhost:<port>/api/admin/reindex
```

Run this again whenever report files change. The command rebuilds everything from the database, so it's safe to repeat.

## Key settings

| Setting | Meaning |
|---|---|
| `spring.ai.ollama.chat.options.model` | Chat model name |
| `spring.ai.ollama.embedding.options.model` | Embedding model name |
| `spring.ai.vectorstore.pgvector.dimensions` | Must match the embedding model (768 here). A mismatch is the most common vector-store error. |
| `spring.ai.vectorstore.pgvector.initialize-schema` | Lets Spring AI create the vector table |
| `spring.ai.model.chat` / `.embedding` | Selects which provider to use when several are on the classpath |

## REST API

**Chat and agent**

| Method | URL | Description |
|---|---|---|
| POST | `/api/chat` | Send `{"message": "..."}` and get `{"reply": "..."}` |
| POST | `/api/admin/reindex` | Rebuild the report chunks (no login yet, development only) |

**Patients and reports (CRUD)**

| Method | URL | Description |
|---|---|---|
| GET | `/api/patients?name=rahul` | Search patients by name (all if no name) |
| GET / PUT / DELETE | `/api/patients/{id}` | Read, update or delete a patient |
| POST | `/api/patients` | Create a patient |
| GET / POST | `/api/patients/{id}/reports` | List or add a patient's reports |
| GET / PUT / DELETE | `/api/reports/{id}` | Read, update or delete a report |

## Try it

The sample data is designed to test the tricky cases: two patients with the same name, and questions with known answers.

| Ask | Expected |
|---|---|
| "Is there a patient named Rahul Sharma?" | Two matches (ids 1 and 2), and it asks which one |
| "The one born in 1990. What was his cholesterol?" | 185 mg/dL from his blood test |
| "What did the MRI of the 1985 Rahul show?" | Mild medial meniscus tear with small joint effusion |
| "What was Priya's hemoglobin?" | 10.9 g/dL, flagged low |
| "What was Priya's cholesterol?" | "Not found", because it isn't in her report |

For every answer, compare it with the source report. Between test sessions, clear the chat history table so old answers don't affect new ones.

## Project layout

```
controllers/   REST endpoints (chat, patients, reports, admin)
service/       Business logic (patients, reports, chat, report indexing)
tools/         What the agent can do (patient tools, report search tool)
config/        Spring AI setup (system prompt, memory, registered tools)
entity/        Database table classes
repository/    Database access
dto/           Request and response shapes
```

## Roadmap

- [x] Database design and CRUD APIs
- [x] Chat endpoint with Spring AI
- [x] Chat memory saved in PostgreSQL
- [x] Agent tools for patient and report lookup
- [x] Local models with Ollama
- [x] pgvector, embeddings and chunking
- [x] Report content search (RAG), scoped to one patient
- [x] Simple browser chat page
- [ ] Report file storage with S3 (LocalStack for testing, through a separate document service)
- [ ] Download links for reports
- [ ] Upload endpoint that stores the file, saves the report row and indexes it in one step
- [ ] Auto-index on report create, update and delete
- [ ] PDF text extraction
- [ ] Separate chat history per user or tab
- [ ] Login, roles and access checks inside the tools
- [ ] Audit log of who looked at which patient
- [ ] Show source excerpts next to answers in the UI

## Limitations

- **No login.** Anyone who can reach the app can ask about any patient. Do not expose it to a network.
- **Small model.** It can skip tools or misquote values, especially on a CPU-only computer, and replies can take tens of seconds.
- **One shared chat history** for all users and tabs.
- **Text files only** for now. PDFs need a text extraction step, and scanned PDFs need OCR.
- **Re-index needed** after report files change (until auto-indexing is added).
- **Not medical advice.** The agent repeats what reports say. It must not diagnose.

## Glossary

| Term | Meaning |
|---|---|
| Prompt | The text sent to the LLM |
| System prompt | Standing rules given to the LLM on every request |
| Token | A small piece of text (a word or part of one) that models count |
| Hallucination | A confident answer that is made up |
| Cosine similarity | A way to measure how close two embeddings are |
| Metadata | Labels stored with a chunk (patient id, report id) used for filtering |
| Presigned URL | A temporary download link generated for a private file |

## License

Add a license of your choice (for example MIT).