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

package androidx.wear.protolayout.renderer.inflater

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.SparseLongArray
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.wear.protolayout.renderer.inflater.ConstrainedImageDecoder.DEFAULT_DECODE_HARD_LIMIT_PX
import androidx.wear.protolayout.renderer.inflater.ConstrainedImageDecoder.checkSize
import java.util.WeakHashMap
import org.xmlpull.v1.XmlPullParser

/**
 * Validates compiled Android XML drawable resources and any nested drawable or bitmap references
 * against per-image dimension and cumulative pixel memory budgets before inflation.
 */
internal object XmlDrawableValidator {
    private const val MAX_CUMULATIVE_PIXELS: Long =
        DEFAULT_DECODE_HARD_LIMIT_PX.toLong() * DEFAULT_DECODE_HARD_LIMIT_PX
    private const val UNVERIFIED: Long = -1L
    private const val VERIFIED_VECTOR: Long = -2L

    const val ANDROID_NAMESPACE: String = "http://schemas.android.com/apk/res/android"
    const val TAG_VECTOR: String = "vector"
    const val TAG_SHAPE: String = "shape"
    const val TAG_ANIMATED_VECTOR: String = "animated-vector"
    const val ATTR_DRAWABLE: String = "drawable"

    /**
     * Memoizes verified raster pixel cost (`>= 0L`) or [VERIFIED_VECTOR] (`-2L`) per `(Resources,
     * resId)` without retaining Drawables or Bitmaps.
     */
    private val verifiedPixelCostCache = WeakHashMap<Resources, SparseLongArray>()

