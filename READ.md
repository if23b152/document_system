# Document Management System
By Mikail Kaya (if23b196), Nichita Pavliuc (if23b099), Ibrahim Barakat (if23b152)

### Link to GitHub repository
https://github.com/if23b196/Document-Management-System.git

#### Note
There is an api.env file in the root directory of the project, which is not in the github repository, that looks like this:

`GEMINI_API_KEY=api_key`

`GOOGLE_API_KEY=api_key`

## REST Server Module

The `rest-server` module provides the main backend API for the Document Management System (DMS). It handles all operations related to document metadata, file uploads, OCR processing, AI-generated summaries, search, and additional features like comments. The module is built using **Spring Boot**, with a layered architecture separating controllers, services, repositories, and mapping logic.

### Key Responsibilities

- **Document Management:** Upload, retrieve, update, and delete documents. Metadata is persisted in **PostgreSQL**, while actual files are stored in **MinIO**.
- **Asynchronous Processing:** When a document is uploaded, a message is sent to the **OCR worker** via **RabbitMQ**. Once OCR and AI summarization are complete, results are returned asynchronously.
- **GenAI Integration:** Extracted text from OCR is sent to a GenAI API (e.g., Google Gemini) to generate document summaries, which are stored in the database.
- **Full-Text Search:** Text content and summaries are indexed in **Elasticsearch** for fast full-text and fuzzy search queries.
- **Comments:** Supports adding, retrieving, and deleting comments for documents, demonstrating the ability to extend the domain model with new entities.
- **Error Handling and Logging:** Layered exception handling ensures failures in asynchronous processing do not block REST endpoints. All critical operations are logged for monitoring.

### Technology Stack

- Java 21+ with Spring Boot 3+
- PostgreSQL for relational data
- MinIO for object storage
- RabbitMQ for asynchronous message queues
- Elasticsearch for search indexing
- GenAI API for automatic summaries
- MapStruct for mapping between entities and DTOs

### Module Structure

- **Controller Layer:** Handles REST endpoints, input validation, and mapping responses.
- **Service Layer:** Contains business logic, including document persistence, MinIO upload/download, OCR and summary processing, and comment management.
- **Repository Layer:** JPA repositories for documents and comments.
- **Messaging:** RabbitMQ producers and listeners manage communication with worker services for OCR and summary generation.
- **DTOs and Mappers:** MapStruct is used to convert between entities and DTOs for API responses.

The REST server module acts as the orchestrator of the DMS, coordinating storage, search, AI processing, and messaging to provide a responsive and scalable backend.


## UI Module

The `ui` module provides the frontend interface for interacting with documents in the system. It is built using **HTML, CSS, JavaScript**, and **Bootstrap 5** for responsive styling. The frontend communicates with the REST server to perform all document-related operations.

### Features

1. **Document Dashboard**
    - Displays a list of uploaded documents with their file names and sizes.
    - Allows uploading new PDF documents.
    - Provides a search bar to filter documents by name.
    - Clicking a document navigates to its **details page**.

2. **Document Details Page**
    - Shows all metadata for a selected document:
        - File name, size, upload timestamp, tags, and AI-generated summary.
    - Allows editing of the file name, tags, and summary.
    - Supports canceling edits and saving updates via the backend API.
    - Includes a **delete button** to remove documents from the system.

3. **Comments Section**
    - Users can add comments to each document.
    - Displays a list of all comments with timestamps.
    - Supports posting new comments via the backend API.

### Technical Details

- **AJAX / Fetch API** is used to asynchronously interact with the REST server:
    - Fetch document lists (`GET /api/documents`)
    - Fetch single document details (`GET /api/documents/{id}`)
    - Update document metadata (`PUT /api/documents/{id}`)
    - Delete documents (`DELETE /api/documents/{id}`)
    - Manage comments (`GET /api/comments/document/{id}`, `POST /api/comments/document/{id}`)
- **Dynamic DOM manipulation** is used to populate lists and update form fields.
- **Forms** are validated client-side before sending requests.
- **Bootstrap cards and grids** provide a clean, responsive layout for both dashboard and details pages.

### Notes

- The frontend relies on the REST server to serve the API endpoints.
- File uploads are sent via `FormData` in POST requests.
- Metadata edits and comments are automatically synchronized with the backend.
- Temporary visual feedback (alerts) informs the user about success or failure of operations.

This module provides a complete, interactive interface for end-users to manage, search, and annotate documents in the system efficiently.


