/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.CodepointTransformation
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
class TextFieldLayoutStateCacheTest {

    @get:Rule
    val rule = createComposeRule()

    private var textFieldState = TextFieldState()
    private var codepointTransformation: CodepointTransformation? = null
    private var textStyle = TextStyle()
    private var singleLine = false
    private var softWrap = false
    private var cache = TextFieldLayoutStateCache()
    private var density = Density(1f, 1f)
    private var layoutDirection = LayoutDirection.Ltr
    private var fontFamilyResolver =
        createFontFamilyResolver(InstrumentationRegistry.getInstrumentation().context)
    private var constraints = Constraints()

    @Test
    fun updateAllInputs_doesntInvalidateSnapshot_whenNothingChanged() {
        assertInvalidationsOnChange(0) {
            updateNonMeasureInputs()
            updateMeasureInputs()
        }
    }

    @Test
    fun updateNonMeasureInputs_invalidatesSnapshot_whenTextContentChanged() {
        textFieldState.edit {
            replace(0, length, "")
            placeCursorBeforeCharAt(0)
        }
        assertInvalidationsOnChange(1) {
            textFieldState.edit {
                append("hello")
                placeCursorBeforeCharAt(0)
            }
        }
    }

    @Test
    fun updateNonMeasureInputs_invalidatesSnapshot_whenTextSelectionChanged() {
        textFieldState.edit {
            append("hello")
            placeCursorBeforeCharAt(0)
        }
        assertInvalidationsOnChange(1) {
            textFieldState.edit {
                placeCursorBeforeCharAt(1)
            }
        }
    }

    @Test
    fun updateNonMeasureInputs_invalidatesSnapshot_whenCodepointTransformationChanged() {
        assertInvalidationsOnChange(1) {
            codepointTransformation = CodepointTransformation { _, codepoint -> codepoint }
            updateNonMeasureInputs()
        }
    }

    @Test
    fun updateNonMeasureInputs_invalidatesSnapshot_whenStyleLayoutAffectingAttrsChanged() {
        textStyle = TextStyle(fontSize = 12.sp)
        assertInvalidationsOnChange(1) {
            textStyle = TextStyle(fontSize = 23.sp)
            updateNonMeasureInputs()
        }
    }

    @Test
    fun updateNonMeasureInputs_doesntInvalidateSnapshot_whenStyleDrawAffectingAttrsChanged() {
        textStyle = TextStyle(color = Color.Black)
        assertInvalidationsOnChange(0) {
            textStyle = TextStyle(color = Color.Blue)
            updateNonMeasureInputs()
        }
    }

    @Test
    fun updateNonMeasureInputs_invalidatesSnapshot_whenSingleLineChanged() {
        assertInvalidationsOnChange(1) {
            singleLine = !singleLine
            updateNonMeasureInputs()
        }
    }

    @Test
    fun updateNonMeasureInputs_invalidatesSnapshot_whenSoftWrapChanged() {
        assertInvalidationsOnChange(1) {
            softWrap = !softWrap
            updateNonMeasureInputs()
        }
    }

    @Test
    fun updateMeasureInputs_invalidatesSnapshot_whenDensityInstanceChangedWithDifferentValues() {
        density = Density(1f, 1f)
        assertInvalidationsOnChange(1) {
            density = Density(1f, 2f)
            updateMeasureInputs()
        }
    }

    @Test
    fun updateMeasureInputs_doesntInvalidateSnapshot_whenDensityInstanceChangedWithSameValues() {
        density = Density(1f, 1f)
        assertInvalidationsOnChange(0) {
            density = Density(1f, 1f)
            updateMeasureInputs()
        }
    }

    @Test
    fun updateMeasureInputs_invalidatesSnapshot_whenDensityValueChangedWithSameInstance() {
        var densityValue = 1f
        density = object : Density {
            override val density: Float
                get() = densityValue
            override val fontScale: Float = 1f
        }
        assertInvalidationsOnChange(1) {
            densityValue = 2f
            updateMeasureInputs()
        }
    }

    @Test
    fun updateMeasureInputs_invalidatesSnapshot_whenFontScaleChangedWithSameInstance() {
        var fontScale = 1f
        density = object : Density {
            override val density: Float = 1f
            override val fontScale: Float
                get() = fontScale
        }
        assertInvalidationsOnChange(1) {
            fontScale = 2f
            updateMeasureInputs()
        }
    }

