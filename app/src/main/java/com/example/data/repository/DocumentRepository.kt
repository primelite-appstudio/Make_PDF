package com.example.data.repository

import com.example.data.local.DocumentDao
import com.example.data.model.ConvertedDocument
import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val dao: DocumentDao) {
    val allDocuments: Flow<List<ConvertedDocument>> = dao.getAllDocuments()

    suspend fun getDocumentById(id: Long): ConvertedDocument? = dao.getDocumentById(id)

    suspend fun saveDocument(doc: ConvertedDocument): Long = dao.insertDocument(doc)

    suspend fun deleteDocument(id: Long) = dao.deleteDocumentById(id)

    suspend fun clearHistory() = dao.clearAll()
}
