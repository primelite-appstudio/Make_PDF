package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ConvertedDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM converted_documents ORDER BY timestamp DESC")
    fun getAllDocuments(): Flow<List<ConvertedDocument>>

    @Query("SELECT * FROM converted_documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): ConvertedDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: ConvertedDocument): Long

    @Query("DELETE FROM converted_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("DELETE FROM converted_documents")
    suspend fun clearAll()
}
