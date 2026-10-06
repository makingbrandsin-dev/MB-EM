package com.example.data.banner

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.data.firebase.FirebaseStorageManager
import com.example.data.firebase.safeBoolean
import com.example.data.firebase.safeLong
import com.example.data.firebase.safeLongOrNull
import com.example.data.firebase.safeString
import com.example.presentation.components.banner.AppOfferBanner
import com.example.presentation.components.banner.defaultOfferBanners
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Repository and Manager for Promotional & Marketing Banners displayed on the App Home Screen.
 *
 * Integrates with:
 * 1. Firebase Firestore: 'Banners' collection for real-time cloud synchronization.
 * 2. Firebase Storage: Cloud storage for banner graphics and promotional assets.
 * 3. Local Cache: Persistent SharedPreferences cache for offline continuity and instant startup.
 */
class BannerManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _banners = MutableStateFlow<List<AppOfferBanner>>(emptyList())
    val banners: StateFlow<List<AppOfferBanner>> = _banners.asStateFlow()

    private val _activeBanners = MutableStateFlow<List<AppOfferBanner>>(emptyList())
    val activeBanners: StateFlow<List<AppOfferBanner>> = _activeBanners.asStateFlow()

    private val _isFirestoreConnected = MutableStateFlow(false)
    val isFirestoreConnected: StateFlow<Boolean> = _isFirestoreConnected.asStateFlow()

    private var firestoreListener: ListenerRegistration? = null

    init {
        // 1. Immediately load cached banners from SharedPreferences
        loadCachedBanners()
        // 2. Attach real-time snapshot listener to Firebase Firestore 'Banners' collection
        initFirestoreSync()
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            com.example.data.firebase.FirestoreDbProvider.getFirestore(context)
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore initialization error: ${e.message}")
            null
        }
    }

    /**
     * Attaches a real-time listener to the Firestore 'Banners' collection.
     * When remote banners are created, updated, or toggled, this listener updates the UI state.
     */
    private fun initFirestoreSync() {
        val fs = getFirestore() ?: return

        try {
            firestoreListener?.remove()
            firestoreListener = fs.collection(COLLECTION_BANNERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error listening to Firestore 'Banners' collection: ${error.message}")
                        _isFirestoreConnected.value = false
                        return@addSnapshotListener
                    }

                    _isFirestoreConnected.value = true

                    if (snapshot != null && !snapshot.isEmpty) {
                        val cloudBanners = mutableListOf<AppOfferBanner>()
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val headline = doc.getString("headline") ?: doc.getString("title") ?: "Announcement"
                                val subtext = doc.getString("subtext") ?: doc.getString("description")
                                val ctaText = doc.getString("ctaText") ?: "Explore Now"
                                val disclaimer = doc.getString("disclaimer")
                                val badge = doc.getString("badge")
                                val routeAction = doc.getString("routeAction") ?: "leads"
                                val imageUri = doc.getString("imageUrl") ?: doc.getString("imageUri")
                                val isActive = doc.safeBoolean("isActive", true)
                                val displayOrder = (doc.safeLongOrNull("displayOrder") ?: doc.safeLongOrNull("order") ?: cloudBanners.size.toLong()).toInt()
                                val startColor = doc.safeLong("startColor", 0xFF0D9488)
                                val endColor = doc.safeLong("endColor", 0xFF10B981)
                                val ctaColor = doc.safeLong("ctaColor", 0xFF047857)

                                cloudBanners.add(
                                    AppOfferBanner(
                                        id = id,
                                        headline = headline,
                                        subtext = subtext?.takeIf { it.isNotBlank() },
                                        ctaText = ctaText,
                                        disclaimer = disclaimer?.takeIf { it.isNotBlank() },
                                        badge = badge?.takeIf { it.isNotBlank() },
                                        bgGradientColors = listOf(Color(startColor), Color(endColor)),
                                        ctaButtonColor = Color(ctaColor),
                                        ctaTextColor = Color.White,
                                        routeAction = routeAction,
                                        imageUri = imageUri?.takeIf { it.isNotBlank() },
                                        isActive = isActive,
                                        displayOrder = displayOrder
                                    )
                                )
                            } catch (docEx: Exception) {
                                Log.w(TAG, "Error parsing Firestore banner doc ${doc.id}: ${docEx.message}")
                            }
                        }

                        if (cloudBanners.isNotEmpty()) {
                            val sorted = cloudBanners.sortedBy { it.displayOrder }
                            _banners.value = sorted
                            _activeBanners.value = sorted.filter { it.isActive }
                            saveToLocalCache(sorted)
                        }
                    } else if (snapshot != null && snapshot.isEmpty) {
                        // If cloud collection is currently empty, seed the default banners to Firestore
                        seedDefaultBannersToFirestore(fs)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Firestore banners listener: ${e.message}")
        }
    }

    /**
     * Seeds initial promotional banners to Firestore 'Banners' collection if it is currently empty.
     */
    private fun seedDefaultBannersToFirestore(fs: FirebaseFirestore) {
        scope.launch {
            try {
                for (b in defaultOfferBanners) {
                    val map = createFirestoreBannerMap(b)
                    fs.collection(COLLECTION_BANNERS).document(b.id).set(map, SetOptions.merge())
                }
                Log.d(TAG, "Seeded default banners to Firestore 'Banners' collection")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to seed default banners to Firestore: ${e.message}")
            }
        }
    }

    private fun createFirestoreBannerMap(banner: AppOfferBanner): Map<String, Any?> {
        val startColorLong = banner.bgGradientColors.firstOrNull()?.value?.toLong() ?: 0xFF0D9488
        val endColorLong = banner.bgGradientColors.lastOrNull()?.value?.toLong() ?: 0xFF10B981
        val ctaColorLong = banner.ctaButtonColor.value.toLong()

        return hashMapOf(
            "id" to banner.id,
            "headline" to banner.headline,
            "subtext" to (banner.subtext ?: ""),
            "ctaText" to banner.ctaText,
            "disclaimer" to (banner.disclaimer ?: ""),
            "badge" to (banner.badge ?: "FEATURED"),
            "routeAction" to banner.routeAction,
            "imageUrl" to (banner.imageUri ?: ""),
            "imageUri" to (banner.imageUri ?: ""),
            "isActive" to banner.isActive,
            "displayOrder" to banner.displayOrder,
            "order" to banner.displayOrder,
            "startColor" to startColorLong,
            "endColor" to endColorLong,
            "ctaColor" to ctaColorLong,
            "createdAt" to System.currentTimeMillis()
        )
    }

    private fun loadCachedBanners() {
        val savedJson = prefs.getString(KEY_BANNERS_JSON, null)
        if (savedJson.isNullOrBlank()) {
            _banners.value = defaultOfferBanners.sortedBy { it.displayOrder }
            _activeBanners.value = defaultOfferBanners.filter { it.isActive }.sortedBy { it.displayOrder }
        } else {
            try {
                val list = mutableListOf<AppOfferBanner>()
                val jsonArray = JSONArray(savedJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", UUID.randomUUID().toString())
                    val headline = obj.optString("headline", "Special Announcement")
                    val subtext = obj.optString("subtext").takeIf { it.isNotBlank() }
                    val ctaText = obj.optString("ctaText", "Explore Now")
                    val disclaimer = obj.optString("disclaimer").takeIf { it.isNotBlank() }
                    val badge = obj.optString("badge").takeIf { it.isNotBlank() }
                    val routeAction = obj.optString("routeAction", "leads")
                    val imageUri = obj.optString("imageUri").takeIf { it.isNotBlank() }
                    val isActive = obj.optBoolean("isActive", true)
                    val displayOrder = obj.optInt("displayOrder", i)

                    val startColorLong = obj.optLong("startColor", 0xFF0D9488)
                    val endColorLong = obj.optLong("endColor", 0xFF10B981)
                    val ctaColorLong = obj.optLong("ctaColor", 0xFF047857)

                    val bgGradientColors = listOf(Color(startColorLong), Color(endColorLong))
                    val ctaButtonColor = Color(ctaColorLong)

                    list.add(
                        AppOfferBanner(
                            id = id,
                            headline = headline,
                            subtext = subtext,
                            ctaText = ctaText,
                            disclaimer = disclaimer,
                            badge = badge,
                            bgGradientColors = bgGradientColors,
                            ctaButtonColor = ctaButtonColor,
                            ctaTextColor = Color.White,
                            routeAction = routeAction,
                            imageUri = imageUri,
                            isActive = isActive,
                            displayOrder = displayOrder
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    val sorted = list.sortedBy { it.displayOrder }
                    _banners.value = sorted
                    _activeBanners.value = sorted.filter { it.isActive }
                } else {
                    _banners.value = defaultOfferBanners.sortedBy { it.displayOrder }
                    _activeBanners.value = defaultOfferBanners.filter { it.isActive }.sortedBy { it.displayOrder }
                }
            } catch (e: Exception) {
                _banners.value = defaultOfferBanners.sortedBy { it.displayOrder }
                _activeBanners.value = defaultOfferBanners.filter { it.isActive }.sortedBy { it.displayOrder }
            }
        }
    }

    private fun saveToLocalCache(list: List<AppOfferBanner>) {
        scope.launch {
            try {
                val jsonArray = JSONArray()
                for (b in list) {
                    val obj = JSONObject().apply {
                        put("id", b.id)
                        put("headline", b.headline)
                        put("subtext", b.subtext ?: "")
                        put("ctaText", b.ctaText)
                        put("disclaimer", b.disclaimer ?: "")
                        put("badge", b.badge ?: "")
                        put("routeAction", b.routeAction)
                        put("imageUri", b.imageUri ?: "")
                        put("isActive", b.isActive)
                        put("displayOrder", b.displayOrder)
                        val startColorLong = b.bgGradientColors.firstOrNull()?.value?.toLong() ?: 0xFF0D9488
                        val endColorLong = b.bgGradientColors.lastOrNull()?.value?.toLong() ?: 0xFF10B981
                        put("startColor", startColorLong)
                        put("endColor", endColorLong)
                        put("ctaColor", b.ctaButtonColor.value.toLong())
                    }
                    jsonArray.put(obj)
                }
                prefs.edit().putString(KEY_BANNERS_JSON, jsonArray.toString()).apply()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Uploads an image to Firebase Storage under `banners/{fileName}.jpg` and returns the download URL.
     * If Firebase Storage is temporarily unavailable or device is offline, gracefully caches the file locally.
     */
    suspend fun uploadBannerImageToStorage(
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> {
        return FirebaseStorageManager.uploadBannerImage(context, imageUri, onProgress)
    }

    /**
     * Saves an uploaded image locally as an offline fallback.
     */
    fun saveBannerImageLocally(sourceUri: Uri): String? {
        return try {
            val bannersDir = File(context.filesDir, "banners").apply { mkdirs() }
            val fileName = "banner_${System.currentTimeMillis()}.jpg"
            val destFile = File(bannersDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            sourceUri.toString()
        }
    }

    /**
     * Adds a new promotional banner, saving it immediately to:
     * 1. Firebase Firestore collection 'Banners'
     * 2. Local StateFlow and SharedPreferences cache
     */
    fun addBanner(
        headline: String,
        subtext: String?,
        ctaText: String,
        badge: String?,
        routeAction: String,
        imageUri: String?,
        bgGradientColors: List<Color>,
        ctaButtonColor: Color,
        isActive: Boolean = true,
        displayOrder: Int? = null
    ) {
        val calculatedOrder = displayOrder ?: ((_banners.value.maxOfOrNull { it.displayOrder } ?: -1) + 1)
        val newBannerId = "banner_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val newBanner = AppOfferBanner(
            id = newBannerId,
            headline = headline.trim(),
            subtext = subtext?.trim()?.takeIf { it.isNotEmpty() },
            ctaText = if (ctaText.isNotBlank()) ctaText.trim() else "Explore Now",
            disclaimer = "*Active for Making Brands Enterprise Suite",
            badge = badge?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: "FEATURED",
            bgGradientColors = bgGradientColors,
            ctaButtonColor = ctaButtonColor,
            ctaTextColor = Color.White,
            routeAction = routeAction,
            imageUri = imageUri,
            isActive = isActive,
            displayOrder = calculatedOrder
        )

        // 1. Immediately insert and sort local list for snappy UI feedback
        val currentList = _banners.value.filter { it.id != newBannerId }
        val updatedList = (currentList + newBanner).sortedBy { it.displayOrder }
        _banners.value = updatedList
        _activeBanners.value = updatedList.filter { it.isActive }
        saveToLocalCache(updatedList)

        // 2. Persist to Firebase Firestore 'Banners' collection
        scope.launch {
            try {
                val fs = getFirestore()
                if (fs != null) {
                    val map = createFirestoreBannerMap(newBanner)
                    fs.collection(COLLECTION_BANNERS).document(newBannerId)
                        .set(map, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "Banner $newBannerId successfully added to Firestore 'Banners' collection")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to write banner to Firestore 'Banners': ${e.message}")
                        }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error saving banner to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Updates an existing banner's content and persists to Firestore.
     */
    fun editBanner(
        bannerId: String,
        headline: String,
        subtext: String?,
        ctaText: String,
        badge: String?,
        routeAction: String,
        imageUri: String?,
        bgGradientColors: List<Color>,
        ctaButtonColor: Color,
        isActive: Boolean,
        displayOrder: Int
    ) {
        val updatedList = _banners.value.map {
            if (it.id == bannerId) {
                it.copy(
                    headline = headline.trim(),
                    subtext = subtext?.trim()?.takeIf { s -> s.isNotEmpty() },
                    ctaText = if (ctaText.isNotBlank()) ctaText.trim() else "Explore Now",
                    badge = badge?.trim()?.uppercase()?.takeIf { b -> b.isNotEmpty() } ?: "FEATURED",
                    routeAction = routeAction,
                    imageUri = imageUri,
                    bgGradientColors = bgGradientColors,
                    ctaButtonColor = ctaButtonColor,
                    isActive = isActive,
                    displayOrder = displayOrder
                )
            } else it
        }.sortedBy { it.displayOrder }

        _banners.value = updatedList
        _activeBanners.value = updatedList.filter { it.isActive }
        saveToLocalCache(updatedList)

        scope.launch {
            try {
                val banner = updatedList.firstOrNull { it.id == bannerId } ?: return@launch
                val fs = getFirestore()
                if (fs != null) {
                    val map = createFirestoreBannerMap(banner)
                    fs.collection(COLLECTION_BANNERS).document(bannerId).set(map, SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error editing banner in Firestore: ${e.message}")
            }
        }
    }

    /**
     * Moves a banner UP in display order (e.g., from #2 to #1).
     * Swaps displayOrder values and updates Firestore 'Banners' collection.
     */
    fun moveBannerUp(bannerId: String) {
        val current = _banners.value.sortedBy { it.displayOrder }
        val index = current.indexOfFirst { it.id == bannerId }
        if (index > 0) {
            val targetIndex = index - 1
            val itemA = current[index]
            val itemB = current[targetIndex]

            val orderA = itemA.displayOrder
            val orderB = itemB.displayOrder
            val newOrderForA = if (orderA <= orderB) orderB - 1 else orderB
            val newOrderForB = if (orderA <= orderB) orderA else orderA

            val updatedItemA = itemA.copy(displayOrder = newOrderForA)
            val updatedItemB = itemB.copy(displayOrder = newOrderForB)

            val updatedList = current.toMutableList().apply {
                set(index, updatedItemA)
                set(targetIndex, updatedItemB)
            }.sortedBy { it.displayOrder }

            // Normalize orders to 0..n-1 for consistency
            val normalizedList = updatedList.mapIndexed { idx, item -> item.copy(displayOrder = idx) }
            _banners.value = normalizedList
            _activeBanners.value = normalizedList.filter { it.isActive }
            saveToLocalCache(normalizedList)

            // Persist updated orders to Firestore
            scope.launch {
                try {
                    val fs = getFirestore()
                    if (fs != null) {
                        val batch = fs.batch()
                        for (item in normalizedList) {
                            val ref = fs.collection(COLLECTION_BANNERS).document(item.id)
                            batch.update(ref, mapOf("displayOrder" to item.displayOrder, "order" to item.displayOrder))
                        }
                        batch.commit()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating banner order up in Firestore: ${e.message}")
                }
            }
        }
    }

    /**
     * Moves a banner DOWN in display order (e.g., from #1 to #2).
     * Swaps displayOrder values and updates Firestore 'Banners' collection.
     */
    fun moveBannerDown(bannerId: String) {
        val current = _banners.value.sortedBy { it.displayOrder }
        val index = current.indexOfFirst { it.id == bannerId }
        if (index in 0 until current.size - 1) {
            val targetIndex = index + 1
            val itemA = current[index]
            val itemB = current[targetIndex]

            val updatedList = current.toMutableList().apply {
                set(index, itemB)
                set(targetIndex, itemA)
            }

            val normalizedList = updatedList.mapIndexed { idx, item -> item.copy(displayOrder = idx) }
            _banners.value = normalizedList
            _activeBanners.value = normalizedList.filter { it.isActive }
            saveToLocalCache(normalizedList)

            scope.launch {
                try {
                    val fs = getFirestore()
                    if (fs != null) {
                        val batch = fs.batch()
                        for (item in normalizedList) {
                            val ref = fs.collection(COLLECTION_BANNERS).document(item.id)
                            batch.update(ref, mapOf("displayOrder" to item.displayOrder, "order" to item.displayOrder))
                        }
                        batch.commit()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating banner order down in Firestore: ${e.message}")
                }
            }
        }
    }

    /**
     * Directly updates a banner's display order.
     */
    fun updateBannerOrder(bannerId: String, newOrder: Int) {
        val current = _banners.value
        val updatedList = current.map {
            if (it.id == bannerId) it.copy(displayOrder = newOrder) else it
        }.sortedBy { it.displayOrder }

        val normalizedList = updatedList.mapIndexed { idx, item -> item.copy(displayOrder = idx) }
        _banners.value = normalizedList
        _activeBanners.value = normalizedList.filter { it.isActive }
        saveToLocalCache(normalizedList)

        scope.launch {
            try {
                val fs = getFirestore()
                if (fs != null) {
                    val batch = fs.batch()
                    for (item in normalizedList) {
                        val ref = fs.collection(COLLECTION_BANNERS).document(item.id)
                        batch.update(ref, mapOf("displayOrder" to item.displayOrder, "order" to item.displayOrder))
                    }
                    batch.commit()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update order for banner $bannerId: ${e.message}")
            }
        }
    }

    /**
     * Toggles a banner's visibility on the home screen.
     * Updates the 'isActive' boolean property in Firebase Firestore 'Banners' collection
     * and triggers reactive update in the home screen Pager.
     */
    fun toggleBannerStatus(bannerId: String, isActive: Boolean) {
        // 1. Instant local update
        val updatedList = _banners.value.map {
            if (it.id == bannerId) it.copy(isActive = isActive) else it
        }.sortedBy { it.displayOrder }
        _banners.value = updatedList
        _activeBanners.value = updatedList.filter { it.isActive }
        saveToLocalCache(updatedList)

        // 2. Propagate to Firestore 'Banners' collection
        scope.launch {
            try {
                val fs = getFirestore()
                if (fs != null) {
                    fs.collection(COLLECTION_BANNERS).document(bannerId)
                        .update("isActive", isActive)
                        .addOnFailureListener {
                            // If document did not exist in cloud yet, write the full banner
                            val banner = updatedList.firstOrNull { it.id == bannerId }
                            if (banner != null) {
                                val map = createFirestoreBannerMap(banner)
                                fs.collection(COLLECTION_BANNERS).document(bannerId).set(map, SetOptions.merge())
                            }
                        }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error updating banner active status in Firestore: ${e.message}")
            }
        }
    }

    /**
     * Deletes a banner permanently from Firestore 'Banners' collection and local storage.
     */
    fun deleteBanner(bannerId: String) {
        val updatedList = _banners.value.filter { it.id != bannerId }.sortedBy { it.displayOrder }
        val normalizedList = updatedList.mapIndexed { idx, item -> item.copy(displayOrder = idx) }
        _banners.value = normalizedList
        _activeBanners.value = normalizedList.filter { it.isActive }
        saveToLocalCache(normalizedList)

        scope.launch {
            try {
                val fs = getFirestore()
                fs?.collection(COLLECTION_BANNERS)?.document(bannerId)?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting banner from Firestore: ${e.message}")
            }
        }
    }

    fun resetToDefaults() {
        val resetList = defaultOfferBanners.sortedBy { it.displayOrder }
        _banners.value = resetList
        _activeBanners.value = resetList.filter { it.isActive }
        saveToLocalCache(resetList)
        scope.launch {
            val fs = getFirestore()
            if (fs != null) {
                seedDefaultBannersToFirestore(fs)
            }
        }
    }

    companion object {
        private const val TAG = "BannerManager"
        const val COLLECTION_BANNERS = "Banners"
        private const val PREF_NAME = "mb_app_banners_prefs"
        private const val KEY_BANNERS_JSON = "saved_banners_json"

        @Volatile
        private var instance: BannerManager? = null

        fun getInstance(context: Context): BannerManager {
            return instance ?: synchronized(this) {
                instance ?: BannerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
