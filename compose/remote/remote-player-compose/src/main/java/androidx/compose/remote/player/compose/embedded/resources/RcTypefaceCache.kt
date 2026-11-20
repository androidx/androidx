/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.remote.player.compose.embedded.resources

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.remote.core.Limits
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.compose.embedded.EmbeddedPlayerTypefaceResolver
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.embedded.RcTypefaceRequest
import androidx.compose.remote.player.compose.embedded.RcTypefaceResolver
import androidx.compose.remote.player.compose.embedded.components.RcPlayerText
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface as ComposeTypeface
import androidx.compose.ui.util.fastForEach
import androidx.core.provider.FontRequest
import java.util.Collections
import java.util.WeakHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job

/**
 * Applies [variationSettings] (if non-empty) to [typeface] via [Paint.setFontVariationSettings],
 * returning the derived [Typeface] (or [typeface] if the underlying font does not support the
 * requested variation axes).
 */
internal fun applyVariationSettings(typeface: Typeface, variationSettings: String?): Typeface {
    if (variationSettings.isNullOrEmpty()) return typeface
    val paint = Paint()
    paint.typeface = typeface
    try {
        paint.fontVariationSettings = variationSettings
    } catch (_: IllegalArgumentException) {
        // Malformed settings from document input must never crash the host; keep the base face.
        return typeface
    }
    return paint.typeface ?: typeface
}

/**
 * Maximum number of font variation axes accepted per text style, matching
 * [PaintBundle.setTextAxis].
 */
internal const val MAX_FONT_AXES = 8

/**
 * Formats variation axis [tags] and [values] into the CSS-style string expected by
 * [Paint.setFontVariationSettings] and [FontRequest] (e.g. `"'wdth' 110.0, 'wght' 500.0"`),
 * ignoring OpenType font feature tags (4-character lowercase tags other than standard variation
 * axes) and malformed tags. Axes are sorted by tag, see [buildVariationAxes].
 */
internal fun formatVariationAxes(tags: Array<out String>, values: FloatArray): String? =
    buildVariationAxes(minOf(tags.size, values.size), { tags[it] }, { values[it] })

/**
 * Builds the CSS-style variation settings string for up to [MAX_FONT_AXES] axes in `0 until count`
 * whose [tagAt] is a variation axis tag (see [isVariationAxisTag]), sorted by tag.
 *
 * The string is part of the [RcTypefaceRequest] and document font cache keys, so sorting lets the
 * same axes in a different order share a [Typeface]. The sort is stable, so duplicate tags keep
 * their relative order.
 */
internal inline fun buildVariationAxes(
    count: Int,
    tagAt: (Int) -> String?,
    valueAt: (Int) -> Float,
): String? {
    if (count <= 0) return null
    val maxAxes = minOf(count, MAX_FONT_AXES)
    var firstTag: String? = null
    var firstValue = 0f
    var tags: Array<String>? = null
    var values: FloatArray? = null
    var size = 0
    for (i in 0 until count) {
        if (size >= maxAxes) break
        val tag = tagAt(i) ?: continue
        if (!isVariationAxisTag(tag)) continue
        val value = valueAt(i)
        if (size == 0) {
            firstTag = tag
            firstValue = value
            size = 1
            continue
        }
        val t = tags ?: Array(maxAxes) { firstTag!! }.also { tags = it }
        val v =
            values
                ?: FloatArray(maxAxes).also {
                    it[0] = firstValue
                    values = it
                }
        // Stable insertion sort over at most MAX_FONT_AXES entries: duplicate tags keep their
        // relative order.
        var pos = size
        while (pos > 0 && t[pos - 1] > tag) {
            t[pos] = t[pos - 1]
            v[pos] = v[pos - 1]
            pos--
        }
        t[pos] = tag
        v[pos] = value
        size++
    }
    if (size == 0) return null
    if (size == 1) return "'$firstTag' $firstValue"
    val t = tags!!
    val v = values!!
    val sb = StringBuilder()
    for (i in 0 until size) {
        if (i > 0) sb.append(", ")
        sb.append('\'').append(t[i]).append("' ").append(v[i])
    }
    return sb.toString()
}

/** Registered OpenType variation axes that have no layout-feature counterpart. */
private val AXIS_ONLY_TAGS = setOf("opsz", "slnt", "wdth", "wght")

/**
 * Returns whether [tag] is a well-formed OpenType tag: exactly four printable ASCII characters
 * (`0x20..0x7E`) without a leading space. Quote and comma characters are also rejected since they
 * cannot be represented in the CSS-style settings strings built from these tags.
 */
