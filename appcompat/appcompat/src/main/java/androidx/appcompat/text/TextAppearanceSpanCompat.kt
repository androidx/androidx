/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appcompat.text

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.text.style.TextAppearanceSpan
import androidx.appcompat.R
import androidx.core.content.res.ResourcesCompat
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

/** A TextAppearanceSpan works with Android X Downloadable Font */
public class TextAppearanceSpanCompat
private constructor(
    context: Context,
    appearance: Int,
    colorList: Int,
    private val fontResId: Int,
    internal val appCompatTypeface: AtomicReference<Typeface>,
) : MetricAffectingSpan() {

    // The platform TextAppearanceSpan inherits ParcelableSpan but it cannot be supported
    // in Android X library, so wrapping it instead of extending it.
    private val platformTextAppearanceSpan = TextAppearanceSpan(context, appearance, colorList)

    override fun updateMeasureState(ds: TextPaint) {
        platformTextAppearanceSpan.updateMeasureState(ds)
        updateTypefaceIfNecessary(ds)
    }

    override fun updateDrawState(ds: TextPaint?) {
        platformTextAppearanceSpan.updateDrawState(ds)
        updateTypefaceIfNecessary(ds)
    }

    private fun updateTypefaceIfNecessary(paint: Paint?) {
        if (paint == null) {
            return
        }

        if (fontResId == -1) {
            // Nothing is specified in android:fontFamily. Everything handled by platform.
            return
        }

        if (Build.VERSION.SDK_INT >= 29 && platformTextAppearanceSpan.typeface != null) {
            // Do nothing if android:fontFamily is processed by platform. This indicates the
            // downloadable is unlikely specified in the TextAppearance.
            return
        }

        val appCompatTypeface = appCompatTypeface.get()
        if (appCompatTypeface == null) {
            // The font file is not fetched yet.
            return
        }

        paint.typeface = appCompatTypeface
    }

    public companion object {
        /**
         * Creates a TextAppearanceSpanCompat.
         *
         * The android:fontFamily may need to fetched asynchronously. The returned
         * TextAppearanceSpanCompat may not immediately reflect the custom font, but it will be
         * updated once the font is fetched. The provided callback will not be executed if the
         * asynchronous fetch is not needed.
         *
         * @param context The context to use.
         * @param appearance The resource ID of a TextAppearance style.
         * @param colorList The resource ID of a ColorStateList.
         * @param executor The executor to use for fetching the font.
         * @param onComplete A runnable to be executed when the font fetching is complete. This
         *   runnable will be executed on the executor's thread. The client is responsible for
         *   invalidating the text layout if necessary.
         * @return A TextAppearanceSpanCompat.
         */
        @JvmStatic
        public fun create(
            context: Context,
            appearance: Int,
            colorList: Int,
            executor: Executor,
            onComplete: Runnable,
        ): TextAppearanceSpanCompat =
            createInternal(
                context,
                appearance,
                colorList,
                executor,
                onComplete,
                DEFAULT_FONT_FETCHER,
            )

        private fun extractFontFamilyAttribute(context: Context, appearance: Int): Int {
            val a = context.obtainStyledAttributes(appearance, R.styleable.TextAppearance)
            try {
                return a.getResourceId(R.styleable.TextAppearance_android_fontFamily, -1)
            } finally {
                a.recycle()
            }
        }

        // Interface for mocking font fetch logic in the unit test.
        internal interface FontFetcher {
            fun getCachedFont(context: Context, fontResId: Int): Typeface?

            fun getFont(context: Context, fontResId: Int): Typeface?
        }

        private val DEFAULT_FONT_FETCHER =
            object : FontFetcher {
                override fun getCachedFont(context: Context, fontResId: Int): Typeface? {
                    return ResourcesCompat.getCachedFont(context, fontResId)
                }

                override fun getFont(context: Context, fontResId: Int): Typeface? {
                    return ResourcesCompat.getFont(context, fontResId)
                }
            }

        internal fun createInternal(
            context: Context,
            appearance: Int,
            colorList: Int,
            executor: Executor,
            onComplete: Runnable,
            fetcher: FontFetcher,
        ): TextAppearanceSpanCompat {
            val fontResId = extractFontFamilyAttribute(context, appearance)
            if (fontResId == -1) {
                // If nothing is specified to android:fontFamily, do nothing.
                return TextAppearanceSpanCompat(
                    context,
                    appearance,
                    colorList,
                    fontResId,
                    AtomicReference(null),
                )
            }

            // Check if the typeface is already cached.
            val typeface = fetcher.getCachedFont(context, fontResId)
            if (typeface != null) {
                return TextAppearanceSpanCompat(
                    context,
                    appearance,
                    colorList,
                    fontResId,
                    AtomicReference(typeface),
                )
            }

            // Need to fetch the font in background thread.
            val appCompatTypeface = AtomicReference<Typeface>(null)
            // Use application context to avoid leaking activity context.
            val appContext = context.applicationContext
            executor.execute {
                val typeface = fetcher.getFont(appContext, fontResId)
                if (typeface != null) {
                    appCompatTypeface.set(typeface)
                }
                onComplete.run()
            }
            return TextAppearanceSpanCompat(
                context,
                appearance,
                colorList,
                fontResId,
                appCompatTypeface,
            )
        }
    }
}
