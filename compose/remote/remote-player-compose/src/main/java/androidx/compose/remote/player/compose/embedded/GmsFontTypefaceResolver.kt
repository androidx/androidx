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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.ArrayRes
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.compose.embedded.resources.LruCache
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine

internal fun interface GmsFontLoader {
    suspend fun loadFont(context: Context, fontRequest: FontRequest): Typeface?
}

private object DefaultGmsFontLoader : GmsFontLoader {
    override suspend fun loadFont(context: Context, fontRequest: FontRequest): Typeface? {
        if (isLayoutlibPreview(context) || !isFontProviderAvailable(context, fontRequest)) {
            return null
        }
        return suspendCancellableCoroutine { continuation ->
            val callback =
                object : FontsContractCompat.FontRequestCallback() {
                    override fun onTypefaceRetrieved(typeface: Typeface) {
                        if (continuation.isActive) {
                            continuation.resume(typeface)
                        }
                    }

                    override fun onTypefaceRequestFailed(reason: Int) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            FontsContractCompat.requestFont(
                context,
                fontRequest,
                Typeface.NORMAL,
                null,
                Executor { it.run() },
                callback,
            )
        }
    }

    private fun isLayoutlibPreview(context: Context): Boolean =
        Build.FINGERPRINT == "layoutlib" ||
            context.javaClass.name.startsWith("com.android.layoutlib.")

    private fun isFontProviderAvailable(context: Context, fontRequest: FontRequest): Boolean {
        val packageManager = context.packageManager ?: return false
        @Suppress("DEPRECATION")
        val providerInfo =
            packageManager.resolveContentProvider(fontRequest.providerAuthority, 0) ?: return false
        return providerInfo.packageName == fontRequest.providerPackage
    }
}

/**
 * An [RcTypefaceResolver] that handles `"google:"` fonts via GMS [FontRequest] using the specified
 * [fontCertsResId]. Delegates to [delegate] (defaults to [EmbeddedPlayerTypefaceResolver]) for
 * other font resolution or if the downloadable font request fails.
 *
 * Each family, weight and style is downloaded once, without [RcTypefaceRequest.variationSettings],
 * and the player applies the variation axes to the returned base typeface, so animating an axis
 * does not start a new download per frame. Loaded and failed families are kept in a bounded cache.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GmsFontTypefaceResolver
internal constructor(
    private val context: Context?,
    @ArrayRes public val fontCertsResId: Int,
    private val delegate: RcTypefaceResolver = EmbeddedPlayerTypefaceResolver,
    private val fontLoader: GmsFontLoader = DefaultGmsFontLoader,
) : RcTypefaceResolver {

    public constructor(
        context: Context?,
        @ArrayRes fontCertsResId: Int,
        delegate: RcTypefaceResolver = EmbeddedPlayerTypefaceResolver,
    ) : this(
        context = context,
        fontCertsResId = fontCertsResId,
        delegate = delegate,
        fontLoader = DefaultGmsFontLoader,
    )

    public constructor(
        remoteContext: RemoteContext,
        @ArrayRes fontCertsResId: Int,
        delegate: RcTypefaceResolver = EmbeddedPlayerTypefaceResolver,
    ) : this(
        context = (remoteContext as? AndroidRemoteContext)?.androidContext,
        fontCertsResId = fontCertsResId,
        delegate = delegate,
        fontLoader = DefaultGmsFontLoader,
    )

    private val lock = Any()

    /**
     * Base (axis-free) typefaces keyed by [RcTypefaceRequest] with `variationSettings == null`.
     * Failed loads are cached as a `null` [LoadedTypeface.typeface]. Bounded because a long-lived
     * resolver can see an unbounded number of families, weights and styles.
     */
    private val loaded = LruCache<RcTypefaceRequest, LoadedTypeface>()

    /** Loads in progress, so concurrent requests for the same base family share one load. */
    private val inFlight = HashMap<RcTypefaceRequest, CompletableDeferred<Typeface?>>()

    private class LoadedTypeface(val typeface: Typeface?)

    internal fun createFontRequest(realFontName: String, request: RcTypefaceRequest): FontRequest {
        val query =
            "name=$realFontName&weight=${request.weight}&italic=${if (request.italic) 1 else 0}&besteffort=true"
        // Variation axes are applied by the player to the loaded base typeface, so the provider is
        // only asked for the base family, once, however the axes animate.
        return FontRequest(GMS_PROVIDER_AUTHORITY, GMS_PROVIDER_PACKAGE, query, fontCertsResId)
    }

    override suspend fun resolve(request: RcTypefaceRequest): Typeface? {
        val fontName = request.fontFamily
        val androidContext = context
        if (
            fontName != null &&
                fontName.startsWith("google:") &&
                fontCertsResId != 0 &&
                androidContext != null
        ) {
            val realFontName = fontName.substring("google:".length)
            loadBase(androidContext, realFontName, request.copy(variationSettings = null))?.let {
                // The player applies request.variationSettings to the returned typeface.
                return it
            }
        }

        return delegate.resolve(request)
    }

    private suspend fun loadBase(
        androidContext: Context,
        realFontName: String,
        base: RcTypefaceRequest,
    ): Typeface? {
        while (true) {
            var pending: CompletableDeferred<Typeface?>? = null
            var owned: CompletableDeferred<Typeface?>? = null
            synchronized(lock) {
                loaded[base]?.let {
                    return it.typeface
                }
                pending = inFlight[base]
                if (pending == null) {
                    owned = CompletableDeferred<Typeface?>().also { inFlight[base] = it }
                }
            }
            val waitFor = pending
            if (waitFor != null) {
                try {
                    return waitFor.await()
                } catch (e: CancellationException) {
                    // The caller that owned the load was cancelled; retry unless this caller was
                    // cancelled too.
                    currentCoroutineContext().ensureActive()
                    continue
                }
            }
            val deferred = owned!!
            try {
                val typeface =
                    fontLoader.loadFont(androidContext, createFontRequest(realFontName, base))
                synchronized(lock) {
                    loaded[base] = LoadedTypeface(typeface)
                    inFlight.remove(base)
                }
                deferred.complete(typeface)
                return typeface
            } catch (e: Throwable) {
                // Cancelled or failed loads are not cached, so a later request tries again.
                synchronized(lock) { inFlight.remove(base) }
                deferred.completeExceptionally(e)
                throw e
            }
        }
    }

    private companion object {
        private const val GMS_PROVIDER_AUTHORITY = "com.google.android.gms.fonts"
        private const val GMS_PROVIDER_PACKAGE = "com.google.android.gms"
    }
}
