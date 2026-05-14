package com.itihaasa.nammakathey.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.itihaasa.nammakathey.model.StoryCatalogEntry
import com.itihaasa.nammakathey.model.toExplorerRank
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class StoryProgressRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun getProgressState(localHomeDistrict: String?): StoryProgressState {
        val user = firebaseAuth.currentUser
        if (user == null || user.isAnonymous) {
            return StoryProgressState(
                homeDistrict = localHomeDistrict?.takeIf { it.isNotBlank() }
            )
        }

        val snapshot = firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .get()
            .await()
        val profileHomeDistrict = snapshot.getString("homeDistrict")
            ?.takeIf { it.isNotBlank() }
        val homeDistrict = profileHomeDistrict ?: localHomeDistrict?.takeIf { it.isNotBlank() }
        val profileActiveDistrict = snapshot.getString("activeDistrict")
            ?.takeIf { it.isNotBlank() }
        val activeDistrict = profileActiveDistrict ?: homeDistrict
        val remoteCompleted = snapshot.getStringList("completedHeroIds")
            .ifEmpty { snapshot.getPlaceIdsFromMaps("badgesEarned") }
            .toSet()
        val remoteUnlocked = snapshot.getStringList("unlockedDistricts").toSet()
        val completedHeroIds = remoteCompleted
        val unlockedDistricts =
            remoteUnlocked + listOfNotNull(homeDistrict, activeDistrict)

        return StoryProgressState(
            homeDistrict = homeDistrict,
            activeDistrict = activeDistrict,
            completedHeroIds = completedHeroIds,
            unlockedDistricts = unlockedDistricts
        )
    }

    suspend fun markHeroCompleted(hero: StoryCatalogEntry) {
        if (hero.placeId.isBlank()) return
        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return

        val userDocument = firestore.collection(USERS_COLLECTION).document(user.uid)
        val snapshot = userDocument.get().await()
        val earnedBadgeIds = snapshot.getPlaceIdsFromMaps("badgesEarned").toSet()
        val hasHeroBadge = hero.placeId in earnedBadgeIds
        val nextBadgeCount = if (hasHeroBadge) {
            earnedBadgeIds.size
        } else {
            earnedBadgeIds.size + 1
        }
        val nextRank = nextBadgeCount.toExplorerRank()

        if (hasHeroBadge) {
            userDocument.set(
                mapOf(
                    "completedHeroIds" to FieldValue.arrayUnion(hero.placeId),
                    "explorerRank" to nextRank.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            return
        }

        val now = System.currentTimeMillis()
        userDocument.set(
            mapOf(
                "completedHeroIds" to FieldValue.arrayUnion(hero.placeId),
                "badgesEarned" to FieldValue.arrayUnion(
                    mapOf(
                        "placeId" to hero.placeId,
                        "placeName" to hero.title,
                        "district" to hero.district,
                        "earnedAt" to now,
                        "badgeType" to "hero"
                    )
                ),
                "placesExplored" to FieldValue.arrayUnion(
                    mapOf(
                        "placeId" to hero.placeId,
                        "name" to hero.title,
                        "timestamp" to now,
                        "badgeEarned" to true
                    )
                ),
                "quizStreak" to FieldValue.increment(1),
                "explorerRank" to nextRank.name,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun markDistrictCompleted(districtName: String) {
        if (districtName.isBlank()) return
        val districtBadgeId = "district:${districtName.lowercase()}"

        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return

        val userDocument = firestore.collection(USERS_COLLECTION).document(user.uid)
        val snapshot = userDocument.get().await()
        if (districtBadgeId in snapshot.getPlaceIdsFromMaps("districtBadges")) return
        val now = System.currentTimeMillis()
        userDocument.set(
            mapOf(
                "districtBadges" to FieldValue.arrayUnion(
                    mapOf(
                        "placeId" to districtBadgeId,
                        "placeName" to "$districtName District Crest",
                        "district" to districtName,
                        "earnedAt" to now,
                        "badgeType" to "district"
                    )
                ),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun unlockDistrict(districtName: String) {
        if (districtName.isBlank()) return
        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    "unlockedDistricts" to FieldValue.arrayUnion(districtName),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun setActiveDistrict(districtName: String) {
        if (districtName.isBlank()) return
        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return

        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    "activeDistrict" to districtName,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.getStringList(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.getPlaceIdsFromMaps(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? Map<*, *> }
            ?.mapNotNull { it["placeId"] as? String }
            .orEmpty()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}

data class StoryProgressState(
    val homeDistrict: String? = null,
    val activeDistrict: String? = null,
    val completedHeroIds: Set<String> = emptySet(),
    val unlockedDistricts: Set<String> = emptySet()
)
