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

package androidx.compose.remote.document

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo

/**
 * Bitmap glyph entry for [RemoteDocumentWriter.addBitmapFont].
 *
 * Each glyph maps a character sequence [chars] to an individual bitmap resource rather than a
 * shared texture atlas.
 *
 * @property chars character or ligature sequence matched by this glyph
 * @property bitmapId bitmap resource ID for this glyph, or `-1` for a blank space glyph
 * @property marginLeft left padding around the glyph bitmap in pixels
 * @property marginTop top padding above the glyph bitmap in pixels
 * @property marginRight right padding around the glyph bitmap in pixels
 * @property marginBottom bottom padding below the glyph bitmap in pixels
 * @property width glyph draw width in pixels (must be positive for non-space glyphs; `0` for blank
 *   space glyphs)
 * @property height glyph draw height in pixels (must be positive for non-space glyphs; `0` for
 *   blank space glyphs)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteBitmapGlyph(
    public val chars: String,
    public val bitmapId: Int,
    @Suppress("NoByteOrShort") public val marginLeft: Short,
    @Suppress("NoByteOrShort") public val marginTop: Short,
    @Suppress("NoByteOrShort") public val marginRight: Short,
    @Suppress("NoByteOrShort") public val marginBottom: Short,
    @Suppress("NoByteOrShort") public val width: Short = 0,
    @Suppress("NoByteOrShort") public val height: Short = 0,
)

/**
 * Typed key-value property passed to [RemoteDocumentWriter.startCustom].
 *
 * Construct instances with the companion [invoke] factories:
 * ```kotlin
 * val count = RemoteCustomProperty(id = 1, type = RemoteCustomProperty.INT_PROP, value = 42)
 * val progress =
 *     RemoteCustomProperty(id = 2, type = RemoteCustomProperty.FLOAT_PROP, value = 0.5f.mvf)
 * ```
 *
 * @property id component-defined property identifier
 * @property type wire property type tag (such as [INT_PROP], [FLOAT_PROP], or [FLOAT_RETURN])
 * @property intValue integer payload for integer, color, or variable-ID property types
 * @property floatValue float or NaN-variable payload for [FLOAT_PROP] and [FLOAT_RETURN] types
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCustomProperty
private constructor(
    @Suppress("NoByteOrShort") public val id: Short,
    @Suppress("NoByteOrShort") public val type: Short,
    public val intValue: Int,
    public val floatValue: MaybeVariableFloat,
) {
    public companion object {
        /** Literal integer property; pass the value via the `Int` [invoke] overload. */
        @Suppress("NoByteOrShort")
        public const val INT_PROP: Short = CustomPropertyType.INT_PROP.toShort()

        /** Float or NaN-variable property; pass the value via the [MaybeVariableFloat] overload. */
        @Suppress("NoByteOrShort")
        public const val FLOAT_PROP: Short = CustomPropertyType.FLOAT_PROP.toShort()

        /** String variable ID property; pass the text ID via the `Int` [invoke] overload. */
        @Suppress("NoByteOrShort")
        public const val STRING_PROP: Short = CustomPropertyType.STRING_PROP.toShort()

        /**
         * Float output variable slot; pass the target variable as a NaN-encoded
         * [MaybeVariableFloat] via the [MaybeVariableFloat] [invoke] overload.
         */
        @Suppress("NoByteOrShort")
        public const val FLOAT_RETURN: Short = CustomPropertyType.FLOAT_RETURN.toShort()

        /** String output variable ID; pass the target ID via the `Int` [invoke] overload. */
        @Suppress("NoByteOrShort")
        public const val TEXT_RETURN: Short = CustomPropertyType.TEXT_RETURN.toShort()

        /** Integer output variable ID; pass the target ID via the `Int` [invoke] overload. */
        @Suppress("NoByteOrShort")
        public const val INT_RETURN: Short = CustomPropertyType.INT_RETURN.toShort()

        /** Color output variable ID; pass the target ID via the `Int` [invoke] overload. */
        @Suppress("NoByteOrShort")
        public const val COLOR_RETURN: Short = CustomPropertyType.COLOR_RETURN.toShort()

        /** Color variable ID property; pass the color ID via the `Int` [invoke] overload. */
        @Suppress("NoByteOrShort")
        public const val COLOR_ID_PROP: Short = CustomPropertyType.COLOR_ID_PROP.toShort()

        /**
         * Literal ARGB color property; pass the packed ARGB int via the `Int` [invoke] overload.
         */
        @Suppress("NoByteOrShort")
        public const val COLOR_PROP: Short = CustomPropertyType.COLOR_PROP.toShort()

        /** Integer variable ID property; pass the variable ID via the `Int` [invoke] overload. */
        @Suppress("NoByteOrShort")
        public const val INT_ID_PROP: Short = CustomPropertyType.INT_ID_PROP.toShort()

        /**
         * Creates an integer-backed [RemoteCustomProperty].
         *
         * Use this overload for literal integers, packed ARGB colors, and variable IDs.
         *
         * @param id component-defined property identifier
         * @param type wire property type tag
         * @param value literal integer, ARGB color, or variable ID payload
         * @return property descriptor with [intValue] set to [value]
         */
        public operator fun invoke(
            @Suppress("NoByteOrShort") id: Short,
            @Suppress("NoByteOrShort") type: Short,
            value: Int,
        ): RemoteCustomProperty = RemoteCustomProperty(id, type, value, 0f.mvf)

        /**
         * Creates a float-backed [RemoteCustomProperty].
         *
         * Use this overload for float values and NaN-encoded float variable references.
         *
         * @param id component-defined property identifier
         * @param type wire property type tag
         * @param value literal float or NaN-boxed variable reference
         * @return property descriptor with [floatValue] set to [value]
         */
        public operator fun invoke(
            @Suppress("NoByteOrShort") id: Short,
            @Suppress("NoByteOrShort") type: Short,
            value: MaybeVariableFloat,
        ): RemoteCustomProperty = RemoteCustomProperty(id, type, 0, value)
    }
}

