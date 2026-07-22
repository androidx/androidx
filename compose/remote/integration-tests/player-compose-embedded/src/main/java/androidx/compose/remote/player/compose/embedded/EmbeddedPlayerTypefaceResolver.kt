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
import android.util.Log
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.ui.text.googlefonts.R as GoogleFontR
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat

/**
 * A [TypefaceResolver] for the embedded player that handles custom prefixes like "device:" and
 * "google:" directly without delegation. Supports [FontRequest] API for "google:" fonts.
 */
internal class EmbeddedPlayerTypefaceResolver(private val remoteContext: RemoteContext) :
    TypefaceResolver {

    private val cache = mutableMapOf<String, FontInstance>()

    override fun resolve(
        fontType: Int,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        val baseTypeface =
            when (fontType) {
                1 -> Typeface.SANS_SERIF
                2 -> Typeface.SERIF
                3 -> Typeface.MONOSPACE
                else -> Typeface.DEFAULT
            }
        val typeface = createTypeface(baseTypeface, weight, italic)
        return SimpleFontInstance(typeface)
    }

    override fun resolve(
        fontName: String,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        if (fontName.startsWith("google:")) {
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
                        GoogleFontR.array.com_google_android_gms_fonts_certs,
                    )

                val callback =
                    object : FontsContractCompat.FontRequestCallback() {
                        override fun onTypefaceRetrieved(typeface: Typeface) {
                            Log.d("EmbeddedTypeface", "Successfully retrieved typeface: $fontName")
                            fontInstance.updateTypeface(typeface)
                            remoteContext.needsRepaint()
                        }

                        override fun onTypefaceRequestFailed(reason: Int) {
                            Log.e(
                                "EmbeddedTypeface",
                                "Failed to retrieve typeface: $fontName, reason: $reason",
                            )
                        }
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

        val actualName =
            when {
                fontName.startsWith("device:") -> fontName.substring("device:".length)
                fontName.startsWith("google:") -> fontName.substring("google:".length)
                else -> fontName
            }

        val baseTypeface = Typeface.create(actualName, Typeface.NORMAL)
        val typeface = createTypeface(baseTypeface, weight, italic)
        return SimpleFontInstance(typeface)
    }

    private fun createTypeface(base: Typeface, weight: Int, italic: Boolean): Typeface {
        return Typeface.create(base, weight, italic)
    }

    private class SimpleFontInstance(private val typeface: Typeface) : FontInstance {
        override fun getTypeface(): Typeface = typeface

        override fun applyVariationSettings(tags: Array<String>, values: FloatArray): Typeface {
            return typeface
        }

        override fun setOnLoadedListener(listener: Runnable) {
            // Already loaded
        }
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
