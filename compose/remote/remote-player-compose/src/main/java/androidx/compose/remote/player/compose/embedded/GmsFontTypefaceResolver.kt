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

@file:Suppress("RestrictedApiAndroidX", "BanConcurrentHashMap")

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.ArrayRes
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.compose.embedded.resources.applyVariationSettings
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import kotlin.coroutines.resume
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
 * [fontCertsResId] and [RcTypefaceRequest.variationSettings]. Delegates to [delegate] (defaults to
 * [EmbeddedPlayerTypefaceResolver]) for other font resolution or if the downloadable font request
 * fails.
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

    private val cache = ConcurrentHashMap<RcTypefaceRequest, Typeface>()
    private val failedRequests: MutableSet<RcTypefaceRequest> = ConcurrentHashMap.newKeySet()

    internal fun createFontRequest(realFontName: String, request: RcTypefaceRequest): FontRequest {
        val query =
            "name=$realFontName&weight=${request.weight}&italic=${if (request.italic) 1 else 0}&besteffort=true"
        return FontRequest(
            GMS_PROVIDER_AUTHORITY,
            GMS_PROVIDER_PACKAGE,
            query,
            fontCertsResId,
            request.variationSettings,
        )
    }

    override suspend fun resolve(request: RcTypefaceRequest): Typeface? {
        val fontName = request.fontFamily
        if (fontName != null && fontName.startsWith("google:") && fontCertsResId != 0) {
            cache[request]?.let {
                return it
            }

            val androidContext = context
            if (androidContext != null && request !in failedRequests) {
                val realFontName = fontName.substring("google:".length)
                val fontRequest = createFontRequest(realFontName, request)
                val loaded = fontLoader.loadFont(androidContext, fontRequest)
                if (loaded != null) {
                    val varied = applyVariationSettings(loaded, request.variationSettings)
                    cache[request] = varied
                    return varied
                }
                failedRequests.add(request)
            }
        }

        return delegate.resolve(request)
    }

    private companion object {
        private const val GMS_PROVIDER_AUTHORITY = "com.google.android.gms.fonts"
        private const val GMS_PROVIDER_PACKAGE = "com.google.android.gms"
    }
}
