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

package androidx.compose.ui.text

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString.Annotation
import androidx.compose.ui.util.fastForEach

/**
 * Receiver scope for [DrawAnnotation] callbacks that draws behind or over a character range in
 * laid-out text.
 *
 * Aligns the [DrawScope] coordinate space with the text layout origin and exposes the measured
 * [textLayoutResult] along with the resolved `[start, end)` character offsets for the annotated
 * range. Call [TextLayoutResult.getBoundingBoxesForRange] or [TextLayoutResult.fillPathForRange]
 * with [start] and [end] to obtain per-line bounding boxes or contour paths.
 *
 * @see DrawAnnotation
 * @see TextLayoutResult
 */
public interface DrawAnnotationScope : DrawScope {
    /** Measured [TextLayoutResult] for the text being drawn. */
    public val textLayoutResult: TextLayoutResult

    /** Inclusive start character offset of the annotated range, clamped to the text length. */
    public val start: Int

    /** Exclusive end character offset of the annotated range, clamped to `[start, text.length]`. */
    public val end: Int
}

/**
 * Attaches custom draw-phase rendering behind or over a text range.
 *
 * Runs during the Compose draw phase with access to [DrawAnnotationScope.textLayoutResult] and the
 * resolved `[start, end)` offsets ([DrawAnnotationScope.start] and [DrawAnnotationScope.end]) so
 * callbacks can query per-line bounding boxes via [TextLayoutResult.getBoundingBoxesForRange] or
 * contour paths via [TextLayoutResult.fillPathForRange]. Unlike [SpanStyle.background], updating or
 * animating state read inside [drawBehind] or [drawOver] only invalidates drawing without
 * triggering text measurement or relayout.
 *
 * ### Subclassing and State Guidelines
 * - **Prefer `data class` or `data object` subclasses**: Reusable decorations should subclass
 *   [DrawAnnotation] as a top-level `data class` or `data object` holding only primitive or
 *   immutable graphics parameters so structural [equals] and [hashCode] are generated
 *   automatically. On Android, annotating a `data class` subclass with `@Parcelize` and
 *   implementing `Parcelable` allows [AnnotatedString.Saver] (for example, when saving an
 *   [AnnotatedString] via `rememberSaveable(stateSaver = AnnotatedString.Saver)`) to persist and
 *   restore the annotation across configuration changes and process death. Note that system
 *   clipboard copy/paste converts text through platform `Spanned` representations and does not
 *   transport [DrawAnnotation] instances.
 * - **Instance reuse and caching**: [DrawAnnotation] implementations may be shared across multiple
 *   ranges, shared across multiple `Text` layouts, or split across sub-ranges when
 *   [AnnotatedString.subSequence] slices text. Hold a reusable [Path] instance in a private field
 *   and pass it as `target` to [TextLayoutResult.fillPathForRange] to avoid allocating a [Path] on
 *   each draw pass; [TextLayoutResult] caches the per-range bounding boxes internally.
 * - **Avoid capturing heavy references**: [AnnotatedString] instances are frequently cached in
 *   `ViewModel`s or long-lived state holders. Avoid non-static `inner class` subclasses or
 *   properties that retain `View`, `Context`, or `ViewModel` references.
 * - **Visual-only decoration**: [DrawAnnotation] executes imperative canvas commands during the
 *   draw phase and is opaque to accessibility services, clipboard copy, and HTML export. Represent
 *   semantic meaning alongside [DrawAnnotation] using [SpanStyle], [TtsAnnotation],
 *   [LinkAnnotation], or [StringAnnotation].
 *
 * For one-off inline decorations, use the [DrawAnnotation] factory function.
 *
 * @sample androidx.compose.ui.text.samples.DrawAnnotationSample
 * @sample androidx.compose.ui.text.samples.DrawAnnotationSubclassSample
 * @see AnnotatedString.Builder.withDrawAnnotation
 * @see AnnotatedString.Builder.addDrawAnnotation
 */
@Stable
public abstract class DrawAnnotation : Annotation {
    /**
     * Invoked before the text glyphs are painted. The receiver [DrawAnnotationScope] is aligned
     * with the text layout origin and provides [DrawAnnotationScope.textLayoutResult],
     * [DrawAnnotationScope.start], and [DrawAnnotationScope.end] to query geometry for the
     * annotated range.
     */
    public open fun DrawAnnotationScope.drawBehind() {}

    /**
     * Invoked after the text glyphs are painted. The receiver [DrawAnnotationScope] is aligned with
     * the text layout origin and provides [DrawAnnotationScope.textLayoutResult],
     * [DrawAnnotationScope.start], and [DrawAnnotationScope.end] to query geometry for the
     * annotated range.
     */
    public open fun DrawAnnotationScope.drawOver() {}

