/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.common

import android.content.Context
import android.text.Annotation as AnnotationSpan
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE
import android.util.Log
import androidx.annotation.StringRes
import androidx.car.app.model.CarColor
import androidx.car.app.model.ClickableSpan
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.utils.LogTags.TAG

/**
 * Provides [SpannableStringBuilder] extension methods, which can be used to find & replace
 * [android.text.Annotation] tags.
 */
object SpannableStringBuilderAnnotationExtensions {
    @JvmStatic
    fun Context.getSpannableStringBuilder(@StringRes resId: Int): SpannableStringBuilder =
        SpannableStringBuilder(getText(resId))

    private fun SpannableStringBuilder.findAnnotationBounds(
        key: String,
        value: String
    ): Pair<Int, Int>? =
        getSpans(0, length, AnnotationSpan::class.java)
            .find { it.key == key && it.value == value }
            ?.let { Pair(getSpanStart(it), getSpanEnd(it)) }

    /**
     * Nests the provided span within an existing [android.text.Annotation].
     *
     * The existing [android.text.Annotation] is found via the provided [key] and [value]. If no
     * [android.text.Annotation] is found, then a new span will not be added.
     */
    @JvmStatic
    fun SpannableStringBuilder.addSpanToAnnotatedPosition(
        key: String,
        value: String,
        span: Any
    ): SpannableStringBuilder {
        findAnnotationBounds(key, value)?.let {
            setSpan(span, it.first, it.second, SPAN_INCLUSIVE_INCLUSIVE)
        } ?: Log.e(TAG, "Unable to find annotation span for $key:$value")

        return this
    }

    @JvmStatic
    fun SpannableStringBuilder.addSpanToAnnotatedPosition(
        key: String,
        value: String,
        color: CarColor
    ): SpannableStringBuilder =
        addSpanToAnnotatedPosition(key, value, ForegroundCarColorSpan.create(color))

    @JvmStatic
    fun SpannableStringBuilder.addSpanToAnnotatedPosition(
        key: String,
        value: String,
        onClick: () -> Unit
    ): SpannableStringBuilder =
        addSpanToAnnotatedPosition(key, value, ClickableSpan.create(onClick))
}
