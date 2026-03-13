# KDocOck

A Kotlin Multiplatform application that converts business documents (Word, Excel, Visio) into Gherkin feature files using local LLMs.

## Features

- **Multi-file selection**: Select and process multiple documents in one session
- **Cross-file context**: The LLM sees summaries of all previously processed files so references between documents are preserved
- **Document support**: 
  - Word (.docx) - Extracts paragraph text and table data
  - Excel (.xlsx) - Extracts worksheet and cell data
  - Visio (.vsdx) - Extracts shape labels and text
- **Local LLM**: Runs 100% locally via Ollama – no data leaves your machine
- **Configurable model**: Any model supported by Ollama can be used (default: llama3.2)
- **RAG system**: pgvector + Exposed R2DBC for storing and retrieving document embeddings with semantic search
- **Cross-platform UI**: Compose Multiplatform for desktop (JVM) and web (WASM)
- **Web API**: Ktor REST API for programmatic access
- **Advanced agent orchestration**: Koog's graph-based workflows with streaming and parallel tool execution
- **Error handling**: arrow-kt for functional error handling and coroutine helpers
- **Reactive Kotlin**: Kotlin-first approach with coroutines for async operations, functional programming patterns via arrow-kt

## Quick Start

### Prerequisites

- **JDK 25**: [Download](https://www.oracle.com/java/technologies/downloads/)
- **Gradle 9.4.0**: [Download](https://gradle.org/releases/)
- **Docker & Docker Compose**: For running PostgreSQL with pgvector

### 1. Start Ollama

```bash
docker-compose up -d
```

This pulls the `ollama/ollama` image, starts the server on port **11434**, and pulls the `llama3.2` model.
Model data is persisted in the `ollama_data` Docker volume so subsequent starts are instant.

### 2. Build and Run

```bash
./gradlew build
```

The first build downloads all Kotlin dependencies and may take a few minutes.

### 3. Use Application

#### Desktop UI

```bash
./gradlew :desktop:run
```

#### Web API

```bash
./gradlew :api:run
```

#### Programmatic Access

The API is available at `http://localhost:8080/api`

## Project Structure

```
kdockerck/
├── api/                    # Ktor REST API
│   └── src/
│   │   └── jvmMain/      # JVM API implementation
│   │   └── build.gradle.kts
│   │   └── wasmJsMain/    # WASM support (future)
│   │   │   └── wasmJsTest/  # WASM tests
│   │   └── build.gradle.kts
│   ├── shared/                  # Shared business logic
│   │   ├── src/
│   │   │   └── commonMain/    # Cross-platform code
│   │   │   └── jvmMain/      # JVM-specific code
│   │   │   └── jvmTest/      # JVM tests
│   │   │   └── wasmJsMain/    # WASM code
│   │   │   └── wasmJsTest/    # WASM tests
│   │   │   └── build.gradle.kts
│   ├── desktop/               # Compose Multiplatform desktop
│   │   ├── src/
│   │   │   └── jvmMain/      # JVM desktop
│   │   │   └── jvmTest/      # JVM tests
│   │   │   └── build.gradle.kts
│   ├── web/                     # Compose Multiplatform web
│   │   ├── src/
│   │   │   └── wasmJsMain/    # WASM code
│   │   │   └── wasmJsTest/    # WASM tests
│   │   │   └── build.gradle.kts
├── docker-compose.yml           # PostgreSQL + pgvector
├── build.gradle.kts           # Root build configuration
├── gradle/                    # Gradle wrapper
├── gradle.properties            # Gradle properties
├── settings.gradle.kts        # Project settings
├── gradle/                     # Convention plugins
├── gradle/                    # Kotlin DSL
```

## Configuration

The application can be configured through:

1. **Desktop UI Settings Dialog**:
   - Ollama endpoint (default: `http://localhost:11434`)
   - Ollama model (default: `llama3.2`)
   - Output directory for saved `.feature` files
   - Log panel visibility

2. **API Configuration** (`api/config`):
   - API key for authentication (optional)
   - CORS settings
   - Port (default: `8080`)

3. **Database Configuration** (`app/config`):
   - PostgreSQL connection string
   - Connection pool size
   - Vector embedding dimension (default: 768)
   - Similarity threshold (default: 0.7)
   - Max search results (default: 5)

## API Endpoints

### Health Check
- `GET /api/health` - Health check

### Configuration
- `GET /api/config` - Get current configuration

### Documents
- `POST /api/documents` - Upload a document
- `GET /api/documents` - List all documents
- `GET /api/documents/{id}` - Get document metadata
- `DELETE /api/documents/{id}` - Delete a document

### Processing
- `POST /api/documents/{id}/process` - Process a document
- `POST /api/documents/batch-process` - Batch process documents

### Status Tracking
- `GET /api/documents/{id}/status` - Get document processing status
- `GET /api/jobs/{jobId}/status` - Get batch job status

### Results
- `GET /api/documents/{id}/result` - Get generated Gherkin content
- `GET /api/jobs/{jobId}/results` - Get batch job results

### WebSocket Endpoints
- `ws://localhost:8080/ws/progress` - Stream processing progress
- `ws://localhost:8080/ws/tokens` - Stream LLM tokens

## WebSocket Events

### Progress Updates
```json
{
  "type": "progress",
  "timestamp": "2024-01-01T00:00.000Z",
  "message": "Processing document: example.docx",
  "documentId": "doc-1",
  "percentage": 45.0
}
```

### Token Stream
```json
{
  "type": "token",
  "timestamp": "2024-01-01T00:00.000Z",
  "documentId": "doc-1",
  "token": "Given"
}
```

## Error Responses

All errors return structured JSON:

```json
{
  "error": "error type",
  "message": "Human-readable error message",
  "timestamp": "2024-01-01T00:00.000Z",
  "details": {
    "field": "value",
    ...
  }
}
```

## Development

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :shared:test
./gradlew :api:test
./gradlew :desktop:test
```

### Code Coverage

```bash
# Generate coverage report
./gradlew koverX
```

### Running with Coverage

```bash
# Run tests with coverage
./gradlew test koverX --html
```

## Architecture

### Core Components

1. **Document Parsing** (`shared/parsing`):
   - `DocumentParserDispatcher` - Routes to appropriate parser based on file type
   - `WordDocumentParser` - Apache POI-based .docx parsing
   - `ExcelDocumentParser` - Apache POI-based .xlsx parsing
   - `VisioDocumentParser` - XML-based .vsdx parsing

2. **RAG System** (`shared/rag`):
   - `VectorStore` - PostgreSQL/pgvector storage
   - `VectorSimilaritySearch` - HNSW-based similarity search
   - `BatchEmbeddingGenerator` - Parallel embedding generation
   - `CrossFileContextRetriever` - Context retrieval for cross-file references
   - `ContextInjector` - Injects RAG context into LLM prompts

3. **LLM Orchestration** (`shared/agents`):
   - `OllamaClient` - HTTP client for Ollama
   - `WorkflowOrchestrator` - Graph-based workflow execution
   - Agent definitions:
     - `ParsingAgent` - Extract structured information
     - `EmbeddingAgent` - Generate embeddings
     - `GenerationAgent` - Generate Gherkin content

4. **Gherkin Generation** (`shared/gherkin`):
   - `GherkinGenerator` - Streaming Gherkin generation with RAG context
   - `GherkinValidator` - Gherkin syntax validation
   - `GherkinSyntaxRepairer` - Automatic syntax repair

5. **Web API** (`api/`):
   - `KtorServer` - Ktor REST API with WebSocket support
   - Endpoints for document management and processing
   - Authentication and CORS support
   - Content negotiation (JSON, plain text)

6. **Desktop UI** (`desktop/`):
   - `DesktopApp` - Compose Multiplatform application
   - `FileSelectionDialog` - File selection dialog
   - `FileListPanel` - Document list management
   - `ProgressBar` - Progress tracking
   - `GherkinContentPanel` - Gherkin content display
   - `LogPanel` - Logging with color coding
   - `ProcessedDocumentsPanel` - Processed documents list
   - `SettingsDialog` - Configuration dialog
   - `ToastNotificationSystem` - Toast notifications

## Data Flow

1. **Document Processing Flow**:
   ```
   User selects files
   → Documents are parsed in parallel
   → Each document is embedded
   → Context is retrieved from RAG system
   → Gherkin is generated with cross-file context
   → Results are validated and repaired
   ```

2. **LLM Agent Workflow**:
   ```
   Parsing Agent → Embedding Agent → Generation Agent
   → Each agent streams results to UI
   ```

3. **API Flow**:
   ```
   Document upload → Parsing → Embedding → Context Retrieval → Generation → Validation → Repair
   → WebSocket streams progress and tokens
   ```

## Configuration Files

- `~/.kdockerck/config.json` - User preferences (Ollama endpoint, model, output directory)
- `~/.kdockerck/.kdockerck/config.json` - Application config (API key, port, database)
- `~/.kdockerck/.kdockerck/schema_migrations/` - Database schema version tracking

## Logging

The application uses structured logging with levels:
- `DEBUG` - Detailed debugging information
- `INFO` - General informational messages
- `WARN` - Warning messages
- `ERROR` - Error messages

Logs are displayed in:
- Desktop UI log panel (color-coded)
- API server logs (stdout/stderr)
- File logs (separate log files)

## Performance Considerations

1. **Vector Similarity Search**:
   - Use HNSW indexes for O(1) million vectors
   - Tune HNSW parameters (m, ef_construction, ef_runtime)
   - Consider materialized views for large datasets

2. **Document Processing**:
   - Parse documents in parallel where possible
   - Use streaming for LLM responses
   - Chunk large documents before embedding

3. **LLM Integration**:
   - Implement retry logic with exponential backoff
   - Use circuit breaker for failing services
   - Cache frequently accessed documents

4. **UI Performance**:
   - Use Compose recomposition for smooth 60fps rendering
   - Lazy load large Gherkin content
   - Virtualize lists for document lists

## Security Considerations

1. **API Authentication**:
   - API key authentication (configurable)
   - HTTPS support for production
   - Rate limiting

2. **File Handling**:
   - Validate file types (only .docx, .xlsx, .vsdx)
   - Limit file size (configurable, e.g., 100MB)
   - Sanitize file names

3. **Database**:
   - Use connection pooling
   - Use parameterized queries
   - Validate all inputs
   - Handle connection errors gracefully

4. **LLM Integration**:
   - Validate LLM responses
   - Implement timeout for LLM requests
   - Sanitize prompts before sending to LLM
   - Handle streaming errors gracefully

## Troubleshooting

### Common Issues

**Ollama Connection Failed**
- Check if Ollama is running: `ollama serve` or `docker-compose up -d`
- Check if port 11434 is available
- Verify Ollama model is installed: `ollama pull llama3.2`

**PostgreSQL Connection Failed**
- Check if PostgreSQL is running: `docker-compose up -d`
- Verify pgvector extension is enabled: `CREATE EXTENSION IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pgvector')`
- Check database credentials

**Build Issues**
- Clear Gradle cache: `./gradlew clean --no-daemon`
- Use Kotlin 2.3.10 with JDK 25
- Update dependencies: `./gradlew --refresh-dependencies`

**Runtime Issues**
- Out of memory errors: Increase JVM heap size
- Connection timeouts: Increase timeout values
- Slow LLM responses: Consider reducing context size or model size

### Support

For issues, questions, or contributions:
- GitHub Issues: [https://github.com/yourusername/kdockerck/issues](https://github.com/yourusername/kdockerck/issues)
- GitHub Discussions: [https://github.com/yourusername/kdockerck/discussions](https://github.com/yourusername/kdockerck/discussions)
- Stack Overflow: [https://stackoverflow.com/questions/tagged/kotlin](https://stackoverflow.com/questions/tagged/kotlin)
- Kotlin Discord: [https://kotlinlang.slack.com/](https://kotlinlang.slack.com/)