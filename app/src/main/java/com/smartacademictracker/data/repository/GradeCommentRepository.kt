package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.GradeComment
import com.smartacademictracker.data.model.CommentType
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeCommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val commentsCollection = firestore.collection("grade_comments")

    suspend fun createComment(comment: GradeComment): Result<GradeComment> {
        return try {
            val docRef = commentsCollection.add(comment).await()
            val createdComment = comment.copy(id = docRef.id)
            commentsCollection.document(docRef.id).set(createdComment).await()
            Result.success(createdComment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentsByGrade(gradeId: String): Result<List<GradeComment>> {
        return try {
            val snapshot = commentsCollection
                .whereEqualTo("gradeId", gradeId)
                .orderBy("createdAt")
                .get()
                .await()
            val comments = snapshot.toObjects(GradeComment::class.java)
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentsByStudent(studentId: String): Result<List<GradeComment>> {
        return try {
            val snapshot = commentsCollection
                .whereEqualTo("studentId", studentId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val comments = snapshot.toObjects(GradeComment::class.java)
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentsByTeacher(teacherId: String): Result<List<GradeComment>> {
        return try {
            val snapshot = commentsCollection
                .whereEqualTo("teacherId", teacherId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val comments = snapshot.toObjects(GradeComment::class.java)
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateComment(commentId: String, updatedComment: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "comment" to updatedComment,
                "updatedAt" to System.currentTimeMillis()
            )
            commentsCollection.document(commentId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(commentId: String): Result<Unit> {
        return try {
            commentsCollection.document(commentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentById(commentId: String): Result<GradeComment> {
        return try {
            val document = commentsCollection.document(commentId).get().await()
            val comment = document.toObject(GradeComment::class.java)
                ?: throw Exception("Comment not found")
            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentsByType(commentType: CommentType): Result<List<GradeComment>> {
        return try {
            val snapshot = commentsCollection
                .whereEqualTo("commentType", commentType.name)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val comments = snapshot.toObjects(GradeComment::class.java)
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
