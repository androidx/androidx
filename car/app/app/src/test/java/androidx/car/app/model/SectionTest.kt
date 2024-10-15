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

package androidx.car.app.model

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.annotations.CarProtocol
import androidx.car.app.testing.TestDelegateInvoker.requestAllItemsForTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SectionTest {
    /** An example item containing a uniquely identifying field. */
    @CarProtocol
    private data class TestItem(val someUniquelyIdentifyingField: Int) : Item {
        constructor() : this(-1)
    }

    /** An empty section implementation to test the base class. */
    @CarProtocol
    private class TestSection(builder: Builder) : Section<TestItem>(builder) {
        /** An empty builder implementation to test the base class. */
        class Builder : BaseBuilder<TestItem, Builder>() {
            fun build(): TestSection = TestSection(this)
        }
    }

    @Test
    fun getItems() {
        val item = TestItem(1)
        val section = TestSection.Builder().addItem(item).build()

        assertThat(section.itemsDelegate.requestAllItemsForTest()).containsExactly(item)
    }

    @Test
    fun getTitle() {
        val header = CarText.create("test header")
        val section = TestSection.Builder().setTitle(header).build()

        assertThat(section.title).isEqualTo(header)
    }

    @Test
    fun getNoItemsMessage() {
        val message = CarText.create("No items!")
        val section = TestSection.Builder().setNoItemsMessage(message).build()

        assertThat(section.noItemsMessage).isEqualTo(message)
    }

    @Test
    fun setTitleCarText_throwsException_whenMoreThanJustText() {
        try {
            // #setTitle(CarText)
            TestSection.Builder().setTitle(CarText.create(createNonTextOnlyCharSequence()))
            assertWithMessage("Expected builder to throw an exception, but it didn't").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("type is not allowed")
        }
    }

    @Test
    fun setTitleCharSequence_throwsException_whenMoreThanJustText() {
        try {
            // #setTitle(CharSequence)
            TestSection.Builder().setTitle(createNonTextOnlyCharSequence())
            assertWithMessage("Expected builder to throw an exception, but it didn't").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("type is not allowed")
        }
    }

    @Test
    fun setNoItemsMessageCarText_throwsException_whenMoreThanJustText() {
        try {
            // #setNoItemsMessage(CarText)
            TestSection.Builder().setNoItemsMessage(CarText.create(createNonTextOnlyCharSequence()))
            assertWithMessage("Expected builder to throw an exception, but it didn't").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("type is not allowed")
        }
    }

    @Test
    fun setNoItemsMessageCharSequence_throwsException_whenMoreThanJustText() {
        try {
            // #setNoItemsMessage(CarText)
            TestSection.Builder().setNoItemsMessage(createNonTextOnlyCharSequence())
            assertWithMessage("Expected builder to throw an exception, but it didn't").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("type is not allowed")
        }
    }

    @Test
    fun equals_returnsFalse_whenPassedNull() {
        val section = TestSection.Builder().build()

        assertThat(section.equals(null)).isFalse()
    }

    @Test
    fun equals_isReflexive() {
        val section =
            TestSection.Builder()
                .addItem(TestItem(1))
                .addItem(TestItem(2))
                .setNoItemsMessage("Some message")
                .setTitle("some title")
                .build()

        @Suppress("ReplaceCallWithBinaryOperator") assertThat(section.equals(section)).isTrue()
    }

    @Test
    fun equals_returnsTrue_whenSectionsHaveTheSameContent() {
        val section1 =
            TestSection.Builder()
                .addItem(TestItem(1))
                .addItem(TestItem(2))
                .setNoItemsMessage("Some message")
                .setTitle("some title")
                .build()
        val section2 =
            TestSection.Builder()
                .addItem(TestItem(1))
                .addItem(TestItem(2))
                .setNoItemsMessage("Some message")
                .setTitle("some title")
                .build()

        // Is symmetric
        assertThat(section1).isEqualTo(section2)
        assertThat(section2).isEqualTo(section1)
    }

    @Test
    fun equals_returnsFalse_whenNotEqual() {
        val sections =
            listOf(
                TestSection.Builder().build(),
                TestSection.Builder().addItem(TestItem(1)).build(),
                TestSection.Builder().addItem(TestItem(2)).build(),
                TestSection.Builder().setTitle("title").build(),
                TestSection.Builder().setNoItemsMessage("no items").build()
            )

        // Test all different sections against each other
        for (i in sections.indices) {
            for (j in sections.indices) {
                if (i == j) {
                    continue
                }
                assertThat(sections[i]).isNotEqualTo(sections[j])
            }
        }
    }

    private fun createNonTextOnlyCharSequence(): CharSequence {
        val result = SpannableString("Example text")
        val colorSpan = ForegroundCarColorSpan.create(CarColor.RED)
        result.setSpan(colorSpan, 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        return result
    }
}
