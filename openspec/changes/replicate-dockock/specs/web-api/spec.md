## ADDED Requirements

### Requirement: Provide REST API for document upload
The system SHALL provide Ktor REST API endpoints for uploading documents.

#### Scenario: Upload single document
- **GIVEN** the API is running
- **WHEN** a client POSTs a document to /api/documents
- **THEN** the document is stored
- **AND** the API returns a document ID

#### Scenario: Upload multiple documents
- **GIVEN** the API is running
- **WHEN** a client POSTs multiple documents to /api/documents/batch
- **THEN** all documents are stored
- **AND** the API returns document IDs for all files

#### Scenario: Validate document type
- **GIVEN** the API is running
- **WHEN** a client uploads an unsupported file type
- **THEN** the API returns a 400 error
- **AND** the error message indicates supported types

### Requirement: Provide document processing endpoint
The system SHALL provide API endpoints to trigger document processing.

#### Scenario: Process single document
- **GIVEN** a stored document
- **WHEN** a client POSTs to /api/documents/{id}/process
- **THEN** the document is parsed and Gherkin is generated
- **AND** the API returns a processing job ID

#### Scenario: Process batch of documents
- **GIVEN** multiple stored documents
- **WHEN** a client POSTs to /api/documents/batch-process
- **THEN** all documents are processed
- **AND** the API returns job IDs for all documents

#### Scenario: Process with cross-file context
- **GIVEN** multiple documents to process
- **WHEN** processing is triggered
- **THEN** each document includes context from previously processed documents
- **AND** the processing order is preserved

### Requirement: Provide status tracking endpoint
The system SHALL provide API endpoints to track processing status.

#### Scenario: Get document status
- **GIVEN** a document being processed
- **WHEN** a client GETs /api/documents/{id}/status
- **THEN** the API returns the current status (pending, processing, completed, failed)
- **AND** includes progress percentage

#### Scenario: Get batch status
- **GIVEN** multiple documents being processed
- **WHEN** a client GETs /api/jobs/{jobId}/status
- **THEN** the API returns status for all documents in the batch
- **AND** includes overall progress

#### Scenario: Handle invalid document ID
- **GIVEN** the API is running
- **WHEN** a client requests status for a non-existent document
- **THEN** the API returns a 404 error
- **AND** the error message indicates the document was not found

### Requirement: Provide result retrieval endpoint
The system SHALL provide API endpoints to retrieve generated Gherkin content.

#### Scenario: Get single result
- **GIVEN** a document with generated Gherkin
- **WHEN** a client GETs /api/documents/{id}/result
- **THEN** the API returns the Gherkin content
- **AND** the content type is text/plain

#### Scenario: Get batch results
- **GIVEN** multiple documents with generated Gherkin
- **WHEN** a client GETs /api/jobs/{jobId}/results
- **THEN** the API returns all Gherkin contents
- **AND** results are keyed by document ID

#### Scenario: Get result metadata
- **GIVEN** a processed document
- **WHEN** a client GETs /api/documents/{id}
- **THEN** the API returns document metadata
- **AND** includes file name, status, and processing timestamp

### Requirement: Support WebSocket for streaming
The system SHALL support WebSocket connections for real-time streaming of processing progress.

#### Scenario: Stream processing progress
- **GIVEN** a client connected via WebSocket
- **WHEN** a document is being processed
- **THEN** the server sends progress updates
- **AND** updates include current step and percentage

#### Scenario: Stream LLM tokens
- **GIVEN** a client connected via WebSocket
- **WHEN** the LLM generates Gherkin content
- **THEN** the server streams tokens as they are generated
- **AND** the client can display partial content

#### Scenario: Handle WebSocket disconnection
- **GIVEN** a client connected via WebSocket
- **WHEN** the client disconnects
- **THEN** the server stops sending updates
- **AND** processing continues on the server

### Requirement: Implement authentication
The system SHALL implement authentication for API endpoints.

#### Scenario: Authenticate with API key
- **GIVEN** the API is configured with an API key
- **WHEN** a client requests an endpoint without the API key
- **THEN** the API returns a 401 error
- **AND** the error message indicates authentication is required

#### Scenario: Valid API key
- **GIVEN** the API is configured with an API key
- **WHEN** a client requests an endpoint with a valid API key
- **THEN** the request proceeds
- **AND** the API returns the expected response

### Requirement: Implement content negotiation
The system SHALL support multiple response formats via content negotiation.

#### Scenario: Return JSON by default
- **GIVEN** a client requests an endpoint
- **WHEN** no Accept header is provided
- **THEN** the API returns JSON responses

#### Scenario: Return JSON when requested
- **GIVEN** a client requests an endpoint with Accept: application/json
- **WHEN** the request is processed
- **THEN** the API returns JSON responses

#### Scenario: Return plain text when requested
- **GIVEN** a client requests Gherkin content with Accept: text/plain
- **WHEN** the request is processed
- **THEN** the API returns plain text responses

### Requirement: Handle errors gracefully
The system SHALL return appropriate HTTP status codes and error messages for all error conditions.

#### Scenario: Return 400 for bad requests
- **GIVEN** a client sends an invalid request
- **WHEN** the API processes the request
- **THEN** the API returns a 400 status
- **AND** the error message describes the validation error

#### Scenario: Return 500 for server errors
- **GIVEN** an unexpected server error occurs
- **WHEN** the API processes a request
- **THEN** the API returns a 500 status
- **AND** the error message is logged for debugging

#### Scenario: Return structured error responses
- **GIVEN** an error occurs
- **WHEN** the API returns an error response
- **THEN** the response includes error code and message
- **AND** the response is in JSON format