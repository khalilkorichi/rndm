package com.rndm.app.data.remote.firebase

import android.util.Log
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rndm.app.data.remote.firebase.dto.FirestoreUserDto
import com.rndm.app.domain.model.UserProfile
import com.rndm.app.domain.model.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    companion object {
        val MASTER_ADMIN_EMAILS = setOf(
            "khalil.xdz@gmail.com",
            "abdousaad430@gmail.com"
        )
    }

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    fun isMasterAdminEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val clean = email.trim().lowercase()
        return MASTER_ADMIN_EMAILS.contains(clean) || clean.startsWith("admin@")
    }

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInAnonymously(): Result<String> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                Result.success(user.uid)
            } else {
                val authResult = firebaseAuth.signInAnonymously().await()
                val uid = authResult.user?.uid ?: throw IllegalStateException("User UID is null")
                Result.success(uid)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<String> {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("يرجى إدخال صيغة بريد إلكتروني صحيحة ومعتمدة (example@domain.com)"))
        }

        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user ?: throw IllegalStateException("User is null after sign up")
            val fallbackName = cleanEmail.substringBefore("@")
            val finalDisplayName = displayName.trim().ifBlank { fallbackName }

            // Update Firebase Auth profile displayName
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(finalDisplayName)
                .build()
            user.updateProfile(profileUpdates).await()

            // Save user document in Firestore users collection
            val role = if (isMasterAdminEmail(cleanEmail)) "admin" else "user"
            val userDto = FirestoreUserDto(
                uid = user.uid,
                email = cleanEmail,
                username = fallbackName,
                displayName = finalDisplayName,
                role = role,
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("users").document(user.uid).set(userDto).await()

            Log.d("AUTH_RNDM", "Signed up successfully with email: ${user.uid} ($cleanEmail, role=$role)")
            Result.success(user.uid)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("AUTH_RNDM", "Failed to sign up", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<String> {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("يرجى إدخال بريد إلكتروني صالح"))
        }

        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user ?: throw IllegalStateException("User UID is null")

            // Ensure Firestore user document exists and has the correct role
            val fallbackName = cleanEmail.substringBefore("@")
            val isMaster = isMasterAdminEmail(cleanEmail)
            val userRef = firestore.collection("users").document(user.uid)
            val userDoc = userRef.get().await()

            if (!userDoc.exists()) {
                val userDto = FirestoreUserDto(
                    uid = user.uid,
                    email = cleanEmail,
                    username = fallbackName,
                    displayName = user.displayName ?: fallbackName,
                    role = if (isMaster) "admin" else "user",
                    createdAt = System.currentTimeMillis()
                )
                userRef.set(userDto).await()
            } else if (isMaster && userDoc.getString("role") != "admin") {
                userRef.update("role", "admin").await()
            }

            Log.d("AUTH_RNDM", "Signed in successfully: ${user.uid} ($cleanEmail)")
            Result.success(user.uid)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("AUTH_RNDM", "Failed to sign in", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            signInAnonymously()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun determineUserRole(): UserRole {
        val user = firebaseAuth.currentUser ?: return UserRole.GUEST
        if (user.isAnonymous) {
            return UserRole.GUEST
        }

        val email = user.email.orEmpty().lowercase()
        if (isMasterAdminEmail(email)) {
            return UserRole.ADMIN
        }

        return try {
            val tokenResult = user.getIdToken(false).await()
            val roleClaim = tokenResult.claims["role"] as? String
            if (roleClaim.equals("admin", ignoreCase = true)) {
                return UserRole.ADMIN
            }

            // Check Firestore users collection
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            if (userDoc.exists()) {
                val roleStr = userDoc.getString("role")
                if (roleStr.equals("admin", ignoreCase = true)) {
                    UserRole.ADMIN
                } else {
                    UserRole.USER
                }
            } else {
                if (email.isNotBlank()) UserRole.USER else UserRole.GUEST
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (isMasterAdminEmail(email)) UserRole.ADMIN else if (email.isNotBlank()) UserRole.USER else UserRole.GUEST
        }
    }

    suspend fun getCurrentUserProfile(): UserProfile? {
        val user = firebaseAuth.currentUser ?: return null
        if (user.isAnonymous) return null

        val email = user.email.orEmpty().lowercase()
        val isMaster = isMasterAdminEmail(email)

        return try {
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            if (userDoc.exists()) {
                val userDto = userDoc.toObject(FirestoreUserDto::class.java)
                if (userDto != null) {
                    val role = if (isMaster || userDto.role.equals("admin", ignoreCase = true)) {
                        UserRole.ADMIN
                    } else {
                        UserRole.USER
                    }
                    return UserProfile(
                        uid = userDto.uid.ifBlank { user.uid },
                        email = userDto.email.ifBlank { user.email.orEmpty() },
                        username = userDto.username.ifBlank { user.email?.substringBefore("@").orEmpty() },
                        displayName = userDto.displayName.ifBlank { user.displayName ?: user.email?.substringBefore("@") ?: "مستخدم" },
                        role = role,
                        createdAt = userDto.createdAt
                    )
                }
            }
            UserProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                username = user.email?.substringBefore("@").orEmpty(),
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "مستخدم",
                role = if (isMaster) UserRole.ADMIN else determineUserRole(),
                createdAt = user.metadata?.creationTimestamp ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            UserProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                username = user.email?.substringBefore("@").orEmpty(),
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "مستخدم",
                role = if (isMaster) UserRole.ADMIN else determineUserRole()
            )
        }
    }

    // ── User Management & Promotion (Admin Only) ──────────────────────────

    fun observeAllUsers(): Flow<List<FirestoreUserDto>> = callbackFlow {
        val listener = firestore.collection("users")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("AUTH_RNDM", "Error observing users: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.toObjects(FirestoreUserDto::class.java)
                    trySend(users)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateUserRole(targetUid: String, newRole: String): Result<Unit> {
        return try {
            firestore.collection("users").document(targetUid).update("role", newRole).await()
            Log.d("AUTH_RNDM", "Updated role for $targetUid to $newRole")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("AUTH_RNDM", "Failed to update role for $targetUid", e)
            Result.failure(e)
        }
    }

    suspend fun promoteUserByEmail(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("يرجى إدخال صيغة بريد إلكتروني صالحة"))
        }

        return try {
            val query = firestore.collection("users")
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get().await()

            if (!query.isEmpty) {
                val docId = query.documents.first().id
                firestore.collection("users").document(docId).update("role", "admin").await()
                Log.d("AUTH_RNDM", "Promoted user $cleanEmail ($docId) to admin")
                Result.success(Unit)
            } else {
                // Pre-register user email as admin
                val placeholderId = "user_${UUID.randomUUID().toString().take(12)}"
                val userDto = FirestoreUserDto(
                    uid = placeholderId,
                    email = cleanEmail,
                    username = cleanEmail.substringBefore("@"),
                    displayName = cleanEmail.substringBefore("@"),
                    role = "admin",
                    createdAt = System.currentTimeMillis()
                )
                firestore.collection("users").document(placeholderId).set(userDto).await()
                Log.d("AUTH_RNDM", "Pre-registered admin role for $cleanEmail")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("AUTH_RNDM", "Failed to promote user by email $cleanEmail", e)
            Result.failure(e)
        }
    }
}