internal fun isValidOpenTypeTag(tag: String): Boolean {
    if (tag.length != 4 || tag[0] == ' ') return false
    for (c in tag) {
        if (c < ' ' || c > '~' || c == '\'' || c == '"' || c == ',') return false
    }
    return true
}

/**
 * Returns whether [tag] should be applied as a font variation axis. Registered axes (`wght`,
 * `wdth`, `slnt`, `opsz`, `ital`) and foundry-defined axes (which contain an uppercase letter) are
 * axes; malformed tags are never axes.
 */
internal fun isVariationAxisTag(tag: String): Boolean =
    isValidOpenTypeTag(tag) &&
        (tag in AXIS_ONLY_TAGS || tag == "ital" || tag.any { it in 'A'..'Z' })

/**
 * Returns whether [tag] should be applied as an OpenType layout feature. `ital` and uppercase
 * (private) tags are valid both as axes and as features, so they are reported here as well as by
 * [isVariationAxisTag]; applying a tag the font does not support has no effect.
 * [buildFontFeatureSettings] only applies those dual-use tags as features for constant whole-number
 * values.
 */
internal fun isFontFeatureTag(tag: String): Boolean =
    isValidOpenTypeTag(tag) && tag !in AXIS_ONLY_TAGS

/** Access-ordered map that evicts its least recently used entry beyond [maxEntries]. */
internal class LruCache<K, V>(private val maxEntries: Int = Limits.MAX_CACHE_ENTRIES) :
    LinkedHashMap<K, V>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean =
        size > maxEntries
}

/** Key for device [FontFamily] instances built for variation settings. */
private data class DeviceFontFamilyKey(
    val familyName: String,
    val weight: FontWeight,
    val style: FontStyle,
    val settings: FontVariation.Settings,
)

/** Holder so that a `null` resolver result can be cached. */
private class ResolvedTypeface(val typeface: Typeface?)

/**
 * Reactive cache that bridges suspending [RcTypefaceResolver.resolve] calls to Compose text
 * ([RcPlayerText]), synchronous canvas/text-measurement paths (`toNativeTextPaint` /
 * `toTextStyle`), and `remote-player-core`'s [TypefaceResolver] via [coreTypefaceResolver].
 *
 * No `CoroutineScope` is stored in this cache; instead, [RcPlayer] manages attachment/disposal via
 * [onAttach] and [onDetach] inside a `DisposableEffect`. Synchronous [RcTypefaceResolver]s complete
 * inline without allocating a [MutableState] or keeping any coroutine alive.
 *
 * Every request-keyed map is bounded to [Limits.MAX_CACHE_ENTRIES] (least recently used entries are
 * evicted), since animated variation axes produce a new [RcTypefaceRequest] on every frame. Only
 * in-flight requests are never evicted, so their pending [MutableState] is always published.
 */
internal class RcTypefaceCache(var resolver: RcTypefaceResolver? = null) {
    private val resolvedTypefaces = LruCache<RcTypefaceRequest, ResolvedTypeface>()
    private val states = HashMap<RcTypefaceRequest, MutableState<Typeface?>>()
    private val fontFamilies = LruCache<RcTypefaceRequest, Pair<Typeface, FontFamily>>()
    private val pendingRequests = LinkedHashSet<RcTypefaceRequest>()
    private val inFlightJobs = HashMap<RcTypefaceRequest, CompletableJob>()
    private val listeners = HashMap<RcTypefaceRequest, MutableList<Runnable>>()
    private val defaultTypefaces = LruCache<RcTypefaceRequest, Typeface>()
    private val deviceFontFamilies = LruCache<DeviceFontFamilyKey, FontFamily>()
    // Weakly keyed so that typefaces built from a replaced document's fonts can be collected.
    private val documentFonts = WeakHashMap<RemoteContext.FontInfo, DocumentFontCache>()
    private var attachedCount = 0
    private var lastResolver: RcTypefaceResolver? = null

    val coreTypefaceResolver: TypefaceResolver = RcCoreTypefaceResolverAdapter(this)

    private fun ensureResolverUpToDate(activeResolver: RcTypefaceResolver) {
        if (activeResolver !== lastResolver) {
            lastResolver = activeResolver
            inFlightJobs.values.toList().fastForEach { it.cancel() }
            inFlightJobs.clear()
            resolvedTypefaces.clear()
            states.clear()
            fontFamilies.clear()
            pendingRequests.clear()
            listeners.clear()
        }
    }

