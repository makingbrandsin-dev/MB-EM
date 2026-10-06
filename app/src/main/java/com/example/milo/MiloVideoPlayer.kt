package com.example.milo

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import com.example.ui.screens.MainViewModel
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.domain.milo.MiloState
import java.io.File
import java.io.FileOutputStream

/**
 * Singleton Cache Provider for ExoPlayer MP4 Video Streaming.
 * Uses Media3 SimpleCache with LeastRecentlyUsedCacheEvictor (100MB limit)
 * to locally store streamed Milo status animation MP4s for instant, smooth playback.
 */
@OptIn(UnstableApi::class)
object MiloVideoCache {
    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.applicationContext.cacheDir, "milo_video_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB Cache Limit
            val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    @Synchronized
    fun createCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getCache(context)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val upstreamFactory = DefaultDataSource.Factory(context.applicationContext, httpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}

object MiloVideoHelper {

    /**
     * Resolves the best available Uri for Milo status animation:
     * 1. Internal stored File uploaded specifically for this state (e.g. milo_working.mp4, milo_thinking.mp4)
     * 2. Saved custom remote URL for this state
     * 3. General fallback custom uploaded file
     * Returns null if no custom video is uploaded, allowing clean vector illustration rendering.
     */
    fun getMiloVideoUri(context: Context, state: MiloState): Uri? {
        val stateFile = getMiloSpecificStateFile(context, state)
        if (stateFile != null) return Uri.fromFile(stateFile)

        val savedUrl = getMiloStateUrl(context, state)
        if (!savedUrl.isNullOrBlank()) return Uri.parse(savedUrl)

        val generalFile = getGeneralMiloVideoFile(context)
        if (generalFile != null) return Uri.fromFile(generalFile)

        return null
    }

    /**
     * Checks if a custom MP4 file or remote URL was specifically uploaded/configured for this state.
     */
    fun hasCustomMiloVideo(context: Context, state: MiloState): Boolean {
        val stateFile = getMiloSpecificStateFile(context, state)
        if (stateFile != null) return true
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString("milo_url_${state.name.lowercase()}", null)
        return !url.isNullOrBlank()
    }

    /**
     * Saves an uploaded MP4 video specifically for a particular MiloState (e.g. milo_thinking.mp4).
     */
    fun saveMiloStateVideo(context: Context, uri: Uri, state: MiloState): Boolean {
        val fileName = "milo_${state.name.lowercase()}.mp4"
        return saveMiloVideo(context, uri, fileName)
    }

    /**
     * Removes custom uploaded MP4 video and custom URL for a particular MiloState, reverting to default.
     */
    fun deleteMiloStateVideo(context: Context, state: MiloState): Boolean {
        val dir = File(context.filesDir, "milo")
        val stateFile = File(dir, "milo_${state.name.lowercase()}.mp4")
        if (stateFile.exists()) {
            stateFile.delete()
        }
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("milo_url_${state.name.lowercase()}").apply()
        return true
    }

    /**
     * Saves a custom streaming URL specifically for a MiloState.
     */
    fun saveMiloStateUrl(context: Context, url: String, state: MiloState) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        if (url.isBlank()) {
            prefs.edit().remove("milo_url_${state.name.lowercase()}").apply()
        } else {
            prefs.edit().putString("milo_url_${state.name.lowercase()}", url.trim()).apply()
        }
    }

    /**
     * Retrieves custom streaming URL specifically for a MiloState.
     */
    fun getMiloStateUrl(context: Context, state: MiloState): String? {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        return prefs.getString("milo_url_${state.name.lowercase()}", null)
    }

    private fun getMiloSpecificStateFile(context: Context, state: MiloState): File? {
        val dir = File(context.filesDir, "milo")
        if (!dir.exists()) return null
        val stateFile = File(dir, "milo_${state.name.lowercase()}.mp4")
        if (stateFile.exists() && stateFile.length() > 0) return stateFile
        return null
    }

    private fun getGeneralMiloVideoFile(context: Context): File? {
        val dir = File(context.filesDir, "milo")
        if (!dir.exists()) return null
        val generalFile = File(dir, "milo_status.mp4")
        if (generalFile.exists() && generalFile.length() > 0) return generalFile
        return null
    }

    fun getMiloAssistantCardVideoUri(context: Context): Uri? {
        val dir = File(context.filesDir, "milo")
        val file = File(dir, "milo_assistant_card.mp4")
        if (file.exists() && file.length() > 0) return Uri.fromFile(file)

        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString("milo_url_assistant_card", null)
        if (!url.isNullOrBlank()) return Uri.parse(url)

        return getMiloVideoUri(context, MiloState.WORKING)
    }

