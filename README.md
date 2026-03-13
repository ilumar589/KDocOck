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
- **Reactive Kotlin-first approach**: Kotlin-first approach with coroutines for async operations, functional programming patterns via arrow-kt

## Technology Stack

- **Kotlin**: 2.3.10
- **JDK**: 25
- **Kotlin Multiplatform**: org.jetbrains.compose:1.7.1
- **Ktor**: io.ktor:ktor-*, ktor-client-*, ktor-websockets*, ktor-content-negotiation*, ktor-status-pages*
- **Koog**: org.jetbrains.koog:koop-*
- **Exposed**: org.jetbrains.exposed:exposed-core:0.55.0, org.jetbrains.exposedx:exposed-r2dbc:0.3.1
- **pgvector**: PostgreSQL pgvector extension
- **Apache POI**: org.apache.poi:poi:5.3.0, poi-ooxml:5.3.0
- **arrow-kt**: io.arrow-kt:arrow-core:1.2.4, arrow-fx-coroutines:1.2.4
- **kotlinx.serialization**: org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3

## Quick Start

### Prerequisites

**JDK 25**: [Download](https://www.oracle.com/java/technologies/downloads/)
- **Gradle 9.4.0**: [Download](https://gradle.org/releases/)
- **Docker & Docker Compose**: For running PostgreSQL with pgvector

### 1. Start Ollama

```bash
docker-compose up -d
```

This pulls `ollama/ollama` image, starts up server on port **11434** and pulls `llama3.2` model.
Model data is persisted in `ollama_data` Docker volume so subsequent starts are instant.

### 2. Build and Run

```bash
cargo run --release
```

The first build downloads all Rust crates and may take a few minutes.

### 3. Use the Application

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
│   └── jvmMain/      # JVM API implementation
│   ├── build.gradle.kts
│   └── wasmJsMain/    # WASM support
│   │   └── wasmJsTest/    # WASM tests
│   │   └── build.gradle.kts
│   ├── shared/                  # Shared business logic
│   │   ├── src/
│   │   └── jvmMain/      # JVM-specific code
│   │   └── jvmTest/      # JVM tests
│   │   └── wasmJsMain/    # WASM code
│   │   └── build.gradle.kts
│   ├── desktop/               # Compose Multiplatform desktop
│   │   ├── src/
│   │   └── jvmMain/      # JVM desktop
│   │   └── jvmTest/      # JVM tests
│   │   └── wasmJsMain/    # WASM tests
│   │   └── build.gradle.kts
├── docker-compose.yml           # PostgreSQL + pgvector
├── build.gradle.kts           # Root build configuration
├── gradle.properties           # Gradle wrapper
├── gradle/                   # Gradle wrapper
├── settings.gradle.kts        # Project settings
```

## Configuration

The application can be configured through:

1. **Desktop UI Settings Dialog**:
   - Ollama endpoint (default: `http://localhost:11434`)
   - Ollama model (default: `llama3.2`)
   - Output directory for saved `.feature` files
   - Log panel visibility
   - API key for authentication (optional)
   - CORS settings

2. **API Configuration** (`api/config`):
   - API key for authentication (optional)
   - Port (default: `8080`)
   - CORS settings
   - Database configuration:
     - PostgreSQL connection string
     - Connection pool size
     - Vector embedding dimension (default: 768)
     - Similarity threshold (default: 0.7)
     - Max search results (default) 5)

## Development

### Running Tests

```bash
# Run all tests
./gradlew test
```

# Run specific test module
./gradlew :shared:test
./gradlew :api:test
./gradlew :desktop:test
```

### Code Coverage

```bash
./gradlew test jacocoTestReport
```

### Architecture

### Core Components

1. **Document Parsing** (`shared/parsing`):
   - `DocumentParserDispatcher` - Routes to appropriate parser based on file type
   - `WordDocumentParser` - Apache POI-based .docx parsing
   - `ExcelDocumentParser` - Apache POI-based .xlsx parsing
   - `VisioDocumentParser` - XML-based .vsdx parsing

2. **RAG System** (`shared/rag`):
   - `VectorStore` - PostgreSQL/pgvector storage
   - `VectorSimilaritySearch` - HNSW-based similarity search
   - `CrossFileContextRetri` - Retrieves relevant context from previous documents
   - `BatchEmbeddingGenerator` - Parallel embedding generation
   - `ContextInjector` - Injects RAG context into LLM prompts

3. **LLM Orchestration** (`shared/agents`):
   - `WorkflowOrchestrator` - Graph-based workflow execution
   - `OllamaClient` - HTTP client for Ollama
   - `GherkinGenerator` - Gherkin content generation with streaming
   - `GherkinValidator` - Gherkin syntax validation
   - `GherkinSyntaxRepairer` - Automatic syntax repair

4. **Gherkin Generation** (`shared/gherkin`):
   - `GherkinPromptTemplates` - LLM prompt templates
   - `ContextInjector` - RAG context injection
   - `GherkinGenerator` - Streaming Gherkin generation
   - `GherkinValidator` - Gherkin syntax validation
   - `GherkinSyntaxRepairer` - Automatic syntax repair

5. **Web API** (`api/`):
   - `KtorServer` - Ktor server with Netty engine
   - REST API endpoints for document management
   - WebSocket support for streaming
   - Authentication with API keys
   - Error handling with appropriate status codes
   - Structured error responses

6. **Desktop UI** (`desktop/`):
   - Compose Multiplatform desktop
   - File selection and management
   - Progress tracking
   - Log panel with color coding
   - Gherkin content display with syntax highlighting
   - Toast notifications
   - Settings persistence

### Data Flow

1. **Document Upload**:
   - User uploads document(s) via UI or API
   - Document is parsed to extract structured content
   - Parsed content is embedded and stored in vector database

2. **Context Retrieval**:
   - Before generating Gherkin, relevant context is retrieved from RAG system
   - Context includes summaries of previously processed documents
   - Context is injected into LLM prompt

3. **Gherkin Generation**:
   - LLM generates Gherkin content with cross-file context
   - Streaming updates sent to UI via WebSocket
   - Generated Gherkin is validated and repaired if needed
   - Generated Gherkin is stored with metadata

4. **Result Storage**:
   - Generated Gherkin is stored with metadata
   - User can view, copy, or save `.feature` files

5. **Performance Considerations**:
   - Use HNSW indexes for O(1) million vectors
   - Tune HNSW parameters (m, ef_construction)
   - Batch processing for large documents
   - Cache frequently accessed documents

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
   - `CrossFileContextRetriever` - Retrieves relevant context from previous documents
   - `BatchEmbeddingGenerator` - Parallel embedding generation
   - `ContextInjector` - Injects RAG context into LLM prompts

3. **LLM Orchestration** (`shared/agents`):
   - `WorkflowOrchestrator` - Graph-based workflow execution
   - `OllamaClient` - HTTP client for Ollama
   - `GherkinGenerator` - Gherkin content generation with streaming
   - `GherkinValidator` - Gherkin syntax validation
   - `GherkinSyntaxRepairer` - Automatic syntax repair

4. **Gherkin Generation** (`shared/gherkin`):
   - `GherkinPromptTemplates` - LLM prompt templates
   - `ContextInjector` - RAG context injection
   - `GherkinGenerator` - Streaming Gherkin generation
   - `GherkinValidator` - Gherkin syntax validation
   - `GherkinSyntaxRepairer` - Automatic syntax repair

5. **Web API** (`api/`):
   - `KtorServer` - Ktor server with Netty engine
   - REST API endpoints for document management
   - WebSocket support for streaming
   - Authentication with API keys
   - Error handling with appropriate status codes
   - Structured error responses

6. **Desktop UI** (`desktop/`):
   - Compose Multiplatform desktop
   - File selection and management
   - Progress tracking
   - Log panel with color coding
   - Gherkin content display with syntax highlighting
   - Toast notifications
   - Settings persistence

### Data Flow

1. **Document Upload**:
   - User uploads document(s) via UI or API
   - Document is parsed to extract structured content
   - Parsed content is embedded and stored in vector database

2. **Context Retrieval**:
   - Before generating Gherkin, relevant context is retrieved from RAG system
   - Context includes summaries of previously processed documents
   - Context is injected into LLM prompt

3. **Gherkin Generation**:
   - LLM generates Gherkin content with cross-file context
   - Streaming updates sent to UI via WebSocket
   - Generated Gherkin is validated and repaired if needed
   - Generated Gherkin is stored with metadata

4. **Result Storage**:
   - Generated Gherkin is stored with metadata
   - User can view, copy, or save `.feature` files

5. **Performance Considerations**:
   - Use HNSW indexes for O(1) million vectors
   - Tune HNSW parameters (m, ef_construction)
   - Batch processing for large documents
   - Cache frequently accessed documents