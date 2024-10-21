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

package androidx.compose.foundation.text.input.internal

import android.graphics.Typeface
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setSelectionCoerced
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TextFieldLayoutStateCacheTest {

    @get:Rule val rule = createComposeRule()

    private var textFieldState = TextFieldState()
    private var transformedTextFieldState =
        TransformedTextFieldState(
            textFieldState,
            inputTransformation = null,
            codepointTransformation = null
        )
    private var textStyle = TextStyle()
    private var singleLine = false
    private var softWrap = false
    private var keyboardOptions = KeyboardOptions.Default
    private var cache = TextFieldLayoutStateCache()
    private var density = Density(1f, 1f)
    private var layoutDirection = LayoutDirection.Ltr
    private var fontFamilyResolver =
        createFontFamilyResolver(InstrumentationRegistry.getInstrumentation().context)
    private var constraints = Constraints()

    private lateinit var globalWriteObserverHandle: ObserverHandle

    @Before
    fun setUp() {
        globalWriteObserverHandle =
            Snapshot.registerGlobalWriteObserver { Snapshot.sendApplyNotifications() }
    }

    @After
    fun tearDown() {
        globalWriteObserverHandle.dispose()
    }

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
        assertInvalidationsOnChange(1) { textFieldState.edit { placeCursorBeforeCharAt(1) } }
    }

    @Test
    fun updateNonMeasureInputs_invalidatesSnapshot_whenCodepointTransformationChanged() {
        assertInvalidationsOnChange(1) {
            val codepointTransformation = CodepointTransformation { _, codepoint -> codepoint }
            transformedTextFieldState =
                TransformedTextFieldState(
                    textFieldState,
                    inputTransformation = null,
                    codepointTransformation
                )
            updateNonMeasureInputs()
        }
    }

    @FlakyTest(bugId = 300168644)
    @Test
    fun updateNonMeasureInputs_invalidatesSnapshot_whenStyleLayoutAffectingAttrsChanged() {
        textStyle = TextStyle(fontSize = 12.sp)
        assertInvalidationsOnChange(1) {
            textStyle = TextStyle(fontSize = 23.sp)
            updateNonMeasureInputs()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updateNonMeasureInputs_invalidatesSnapshot_whenFontFamilyResolves() {
        val loader = AsyncTestTypefaceLoader()
        val asyncFauxFont = AsyncFauxFont(loader)
        val fontFamily = asyncFauxFont.toFontFamily()

        val context = InstrumentationRegistry.getInstrumentation().context

        textStyle = TextStyle(fontSize = 12.sp, fontFamily = fontFamily)
        runTest(UnconfinedTestDispatcher()) {
            val resolverJob = Job(coroutineContext[Job])
            val resolverContext = coroutineContext + resolverJob
            fontFamilyResolver = createFontFamilyResolver(context, resolverContext)

            assertInvalidationsOnChange(1) {
                Snapshot.withMutableSnapshot {
                    loader.completeOne(asyncFauxFont, Typeface.MONOSPACE)
                }
            }

            resolverJob.cancel()
        }
    }

    @Test
    fun updateNonMeasureInputs_doesInvalidateSnapshot_whenStyleDrawAffectingAttrsChanged() {
        // Measure still does not happen but TextLayoutInput object inside TextLayoutResult
        // should change to reflect the latest inputs that need to be used during the draw phase.
        textStyle = TextStyle(color = Color.Black)
        assertInvalidationsOnChange(1) {
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
    fun updateNonMeasureInputs_invalidatesSnapshot_whenKeyboardTypePhoneChanged() {
        assertInvalidationsOnChange(1) {
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            updateNonMeasureInputs()
        }
    }

    @Test
    fun updateNonMeasureInputs_doesNotInvalidateSnapshot_whenKeyboardTypeNotPhoneChanged() {
        assertInvalidationsOnChange(0) {
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
        density =
            object : Density {
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
        density =
            object : Density {
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
        assertInvalidationsOnChange(1) { TODO("b/294443266: make fonts stale") }
    }

    @Test
    fun updateMeasureInputs_invalidatesSnapshot_whenConstraintsChanged() {
        constraints = Constraints.fixed(5, 5)
        assertInvalidationsOnChange(1) {
            constraints = Constraints.fixed(6, 5)
            updateMeasureInputs()
        }
    }

    /**
     * A scope that reads the layout cache and has a full cache hit – that is, all the inputs match
     * – will never run the CodepointTransformation, and thus never register a "read" of any of the
     * state objects the transformation function happens to read. If those inputs change, all scopes
     * should be invalidated, even the ones that never actually ran the transformation function.
     *
     * The first time we compute layout with a transformation function, we invoke the transformation
     * function. This function is provided externally and may perform zero or more state reads. The
     * first time the layout is computed, those state reads will be seen by whatever snapshot
     * observer is observing the layout call, and when they change, that reader will be invalidated.
     * However, if somewhere else some different code asks for the layout, and none of the inputs
     * have changed, it will return the cached value without ever running the transformation
     * function. This means that when states read by the transformation change, that second reader
     * won't be invalidated since it never observed those reads.
     *
     * To fix this, we manually record reads done by the transformation function and re-read them
     * explicitly when checking for a full cache hit.
     */
    @Ignore("b/306198696")
    @Test
    fun invalidatesAllReaders_whenTransformationDependenciesChanged_producingSameVisualText() {
        var transformationState by mutableStateOf(1)
        var transformationInvocations = 0
        val codepointTransformation = CodepointTransformation { _, codepoint ->
            transformationInvocations++
            @Suppress("UNUSED_EXPRESSION") transformationState
            codepoint + 1
        }
        transformedTextFieldState =
            TransformedTextFieldState(
                textFieldState,
                inputTransformation = null,
                codepointTransformation
            )
        // Transformation isn't applied if there's no text. Keep this at 1 char to make the math
        // simpler.
        textFieldState.setTextAndPlaceCursorAtEnd("h")
        val expectedVisualText = "i"

        fun assertVisualText() {
            assertThat(cache.value?.layoutInput?.text?.text).isEqualTo(expectedVisualText)
        }

        updateNonMeasureInputs()
        updateMeasureInputs()
        var primaryInvalidations = 0
        var secondaryInvalidations = 0

        val primaryObserver = SnapshotStateObserver(onChangedExecutor = { it() })
        val secondaryObserver = SnapshotStateObserver(onChangedExecutor = { it() })
        try {
            primaryObserver.start()
            secondaryObserver.start()

            // This will compute the initial layout.
            primaryObserver.observeReads(
                Unit,
                onValueChangedForScope = {
                    primaryInvalidations++
                    assertVisualText()
                }
            ) {
                assertVisualText()
            }
            assertThat(transformationInvocations).isEqualTo(1)

            // This should be a full cache hit.
            secondaryObserver.observeReads(
                Unit,
                onValueChangedForScope = {
                    secondaryInvalidations++
                    assertVisualText()
                }
            ) {
                assertVisualText()
            }
            assertThat(transformationInvocations).isEqualTo(1)

            // Invalidate the transformation.
            transformationState++
        } finally {
            primaryObserver.stop()
            secondaryObserver.stop()
        }

        assertVisualText()
        assertThat(transformationInvocations).isEqualTo(2)
        assertThat(primaryInvalidations).isEqualTo(1)
        assertThat(secondaryInvalidations).isEqualTo(1)
    }

    @FlakyTest(bugId = 299662404)
    @Test
    fun invalidatesAllReaders_whenTransformationDependenciesChanged_producingNewVisualText() {
        var transformationState by mutableStateOf(1)
        var transformationInvocations = 0
        val codepointTransformation = CodepointTransformation { _, codepoint ->
            transformationInvocations++
            codepoint + transformationState
        }
        transformedTextFieldState =
            TransformedTextFieldState(
                textFieldState,
                inputTransformation = null,
                codepointTransformation
            )
        // Transformation isn't applied if there's no text. Keep this at 1 char to make the math
        // simpler.
        textFieldState.setTextAndPlaceCursorAtEnd("h")
        var expectedVisualText = "i"

        fun assertVisualText() {
            assertThat(cache.value?.layoutInput?.text?.text).isEqualTo(expectedVisualText)
        }

        updateNonMeasureInputs()
        updateMeasureInputs()
        var primaryInvalidations = 0
        var secondaryInvalidations = 0

        val primaryObserver = SnapshotStateObserver(onChangedExecutor = { it() })
        val secondaryObserver = SnapshotStateObserver(onChangedExecutor = { it() })
        try {
            primaryObserver.start()
            secondaryObserver.start()

            // This will compute the initial layout.
            primaryObserver.observeReads(
                Unit,
                onValueChangedForScope = {
                    primaryInvalidations++
                    assertVisualText()
                }
            ) {
                assertVisualText()
            }
            assertThat(transformationInvocations).isEqualTo(1)

            // This should be a full cache hit.
            secondaryObserver.observeReads(
                Unit,
                onValueChangedForScope = {
                    secondaryInvalidations++
                    assertVisualText()
                }
            ) {
                assertVisualText()
            }
            assertThat(transformationInvocations).isEqualTo(1)

            // Invalidate the transformation.
            expectedVisualText = "j"
            transformationState++
        } finally {
            primaryObserver.stop()
            secondaryObserver.stop()
        }

        assertVisualText()
        // Two more reads means two more applications of the transformation.
        assertThat(transformationInvocations).isEqualTo(2)
        assertThat(primaryInvalidations).isEqualTo(1)
        assertThat(secondaryInvalidations).isEqualTo(1)
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
    fun value_returnsNewLayout_whenCompositionChanged() {
        textFieldState.edit {
            replace(0, length, "hello")
            placeCursorBeforeCharAt(0)
        }
        assertLayoutChange(
            change = {
                textFieldState.editAsUser(inputTransformation = null) { setComposition(2, 3) }
            },
        ) { old, new ->
            assertThat(
                    old.multiParagraph.intrinsics.annotatedString.spanStyles.any {
                        it.item.textDecoration == TextDecoration.Underline
                    }
                )
                .isFalse()
            assertThat(
                    new.multiParagraph.intrinsics.annotatedString.spanStyles.any {
                        it.item.textDecoration == TextDecoration.Underline
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun value_returnsCachedLayout_whenCompositionDoesNotChange() {
        textFieldState.editAsUser(inputTransformation = null) {
            replace(0, length, "hello")
            setSelectionCoerced(0)
            setComposition(0, 5)
        }
        updateNonMeasureInputs()
        updateMeasureInputs()
        val initialLayout = cache.value
        // this shouldn't cause a recompute
        val secondLayout = cache.value
        assertThat(initialLayout).isSameInstanceAs(secondLayout)
    }

    @Test
    fun value_returnsNewLayout_whenComposingAnnotationsChanged() {
        textFieldState.editAsUser(inputTransformation = null) {
            replace(0, length, "hello")
            setComposition(
                0,
                5,
                listOf(AnnotatedString.Range(SpanStyle(background = Color.Blue), 0, 5))
            )
        }
        // change composing region but not the annotations.
        assertLayoutChange(
            change = {
                textFieldState.editAsUser(inputTransformation = null) {
                    setComposition(
                        0,
                        5,
                        listOf(AnnotatedString.Range(SpanStyle(background = Color.Red), 0, 5))
                    )
                }
            },
        ) { old, new ->
            assertThat(
                    old.multiParagraph.intrinsics.annotatedString.spanStyles.any {
                        it.item.background == Color.Blue
                    }
                )
                .isTrue()
            assertThat(
                    new.multiParagraph.intrinsics.annotatedString.spanStyles.any {
                        it.item.background == Color.Red
                    }
                )
                .isTrue()
        }
    }

    @Test
    fun value_returnsCachedLayout_whenComposingAnnotationsDoNotChange() {
        textFieldState.editAsUser(inputTransformation = null) {
            replace(0, length, "hello")
            setSelectionCoerced(0)
            setComposition(
                0,
                5,
                listOf(AnnotatedString.Range(SpanStyle(background = Color.Red), 0, 5))
            )
        }
        updateNonMeasureInputs()
        updateMeasureInputs()
        val initialLayout = cache.value
        // this shouldn't cause a recompute
        val secondLayout = cache.value
        assertThat(initialLayout).isSameInstanceAs(secondLayout)
    }

    @Test
    fun value_returnsCachedLayout_whenTextSelectionChanged() {
        textFieldState.edit {
            replace(0, length, "hello")
            placeCursorBeforeCharAt(0)
        }
        assertLayoutChange(change = { textFieldState.edit { placeCursorBeforeCharAt(1) } }) {
            old,
            new ->
            assertThat(new).isSameInstanceAs(old)
        }
    }

    @Test
    fun value_returnsNewLayout_whenCodepointTransformationInstanceChangedWithDifferentOutput() {
        textFieldState.setTextAndPlaceCursorAtEnd("h")
        var codepointTransformation = CodepointTransformation { _, codepoint -> codepoint }
        transformedTextFieldState =
            TransformedTextFieldState(
                textFieldState,
                inputTransformation = null,
                codepointTransformation
            )
        assertLayoutChange(
            change = {
                codepointTransformation = CodepointTransformation { _, codepoint -> codepoint + 1 }
                transformedTextFieldState =
                    TransformedTextFieldState(
                        textFieldState,
                        inputTransformation = null,
                        codepointTransformation
                    )
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
        var codepointTransformation = CodepointTransformation { _, codepoint -> codepoint }
        transformedTextFieldState =
            TransformedTextFieldState(
                textFieldState,
                inputTransformation = null,
                codepointTransformation
            )
        assertLayoutChange(
            change = {
                codepointTransformation = CodepointTransformation { _, codepoint -> codepoint }
                transformedTextFieldState =
                    TransformedTextFieldState(
                        textFieldState,
                        inputTransformation = null,
                        codepointTransformation
                    )
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
            assertThat(old.multiParagraph).isNotSameInstanceAs(new.multiParagraph)
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
            // TextLayoutInput needs to change. We only care whether multiParagraph is reused.
            assertThat(new.multiParagraph).isSameInstanceAs(old.multiParagraph)
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
            assertThat(old.layoutInput.maxLines).isEqualTo(Int.MAX_VALUE)
            assertThat(new.layoutInput.maxLines).isEqualTo(1)
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
        density =
            object : Density {
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
        density =
            object : Density {
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
        assertLayoutChange(change = { TODO("b/294443266: make fonts stale") }) { old, new ->
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

    @Test
    fun textLayoutCalculatedInReadOnlySnapshot_returnedFromCacheWhenCalledFromWriteable() {
        singleLine = true
        updateNonMeasureInputs()
        updateMeasureInputs()
        val initialLayout = cache.value!!

        singleLine = false
        updateNonMeasureInputs()
        val snapshot = Snapshot.takeSnapshot()

        lateinit var layoutFromSnapshot: TextLayoutResult
        snapshot.enter {
            with(cache.value!!) {
                layoutFromSnapshot = this
                assertThat(initialLayout.layoutInput.maxLines).isEqualTo(1)
                assertThat(layoutInput.maxLines).isEqualTo(Int.MAX_VALUE)
            }
        }

        val finalLayout = cache.value!!

        assertThat(initialLayout.multiParagraph)
            .isNotSameInstanceAs(layoutFromSnapshot.multiParagraph)

        // Even though the initial text layout calculation after TextStyle change was done in a
        // read-only snapshot, we still expect to get the same MultiParagraph instance when called
        // with the same measure/non-measure arguments.
        assertThat(finalLayout.multiParagraph).isSameInstanceAs(layoutFromSnapshot.multiParagraph)

        assertThat(finalLayout.layoutInput).isEqualTo(layoutFromSnapshot.layoutInput)
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

        val observer = SnapshotStateObserver(onChangedExecutor = { it() })
        observer.start()
        try {
            observer.observeReads(
                scope = Unit,
                onValueChangedForScope = { invalidations++ },
                block = { cache.value }
            )
            update()
            // Ensure any changes made by block are processed.
            Snapshot.sendApplyNotifications()
        } finally {
            observer.stop()
        }

        Truth.assertWithMessage("Expected $expectedInvalidations invalidations")
            .that(invalidations)
            .isEqualTo(expectedInvalidations)
    }

    private fun updateNonMeasureInputs() {
        cache.updateNonMeasureInputs(
            textFieldState = transformedTextFieldState,
            textStyle = textStyle,
            singleLine = singleLine,
            softWrap = softWrap,
            keyboardOptions = keyboardOptions
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
}