    fun saveMiloAssistantCardVideo(context: Context, uri: Uri): Boolean {
        return saveMiloVideo(context, uri, "milo_assistant_card.mp4")
    }

    fun saveMiloAssistantCardUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("milo_url_assistant_card", url).apply()
    }

    fun getAskMiloButtonVideoUri(context: Context): Uri? {
        val dir = File(context.filesDir, "milo")
        val file = File(dir, "milo_ask_button.mp4")
        if (file.exists() && file.length() > 0) return Uri.fromFile(file)

        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString("milo_url_ask_button", null)
        if (!url.isNullOrBlank()) return Uri.parse(url)

        return getMiloVideoUri(context, MiloState.THINKING)
    }

    fun saveAskMiloButtonVideo(context: Context, uri: Uri): Boolean {
        return saveMiloVideo(context, uri, "milo_ask_button.mp4")
    }

    fun saveAskMiloButtonUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("milo_url_ask_button", url).apply()
    }

    fun getSplashScreenVideoUri(context: Context): Uri? {
        val dir = File(context.filesDir, "milo")
        val file = File(dir, "milo_splash.mp4")
        if (file.exists() && file.length() > 0) return Uri.fromFile(file)

        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString("milo_url_splash", null)
        if (!url.isNullOrBlank()) return Uri.parse(url)

        val resId = getSplashVideoResId(context)
        if (resId != null) return Uri.parse("android.resource://${context.packageName}/$resId")

        return null
    }

    fun saveSplashScreenVideo(context: Context, uri: Uri): Boolean {
        return saveMiloVideo(context, uri, "milo_splash.mp4")
    }

    fun saveSplashScreenUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("milo_url_splash", url).apply()
    }

    fun getSplashScreenStatusText(context: Context): String {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        return prefs.getString("milo_splash_status_text", "🤖 Milo AI: Initializing Making Brands Cloud...") ?: "🤖 Milo AI: Initializing Making Brands Cloud..."
    }

    fun saveSplashScreenStatusText(context: Context, text: String) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("milo_splash_status_text", text).apply()
    }

    fun getSplashScreenMiloState(context: Context): MiloState {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        val stateName = prefs.getString("milo_splash_state_name", MiloState.WELCOME.name) ?: MiloState.WELCOME.name
        return try {
            MiloState.valueOf(stateName)
        } catch (_: Exception) {
            MiloState.WELCOME
        }
    }

    fun saveSplashScreenMiloState(context: Context, state: MiloState) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("milo_splash_state_name", state.name).apply()
    }

    fun getAskMiloDefaultStatusText(context: Context): String {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        return prefs.getString("milo_ask_status_text", "Online • Ready to assist with leads, tasks & sales") ?: "Online • Ready to assist with leads, tasks & sales"
    }

    fun saveAskMiloDefaultStatusText(context: Context, text: String) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("milo_ask_status_text", text).apply()
    }

    /**
     * Looks up any MP4 resource placed in res/raw by filename convention:
     * 1. Specific state: milo_idle.mp4, milo_thinking.mp4, milo_working.mp4, etc.
     * 2. General fallback: milo_status.mp4, milo_video.mp4, milo_splash.mp4
     */
    fun getMiloVideoResId(context: Context, state: MiloState): Int? {
        val stateName = when (state) {
            MiloState.IDLE -> "milo_idle"
            MiloState.THINKING -> "milo_thinking"
            MiloState.CELEBRATION -> "milo_celebrating"
            MiloState.CONVERTED -> "milo_celebrating"
            MiloState.WORKING -> "milo_working"
            MiloState.WELCOME -> "milo_welcome"
            MiloState.GOODBYE -> "milo_sleeping"
            else -> "milo_${state.name.lowercase()}"
        }
        val stateResId = context.resources.getIdentifier(stateName, "raw", context.packageName)
        if (stateResId != 0) return stateResId

        val idleResId = context.resources.getIdentifier("milo_idle", "raw", context.packageName)
        if (idleResId != 0) return idleResId

        val generalResId = context.resources.getIdentifier("milo_status", "raw", context.packageName)
        if (generalResId != 0) return generalResId

        val defaultVideoResId = context.resources.getIdentifier("milo_video", "raw", context.packageName)
        if (defaultVideoResId != 0) return defaultVideoResId

        return null
    }

    /**
     * Checks if a custom MP4 file exists in app internal storage.
     */
    fun getMiloVideoFile(context: Context, state: MiloState): File? {
        val dir = File(context.filesDir, "milo")
        if (!dir.exists()) return null

        val stateFile = File(dir, "milo_${state.name.lowercase()}.mp4")
        if (stateFile.exists() && stateFile.length() > 0) return stateFile

        val generalFile = File(dir, "milo_status.mp4")
        if (generalFile.exists() && generalFile.length() > 0) return generalFile

        return null
    }

    /**
     * Gets custom saved remote video URL from preferences.
     */
    fun getMiloVideoUrl(context: Context, state: MiloState? = null): String? {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        val key = if (state != null) "milo_url_${state.name.lowercase()}" else "milo_url_default"
        return prefs.getString(key, null) ?: prefs.getString("milo_url_default", null)
    }

    /**
     * Saves custom remote video URL to preferences.
     */
    fun saveMiloVideoUrl(context: Context, url: String, state: MiloState? = null) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        val key = if (state != null) "milo_url_${state.name.lowercase()}" else "milo_url_default"
        prefs.edit().putString(key, url).apply()
    }

    /**
     * Checks for splash video file in res/raw or internal files.
     */
    fun getSplashVideoResId(context: Context): Int? {
        val splashId = context.resources.getIdentifier("milo_splash", "raw", context.packageName)
        if (splashId != 0) return splashId
        val statusId = context.resources.getIdentifier("milo_status", "raw", context.packageName)
        if (statusId != 0) return statusId
        return null
    }

    fun isChromaKeyEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("milo_chroma_key_enabled", true)
    }

    fun setChromaKeyEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("milo_chroma_key_enabled", enabled).apply()
    }

    fun getChromaKeyThreshold(context: Context): Float {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        return prefs.getFloat("milo_chroma_key_threshold", 0.25f)
    }

    fun setChromaKeyThreshold(context: Context, threshold: Float) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("milo_chroma_key_threshold", threshold).apply()
    }

    fun getVideoFitMode(context: Context): Int {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("milo_video_fit_mode", 0) // 0: ZOOM, 1: FIT, 2: FILL
    }

    fun setVideoFitMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("milo_video_fit_mode", mode).apply()
    }

    /**
     * Saves an uploaded MP4 file to local storage.
     */
    fun saveMiloVideo(context: Context, uri: Uri, customName: String = "milo_status.mp4"): Boolean {
        return try {
            val dir = File(context.filesDir, "milo").apply { mkdirs() }
            val destFile = File(dir, customName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * AGSL RuntimeShader for real-time Green Screen / Chroma Key removal directly in Compose.
 * Runs on Android RenderThread Skia GPU pipeline on Android 13+ (API 33+) without GLSurfaceView.
 */
private const val CHROMA_KEY_AGSL = """
    uniform shader image;
    uniform float threshold;
    uniform int chromaEnabled;

    half4 main(float2 fragCoord) {
        half4 color = image.eval(fragCoord);
        if (chromaEnabled == 1) {
            float maxRB = max(color.r, color.b);
            float greenDominance = color.g - maxRB;
            if ((color.g > 0.28 && color.g > color.r * 1.12 && color.g > color.b * 1.12) || greenDominance > threshold) {
                float diff = max(greenDominance, color.g - maxRB);
                float alpha = 1.0 - smoothstep(threshold * 0.5, threshold + 0.12, diff);
                return half4(color.rgb * alpha, alpha);
            }
        }
        return color;
    }
"""

/**
 * Looping, muted MP4 Video Player Composable powered by ExoPlayer + TextureView.
 * Streams remote or local MP4 files efficiently with zero GLSurfaceView EGL conflicts.
 */
@OptIn(UnstableApi::class)
@Composable
fun MiloVideoSurface(
    videoResId: Int? = null,
    videoFile: File? = null,
    videoUrl: String? = null,
    videoUri: Uri? = null,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val targetUri: Uri? = remember(videoResId, videoFile, videoUrl, videoUri) {
        when {
            videoUri != null -> videoUri
            !videoUrl.isNullOrBlank() -> Uri.parse(videoUrl)
            videoResId != null -> Uri.parse("android.resource://${context.packageName}/$videoResId")
            videoFile != null -> Uri.fromFile(videoFile)
            else -> null
        }
    }

    if (targetUri == null) return

    val isChromaKeyEnabled = remember { MiloVideoHelper.isChromaKeyEnabled(context) }
    val chromaThreshold = remember { MiloVideoHelper.getChromaKeyThreshold(context) }

    val exoPlayer = remember(appContext) {
        try {
            val renderersFactory = DefaultRenderersFactory(appContext).apply {
                setEnableDecoderFallback(true)
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            }
            ExoPlayer.Builder(appContext, renderersFactory).build().apply {
                volume = 0f // Silent loop for status animation
                repeatMode = Player.REPEAT_MODE_ALL // Infinite seamless loop
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.w("MiloVideoSurface", "Video playback warning safely handled: ${error.message}")
                    }
                })
            }
        } catch (e: Exception) {
            android.util.Log.e("MiloVideoSurface", "Error initializing ExoPlayer", e)
            null
        }
    }

    if (exoPlayer == null) return

    LaunchedEffect(exoPlayer, targetUri) {
        try {
            val isRemote = targetUri.scheme?.startsWith("http") == true
            val mediaSource = if (isRemote) {
                val cacheDataSourceFactory = MiloVideoCache.createCacheDataSourceFactory(appContext)
                ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(targetUri))
            } else {
                val dataSourceFactory = DefaultDataSource.Factory(appContext)
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(targetUri))
            }
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            android.util.Log.e("MiloVideoSurface", "Error updating ExoPlayer media source", e)
        }
    }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    DisposableEffect(exoPlayer) {
        VoiceManager.getInstance(appContext).registerPlayer(exoPlayer)
        onDispose {
            try {
                VoiceManager.getInstance(appContext).unregisterPlayer(exoPlayer)
                playerViewRef?.player = null
                playerViewRef = null
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            } catch (_: Exception) {}
        }
    }

    val chromaModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isChromaKeyEnabled) {
        val runtimeShader = remember { RuntimeShader(CHROMA_KEY_AGSL) }
        runtimeShader.setFloatUniform("threshold", chromaThreshold)
        runtimeShader.setIntUniform("chromaEnabled", 1)
        Modifier.graphicsLayer {
            renderEffect = RenderEffect.createRuntimeShaderEffect(
                runtimeShader,
                "image"
            ).asComposeRenderEffect()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier.then(chromaModifier),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val pv = android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.milo_texture_player_view, null) as PlayerView
                pv.apply {
                    this.resizeMode = resizeMode
                    keepScreenOn = false
                    player = exoPlayer
                    playerViewRef = this
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    useController = false
                    focusable = android.view.View.NOT_FOCUSABLE
                }
            },
            update = { playerView ->
                playerViewRef = playerView
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
            }
        )
    }
}

