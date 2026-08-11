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

import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import androidx.annotation.ArrayRes
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat

/**
 * Interface implemented by a [TypefaceResolver] that provides a GMS Google Fonts certificate array
 * resource ID for downloadable font requests.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface HasFontCerts {
    /** Array resource ID for GMS Google Fonts certificates array. Defaults to 0 (unconfigured). */
    public val fontCertsResId: Int
}

/**
 * A [TypefaceResolver] that handles "google:" fonts via GMS [FontRequest] using specified
 * certificates. Delegates to [delegate] (defaults to [EmbeddedPlayerTypefaceResolver]) for other
 * font resolution.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GmsFontTypefaceResolver(
    private val remoteContext: RemoteContext,
    @ArrayRes override val fontCertsResId: Int,
    private val delegate: TypefaceResolver = EmbeddedPlayerTypefaceResolver(remoteContext),
) : TypefaceResolver, HasFontCerts {

    private val cache = mutableMapOf<String, FontInstance>()

    override fun resolve(
        fontType: Int,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        return delegate.resolve(
            fontType,
            weight,
            italic,
            fallbackTypeface,
            fallbackWeight,
            fallbackItalic,
        )
    }

    override fun resolve(
        fontName: String,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        if (fontName.startsWith("google:") && fontCertsResId != 0) {
            val key = "$fontName:$weight:$italic"
            cache[key]?.let {
                return it
            }

            val androidContext = (remoteContext as? AndroidRemoteContext)?.androidContext
            if (androidContext != null) {
                val realFontName = fontName.substring("google:".length)
                val fallback = fallbackTypeface ?: Typeface.DEFAULT
                val fontInstance = AsyncFontInstance(fallback)
                cache[key] = fontInstance

                val query =
                    "name=$realFontName&weight=$weight&italic=${if (italic) 1 else 0}&besteffort=true"
                val request =
                    FontRequest(
                        "com.google.android.gms.fonts",
                        "com.google.android.gms",
                        query,
                        fontCertsResId,
                    )

                val callback =
                    object : FontsContractCompat.FontRequestCallback() {
                        override fun onTypefaceRetrieved(typeface: Typeface) {
                            fontInstance.updateTypeface(typeface)
                            remoteContext.needsRepaint()
                        }

                        override fun onTypefaceRequestFailed(reason: Int) {}
                    }

                val handler = Handler(Looper.getMainLooper())
                FontsContractCompat.requestFont(
                    androidContext,
                    request,
                    Typeface.NORMAL,
                    false, /* isBlockingFetch */
                    0, /* timeout */
                    handler,
                    callback,
                )

                return fontInstance
            }
        }

        return delegate.resolve(
            fontName,
            weight,
            italic,
            fallbackTypeface,
            fallbackWeight,
            fallbackItalic,
        )
    }

    private class AsyncFontInstance(private var typeface: Typeface) : FontInstance {
        private var listener: Runnable? = null

        fun updateTypeface(typeface: Typeface) {
            this.typeface = typeface
            listener?.run()
        }

        override fun getTypeface(): Typeface = typeface

        override fun applyVariationSettings(tags: Array<String>, values: FloatArray): Typeface {
            return typeface
        }

        override fun setOnLoadedListener(listener: Runnable) {
            this.listener = listener
        }
    }
}