    private class TraversalState {
        val inProgress = mutableSetOf<Int>()
        val outValue = TypedValue()
        val bitmapOptions =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                inScaled = false
            }
    }

    /**
     * Verifies an XML drawable, strictly enforcing that its root element is a `<vector>`.
     *
     * Because standard Android `<vector>` elements cannot contain embedded raster images, this
     * method operates in O(1) time by checking the root tag and immediately returning. Repeated
     * calls for the same `(res, resId)` return immediately from [verifiedPixelCostCache] without
     * crossing JNI. It serves as a strict security guard to prevent unbounded memory allocation,
     * ensuring the provided resource is a pure vector graphic (for example, when parsing the target
     * of an `<animated-vector>`).
     *
     * @param res The [Resources] instance to resolve the drawable.
     * @param resId The resource ID of the XML drawable to verify.
     * @throws IllegalArgumentException if the resolved resource is a raster image, or if the XML
     *   root tag is not `<vector>`.
     */
    @JvmStatic
    @SuppressLint("ResourceType")
    fun verifyVectorDrawableXml(res: Resources, @DrawableRes resId: Int) {
        val cached = getCachedEntry(res, resId)
        if (cached == VERIFIED_VECTOR) {
            return
        }
        if (cached == UNVERIFIED) {
            val outValue = TypedValue()
            res.getValue(resId, outValue, /* resolveRefs= */ true)
            if (outValue.string?.endsWith(".xml") == true) {
                res.getXml(resId).use { parser ->
                    var type = parser.next()
                    while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
                        type = parser.next()
                    }
                    if (type == XmlPullParser.START_TAG && parser.name == TAG_VECTOR) {
                        addCacheEntry(res, resId, VERIFIED_VECTOR)
                        return
                    }
                }
            }
        }
        throw IllegalArgumentException(
            "Only XML <vector> drawables are supported to prevent unbounded memory allocation."
        )
    }

    /**
     * Verifies an XML drawable and any transitively referenced child drawables or raster images
     * against per-image dimension and cumulative pixel memory limits before inflation.
     *
     * Used when decoding compiled XML drawables (`*.xml`) that cannot be inspected directly by
     * [android.graphics.ImageDecoder]. Repeated lookups for the same `(res, resId)` return in O(1)
     * time via an internal pixel-cost cache, and pure `<vector>` or `<shape>` drawables
     * short-circuit in O(1) time at the root start tag without allocating traversal state.
     * Composite drawables (such as `<layer-list>`, `<selector>`, `<bitmap>`, `<nine-patch>`, or
     * `<animated-vector>`) are recursively scanned to verify unscaled raster bitmap headers and
     * enforce the cumulative pixel budget.
     *
     * @param res The [Resources] instance to resolve the drawable.
     * @param resId The resource ID of the XML drawable to verify.
     * @throws IllegalArgumentException if any referenced raster bitmap exceeds
     *   [DEFAULT_DECODE_HARD_LIMIT_PX], cumulative raster pixels exceed the memory budget, an
     *   `<animated-vector>` targets a non-`<vector>` drawable, or the XML has no root start tag.
     */
    @JvmStatic
    @SuppressLint("ResourceType")
    fun verifyDrawableXml(res: Resources, @DrawableRes resId: Int) {
        if (getCachedEntry(res, resId) != UNVERIFIED) {
            return
        }
        val entry = computeXmlEntry(res = res, resId = resId, state = null)
        addCacheEntry(res, resId, entry)
    }

    /**
     * Resolves [resId] and returns its total raster pixel allocation cost (expressed as total pixel
     * area: width * height), memoizing the result per `(res, resId)` upon success.
     *
     * A previously verified entry in [verifiedPixelCostCache] returns its non-negative pixel cost
     * immediately in O(1) time. Otherwise, [resId] is classified into one of three cases via
     * [Resources.getValue]:
     * - Non-file primitive values (such as colors) or unresolvable resource IDs contribute `0L`
     *   pixels.
     * - Raw raster assets (`!path.endsWith(".xml")`) have their unscaled image headers decoded via
     *   [BitmapFactory] (`inJustDecodeBounds = true`, `inScaled = false`) to enforce
     *   [DEFAULT_DECODE_HARD_LIMIT_PX] and compute `width * height`.
     * - Compiled XML drawables (`*.xml`) delegate to [computeXmlEntry].
     *
     * Cycle detection via [TraversalState.inProgress] prevents infinite recursion on cyclic
     * resource references.
     *
     * @param res The [Resources] instance to resolve [resId].
     * @param resId The drawable resource ID whose pixel cost is being evaluated.
     * @param state Reusable traversal state holding scratch buffers and active recursion set.
     * @return The verified cumulative raster pixel cost (`width * height`) of [resId].
     * @throws IllegalArgumentException if any dimension or cumulative pixel limit is exceeded.
     */
    @SuppressLint("ResourceType")
    private fun getOrComputePixelCost(
        res: Resources,
        @DrawableRes resId: Int,
        state: TraversalState,
    ): Long {
        val cached = getCachedEntry(res, resId)
        if (cached != UNVERIFIED) {
            return cached.coerceAtLeast(0L)
        }
        if (!state.inProgress.add(resId)) {
            return 0L
        }
        try {
            try {
                res.getValue(resId, state.outValue, /* resolveRefs= */ true)
            } catch (_: Resources.NotFoundException) {
                addCacheEntry(res, resId, 0L)
                return 0L
            }
            val path = state.outValue.string
            val entry =
                when {
                    // Non-file primitive resource (e.g., color integer) allocates no raster pixels.
                    path == null -> 0L
                    // Binary raster image: inspect unscaled header bounds without decoding bitmap
                    // pixels.
                    !path.endsWith(".xml") -> {
                        state.bitmapOptions.outWidth = 0
                        state.bitmapOptions.outHeight = 0
                        res.openRawResource(resId, state.outValue).use { stream ->
                            BitmapFactory.decodeStream(stream, null, state.bitmapOptions)
                        }
                        checkSize(state.bitmapOptions.outWidth, state.bitmapOptions.outHeight)
                        state.bitmapOptions.outWidth.toLong() *
                            state.bitmapOptions.outHeight.toLong()
                    }
                    // Compiled XML drawable: recursively accumulate raster pixel costs across child
                    // elements.
                    else -> computeXmlEntry(res = res, resId = resId, state = state)
                }

            addCacheEntry(res, resId, entry)
            return entry.coerceAtLeast(0L)
        } finally {
            state.inProgress.remove(resId)
        }
    }

    /**
     * Parses the compiled XML drawable [resId] and computes its cache entry ([VERIFIED_VECTOR] for
     * pure `<vector>` drawables, or the non-negative cumulative raster pixel cost for all other XML
     * drawables).
     *
     * Pure `<vector>`, `<shape>`, and root `<animated-vector>` elements short-circuit immediately
     * in O(1) time at the root start tag without allocating [TraversalState]. For composite XML
     * elements (such as `<layer-list>`, `<selector>`, `<bitmap>`, or `<nine-patch>`), every
     * attribute referencing a resource ID is recursively evaluated via [getOrComputePixelCost].
     *
     * @param res The [Resources] instance to open the compiled XML parser.
     * @param resId The XML drawable resource ID to parse.
     * @param state Optional existing [TraversalState], or `null` if not yet allocated for a root
     *   call.
     * @return [VERIFIED_VECTOR] if [resId] is a `<vector>`, otherwise its cumulative raster pixel
     *   cost (`>= 0L`).
     * @throws IllegalArgumentException if no XML start tag is found, any child raster bitmap
     *   exceeds [DEFAULT_DECODE_HARD_LIMIT_PX], or cumulative pixels exceed
     *   [MAX_CUMULATIVE_PIXELS].
     */
    @SuppressLint("ResourceType")
    private fun computeXmlEntry(
        res: Resources,
        @DrawableRes resId: Int,
        state: TraversalState?,
    ): Long {
        var activeState = state
        var totalPixels = 0L
        res.getXml(resId).use { parser ->
            var type = parser.next()
            var hasStartTag = false
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    if (!hasStartTag) {
                        hasStartTag = true
                        // O(1) fast path: <vector> and <shape> cannot contain raster bitmaps.
                        when (parser.name) {
                            TAG_VECTOR -> return VERIFIED_VECTOR
                            TAG_SHAPE -> return 0L
                        }
                    }
                    if (parser.name == TAG_ANIMATED_VECTOR) {
                        val avdTargetId =
                            parser.getAttributeResourceValue(ANDROID_NAMESPACE, ATTR_DRAWABLE, 0)
                        if (avdTargetId != 0) {
                            verifyVectorDrawableXml(res, avdTargetId)
                        }
                        if (parser.depth == 1) {
                            return 0L
                        }
                    } else {
                        val currentState =
                            activeState
                                ?: TraversalState().also {
                                    it.inProgress.add(resId)
                                    activeState = it
                                }
                        for (i in 0 until parser.attributeCount) {
                            val attrResId = parser.getAttributeResourceValue(i, 0)
                            if (attrResId != 0) {
                                totalPixels +=
                                    getOrComputePixelCost(
                                        res = res,
                                        resId = attrResId,
                                        state = currentState,
                                    )
                                if (totalPixels > MAX_CUMULATIVE_PIXELS) {
                                    throw IllegalArgumentException(
                                        "Cumulative image size exceeds the memory budget."
                                    )
                                }
                            }
                        }
                    }
                }
                type = parser.next()
            }
            if (!hasStartTag) {
                throw IllegalArgumentException("No start tag found in XML resource")
            }
        }
        return totalPixels
    }

    private fun getCachedEntry(res: Resources, @DrawableRes resId: Int): Long =
        synchronized(verifiedPixelCostCache) {
            verifiedPixelCostCache[res]?.get(resId, UNVERIFIED) ?: UNVERIFIED
        }

    private fun addCacheEntry(res: Resources, @DrawableRes resId: Int, entry: Long) {
        synchronized(verifiedPixelCostCache) {
            verifiedPixelCostCache.getOrPut(res) { SparseLongArray(4) }.put(resId, entry)
        }
    }
}
