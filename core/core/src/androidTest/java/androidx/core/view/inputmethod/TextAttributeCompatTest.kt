/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.core.view.inputmethod

import android.os.Build
import android.os.PersistableBundle
import android.view.inputmethod.TextAttribute
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class TextAttributeCompatTest {

    @Test
    fun testBuilderAndGetters() {
        val compat =
            TextAttributeCompat.Builder()
                .setTextConversionSuggestions(SUGGESTIONS)
                .setTextSuggestionSelected(false)
                .setExtras(EXTRA_BUNDLE)
                .build()

        assertEquals(SUGGESTIONS, compat.textConversionSuggestions)
        assertFalse(compat.isTextSuggestionSelected)
        assertEquals(EXTRA_BUNDLE, compat.extras)
    }

    @Test
    fun testDefaults() {
        val compat = TextAttributeCompat.Builder().build()

        assertTrue(compat.textConversionSuggestions.isEmpty())
        assertFalse(compat.isTextSuggestionSelected)
        assertNotNull(compat.extras)
    }

    @Test
    fun testWrapNull() {
        assertNull(TextAttributeCompat.wrap(null))
    }

    @Test
    fun testWrapTextAttribute() {
        if (Build.VERSION.SDK_INT >= 37) {
            val textAttribute =
                TextAttribute.Builder()
                    .setTextConversionSuggestions(SUGGESTIONS)
                    .setExtras(EXTRA_BUNDLE)
                    .setTextSuggestionSelected(true)
                    .build()

            val compat = TextAttributeCompat.wrap(textAttribute)
            assertNotNull(compat)
            assertEquals(
                textAttribute.textConversionSuggestions,
                compat!!.textConversionSuggestions,
            )
            assertEquals(textAttribute.isTextSuggestionSelected, compat.isTextSuggestionSelected)
            assertEquals(textAttribute.extras, compat.extras)
        } else if (Build.VERSION.SDK_INT >= 33) {
            val textAttribute =
                TextAttribute.Builder()
                    .setTextConversionSuggestions(SUGGESTIONS)
                    .setExtras(EXTRA_BUNDLE)
                    .build()

            val compat = TextAttributeCompat.wrap(textAttribute)
            assertNotNull(compat)
            assertEquals(
                textAttribute.textConversionSuggestions,
                compat!!.textConversionSuggestions,
            )
            assertFalse(compat.isTextSuggestionSelected)
            assertEquals(textAttribute.extras, compat.extras)
        }
    }

    @Test
    fun testUnwrapFallback() {
        val compat = TextAttributeCompat.Builder().build()
        if (Build.VERSION.SDK_INT >= 33) {
            assertNotNull(compat.unwrap())
        } else {
            assertNull(compat.unwrap())
        }
    }

    companion object {
        private const val SUGGESTION = "suggestion"
        private const val EXTRAS_KEY = "extras_key"
        private const val EXTRAS_VALUE = "extras_value"

        private val SUGGESTIONS = listOf(SUGGESTION)
        private val EXTRA_BUNDLE = PersistableBundle().apply { putString(EXTRAS_KEY, EXTRAS_VALUE) }
    }
}