    /**
     * Requires subclasses to implement structural equality. Using a `data class` or `data object`
     * is sufficient.
     */
    abstract override fun equals(other: Any?): Boolean

    /**
     * Requires subclasses to implement a structural hash code. Using a `data class` or `data
     * object` is sufficient.
     */
    abstract override fun hashCode(): Int
}

/**
 * Creates an ad-hoc [DrawAnnotation] from [drawBehind] and [drawOver] lambdas.
 *
 * Note: Lambda-backed [DrawAnnotation] instances capture closures and are not persisted by
 * [AnnotatedString.Saver]. To define a reusable and saveable decoration, subclass [DrawAnnotation]
 * instead.
 *
 * @param drawBehind optional callback run before painting the text glyphs
 * @param drawOver optional callback run after painting the text glyphs
 * @see DrawAnnotation
 */
public fun DrawAnnotation(
    drawBehind: (DrawAnnotationScope.() -> Unit)? = null,
    drawOver: (DrawAnnotationScope.() -> Unit)? = null,
): DrawAnnotation = CombinedDrawAnnotation(drawBehind, drawOver)

internal class CombinedDrawAnnotation(
    val drawBehindLambda: (DrawAnnotationScope.() -> Unit)?,
    val drawOverLambda: (DrawAnnotationScope.() -> Unit)?,
) : DrawAnnotation() {
    override fun DrawAnnotationScope.drawBehind() {
        drawBehindLambda?.invoke(this)
    }

    override fun DrawAnnotationScope.drawOver() {
        drawOverLambda?.invoke(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CombinedDrawAnnotation) return false
        if (drawBehindLambda !== other.drawBehindLambda) return false
        if (drawOverLambda !== other.drawOverLambda) return false
        return true
    }

    override fun hashCode(): Int {
        var result = drawBehindLambda?.hashCode() ?: 0
        result = 31 * result + (drawOverLambda?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "DrawAnnotation(drawBehind=$drawBehindLambda, drawOver=$drawOverLambda)"
    }
}

/**
 * Reusable [DrawAnnotationScope] implementation that delegates [DrawScope] operations to
 * [drawScope] while allowing [textLayoutResult], [start], and [end] to be updated across draw
 * passes and annotations without allocating.
 */
internal class DrawAnnotationScopeImpl(
    val drawScope: DrawScope,
    override var textLayoutResult: TextLayoutResult,
    override var start: Int = 0,
    override var end: Int = 0,
) : DrawAnnotationScope, DrawScope by drawScope

/**
 * Invokes [DrawAnnotation.drawBehind] for all [DrawAnnotation] ranges in [text] whose clamped range
 * `[start, end)` is non-empty.
 */
internal fun DrawScope.drawAnnotationsBehind(
    text: AnnotatedString,
    textLayoutResult: TextLayoutResult,
    scope: DrawAnnotationScopeImpl? = null,
) {
    drawDrawAnnotations(this, text, textLayoutResult, scope, isBehind = true)
}

/**
 * Invokes [DrawAnnotation.drawOver] for all [DrawAnnotation] ranges in [text] whose clamped range
 * `[start, end)` is non-empty.
 */
internal fun DrawScope.drawAnnotationsOver(
    text: AnnotatedString,
    textLayoutResult: TextLayoutResult,
    scope: DrawAnnotationScopeImpl? = null,
) {
    drawDrawAnnotations(this, text, textLayoutResult, scope, isBehind = false)
}

private fun drawDrawAnnotations(
    drawScope: DrawScope,
    text: AnnotatedString,
    textLayoutResult: TextLayoutResult,
    scope: DrawAnnotationScopeImpl?,
    isBehind: Boolean,
) {
    if (!text.hasDrawAnnotations(0, text.length)) return
    val annotations = text.annotations ?: return
    val textLength = text.length
    val activeScope =
        if (scope != null) {
            scope.textLayoutResult = textLayoutResult
            scope
        } else {
            DrawAnnotationScopeImpl(drawScope, textLayoutResult)
        }
    annotations.fastForEach { range ->
        val annotation = range.item as? DrawAnnotation ?: return@fastForEach
        val startOffset = range.start.coerceIn(0, textLength)
        val endOffset = range.end.coerceIn(startOffset, textLength)
        if (startOffset < endOffset) {
            activeScope.start = startOffset
            activeScope.end = endOffset
            with(annotation) {
                if (isBehind) {
                    activeScope.drawBehind()
                } else {
                    activeScope.drawOver()
                }
            }
        }
    }
}
