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

package androidx.text.vertical

import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.StyleSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

// Helper function to simulate IPC transfer
internal fun <T> parcelRoundTrip(input: T, creator: Parcelable.Creator<T>): T {
    val parcel = Parcel.obtain()
    try {
        // Write
        (input as Parcelable).writeToParcel(parcel, 0)

        // Reset for reading
        parcel.setDataPosition(0)

        // Read
        return creator.createFromParcel(parcel)
    } finally {
        parcel.recycle()
    }
}

@RunWith(AndroidJUnit4::class)
class RubySpanTest {
    private val RUBY_TEXT = "ABCDE"

    @Test
    fun constructor_Position() {
        RubySpan(RUBY_TEXT, AnnotationPosition.After).run {
            assertThat(text).isEqualTo(RUBY_TEXT)
            assertThat(position).isEqualTo(AnnotationPosition.After)
        }

        RubySpan(RUBY_TEXT, AnnotationPosition.After).run {
            assertThat(text).isEqualTo(RUBY_TEXT)
            assertThat(position).isEqualTo(AnnotationPosition.After)
        }
    }

    @Test
    fun parcelable_parcelRoundTrip() {
        // Given a SpannableString with a simple bold span on the first two characters
        val spannedText = SpannableString("ABCDE")
        spannedText.setSpan(StyleSpan(Typeface.BOLD), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val original =
            RubySpan(
                text = spannedText,
                position = AnnotationPosition.After,
                orientation = TextOrientation.Upright,
                textScale = 0.3f,
            )

        // WHEN we write it to a Parcel and read it back
        val restored = parcelRoundTrip(original, RubySpan.CREATOR)

        // THEN the restored object must be identical
        // Only TextUtils.equals checks both the character data and the attached spans
        assertThat(TextUtils.equals(restored.text, original.text)).isTrue()
        assertThat(restored.position).isEqualTo(original.position)
        assertThat(restored.orientation).isEqualTo(original.orientation)
        assertThat(restored.textScale).isEqualTo(original.textScale)

        // Explicitly verify the span survived the trip
        val restoredSpanned = restored.text as Spanned
        val spans = restoredSpanned.getSpans(0, restoredSpanned.length, StyleSpan::class.java)
        assertThat(spans).hasLength(1)
        assertThat(spans[0].style).isEqualTo(Typeface.BOLD)
        assertThat(restoredSpanned.getSpanStart(spans[0])).isEqualTo(0)
        assertThat(restoredSpanned.getSpanEnd(spans[0])).isEqualTo(2)
    }
}