    @Test
    fun updateMeasureInputs_invalidatesSnapshot_whenLayoutDirectionChanged() {
        layoutDirection = LayoutDirection.Ltr
        assertInvalidationsOnChange(1) {
            layoutDirection = LayoutDirection.Rtl
            updateMeasureInputs()
        }
    }

    @Test
    fun updateMeasureInputs_invalidatesSnapshot_whenFontFamilyResolverChanged() {
        assertInvalidationsOnChange(1) {
            fontFamilyResolver =
                createFontFamilyResolver(InstrumentationRegistry.getInstrumentation().context)
            updateMeasureInputs()
        }
    }

    @Ignore("b/294443266: figure out how to make fonts stale for test")
    @Test
    fun updateMeasureInputs_invalidatesSnapshot_whenFontFamilyResolverFontChanged() {
        fontFamilyResolver =
            createFontFamilyResolver(InstrumentationRegistry.getInstrumentation().context)
        assertInvalidationsOnChange(1) {
            TODO("b/294443266: make fonts stale")
        }
    }

    @Test
    fun updateMeasureInputs_invalidatesSnapshot_whenConstraintsChanged() {
        constraints = Constraints.fixed(5, 5)
        assertInvalidationsOnChange(1) {
            constraints = Constraints.fixed(6, 5)
            updateMeasureInputs()
        }
    }

    @Test
    fun value_returnsNewLayout_whenTextContentsChanged() {
        textFieldState.edit {
            replace(0, length, "h")
            placeCursorBeforeCharAt(0)
        }
        assertLayoutChange(
            change = {
                textFieldState.edit {
                    replace(0, length, "hello")
                    placeCursorBeforeCharAt(0)
                }
            },
        ) { old, new ->
            assertThat(old.layoutInput.text.text).isEqualTo("h")
            assertThat(new.layoutInput.text.text).isEqualTo("hello")
        }
    }

    @Test
    fun value_returnsCachedLayout_whenTextSelectionChanged() {
        textFieldState.edit {
            replace(0, length, "hello")
            placeCursorBeforeCharAt(0)
        }
        assertLayoutChange(
            change = {
                textFieldState.edit {
                    placeCursorBeforeCharAt(1)
                }
            }
        ) { old, new ->
            assertThat(new).isSameInstanceAs(old)
        }
    }

    @Test
    fun value_returnsNewLayout_whenCodepointTransformationInstanceChangedWithDifferentOutput() {
        textFieldState.setTextAndPlaceCursorAtEnd("h")
        codepointTransformation = CodepointTransformation { _, codepoint -> codepoint }
        assertLayoutChange(
            change = {
                codepointTransformation = CodepointTransformation { _, codepoint -> codepoint + 1 }
                updateNonMeasureInputs()
            }
        ) { old, new ->
            assertThat(old.layoutInput.text.text).isEqualTo("h")
            assertThat(new.layoutInput.text.text).isEqualTo("i")
        }
    }

    @Test
    fun value_returnsCachedLayout_whenCodepointTransformationInstanceChangedWithSameOutput() {
        textFieldState.setTextAndPlaceCursorAtEnd("h")
        codepointTransformation = CodepointTransformation { _, codepoint -> codepoint }
        assertLayoutChange(
            change = {
                codepointTransformation = CodepointTransformation { _, codepoint -> codepoint }
                updateNonMeasureInputs()
            }
        ) { old, new ->
            assertThat(new).isSameInstanceAs(old)
        }
    }

    @Test
    fun value_returnsNewLayout_whenStyleLayoutAffectingAttributesChanged() {
        textStyle = TextStyle(fontSize = 12.sp)
        assertLayoutChange(
            change = {
                textStyle = TextStyle(fontSize = 23.sp)
                updateNonMeasureInputs()
            }
        ) { old, new ->
            assertThat(old.layoutInput.style.fontSize).isEqualTo(12.sp)
            assertThat(new.layoutInput.style.fontSize).isEqualTo(23.sp)
        }
    }

    @Test
    fun value_returnsCachedLayout_whenStyleDrawAffectingAttributesChanged() {
        textStyle = TextStyle(color = Color.Black)
        assertLayoutChange(
            change = {
                textStyle = TextStyle(color = Color.Blue)
                updateNonMeasureInputs()
            }
        ) { old, new ->
            assertThat(new).isSameInstanceAs(old)
        }
    }

