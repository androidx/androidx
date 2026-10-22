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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.resources

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.text.font.FontFamily
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
    paint.fontVariationSettings = variationSettings
    return paint.typeface ?: typeface
}

/**
 * Formats variation axis [tags] and [values] into the CSS-style string expected by
 * [Paint.setFontVariationSettings] and [FontRequest] (e.g. `"'wght' 500.0, 'wdth' 110.0"`),
 * ignoring OpenType font feature tags (4-character lowercase tags other than standard variation
 * axes).
 */
internal fun formatVariationAxes(tags: Array<out String>, values: FloatArray): String? {
    val count = minOf(tags.size, values.size)
    if (count == 0) return null
    var builder: StringBuilder? = null
    for (i in 0 until count) {
        val tag = tags[i]
        if (!isVariationAxisTag(tag)) continue
        val sb = builder ?: StringBuilder().also { builder = it }
        if (sb.isNotEmpty()) sb.append(", ")
        sb.append('\'').append(tag).append("' ").append(values[i])
    }
    return builder?.toString()
}

private val STANDARD_VARIATION_AXIS_TAGS = setOf("ital", "opsz", "slnt", "wdth", "wght")

internal fun isVariationAxisTag(name: String): Boolean =
    name in STANDARD_VARIATION_AXIS_TAGS || name.any { it.isUpperCase() }

/**
 * Reactive cache that bridges suspending [RcTypefaceResolver.resolve] calls to Compose text
 * ([RcPlayerText]), synchronous canvas/text-measurement paths (`toNativeTextPaint` /
 * `toTextStyle`), and `remote-player-core`'s [TypefaceResolver] via [coreTypefaceResolver].
 *
 * No `CoroutineScope` is stored in this cache; instead, [RcPlayer] manages attachment/disposal via
 * [onAttach] and [onDetach] inside a `DisposableEffect`. Synchronous [RcTypefaceResolver]s complete
 * inline without allocating a [MutableState] or keeping any coroutine alive.
 */
internal class RcTypefaceCache(var resolver: RcTypefaceResolver? = null) {
    private val resolvedTypefaces = HashMap<RcTypefaceRequest, Typeface?>()
    private val states = HashMap<RcTypefaceRequest, MutableState<Typeface?>>()
    private val fontFamilies = HashMap<RcTypefaceRequest, Pair<Typeface, FontFamily>>()
    private val resolvedRequests = HashSet<RcTypefaceRequest>()
    private val pendingRequests = LinkedHashSet<RcTypefaceRequest>()
    private val inFlightJobs = HashMap<RcTypefaceRequest, CompletableJob>()
    private val listeners = HashMap<RcTypefaceRequest, MutableList<Runnable>>()
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
            resolvedRequests.clear()
            pendingRequests.clear()
            listeners.clear()
        }
    }

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
            return EmbeddedPlayerTypefaceResolver.resolveSync(request)
        }
        if (request in resolvedRequests) {
            return resolvedTypefaces[request]
        }
        if (request !in inFlightJobs) {
            startResolution(request, activeResolver)
            if (request in resolvedRequests) {
                return resolvedTypefaces[request]
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
                        pendingRequests.add(request)
                        return
                    }
                    val tf =
                        result.getOrNull()?.let {
                            applyVariationSettings(it, request.variationSettings)
                        }
                    resolvedRequests.add(request)
                    resolvedTypefaces[request] = tf
                    pendingRequests.remove(request)
                    if (!isSync) {
                        states[request]?.value = tf
                        Snapshot.sendApplyNotifications()
                    }
                    listeners.remove(request)?.fastForEach { it.run() }
                }
            }
        )
        isSync = false
    }

    fun onAttach(overrideResolver: RcTypefaceResolver? = resolver) {
        attachedCount++
        val activeResolver = overrideResolver ?: resolver ?: return
        ensureResolverUpToDate(activeResolver)
        if (activeResolver is EmbeddedPlayerTypefaceResolver) return
        val toRestart = pendingRequests.toList()
        pendingRequests.clear()
        toRestart.fastForEach { req ->
            if (req !in resolvedRequests && req !in inFlightJobs) {
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
        if (request in resolvedRequests) {
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
            val base =
                if (fallbackTypeface != null) {
                    Typeface.create(fallbackTypeface, fallbackWeight, fallbackItalic)
                } else {
                    EmbeddedPlayerTypefaceResolver.resolveSync(
                        RcTypefaceRequest(
                            fontFamily = fontFamily,
                            fontType = fontType,
                            weight = weight,
                            italic = italic,
                            variationSettings = null,
                        )
                    )
                }
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
