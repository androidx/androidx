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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.internal.TextFieldLayoutStateCache.MeasureInputs
import androidx.compose.foundation.text.input.internal.TextFieldLayoutStateCache.NonMeasureInputs
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.withCurrent
import androidx.compose.runtime.snapshots.writable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Performs text layout lazily, on-demand for text fields with snapshot-aware caching.
 *
 * You can basically think of this as a `derivedStateOf` that combines all the inputs to text layout
 * — the text itself, configuration parameters, and layout inputs — and spits out a
 * [TextLayoutResult]. The [value] property will register snapshot reads for all the inputs and
 * either return a cached result or re-compute the result and cache it in the current snapshot.
 * The cache is snapshot aware: when a new layout is computed, it will only be cached in the current
 * snapshot. When the snapshot with the new result is applied, its cache will also be visible to the
 * parent snapshot.
 *
 * All the possible inputs to text layout are grouped into two groups: those that come from the
 * layout system ([MeasureInputs]) and those that are passed explicitly to the text field composable
 * ([NonMeasureInputs]). Each of these groups can only be updated in bulk, and each group is stored
 * in an instance of a dedicated class. This means for each type of update, only one state object
 * is needed.
 */
@OptIn(ExperimentalFoundationApi::class, InternalFoundationTextApi::class)
internal class TextFieldLayoutStateCache : State<TextLayoutResult?>, StateObject {
    private var nonMeasureInputs: NonMeasureInputs? by mutableStateOf(
        value = null,
        policy = NonMeasureInputs.mutationPolicy
    )
    private var measureInputs: MeasureInputs? by mutableStateOf(
        value = null,
        policy = MeasureInputs.mutationPolicy
    )

    /**
     * Returns the [TextLayoutResult] for the current text field state and layout inputs, or null
     * if the layout cannot be computed at this time.
     *
     * This method will re-calculate the text layout if the text or any of the other layout inputs
     * have changed, otherwise it will return a cached value.
     *
     * [updateNonMeasureInputs] and [layoutWithNewMeasureInputs] must both be called before this
     * to initialize all the inputs, or it will return null.
     */
    override val value: TextLayoutResult?
        get() {
            // If this is called from the global snapshot, there is technically a race between
            // reading each of our input state objects. That's fine because worst case we'll just
            // re-compute the layout on the next read anyway.
            val nonMeasureInputs = nonMeasureInputs ?: return null
            val measureInputs = measureInputs ?: return null
            return getOrComputeLayout(nonMeasureInputs, measureInputs)
        }

    /**
     * Updates the inputs that aren't from the measure phase.
     *
     * If any of the inputs changed, this method will invalidate any callers of [value]. If the
     * inputs did not change it will not invalidate callers of [value].
     *
     * Note: This will register a snapshot read of [TextFieldState.text] if called from a snapshot
     * observer.
     *
     * @see layoutWithNewMeasureInputs
     */
    fun updateNonMeasureInputs(
        textFieldState: TransformedTextFieldState,
        textStyle: TextStyle,
        singleLine: Boolean,
        softWrap: Boolean,
    ) {
        nonMeasureInputs = NonMeasureInputs(
            textFieldState = textFieldState,
            textStyle = textStyle,
            singleLine = singleLine,
            softWrap = softWrap,
        )
    }

    /**
     * Updates the inputs from the measure phase and returns the most up-to-date [TextLayoutResult].
     *
     * If any of the inputs changed, this method will invalidate any callers of [value], re-compute
     * the text layout, and return the new layout result. If the inputs did not change, it will
     * return a cached value without invalidating callers of [value].
     *
     * @see updateNonMeasureInputs
     */
    fun layoutWithNewMeasureInputs(
        density: Density,
        layoutDirection: LayoutDirection,
        fontFamilyResolver: FontFamily.Resolver,
        constraints: Constraints,
    ): TextLayoutResult {
        val measureInputs = MeasureInputs(
            density = density,
            layoutDirection = layoutDirection,
            fontFamilyResolver = fontFamilyResolver,
            constraints = constraints,
        )
        this.measureInputs = measureInputs
        val nonMeasureInputs = checkNotNull(nonMeasureInputs) {
            "Called layoutWithNewMeasureInputs before updateNonMeasureInputs"
        }
        return getOrComputeLayout(nonMeasureInputs, measureInputs)
    }