    @Test
    fun value_returnsNewLayout_whenSingleLineChanged() {
        assertLayoutChange(
            change = {
                singleLine = !singleLine
                updateNonMeasureInputs()
            }
        ) { old, new ->
            assertThat(new).isNotSameInstanceAs(old)
        }
    }

    @Test
    fun value_returnsNewLayout_whenSoftWrapChanged() {
        assertLayoutChange(
            change = {
                softWrap = !softWrap
                updateNonMeasureInputs()
            }
        ) { old, new ->
            assertThat(old.layoutInput.softWrap).isEqualTo(!softWrap)
            assertThat(new.layoutInput.softWrap).isEqualTo(softWrap)
        }
    }

    @Test
    fun value_returnsNewLayout_whenDensityValueChangedWithSameInstance() {
        var densityValue = 1f
        density = object : Density {
            override val density: Float
                get() = densityValue
            override val fontScale: Float = 1f
        }
        assertLayoutChange(
            change = {
                densityValue = 2f
                updateMeasureInputs()
            }
        ) { old, new ->
            assertThat(new).isNotSameInstanceAs(old)
        }
    }

    @Test
    fun value_returnsNewLayout_whenFontScaleChangedWithSameInstance() {
        var fontScale = 1f
        density = object : Density {
            override val density: Float = 1f
            override val fontScale: Float
                get() = fontScale
        }
        assertLayoutChange(
            change = {
                fontScale = 2f
                updateMeasureInputs()
            }
        ) { old, new ->
            assertThat(new).isNotSameInstanceAs(old)
        }
    }

    @Test
    fun value_returnsCachedLayout_whenDensityInstanceChangedWithSameValues() {
        density = Density(1f, 1f)
        assertLayoutChange(
            change = {
                density = Density(1f, 1f)
                updateMeasureInputs()
            }
        ) { old, new ->
            assertThat(new).isSameInstanceAs(old)
        }
    }

    @Test
    fun value_returnsNewLayout_whenLayoutDirectionChanged() {
        layoutDirection = LayoutDirection.Ltr
        assertLayoutChange(
            change = {
                layoutDirection = LayoutDirection.Rtl
                updateMeasureInputs()
            }
        ) { old, new ->
            assertThat(old.layoutInput.layoutDirection).isEqualTo(LayoutDirection.Ltr)
            assertThat(new.layoutInput.layoutDirection).isEqualTo(LayoutDirection.Rtl)
        }
    }

    @Test
    fun value_returnsNewLayout_whenFontFamilyResolverChanged() {
        assertLayoutChange(
            change = {
                fontFamilyResolver =
                    createFontFamilyResolver(InstrumentationRegistry.getInstrumentation().context)
                updateMeasureInputs()
            }
        ) { old, new ->
            assertThat(new).isNotSameInstanceAs(old)
        }
    }

    @Ignore("b/294443266: figure out how to make fonts stale for test")
    @Test
    fun value_returnsNewLayout_whenFontFamilyResolverFontChanged() {
        fontFamilyResolver =
            createFontFamilyResolver(InstrumentationRegistry.getInstrumentation().context)
        assertLayoutChange(
            change = {
                TODO("b/294443266: make fonts stale")
            }
        ) { old, new ->
            assertThat(new).isNotSameInstanceAs(old)
        }
    }

    @Test
    fun value_returnsNewLayout_whenConstraintsChanged() {
        constraints = Constraints.fixed(5, 5)
        assertLayoutChange(
            change = {
                constraints = Constraints.fixed(6, 5)
                updateMeasureInputs()
            }
        ) { old, new ->
            assertThat(old.layoutInput.constraints).isEqualTo(Constraints.fixed(5, 5))
            assertThat(new.layoutInput.constraints).isEqualTo(Constraints.fixed(6, 5))
        }
    }