## Worker Service

The `worker-service` module is responsible for processing documents asynchronously. It acts as a background worker that handles CPU- and I/O-intensive tasks so the REST server can remain fast and responsive. The main responsibilities of this module include:

### Overview

1. **Message Consumption**
    - The worker listens for document processing requests sent via **RabbitMQ** from the REST server.
    - Each message contains metadata about the document, including its ID and location in **MinIO** object storage.

2. **File Retrieval**
    - Documents are downloaded from **MinIO**, a lightweight object storage system, to a temporary directory in the worker.

3. **OCR Processing**
    - The worker uses **Tesseract OCR** to extract text from PDF documents.
    - The OCR engine is fully containerized and runs inside the worker-service.

4. **AI Summarization**
    - After OCR, the extracted text is sent to a **GenAI model** (Google Gemini) for automatic summary generation.
    - This step produces a concise summary of the document content.

5. **Search Indexing**
    - The worker indexes the document’s text and summary into **Elasticsearch**, enabling full-text search capabilities in the application.

6. **Result Reporting**
    - Once processing is complete (or if any step fails), the worker sends a **ResultMessage** back to the REST server via RabbitMQ.
    - The REST server updates the document status and stores the summary in the database if successful.

### Key Components

- **MessageListener** – Receives document processing requests from the queue.
- **DocumentProcessingService** – Orchestrates the workflow: download → OCR → AI summary → indexing → result reporting.
- **MinioService** – Downloads PDF files from MinIO.
- **OcrService / TesseractOcrEngine** – Performs OCR on downloaded documents.
- **GenAiService** – Generates AI summaries from extracted text.
- **ElasticsearchService** – Indexes documents and summaries for search.
- **WorkerResultProducer** – Sends results back to the REST server.

### Workflow Summary

1. The REST server sends a document processing request to RabbitMQ.
2. The worker-service consumes the message and downloads the PDF from MinIO.
3. OCR is performed on the PDF to extract text.
4. AI summarization generates a short summary of the extracted text.
5. The document and summary are indexed in Elasticsearch.
6. The processing result is sent back to the REST server.

### Notes

- Temporary files are cleaned up after processing to save disk space.
- If any step fails (OCR, AI, or indexing), the failure is reported back without crashing the worker.
- The worker-service is designed to run independently of the REST server and can scale horizontally to process multiple documents concurrently.


## How to run the project
If you want to run the project then start the docker desktop app, which automatically starts the docker engine.
Then open the terminal in IntelliJ and run the following command:
docker-compose up --build
Wait until the build is finished.
Then go to a browser of your choice, for example FireFox, and open the link: http://localhost:8080/
This is the frontend of our project and once you are done using our Document Management System application, go back to
the IntelliJ terminal and run the following command:
docker-compose down

## How to take a look at the database
To take a look at the table in the database:
docker exec -it dms-postgres bash
psql -U postgres -d documentdb
List all tables: \dt
SELECT * FROM documents;

## How to get project structure
To get project structure without some unnecessary files run this in a terminal where you have the project folder opened:
Get-ChildItem -Recurse -File | Where-Object { $_.FullName -notmatch '\\.git\\|\\.idea\\|\\target\\|\\test\\' } | Select-Object -ExpandProperty FullName

## How do add a project with pom.xml file as a module
Open Project Structure: Go to File → Project Structure (or press Ctrl+Alt+Shift+S on Windows).

Navigate to Modules: In the left sidebar, select Modules.

Add New Module: Click the + (Add) button above the modules list.

Select Import Module: Choose the option Import Module (or New Module → Import Module from existing sources depending on 
your IntelliJ version).

Locate the pom.xml: Navigate to your new worker-service folder and select its pom.xml file.

Confirm: Click OK or Next. IntelliJ will recognize the worker-service folder as a separate, self-contained Maven module.

## How to add project with no pom.xml file as a module
Open Project Structure: Go to File → Project Structure (Ctrl+Alt+Shift+S).

Navigate to Modules: Select your main document-service (or the top-level content root).

Add Content Root: Go to the Sources tab and check the list of content roots. If the ui folder is already inside your 
root DocumentManagementSystem/ folder, it is likely already part of the content root.

Mark as Resource Root (Optional): You can right-click the ui folder in the Project Explorer and mark it as a "Resource 
Root" or "Sources Root" depending on what you're doing with it, but for simple static files, just having it visible is 
often enough.

