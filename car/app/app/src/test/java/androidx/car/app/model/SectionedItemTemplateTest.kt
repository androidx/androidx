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

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Suppress("DEPRECATION") // Tests must still test deprecated fields/methods.
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SectionedItemTemplateTest {
    class MyCustomSection : Section<Row>()

    private val testSections =
        listOf(
            RowSection.Builder()
                .addItem(Row.Builder().setTitle("my title").build())
                .setTitle(CarText.Builder("Section 1").build())
                .build(),
            GridSection.Builder()
                .addItem(GridItem.Builder().setImage(CarIcon.COMPOSE_MESSAGE).build())
                .setTitle(CarText.Builder("Section 2").build())
                .setItemSize(GridSection.ITEM_SIZE_EXTRA_LARGE)
                .build(),
        )
    private val testActions =
        listOf(
            Action.Builder()
                .setIcon(CarIcon.COMPOSE_MESSAGE)
                .setBackgroundColor(CarColor.RED)
                .build(),
            Action.Builder()
                .setIcon(CarIcon.COMPOSE_MESSAGE)
                .setBackgroundColor(CarColor.BLUE)
                .build(),
        )
    private val testHeader = Header.Builder().setTitle("My title").build()

    @Test
    fun getSections() {
        val template = SectionedItemTemplate.Builder().setSections(testSections).build()

        assertThat(template.sections).containsExactlyElementsIn(testSections)
    }

    @Test
    fun getActions() {
        val template = SectionedItemTemplate.Builder().setActions(testActions).build()

        assertThat(template.actions).containsExactlyElementsIn(testActions)
    }

    @Test
    fun getHeader() {
        val template = SectionedItemTemplate.Builder().setHeader(testHeader).build()

        assertThat(template.header).isEqualTo(testHeader)
    }

    @Test
    fun isLoading() {
        val template = SectionedItemTemplate.Builder().setLoading(true).build()

        assertThat(template.isLoading).isTrue()
    }

    @Test
    fun isAlphabeticalIndexingAllowed() {
        val template = SectionedItemTemplate.Builder().setAlphabeticalIndexingAllowed(true).build()

        assertThat(template.isAlphabeticalIndexingAllowed).isTrue()
    }

    // This is a compat layer test to verify that apps still setting the old field will have the
    // setting respected in the new field.
    @Test
    fun getAlphabeticalIndexingStrategy_returnsIgnoreArticlesAndSymbols_whenDeprecatedAlphabeticalIndexingAllowedFieldIsSet() {
        val template = SectionedItemTemplate.Builder().setAlphabeticalIndexingAllowed(true).build()

        assertThat(template.alphabeticalIndexingStrategy)
            .isEqualTo(
                SectionedItemTemplate.ALPHABETICAL_INDEXING_TITLE_IGNORE_ARTICLES_AND_SYMBOLS
            )
        assertThat(template.isAlphabeticalIndexingAllowed).isTrue()
    }

    @Test
    fun getAlphabeticalIndexingStrategy() {
        val template =
            SectionedItemTemplate.Builder()
                .setAlphabeticalIndexingStrategy(
                    SectionedItemTemplate.ALPHABETICAL_INDEXING_TITLE_AS_IS
                )
                .build()

        assertThat(template.alphabeticalIndexingStrategy)
            .isEqualTo(SectionedItemTemplate.ALPHABETICAL_INDEXING_TITLE_AS_IS)
        assertThat(template.isAlphabeticalIndexingAllowed).isTrue()
    }

    @Test
    fun getAlphabeticalIndexingStrategy_defaultValue_returnsDisabled() {
        val template = SectionedItemTemplate.Builder().build()

        assertThat(template.alphabeticalIndexingStrategy)
            .isEqualTo(SectionedItemTemplate.ALPHABETICAL_INDEXING_DISABLED)
        assertThat(template.isAlphabeticalIndexingAllowed).isFalse()
    }

    @Test
    fun getScrollStatePersistenceStrategy() {
        val template =
            SectionedItemTemplate.Builder()
                .setScrollStatePersistenceStrategy(
                    SectionedItemTemplate.SCROLL_STATE_PRESERVE_INDEX
                )
                .build()

        assertThat(template.scrollStatePersistenceStrategy)
            .isEqualTo(SectionedItemTemplate.SCROLL_STATE_PRESERVE_INDEX)
    }

    @Test
    fun getScrollStatePersistenceStrategy_defaultValue_returnsResetToTop() {
        val template = SectionedItemTemplate.Builder().build()

        assertThat(template.scrollStatePersistenceStrategy)
            .isEqualTo(SectionedItemTemplate.SCROLL_STATE_PRESERVE_INDEX)
    }

    @Test
    fun build_throwsException_whenLoadingAndContainsSections() {
        try {
            SectionedItemTemplate.Builder()
                .setLoading(true)
                .addSection(RowSection.Builder().build())
                .build()
            assertWithMessage("Expected builder to throw exception, but it didn't").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("cannot both be in a loading state and have sections")
        }
    }

    @Test
    fun build_throwsException_whenAddingNonSupportedSectionType() {
        try {
            SectionedItemTemplate.Builder().addSection(MyCustomSection()).build()
            assertWithMessage("Expected builder to throw exception, but it didn't").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("are allowed in SectionedItemTemplate")
        }
    }

    @Test
    fun build_throwsException_whenFilterChipSectionIsNotFirst() {
        try {
            SectionedItemTemplate.Builder()
                .addSection(RowSection.Builder().build())
                .addSection(buildFilterChipSection())
                .build()
            assertWithMessage("Expected builder to throw exception, but it didn't").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("must be the first section")
        }
    }

    @Test
    fun build_throwsException_whenMultipleFilterChipSections() {
        try {
            SectionedItemTemplate.Builder()
                .addSection(buildFilterChipSection())
                .addSection(buildFilterChipSection())
                .build()
            assertWithMessage("Expected builder to throw exception, but it didn't").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Only one FilterChipSection is allowed")
        }
    }

    @Test
    fun build_allowsFilterChipSectionAsFirstSection() {
        val section = buildFilterChipSection()
        val template = SectionedItemTemplate.Builder().addSection(section).build()

        assertThat(template.sections).containsExactly(section)
    }

    @Test
    fun addAction_throwsException_whenNotFabConstrained() {
        try {
            // Back action is not allowed as a FAB
            SectionedItemTemplate.Builder().addAction(Action.BACK)
            assertWithMessage("Expected builder to throw exception, but it didn't").fail()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("is not allowed")
        }
    }

    @Test
    fun createInstance_addMediaPlaybackActionAsRowSecondaryAction() {
        val sections = listOf(RowSection.Builder().addItem(createRowWithMediaAction()).build())
        val template = SectionedItemTemplate.Builder().setSections(sections).build()

        assertThat(template.sections).containsExactlyElementsIn(sections)
    }

    @Test
    fun setActions_throwsException_whenNotFabConstrained() {
        try {
            // Cannot have more than 3 actions
            SectionedItemTemplate.Builder()
                .setActions(
                    List(3) {
                        Action.Builder()
                            .setTitle("Action $it")
                            .setIcon(CarIcon.COMPOSE_MESSAGE)
                            .build()
                    }
                )
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("list exceeded max number")
        }
    }

    @Test
    fun equals_null_returnsFalse() {
        assertThat(buildTemplate().equals(null)).isFalse()
    }

    @Test
    fun equals_sameInstance_returnsTrue() {
        val template = createFullyPopulatedTemplate()

        assertEqual(template, template)
    }

    @Test
    fun equals_differentTemplatesSameContent_returnsTrue() {
        assertEqual(createFullyPopulatedTemplate(), createFullyPopulatedTemplate())
    }

    @Test
    fun equals_oneDifferingField_returnsFalse() {
        val minimalTemplate = buildTemplate()

        assertNotEqual(minimalTemplate, buildTemplate { setSections(testSections) })
        assertNotEqual(minimalTemplate, buildTemplate { setHeader(testHeader) })
        assertNotEqual(minimalTemplate, buildTemplate { setActions(testActions) })
        assertNotEqual(minimalTemplate, buildTemplate { setLoading(true) })
        assertNotEqual(minimalTemplate, buildTemplate { setAlphabeticalIndexingAllowed(true) })
    }

    @Test
    fun toBuilder_build_returnsEquivalentObject() {
        val template = createFullyPopulatedTemplate()

        assertEqual(template, SectionedItemTemplate.Builder(template).build())
    }

    private fun assertEqual(obj1: Any, obj2: Any) {
        assertThat(obj1).isEqualTo(obj2)
        assertThat(obj2).isEqualTo(obj1)
        assertThat(obj1.hashCode()).isEqualTo(obj2.hashCode())
    }

    private fun assertNotEqual(obj1: Any, obj2: Any) {
        assertThat(obj1).isNotEqualTo(obj2)
        assertThat(obj2).isNotEqualTo(obj1)
        assertThat(obj1.hashCode()).isNotEqualTo(obj2.hashCode())
    }

    private fun createFullyPopulatedTemplate() =
        SectionedItemTemplate.Builder()
            .setSections(testSections)
            .setHeader(testHeader)
            .setActions(testActions)
            .setAlphabeticalIndexingAllowed(true)
            .setScrollStatePersistenceStrategy(SectionedItemTemplate.SCROLL_STATE_PRESERVE_INDEX)
            .build()

    private fun buildTemplate(block: SectionedItemTemplate.Builder.() -> Unit = {}) =
        SectionedItemTemplate.Builder().apply { block() }.build()

    private fun createRowWithMediaAction(): Row =
        Row.Builder().setTitle("Bananas").addAction(Action.MEDIA_PLAYBACK).build()

    private fun buildFilterChipSection(): FilterChipSection {
        return FilterChipSection.Builder()
            .addItem(FilterChip.Builder().setTitle("Chip").setOnClickListener {}.build())
            .build()
    }
}
