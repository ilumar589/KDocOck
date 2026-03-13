## Context

This is a new project replicating DockOck's functionality (document-to-Gherkin conversion) using a modern Kotlin stack. The original Rust implementation uses egui for UI and rig-core for LLM integration. The Kotlin version will leverage Kotlin Multiplatform for cross-platform UI, Koog for advanced agent orchestration, and pgvector for RAG-based context sharing between documents.

Key constraints:
- Kotlin 2.3.10 with JDK 25
- Kotlin Multiplatform for UI (Compose Multiplatform)
- Koog for agent orchestration with streaming and parallel execution
- pgvector + Exposed R2DBC for vector embeddings and semantic search
- Ktor for REST API
- arrow-kt for functional error handling
- Gradle 9.4.0 build system
- Stay in Kotlin ecosystem (kotlinx.serialization, not Jackson)

## Goals / Non-Goals

**Goals:**
- Parse Word (.docx), Excel (.xlsx), and Visio (.vsdx) files to extract text content
- Use Koog's graph-based workflows to orchestrate LLM agents with streaming responses
- Implement RAG system using pgvector to share context across files during processing
- Generate Gherkin feature files from extracted document content
- Provide Compose Multiplatform desktop UI matching DockOck's functionality
- Expose Ktor REST API for programmatic access
- Use arrow-kt for robust error handling and functional patterns
- Support parallel tool execution and streaming through Koog

**Non-Goals:**
- Cloud LLM integration (local LLM only via Ollama)
- Mobile platforms (desktop-only initially)
- Real-time collaboration features
- Advanced document editing capabilities
- Alternative output formats beyond Gherkin

## Decisions

### 1. Project Structure: Kotlin Multiplatform with Shared Module

**Decision:** Use Kotlin Multiplatform with a shared business logic module and platform-specific UI modules.

**Rationale:**
- Shared code (document parsing, LLM orchestration, RAG) can be reused across desktop, web, and future mobile platforms
- Compose Multiplatform allows single UI codebase for desktop (JVM) and web (WASM)
- Clear separation of concerns: shared logic vs. platform-specific UI

**Alternatives considered:**
- Pure JVM project: Simpler but no code sharing for future platforms
- Separate projects: Duplication of business logic

### 2. Document Parsing: Apache POI for Office Documents

**Decision:** Use Apache POI (Kotlin wrappers) for .docx and .xlsx parsing, custom XML parsing for .vsdx.

**Rationale:**
- Apache POI is mature, well-maintained, and supports all Office formats
- Kotlin wrappers provide idiomatic Kotlin APIs
- .vsdx is XML-based (ZIP archive), can parse with standard Kotlin XML libraries

**Alternatives considered:**
- Aspose: Commercial license required
- Custom parsers: Too much maintenance overhead

### 3. LLM Integration: Koog with Graph-Based Workflows

**Decision:** Use Koog for agent orchestration with graph-based workflows, streaming responses, and parallel tool execution.

**Rationale:**
- Koog provides built-in streaming and parallel execution
- Graph-based workflows enable complex multi-step document processing
- Kotlin-native, fits the ecosystem constraint
- Observability features built-in

**Alternatives considered:**
- Direct HTTP calls to Ollama: No orchestration, streaming, or observability
- LangChain4j: Less mature, limited streaming support

### 4. RAG System: pgvector + Exposed R2DBC

**Decision:** Use PostgreSQL with pgvector extension and Exposed R2DBC for vector embeddings and semantic search.

**Rationale:**
- pgvector provides efficient vector similarity search
- Exposed R2DBC offers reactive, coroutine-friendly database access
- RAG enables cross-file context sharing (LLM sees summaries of previously processed files)
- Fits Kotlin ecosystem (Exposed is Kotlin-first)

**Alternatives considered:**
- In-memory vector store: No persistence, limited scalability
- External vector DB (Pinecone, Weaviate): Adds complexity and external dependency

### 5. Error Handling: arrow-kt

**Decision:** Use arrow-kt for functional error handling with Either types, Raise, and coroutine helpers.

**Rationale:**
- Functional error handling patterns are more robust than exceptions
- Either types make error paths explicit in type system
- Coroutine helpers integrate seamlessly with Kotlin coroutines
- Transactional memory support if needed

**Alternatives considered:**
- Standard Kotlin exceptions: Less explicit, harder to track error paths
- Result type: Less feature-rich than arrow-kt

### 6. Serialization: kotlinx.serialization

**Decision:** Use kotlinx.serialization for all JSON/data serialization.

**Rationale:**
- Kotlin-native, compile-time safe
- Supports Kotlin Multiplatform
- No runtime reflection overhead
- Fits ecosystem constraint (stay in Kotlin land)

**Alternatives considered:**
- Jackson: Java library, not Kotlin-first
- Gson: Runtime reflection, less type-safe

### 7. Web API: Ktor

**Decision:** Use Ktor for REST API with content negotiation, authentication, and WebSocket support for streaming.

**Rationale:**
- Kotlin-native, coroutine-based
- Excellent support for WebSockets (for streaming LLM responses)
- Lightweight and fast
- Easy integration with Exposed R2DBC

**Alternatives considered:**
- Spring Boot: Heavier, more boilerplate
- Vert.x: Lower-level, more complex

### 8. Build System: Gradle 9.4.0 with Kotlin DSL

**Decision:** Use Gradle 9.4.0 with Kotlin DSL for build configuration.

**Rationale:**
- Latest Gradle features and performance improvements
- Kotlin DSL provides type-safe build scripts
- Excellent Kotlin Multiplatform support
- Convention plugins for reusable build logic

**Alternatives considered:**
- Maven: Less flexible, poor Kotlin Multiplatform support
- Gradle Groovy DSL: Less type-safe

## Risks / Trade-offs

### Risk: Koog Maturity
**Risk:** Koog is relatively new; API stability and documentation may be limited.
**Mitigation:** Pin specific version, abstract LLM orchestration behind interface to allow swapping if needed.

### Risk: pgvector Performance
**Risk:** Vector similarity search may be slow with large document sets.
**Mitigation:** Implement indexing strategies, batch processing, and caching for frequently accessed documents.

### Risk: Kotlin Multiplatform Complexity
**Risk:** Multiplatform adds build complexity and potential platform-specific bugs.
**Mitigation:** Start with JVM desktop target only, add other platforms incrementally. Use expect/actual declarations sparingly.

### Risk: LLM Context Window Limits
**Risk:** Large documents may exceed LLM context window, causing truncation or errors.
**Mitigation:** Implement chunking strategies, summarize large documents before embedding, use RAG to retrieve relevant sections.

### Trade-off: Local LLM Only
**Trade-off:** No cloud LLM support limits model choice to what Ollama provides.
**Acceptance:** Privacy and data locality are priorities; cloud LLMs can be added later if needed.

### Trade-off: Desktop UI Only
**Trade-off:** Initial desktop-only implementation limits accessibility.
**Acceptance:** Compose Multiplatform makes adding web/mobile targets straightforward once desktop is stable.

## Migration Plan

Not applicable (new project).

## Open Questions

1. **Ollama Model Selection:** Which default model to use? (llama3.2, mistral, etc.) - **Decision:** Make configurable, default to llama3.2
2. **Embedding Model:** Which model for document embeddings? - **Decision:** Use same model as generation or separate embedding model (configurable)
3. **Vector Indexing Strategy:** What indexing parameters for pgvector? - **Decision:** Start with default HNSW parameters, tune based on performance
4. **Chunking Strategy:** How to chunk large documents? - **Decision:** Implement semantic chunking (paragraph/sentence boundaries)