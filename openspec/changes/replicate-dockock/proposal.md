## Why

DockOck is a Rust-based tool that converts business documents (Word, Excel, Visio) into Gherkin feature files using local LLMs. Replicating this in Kotlin provides better integration with enterprise Kotlin ecosystems, leverages Kotlin Multiplatform for cross-platform UI, and enables advanced agent orchestration through Koog's graph-based workflows and streaming capabilities.

## What Changes

- **New project structure**: Kotlin Multiplatform project with shared business logic and platform-specific UIs
- **Document parsing modules**: Kotlin implementations for .docx, .xlsx, .vsdx file extraction
- **LLM integration**: Koog-based agent orchestration with streaming and parallel tool execution
- **RAG system**: pgvector + Exposed R2DBC for cross-file context sharing and semantic search
- **Web API**: Ktor-based REST API for document processing and management
- **Desktop UI**: Compose Multiplatform desktop application matching DockOck's egui interface
- **Error handling**: arrow-kt for functional error handling and coroutine helpers
- **Build system**: Gradle 9.4.0 with Kotlin DSL and Kotlin 2.3.10 toolchain

## Capabilities

### New Capabilities

- `document-parsing`: Extract text content from Word (.docx), Excel (.xlsx), and Visio (.vsdx) files using Kotlin libraries
- `llm-orchestration`: Koog-based agent workflows with streaming responses, parallel tool execution, and graph-based coordination
- `rag-context-store`: pgvector integration with Exposed R2DBC for storing and retrieving document embeddings with semantic search
- `gherkin-generation`: Convert extracted document content into Gherkin feature files using LLM with cross-file context
- `web-api`: Ktor REST API endpoints for document upload, processing, status tracking, and result retrieval
- `desktop-ui`: Compose Multiplatform desktop UI with file selection, progress tracking, log panel, and result management
- `error-handling`: arrow-kt integration for functional error handling, Either types, and coroutine-safe operations

### Modified Capabilities

None (this is a new project)

## Impact

- **New dependencies**: Ktor, Koog, pgvector, Exposed R2DBC, Compose Multiplatform, arrow-kt, kotlinx.serialization
- **Build system**: Gradle 9.4.0 with Kotlin DSL, multi-platform configuration
- **Runtime**: Requires JDK 25, PostgreSQL with pgvector extension
- **Architecture**: Shared Kotlin module for business logic, platform-specific modules for UI and native integrations
- **Development workflow**: Kotlin-first approach with coroutines for async operations, functional programming patterns via arrow-kt