    @Test
    fun cacheUpdateInSnapshot_onlyVisibleToParentSnapshotAfterApply() {
        layoutDirection = LayoutDirection.Ltr
        updateNonMeasureInputs()
        updateMeasureInputs()
        val initialLayout = cache.value!!
        val snapshot = Snapshot.takeMutableSnapshot()

        try {
            snapshot.enter {
                layoutDirection = LayoutDirection.Rtl
                updateMeasureInputs()

                val newLayout = cache.value!!
                assertThat(initialLayout.layoutInput.layoutDirection).isEqualTo(LayoutDirection.Ltr)
                assertThat(newLayout.layoutInput.layoutDirection).isEqualTo(LayoutDirection.Rtl)
                assertThat(cache.value!!).isSameInstanceAs(newLayout)
            }

            // Not visible in parent yet.
            assertThat(initialLayout.layoutInput.layoutDirection).isEqualTo(LayoutDirection.Ltr)
            assertThat(cache.value!!).isSameInstanceAs(initialLayout)
            snapshot.apply().check()

            // Now visible in parent.
            val newLayout = cache.value!!
            assertThat(initialLayout.layoutInput.layoutDirection).isEqualTo(LayoutDirection.Ltr)
            assertThat(newLayout.layoutInput.layoutDirection).isEqualTo(LayoutDirection.Rtl)
            assertThat(cache.value!!).isSameInstanceAs(newLayout)
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun cachedValue_recomputed_afterSnapshotWithConflictingInputsApplied() {
        softWrap = false
        layoutDirection = LayoutDirection.Ltr
        updateNonMeasureInputs()
        updateMeasureInputs()
        val snapshot = Snapshot.takeMutableSnapshot()

        try {
            softWrap = true
            updateNonMeasureInputs()
            val initialLayout = cache.value!!

            snapshot.enter {
                layoutDirection = LayoutDirection.Rtl
                updateMeasureInputs()
                with(cache.value!!) {
                    assertThat(layoutInput.softWrap).isEqualTo(false)
                    assertThat(layoutInput.layoutDirection).isEqualTo(LayoutDirection.Rtl)
                    assertThat(cache.value!!).isSameInstanceAs(this)
                }
            }

            // Parent only sees its update.
            with(cache.value!!) {
                assertThat(layoutInput.softWrap).isEqualTo(true)
                assertThat(layoutInput.layoutDirection).isEqualTo(LayoutDirection.Ltr)
                assertThat(this).isSameInstanceAs(initialLayout)
                assertThat(cache.value!!).isSameInstanceAs(this)
            }
            snapshot.apply().check()

            // Cache should now reflect merged inputs.
            with(cache.value!!) {
                assertThat(layoutInput.softWrap).isEqualTo(true)
                assertThat(layoutInput.layoutDirection).isEqualTo(LayoutDirection.Rtl)
                assertThat(cache.value!!).isSameInstanceAs(this)
            }
        } finally {
            snapshot.dispose()
        }
    }

    private fun assertLayoutChange(
        change: () -> Unit,
        compare: (old: TextLayoutResult, new: TextLayoutResult) -> Unit
    ) {
        updateNonMeasureInputs()
        updateMeasureInputs()
        val initialLayout = cache.value

        change()
        val newLayout = cache.value

        assertNotNull(newLayout)
        assertNotNull(initialLayout)
        compare(initialLayout, newLayout)
    }

    private fun assertInvalidationsOnChange(
        expectedInvalidations: Int,
        update: () -> Unit,
    ) {
        updateNonMeasureInputs()
        updateMeasureInputs()
        var invalidations = 0

        observingLayoutCache({ invalidations++ }) {
            update()
        }

        assertThat(invalidations).isEqualTo(expectedInvalidations)
    }

    private fun updateNonMeasureInputs() {
        cache.updateNonMeasureInputs(
            textFieldState = textFieldState,
            codepointTransformation = codepointTransformation,
            textStyle = textStyle,
            singleLine = singleLine,
            softWrap = softWrap
        )
    }

    private fun updateMeasureInputs() {
        cache.layoutWithNewMeasureInputs(
            density = density,
            layoutDirection = layoutDirection,
            fontFamilyResolver = fontFamilyResolver,
            constraints = constraints
        )
    }

    private fun observingLayoutCache(
        onLayoutStateInvalidated: (TextLayoutResult?) -> Unit,
        block: () -> Unit
    ) {
        val globalWriteObserverHandle = Snapshot.registerGlobalWriteObserver {
            Snapshot.sendApplyNotifications()
        }
        val observer = SnapshotStateObserver(onChangedExecutor = { it() })
        observer.start()
        try {
            observer.observeReads(Unit, onValueChangedForScope = {
                onLayoutStateInvalidated(cache.value)
            }) { cache.value }
            block()
        } finally {
            observer.stop()
            globalWriteObserverHandle.dispose()
        }
    }
}