/**
 * Real Milo Status Composable with ExoPlayer caching data source support.
 * Automatically switches to MP4 video when MP4 file or streaming URL is present,
 * or gracefully renders the high-fidelity real Milo Mascot photo with smooth breathing aura.
 */
@Composable
fun MiloRealStatusView(
    state: MiloState,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showStateBadge: Boolean = false,
    isHovered: Boolean = false,
    customVideoUri: Uri? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Infinite breathing & subtle pulse motion (accelerates into energetic pulse when hovered)
    val infiniteTransition = rememberInfiniteTransition(label = "MiloRealMotion")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = if (isHovered) 1.04f else 0.98f,
        targetValue = if (isHovered) 1.14f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isHovered) 700 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    val hoverGrowScale by animateFloatAsState(
        targetValue = if (isHovered) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HoverGrowScale"
    )

    // Check for MP4 videoUri (local file, raw resource, or cached remote URL)
    val videoUri = customVideoUri ?: remember(state) { MiloVideoHelper.getMiloVideoUri(context, state) }
    val hasMp4 = videoUri != null

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Main Mascot Container - Clean, transparent background, NO black background, NO circle shape
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val finalScale = hoverGrowScale * breathingScale
                    scaleX = finalScale
                    scaleY = finalScale
                },
            contentAlignment = Alignment.Center
        ) {
            if (hasMp4 && videoUri != null) {
                // Play Real MP4 Video Loop via ExoPlayer + TextureView on top, removing vector image
                MiloVideoSurface(
                    videoUri = videoUri,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Real Milo Lion Mascot Photo Asset fallback
                val imageRes = if (state == MiloState.THINKING) {
                    R.drawable.milo_thinking
                } else {
                    R.drawable.milo_final
                }

                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Milo Real Status - ${state.title}",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 🦁 Milo Assistant Card for Home Screen (Replaces old widget).
 * Supports MP4 video loop playback from admin upload, dynamic speech status,
 * and quick one-tap executive prompts that open the Milo Assistant.
 */
@Composable
fun MiloAssistantCard(
    isWorking: Boolean = true,
    speechText: String? = null,
    viewModel: MainViewModel? = null,
    onOpenAssistant: () -> Unit,
    onQuickPrompt: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardVideoUri = remember { MiloVideoHelper.getMiloAssistantCardVideoUri(context) }
    val defaultStatus = remember { MiloVideoHelper.getAskMiloDefaultStatusText(context) }

    val displayText = speechText ?: if (isWorking) {
        "Shift Active! You're clocked in — I'm monitoring CRM leads & today's tasks. Tap me to draft proposals or summarize."
    } else {
        defaultStatus.ifBlank { "Ready for duty! Toggle attendance ON or tap to ask Milo anything." }
    }

    val (isMiloMuted, toggleMiloMute) = com.example.milo.rememberMiloMuteState()
    var showTemplatesDialog by remember { mutableStateOf(false) }

    if (showTemplatesDialog && viewModel != null) {
        MiloProposalTemplatesDialog(
            viewModel = viewModel,
            onDismiss = { showTemplatesDialog = false },
            onSelectTemplate = { title, content ->
                onQuickPrompt(content)
                onOpenAssistant()
            }
        )
    }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardHovered by cardInteractionSource.collectIsHoveredAsState()
    var isPointerHovered by remember { mutableStateOf(false) }
    val effectiveHovered = isCardHovered || isPointerHovered

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .hoverable(cardInteractionSource)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> isPointerHovered = true
                            PointerEventType.Exit -> isPointerHovered = false
                        }
                    }
                }
            }
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null
            ) {
                com.example.util.MiloHaptics.performButtonTap(context)
                onOpenAssistant()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main Hero Row: Big prominent Milo character on left + Title & live speech bubble on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🦁 Prominent Big Milo Character (Enlarged to 155.dp, full headroom, NO cuts on head, NO black box, NO circle shape)
                Box(
                    modifier = Modifier
                        .size(155.dp)
                        .padding(top = 2.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (cardVideoUri != null) {
                        MiloVideoSurface(
                            videoUri = cardVideoUri,
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MiloRealStatusView(
                            state = if (isWorking) MiloState.WORKING else MiloState.WELCOME,
                            size = 155.dp,
                            showStateBadge = false,
                            isHovered = effectiveHovered
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right column: Title, status, Ask button & live speech bubble
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "MB Assistant",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0284C7)
                            ) {
                                Text(
                                    text = "20X AI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isWorking) "⚡ Live Copilot · Active Shift" else "🟢 Milo Assistant · Online",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isWorking) Color(0xFF16A34A) else Color(0xFF64748B)
                        )
                    }

                    // Highly readable Speech advice bubble with comfortable spacing
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayText,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B),
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 4,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Action buttons Row (Mute voice toggle & Adjust Ask button)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Mute button: 44.dp height, visual indicator states
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMiloMuted) Color(0xFFFEF2F2) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isMiloMuted) Color(0xFFFCA5A5) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .size(width = 46.dp, height = 42.dp)
                                .clickable {
                                    toggleMiloMute()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isMiloMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMiloMuted) "Unmute Milo" else "Mute Milo",
                                    tint = if (isMiloMuted) Color(0xFFEF4444) else Color(0xFF1E293B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // ASK button: Highly prominent Filled button, comfortable and large touch target
                        Button(
                            onClick = {
                                com.example.util.MiloHaptics.performButtonTap(context)
                                onOpenAssistant()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "ASK MILO",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Advanced features: Active AI Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Milo 2.5 AI Engine",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F)
                    )
                }

                val leadsCount = if (viewModel != null) {
                    val lList by viewModel.leads.collectAsState()
                    lList.size
                } else 6

                val projectsCount = if (viewModel != null) {
                    val pList by viewModel.projects.collectAsState()
                    pList.size
                } else 3

                Text(
                    text = "Syncing: $leadsCount Leads · $projectsCount Projects",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0369A1),
                    modifier = Modifier
                        .background(Color(0xFFE0F2FE), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Prompt Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewModel != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.clickable {
                            com.example.util.MiloHaptics.performButtonTap(context)
                            showTemplatesDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Drafting Templates",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }

                listOf(
                    "📊 Summarize Today" to "Summarize my active leads and tasks for today",
                    "📝 Draft Proposal" to "Draft a WhatsApp proposal for lead follow-up",
                    "🎯 Lead Status" to "Check status of recent CRM leads",
                    "📌 Add Task" to "Remind me: Review client milestones at 5 PM"
                ).forEach { (chipLabel, promptCmd) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable {
                            com.example.util.MiloHaptics.performButtonTap(context)
                            onQuickPrompt(promptCmd)
                            onOpenAssistant()
                        }
                    ) {
                        Text(
                            text = chipLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