/**
 * Records delta paint changes inside [RemoteDocumentWriter.writePaint].
 *
 * Only properties modified inside the lambda are encoded into the emitted paint change operation;
 * unspecified attributes remain unchanged on the player's active paint state.
 *
 * ```kotlin
 * writer.writePaint { paint ->
 *     paint.setColor(0xFF0000FF.toInt())
 *     paint.setStrokeWidth(4f.mvf)
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemotePaintWriter {
    /**
     * Sets the active paint color to a literal ARGB value.
     *
     * @param color packed 32-bit ARGB color
     */
    public fun setColor(@ColorInt color: Int)

    /**
     * Sets the active paint opacity multiplier.
     *
     * @param alpha opacity in `[0, 1]`, or a NaN-boxed float variable reference
     */
    public fun setAlpha(alpha: MaybeVariableFloat)

    /**
     * Sets the active paint color from a dynamic color resource.
     *
     * @param colorId handle to a color constant, themed color, or color expression
     */
    public fun setColorId(colorId: RemoteColorId)

    /**
     * Sets the stroke width in pixels for stroked drawing operations.
     *
     * Pass `0f` for hairline mode, which draws a single pixel regardless of canvas scale.
     *
     * @param width stroke width in pixels, or a NaN-boxed float variable reference
     */
    public fun setStrokeWidth(width: MaybeVariableFloat)

    /**
     * Sets the cap shape applied to the ends of stroked paths and lines.
     *
     * @param cap stroke cap ordinal (`0` = butt, `1` = round, `2` = square)
     */
    public fun setStrokeCap(cap: Int)

    /**
     * Sets the join shape applied where stroked path segments meet.
     *
     * @param join stroke join ordinal (`0` = miter, `1` = round, `2` = bevel)
     */
    public fun setStrokeJoin(join: Int)

    /**
     * Sets the miter limit for sharp stroke joins.
     *
     * @param miter non-negative miter limit ratio, or a NaN-boxed float variable reference
     */
    public fun setStrokeMiter(miter: MaybeVariableFloat)

    /**
     * Sets whether shapes are filled, stroked, or both.
     *
     * @param style paint style constant (`0` = fill, `1` = stroke, `2` = fill and stroke)
     */
    public fun setStyle(style: Int)

    /**
     * Enables or disables edge anti-aliasing.
     *
     * @param antiAlias `true` to smooth drawn edges, `false` for aliased rendering
     */
    public fun setAntiAlias(antiAlias: Boolean)

    /**
     * Enables or disables bilinear filtering when sampling scaled bitmaps.
     *
     * @param filter `true` to interpolate sampled texels, `false` for nearest-neighbor sampling
     */
    public fun setFilterBitmap(filter: Boolean)

    /**
     * Sets the bitmap and image sampling filter quality.
     *
     * @param quality filter quality level (`0` = none, `1` = low, `2` = medium, `3` = high,
     *   matching `RemoteFilterQuality`)
     */
    public fun setImageFilterQuality(quality: Int)

    /**
     * Sets the compositing blend mode for subsequent drawing commands.
     *
     * @param blendMode wire blend mode constant matching `PaintBundle.BLEND_MODE_*`
     */
    public fun setBlendMode(blendMode: Int)

    /**
     * Sets the font size for text drawing commands.
     *
     * @param size text size in pixels, or a NaN-boxed float variable reference
     */
    public fun setTextSize(size: MaybeVariableFloat)

    /**
     * Sets the typeface family or embedded font, weight, and italic style.
     *
     * @param typefaceId built-in family (`0` = default, `1` = sans-serif, `2` = serif, `3` =
     *   monospace), custom font family string ID, or embedded `FontDataNode` ID when [isFontData]
     *   is `true`
     * @param weight font weight in the `100..1000` range
     * @param isItalic `true` for italic style, `false` for upright
     * @param isFontData `true` if [typefaceId] references an embedded `FontDataNode`
     */
    public fun setTextStyle(
        typefaceId: Int,
        weight: Int,
        isItalic: Boolean,
        isFontData: Boolean = false,
    )

    /**
     * Sets the fallback typeface family, font weight, and italic style.
     *
     * @param typefaceId built-in family (`0` = default, `1` = sans-serif, `2` = serif, `3` =
     *   monospace)
     * @param weight font weight in the `100..1000` range
     * @param isItalic `true` for italic style, `false` for upright
     */
    public fun setFallbackTypeFace(typefaceId: Int, weight: Int, isItalic: Boolean)

    /**
     * Sets OpenType variable font variation axes.
     *
     * [axisTagIds] and [axisValues] must have equal lengths (at most 8 axes on the wire format).
     *
     * @param axisTagIds string variable IDs for 4-character axis tags (such as `"wght"`)
     * @param axisValues axis values matching [axisTagIds], each literal or a float variable
     */
    public fun setTextAxis(axisTagIds: IntArray, axisValues: List<MaybeVariableFloat>)

    /**
     * Installs a wire-encoded path effect, or clears the active path effect.
     *
     * @param pathEffect encoded path effect (such as from `RemotePathEffect.encode`), or `null` to
     *   clear
     */
    public fun setPathEffect(pathEffect: FloatArray?)

    /**
     * Applies a literal tint color filter with the given blend mode.
     *
     * @param color packed 32-bit ARGB source color
     * @param blendMode wire blend mode constant matching `PaintBundle.BLEND_MODE_*`
     */
    public fun setColorFilter(@ColorInt color: Int, blendMode: Int)

    /**
     * Applies a dynamic color filter using a color variable and blend mode.
     *
     * @param colorId handle to the dynamic source color
     * @param blendMode wire blend mode constant matching `PaintBundle.BLEND_MODE_*`
     */
    public fun setColorFilterId(colorId: RemoteColorId, blendMode: Int)

    /** Clears the active color filter on the paint. */
    public fun clearColorFilter()

    /**
     * Installs or clears a custom shader on the paint.
     *
     * @param shaderId shader ID returned by [RemoteDocumentWriter.addShader], or `0` to clear
     */
    public fun setShader(shaderId: Int)

    /**
     * Applies a transformation matrix to the active shader.
     *
     * @param matrixId NaN-boxed matrix variable reference, or `0f` to clear the shader matrix
     */
    public fun setShaderMatrix(matrixId: MaybeVariableFloat)

    /**
     * Sets a linear gradient shader between two endpoints.
     *
     * Each entry `colors[i]` (for `i in 0..15`) is treated as a color variable ID when bit `i` of
     * [colorIdMask] is set, and as a literal ARGB int otherwise.
     *
     * @param colors ARGB colors or color variable IDs along the gradient (at most 255 entries)
     * @param colorIdMask 16-bit bitmask (`0x0000..0xFFFF`) whose bit `i` marks `colors[i]` (`i in
     *   0..15`) as a color variable ID
     * @param stops normalized stop positions in `[0, 1]` matching [colors], or `null` for even
     *   spacing
     * @param startX starting point X coordinate in pixels
     * @param startY starting point Y coordinate in pixels
     * @param endX ending point X coordinate in pixels
     * @param endY ending point Y coordinate in pixels
     * @param tileMode edge tiling mode (`0` = clamp, `1` = repeat, `2` = mirror)
     */
    public fun setLinearGradient(
        colors: IntArray,
        colorIdMask: Int,
        stops: List<MaybeVariableFloat>?,
        startX: MaybeVariableFloat,
        startY: MaybeVariableFloat,
        endX: MaybeVariableFloat,
        endY: MaybeVariableFloat,
        tileMode: Int,
    )

    /**
     * Sets a radial gradient shader centered at ([centerX], [centerY]).
     *
     * Each entry `colors[i]` (for `i in 0..15`) is treated as a color variable ID when bit `i` of
     * [colorIdMask] is set, and as a literal ARGB int otherwise.
     *
     * @param colors ARGB colors or color variable IDs from center to edge (at most 255 entries)
     * @param colorIdMask 16-bit bitmask (`0x0000..0xFFFF`) whose bit `i` marks `colors[i]` (`i in
     *   0..15`) as a color variable ID
     * @param stops normalized stop positions in `[0, 1]` matching [colors], or `null` for even
     *   spacing
     * @param centerX circle center X coordinate in pixels
     * @param centerY circle center Y coordinate in pixels
     * @param radius positive circle radius in pixels
     * @param tileMode edge tiling mode (`0` = clamp, `1` = repeat, `2` = mirror)
     */
    public fun setRadialGradient(
        colors: IntArray,
        colorIdMask: Int,
        stops: List<MaybeVariableFloat>?,
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
        radius: MaybeVariableFloat,
        tileMode: Int,
    )

    /**
     * Sets a two-point conical radial gradient shader between two circles.
     *
     * Encodes a two-point conical radial gradient (`PaintBundle.RADIAL_GRADIENT2`). Each entry
     * `colors[i]` (for `i in 0..15`) is treated as a color variable ID when bit `i` of
     * [colorIdMask] is set, and as a literal ARGB int otherwise.
     *
     * @param colors ARGB colors or color variable IDs along the cone (at most 255 entries)
     * @param colorIdMask 16-bit bitmask (`0x0000..0xFFFF`) whose bit `i` marks `colors[i]` (`i in
     *   0..15`) as a color variable ID
     * @param stops normalized stop positions in `[0, 1]` matching [colors], or `null` for even
     *   spacing
     * @param startX focal circle center X coordinate in pixels
     * @param startY focal circle center Y coordinate in pixels
     * @param startRadius non-negative focal circle radius in pixels
     * @param endX end circle center X coordinate in pixels
     * @param endY end circle center Y coordinate in pixels
     * @param endRadius positive end circle radius in pixels
     * @param tileMode edge tiling mode (`0` = clamp, `1` = repeat, `2` = mirror)
     */
    public fun setTwoPointConicalGradient(
        colors: IntArray,
        colorIdMask: Int,
        stops: List<MaybeVariableFloat>?,
        startX: MaybeVariableFloat,
        startY: MaybeVariableFloat,
        startRadius: MaybeVariableFloat,
        endX: MaybeVariableFloat,
        endY: MaybeVariableFloat,
        endRadius: MaybeVariableFloat,
        tileMode: Int,
    )

    /**
     * Sets a sweep (angular) gradient shader around ([centerX], [centerY]).
     *
     * Each entry `colors[i]` (for `i in 0..15`) is treated as a color variable ID when bit `i` of
     * [colorIdMask] is set, and as a literal ARGB int otherwise.
     *
     * @param colors ARGB colors or color variable IDs sweeping clockwise from 3 o'clock (at most
     *   255 entries)
     * @param colorIdMask 16-bit bitmask (`0x0000..0xFFFF`) whose bit `i` marks `colors[i]` (`i in
     *   0..15`) as a color variable ID
     * @param stops normalized stop positions in `[0, 1]` matching [colors], or `null` for even
     *   spacing
     * @param centerX sweep center X coordinate in pixels
     * @param centerY sweep center Y coordinate in pixels
     */
    public fun setSweepGradient(
        colors: IntArray,
        colorIdMask: Int,
        stops: List<MaybeVariableFloat>?,
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
    )

    /**
     * Sets a tiled bitmap texture shader on the paint.
     *
     * @param bitmapId handle to the source bitmap
     * @param tileModeX horizontal tiling mode (`0` = clamp, `1` = repeat, `2` = mirror, `3` =
     *   decal)
     * @param tileModeY vertical tiling mode (`0` = clamp, `1` = repeat, `2` = mirror, `3` = decal)
     * @param filterMode sampling filter mode (`0` = default, `1` = nearest, `2` = linear)
     * @param maxAnisotropy maximum anisotropic filtering level (`0` disables anisotropy; values `>
     *   0` enable anisotropic filtering)
     */
    public fun setTextureShader(
        bitmapId: RemoteBitmapId,
        tileModeX: Int,
        tileModeY: Int,
        filterMode: Int,
        maxAnisotropy: Int,
    )
}

