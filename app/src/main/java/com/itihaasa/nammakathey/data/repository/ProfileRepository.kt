package com.itihaasa.nammakathey.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.itihaasa.nammakathey.model.Badge
import com.itihaasa.nammakathey.model.ExplorerRank
import com.itihaasa.nammakathey.model.ExploredPlace
import com.itihaasa.nammakathey.model.toExplorerRank
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
    suspend fun signInWithEmail(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val user = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Email sign-in failed.")

        ensureUserDocument(
            uid = user.uid,
            displayName = user.displayName.orEmpty(),
            photoUrl = user.photoUrl?.toString().orEmpty()
        )
    }

    suspend fun signUpWithEmail(
        name: String,
        email: String,
        password: String
    ): Boolean = withContext(Dispatchers.IO) {
        val user = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Email sign-up failed.")

        if (name.isNotBlank()) {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
            ).await()
        }

        ensureUserDocument(
            uid = user.uid,
            displayName = name.trim(),
            photoUrl = user.photoUrl?.toString().orEmpty()
        )
    }

    suspend fun signInWithGoogle(idToken: String): Boolean = withContext(Dispatchers.IO) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = firebaseAuth.signInWithCredential(credential).await().user
            ?: error("Google sign-in failed.")

        ensureUserDocument(
            uid = user.uid,
            displayName = user.displayName.orEmpty(),
            photoUrl = user.photoUrl?.toString().orEmpty()
        )
    }

    private suspend fun ensureUserDocument(
        uid: String,
        displayName: String,
        photoUrl: String
    ): Boolean {
        val userDocument = firestore.collection(USERS_COLLECTION).document(uid)
        val snapshot = userDocument.get().await()
        val profileData = mutableMapOf<String, Any>("uid" to uid).apply {
            if (displayName.isNotBlank()) put("displayName", displayName)
            if (photoUrl.isNotBlank()) put("photoUrl", photoUrl)
        }

        if (snapshot.exists()) {
            userDocument.set(profileData, SetOptions.merge()).await()
            return snapshot.getBoolean("profileComplete")
                ?: !snapshot.getString("homeDistrict").isNullOrBlank()
        } else {
            userDocument.set(
                profileData + mapOf(
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "preferredLang" to "en",
                    "homeDistrict" to "",
                    "profileComplete" to false,
                    "badgesEarned" to emptyList<Map<String, Any>>(),
                    "placesExplored" to emptyList<Map<String, Any>>(),
                    "quizStreak" to 0,
                    "explorerRank" to ExplorerRank.NAVARANGA.name,
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return false
        }
    }

    suspend fun completeProfileSetup(
        name: String,
        homeDistrict: String,
        preferredLang: String
    ) = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser
            ?: error("Sign in before setting up your profile.")
        if (user.isAnonymous) error("Sign in before setting up your profile.")

        val trimmedName = name.trim()
        if (trimmedName.isNotBlank()) {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedName)
                    .build()
            ).await()
        }

        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    "uid" to user.uid,
                    "displayName" to trimmedName.ifBlank { user.displayName.orEmpty() },
                    "photoUrl" to user.photoUrl?.toString().orEmpty(),
                    "homeDistrict" to homeDistrict,
                    "preferredLang" to preferredLang,
                    "profileComplete" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    fun signOut() {
        firebaseAuth.signOut()
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

                val heroBadges = snapshot.getListOfMaps("badgesEarned").mapNotNull { it.toBadge() }
                val districtBadges = snapshot.getListOfMaps("districtBadges").mapNotNull { it.toBadge() }

                trySend(
                    ProfileJourney(
                        uid = snapshot.getString("uid").orEmpty().ifBlank { user.uid },
                        displayName = snapshot.getString("displayName").orEmpty()
                            .ifBlank { user.displayName.orEmpty() },
                        photoUrl = snapshot.getString("photoUrl").orEmpty()
                            .ifBlank { user.photoUrl?.toString().orEmpty() },
                        preferredLang = snapshot.getString("preferredLang").orEmpty().ifBlank { "en" },
                        homeDistrict = snapshot.getString("homeDistrict").orEmpty(),
                        profileComplete = snapshot.getBoolean("profileComplete")
                            ?: !snapshot.getString("homeDistrict").isNullOrBlank(),
                        quizStreak = snapshot.getLong("quizStreak")?.toInt() ?: 0,
                        completedHeroIds = snapshot.getStringList("completedHeroIds")
                            .ifEmpty { snapshot.getPlaceIdsFromMaps("badgesEarned") }
                            .toSet(),
                        unlockedDistricts = snapshot.getStringList("unlockedDistricts").toSet(),
                        explorerRank = snapshot.pickExplorerRank(
                            badgeCount = snapshot.getListOfMaps("badgesEarned").size
                        ),
                        joinedAt = snapshot.get("joinedAt").toEpochMillis(),
                        badgesEarned = (heroBadges + districtBadges).distinctBy { it.placeId },
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

    private fun com.google.firebase.firestore.DocumentSnapshot.getStringList(
        field: String
    ): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.getPlaceIdsFromMaps(
        field: String
    ): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? Map<*, *> }
            ?.mapNotNull { it["placeId"] as? String }
            .orEmpty()
    }

    private fun Map<String, Any?>.toBadge(): Badge? {
        val placeId = this["placeId"] as? String ?: return null
        return Badge(
            placeId = placeId,
            placeName = this["placeName"] as? String ?: "",
            district = this["district"] as? String ?: "",
            earnedAt = this["earnedAt"].toEpochMillis(),
            badgeType = this["badgeType"] as? String ?: "hero"
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

    private fun com.google.firebase.firestore.DocumentSnapshot.pickExplorerRank(
        badgeCount: Int
    ): ExplorerRank {
        val derivedRank = badgeCount.toExplorerRank()
        val storedRank = getString("explorerRank")
            ?.let { runCatching { ExplorerRank.valueOf(it) }.getOrNull() }
        return when {
            storedRank == null -> derivedRank
            storedRank.badgesRequired >= derivedRank.badgesRequired -> storedRank
            else -> derivedRank
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
    val homeDistrict: String = "",
    val profileComplete: Boolean = false,
    val badgesEarned: List<Badge> = emptyList(),
    val placesExplored: List<ExploredPlace> = emptyList(),
    val completedHeroIds: Set<String> = emptySet(),
    val unlockedDistricts: Set<String> = emptySet(),
    val quizStreak: Int = 0,
    val explorerRank: ExplorerRank = ExplorerRank.NAVARANGA,
    val joinedAt: Long = 0L
)
