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

package androidx.compose.remote.player.compose.test.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat

/**
 * TypefaceResolver that handles downloadable fonts starting with "google:". This is a sample-only
 * class in testutils.
 */
public class DownloadableTypefaceResolver(
    private val context: Context,
    private val next: TypefaceResolver,
    private val isBlocking: Boolean = false,
) : TypefaceResolver {

    private val cache = mutableMapOf<String, FontInstance>()

    override fun resolve(
        fontType: Int,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        return next.resolve(
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
        if (fontName.startsWith("google:")) {
            val key = "$fontName:$weight:$italic"
            cache[key]?.let {
                return it
            }

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
                    R.array.com_google_android_gms_fonts_certs,
                )

            val callback =
                object : FontsContractCompat.FontRequestCallback() {
                    override fun onTypefaceRetrieved(typeface: Typeface) {
                        Log.d("DownloadableTypeface", "Successfully retrieved typeface: $fontName")
                        fontInstance.updateTypeface(typeface)
                    }

                    override fun onTypefaceRequestFailed(reason: Int) {
                        Log.e(
                            "DownloadableTypeface",
                            "Failed to retrieve typeface: $fontName, reason: $reason",
                        )
                    }
                }

            val timeout = if (isBlocking) 3000 else 0
            val handler = Handler(Looper.getMainLooper())
            val typeface =
                FontsContractCompat.requestFont(
                    context,
                    request,
                    Typeface.NORMAL,
                    isBlocking, /* isBlockingFetch */
                    timeout, /* timeout */
                    handler,
                    callback,
                )

            if (isBlocking && typeface != null) {
                val fontInstance = SimpleFontInstance(typeface)
                cache[key] = fontInstance
                return fontInstance
            }

            return fontInstance
        }
        return next.resolve(
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
            Log.d("DownloadableTypeface", "updateTypeface: updating cached typeface to $typeface")
            this.typeface = typeface
            listener?.run()
        }

        override fun getTypeface(): Typeface = typeface

        override fun applyVariationSettings(tags: Array<String>, values: FloatArray): Typeface {
            val sb = StringBuilder()
            for (i in tags.indices) {
                if (i > 0) sb.append(", ")
                sb.append("'").append(tags[i]).append("' ").append(values[i])
            }
            val variationStr = sb.toString()

            val paint = Paint()
            paint.typeface = typeface
            paint.fontVariationSettings = variationStr
            val variationTypeface = paint.typeface
            if (variationTypeface != null) {
                this.typeface = variationTypeface
            }
            return typeface
        }

        override fun setOnLoadedListener(listener: Runnable) {
            this.listener = listener
        }
    }
}