/**
 * Writes RemoteCompose documents using typed resource handles and [MaybeVariableFloat] values.
 *
 * Resource and expression methods allocate typed handles ([RemoteFloatId], [RemoteStringId],
 * [RemoteBitmapId], and peers). Variable-capable numeric parameters accept [MaybeVariableFloat] so
 * literal floats and dynamic variables stay type-safe. Drawing and layout operations emit
 * sequentially; delimit containers with paired `start*`/`end*` calls (or scoped helpers such as
 * [root] and [loop]), record paint deltas with [writePaint], and serialize with [toByteArray].
 *
 * ```kotlin
 * writer.root {
 *     writer.writePaint { it.setColor(0xFFFF0000.toInt()) }
 *     writer.drawCircle(centerX = 50f.mvf, centerY = 50f.mvf, radius = 25f.mvf)
 * }
 * val bytes = writer.toByteArray()
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteDocumentWriter {

    // ---- 1. Lifecycle, State & Global Hoisting ----

    /** Total bytes written so far (or operation count for AST-backed writers). */
    public val bytesWritten: Int

    /** Most recently emitted layout or component identifier, or `0` before any component starts. */
    public val lastComponentId: Int

    /** Optional writer-specific callback hook attached by the host, or `null` if unused. */
    public val writerCallback: Any?

    /**
     * Allocates the next unique non-system document resource or variable identifier.
     *
     * @return freshly allocated ID starting at [NanEncoding.START_ID]
     */
    public fun allocateId(): Int

    /**
     * Allocates a fresh float variable slot without emitting an initial value operation.
     *
     * @return typed handle to the reserved float variable
     */
    public fun allocateFloatId(): RemoteFloatId

    /**
     * Opens a global hoisting scope so subsequent declarations emit at document root level.
     *
     * Pair every call with a matching [endGlobal]. Do not nest calls unless the underlying
     * implementation explicitly supports re-entrant global scopes, as `RemoteComposeWriter`-backed
     * implementations throw a [RuntimeException] when a global section is already active.
     */
    public fun beginGlobal()

    /**
     * Closes the global hoisting scope opened by [beginGlobal].
     *
     * @throws RuntimeException if called without a matching [beginGlobal]
     */
    public fun endGlobal()

    /**
     * Checks whether the next paint update must emit full state and clears the flag.
     *
     * Used across container boundaries where the player resets or isolates active paint state.
     *
     * @return `true` if the next [writePaint] must re-emit paint attributes
     */
    public fun checkAndClearForceSendingNewPaint(): Boolean

    /**
     * Serializes the document into a wire-format [ByteArray].
     *
     * Applies the compression mode configured on this writer (such as a `Header.COMPRESS` tag).
     *
     * @return encoded RemoteCompose document bytes
     */
    public fun toByteArray(): ByteArray

    // ---- 2. Component Values ----

    /**
     * Allocates and binds a float variable to a metric of the current component.
     *
     * @param type component metric selector (`0` = width, `1` = height, `2` = posX, `3` = posY, `4`
     *   = rootX, `5` = rootY, `6` = contentWidth, `7` = contentHeight)
     * @return handle to the float variable populated during layout
     */
    public fun addComponentValue(type: Int): RemoteFloatId

    /**
     * Binds an existing float variable to a component's layout metric.
     *
     * @param valueId target float variable to receive the metric
     * @param type component metric selector (`0..7`, matching [addComponentValue])
     * @param componentId target component ID, or `-1` to bind to the most recent component
     */
    public fun bindComponentValue(valueId: RemoteFloatId, type: Int, componentId: Int = -1)

    // ---- 3. Variables, Constants & Expressions ----

    /**
     * Writes an immutable float constant, deduplicating equal bit patterns when supported.
     *
     * @param value constant float value
     * @return handle to the float constant
     */
    public fun addFloatConstant(value: Float): RemoteFloatId

    /**
     * Allocates a fresh mutable float variable initialized to [initialValue].
     *
     * Unlike [addFloatConstant], each call reserves a distinct variable ID safe to mutate via
     * actions such as [addValueFloatChangeAction].
     *
     * @param initialValue initial float value
     * @return handle to the mutable float variable
     */
    public fun addMutableFloat(initialValue: Float): RemoteFloatId

    /**
     * Declares a host-addressable float variable with an initial value.
     *
     * @param name unique host-visible variable name
     * @param initialValue initial float value before host overrides
     * @return handle to the named float variable
     */
    public fun addNamedFloat(name: String, initialValue: Float): RemoteFloatId

    /**
     * Writes a Reverse Polish Notation (RPN) float expression.
     *
     * Tokens combine literal floats, NaN-boxed variable IDs ([RemoteFloatId.toNanFloat]), and
     * NaN-boxed float operator tokens.
     *
     * @param values RPN float expression tokens
     * @return handle to the evaluated float expression
     */
    public fun addFloatExpression(vararg values: Float): RemoteFloatId

    /**
     * Writes an animated RPN float expression driven by an optional animation spec.
     *
     * @param expression RPN float expression tokens computing the target value
     * @param animation packed duration easing or spring animation spec, or `null` for no animation
     * @return handle to the animated float variable
     */
    public fun addAnimatedFloat(expression: FloatArray, animation: FloatArray?): RemoteFloatId

    /**
     * Writes a 32-bit integer constant, deduplicating equal values when supported.
     *
     * @param value constant 32-bit integer value
     * @return handle to the integer constant
     */
    public fun addIntConstant(value: Int): RemoteIntId

    /**
     * Allocates a fresh mutable 32-bit integer variable initialized to [initialValue].
     *
     * Unlike [addIntConstant], each call reserves a distinct variable ID safe to mutate via
     * [addValueIntegerChangeAction].
     *
     * @param initialValue initial 32-bit integer value
     * @return handle to the mutable integer variable
     */
    public fun addMutableInt(initialValue: Int): RemoteIntId

    /**
     * Declares a host-addressable 32-bit integer variable with an initial value.
     *
     * @param name unique host-visible variable name
     * @param initialValue initial integer value before host overrides
     * @return handle to the named integer variable
     */
    public fun addNamedInt(name: String, initialValue: Int): RemoteIntId

    /**
     * Writes a 32-bit RPN integer expression.
     *
     * Tokens combine literal `Long` integers, `Long`-encoded integer variable IDs
     * ([RemoteIntId.toLongEncodedId]), and `Long`-encoded integer operators.
     *
     * @param values RPN integer expression tokens
     * @return handle to the evaluated integer expression
     */
    public fun addIntegerExpression(vararg values: Long): RemoteIntId

    /**
     * Writes a 64-bit long constant, deduplicating equal values when supported.
     *
     * @param value constant 64-bit integer value (such as an epoch timestamp in milliseconds)
     * @return handle to the long constant
     */
    public fun addLongConstant(value: Long): RemoteLongId

    /**
     * Allocates a fresh mutable 64-bit long variable initialized to [initialValue].
     *
     * @param initialValue initial 64-bit integer value
     * @return handle to the mutable long variable
     */
    public fun addMutableLong(initialValue: Long): RemoteLongId

    /**
     * Declares a host-addressable 64-bit long variable with an initial value.
     *
     * @param name unique host-visible variable name
     * @param initialValue initial long value before host overrides
     * @return handle to the named long variable
     */
    public fun addNamedLong(name: String, initialValue: Long): RemoteLongId

    /**
     * Associates a host-visible name and variable type tag with an existing variable ID.
     *
     * @param id existing variable or resource identifier
     * @param name host-visible variable name
     * @param type wire variable type tag (`0` = string, `1` = float, `2` = color, `3` = image, `4`
     *   = int, `5` = long, `6` = float array / path)
     */
    public fun setNamedVariable(id: Int, name: String, type: Int)

    // ---- 4. Colors ----

    /**
     * Writes an immutable ARGB color constant.
     *
     * @param color packed 32-bit ARGB color
     * @return handle to the color constant
     */
    public fun addColorConstant(@ColorInt color: Int): RemoteColorId

    /**
     * Declares a host-addressable ARGB color variable with an initial fallback value.
     *
     * @param name unique host-visible color name
     * @param initialColor initial 32-bit ARGB color before host overrides
     * @return handle to the named color variable
     */
    public fun addNamedColor(name: String, @ColorInt initialColor: Int): RemoteColorId

    /**
     * Declares a light/dark theme-aware color with explicit fallback ARGB values.
     *
     * @param groupName theme group identifier
     * @param lightId theme token ID for light mode
     * @param darkId theme token ID for dark mode
     * @param lightFallback packed ARGB fallback used in light mode if [lightId] is unresolved
     * @param darkFallback packed ARGB fallback used in dark mode if [darkId] is unresolved
     * @return handle to the themed color
     */
    public fun addThemedColor(
        groupName: String,
        @Suppress("NoByteOrShort") lightId: Short,
        @Suppress("NoByteOrShort") darkId: Short,
        @ColorInt lightFallback: Int,
        @ColorInt darkFallback: Int,
    ): RemoteColorId

    /**
     * Writes a dynamic color computed from normalized ARGB channels.
     *
     * Channel values are expected in `[0, 1]` and converted via `(channel * 255 + 0.5).toInt()`.
     *
     * @param alpha normalized alpha channel in `[0, 1]`
     * @param red normalized red channel in `[0, 1]`
     * @param green normalized green channel in `[0, 1]`
     * @param blue normalized blue channel in `[0, 1]`
     * @return handle to the ARGB color expression
     */
    public fun addColorExpressionArgb(
        alpha: MaybeVariableFloat,
        red: MaybeVariableFloat,
        green: MaybeVariableFloat,
        blue: MaybeVariableFloat,
    ): RemoteColorId

    /**
     * Writes a dynamic color computed from an 8-bit alpha and normalized HSV channels.
     *
     * @param alpha 8-bit alpha channel in `0..255`
     * @param hue normalized hue in `[0, 1)`
     * @param saturation normalized saturation in `[0, 1]`
     * @param value normalized brightness value in `[0, 1]`
     * @return handle to the HSV color expression
     */
    public fun addColorExpressionHsv(
        alpha: Int,
        hue: MaybeVariableFloat,
        saturation: MaybeVariableFloat,
        value: MaybeVariableFloat,
    ): RemoteColorId

    /**
     * Writes a color expression interpolating between two literal ARGB colors.
     *
     * @param color1 starting ARGB color at `tween = 0f`
     * @param color2 ending ARGB color at `tween = 1f`
     * @param tween interpolation fraction in `[0, 1]`, or a float variable
     * @return handle to the interpolated color expression
     */
    public fun addColorTween(
        @ColorInt color1: Int,
        @ColorInt color2: Int,
        tween: MaybeVariableFloat,
    ): RemoteColorId

    /**
     * Writes a color expression interpolating between two dynamic color variables.
     *
     * @param colorId1 starting color handle at `tween = 0f`
     * @param colorId2 ending color handle at `tween = 1f`
     * @param tween interpolation fraction in `[0, 1]`, or a float variable
     * @return handle to the interpolated color expression
     */
    public fun addColorIdTween(
        colorId1: RemoteColorId,
        colorId2: RemoteColorId,
        tween: MaybeVariableFloat,
    ): RemoteColorId

    /**
     * Extracts a scalar color channel from [baseColorId] as a float variable.
     *
     * @param baseColorId source color handle
     * @param attributeType channel selector (`0` = hue, `1` = saturation, `2` = brightness, `3` =
     *   red, `4` = green, `5` = blue, `6` = alpha)
     * @return handle to the extracted normalized float attribute
     */
    public fun getColorAttribute(baseColorId: RemoteColorId, attributeType: Int): RemoteFloatId

    // ---- 5. Strings, Arrays, Lists, Time & Matrices ----

    /**
     * Writes an immutable text constant, deduplicating equal strings when supported.
     *
     * @param text string content
     * @return handle to the text constant
     */
    public fun addText(text: String): RemoteStringId

    /**
     * Allocates a fresh mutable text variable initialized to [initialValue].
     *
     * Unlike [addText], each call reserves a distinct ID safe to update with
     * [addValueStringChangeAction].
     *
     * @param initialValue initial string value
     * @return handle to the mutable text variable
     */
    public fun addMutableText(initialValue: String): RemoteStringId

    /**
     * Declares a host-addressable string variable with an initial value.
     *
     * @param name unique host-visible variable name
     * @param initialValue initial string value before host overrides
     * @return handle to the named string variable
     */
    public fun addNamedString(name: String, initialValue: String): RemoteStringId

    /**
     * Formats a float value into a dynamic string variable.
     *
     * @param value float value or variable to format
     * @param digitsBefore integer digit width before the decimal point
     * @param digitsAfter fractional digit count after the decimal point
     * @param flags bitmask of padding, grouping, separator, and rounding flags
     * @return handle to the formatted string
     */
    public fun textFromFloat(
        value: MaybeVariableFloat,
        digitsBefore: Int,
        digitsAfter: Int,
        flags: Int,
    ): RemoteStringId

    /**
     * Concatenates two string variables into a new string variable.
     *
     * @param first leading string handle
     * @param second trailing string handle
     * @return handle to the concatenated string
     */
    public fun textMerge(first: RemoteStringId, second: RemoteStringId): RemoteStringId

    /**
     * Extracts a character subrange from [textId].
     *
     * @param textId source string handle
     * @param start 0-based start character index
     * @param len number of characters to take, or `-1f` to read through the end of the string
     * @return handle to the substring
     */
    public fun textSubtext(
        textId: RemoteStringId,
        start: MaybeVariableFloat,
        len: MaybeVariableFloat,
    ): RemoteStringId

    /**
     * Applies a case or trimming transformation to a subrange of [textId].
     *
     * @param textId source string handle
     * @param start 0-based start character index
     * @param len number of characters to transform, or `-1f` through the end of the string
     * @param operation transform mode (`1` = lowercase, `2` = uppercase, `3` = trim, `4` =
     *   capitalize words, `5` = uppercase first character)
     * @return handle to the transformed string
     */
    public fun textTransform(
        textId: RemoteStringId,
        start: MaybeVariableFloat,
        len: MaybeVariableFloat,
        operation: Int,
    ): RemoteStringId

    /**
     * Computes the character count of [textId] as a float variable.
     *
     * @param textId source string handle
     * @return handle to the float length
     */
    public fun textLength(textId: RemoteStringId): RemoteFloatId

    /**
     * Measures a text layout metric or length of [textId] under the active paint.
     *
     * @param textId source string handle
     * @param attributeType metric selector (`0` = width, `1` = height, `2` = left, `3` = right, `4`
     *   = top, `5` = bottom, `6` = length, optionally ORed with measurement flags)
     * @return handle to the measured float value
     */
    public fun textAttribute(textId: RemoteStringId, attributeType: Int): RemoteFloatId

    /**
     * Looks up a string from an ID list using a float index.
     *
     * @param arrayId handle to an ID list created by [addIdList] containing string IDs
     * @param index 0-based element index (truncated to integer at runtime)
     * @return handle to the resolved string
     */
    public fun textLookup(arrayId: RemoteArrayId, index: MaybeVariableFloat): RemoteStringId

    /**
     * Looks up a string from an ID list using an integer variable index.
     *
     * @param arrayId handle to an ID list created by [addIdList] containing string IDs
     * @param indexId handle to a 0-based integer index variable
     * @return handle to the resolved string
     */
    public fun textLookup(arrayId: RemoteArrayId, indexId: RemoteIntId): RemoteStringId

    /**
     * Writes an immutable list of variable or resource identifiers.
     *
     * @param ids element IDs stored in the list
     * @return handle to the ID list (in [NanEncoding.REGION_ARRAY])
     */
    public fun addIdList(ids: IntArray): RemoteArrayId

    /**
     * Looks up a resource or variable ID from an ID list at runtime.
     *
     * @param arrayId handle to an ID list created by [addIdList]
     * @param index 0-based element index
     * @param destId existing destination ID to populate, or `-1` to allocate a new ID
     * @return destination ID holding the looked-up entry
     */
    public fun idLookup(arrayId: RemoteArrayId, index: MaybeVariableFloat, destId: Int = -1): Int

    /**
     * Writes an immutable float array resource.
     *
     * @param values initial float elements
     * @return handle to the float array (in [NanEncoding.REGION_ARRAY])
     */
    public fun addFloatArray(values: FloatArray): RemoteArrayId

    /**
     * Allocates a mutable, dynamically sized float array.
     *
     * @param size element count (literal or float variable)
     * @return handle to the dynamic float array
     */
    public fun addDynamicFloatArray(size: MaybeVariableFloat): RemoteArrayId

    /**
     * Writes a single element into a dynamic float array at runtime.
     *
     * @param arrayId handle to a dynamic array created by [addDynamicFloatArray]
     * @param index 0-based element index
     * @param value float value to store
     */
    public fun setArrayValue(
        arrayId: RemoteArrayId,
        index: MaybeVariableFloat,
        value: MaybeVariableFloat,
    )

    /**
     * Extracts a calendar or elapsed-time component from a 64-bit timestamp variable.
     *
     * @param longId handle to a timestamp in epoch milliseconds
     * @param attributeType time component selector matching `TimeAttribute.TIME_*` constants
     * @return handle to the extracted float value
     */
    public fun timeAttribute(longId: RemoteLongId, attributeType: Int): RemoteFloatId

    /**
     * Writes an RPN matrix expression producing a 4x4 transform matrix variable.
     *
     * @param exp RPN matrix expression tokens (matrix operators, literals, and NaN variable IDs)
     * @return handle to the matrix variable
     */
    public fun matrixExpression(vararg exp: Float): RemoteFloatId

    // ---- 6. Bitmaps, Bitmap Fonts & Paths ----

    /**
     * Encodes and adds an image resource to the document.
     *
     * @param image platform image or bitmap object understood by this writer's encoder
     * @return handle to the bitmap resource
     */
    public fun addBitmap(image: Any): RemoteBitmapId

    /**
     * Encodes and adds a host-addressable named bitmap resource.
     *
     * @param name unique host-visible bitmap name
     * @param image platform image or bitmap object used as the default payload
     * @return handle to the named bitmap resource
     */
    public fun addNamedBitmap(name: String, image: Any): RemoteBitmapId

    /**
     * Registers an external bitmap referenced by URL.
     *
     * @param url remote or asset image URL
     * @param width expected intrinsic width in pixels
     * @param height expected intrinsic height in pixels
     * @return handle to the URL-backed bitmap resource
     */
    public fun addBitmapUrl(url: String, width: Int = 1, height: Int = 1): RemoteBitmapId

    /**
     * Allocates an empty writable bitmap of fixed dimensions.
     *
     * Use [drawOnBitmap] to redirect canvas drawing commands into this bitmap.
     *
     * @param width bitmap width in pixels
     * @param height bitmap height in pixels
     * @return handle to the created bitmap
     */
    public fun createBitmap(width: Int, height: Int): RemoteBitmapId

    /**
     * Queries a dimension attribute of [bitmapId] as a float variable.
     *
     * @param bitmapId source bitmap handle
     * @param attributeType attribute selector (`0` = width in pixels, `1` = height in pixels)
     * @return handle to the float attribute
     */
    public fun bitmapAttribute(bitmapId: RemoteBitmapId, attributeType: Int): RemoteFloatId

    /**
     * Adds a bitmap font built from per-glyph bitmaps and optional kerning pairs.
     *
     * @param glyphs glyph descriptors mapping character sequences to bitmaps
     * @param kerning map from adjacent glyph pairs (concatenated strings) to horizontal kerning
     *   adjustments in pixels
     * @return handle to the bitmap font
     */
    public fun addBitmapFont(
        glyphs: List<RemoteBitmapGlyph>,
        @Suppress("NoByteOrShort") kerning: Map<String, Short> = emptyMap(),
    ): RemoteFontId

    /**
     * Measures a text string rendered with a bitmap font.
     *
     * @param textId source string handle
     * @param bitmapFontId bitmap font handle created by [addBitmapFont]
     * @param measureType metric selector (`0` = width, `1` = height, `2` = left, `3` = right, `4` =
     *   top, `5` = bottom, optionally ORed with measurement flags)
     * @param glyphSpacing extra horizontal spacing between glyphs in pixels
     * @return handle to the measured float metric in pixels
     */
    public fun bitmapTextMeasure(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        measureType: Int,
        glyphSpacing: MaybeVariableFloat = 0f.mvf,
    ): RemoteFloatId

    /**
     * Writes a vector path from NaN-tagged path verbs and coordinate floats.
     *
     * @param pathData flat float array of path verb tokens and coordinate values
     * @param winding fill rule (`0` = non-zero winding, `1` = even-odd, `2` = inverse even-odd, `3`
     *   = inverse winding)
     * @return handle to the vector path
     */
    public fun addPathData(pathData: FloatArray, winding: Int = 0): RemotePathId

    /**
     * Writes a vector path parsed from an SVG path data string.
     *
     * @param pathData SVG path commands (such as `"M0,0 L10,10 Z"`)
     * @return handle to the vector path
     */
    public fun addPathString(pathData: String): RemotePathId

    /**
     * Compiles and registers an AGSL runtime shader with optional uniform bindings.
     *
     * Each entry in [intUniforms] must contain 1 to 4 values matching `int` through `ivec4`.
     *
     * @param shaderSource AGSL shader source code
     * @param floatUniforms map of uniform names to 1..4 float or variable components
     * @param intUniforms map of uniform names to 1..4 integer components
     * @param bitmapUniforms map of shader sampler names to bitmap handles
     * @return handle to the registered shader
     * @throws IllegalArgumentException if an integer uniform array does not have 1..4 elements
     */
    public fun addShader(
        shaderSource: String,
        floatUniforms: Map<String, List<MaybeVariableFloat>> = emptyMap(),
        intUniforms: Map<String, IntArray> = emptyMap(),
        bitmapUniforms: Map<String, RemoteBitmapId> = emptyMap(),
    ): RemoteShaderId

    // ---- 7. Delta Paint ----

    /**
     * Emits a delta paint update containing all attributes set inside [block].
     *
     * If [block] sets no properties, no paint operation is emitted.
     *
     * ```kotlin
     * writer.writePaint {
     *     it.setColor(0xFF222222.toInt())
     *     it.setAntiAlias(true)
     * }
     * ```
     *
     * @param block builder lambda recording paint property changes on a [RemotePaintWriter]
     */
    public fun writePaint(block: (RemotePaintWriter) -> Unit)

    // ---- 8. Canvas Drawing, Transforms, Control Flow & Loom Patterns ----

    /**
     * Saves the current canvas transform matrix and clip state onto the stack.
     *
     * Pair every call with a matching [restore].
     */
    public fun save()

    /** Restores the canvas transform matrix and clip state saved by the most recent [save]. */
    public fun restore()

    /**
     * Translates the canvas coordinate origin by ([dx], [dy]).
     *
     * @param dx horizontal translation in pixels
     * @param dy vertical translation in pixels
     */
    public fun translate(dx: MaybeVariableFloat, dy: MaybeVariableFloat)

    /**
     * Scales the canvas coordinate system around an optional pivot point.
     *
     * Leave [pivotX] and [pivotY] as [MaybeVariableFloat.NaN] to scale around the origin `(0, 0)`.
     *
     * @param scaleX horizontal scale factor
     * @param scaleY vertical scale factor
     * @param pivotX pivot X coordinate in pixels, or [MaybeVariableFloat.NaN] for origin
     * @param pivotY pivot Y coordinate in pixels, or [MaybeVariableFloat.NaN] for origin
     */
    public fun scale(
        scaleX: MaybeVariableFloat,
        scaleY: MaybeVariableFloat,
        pivotX: MaybeVariableFloat = MaybeVariableFloat.NaN,
        pivotY: MaybeVariableFloat = MaybeVariableFloat.NaN,
    )

    /**
     * Rotates the canvas clockwise by [degrees] around an optional pivot point.
     *
     * Leave [pivotX] and [pivotY] as [MaybeVariableFloat.NaN] to rotate around the origin `(0, 0)`.
     *
     * @param degrees clockwise rotation angle in degrees
     * @param pivotX pivot X coordinate in pixels, or [MaybeVariableFloat.NaN] for origin
     * @param pivotY pivot Y coordinate in pixels, or [MaybeVariableFloat.NaN] for origin
     */
    public fun rotate(
        degrees: MaybeVariableFloat,
        pivotX: MaybeVariableFloat = MaybeVariableFloat.NaN,
        pivotY: MaybeVariableFloat = MaybeVariableFloat.NaN,
    )

    /**
     * Skews the canvas coordinate system along the X and Y axes.
     *
     * @param skewX horizontal skew tangent (`tan(angleX)`)
     * @param skewY vertical skew tangent (`tan(angleY)`)
     */
    public fun skew(skewX: MaybeVariableFloat, skewY: MaybeVariableFloat)

    /**
     * Intersects the current canvas clip region with a rectangle.
     *
     * @param left left edge in pixels
     * @param top top edge in pixels
     * @param right right edge in pixels
     * @param bottom bottom edge in pixels
     */
    public fun clipRect(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
    )

    /**
     * Modifies the current canvas clip region with a vector path.
     *
     * @param pathId handle to the clipping path
     * @param regionOp clip combination mode (`0` = replace, `1` = difference, `2` = intersect, `3`
     *   = union, `4` = xor, `5` = reverse difference)
     */
    public fun clipPath(pathId: RemotePathId, regionOp: Int = 0)

    /** Draws the current component's child content inside a draw-content modifier or canvas. */
    public fun drawContent()

    /**
     * Draws an axis-aligned rectangle using the active paint.
     *
     * @param left left edge in pixels
     * @param top top edge in pixels
     * @param right right edge in pixels
     * @param bottom bottom edge in pixels
     */
    public fun drawRect(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
    )

    /**
     * Draws a rounded rectangle using the active paint.
     *
     * @param left left edge in pixels
     * @param top top edge in pixels
     * @param right right edge in pixels
     * @param bottom bottom edge in pixels
     * @param radiusX horizontal corner radius in pixels
     * @param radiusY vertical corner radius in pixels
     */
    public fun drawRoundRect(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        radiusX: MaybeVariableFloat,
        radiusY: MaybeVariableFloat,
    )

    /**
     * Draws a circle using the active paint.
     *
     * @param centerX center X coordinate in pixels
     * @param centerY center Y coordinate in pixels
     * @param radius circle radius in pixels
     */
    public fun drawCircle(
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
        radius: MaybeVariableFloat,
    )

    /**
     * Draws an oval inscribed in the specified bounding rectangle.
     *
     * @param left bounding box left edge in pixels
     * @param top bounding box top edge in pixels
     * @param right bounding box right edge in pixels
     * @param bottom bounding box bottom edge in pixels
     */
    public fun drawOval(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
    )

    /**
     * Draws an open elliptical arc inscribed in the specified bounding rectangle.
     *
     * Angles are measured in degrees clockwise from the positive X-axis (3 o'clock). For a closed
     * pie wedge connecting through the center, use [drawSector].
     *
     * @param left bounding oval left edge in pixels
     * @param top bounding oval top edge in pixels
     * @param right bounding oval right edge in pixels
     * @param bottom bounding oval bottom edge in pixels
     * @param startAngle starting angle in degrees clockwise from 3 o'clock
     * @param sweepAngle clockwise sweep angle in degrees
     */
    public fun drawArc(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        startAngle: MaybeVariableFloat,
        sweepAngle: MaybeVariableFloat,
    )

    /**
     * Draws a closed elliptical sector (pie wedge) inscribed in the bounding rectangle.
     *
     * Angles are measured in degrees clockwise from the positive X-axis (3 o'clock). For an open
     * arc without radial edges to the center, use [drawArc].
     *
     * @param left bounding oval left edge in pixels
     * @param top bounding oval top edge in pixels
     * @param right bounding oval right edge in pixels
     * @param bottom bounding oval bottom edge in pixels
     * @param startAngle starting angle in degrees clockwise from 3 o'clock
     * @param sweepAngle clockwise sweep angle in degrees
     */
    public fun drawSector(
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        startAngle: MaybeVariableFloat,
        sweepAngle: MaybeVariableFloat,
    )

    /**
     * Draws a line segment between two points using the active paint.
     *
     * @param x1 start point X coordinate in pixels
     * @param y1 start point Y coordinate in pixels
     * @param x2 end point X coordinate in pixels
     * @param y2 end point Y coordinate in pixels
     */
    public fun drawLine(
        x1: MaybeVariableFloat,
        y1: MaybeVariableFloat,
        x2: MaybeVariableFloat,
        y2: MaybeVariableFloat,
    )

    /**
     * Draws a vector path using the active paint.
     *
     * @param pathId handle to the path to draw
     */
    public fun drawPath(pathId: RemotePathId)

    /**
     * Draws an interpolated path morphing between [path1Id] and [path2Id].
     *
     * Both paths must have compatible verb structures for smooth morphing.
     *
     * @param path1Id starting path handle at `tween = 0f`
     * @param path2Id ending path handle at `tween = 1f`
     * @param tween morph interpolation factor in `[0, 1]`
     * @param start normalized trim start along the interpolated path (`0f` = beginning)
     * @param stop normalized trim end along the interpolated path (`1f` = end)
     */
    public fun drawTweenPath(
        path1Id: RemotePathId,
        path2Id: RemotePathId,
        tween: MaybeVariableFloat,
        start: MaybeVariableFloat = 0f.mvf,
        stop: MaybeVariableFloat = 1f.mvf,
    )

    /**
     * Draws a bitmap at its intrinsic size anchored at ([left], [top]).
     *
     * @param imageId handle to the bitmap to draw
     * @param left top-left X coordinate in pixels
     * @param top top-left Y coordinate in pixels
     * @param contentDescription optional accessibility description
     */
    public fun drawBitmap(
        imageId: RemoteBitmapId,
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        contentDescription: String? = null,
    )

    /**
     * Draws a bitmap stretched to fill the destination rectangle.
     *
     * @param imageId handle to the bitmap to draw
     * @param left destination left edge in pixels
     * @param top destination top edge in pixels
     * @param right destination right edge in pixels
     * @param bottom destination bottom edge in pixels
     * @param contentDescription optional accessibility description
     */
    public fun drawBitmap(
        imageId: RemoteBitmapId,
        left: MaybeVariableFloat,
        top: MaybeVariableFloat,
        right: MaybeVariableFloat,
        bottom: MaybeVariableFloat,
        contentDescription: String? = null,
    )

    /**
     * Draws an integer source sub-rectangle of a bitmap into an integer destination rectangle.
     *
     * @param imageId handle to the bitmap to draw
     * @param srcLeft source crop left edge in pixels
     * @param srcTop source crop top edge in pixels
     * @param srcRight source crop right edge in pixels
     * @param srcBottom source crop bottom edge in pixels
     * @param dstLeft destination left edge in pixels
     * @param dstTop destination top edge in pixels
     * @param dstRight destination right edge in pixels
     * @param dstBottom destination bottom edge in pixels
     * @param contentDescription optional accessibility description
     */
    public fun drawBitmapInt(
        imageId: RemoteBitmapId,
        srcLeft: Int,
        srcTop: Int,
        srcRight: Int,
        srcBottom: Int,
        dstLeft: Int,
        dstTop: Int,
        dstRight: Int,
        dstBottom: Int,
        contentDescription: String? = null,
    )

    /**
     * Draws a bitmap sub-rectangle into a destination rectangle using a scaling policy.
     *
     * @param imageId handle to the bitmap to draw
     * @param srcLeft source crop left edge in pixels
     * @param srcTop source crop top edge in pixels
     * @param srcRight source crop right edge in pixels
     * @param srcBottom source crop bottom edge in pixels
     * @param dstLeft destination left edge in pixels
     * @param dstTop destination top edge in pixels
     * @param dstRight destination right edge in pixels
     * @param dstBottom destination bottom edge in pixels
     * @param scaleType scaling mode (`0` = none, `1` = inside, `2` = fill width, `3` = fill height,
     *   `4` = fit, `5` = crop, `6` = fill bounds, `7` = fixed scale)
     * @param scaleFactor multiplier applied when [scaleType] is fixed scale (`7`)
     * @param contentDescription optional accessibility description
     */
    public fun drawScaledBitmap(
        imageId: RemoteBitmapId,
        srcLeft: MaybeVariableFloat,
        srcTop: MaybeVariableFloat,
        srcRight: MaybeVariableFloat,
        srcBottom: MaybeVariableFloat,
        dstLeft: MaybeVariableFloat,
        dstTop: MaybeVariableFloat,
        dstRight: MaybeVariableFloat,
        dstBottom: MaybeVariableFloat,
        scaleType: Int,
        scaleFactor: MaybeVariableFloat,
        contentDescription: String? = null,
    )

    /**
     * Draws a shaped text run with surrounding shaping context at ([x], [y]).
     *
     * @param textId handle to the string to draw
     * @param start inclusive start character index of the drawn run
     * @param end exclusive end character index of the drawn run
     * @param contextStart inclusive start index of the shaping context
     * @param contextEnd exclusive end index of the shaping context
     * @param x origin X coordinate in pixels
     * @param y baseline Y coordinate in pixels
     * @param rtl `true` to shape and lay out the run right-to-left
     */
    public fun drawTextRun(
        textId: RemoteStringId,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        rtl: Boolean,
    )

    /**
     * Draws text anchored at ([x], [y]) using normalized bounding-box pan factors.
     *
     * Panning factors map to the measured text box: `0f` centers on ([x], [y]). Horizontally, `panX
     * = -1f` places the box start at [x] and `panX = 1f` places the box end at [x]. Vertically,
     * `panY = 1f` places the box top at [y] and `panY = -1f` places the box bottom at [y]. Pass
     * [MaybeVariableFloat.NaN] for [panY] to align vertically to the text baseline.
     *
     * @param textId handle to the string to draw
     * @param x anchor X coordinate in pixels
     * @param y anchor Y coordinate in pixels
     * @param panX horizontal pan factor in `[-1, 1]`
     * @param panY vertical pan factor in `[-1, 1]`, or [MaybeVariableFloat.NaN] for baseline
     * @param flags bitmask of anchor flags (`1` = RTL, `2` = monospace measure, `4` = measure every
     *   time, `8` = baseline-relative)
     */
    public fun drawTextAnchored(
        textId: RemoteStringId,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        panX: MaybeVariableFloat = 0f.mvf,
        panY: MaybeVariableFloat = 0f.mvf,
        flags: Int = 0,
    )

    /**
     * Draws text following the contour of a vector path.
     *
     * @param textId handle to the string to draw
     * @param pathId handle to the baseline path
     * @param hOffset distance along the path from its start in pixels
     * @param vOffset perpendicular offset from the path in pixels
     */
    public fun drawTextOnPath(
        textId: RemoteStringId,
        pathId: RemotePathId,
        hOffset: MaybeVariableFloat,
        vOffset: MaybeVariableFloat,
    )

    /**
     * Draws curved text along a circular arc.
     *
     * @param textId handle to the string to draw
     * @param centerX circle center X coordinate in pixels
     * @param centerY circle center Y coordinate in pixels
     * @param radius circle radius in pixels
     * @param startAngle starting angle in degrees clockwise from 3 o'clock
     * @param warpRadiusOffset radial offset in pixels used for circular glyph warping
     * @param alignment alignment relative to [startAngle] (`0` = start, `1` = center, `2` = end)
     * @param placement placement relative to the circle perimeter (`0` = outside, `1` = inside)
     */
    public fun drawTextOnCircle(
        textId: RemoteStringId,
        centerX: MaybeVariableFloat,
        centerY: MaybeVariableFloat,
        radius: MaybeVariableFloat,
        startAngle: MaybeVariableFloat,
        warpRadiusOffset: MaybeVariableFloat,
        alignment: Int,
        placement: Int,
    )

    /**
     * Draws a substring using a bitmap font starting at ([x], [y]).
     *
     * @param textId handle to the string to draw
     * @param bitmapFontId handle to the bitmap font created by [addBitmapFont]
     * @param start inclusive start character index
     * @param end exclusive end character index, or `-1` for the full string
     * @param x starting X coordinate in pixels
     * @param y top-edge Y coordinate in pixels
     * @param glyphSpacing extra horizontal spacing between glyphs in pixels
     */
    public fun drawBitmapFontTextRun(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        start: Int,
        end: Int,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        glyphSpacing: MaybeVariableFloat = 0f.mvf,
    )

    /**
     * Draws a bitmap-font text run along a vector path.
     *
     * @param textId handle to the string to draw
     * @param bitmapFontId handle to the bitmap font created by [addBitmapFont]
     * @param pathId handle to the baseline path
     * @param start inclusive start character index
     * @param end exclusive end character index, or `-1` for the full string
     * @param yAdj perpendicular offset from the path in pixels
     * @param glyphSpacing extra horizontal spacing between glyphs in pixels
     */
    public fun drawBitmapFontTextRunOnPath(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        pathId: RemotePathId,
        start: Int,
        end: Int,
        yAdj: MaybeVariableFloat,
        glyphSpacing: MaybeVariableFloat = 0f.mvf,
    )

    /**
     * Draws bitmap-font text anchored at ([x], [y]) with normalized pan factors.
     *
     * @param textId handle to the string to draw
     * @param bitmapFontId handle to the bitmap font created by [addBitmapFont]
     * @param start inclusive start character index
     * @param end exclusive end character index, or `-1f` for the full string
     * @param x anchor X coordinate in pixels
     * @param y anchor Y coordinate in pixels
     * @param panX horizontal pan factor in `[-1, 1]` (`-1f` = start, `0f` = center, `1f` = end)
     * @param panY vertical pan factor in `[-1, 1]` (`1f` = top, `0f` = center, `-1f` = bottom)
     * @param glyphSpacing extra horizontal spacing between glyphs in pixels
     */
    public fun drawBitmapTextAnchored(
        textId: RemoteStringId,
        bitmapFontId: RemoteFontId,
        start: MaybeVariableFloat,
        end: MaybeVariableFloat,
        x: MaybeVariableFloat,
        y: MaybeVariableFloat,
        panX: MaybeVariableFloat = 0f.mvf,
        panY: MaybeVariableFloat = 0f.mvf,
        glyphSpacing: MaybeVariableFloat = 0f.mvf,
    )

    /**
     * Redirects subsequent drawing commands into a bitmap, or back to the main canvas.
     *
     * Pass `RemoteBitmapId(0)` to restore rendering to the main canvas.
     *
     * @param bitmapId target bitmap handle, or `RemoteBitmapId(0)` to resume main canvas drawing
     * @param mode initialization mode (`0` = clear with [color], `1` = preserve contents, `2` =
     *   bind offscreen bitmap to the component ID in [color])
     * @param color ARGB clear color when [mode] is `0`, or component ID when [mode] is `2`
     */
    public fun drawOnBitmap(bitmapId: RemoteBitmapId, mode: Int, @ColorInt color: Int)

    /**
     * Emits a conditional container whose [content] runs only when the comparison holds.
     *
     * @param type comparison operator (`0` = eq, `1` = neq, `2` = lt, `3` = lte, `4` = gt, `5` =
     *   gte, `6` = changed)
     * @param a left-hand operand
     * @param b right-hand operand
     * @param content operations emitted inside the conditional container
     */
    public fun conditionalOperations(
        type: Int,
        a: MaybeVariableFloat,
        b: MaybeVariableFloat,
        content: () -> Unit,
    )

    /**
     * Opens a player-side numeric loop container.
     *
     * Pair every call with [endLoop], or prefer [loop] for scoped closure.
     *
     * @param indexId float variable ID updated on each iteration, or `0` for an anonymous counter
     * @param from starting loop value
     * @param step positive increment added after each iteration
     * @param until exclusive upper loop bound (runs while the loop variable is `< until`)
     */
    public fun startLoop(
        indexId: Int,
        from: MaybeVariableFloat,
        step: MaybeVariableFloat,
        until: MaybeVariableFloat,
    )

    /** Closes the loop container opened by [startLoop]. */
    public fun endLoop()

    /**
     * Emits a scoped player-side loop container, always closing it with [endLoop].
     *
     * @param indexId float variable ID updated on each iteration, or `0` for an anonymous counter
     * @param from starting loop value
     * @param step positive increment added after each iteration
     * @param until exclusive upper loop bound (runs while the loop variable is `< until`)
     * @param content operations emitted inside the loop body
     */
    public fun loop(
        indexId: Int,
        from: MaybeVariableFloat,
        step: MaybeVariableFloat,
        until: MaybeVariableFloat,
        content: () -> Unit,
    ) {
        startLoop(indexId, from, step, until)
        try {
            content()
        } finally {
            endLoop()
        }
    }

    /**
     * Opens a reusable pattern (macro) definition parameterized by [paramIds].
     *
     * Pair every call with [endPatternDefine].
     *
     * @param patternId unique pattern identifier
     * @param paramIds local parameter variable IDs bound during [inflatePattern]
     */
    public fun definePattern(patternId: Int, paramIds: IntArray)

    /** Closes the pattern definition opened by [definePattern]. */
    public fun endPatternDefine()

    /**
     * Opens an inflation site invoking a pattern defined by [definePattern].
     *
     * Pair every call with [endPatternInflation].
     *
     * @param patternId identifier of the pattern to inflate
     * @param argIds argument variable IDs matching the pattern's parameter list
     */
    public fun inflatePattern(patternId: Int, argIds: IntArray)

    /** Closes the pattern inflation opened by [inflatePattern]. */
    public fun endPatternInflation()

    /**
     * Opens a pattern iteration container over each entry of [collectionId].
     *
     * Pair every call with [endPatternForEach].
     *
     * @param collectionId ID of the array or list collection to iterate
     * @param itemId local variable ID receiving the current element on each iteration
     */
    public fun startPatternForEach(collectionId: Int, itemId: Int)

    /** Closes the pattern iteration container opened by [startPatternForEach]. */
    public fun endPatternForEach()

    // ---- 9. Layout Containers ----

    /**
     * Opens the document's root layout container.
     *
     * Pair every call with [endRoot], or use [root] for automatic closing.
     *
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @return resolved component ID assigned to the root layout
     */
    public fun startRoot(componentId: Int = -1): Int

    /** Closes the root layout container opened by [startRoot]. */
    public fun endRoot()

    /**
     * Emits a scoped root layout container, always closing it with [endRoot].
     *
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param content child components and operations emitted inside the root container
     */
    public fun root(componentId: Int = -1, content: () -> Unit) {
        startRoot(componentId)
        try {
            content()
        } finally {
            endRoot()
        }
    }

    /**
     * Opens a stacked box layout container.
     *
     * Pair every call with [endBox].
     *
     * @param horizontalAlignment horizontal child alignment (`1` = start, `2` = center, `3` = end)
     * @param verticalAlignment vertical child alignment (`4` = top, `2` = center, `5` = bottom)
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this container
     */
    public fun startBox(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /** Closes the box layout container opened by [startBox]. */
    public fun endBox()

    /**
     * Opens a fit-box container that displays only the child fitting the available space.
     *
     * Pair every call with [endFitBox].
     *
     * @param horizontalAlignment horizontal child alignment (`1` = start, `2` = center, `3` = end)
     * @param verticalAlignment vertical child alignment (`4` = top, `2` = center, `5` = bottom)
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this container
     */
    public fun startFitBox(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /** Closes the fit-box layout container opened by [startFitBox]. */
    public fun endFitBox()

    /**
     * Opens a horizontal row layout container.
     *
     * Pair every call with [endRow].
     *
     * @param horizontalAlignment horizontal arrangement (`1` = start, `2` = center, `3` = end, `6`
     *   = spaceBetween, `7` = spaceEvenly, `8` = spaceAround)
     * @param verticalAlignment vertical child alignment (`4` = top, `2` = center, `5` = bottom)
     * @param spacedBy horizontal gap between consecutive children in pixels
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this container
     */
    public fun startRow(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat = 0f.mvf,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /** Closes the row layout container opened by [startRow]. */
    public fun endRow()

    /**
     * Opens a vertical column layout container.
     *
     * Pair every call with [endColumn].
     *
     * @param horizontalAlignment horizontal child alignment (`1` = start, `2` = center, `3` = end)
     * @param verticalAlignment vertical arrangement (`4` = top, `2` = center, `5` = bottom, `6` =
     *   spaceBetween, `7` = spaceEvenly, `8` = spaceAround)
     * @param spacedBy vertical gap between consecutive children in pixels
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this container
     */
    public fun startColumn(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat = 0f.mvf,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /** Closes the column layout container opened by [startColumn]. */
    public fun endColumn()

    /**
     * Opens a horizontal row container that drops lower-priority children when space is tight.
     *
     * Pair every call with [endCollapsibleRow].
     *
     * @param horizontalAlignment horizontal arrangement (`1`, `2`, `3`, `6`, `7`, `8`)
     * @param verticalAlignment vertical child alignment (`4` = top, `2` = center, `5` = bottom)
     * @param spacedBy horizontal gap between visible children in pixels
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this container
     */
    public fun startCollapsibleRow(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat = 0f.mvf,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /** Closes the collapsible row container opened by [startCollapsibleRow]. */
    public fun endCollapsibleRow()

    /**
     * Opens a vertical column container that drops lower-priority children when space is tight.
     *
     * Pair every call with [endCollapsibleColumn].
     *
     * @param horizontalAlignment horizontal child alignment (`1` = start, `2` = center, `3` = end)
     * @param verticalAlignment vertical arrangement (`4`, `2`, `5`, `6`, `7`, `8`)
     * @param spacedBy vertical gap between visible children in pixels
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this container
     */
    public fun startCollapsibleColumn(
        horizontalAlignment: Int,
        verticalAlignment: Int,
        spacedBy: MaybeVariableFloat = 0f.mvf,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /** Closes the collapsible column container opened by [startCollapsibleColumn]. */
    public fun endCollapsibleColumn()

    /**
     * Opens a wrapping flow layout container.
     *
     * Pair every call with [endFlow].
     *
     * @param positioning horizontal arrangement within each row (`1`, `2`, `3`, `6`, `7`, `8`)
     * @param verticalAlignment vertical arrangement across rows (`4`, `2`, `5`, `6`, `7`, `8`)
     * @param maxItemsInEachRow maximum children placed on a single row
     * @param maxLines maximum number of wrapped rows
     * @param spacedBy gap between children in pixels
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this container
     */
    public fun startFlow(
        positioning: Int,
        verticalAlignment: Int,
        maxItemsInEachRow: Int = Int.MAX_VALUE,
        maxLines: Int = Int.MAX_VALUE,
        spacedBy: MaybeVariableFloat = 0f.mvf,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /** Closes the flow layout container opened by [startFlow]. */
    public fun endFlow()

    /**
     * Opens a state-switching layout that transitions between its child layouts by index.
     *
     * Pair every call with [endStateLayout].
     *
     * @param indexId integer variable ID selecting the active child state (0-based)
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this container
     */
    public fun startStateLayout(
        indexId: Int,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /** Closes the state layout container opened by [startStateLayout]. */
    public fun endStateLayout()

    /**
     * Opens a canvas component for imperative 2D drawing commands.
     *
     * Pair every call with [endCanvas].
     *
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this canvas
     * @return resolved component ID assigned to the canvas
     */
    public fun startCanvas(componentId: Int = -1, modifiers: () -> Unit = {}): Int

    /** Closes the canvas component opened by [startCanvas]. */
    public fun endCanvas()

    /**
     * Opens a standalone canvas-operations block inside a modifier or draw hook.
     *
     * Pair every call with [endCanvasOperations].
     */
    public fun startCanvasOperations()

    /** Closes the canvas-operations block opened by [startCanvasOperations]. */
    public fun endCanvasOperations()

    /**
     * Emits a self-contained image layout component.
     *
     * @param imageId handle to the bitmap to display
     * @param scaleType scaling mode (`0` = none, `1` = inside, `2` = fill width, `3` = fill height,
     *   `4` = fit, `5` = crop, `6` = fill bounds, `7` = fixed scale)
     * @param alpha opacity in `[0, 1]`
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this image
     */
    public fun imageComponent(
        imageId: RemoteBitmapId,
        scaleType: Int,
        alpha: MaybeVariableFloat,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /**
     * Registers a reusable text style referenced by [textComponent].
     *
     * Unspecified (`null`) attributes inherit from [parentId] or platform defaults.
     *
     * @param color literal ARGB text color, or `null` to inherit
     * @param colorId dynamic color variable ID, or `null` to inherit
     * @param fontSize font size in pixels, or `null` to inherit
     * @param minFontSize minimum autosize font size in pixels, or `null`
     * @param maxFontSize maximum autosize font size in pixels, or `null`
     * @param fontStyle font style flag (`0` = normal, `1` = italic), or `null`
     * @param fontWeight numeric font weight (`100..1000`), or `null`
     * @param fontFamily font family name, or `null` for default
     * @param textAlign text alignment (`1` = left, `2` = right, `3` = center, `4` = justify, `5` =
     *   start, `6` = end), or `null`
     * @param overflow overflow handling (`1` = clip, `2` = visible, `3` = ellipsis, `4` = start
     *   ellipsis, `5` = middle ellipsis), or `null`
     * @param maxLines maximum line count, or `null`
     * @param letterSpacing additional tracking in em units, or `null`
     * @param lineHeightAdd extra spacing added to each line in pixels, or `null`
     * @param lineHeightMultiplier line height multiplier, or `null`
     * @param lineBreakStrategy line-breaking strategy constant, or `null`
     * @param hyphenationFrequency hyphenation frequency constant, or `null`
     * @param justificationMode text justification mode constant, or `null`
     * @param underline `true` to underline text, or `null`
     * @param strikethrough `true` to strike through text, or `null`
     * @param fontAxisNames 4-character OpenType variation axis names, or `null`
     * @param fontAxisValues variation axis values matching [fontAxisNames], or `null`
     * @param autosize `true` to enable automatic font-size fitting, or `null`
     * @param parentId parent text style ID to inherit unset properties from, or `0` for none
     * @return allocated text style identifier
     */
    public fun addTextStyle(
        color: Int? = null,
        colorId: Int? = null,
        fontSize: MaybeVariableFloat? = null,
        minFontSize: MaybeVariableFloat? = null,
        maxFontSize: MaybeVariableFloat? = null,
        fontStyle: Int? = null,
        fontWeight: MaybeVariableFloat? = null,
        fontFamily: String? = null,
        textAlign: Int? = null,
        overflow: Int? = null,
        maxLines: Int? = null,
        letterSpacing: MaybeVariableFloat? = null,
        lineHeightAdd: MaybeVariableFloat? = null,
        lineHeightMultiplier: MaybeVariableFloat? = null,
        lineBreakStrategy: Int? = null,
        hyphenationFrequency: Int? = null,
        justificationMode: Int? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
        fontAxisNames: Array<String>? = null,
        fontAxisValues: List<MaybeVariableFloat>? = null,
        autosize: Boolean? = null,
        parentId: Int = 0,
    ): Int

    /**
     * Emits a text layout component styled by a registered [styleId].
     *
     * @param textId handle to the string to display
     * @param styleId text style ID returned by [addTextStyle]
     * @param flags reserved core text flags bitmask (default `0`)
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this component
     */
    public fun textComponent(
        textId: RemoteStringId,
        styleId: Int,
        flags: Int = 0,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /**
     * Emits a text layout component with inline typography attributes.
     *
     * @param textId handle to the string to display
     * @param color literal ARGB text color
     * @param colorId dynamic color variable ID, or `-1` when using literal [color]
     * @param fontSize font size in pixels
     * @param minFontSize minimum autosize font size in pixels, or `null` if unset
     * @param maxFontSize maximum autosize font size in pixels, or `null` if unset
     * @param fontStyle font style flag (`0` = normal, `1` = italic)
     * @param fontWeight numeric font weight (`100..1000`)
     * @param fontFamily font family name, or `null` for default
     * @param textAlign text alignment (`1` = left, `2` = right, `3` = center, `4` = justify, `5` =
     *   start, `6` = end)
     * @param overflow overflow handling (`1` = clip, `2` = visible, `3` = ellipsis, `4` = start
     *   ellipsis, `5` = middle ellipsis)
     * @param maxLines maximum line count
     * @param letterSpacing additional tracking in em units
     * @param lineHeightAdd extra spacing added to each line in pixels
     * @param lineHeightMultiplier line height multiplier
     * @param lineBreakStrategy line-breaking strategy constant
     * @param hyphenationFrequency hyphenation frequency constant
     * @param justificationMode text justification mode constant
     * @param underline `true` to underline text
     * @param strikethrough `true` to strike through text
     * @param fontAxisNames 4-character OpenType variation axis names, or `null`
     * @param fontAxisValues variation axis values matching [fontAxisNames], or `null`
     * @param autosize `true` to enable automatic font-size fitting
     * @param flags reserved core text flags bitmask (default `0`; dynamic color is controlled by
     *   [colorId])
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this component
     */
    public fun textComponent(
        textId: RemoteStringId,
        color: Int,
        colorId: Int,
        fontSize: MaybeVariableFloat,
        minFontSize: MaybeVariableFloat?,
        maxFontSize: MaybeVariableFloat?,
        fontStyle: Int,
        fontWeight: MaybeVariableFloat,
        fontFamily: String?,
        textAlign: Int,
        overflow: Int,
        maxLines: Int,
        letterSpacing: MaybeVariableFloat,
        lineHeightAdd: MaybeVariableFloat,
        lineHeightMultiplier: MaybeVariableFloat,
        lineBreakStrategy: Int,
        hyphenationFrequency: Int,
        justificationMode: Int,
        underline: Boolean,
        strikethrough: Boolean,
        fontAxisNames: Array<String>?,
        fontAxisValues: List<MaybeVariableFloat>?,
        autosize: Boolean,
        flags: Int,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /**
     * Opens a host-extensible custom layout component.
     *
     * Pair every call with [endCustom].
     *
     * @param config component type or configuration identifier string
     * @param properties typed input and return properties passed to the custom component
     * @param componentId explicit component ID, or `-1` to auto-generate one
     * @param modifiers lambda emitting layout modifiers attached to this component
     */
    public fun startCustom(
        config: String,
        properties: List<RemoteCustomProperty>,
        componentId: Int = -1,
        modifiers: () -> Unit = {},
    )

    /** Closes the custom component opened by [startCustom]. */
    public fun endCustom()

    // ---- 10. Actions ----

    /**
     * Emits a host callback action identified by an integer ID.
     *
     * @param actionId host-defined action identifier
     */
    public fun addHostAction(actionId: Int)

    /**
     * Emits a host callback action carrying a string metadata payload.
     *
     * @param actionId host-defined action identifier
     * @param metadataId handle to the string metadata passed to the host
     */
    public fun addHostActionMetadata(actionId: Int, metadataId: RemoteStringId)

    /**
     * Emits a named host callback action with an optional typed payload variable.
     *
     * @param nameId handle to the action name string
     * @param type payload type tag (`-1` = none, `0` = float, `1` = int, `2` = string, `3` = float
     *   array)
     * @param valueId variable ID of the payload, or `-1` when [type] is `-1`
     */
    public fun addHostNamedAction(nameId: RemoteStringId, type: Int, valueId: Int)

    /**
     * Emits an action that assigns a literal integer to [targetValueId].
     *
     * @param targetValueId mutable integer variable to update
     * @param value literal integer value to assign
     */
    public fun addValueIntegerChangeAction(targetValueId: RemoteIntId, value: Int)

    /**
     * Emits an action that copies the current value of [srcValueId] into [targetValueId].
     *
     * @param targetValueId mutable integer variable to update
     * @param srcValueId source integer variable or expression handle
     */
    public fun addValueIntegerExpressionChangeAction(
        targetValueId: RemoteIntId,
        srcValueId: RemoteIntId,
    )

    /**
     * Emits an action that assigns a float value to [targetValueId].
     *
     * @param targetValueId mutable float variable to update
     * @param value literal float or variable reference to assign
     */
    public fun addValueFloatChangeAction(targetValueId: RemoteFloatId, value: MaybeVariableFloat)

    /**
     * Emits an action that copies the evaluated value of [srcValueId] into [targetValueId].
     *
     * @param targetValueId mutable float variable to update
     * @param srcValueId source float variable or expression handle
     */
    public fun addValueFloatExpressionChangeAction(
        targetValueId: RemoteFloatId,
        srcValueId: RemoteFloatId,
    )

    /**
     * Emits an action that copies the string value of [srcTextId] into [targetTextId].
     *
     * @param targetTextId mutable string variable to update
     * @param srcTextId source string handle
     */
    public fun addValueStringChangeAction(targetTextId: RemoteStringId, srcTextId: RemoteStringId)
}

/**
 * Writes [ids] as a variable-ID list and returns its [RemoteArrayId].
 *
 * @param ids variable or resource identifiers to store in the list
 * @return handle to the created ID list
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteDocumentWriter.addIdList(vararg ids: Int): RemoteArrayId = addIdList(ids)
