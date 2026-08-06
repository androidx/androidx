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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Density
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
    public val fontVariationSettings: FontVariation.Settings? = null,
    public val fontVariationSettingsMap: Map<String, FontVariation.Settings> = emptyMap(),
) : TypefaceResolver {

    private val cache = mutableMapOf<String, FontInstance>()

    public fun prefetchFonts(
        fontNames: List<String>,
        timeoutMs: Long = DEFAULT_PREFETCH_TIMEOUT_MS,
    ) {
        val latch = java.util.concurrent.CountDownLatch(fontNames.size)
        val handler = Handler(Looper.getMainLooper())
        for (fontName in fontNames) {
            resolve(fontName, DEFAULT_WEIGHT, false, null, DEFAULT_WEIGHT, false)
            val realFontName = fontName.substring("google:".length)
            val fontSpecificSettings =
                fontVariationSettingsMap[realFontName] ?: fontVariationSettings
            val request =
                if (fontSpecificSettings != null) {
                    FontRequest(
                        GMS_PROVIDER_AUTHORITY,
                        GMS_PROVIDER_PACKAGE,
                        "name=$realFontName&weight=$DEFAULT_WEIGHT&italic=0&besteffort=true",
                        R.array.com_google_android_gms_fonts_certs,
                        fontSpecificSettings.toVariationSettingsString(context),
                    )
                } else {
                    FontRequest(
                        GMS_PROVIDER_AUTHORITY,
                        GMS_PROVIDER_PACKAGE,
                        "name=$realFontName&weight=$DEFAULT_WEIGHT&italic=0&besteffort=true",
                        R.array.com_google_android_gms_fonts_certs,
                    )
                }
            val key = "$fontName:$DEFAULT_WEIGHT:false:${fontVariationSettings ?: ""}"
            val fontInstance =
                (cache[key] as? DownloadableFontInstance)
                    ?: DownloadableFontInstance(
                        realFontName,
                        DEFAULT_WEIGHT,
                        false,
                        Typeface.DEFAULT,
                    )

            val callback =
                object : FontsContractCompat.FontRequestCallback() {
                    override fun onTypefaceRetrieved(typeface: Typeface) {
                        fontInstance.updateTypeface(typeface)
                        latch.countDown()
                    }

                    override fun onTypefaceRequestFailed(reason: Int) {
                        latch.countDown()
                    }
                }

            FontsContractCompat.requestFont(
                context,
                request,
                Typeface.NORMAL,
                false, /* isBlockingFetch */
                0, /* timeout */
                handler,
                callback,
            )
        }
        latch.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    private fun FontVariation.Settings.toVariationSettingsString(context: Context): String {
        val density = Density(context)
        return settings.joinToString(",") { "'${it.axisName}' ${it.toVariationValue(density)}" }
    }

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
            val key = "$fontName:$weight:$italic:${fontVariationSettings ?: ""}"
            cache[key]?.let {
                return it
            }

            val realFontName = fontName.substring("google:".length)
            val fallback = fallbackTypeface ?: Typeface.DEFAULT
            val fontInstance = DownloadableFontInstance(realFontName, weight, italic, fallback)
            cache[key] = fontInstance

            val query =
                "name=$realFontName&weight=$weight&italic=${if (italic) 1 else 0}&besteffort=true"
            val fontSpecificSettings =
                fontVariationSettingsMap[realFontName] ?: fontVariationSettings
            val request =
                if (fontSpecificSettings != null) {
                    FontRequest(
                        GMS_PROVIDER_AUTHORITY,
                        GMS_PROVIDER_PACKAGE,
                        query,
                        R.array.com_google_android_gms_fonts_certs,
                        fontSpecificSettings.toVariationSettingsString(context),
                    )
                } else {
                    FontRequest(
                        GMS_PROVIDER_AUTHORITY,
                        GMS_PROVIDER_PACKAGE,
                        query,
                        R.array.com_google_android_gms_fonts_certs,
                    )
                }

            val callback =
                object : FontsContractCompat.FontRequestCallback() {
                    override fun onTypefaceRetrieved(typeface: Typeface) {
                        val retrievedWeight =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                typeface.weight
                            } else {
                                weight
                            }
                        Log.d(
                            "DownloadableTypeface",
                            "Successfully retrieved typeface: $fontName -> $typeface (weight=$retrievedWeight, italic=${typeface.isItalic})",
                        )
                        fontInstance.updateTypeface(typeface)
                    }

                    override fun onTypefaceRequestFailed(reason: Int) {
                        Log.e(
                            "DownloadableTypeface",
                            "Failed to retrieve typeface: $fontName, reason: $reason",
                        )
                    }
                }

            val timeout = if (isBlocking) BLOCKING_TIMEOUT_MS else 0
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
                fontInstance.updateTypeface(typeface)
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

    private inner class DownloadableFontInstance(
        private val realFontName: String,
        private val weight: Int,
        private val italic: Boolean,
        private var typeface: Typeface,
    ) : FontInstance {
        private var listener: Runnable? = null
        private val variationCache = mutableMapOf<String, Typeface>()

        fun updateTypeface(typeface: Typeface) {
            Log.d("DownloadableTypeface", "updateTypeface: updating cached typeface to $typeface")
            this.typeface = typeface
            listener?.run()
        }

        override fun getTypeface(): Typeface = typeface

        override fun applyVariationSettings(tags: Array<String>, values: FloatArray): Typeface {
            if (tags.isEmpty()) return typeface
            if (!validateVariationAxes(tags, values)) return typeface

            val sb = StringBuilder()
            for (i in tags.indices) {
                if (i > 0) sb.append(",")
                sb.append("'").append(tags[i]).append("' ").append(values[i])
            }
            val variationStr = sb.toString()

            val paint = Paint()
            paint.typeface = typeface
            paint.fontVariationSettings = variationStr
            val paintTypeface = paint.typeface
            if (paintTypeface != null && paintTypeface != typeface) {
                Log.d(
                    "DownloadableTypeface",
                    "Successfully applied variation settings via Paint to $realFontName: $variationStr",
                )
                return paintTypeface
            }

            variationCache[variationStr]?.let {
                return it
            }

            val query =
                "name=$realFontName&weight=$weight&italic=${if (italic) 1 else 0}&besteffort=true"
            val request =
                FontRequest(
                    GMS_PROVIDER_AUTHORITY,
                    GMS_PROVIDER_PACKAGE,
                    query,
                    R.array.com_google_android_gms_fonts_certs,
                    variationStr,
                )

            val timeout = if (isBlocking) BLOCKING_TIMEOUT_MS else 0
            val handler = Handler(Looper.getMainLooper())
            val callback = object : FontsContractCompat.FontRequestCallback() {}
            val requestedTypeface =
                FontsContractCompat.requestFont(
                    context,
                    request,
                    Typeface.NORMAL,
                    isBlocking,
                    timeout,
                    handler,
                    callback,
                )

            if (requestedTypeface != null) {
                Log.d(
                    "DownloadableTypeface",
                    "Successfully retrieved variable typeface for $realFontName with settings: $variationStr",
                )
                variationCache[variationStr] = requestedTypeface
                return requestedTypeface
            }

            Log.w(
                "DownloadableTypeface",
                "Failed to apply variation settings '$variationStr' to $realFontName, returning default typeface",
            )
            return typeface
        }

        private fun validateVariationAxes(tags: Array<String>, values: FloatArray): Boolean {
            if (tags.size != values.size) {
                Log.w(
                    "DownloadableTypeface",
                    "Mismatched tags (${tags.size}) and values (${values.size}) for $realFontName",
                )
                return false
            }
            for (tag in tags) {
                if (tag.length != 4) {
                    Log.w(
                        "DownloadableTypeface",
                        "Invalid font variation axis tag '$tag' for $realFontName (axis tag must be exactly 4 characters)",
                    )
                    return false
                }
            }
            return true
        }

        override fun setOnLoadedListener(listener: Runnable) {
            this.listener = listener
        }
    }

    private companion object {
        private const val DEFAULT_WEIGHT = 400
        private const val BLOCKING_TIMEOUT_MS = 3000
        private const val DEFAULT_PREFETCH_TIMEOUT_MS = 5000L
        private const val GMS_PROVIDER_AUTHORITY = "com.google.android.gms.fonts"
        private const val GMS_PROVIDER_PACKAGE = "com.google.android.gms"
    }
}
