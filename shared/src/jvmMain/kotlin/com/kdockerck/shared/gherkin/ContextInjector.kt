package com.kdockerck.shared.gherkin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.kdockerck.shared.domain.ParsedContent
import com.kdockerck.shared.rag.CrossFileContextRetriever
import com.kdockerck.shared.rag.ContextWithMetadata
import com.kdockerck.shared.errors.AppError

class ContextInjector(
    private val contextRetriever: CrossFileContextRetriever
) {
    
    suspend fun injectContext(
        currentDocument: ParsedContent,
        maxContextLength: Int = 2000
    ): Either<AppError, String> {
        return contextRetriever.retrieveContextSummary(currentDocument).map { summary ->
            if (summary.length > maxContextLength) {
                summary.take(maxContextLength) + "\n\n... (context truncated)"
            } else {
                summary
            }
        }
    }
    
    suspend fun injectContextWithMetadata(
        currentDocument: ParsedContent,
        maxContextLength: Int = 2000
    ): Either<AppError, ContextWithMetadata> {
        return contextRetriever.retrieveContextWithMetadata(currentDocument).map { contextWithMetadata ->
            val truncatedText = if (contextWithMetadata.contextText.length > maxContextLength) {
                contextWithMetadata.contextText.take(maxContextLength) + "\n\n... (context truncated)"
            } else {
                contextWithMetadata.contextText
            }
            
            contextWithMetadata.copy(contextText = truncatedText)
        }
    }
    
    suspend fun injectRelevantContext(
        currentDocument: ParsedContent,
        query: String,
        maxResults: Int = 5
    ): Either<AppError, String> {
        return contextRetriever.retrieveRelevantContext(query, maxResults).map { results ->
            buildString {
                appendLine("Relevant Context:")
                appendLine("Number of relevant documents: ${results.size}")
                appendLine()
                
                results.forEach { result ->
                    appendLine("Document: ${result.embedding.documentId}")
                    appendLine("Similarity Score: ${"%.2f".format(result.score)}")
                    appendLine("Content: ${result.embedding.text.take(200)}...")
                    appendLine()
                }
            }
        }
    }
    
    suspend fun injectContextForMultipleDocuments(
        documents: List<ParsedContent>,
        excludeDocumentIds: Set<String> = emptySet(),
        maxContextLength: Int = 2000
    ): Either<AppError, Map<String, String>> {
        return contextRetriever.retrieveContextForMultipleDocuments(documents, excludeDocumentIds).map { contextMap ->
            contextMap.mapValues { (documentId, results) ->
                val contextText = buildString {
                    appendLine("Cross-Document Context for $documentId:")
                    appendLine("Number of relevant documents: ${results.size}")
                    appendLine()
                    
                    results.forEach { result ->
                        appendLine("Document: ${result.embedding.documentId}")
                        appendLine("Similarity Score: ${"%.2f".format(result.score)}")
                        appendLine("Content: ${result.embedding.text.take(200)}...")
                        appendLine()
                    }
                }
                
                if (contextText.length > maxContextLength) {
                    contextText.take(maxContextLength) + "\n\n... (context truncated)"
                } else {
                    contextText
                }
            }
        }
    }
    
    suspend fun injectMinimalContext(
        currentDocument: ParsedContent,
        maxDocuments: Int = 3
    ): Either<AppError, String> {
        return contextRetriever.retrieveContext(currentDocument).map { results ->
            val topResults = results.take(maxDocuments)
            
            buildString {
                appendLine("Relevant Context (Top ${topResults.size} Documents):")
                appendLine()
                
                topResults.forEach { result ->
                    appendLine("Document: ${result.embedding.documentId}")
                    appendLine("Score: ${"%.2f".format(result.score)}")
                    appendLine(result.embedding.text.take(100))
                    appendLine()
                }
            }
        }
    }
    
    fun formatContextForPrompt(context: String): String {
        return """
            ---
            Cross-File Context:
            ---
            $context
            ---
            End of Context
            ---
        """.trimIndent()
    }
    
    fun formatContextWithSources(context: String, sources: List<String>): String {
        val sourcesText = sources.joinToString("\n") { "- $it" }
        
        return """
            ---
            Cross-File Context:
            Sources:
            $sourcesText
            ---
            $context
            ---
            End of Context
            ---
        """.trimIndent()
    }
}