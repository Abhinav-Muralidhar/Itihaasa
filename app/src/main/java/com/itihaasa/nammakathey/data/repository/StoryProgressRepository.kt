package com.itihaasa.nammakathey.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.itihaasa.nammakathey.model.StoryCatalogEntry
import com.itihaasa.nammakathey.model.toExplorerRank
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class StoryProgressRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val preferences = context.getSharedPreferences(STORY_PROGRESS_PREFS, Context.MODE_PRIVATE)
    private val legacyPreferences = context.getSharedPreferences(LEGACY_STORY_PROGRESS_PREFS, Context.MODE_PRIVATE)

    fun getCompletedHeroIds(): Set<String> =
        preferences.getStringSet(KEY_COMPLETED_HERO_IDS, emptySet()).orEmpty() +
            legacyPreferences.getStringSet(KEY_COMPLETED_HERO_IDS, emptySet()).orEmpty()

    fun markHeroCompletedLocally(hero: StoryCatalogEntry): Set<String> {
        if (hero.placeId.isBlank()) return getCompletedHeroIds()
        val updated = getCompletedHeroIds() + hero.placeId
        val earnedAtKey = KEY_COMPLETED_HERO_EARNED_AT_PREFIX + hero.placeId
        val earnedAt = preferences.getLong(earnedAtKey, 0L).takeIf { it > 0L }
            ?: System.currentTimeMillis()
        preferences.edit()
            .putStringSet(KEY_COMPLETED_HERO_IDS, updated)
            .putLong(earnedAtKey, earnedAt)
            .apply()
        return updated
    }

    suspend fun getProgressState(localHomeDistrict: String?): StoryProgressState {
        val localCompleted = getCompletedHeroIds()
        val localUnlocked = preferences.getStringSet(KEY_UNLOCKED_DISTRICTS, emptySet()).orEmpty() +
            legacyPreferences.getStringSet(KEY_UNLOCKED_DISTRICTS, emptySet()).orEmpty()
        val manualUnlocked = preferences.getStringSet(KEY_MANUAL_UNLOCKED_DISTRICTS, emptySet()).orEmpty() +
            legacyPreferences.getStringSet(KEY_MANUAL_UNLOCKED_DISTRICTS, emptySet()).orEmpty()
        val localActiveDistrict = (
            preferences.getString(KEY_ACTIVE_DISTRICT, null)
                ?: legacyPreferences.getString(KEY_ACTIVE_DISTRICT, null)
            )?.takeIf { it.isNotBlank() }
        val user = firebaseAuth.currentUser
        if (user == null || user.isAnonymous) {
            val homeDistrict = localHomeDistrict?.takeIf { it.isNotBlank() }
            val activeDistrict = localActiveDistrict ?: homeDistrict
            return StoryProgressState(
                homeDistrict = homeDistrict,
                activeDistrict = activeDistrict,
                completedHeroIds = localCompleted,
                unlockedDistricts = localUnlocked + manualUnlocked + listOfNotNull(homeDistrict, activeDistrict)
            )
        }

        val snapshot = runCatching {
            firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .get()
            .await()
        }.getOrElse {
            val homeDistrict = localHomeDistrict?.takeIf { it.isNotBlank() }
            val activeDistrict = localActiveDistrict ?: homeDistrict
            return StoryProgressState(
                homeDistrict = homeDistrict,
                activeDistrict = activeDistrict,
                completedHeroIds = localCompleted,
                unlockedDistricts = localUnlocked + manualUnlocked + listOfNotNull(homeDistrict, activeDistrict)
            )
        }
        val profileHomeDistrict = snapshot.getString("homeDistrict")
            ?.takeIf { it.isNotBlank() }
        val homeDistrict = profileHomeDistrict ?: localHomeDistrict?.takeIf { it.isNotBlank() }
        val profileActiveDistrict = snapshot.getString("activeDistrict")
            ?.takeIf { it.isNotBlank() }
        val activeDistrict = profileActiveDistrict ?: localActiveDistrict ?: homeDistrict
        val remoteCompleted = snapshot.getStringList("completedHeroIds")
            .ifEmpty { snapshot.getPlaceIdsFromMaps("badgesEarned") }
            .toSet()
        val remoteUnlocked = snapshot.getStringList("unlockedDistricts").toSet()
        val completedHeroIds = localCompleted + remoteCompleted
        val unlockedDistricts =
            localUnlocked + manualUnlocked + remoteUnlocked + listOfNotNull(homeDistrict, activeDistrict)

        syncLocalProgress(
            homeDistrict = homeDistrict,
            activeDistrict = activeDistrict,
            completedHeroIds = completedHeroIds,
            unlockedDistricts = unlockedDistricts
        )

        return StoryProgressState(
            homeDistrict = homeDistrict,
            activeDistrict = activeDistrict,
            completedHeroIds = completedHeroIds,
            unlockedDistricts = unlockedDistricts
        )
    }

    suspend fun markHeroCompleted(hero: StoryCatalogEntry) {
        if (hero.placeId.isBlank()) return
        val wasAlreadyCompleted = hero.placeId in getCompletedHeroIds()
        markHeroCompletedLocally(hero)

        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return

        runCatching {
            val userDocument = firestore.collection(USERS_COLLECTION).document(user.uid)
            val snapshot = userDocument.get().await()
            val remoteCompleted = snapshot.getStringList("completedHeroIds")
                .ifEmpty { snapshot.getPlaceIdsFromMaps("badgesEarned") }
                .toSet()
            val currentBadgeCount = snapshot.getPlaceIdsFromMaps("badgesEarned").toSet().size
            val nextBadgeCount = if (wasAlreadyCompleted || hero.placeId in remoteCompleted) {
                currentBadgeCount
            } else {
                currentBadgeCount + 1
            }
            val nextRank = nextBadgeCount.toExplorerRank()

            if (wasAlreadyCompleted || hero.placeId in remoteCompleted) {
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
                            "earnedAt" to now
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
    }

    suspend fun markDistrictCompleted(districtName: String) {
        if (districtName.isBlank()) return
        val districtBadgeId = "district:${districtName.lowercase()}"
        val completedDistricts = preferences
            .getStringSet(KEY_COMPLETED_DISTRICT_BADGES, emptySet())
            .orEmpty()
        if (districtBadgeId in completedDistricts) return

        preferences.edit()
            .putStringSet(KEY_COMPLETED_DISTRICT_BADGES, completedDistricts + districtBadgeId)
            .apply()

        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return

        runCatching {
            val now = System.currentTimeMillis()
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .set(
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
                )
                .await()
        }
    }

    suspend fun unlockDistrict(districtName: String) {
        if (districtName.isBlank()) return
        val updated = preferences.getStringSet(KEY_UNLOCKED_DISTRICTS, emptySet()).orEmpty() + districtName
        preferences.edit()
            .putStringSet(KEY_UNLOCKED_DISTRICTS, updated)
            .apply()

        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return
        runCatching {
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
    }

    suspend fun setActiveDistrict(districtName: String) {
        if (districtName.isBlank()) return
        preferences.edit()
            .putString(KEY_ACTIVE_DISTRICT, districtName)
            .apply()

        val user = firebaseAuth.currentUser ?: return
        if (user.isAnonymous) return

        runCatching {
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
    }

    fun syncLocalProgress(
        homeDistrict: String?,
        activeDistrict: String?,
        completedHeroIds: Set<String>,
        unlockedDistricts: Set<String>
    ) {
        preferences.edit()
            .putStringSet(KEY_COMPLETED_HERO_IDS, completedHeroIds)
            .putStringSet(KEY_UNLOCKED_DISTRICTS, unlockedDistricts)
            .apply()
        if (!homeDistrict.isNullOrBlank()) {
            preferences.edit()
                .putString(KEY_HOME_DISTRICT, homeDistrict)
                .putBoolean(KEY_HOME_DISTRICT_SET, true)
                .apply()
        }
        val normalizedActiveDistrict = activeDistrict?.takeIf { it.isNotBlank() } ?: homeDistrict
        if (!normalizedActiveDistrict.isNullOrBlank()) {
            preferences.edit()
                .putString(KEY_ACTIVE_DISTRICT, normalizedActiveDistrict)
                .apply()
        }
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
        const val STORY_PROGRESS_PREFS = "itihaasa_prefs"
        const val LEGACY_STORY_PROGRESS_PREFS = "story_progress"
        const val USERS_COLLECTION = "users"
        const val KEY_HOME_DISTRICT = "home_district"
        const val KEY_HOME_DISTRICT_SET = "home_district_set"
        const val KEY_ACTIVE_DISTRICT = "active_district"
        const val KEY_COMPLETED_HERO_IDS = "completed_hero_ids"
        const val KEY_COMPLETED_DISTRICT_BADGES = "completed_district_badges"
        const val KEY_UNLOCKED_DISTRICTS = "unlocked_districts"
        const val KEY_MANUAL_UNLOCKED_DISTRICTS = "manually_unlocked_districts"
        const val KEY_COMPLETED_HERO_EARNED_AT_PREFIX = "completed_hero_earned_at_"
    }
}

data class StoryProgressState(
    val homeDistrict: String? = null,
    val activeDistrict: String? = null,
    val completedHeroIds: Set<String> = emptySet(),
    val unlockedDistricts: Set<String> = emptySet()
)