    private fun getOrComputeLayout(
        nonMeasureInputs: NonMeasureInputs,
        measureInputs: MeasureInputs
    ): TextLayoutResult {
        val visualText = nonMeasureInputs.textFieldState.visualText

        // Use withCurrent here so the cache itself is never reported as a read state object. It
        // doesn't need to be, because it's always guaranteed to return the same value for the same
        // inputs, so it's good enough to read the input states and those will invalidate the
        // caller when they change.
        record.withCurrent { cachedRecord ->
            val cachedResult = cachedRecord.layoutResult

            if (cachedResult != null &&
                cachedRecord.visualText?.contentEquals(visualText) == true &&
                cachedRecord.singleLine == nonMeasureInputs.singleLine &&
                cachedRecord.softWrap == nonMeasureInputs.softWrap &&
                cachedRecord.layoutDirection == measureInputs.layoutDirection &&
                cachedRecord.densityValue == measureInputs.density.density &&
                cachedRecord.fontScale == measureInputs.density.fontScale &&
                cachedRecord.constraints == measureInputs.constraints &&
                cachedRecord.fontFamilyResolver == measureInputs.fontFamilyResolver
            ) {
                // Fast path: None of the inputs changed.
                if (cachedRecord.textStyle == nonMeasureInputs.textStyle) return cachedResult
                // Slightly slower than fast path: Layout did not change but TextLayoutInput did
                if (cachedRecord.textStyle
                        ?.hasSameDrawAffectingAttributes(nonMeasureInputs.textStyle) == true
                ) {
                    return cachedResult.copy(
                        layoutInput = TextLayoutInput(
                            cachedResult.layoutInput.text,
                            nonMeasureInputs.textStyle,
                            cachedResult.layoutInput.placeholders,
                            cachedResult.layoutInput.maxLines,
                            cachedResult.layoutInput.softWrap,
                            cachedResult.layoutInput.overflow,
                            cachedResult.layoutInput.density,
                            cachedResult.layoutInput.layoutDirection,
                            cachedResult.layoutInput.fontFamilyResolver,
                            cachedResult.layoutInput.constraints
                        )
                    )
                }
            }

            // Slow path: Some input changed, need to re-layout.
            return computeLayout(visualText, nonMeasureInputs, measureInputs, cachedResult)
                .also { newResult ->
                    // TODO(b/294403840) TextDelegate does its own caching and may return the same
                    //  TextLayoutResult object. We should inline that so we don't check twice.
                    if (newResult != cachedResult) {
                        updateCacheIfWritable {
                            this.visualText = visualText
                            this.singleLine = nonMeasureInputs.singleLine
                            this.softWrap = nonMeasureInputs.softWrap
                            this.textStyle = nonMeasureInputs.textStyle
                            this.layoutDirection = measureInputs.layoutDirection
                            this.densityValue = measureInputs.densityValue
                            this.fontScale = measureInputs.fontScale
                            this.constraints = measureInputs.constraints
                            this.fontFamilyResolver = measureInputs.fontFamilyResolver
                            this.layoutResult = newResult
                        }
                    }
                }
        }
    }

    private inline fun updateCacheIfWritable(block: CacheRecord.() -> Unit) {
        val snapshot = Snapshot.current
        // We can't write to the cache when called from a read-only snapshot.
        if (!snapshot.readOnly) {
            record.writable(this, snapshot, block)
        }
    }

    private fun computeLayout(
        visualText: CharSequence,
        nonMeasureInputs: NonMeasureInputs,
        measureInputs: MeasureInputs,
        prevResult: TextLayoutResult?
    ): TextLayoutResult {
        // TODO(b/294403840) Don't use TextDelegate – it is not designed for this use case,
        //  optimized for re-use which we don't take advantage of here, and does its own caching
        //  checks. Maybe we can use MultiParagraphLayoutCache like BasicText?

        // We have to always create a new TextDelegate since it contains internal state that is
        // not snapshot-aware.
        val textDelegate = TextDelegate(
            text = AnnotatedString(visualText.toString()),
            style = nonMeasureInputs.textStyle,
            density = measureInputs.density,
            fontFamilyResolver = measureInputs.fontFamilyResolver,
            softWrap = nonMeasureInputs.softWrap,
            placeholders = emptyList()
        )

        return textDelegate.layout(
            layoutDirection = measureInputs.layoutDirection,
            constraints = measureInputs.constraints,
            prevResult = prevResult
        )
    }

    // region StateObject
    private var record = CacheRecord()
    override val firstStateRecord: StateRecord
        get() = record

    override fun prependStateRecord(value: StateRecord) {
        this.record = value as CacheRecord
    }