    /**
     * Resolves [request] with the built-in [EmbeddedPlayerTypefaceResolver], reusing a recent
     * result so that per-frame canvas text does not re-create typefaces.
     */
    fun resolveDefault(request: RcTypefaceRequest): Typeface =
        defaultTypefaces.getOrPut(request) { EmbeddedPlayerTypefaceResolver.resolveSync(request) }

    /**
     * Returns a [FontFamily] for the device font [familyName] with the given variation [settings],
     * reusing a recent instance for identical inputs.
     */
    fun deviceFontFamily(
        familyName: String,
        weight: FontWeight,
        style: FontStyle,
        settings: FontVariation.Settings,
    ): FontFamily =
        deviceFontFamilies.getOrPut(DeviceFontFamilyKey(familyName, weight, style, settings)) {
            FontFamily(
                Font(
                    DeviceFontFamilyName(familyName),
                    weight = weight,
                    style = style,
                    variationSettings = settings,
                )
            )
        }

    /** Returns the cache of typefaces built from the document font [info]. */
    fun documentFontCache(info: RemoteContext.FontInfo): DocumentFontCache =
        documentFonts.getOrPut(info) { DocumentFontCache(info.mFontData) }

    fun resolveFontFamily(
        request: RcTypefaceRequest,
        overrideResolver: RcTypefaceResolver? = resolver,
    ): FontFamily? {
        val typeface = resolve(request, overrideResolver) ?: return null
        val cached = fontFamilies[request]
        if (cached != null && cached.first === typeface) {
            return cached.second
        }
        val family = FontFamily(ComposeTypeface(typeface))
        fontFamilies[request] = typeface to family
        return family
    }

    fun resolve(
        request: RcTypefaceRequest,
        overrideResolver: RcTypefaceResolver? = resolver,
    ): Typeface? {
        val activeResolver = overrideResolver ?: resolver ?: return null
        ensureResolverUpToDate(activeResolver)
        if (activeResolver is EmbeddedPlayerTypefaceResolver) {
            return resolveDefault(request)
        }
        resolvedTypefaces[request]?.let {
            return it.typeface
        }
        if (request !in inFlightJobs) {
            startResolution(request, activeResolver)
            resolvedTypefaces[request]?.let {
                return it.typeface
            }
        }
        return states.getOrPut(request) { mutableStateOf(null) }.value
    }

    private fun startResolution(
        request: RcTypefaceRequest,
        activeResolver: RcTypefaceResolver,
    ) {
        pendingRequests.remove(request)
        val job = Job()
        inFlightJobs[request] = job
        var isSync = true
        val suspendResolve: suspend () -> Typeface? = { activeResolver.resolve(request) }
        suspendResolve.startCoroutine(
            object : Continuation<Typeface?> {
                override val context: CoroutineContext = job

                override fun resumeWith(result: Result<Typeface?>) {
                    inFlightJobs.remove(request)
                    job.complete()
                    if (result.exceptionOrNull() is CancellationException) {
                        addPendingRequest(request)
                        return
                    }
                    val tf =
                        result.getOrNull()?.let {
                            applyVariationSettings(it, request.variationSettings)
                        }
                    resolvedTypefaces[request] = ResolvedTypeface(tf)
                    pendingRequests.remove(request)
                    // The state is only needed until its readers observe the result; dropping it
                    // here keeps per-frame requests from accumulating.
                    val state = states.remove(request)
                    if (!isSync && state != null) {
                        state.value = tf
                        Snapshot.sendApplyNotifications()
                    }
                    listeners.remove(request)?.fastForEach { it.run() }
                }
            }
        )
        isSync = false
    }

    private fun addPendingRequest(request: RcTypefaceRequest) {
        pendingRequests.add(request)
        while (pendingRequests.size > Limits.MAX_CACHE_ENTRIES) {
            val eldest = pendingRequests.first()
            pendingRequests.remove(eldest)
            states.remove(eldest)
            listeners.remove(eldest)
        }
    }

    fun onAttach(overrideResolver: RcTypefaceResolver? = resolver) {
        attachedCount++
        val activeResolver = overrideResolver ?: resolver ?: return
        ensureResolverUpToDate(activeResolver)
        if (activeResolver is EmbeddedPlayerTypefaceResolver) return
        val toRestart = pendingRequests.toList()
        pendingRequests.clear()
        toRestart.fastForEach { req ->
            if (req !in resolvedTypefaces && req !in inFlightJobs) {
                startResolution(req, activeResolver)
            }
        }
    }

    fun onDetach() {
        if (attachedCount > 0) {
            attachedCount--
        }
        if (attachedCount == 0 && inFlightJobs.isNotEmpty()) {
            val jobsToCancel = inFlightJobs.values.toList()
            jobsToCancel.fastForEach { it.cancel() }
        }
    }

