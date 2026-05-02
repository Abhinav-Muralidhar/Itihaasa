package com.itihaasa.nammakathey.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.itihaasa.nammakathey.model.Badge
import com.itihaasa.nammakathey.model.ExploredPlace
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class ProfileRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun signInWithGoogle(idToken: String) = withContext(Dispatchers.IO) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = firebaseAuth.signInWithCredential(credential).await().user
            ?: error("Google sign-in failed.")

        val userDocument = firestore.collection(USERS_COLLECTION).document(user.uid)
        val snapshot = userDocument.get().await()
        val profileData = mapOf(
            "uid" to user.uid,
            "displayName" to user.displayName.orEmpty(),
            "photoUrl" to user.photoUrl?.toString().orEmpty()
        )

        if (snapshot.exists()) {
            userDocument.set(profileData, SetOptions.merge()).await()
        } else {
            userDocument.set(
                profileData + mapOf(
                    "preferredLang" to "en",
                    "badgesEarned" to emptyList<Map<String, Any>>(),
                    "placesExplored" to emptyList<Map<String, Any>>(),
                    "quizStreak" to 0,
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    fun observeProfile(): Flow<ProfileJourney?> = callbackFlow {
        val user = firebaseAuth.currentUser
        if (user == null || user.isAnonymous) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(
                        ProfileJourney(
                            uid = user.uid,
                            displayName = user.displayName.orEmpty(),
                            photoUrl = user.photoUrl?.toString().orEmpty()
                        )
                    )
                    return@addSnapshotListener
                }

                trySend(
                    ProfileJourney(
                        uid = snapshot.getString("uid").orEmpty().ifBlank { user.uid },
                        displayName = snapshot.getString("displayName").orEmpty()
                            .ifBlank { user.displayName.orEmpty() },
                        photoUrl = snapshot.getString("photoUrl").orEmpty()
                            .ifBlank { user.photoUrl?.toString().orEmpty() },
                        preferredLang = snapshot.getString("preferredLang").orEmpty().ifBlank { "en" },
                        quizStreak = snapshot.getLong("quizStreak")?.toInt() ?: 0,
                        joinedAt = snapshot.get("joinedAt").toEpochMillis(),
                        badgesEarned = snapshot.getListOfMaps("badgesEarned").mapNotNull { it.toBadge() },
                        placesExplored = snapshot.getListOfMaps("placesExplored").mapNotNull { it.toExploredPlace() }
                    )
                )
            }

        awaitClose { registration.remove() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.getListOfMaps(
        field: String
    ): List<Map<String, Any?>> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? Map<*, *> }
            ?.map { raw -> raw.entries.associate { it.key.toString() to it.value } }
            .orEmpty()
    }

    private fun Map<String, Any?>.toBadge(): Badge? {
        val placeId = this["placeId"] as? String ?: return null
        return Badge(
            placeId = placeId,
            placeName = this["placeName"] as? String ?: "",
            district = this["district"] as? String ?: "",
            earnedAt = this["earnedAt"].toEpochMillis()
        )
    }

    private fun Map<String, Any?>.toExploredPlace(): ExploredPlace? {
        val placeId = this["placeId"] as? String ?: return null
        return ExploredPlace(
            placeId = placeId,
            name = this["name"] as? String ?: "",
            timestamp = this["timestamp"].toEpochMillis(),
            badgeEarned = this["badgeEarned"] as? Boolean ?: false
        )
    }

    private fun Any?.toEpochMillis(): Long {
        return when (this) {
            is Long -> this
            is Double -> toLong()
            is Timestamp -> toDate().time
            else -> 0L
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}

data class ProfileJourney(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val preferredLang: String = "en",
    val badgesEarned: List<Badge> = emptyList(),
    val placesExplored: List<ExploredPlace> = emptyList(),
    val quizStreak: Int = 0,
    val joinedAt: Long = 0L
)