    override fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord
    ): StateRecord {
        // This is just a cache, so it's safe to always take the most recent record – worst case
        // we'll just re-compute the layout.
        // However, if we needed to, we could increase the chance of a cache hit by comparing
        // property-by-property and taking the latest version of each property.
        return applied
    }

    /**
     * State record that stores the cached [TextLayoutResult], as well as all the inputs used to
     * generate that result.
     */
    private class CacheRecord : StateRecord() {
        // Inputs. These are slightly different from the values in (Non)MeasuredInputs because they
        // represent the values read from objects in the inputs that are relevant to layout, whereas
        // the Inputs classes contain objects where we don't always care about the entire object.
        // E.g. text layout doesn't care about TextFieldState instances, it only cares about the
        // actual text. If the TFS instance changes but has the same text, we don't need to
        // re-layout. Also if the TFS object _doesn't_ change but its text _does_, we do need to
        // re-layout. That state read happens in getOrComputeLayout to invalidate correctly.
        var visualText: CharSequence? = null
        var textStyle: TextStyle? = null
        var singleLine: Boolean = false
        var softWrap: Boolean = false
        var densityValue: Float = Float.NaN
        var fontScale: Float = Float.NaN
        var layoutDirection: LayoutDirection? = null
        var fontFamilyResolver: FontFamily.Resolver? = null

        /** Not nullable to avoid boxing. */
        var constraints: Constraints = Constraints()

        // Outputs.
        var layoutResult: TextLayoutResult? = null

        override fun create(): StateRecord = CacheRecord()

        override fun assign(value: StateRecord) {
            value as CacheRecord
            visualText = value.visualText
            textStyle = value.textStyle
            singleLine = value.singleLine
            softWrap = value.softWrap
            densityValue = value.densityValue
            fontScale = value.fontScale
            layoutDirection = value.layoutDirection
            fontFamilyResolver = value.fontFamilyResolver
            constraints = value.constraints
            layoutResult = value.layoutResult
        }

        override fun toString(): String = "CacheRecord(" +
            "visualText=$visualText, " +
            "textStyle=$textStyle, " +
            "singleLine=$singleLine, " +
            "softWrap=$softWrap, " +
            "densityValue=$densityValue, " +
            "fontScale=$fontScale, " +
            "layoutDirection=$layoutDirection, " +
            "fontFamilyResolver=$fontFamilyResolver, " +
            "constraints=$constraints, " +
            "layoutResult=$layoutResult" +
            ")"
    }
    // endregion

    // region Input holders
    private class NonMeasureInputs(
        val textFieldState: TransformedTextFieldState,
        val textStyle: TextStyle,
        val singleLine: Boolean,
        val softWrap: Boolean,
    ) {

        override fun toString(): String = "NonMeasureInputs(" +
            "textFieldState=$textFieldState, " +
            "textStyle=$textStyle, " +
            "singleLine=$singleLine, " +
            "softWrap=$softWrap" +
            ")"

        companion object {
            /**
             * Implements equivalence by comparing only the parts of [NonMeasureInputs] that may
             * require re-computing text layout. Notably, it reads the [TextFieldState.text] state
             * property and compares only the text (not selection). This means that when the text
             * state changes it will invalidate any snapshot observer that sets this state.
             */
            val mutationPolicy = object : SnapshotMutationPolicy<NonMeasureInputs?> {
                override fun equivalent(a: NonMeasureInputs?, b: NonMeasureInputs?): Boolean =
                    if (a != null && b != null) {
                        // We don't need to compare text contents here because the text state is read
                        // by getOrComputeLayout – if the text state changes, that method will already
                        // be invalidated. The only reason to compare text here would be to avoid
                        // invalidating if the TextFieldState is a different instance but with the same
                        // text, but that is unlikely to happen.
                        a.textFieldState === b.textFieldState &&
                            a.textStyle == b.textStyle &&
                            a.singleLine == b.singleLine &&
                            a.softWrap == b.softWrap
                    } else {
                        !((a == null) xor (b == null))
                    }
            }
        }
    }

    /**
     * We store both the [Density] object, as well as its component values, because the same density
     * object can report different actual densities over time so we need to be able to see when
     * those values change. We still need the [Density] object to pass to [TextDelegate] though.
     */
    private class MeasureInputs(
        val density: Density,
        val layoutDirection: LayoutDirection,
        val fontFamilyResolver: FontFamily.Resolver,
        val constraints: Constraints,
    ) {
        val densityValue: Float = density.density
        val fontScale: Float = density.fontScale

        override fun toString(): String = "MeasureInputs(" +
            "density=$density, " +
            "densityValue=$densityValue, " +
            "fontScale=$fontScale, " +
            "layoutDirection=$layoutDirection, " +
            "fontFamilyResolver=$fontFamilyResolver, " +
            "constraints=$constraints" +
            ")"

        companion object {
            val mutationPolicy = object : SnapshotMutationPolicy<MeasureInputs?> {
                override fun equivalent(a: MeasureInputs?, b: MeasureInputs?): Boolean =
                    if (a != null && b != null) {
                        // Don't compare density – we don't care if the density instance changed,
                        // only if the actual values used in density calculations did.
                        a.densityValue == b.densityValue &&
                            a.fontScale == b.fontScale &&
                            a.layoutDirection == b.layoutDirection &&
                            a.fontFamilyResolver == b.fontFamilyResolver &&
                            a.constraints == b.constraints
                    } else {
                        !((a == null) xor (b == null))
                    }
            }
        }
    }
    // endregion
}