    fun addOnLoadedListener(request: RcTypefaceRequest, listener: Runnable) {
        if (request in resolvedTypefaces) {
            listener.run()
        } else {
            listeners.getOrPut(request) { mutableListOf() }.add(listener)
        }
    }
}

/**
 * Adapter that bridges [RcTypefaceCache] (and its suspending [RcTypefaceResolver]) back to
 * `remote-player-core`'s synchronous [TypefaceResolver] interface.
 *
 * Returns the default/fallback [Typeface] immediately when a font is not yet loaded, enqueues the
 * [RcTypefaceRequest] in [cache], and later returns the resolved [Typeface] and notifies
 * [FontInstance.setOnLoadedListener] once loading finishes.
 */
internal class RcCoreTypefaceResolverAdapter(private val cache: RcTypefaceCache) :
    TypefaceResolver {

    override fun resolve(
        fontType: Int,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance =
        AdapterFontInstance(
            fontFamily = null,
            fontType = fontType,
            weight = weight,
            italic = italic,
            fallbackTypeface = fallbackTypeface,
            fallbackWeight = fallbackWeight,
            fallbackItalic = fallbackItalic,
        )

    override fun resolve(
        fontName: String,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance =
        AdapterFontInstance(
            fontFamily = fontName,
            fontType = PaintBundle.FONT_TYPE_DEFAULT,
            weight = weight,
            italic = italic,
            fallbackTypeface = fallbackTypeface,
            fallbackWeight = fallbackWeight,
            fallbackItalic = fallbackItalic,
        )

    private inner class AdapterFontInstance(
        private val fontFamily: String?,
        private val fontType: Int,
        private val weight: Int,
        private val italic: Boolean,
        private val fallbackTypeface: Typeface?,
        private val fallbackWeight: Int,
        private val fallbackItalic: Boolean,
    ) : FontInstance {
        private var lastRequest: RcTypefaceRequest =
            RcTypefaceRequest(
                fontFamily = fontFamily,
                fontType = fontType,
                weight = weight,
                italic = italic,
                variationSettings = null,
            )

        override fun getTypeface(): Typeface {
            val request =
                RcTypefaceRequest(
                    fontFamily = fontFamily,
                    fontType = fontType,
                    weight = weight,
                    italic = italic,
                    variationSettings = null,
                )
            lastRequest = request
            cache.resolve(request)?.let {
                return it
            }
            return resolveFallback(null)
        }

        override fun applyVariationSettings(tags: Array<String>, values: FloatArray): Typeface {
            val variationSettings = formatVariationAxes(tags, values)
            val request =
                RcTypefaceRequest(
                    fontFamily = fontFamily,
                    fontType = fontType,
                    weight = weight,
                    italic = italic,
                    variationSettings = variationSettings,
                )
            lastRequest = request
            cache.resolve(request)?.let {
                return it
            }
            return resolveFallback(variationSettings)
        }

        override fun setOnLoadedListener(listener: Runnable) {
            cache.addOnLoadedListener(lastRequest, listener)
        }

        private fun resolveFallback(variationSettings: String?): Typeface {
            if (fallbackTypeface == null) {
                return cache.resolveDefault(
                    RcTypefaceRequest(
                        fontFamily = fontFamily,
                        fontType = fontType,
                        weight = weight,
                        italic = italic,
                        variationSettings = variationSettings,
                    )
                )
            }
            val base = Typeface.create(fallbackTypeface, fallbackWeight, fallbackItalic)
            return applyVariationSettings(base, variationSettings)
        }
    }
}

private val fallbackTypefaceCaches =
    Collections.synchronizedMap(WeakHashMap<RemoteContext, RcTypefaceCache>())

internal val RemoteContext.typefaceCache: RcTypefaceCache
    get() =
        (this as? GraphContext)?.typefaceCache
            ?: fallbackTypefaceCaches.getOrPut(this) { RcTypefaceCache() }

internal var RemoteContext.rcTypefaceResolver: RcTypefaceResolver?
    get() =
        (this as? GraphContext)?.typefaceResolver
            ?: (this as? GraphContext)?.store?.let { fallbackTypefaceCaches[it]?.resolver }
            ?: fallbackTypefaceCaches[this]?.resolver
    set(value) {
        if (this is GraphContext) {
            this.typefaceResolver = value
        } else {
            val cache = typefaceCache
            cache.resolver = value
            if (this is AndroidRemoteContext) {
                this.setTypefaceResolver(cache.coreTypefaceResolver)
            }
        }
    }
