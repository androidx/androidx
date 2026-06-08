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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@file:JvmName("RcScopes")

package androidx.compose.remote.creation.dsl

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.modifiers.RecordingModifier

/** Root scope for building RemoteCompose documents. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @DslMarker public annotation class RcDslMarker

/**
 * Root procedural DSL scope for constructing RemoteCompose layout trees, reactive variables, and
 * drawing resources inside [createRcBuffer].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcScope {

    /** Adds a Box layout container to the document. */
    public fun Box(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        content: RcBoxScope.() -> Unit = {},
    )

    /** Adds a FitBox layout container that scales its content to fit within its bounds. */
    public fun FitBox(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        content: RcScope.() -> Unit = {},
    )

    /** Adds a StateLayout container that switches its active child based on [stateIndex]. */
    public fun StateLayout(
        stateIndex: RcInteger,
        modifier: Modifier = Modifier,
        content: RcScope.() -> Unit = {},
    )

    /** Adds a platform-specific CustomComponent layout manager node to host native views. */
    @ExperimentalRemoteCreationApi
    public fun CustomComponent(
        config: String,
        modifier: Modifier = Modifier,
        properties: List<RcCustomProperty> = emptyList(),
        content: RcScope.() -> Unit = {},
    )

    /** Adds a vertical Column layout container to the document. */
    public fun Column(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcColumnVerticalPositioning = RcColumnVerticalPositioning.Top,
        content: RcColumnScope.() -> Unit = {},
    )

    /** Adds a horizontal Row layout container to the document. */
    public fun Row(
        modifier: Modifier = Modifier,
        horizontal: RcRowHorizontalPositioning = RcRowHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        content: RcRowScope.() -> Unit = {},
    )

    /** Adds a wrapping Flow layout container to the document. */
    public fun Flow(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        maxItemsInEachRow: Int = Int.MAX_VALUE,
        maxLines: Int = Int.MAX_VALUE,
        content: RcFlowScope.() -> Unit = {},
    )

    /** Adds a CollapsibleColumn layout to the document. */
    public fun CollapsibleColumn(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        content: RcCollapsibleColumnScope.() -> Unit = {},
    )

    /** Adds a CollapsibleRow layout to the document. */
    public fun CollapsibleRow(
        modifier: Modifier = Modifier,
        horizontal: RcHorizontalPositioning = RcHorizontalPositioning.Start,
        vertical: RcVerticalPositioning = RcVerticalPositioning.Top,
        content: RcCollapsibleRowScope.() -> Unit = {},
    )

    /** Adds a Text component to the document. */
    public fun Text(
        text: String,
        modifier: Modifier = Modifier,
        @ColorInt color: Int = 0xFF000000.toInt(),
        fontSize: RcSp = 16.rsp,
        fontStyle: RcFontStyle = RcFontStyle.Normal,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        fontFamily: String? = null,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        underline: Boolean = false,
        strikethrough: Boolean = false,
        style: RcTextStyle? = null,
    )

    /** Adds a Text component using a remote string reference. */
    public fun Text(
        text: RcText,
        modifier: Modifier = Modifier,
        @ColorInt color: Int = 0xFF000000.toInt(),
        fontSize: RcSp = 16.rsp,
        fontStyle: RcFontStyle = RcFontStyle.Normal,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        fontFamily: String? = null,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        underline: Boolean = false,
        strikethrough: Boolean = false,
        style: RcTextStyle? = null,
    )

    /**
     * Adds a text input layout: a box whose content displays [text], which the user can edit. When
     * the layout is clicked on a player with text input support, a platform editor opens over it
     * and writes edits back into [text]; every binding on that string updates. The content
     * typically includes a [Text] bound to [text] (the editor is aligned with it and styled from
     * it), and may add decorations such as a label, an icon or a border. [text] must be a plain
     * string (e.g. from [remoteText]), not a derived one. See [EditableText] for the common case.
     *
     * @param text the edited string
     * @param keyboardType the keyboard to show
     * @param imeAction the action key of the keyboard
     * @param singleLine whether the editor is a single line (the action key replaces newline)
     * @param commitOnEveryKeystroke write the string on every keystroke (true) or only on the IME
     *   action / focus loss (false)
     * @param maxLength maximum number of characters, 0 for unlimited
     * @param filter characters the editor accepts
     * @param hint placeholder shown when the text is empty
     * @param autoCorrect whether the keyboard auto-correction is enabled
     * @param capitalization automatic capitalization
     * @param contentType what the string holds, for autofill and keyboard suggestions
     * @param content the content of the layout
     */
    public fun TextInputLayout(
        text: RcText,
        modifier: Modifier = Modifier,
        keyboardType: RcKeyboardType = RcKeyboardType.Text,
        imeAction: RcImeAction = RcImeAction.Default,
        singleLine: Boolean = true,
        commitOnEveryKeystroke: Boolean = true,
        maxLength: Int = 0,
        filter: RcInputFilter = RcInputFilter.None,
        hint: RcText? = null,
        autoCorrect: Boolean = false,
        capitalization: RcCapitalization = RcCapitalization.None,
        contentType: RcContentType = RcContentType.None,
        content: RcBoxScope.() -> Unit,
    )

    /**
     * Adds an editable Text: a [TextInputLayout] whose content is a [Text] displaying [text], so
     * that tapping the text opens the platform editor over it on players with text input support.
     * The [modifier] applies to the layout (background, border, padding...); the text fills it.
     *
     * @param text the displayed and edited string
     * @param maxLines maximum number of lines; a single-line field always uses one line
     * @param keyboardType the keyboard to show
     * @param imeAction the action key of the keyboard
     * @param singleLine whether the editor is a single line (the action key replaces newline)
     * @param commitOnEveryKeystroke write the string on every keystroke (true) or only on the IME
     *   action / focus loss (false)
     * @param maxLength maximum number of characters, 0 for unlimited
     * @param filter characters the editor accepts
     * @param hint placeholder shown when the text is empty
     * @param autoCorrect whether the keyboard auto-correction is enabled
     * @param capitalization automatic capitalization
     * @param contentType what the string holds, for autofill and keyboard suggestions
     */
    public fun EditableText(
        text: RcText,
        modifier: Modifier = Modifier,
        @ColorInt color: Int = 0xFF000000.toInt(),
        fontSize: RcSp = 16.rsp,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        keyboardType: RcKeyboardType = RcKeyboardType.Text,
        imeAction: RcImeAction = RcImeAction.Default,
        singleLine: Boolean = true,
        commitOnEveryKeystroke: Boolean = true,
        maxLength: Int = 0,
        filter: RcInputFilter = RcInputFilter.None,
        hint: RcText? = null,
        autoCorrect: Boolean = false,
        capitalization: RcCapitalization = RcCapitalization.None,
        contentType: RcContentType = RcContentType.None,
    ): Unit =
        TextInputLayout(
            text = text,
            modifier = modifier,
            keyboardType = keyboardType,
            imeAction = imeAction,
            singleLine = singleLine,
            commitOnEveryKeystroke = commitOnEveryKeystroke,
            maxLength = maxLength,
            filter = filter,
            hint = hint,
            autoCorrect = autoCorrect,
            capitalization = capitalization,
            contentType = contentType,
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = textAlign,
                overflow = overflow,
                maxLines = if (singleLine) 1 else maxLines,
            )
        }

    /** Adds an editable [Text] component with a static [RcColorValue]. */
    public fun EditableText(
        text: RcText,
        color: RcColorValue,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        keyboardType: RcKeyboardType = RcKeyboardType.Text,
        imeAction: RcImeAction = RcImeAction.Default,
        singleLine: Boolean = true,
        commitOnEveryKeystroke: Boolean = true,
        maxLength: Int = 0,
        filter: RcInputFilter = RcInputFilter.None,
        hint: RcText? = null,
        autoCorrect: Boolean = false,
        capitalization: RcCapitalization = RcCapitalization.None,
        contentType: RcContentType = RcContentType.None,
    ): Unit =
        EditableText(
            text = text,
            modifier = modifier,
            color = color.color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
            keyboardType = keyboardType,
            imeAction = imeAction,
            singleLine = singleLine,
            commitOnEveryKeystroke = commitOnEveryKeystroke,
            maxLength = maxLength,
            filter = filter,
            hint = hint,
            autoCorrect = autoCorrect,
            capitalization = capitalization,
            contentType = contentType,
        )

    /** Adds an editable [Text] component with a 32-bit packed ARGB `Long` hex literal color. */
    public fun EditableText(
        text: RcText,
        color: Long,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        keyboardType: RcKeyboardType = RcKeyboardType.Text,
        imeAction: RcImeAction = RcImeAction.Default,
        singleLine: Boolean = true,
        commitOnEveryKeystroke: Boolean = true,
        maxLength: Int = 0,
        filter: RcInputFilter = RcInputFilter.None,
        hint: RcText? = null,
        autoCorrect: Boolean = false,
        capitalization: RcCapitalization = RcCapitalization.None,
        contentType: RcContentType = RcContentType.None,
    ): Unit =
        EditableText(
            text = text,
            modifier = modifier,
            color = color.toInt(),
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
            keyboardType = keyboardType,
            imeAction = imeAction,
            singleLine = singleLine,
            commitOnEveryKeystroke = commitOnEveryKeystroke,
            maxLength = maxLength,
            filter = filter,
            hint = hint,
            autoCorrect = autoCorrect,
            capitalization = capitalization,
            contentType = contentType,
        )

    /** Adds an Image component to the document. */
    public fun Image(
        image: RcImage,
        modifier: Modifier = Modifier,
        contentDescription: String? = null,
        contentScale: RcContentScale = RcContentScale.Fit,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f,
    )

    /** Adds an Icon component to the document. */
    public fun Icon(
        image: RcImage,
        modifier: Modifier = Modifier,
        contentDescription: String? = null,
    ) {
        // In RemoteCompose, an Icon is represented as an Image node.
        Image(
            image = image,
            modifier = modifier,
            contentDescription = contentDescription,
            contentScale = RcContentScale.Fit,
            alpha = 1f,
        )
    }

    /** Adds a Canvas drawing component to the document. */
    public fun Canvas(modifier: Modifier = Modifier, content: RcCanvasScope.() -> Unit)

    /**
     * Executes [block] with a typed [RcPaintScope] wrapping the current paint — uses typed value
     * classes (`RcColor`, `RcPaintStyle`, `RcStrokeCap`, `RcBlendMode`, ...) instead of raw `Int`
     * opcodes.
     */
    public fun paint(block: RcPaintScope.() -> Unit)

    /** Executes [block] within a global section. */
    public fun <T> global(block: RcScope.() -> T): T

    /** In the context of a draw modifier, draw the component content */
    public fun drawComponentContent()

    /**
     * Define a custom visibility animation function. The block receives the target `component` as
     * an [RcComponent] along with `progress`, `width`, `height`, `x`, `y` as [RcFloat] variables.
     * Call [drawComponentContent] or [drawComponentToBitmap] inside the block to render the
     * component content.
     *
     * @return an [RcVisibilityAnimation] handle that can be passed to [Modifier.animationSpec].
     */
    public fun defineVisibilityAnimation(
        block:
            RcCanvasScope.(
                component: RcComponent,
                progress: RcFloat,
                width: RcFloat,
                height: RcFloat,
                x: RcFloat,
                y: RcFloat,
            ) -> Unit
    ): RcVisibilityAnimation

    /**
     * Define a custom visibility animation function without requiring the component ID parameter.
     */
    public fun defineVisibilityAnimation(
        block:
            RcCanvasScope.(
                progress: RcFloat,
                width: RcFloat,
                height: RcFloat,
                x: RcFloat,
                y: RcFloat,
            ) -> Unit
    ): RcVisibilityAnimation = defineVisibilityAnimation { _, progress, width, height, x, y ->
        block(progress, width, height, x, y)
    }

    /** Registers a text resource and returns its reference. */
    public fun remoteText(text: String): RcText

    /** Registers a named text resource and returns its reference. */
    public fun remoteNamedText(name: String, text: String): RcText

    /** Registers a string list resource and returns its reference. */
    public fun remoteArrayOf(vararg strings: String): RcTextList

    /** Returns a remote text lookup from the specified data set and index. */
    public fun textLookup(dataSetId: RcTextList, index: RcFloat): RcText

    public operator fun RcTextList.get(index: RcFloat): RcText {
        return textLookup(this, index)
    }

    /** Returns a remote text lookup from the specified data set and index. */
    public fun textLookup(dataSetId: RcTextList, indexId: RcInteger): RcText

    /** Merges two remote text references. */
    public operator fun RcText.plus(v: RcText): RcText

    public infix fun String.join(v: RcText): RcText = remoteText(this) + v

    public operator fun RcText.plus(v: String): RcText = this + remoteText(v)

    /** Registers a color resource and returns its reference. */
    public fun remoteColor(@ColorInt color: Int): RcColor

    /** Registers a color resource from a 32-bit packed ARGB `Long` hex literal. */
    public fun remoteColor(color: Long): RcColor = remoteColor(color.toInt())

    /** Registers a static [RcColorValue] as a color resource and returns its reference. */
    public fun remoteColor(color: RcColorValue): RcColor = remoteColor(color.color)

    /** Wraps a packed ARGB int (`0xAARRGGBB`) as a static [RcColorValue]. */
    public fun remoteColorValue(@ColorInt color: Int): RcColorValue

    /** Wraps a 32-bit packed ARGB `Long` hex literal (`0xAARRGGBB`) as a static [RcColorValue]. */
    public fun remoteColorValue(color: Long): RcColorValue = RcColorValue(color)

    /** Registers a named color resource and returns its reference. */
    public fun remoteNamedColor(name: String, @ColorInt color: Int): RcColor

    /** Registers a named color resource from a 32-bit packed ARGB `Long` hex literal. */
    public fun remoteNamedColor(name: String, color: Long): RcColor =
        remoteNamedColor(name, color.toInt())

    /** Registers a named color resource from a static [RcColorValue] and returns its reference. */
    public fun remoteNamedColor(name: String, color: RcColorValue): RcColor =
        remoteNamedColor(name, color.color)

    /** Registers a themed color resource and returns its reference. */
    public fun remoteThemedColor(@ColorInt light: Int, @ColorInt dark: Int): RcColor

    /** Registers a themed color resource from 32-bit packed ARGB `Long` hex literals. */
    public fun remoteThemedColor(light: Long, dark: Long): RcColor =
        remoteThemedColor(light.toInt(), dark.toInt())

    /** Registers a themed color resource from static [RcColorValue]s and returns its reference. */
    public fun remoteThemedColor(light: RcColorValue, dark: RcColorValue): RcColor =
        remoteThemedColor(light.color, dark.color)

    /** Registers a themed color resource and returns its reference. */
    public fun remoteThemedColor(light: RcColor, dark: RcColor): RcColor

    /**
     * Registers a themed color resource with names and default values and returns its reference.
     */
    public fun remoteThemedColor(
        light: String,
        @ColorInt lightDefault: Int,
        dark: String,
        @ColorInt darkDefault: Int,
    ): RcColor

    /**
     * Registers a themed color resource with names and 32-bit packed ARGB `Long` default colors.
     */
    public fun remoteThemedColor(
        light: String,
        lightDefault: Long,
        dark: String,
        darkDefault: Long,
    ): RcColor = remoteThemedColor(light, lightDefault.toInt(), dark, darkDefault.toInt())

    /**
     * Registers a themed color resource with names and static [RcColorValue] default colors and
     * returns its reference.
     */
    public fun remoteThemedColor(
        light: String,
        lightDefault: RcColorValue,
        dark: String,
        darkDefault: RcColorValue,
    ): RcColor = remoteThemedColor(light, lightDefault.color, dark, darkDefault.color)

    /** Creates a color resource from an HSV expression. */
    public fun remoteColorExpression(
        @IntRange(from = 0, to = 255) alpha: Int,
        hue: RcFloat,
        @FloatRange(from = 0.0, to = 1.0) saturation: Float,
        @FloatRange(from = 0.0, to = 1.0) value: Float,
    ): RcColor

    /** Creates a color resource from an HSV expression. */
    public fun remoteColorExpression(
        @IntRange(from = 0, to = 255) alpha: Int,
        hue: Float,
        @FloatRange(from = 0.0, to = 1.0) saturation: Float,
        @FloatRange(from = 0.0, to = 1.0) value: Float,
    ): RcColor

    /** Creates a text resource from a float value with formatting. */
    public fun createTextFromFloat(
        value: Float,
        @IntRange(from = 0) whole: Int,
        @IntRange(from = 0) decimal: Int = 0,
        spec: RcTextFromFloatSpec = RcTextFromFloatSpec.Default,
    ): RcText

    /** Creates a text resource from a remote float with formatting. */
    public fun createTextFromFloat(
        value: RcFloat,
        @IntRange(from = 0) whole: Int,
        @IntRange(from = 0) decimal: Int = 0,
        spec: RcTextFromFloatSpec = RcTextFromFloatSpec.Default,
    ): RcText

    /** Registers a float array and returns its reference. */
    public fun remoteFloatArray(array: FloatArray): RcFloat

    /** Registers a named float array and returns its reference. */
    public fun remoteNamedFloatArray(name: String, array: FloatArray): RcFloat

    /** Sets the name of a remote float in the buffer. */
    public fun RcFloat.named(name: String): RcFloat

    /** Sets the name of a remote color in the buffer. */
    public fun RcColor.named(name: String): RcColor

    /** Sets the name of a remote text in the buffer. */
    public fun RcText.named(name: String): RcText

    /** Returns an RcFloat representing the current animation time. */
    public fun animationTime(): RcFloat

    /** Returns an RcFloat representing the last touch event time. */
    public fun touchTime(): RcFloat

    /** Returns an RcFloat representing the current day of the week. */
    public fun dayOfWeek(): RcFloat

    /** Returns an RcFloat representing the current day of the month. */
    public fun dayOfMonth(): RcFloat

    /** Returns an RcFloat representing the current hour. */
    public fun hour(): RcFloat

    /** Returns an RcFloat representing the current minutes. */
    public fun minutes(): RcFloat

    /** Returns an RcFloat representing the current seconds. */
    public fun seconds(): RcFloat

    /** Returns an RcFloat representing continuous seconds. */
    public fun continuousSeconds(): RcFloat

    /**
     * Adds a Cartesian parametric `(x(t), y(t))` path expression and returns its reference.
     *
     * @param expressionX the expression for the x-coordinate `x(t)`
     * @param expressionY the expression for the y-coordinate `y(t)`
     * @param start the start value of parameter `t`
     * @param end the end value of parameter `t`
     * @param count the number of sampled points along `t`
     * @param type the path interpolation type
     */
    public fun remoteXyPath(
        expressionX: RcFloat,
        expressionY: RcFloat,
        start: Float,
        end: Float,
        @IntRange(from = 1) count: Int,
        type: RcPathType = RcPathType.Spline,
    ): RcPath

    /**
     * Adds a polar path expression and returns its reference.
     *
     * @param expression the expression for the radius
     * @param start the start angle
     * @param end the end angle
     * @param count the number of points
     * @param centerX the center x coordinate
     * @param centerY the center y coordinate
     * @param type the path interpolation type
     */
    public fun remotePolarPath(
        expression: RcFloat,
        start: Float,
        end: Float,
        @IntRange(from = 1) count: Int,
        centerX: RcFloat,
        centerY: RcFloat,
        type: RcPathType = RcPathType.Spline,
    ): RcPath

    /**
     * Returns an [RcFloat] representing a function of a single variable.
     *
     * @param block the function block
     */
    public fun remoteFunction(block: (RcFloat) -> RcFloat): RcFloat

    public fun density(): RcFloat

    public fun fontSize(): RcFloat

    public fun apiLevel(): RcFloat

    /** Returns the window's width as a remote float. */
    public fun windowWidth(): RcFloat

    /** Returns the window's height as a remote float. */
    public fun windowHeight(): RcFloat

    /** ID_OFFSET_TO_UTC is the offset from UTC in sec (typically / 3600f) */
    public fun utcOffset(): RcFloat

    /** Month of year quantized to months 1–12 (`1` = January). */
    public fun month(): RcFloat

    // sensors
    /** Returns the current X accelerometer sensor value */
    public fun accelerometerX(): RcFloat

    /** Returns the current Y accelerometer sensor value */
    public fun accelerometerY(): RcFloat

    /** Returns the current Z accelerometer sensor value */
    public fun accelerometerZ(): RcFloat

    /** Returns the current X gyroscope sensor value */
    public fun gyroscopeX(): RcFloat

    /** Returns the current Y gyroscope sensor value */
    public fun gyroscopeY(): RcFloat

    /** Returns the current Z gyroscope sensor value */
    public fun gyroscopeZ(): RcFloat

    /** Returns the current X magnetometer sensor value */
    public fun magnetometerX(): RcFloat

    /** Returns the current Y magnetometer sensor value */
    public fun magnetometerY(): RcFloat

    /** Returns the current Z magnetometer sensor value */
    public fun magnetometerZ(): RcFloat

    /** Returns the current ambient light level */
    public fun ambientLightLevel(): RcFloat

    /** Returns the component's width as a remote float. */
    public fun componentWidth(): RcFloat

    /** Returns the component's height as a remote float. */
    public fun componentHeight(): RcFloat

    /** Registers an encoded bitmap resource (e.g. PNG/WebP bytes) and returns its reference. */
    public fun remoteBitmap(
        @IntRange(from = 1) width: Int,
        @IntRange(from = 1) height: Int,
        data: ByteArray,
    ): RcImage

    /**
     * Registers a named encoded bitmap resource (e.g. PNG/WebP bytes) and returns its reference.
     */
    public fun remoteNamedBitmap(
        name: String,
        @IntRange(from = 1) width: Int,
        @IntRange(from = 1) height: Int,
        data: ByteArray,
    ): RcImage

    /** Registers a bitmap URL resource and returns its reference. */
    public fun remoteBitmapUrl(url: String): RcImage

    /** Registers a named bitmap URL resource and returns its reference. */
    public fun remoteNamedBitmapUrl(name: String, url: String): RcImage

    /** Registers an embedded font resource (e.g. TTF/OTF bytes) and returns its reference. */
    public fun remoteFont(data: ByteArray): RcFont

    /** Registers a float variable and returns its reference. */
    public fun remoteFloat(value: Float): RcFloat

    /** Registers a named float variable and returns its reference. */
    public fun remoteNamedFloat(name: String, value: Float): RcFloat

    /** Registers an integer variable and returns its reference. */
    public fun remoteInteger(value: Int): RcInteger

    /** Registers a named integer variable and returns its reference. */
    public fun remoteNamedInteger(name: String, value: Int): RcInteger

    /** Registers a long constant and returns its reference. */
    public fun remoteLong(value: Long): RcInteger

    /** Registers a boolean constant and returns a typed [RcBool] reference. */
    public fun remoteBoolean(value: Boolean): RcBool

    /** Allocates a custom property return slot for a dynamic [RcFloat] output of [type]. */
    @ExperimentalRemoteCreationApi public fun customReturnFloat(type: Int): RcCustomProperty

    /** Allocates a custom property return slot for a dynamic [RcText] output of [type]. */
    @ExperimentalRemoteCreationApi public fun customReturnText(type: Int): RcCustomProperty

    /** Allocates a custom property return slot for a dynamic [RcInteger] output of [type]. */
    @ExperimentalRemoteCreationApi public fun customReturnInt(type: Int): RcCustomProperty

    /** Allocates a custom property return slot for a dynamic [RcColor] output of [type]. */
    @ExperimentalRemoteCreationApi public fun customReturnColor(type: Int): RcCustomProperty

    /** Registers a path data resource with the specified [fillType] and returns its reference. */
    public fun remotePathData(
        path: RcPathData,
        fillType: RcPathFillType = RcPathFillType.Winding,
    ): RcPath

    /**
     * Parses an Android Vector Drawable (AVD) `android:pathData` string, registers the resulting
     * path data with the specified [fillType], and returns its reference.
     */
    public fun remotePathData(
        avdPathData: String,
        fillType: RcPathFillType = RcPathFillType.Winding,
    ): RcPath = remotePathData(parseAvdPathData(avdPathData), fillType)

    /** Builds path data via [block], registers it with [fillType], and returns its reference. */
    public fun remotePathData(
        fillType: RcPathFillType = RcPathFillType.Winding,
        block: RcPathData.() -> Unit,
    ): RcPath = remotePathData(RcPathData().apply(block), fillType)

    /** Registers a reusable [RcTextStyle] resource for [Text] and returns its reference. */
    public fun remoteTextStyle(
        @ColorInt color: Int? = null,
        fontSize: RcSp? = null,
        fontStyle: RcFontStyle? = null,
        fontWeight: RcFontWeight? = null,
        fontFamily: String? = null,
        textAlign: RcTextAlign? = null,
        overflow: RcTextOverflow? = null,
        @IntRange(from = 1) maxLines: Int? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
        parent: RcTextStyle? = null,
    ): RcTextStyle

    /** Registers a reusable [RcTextStyle] with a static [RcColorValue] color. */
    public fun remoteTextStyle(
        color: RcColorValue,
        fontSize: RcSp? = null,
        fontStyle: RcFontStyle? = null,
        fontWeight: RcFontWeight? = null,
        fontFamily: String? = null,
        textAlign: RcTextAlign? = null,
        overflow: RcTextOverflow? = null,
        @IntRange(from = 1) maxLines: Int? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
        parent: RcTextStyle? = null,
    ): RcTextStyle =
        remoteTextStyle(
            color = color.color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
            underline = underline,
            strikethrough = strikethrough,
            parent = parent,
        )

    /** Registers a reusable [RcTextStyle] with a 32-bit packed ARGB `Long` hex literal color. */
    public fun remoteTextStyle(
        color: Long,
        fontSize: RcSp? = null,
        fontStyle: RcFontStyle? = null,
        fontWeight: RcFontWeight? = null,
        fontFamily: String? = null,
        textAlign: RcTextAlign? = null,
        overflow: RcTextOverflow? = null,
        @IntRange(from = 1) maxLines: Int? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
        parent: RcTextStyle? = null,
    ): RcTextStyle =
        remoteTextStyle(
            color = color.toInt(),
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
            underline = underline,
            strikethrough = strikethrough,
            parent = parent,
        )

    /** Registers a reusable [RcTextStyle] with a dynamic [RcColor] color reference. */
    public fun remoteTextStyle(
        color: RcColor,
        fontSize: RcSp? = null,
        fontStyle: RcFontStyle? = null,
        fontWeight: RcFontWeight? = null,
        fontFamily: String? = null,
        textAlign: RcTextAlign? = null,
        overflow: RcTextOverflow? = null,
        @IntRange(from = 1) maxLines: Int? = null,
        underline: Boolean? = null,
        strikethrough: Boolean? = null,
        parent: RcTextStyle? = null,
    ): RcTextStyle

    /** Registers a typed key-value [RcDataMap] populated by [block] and returns its reference. */
    public fun remoteDataMap(block: RcDataMapScope.() -> Unit): RcDataMap

    /** Looks up a string entry stored under [key] in this [RcDataMap]. */
    public fun RcDataMap.lookupText(key: String): RcText

    /** Looks up a string entry stored under dynamic [key] in this [RcDataMap]. */
    public fun RcDataMap.lookupText(key: RcText): RcText

    /** Looks up a float entry stored under [key] in this [RcDataMap]. */
    public fun RcDataMap.lookupFloat(key: String): RcFloat

    /** Looks up a float entry stored under dynamic [key] in this [RcDataMap]. */
    public fun RcDataMap.lookupFloat(key: RcText): RcFloat

    /** Looks up an integer or long entry stored under [key] in this [RcDataMap]. */
    public fun RcDataMap.lookupInt(key: String): RcInteger

    /** Looks up an integer or long entry stored under dynamic [key] in this [RcDataMap]. */
    public fun RcDataMap.lookupInt(key: RcText): RcInteger

    /** Looks up a boolean entry stored under [key] in this [RcDataMap]. */
    public fun RcDataMap.lookupBoolean(key: String): RcBool

    /** Looks up a boolean entry stored under dynamic [key] in this [RcDataMap]. */
    public fun RcDataMap.lookupBoolean(key: RcText): RcBool

    /** Registers a new bitmap resource with the given dimensions. */
    public fun createBitmap(
        @IntRange(from = 1) width: Int,
        @IntRange(from = 1) height: Int,
    ): RcImage

    /**
     * Reserves an offscreen bitmap handle whose backing bitmap is lazily acquired from the player's
     * reusable bitmap pool when drawn into via [drawComponentToBitmap] or [drawOnBitmap].
     */
    public fun createOffscreenBitmap(): RcImage

    /** Renders the [component] into the specified offscreen [image]. */
    public fun drawComponentToBitmap(component: RcComponent, image: RcImage)

    /** Renders the active component's content into the specified offscreen [image]. */
    public fun drawComponentToBitmap(image: RcImage)

    /** Redirects subsequent drawing operations to the specified bitmap resource. */
    public fun drawOnBitmap(
        image: RcImage,
        mode: RcDrawOnBitmapMode = RcDrawOnBitmapMode.Clear,
        color: RcColorValue = 0.rcColor(),
        block: RcCanvasScope.() -> Unit,
    )

    /**
     * Redirects subsequent drawing operations to [image], clearing with a 32-bit packed ARGB `Long`
     * hex literal [color].
     */
    public fun drawOnBitmap(
        image: RcImage,
        mode: RcDrawOnBitmapMode = RcDrawOnBitmapMode.Clear,
        color: Long,
        block: RcCanvasScope.() -> Unit,
    ): Unit = drawOnBitmap(image, mode, RcColorValue(color), block)

    /**
     * Registers a custom shader and configures its uniforms via a typed [RcShaderScope]. Uniform
     * setters are picked by value type (no reflection, no `Any`).
     */
    public fun shader(shaderString: String, block: RcShaderScope.() -> Unit = {}): RcShader

    /** Adds a spacer component. */
    public fun Spacer(modifier: Modifier = Modifier)

    /** Emits a player debug log [message] along with the current evaluated [value]. */
    public fun debug(message: String, value: RcFloat)

    /** Combines two paths using the specified operation. */
    public fun RcPath.combine(path2: RcPath, op: RcPathCombineOp): RcPath

    /** Trigger a haptic-feedback pulse on the player device. */
    public fun performHaptic(haptic: RcHaptic)

    /**
     * Registers embedded audio data (WAV or SC PCM format) as a reusable resource and returns an
     * [RcSound] handle to pass to [playSound].
     */
    @ExperimentalRemoteCreationApi public fun remoteSound(data: ByteArray): RcSound

    /**
     * Define a sound synthesis expression and register it as a resource. Returns an
     * [RcSoundExpression] ID to pass to [playSound].
     *
     * For [RcSoundType.Tone]: the tone is synthesized from [frequency] (Hz), [durationSeconds], and
     * [waveform] on the player device at load time.
     *
     * @param type synthesis type (currently only [RcSoundType.Tone])
     * @param frequency tone frequency in Hz ([RcSoundType.Tone] only)
     * @param durationSeconds tone duration in seconds ([RcSoundType.Tone] only)
     * @param waveform waveform shape ([RcSoundType.Tone] only)
     * @param leftVolume left-channel volume, 0.0–1.0
     * @param rightVolume right-channel volume, 0.0–1.0
     * @param rate playback rate, 1.0 = normal
     */
    @ExperimentalRemoteCreationApi
    public fun remoteSoundExpression(
        type: RcSoundType = RcSoundType.Tone,
        @FloatRange(from = 0.0, fromInclusive = false) frequency: Float = 440f,
        @FloatRange(from = 0.0, fromInclusive = false) durationSeconds: Float = 0.1f,
        waveform: RcWaveform = RcWaveform.Sine,
        @FloatRange(from = 0.0, to = 1.0) leftVolume: Float = 1f,
        @FloatRange(from = 0.0, to = 1.0) rightVolume: Float = 1f,
        @FloatRange(from = 0.0, fromInclusive = false) rate: Float = 1f,
    ): RcSoundExpression

    /** Triggers playback of a previously registered [RcSound] audio resource. */
    @ExperimentalRemoteCreationApi public fun playSound(sound: RcSound)

    /**
     * Trigger playback of a previously defined [RcSoundExpression]. Usable anywhere [performHaptic]
     * is usable (action handlers, `onClick`, etc.).
     *
     * @param expression the sound expression to play
     */
    @ExperimentalRemoteCreationApi public fun playSound(expression: RcSoundExpression)

    /** Tells the system to wake up in a given number of seconds. */
    public fun wakeIn(@FloatRange(from = 0.0) seconds: Float)

    /** Read a single attribute (HSV/RGBA component) of a color resource. */
    public fun colorAttribute(baseColor: RcColor, attr: RcColorAttr): RcFloat

    /** Returns a substring of the text. */
    public fun RcText.substring(start: RcFloat, length: RcFloat = (-1f).rf): RcText

    /** Returns a substring of the text. */
    public fun RcText.substring(start: Float, length: Float = -1f): RcText

    /** Measure bitmap font text dimensions. */
    public fun RcText.measure(
        font: RcBitmapFont,
        type: RcBitmapTextMeasure,
        glyphSpacing: Float = 0f,
    ): RcFloat = font.measure(this, type, glyphSpacing)

    /** Measure bitmap font text dimensions. */
    public fun RcBitmapFont.measure(
        text: RcText,
        type: RcBitmapTextMeasure,
        glyphSpacing: Float = 0f,
    ): RcFloat

    /** Registers a bitmap font with an optional kerning table and returns its reference. */
    public fun createBitmapFont(
        glyphs: List<RcBitmapFontGlyph>,
        kerningTable: Map<String, Int> = emptyMap(),
    ): RcBitmapFont

    /** Interpolates between two paths. */
    public fun RcPath.tween(path2: RcPath, tween: Float): RcPath

    /** Interpolates between two paths using a remote float. */
    public fun RcPath.tween(path2: RcPath, tween: RcFloat): RcPath

    /** Transforms the text. */
    public fun RcText.transform(
        start: RcFloat,
        length: RcFloat,
        operation: RcTextTransformOp,
    ): RcText

    /** Transforms the text. */
    public fun RcText.transform(start: Float, length: Float, operation: RcTextTransformOp): RcText

    /** Measure the text and return the requested dimension as an [RcFloat]. */
    public fun textMeasure(text: RcText, mode: RcTextMeasureMode = RcTextMeasureMode.Width): RcFloat

    /** Compute a derived float from a time-valued integer [variable]. */
    public fun timeAttribute(variable: RcInteger, attr: RcTimeAttr): RcFloat

    /**
     * Compute a derived float comparing a time-valued integer [variable] against [referenceTime]
     * (used with [RcTimeAttr.TimeFromArgSec], [RcTimeAttr.TimeFromArgMin], and
     * [RcTimeAttr.TimeFromArgHr]).
     */
    public fun timeAttribute(
        variable: RcInteger,
        attr: RcTimeAttr,
        referenceTime: RcInteger,
    ): RcFloat

    /**
     * Adds a parametric 2D mesh whose vertices come from expressions over `(u, v)`, and returns a
     * handle to it.
     *
     * Grid topology is implicit - there is no index array - which is most of the size win, and the
     * animation is free: a waving flag costs what a flat one costs.
     *
     * ```
     * val flag = remoteMesh2D(RcMeshLayout.Grid, uCount = 24, vCount = 16) {
     *     x = u * 300f
     *     y = v * 200f + sin(u * 6f + continuousSeconds()) * 18f
     * }
     * drawMesh2D(flag, image = banner)
     * ```
     *
     * @param layout the domain topology, which also decides the default geometry
     * @param uCount the resolution along u, required so the runtime cost is visible where authored
     * @param vCount the resolution along v
     * @param path the path a [RcMeshLayout.PathStrip] follows; ignored by every other layout
     * @param block sets the channel expressions; see [RcMesh2DScope]
     */
    @ExperimentalRemoteCreationApi
    public fun remoteMesh2D(
        layout: RcMeshLayout,
        @IntRange(from = 1) uCount: Int,
        @IntRange(from = 1) vCount: Int,
        path: RcPath? = null,
        block: RcMesh2DScope.() -> Unit,
    ): RcMesh

    /**
     * Adds a 2D mesh from explicit geometry - the escape hatch for tool generated meshes, and the
     * closest thing to Android's `drawVertices`.
     *
     * @param verts x, y pairs
     * @param indices the triangle list, or null to draw the vertices in order
     * @param uv u, v pairs in 0..1, or null
     * @param colors packed ARGB per vertex, or null
     * @param halfFloat write positions and uv as IEEE half floats, halving the wire size. Exact for
     *   integers up to 2048, so a surface wider than about 4096 is where it stops being free.
     * @param layout the domain topology, used when sampling the surface for a matrix
     * @param uCount the resolution along u if the geometry describes a grid, otherwise 0
     * @param vCount the resolution along v if the geometry describes a grid, otherwise 0
     */
    @ExperimentalRemoteCreationApi
    public fun remoteMesh2DValues(
        verts: FloatArray,
        indices: IntArray? = null,
        uv: FloatArray? = null,
        @ColorInt colors: IntArray? = null,
        halfFloat: Boolean = false,
        layout: RcMeshLayout = RcMeshLayout.Grid,
        @IntRange(from = 0) uCount: Int = 0,
        @IntRange(from = 0) vCount: Int = 0,
    ): RcMesh

    /**
     * Adds a 2D mesh from explicit vertex geometry and static [RcColorValue] vertex colors.
     *
     * @param verts x, y pairs
     * @param colors static ARGB color per vertex
     * @param indices the triangle list, or null to draw the vertices in order
     * @param uv u, v pairs in 0..1, or null
     * @param halfFloat write positions and uv as IEEE half floats, halving the wire size
     * @param layout the domain topology, used when sampling the surface for a matrix
     * @param uCount the resolution along u if the geometry describes a grid, otherwise 0
     * @param vCount the resolution along v if the geometry describes a grid, otherwise 0
     */
    @ExperimentalRemoteCreationApi
    public fun remoteMesh2DValues(
        verts: FloatArray,
        colors: List<RcColorValue>,
        indices: IntArray? = null,
        uv: FloatArray? = null,
        halfFloat: Boolean = false,
        layout: RcMeshLayout = RcMeshLayout.Grid,
        @IntRange(from = 0) uCount: Int = 0,
        @IntRange(from = 0) vCount: Int = 0,
    ): RcMesh =
        remoteMesh2DValues(
            verts = verts,
            indices = indices,
            uv = uv,
            colors = IntArray(colors.size) { colors[it].color },
            halfFloat = halfFloat,
            layout = layout,
            uCount = uCount,
            vCount = vCount,
        )

    /**
     * Adds a parametric 2D mesh whose edges fade out over [antialiasWidth] instead of stopping at a
     * hard, stepped edge.
     *
     * Meshes are drawn with `drawVertices`, which does not antialias, so a mesh's outline aliases
     * visibly. This grows a thin skirt around the outline whose vertices repeat their neighbors'
     * color at alpha 0, so the edge fades out. The fade is made from the vertex colors, which is
     * why the block must end with [RcMesh2DAntialiasScope.color]:
     * ```
     * val trail = remoteMesh2DAntialias(
     *     RcMeshLayout.PathStrip, uCount = 96, vCount = 2, antialiasWidth = 1f.rf, path = orbit,
     * ) {
     *     width = w * 0.005f + u * w * 0.065f
     *     color(red = 0.55f + u * 0.45f, green = 0.25f + u * 0.62f, blue = 1f.rf, alpha = u * u)
     * }
     * ```
     *
     * The skirt only grows outward, so an edge looks about half a skirt heavier than without it.
     *
     * @param layout the domain topology, which also decides the default geometry
     * @param uCount the resolution along u
     * @param vCount the resolution along v
     * @param antialiasWidth how far the fade reaches beyond the edge, in the mesh's own units.
     *   About one pixel: `1f.rf` for a mesh authored in pixels, `1f.rf / w` for a unit mesh drawn
     *   under `scale(w, h)`
     * @param path the path a [RcMeshLayout.PathStrip] follows; ignored by every other layout
     * @param block sets the channel expressions and returns the color; see [RcMesh2DAntialiasScope]
     */
    @ExperimentalRemoteCreationApi
    public fun remoteMesh2DAntialias(
        layout: RcMeshLayout,
        @IntRange(from = 1) uCount: Int,
        @IntRange(from = 1) vCount: Int,
        antialiasWidth: RcFloat,
        path: RcPath? = null,
        block: RcMesh2DAntialiasScope.() -> RcMeshColor,
    ): RcMesh

    /**
     * Adds a 2D mesh from explicit geometry whose edges fade out over [antialiasWidth].
     *
     * As [remoteMesh2DValues], plus the skirt described at [remoteMesh2DAntialias]. The skirt is
     * grown from the outline the layout describes and fades the vertex colors, so both are
     * required: one color per vertex, and a [layout], [uCount] and [vCount] that account for every
     * vertex, in the order the layout numbers them.
     *
     * @param verts x, y pairs
     * @param colors packed ARGB per vertex
     * @param layout the domain topology the vertices are laid out in
     * @param uCount the resolution along u
     * @param vCount the resolution along v
     * @param antialiasWidth how far the fade reaches beyond the edge, in the mesh's own units
     * @param indices the triangle list, or null for the layout's own triangulation
     * @param uv u, v pairs in 0..1, or null
     * @param halfFloat write positions and uv as IEEE half floats, halving the wire size
     */
    @ExperimentalRemoteCreationApi
    public fun remoteMesh2DValuesAntialias(
        verts: FloatArray,
        @ColorInt colors: IntArray,
        layout: RcMeshLayout,
        @IntRange(from = 1) uCount: Int,
        @IntRange(from = 1) vCount: Int,
        antialiasWidth: RcFloat,
        indices: IntArray? = null,
        uv: FloatArray? = null,
        halfFloat: Boolean = false,
    ): RcMesh

    /**
     * Adds a 2D mesh from explicit geometry and static [RcColorValue] vertex colors whose edges
     * fade out over [antialiasWidth].
     *
     * @param verts x, y pairs
     * @param colors static ARGB color per vertex
     * @param layout the domain topology the vertices are laid out in
     * @param uCount the resolution along u
     * @param vCount the resolution along v
     * @param antialiasWidth how far the fade reaches beyond the edge, in the mesh's own units
     * @param indices the triangle list, or null for the layout's own triangulation
     * @param uv u, v pairs in 0..1, or null
     * @param halfFloat write positions and uv as IEEE half floats, halving the wire size
     */
    @ExperimentalRemoteCreationApi
    public fun remoteMesh2DValuesAntialias(
        verts: FloatArray,
        colors: List<RcColorValue>,
        layout: RcMeshLayout,
        @IntRange(from = 1) uCount: Int,
        @IntRange(from = 1) vCount: Int,
        antialiasWidth: RcFloat,
        indices: IntArray? = null,
        uv: FloatArray? = null,
        halfFloat: Boolean = false,
    ): RcMesh =
        remoteMesh2DValuesAntialias(
            verts = verts,
            colors = IntArray(colors.size) { colors[it].color },
            layout = layout,
            uCount = uCount,
            vCount = vCount,
            antialiasWidth = antialiasWidth,
            indices = indices,
            uv = uv,
            halfFloat = halfFloat,
        )

    /**
     * Adds a ribbon that follows a path, its cross width a monotonic spline through [widths].
     *
     * The variable width stroke, without spending expression tokens on it. One width is a constant
     * width; two or more are interpolated along the path, so a stroke that swells in the middle and
     * tapers at both ends is three numbers.
     *
     * ```
     * val brush = remoteMesh2DPathStrip(stroke, segments = 48, widths = floatArrayOf(0f, 26f, 4f))
     * drawMesh2D(brush)
     * ```
     *
     * Geometry only: uv is the identity mapping and there are no vertex colors, so color comes from
     * the paint, or from passing an image to drawMesh2D.
     *
     * @param path the path to follow
     * @param segments roughly how many quads to divide the path into; at least 1
     * @param widths the width control points, at least one, in the path's own units
     * @param positions where each width sits along the path as a fraction of arclength, 0..1. Null
     *   spreads the widths evenly, so the first is the width at the start of the path and the last
     *   the width at the end. Otherwise there must be one entry per width, increasing.
     */
    @ExperimentalRemoteCreationApi
    public fun remoteMesh2DPathStrip(
        path: RcPath,
        @IntRange(from = 1) segments: Int,
        widths: FloatArray,
        positions: FloatArray? = null,
    ): RcMesh

    /**
     * Adds a ribbon that follows a path, its cross width a monotonic spline through [widths], where
     * the widths are remote floats and so can animate.
     *
     * The spline is refitted whenever one of them changes, so the profile can be driven by time, a
     * gesture or incoming data - a stroke that thickens under the finger, a trail that fades to
     * nothing behind a moving head.
     *
     * @param path the path to follow
     * @param segments roughly how many quads to divide the path into; at least 1
     * @param widths the width control points, at least one, in the path's own units
     * @param positions where each width sits along the path as a fraction of arclength, 0..1, or
     *   null to spread the widths evenly. Otherwise there must be one entry per width.
     */
    @ExperimentalRemoteCreationApi
    public fun remoteMesh2DPathStrip(
        path: RcPath,
        @IntRange(from = 1) segments: Int,
        widths: List<RcFloat>,
        positions: List<RcFloat>? = null,
    ): RcMesh

    /**
     * Adds a spline width ribbon that rounds off at both ends.
     *
     * [remoteMesh2DPathStrip] stops dead at each end of the path, leaving a square edge that reads
     * as unfinished on anything meant to look drawn. This closes each end with a semicircle of
     * radius half the ribbon's width there - the mesh equivalent of a round stroke cap. A profile
     * that tapers to zero therefore comes to a point rather than a blunt stub.
     *
     * ```
     * val stroke = remoteMesh2DRoundStrip(ink, segments = 48, widths = floatArrayOf(4f, 26f, 4f))
     * drawMesh2D(stroke)
     * ```
     *
     * [segments] still counts only the columns spanning the path; the caps are added on top, so
     * switching between the two does not change how closely the ribbon tracks its path.
     *
     * @param path the path to follow
     * @param segments roughly how many quads to divide the path into, excluding the caps
     * @param widths the width control points, at least one, in the path's own units
     * @param positions where each width sits along the path as a fraction of arclength, 0..1. Null
     *   spreads the widths evenly, so the first is the width at the start of the path and the last
     *   the width at the end. Otherwise there must be one entry per width, increasing.
     */
    @ExperimentalRemoteCreationApi
    public fun remoteMesh2DRoundStrip(
        path: RcPath,
        @IntRange(from = 1) segments: Int,
        widths: FloatArray,
        positions: FloatArray? = null,
    ): RcMesh

    /**
     * Adds a spline width ribbon that rounds off at both ends, with animatable [widths].
     *
     * As [remoteMesh2DRoundStrip], but the control points are remote floats, so the profile - and
     * with it the radius of each cap - can be driven by time, a gesture or incoming data.
     *
     * @param path the path to follow
     * @param segments roughly how many quads to divide the path into, excluding the caps
     * @param widths the width control points, at least one, in the path's own units
     * @param positions where each width sits along the path as a fraction of arclength, 0..1, or
     *   null to spread the widths evenly. Otherwise there must be one entry per width.
     */
    @ExperimentalRemoteCreationApi
    public fun remoteMesh2DRoundStrip(
        path: RcPath,
        @IntRange(from = 1) segments: Int,
        widths: List<RcFloat>,
        positions: List<RcFloat>? = null,
    ): RcMesh

    /** Conditionally executes [block] on the player when [condition] evaluates to `true`. */
    public fun <S : RcScope> S.ifTrue(condition: RcCondition, block: S.() -> Unit)

    /** Returns the length of this RcText as an RcFloat. */
    public val RcText.length: RcFloat

    /** Defines a reusable remote Macro template. */
    @ExperimentalRemoteCreationApi
    public fun defineMacro(
        name: String,
        parameters: List<String>,
        content: RcScope.(Map<String, RcMacroArg>) -> Unit,
    ): RcMacro

    /** Inflates an existing [RcMacro] with arguments configured via [block]. */
    @ExperimentalRemoteCreationApi
    public fun RcMacro.inflate(block: RcMacroArgsScope.() -> Unit = {})

    /** Inserts a macro parameter argument block placeholder at this position. */
    @ExperimentalRemoteCreationApi public fun RcMacroArg.insertArgument()

    /** Extension property to convert an Int to a RcFloat within this scope. */
    public val Int.rf: RcFloat

    /** Extension property to convert a Float to a RcFloat within this scope. */
    public val Float.rf: RcFloat

    /** Extension property to register an Int constant as a RcInteger within this scope. */
    public val Int.ri: RcInteger

    /** Extension property to register a Boolean constant as a typed RcBool within this scope. */
    public val Boolean.rb: RcBool

    /** Registers a particle system definition and returns its handle. */
    public fun createParticles(
        variables: FloatArray,
        initialExpressions: List<RcFloat>,
        @IntRange(from = 1) particleCount: Int,
    ): RcFloat

    /** Steps and renders each particle in [id] on every frame. */
    public fun particlesLoop(
        id: RcFloat,
        restart: RcFloat?,
        expressions: List<RcFloat>,
        block: RcCanvasScope.() -> Unit,
    )

    /**
     * Adds a particle loop whose update equations may be scalar ([RcFloat]) or vector ([RcVec]).
     *
     * A vector entry of dimensionality d writes its components to d consecutive particle variable
     * slots, so [entries] must follow the particle variable layout and their dimensionalities must
     * add up to the particle variable count.
     *
     * @param id the particle system, from [createParticles]
     * @param restart an expression that recycles a particle when it is negative, or null
     * @param entries one update per particle variable slot, scalar or vector, in slot order
     * @param block the drawing done once per particle
     */
    public fun particlesLoopVec(
        id: RcFloat,
        restart: RcFloat?,
        entries: List<RcParticleValue>,
        block: RcCanvasScope.() -> Unit,
    )

    /**
     * Compares pairs of particles in `[min, max)` and applies [then1] / [then2] updates when
     * [condition] evaluates greater than zero.
     */
    public fun particlesComparison(
        id: RcFloat,
        min: RcFloat,
        max: RcFloat,
        condition: (RcParticleComparisonScope.() -> RcFloat)?,
        then1: (RcParticleComparisonScope.() -> List<RcFloat>)?,
        then2: (RcParticleComparisonScope.() -> List<RcFloat>)?,
        flags: RcParticleComparisonFlags = RcParticleComparisonFlags.None,
        block: RcCanvasScope.() -> Unit,
    )

    /**
     * Adds a two-body particle comparison whose updates may be scalar ([RcFloat]) or vector
     * ([RcVec]). The vector form of [particlesComparison]: a vector entry of dimensionality d
     * writes d consecutive particle variable slots; [condition] is always scalar.
     *
     * @param id the particle system, from [createParticles]
     * @param min the lower bound of the comparison range
     * @param max the upper bound of the comparison range
     * @param condition the pairwise test, positive when the pair matches, or null
     * @param thenFirstParticle the updates of the first particle of a matching pair, in slot order,
     *   or null
     * @param thenSecondParticle the updates of the second particle of a matching pair, in slot
     *   order, or null
     * @param flags comparison behavior flags
     * @param block the drawing done per matching pair
     */
    public fun particlesComparisonVec(
        id: RcFloat,
        min: RcFloat,
        max: RcFloat,
        condition: (RcParticleComparisonScope.() -> RcFloat)?,
        thenFirstParticle: (RcParticleComparisonScope.() -> List<RcParticleValue>)?,
        thenSecondParticle: (RcParticleComparisonScope.() -> List<RcParticleValue>)?,
        flags: RcParticleComparisonFlags = RcParticleComparisonFlags.None,
        block: RcCanvasScope.() -> Unit,
    )

    /** Add a particle test */
    public fun particlesComparison(
        id: RcFloat,
        min: RcFloat,
        max: RcFloat,
        condition: RcFloat?,
        then: List<RcFloat>?,
        flags: RcParticleComparisonFlags = RcParticleComparisonFlags.None,
        block: RcCanvasScope.() -> Unit,
    )

    /**
     * Add an impulse container contents execute once when conditions are met except for
     * impulseProcess
     */
    public fun impulse(duration: RcFloat, start: RcFloat, block: RcImpulseScope.() -> Unit)

    /** Add an impulse process container contents execute for the duration of the impulse */
    public fun impulseProcess(block: RcImpulseScope.() -> Unit)

    public fun runAction(block: RcRunActionScope.() -> Unit)

    /**
     * Open a 3D scene block. Operations issued in [block] are the 3D operations of
     * [RcScene3DScope], and they composite onto the canvas after the block returns.
     *
     * Declared on [RcScope] (not only [RcCanvasScope]) — like the 2D draw ops — so 3D can also be
     * emitted inside non-canvas containers such as a [particlesLoop]/[impulse] body, enabling **3D
     * particle systems** (a `drawMesh` per particle).
     */
    @ExperimentalRemoteCreationApi public fun scene3D(block: RcScene3DScope.() -> Unit)

    /** Skip [block] when the player matches [kind] against [value]. */
    public fun <S : RcScope> S.skip(kind: RcSkipKind, value: Int, block: S.() -> Unit)

    /** Loops from [start] to [end] with a specified [step]. */
    public fun <S : RcScope> S.loop(
        start: RcFloat,
        step: Float,
        end: RcFloat,
        block: S.(RcFloat) -> Unit,
    )

    /** Loops from [start] to [end] with a specified [step]. */
    public fun <S : RcScope> S.loop(
        start: RcFloat,
        step: RcFloat,
        end: RcFloat,
        block: S.(RcFloat) -> Unit,
    )

    /** Returns an RcFloat representing the animation delta time. */
    public fun deltaTime(): RcFloat

    /** Creates a new path starting at the specified position. */
    public fun remotePath(x: Float, y: Float): RcDynamicPath

    /**
     * The particle index (`0..count-1`) inside a [createParticles] initializer expression — the
     * per-particle `VAR1` marker the engine substitutes per instance (e.g. to spread particles).
     */
    public fun particleIndex(): RcFloat

    public fun RcDynamicPath.lineTo(x: Float, y: Float)

    public fun RcDynamicPath.moveTo(x: Float, y: Float)

    public fun RcDynamicPath.quadTo(x1: Float, y1: Float, x2: Float, y2: Float)

    public fun RcDynamicPath.cubicTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
    )

    public fun RcDynamicPath.arcTo(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean = false,
    )

    public fun RcDynamicPath.arcTo(
        x0: Float,
        y0: Float,
        rx: Float,
        ry: Float,
        angle: Float,
        largeArc: Boolean,
        sweep: Boolean,
        x1: Float,
        y1: Float,
    )

    public fun RcDynamicPath.arcTo(
        rx: Float,
        ry: Float,
        angle: Float,
        largeArc: Boolean,
        sweep: Boolean,
        x: Float,
        y: Float,
    )

    /** Returns the [RcPath] handle for this dynamic path. */
    public fun RcDynamicPath.toPath(): RcPath

    public fun RcDynamicPath.close()

    public fun RcDynamicPath.reset()

    /** Registers an integer array and returns its reference. */
    public fun remoteIntArray(array: IntArray): RcFloat

    /** Registers a dynamic float array with the given size and returns its reference. */
    public fun remoteDynamicFloatArray(size: Float): RcFloat

    /** Registers a float list and returns its reference. */
    public fun remoteFloatList(values: FloatArray): RcFloat

    /** Registers a float map and returns its reference. */
    public fun remoteFloatMap(keys: List<String>, values: FloatArray): RcFloat

    /** Sets a value in the array at the given index. */
    public fun setArrayValue(array: RcFloat, index: RcFloat, value: RcFloat)

    /** Adds a Text component with a dynamic [RcColor]. */
    public fun Text(
        text: String,
        color: RcColor,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontStyle: RcFontStyle = RcFontStyle.Normal,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        fontFamily: String? = null,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        underline: Boolean = false,
        strikethrough: Boolean = false,
        style: RcTextStyle? = null,
    )

    /** Adds a Text component using a remote string reference and a dynamic [RcColor]. */
    public fun Text(
        text: RcText,
        color: RcColor,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontStyle: RcFontStyle = RcFontStyle.Normal,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        fontFamily: String? = null,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        underline: Boolean = false,
        strikethrough: Boolean = false,
        style: RcTextStyle? = null,
    )

    /** Adds a Text component with a static [RcColorValue]. */
    public fun Text(
        text: String,
        color: RcColorValue,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontStyle: RcFontStyle = RcFontStyle.Normal,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        fontFamily: String? = null,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        underline: Boolean = false,
        strikethrough: Boolean = false,
        style: RcTextStyle? = null,
    ): Unit =
        Text(
            text = text,
            modifier = modifier,
            color = color.color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
            underline = underline,
            strikethrough = strikethrough,
            style = style,
        )

    /** Adds a Text component using a remote string reference and a static [RcColorValue]. */
    public fun Text(
        text: RcText,
        color: RcColorValue,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontStyle: RcFontStyle = RcFontStyle.Normal,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        fontFamily: String? = null,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        underline: Boolean = false,
        strikethrough: Boolean = false,
        style: RcTextStyle? = null,
    ): Unit =
        Text(
            text = text,
            modifier = modifier,
            color = color.color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
            underline = underline,
            strikethrough = strikethrough,
            style = style,
        )

    /** Adds a Text component with a 32-bit packed ARGB `Long` hex literal color. */
    public fun Text(
        text: String,
        color: Long,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontStyle: RcFontStyle = RcFontStyle.Normal,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        fontFamily: String? = null,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        underline: Boolean = false,
        strikethrough: Boolean = false,
        style: RcTextStyle? = null,
    ): Unit =
        Text(
            text = text,
            modifier = modifier,
            color = color.toInt(),
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
            underline = underline,
            strikethrough = strikethrough,
            style = style,
        )

    /**
     * Adds a Text component using a remote string reference and a 32-bit packed ARGB `Long` hex
     * literal color.
     */
    public fun Text(
        text: RcText,
        color: Long,
        modifier: Modifier = Modifier,
        fontSize: RcSp = 16.rsp,
        fontStyle: RcFontStyle = RcFontStyle.Normal,
        fontWeight: RcFontWeight = RcFontWeight.Normal,
        fontFamily: String? = null,
        textAlign: RcTextAlign = RcTextAlign.Start,
        overflow: RcTextOverflow = RcTextOverflow.Clip,
        @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
        underline: Boolean = false,
        strikethrough: Boolean = false,
        style: RcTextStyle? = null,
    ): Unit =
        Text(
            text = text,
            modifier = modifier,
            color = color.toInt(),
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
            underline = underline,
            strikethrough = strikethrough,
            style = style,
        )

    /** Wraps each allocated particle register in [variables] as an [RcFloat] handle. */
    public fun rcFloats(variables: FloatArray): List<RcFloat> {
        return List(variables.size) { RcFloat(variables[it]) }
    }

    /** Content Width */
    public fun componentContentWidth(): RcFloat

    /** Content Height */
    public fun componentContentHeight(): RcFloat

    /** X */
    public fun componentX(): RcFloat

    /** Y */
    public fun componentY(): RcFloat

    /** ROOT X */
    public fun componentRootX(): RcFloat

    /** ROOT Y */
    public fun componentRootY(): RcFloat

    /** generate random number */
    public fun rand(): RcFloat

    /** the index variable in the particle system */
    public fun index(): RcFloat
}

/** Impulse container scope. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcImpulseScope : RcCanvasScope {
    /** Add an impulse process container */
    public fun process(block: RcCanvasScope.() -> Unit)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcBoxScope : RcScope {
    /** Matches child size to the parent Box. */
    public fun Modifier.matchParentSize(): Modifier = fillMaxSize()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcColumnScope : RcScope {
    /** Sets the vertical weight of the component within a Column. */
    public fun Modifier.weight(weight: Float): Modifier
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcRowScope : RcScope {
    /** Sets the horizontal weight of the component within a Row. */
    public fun Modifier.weight(weight: Float): Modifier
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcFlowScope : RcScope {
    /** Sets the horizontal weight of the component within a Row. */
    public fun Modifier.weight(weight: Float): Modifier
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcCollapsibleColumnScope : RcScope {
    /** Sets the vertical weight of the component within a CollapsibleColumn. */
    public fun Modifier.weight(weight: Float): Modifier
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcCollapsibleRowScope : RcScope {
    /** Sets the horizontal weight of the component within a CollapsibleRow. */
    public fun Modifier.weight(weight: Float): Modifier
}

/** Procedural 2D drawing scope inside [RcScope.Canvas] or [createRawRcBuffer]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcCanvasScope : RcScope {
    /** Returns the width of the canvas as a remote float. */
    public val width: RcFloat
    /** Returns the height of the canvas as a remote float. */
    public val height: RcFloat

    /** Saves the current canvas state. */
    public fun save()

    /** Restores the previous canvas state. */
    public fun restore()

    /** Executes a block within a save/restore pair. */
    public fun save(block: RcCanvasScope.() -> Unit)

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: Float, scaleY: Float)

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float)

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: RcFloat, scaleY: RcFloat)

    /** Preconcat the current matrix with the specified scale. */
    public fun scale(scaleX: RcFloat, scaleY: RcFloat, centerX: RcFloat, centerY: RcFloat)

    /** Pre-concat the current matrix with the specified skew. */
    public fun skew(skewX: Float, skewY: Float)

    /** Sets the matrix relative to [path] according to [flags]. */
    public fun matrixFromPath(
        path: RcPath,
        fraction: Float,
        vOffset: Float,
        flags: RcMatrixFromPathFlags = RcMatrixFromPathFlags.None,
    )

    /** Sets the matrix relative to [path] according to [flags] using remote floats. */
    public fun matrixFromPath(
        path: RcPath,
        fraction: RcFloat,
        vOffset: RcFloat,
        flags: RcMatrixFromPathFlags = RcMatrixFromPathFlags.None,
    )

    /** Draws a rectangle. */
    public fun drawRect(left: Float, top: Float, right: Float, bottom: Float)

    /** Draws a rectangle using remote floats. */
    public fun drawRect(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat)

    /** drawRect taking a typed RcRect. */
    public fun drawRect(rect: RcRect): Unit = drawRect(rect.left, rect.top, rect.right, rect.bottom)

    /** Draws a circle. */
    public fun drawCircle(centerX: Float, centerY: Float, radius: Float)

    /** Draws a circle using remote floats. */
    public fun drawCircle(centerX: RcFloat, centerY: RcFloat, radius: RcFloat)

    /** drawCircle taking a typed RcPoint center. */
    public fun drawCircle(center: RcPoint, radius: Float): Unit =
        drawCircle(center.x, center.y, RcFloat(radius))

    /** Draws a line. */
    public fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float)

    /** Draws a line using remote floats. */
    public fun drawLine(x1: RcFloat, y1: RcFloat, x2: RcFloat, y2: RcFloat)

    /** drawLine taking typed RcPoint endpoints. */
    public fun drawLine(from: RcPoint, to: RcPoint): Unit = drawLine(from.x, from.y, to.x, to.y)

    /** Draws an arc. */
    public fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    )

    /** Draws an arc using remote floats. */
    public fun drawArc(
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
        startAngle: RcFloat,
        sweepAngle: RcFloat,
    )

    /** drawArc taking a typed RcRect. */
    public fun drawArc(rect: RcRect, startAngle: Float, sweepAngle: Float): Unit =
        drawArc(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            RcFloat(startAngle),
            RcFloat(sweepAngle),
        )

    /** Draws a sector. */
    public fun drawSector(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    )

    /** Draws a sector using remote floats. */
    public fun drawSector(
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
        startAngle: RcFloat,
        sweepAngle: RcFloat,
    )

    /** drawSector taking a typed RcRect. */
    public fun drawSector(rect: RcRect, startAngle: Float, sweepAngle: Float): Unit =
        drawSector(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            RcFloat(startAngle),
            RcFloat(sweepAngle),
        )

    /** Draws an oval. */
    public fun drawOval(left: Float, top: Float, right: Float, bottom: Float)

    /** Draws an oval using remote floats. */
    public fun drawOval(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat)

    /** drawOval taking a typed RcRect. */
    public fun drawOval(rect: RcRect): Unit = drawOval(rect.left, rect.top, rect.right, rect.bottom)

    /** Draws a rounded rectangle. */
    public fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
    )

    /** Draws a rounded rectangle using remote floats. */
    public fun drawRoundRect(
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
        radiusX: RcFloat,
        radiusY: RcFloat,
    )

    /** drawRoundRect taking a typed RcRect. */
    public fun drawRoundRect(rect: RcRect, radiusX: Float, radiusY: Float): Unit =
        drawRoundRect(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            RcFloat(radiusX),
            RcFloat(radiusY),
        )

    /** Draws a path. */
    public fun drawPath(path: RcPath)

    /** Draws a path that interpolates between two other paths. */
    public fun drawTweenPath(path1: RcPath, path2: RcPath, tween: Float, start: Float, stop: Float)

    /** Draws a path that interpolates between two other paths using remote floats. */
    public fun drawTweenPath(
        path1: RcPath,
        path2: RcPath,
        tween: RcFloat,
        start: RcFloat,
        stop: RcFloat,
    )

    /** Draws text with specified anchor/pan and optional [flags]. */
    public fun drawTextAnchored(
        text: RcText,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        flags: RcTextAnchorFlags = RcTextAnchorFlags.None,
    )

    /** Draws text with specified anchor/pan and optional [flags] using remote floats. */
    public fun drawTextAnchored(
        text: RcText,
        x: RcFloat,
        y: RcFloat,
        panX: RcFloat,
        panY: RcFloat,
        flags: RcTextAnchorFlags = RcTextAnchorFlags.None,
    )

    /** Draws a text along a path. */
    public fun drawTextOnPath(text: RcText, path: RcPath, hOffset: Float, vOffset: Float)

    /** Draws a text along a path using remote floats. */
    public fun drawTextOnPath(text: RcText, path: RcPath, hOffset: RcFloat, vOffset: RcFloat)

    /** Draws a text on a circle. */
    public fun drawTextOnCircle(
        text: RcText,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        warpRadiusOffset: Float = 0f,
        alignment: RcCircleTextAlignment = RcCircleTextAlignment.Start,
        placement: RcCircleTextPlacement = RcCircleTextPlacement.Outside,
    )

    /** Draws a text on a circle using remote floats. */
    public fun drawTextOnCircle(
        text: RcText,
        centerX: RcFloat,
        centerY: RcFloat,
        radius: RcFloat,
        startAngle: RcFloat,
        warpRadiusOffset: RcFloat = RcFloat(0f),
        alignment: RcCircleTextAlignment = RcCircleTextAlignment.Start,
        placement: RcCircleTextPlacement = RcCircleTextPlacement.Outside,
    )

    /** Draws a text run using a bitmap font. */
    public fun drawText(
        text: RcText,
        font: RcBitmapFont,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        glyphSpacing: Float,
    )

    /** Draws a text run using a bitmap font. */
    public fun drawText(
        text: RcText,
        font: RcBitmapFont,
        start: Int,
        end: Int,
        x: RcFloat,
        y: RcFloat,
        glyphSpacing: Float,
    )

    /** Draws a text run along a path using a bitmap font. */
    public fun drawTextOnPath(
        text: RcText,
        font: RcBitmapFont,
        path: RcPath,
        start: Int,
        end: Int,
        yOffset: Float,
        glyphSpacing: Float,
    )

    /** Draws a text run along a path using a bitmap font. */
    public fun drawTextOnPath(
        text: RcText,
        font: RcBitmapFont,
        path: RcPath,
        start: Int,
        end: Int,
        yOffset: RcFloat,
        glyphSpacing: Float,
    )

    /** Draws a text anchored using a bitmap font. */
    public fun drawTextAnchored(
        text: RcText,
        font: RcBitmapFont,
        start: Float,
        end: Float,
        x: Float,
        y: Float,
        panX: Float,
        panY: Float,
        glyphSpacing: Float,
    )

    /** Draws a text anchored using a bitmap font. */
    public fun drawTextAnchored(
        text: RcText,
        font: RcBitmapFont,
        start: RcFloat,
        end: RcFloat,
        x: RcFloat,
        y: RcFloat,
        panX: RcFloat,
        panY: RcFloat,
        glyphSpacing: Float,
    )

    /** Draws a bitmap at the specified coordinates. */
    public fun drawBitmap(image: RcImage, left: Float, top: Float, right: Float, bottom: Float)

    /** Draws a bitmap at the specified position. */
    public fun drawBitmap(image: RcImage, left: Float, top: Float)

    /** Draws a bitmap at the specified coordinates using remote floats. */
    public fun drawBitmap(
        image: RcImage,
        left: RcFloat,
        top: RcFloat,
        right: RcFloat,
        bottom: RcFloat,
    )

    /** drawBitmap taking a typed destination RcRect. */
    public fun drawBitmap(image: RcImage, dst: RcRect): Unit =
        drawBitmap(image, dst.left, dst.top, dst.right, dst.bottom)

    /** Draws a scaled bitmap within source and destination rectangles. */
    public fun drawScaledBitmap(
        image: RcImage,
        srcLeft: Float,
        srcTop: Float,
        srcRight: Float,
        srcBottom: Float,
        dstLeft: Float,
        dstTop: Float,
        dstRight: Float,
        dstBottom: Float,
        scaleType: RcContentScale = RcContentScale.None,
        scaleFactor: Float = 1f,
        contentDescription: String? = null,
    )

    /** Draws a scaled bitmap within source and destination rectangles using remote floats. */
    public fun drawScaledBitmap(
        image: RcImage,
        srcLeft: RcFloat,
        srcTop: RcFloat,
        srcRight: RcFloat,
        srcBottom: RcFloat,
        dstLeft: RcFloat,
        dstTop: RcFloat,
        dstRight: RcFloat,
        dstBottom: RcFloat,
        scaleType: RcContentScale = RcContentScale.None,
        scaleFactor: RcFloat = 1f.rf,
        contentDescription: String? = null,
    )

    /**
     * Draws a mesh, positioned by the ordinary 2D canvas matrix.
     *
     * @param mesh the mesh to draw
     * @param image the bitmap to sample through the mesh's uv, or null for an untextured mesh
     * @param blend how vertex color and texel combine; defaults to what [image] implies
     */
    @ExperimentalRemoteCreationApi
    public fun drawMesh2D(mesh: RcMesh, image: RcImage? = null, blend: RcMeshBlend? = null)

    /**
     * Multiplies the mesh's local frame at `(u, v)` into the current canvas matrix, so ordinary
     * drawing can be placed onto a deformed surface.
     *
     * @param mesh the mesh to read the surface from
     * @param u where in the domain to sample
     * @param v where in the domain to sample
     * @param apply how much of the local frame to apply
     */
    @ExperimentalRemoteCreationApi
    public fun matrixFromMesh2D(
        mesh: RcMesh,
        u: Float,
        v: Float,
        apply: RcMeshMatrix = RcMeshMatrix.Full,
    )

    /**
     * Multiplies the mesh's local frame at `(u, v)` into the current canvas matrix, so ordinary
     * drawing can be placed onto a deformed surface.
     *
     * This is the two-dimensional analogue of [matrixFromPath], and it is what stops meshes being a
     * closed world: attach a label to a waving flag, put icons around a ring, run text along a path
     * strip. The content is drawn with the commands that already exist and never learns a mesh was
     * involved - the mesh only supplies the matrix.
     *
     * @param mesh the mesh to read the surface from
     * @param u where in the domain to sample
     * @param v where in the domain to sample
     * @param apply how much of the local frame to apply
     */
    @ExperimentalRemoteCreationApi
    public fun matrixFromMesh2D(
        mesh: RcMesh,
        u: RcFloat,
        v: RcFloat,
        apply: RcMeshMatrix = RcMeshMatrix.Full,
    )

    /** Clips the current canvas to the specified rectangle. */
    public fun clipRect(left: Float, top: Float, right: Float, bottom: Float)

    /** Clips the current canvas to the specified rectangle using remote floats. */
    public fun clipRect(left: RcFloat, top: RcFloat, right: RcFloat, bottom: RcFloat)

    /** Preconcat the current matrix with the specified rotation in degrees. */
    public fun rotate(angle: Float)

    /** Preconcat the current matrix with the specified rotation in degrees. */
    public fun rotate(angle: RcFloat)

    /** Preconcat the current matrix with the specified rotation in degrees. */
    public fun rotate(angle: Float, centerX: Float, centerY: Float)

    /** Preconcat the current matrix with the specified rotation in degrees. */
    public fun rotate(angle: RcFloat, centerX: RcFloat, centerY: RcFloat)

    /** Preconcat the current matrix with the specified translation. */
    public fun translate(x: Float, y: Float)

    /** Preconcat the current matrix with the specified translation. */
    public fun translate(x: RcFloat, y: RcFloat)

    /** Clips the current canvas to the specified path. */
    public fun clipPath(path: RcPath)

    /**
     * Adds a touch handler with reactive [defaultValue], [min], and [max] bounds and an [RcFloat]
     * touch input [expression] (typically built from [touchPosX] or [touchPosY]).
     */
    public fun addTouch(
        defaultValue: RcFloat,
        min: RcFloat,
        max: RcFloat,
        expression: RcFloat,
        stopMode: RcTouchStopMode = RcTouchStopMode.Gently,
        velocity: RcFloat = RcFloat(0f),
        notchHaptic: RcHaptic = RcHaptic.NoHaptics,
        notches: FloatArray? = null,
        easing: FloatArray? = null,
    ): RcFloat

    /**
     * Adds a touch handler over a static [range] driven by [expression] (typically [touchPosX] or
     * [touchPosY]).
     *
     * @param defaultValue initial value of the touch variable
     * @param range allowed range of the touch variable
     * @param expression touch input expression evaluated on each event
     * @param stopMode behavior when the user releases the touch
     * @param velocity initial/reference velocity value
     * @param notchHaptic haptic kind to fire when the touch crosses a notch
     * @param notches notch positions (interpretation depends on [stopMode]); null for non-notch
     *   modes
     * @param easing easing-spec `FloatArray`; null for default
     */
    public fun addTouch(
        defaultValue: Float,
        range: ClosedFloatingPointRange<Float>,
        expression: RcFloat,
        stopMode: RcTouchStopMode = RcTouchStopMode.Gently,
        velocity: Float = 0f,
        notchHaptic: RcHaptic = RcHaptic.NoHaptics,
        notches: FloatArray? = null,
        easing: FloatArray? = null,
    ): RcFloat =
        addTouch(
            defaultValue = RcFloat(defaultValue),
            min = RcFloat(range.start),
            max = RcFloat(range.endInclusive),
            expression = expression,
            stopMode = stopMode,
            velocity = RcFloat(velocity),
            notchHaptic = notchHaptic,
            notches = notches,
            easing = easing,
        )

    /**
     * Adds a touch handler with **reactive** [min] and [max] bounds and an [RcFloat] touch input
     * [expression]. The expression (typically built from [touchPosX] and [touchPosY], e.g.
     * `touchPosX() * componentWidth() / windowWidth()`) is evaluated on each event to produce the
     * value, which is then clamped to `[min, max]`. Both bounds may be live expressions (e.g. plot
     * edges).
     */
    public fun addTouch(
        defaultValue: Float,
        min: RcFloat,
        max: RcFloat,
        expression: RcFloat,
        stopMode: RcTouchStopMode = RcTouchStopMode.Gently,
        velocity: Float = 0f,
        notchHaptic: RcHaptic = RcHaptic.NoHaptics,
        notches: FloatArray? = null,
        easing: FloatArray? = null,
    ): RcFloat =
        addTouch(
            defaultValue = RcFloat(defaultValue),
            min = min,
            max = max,
            expression = expression,
            stopMode = stopMode,
            velocity = RcFloat(velocity),
            notchHaptic = notchHaptic,
            notches = notches,
            easing = easing,
        )
}

/** Internal helper to convert the new Modifier chain to the legacy RecordingModifier. */
internal fun Modifier.toRecordingModifier(): RecordingModifier {
    val recording = RecordingModifier()
    foldIn(Unit) { _, element -> (element as? RecordingModifierElement)?.applyTo(recording) }
    return recording
}

/** Scope to configure accessibility semantics on layouts. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcSemanticsScope {
    /** Sets the content description text. */
    public fun contentDescription(text: String)

    /** Sets the content description using a remote string reference. */
    public fun contentDescription(text: RcText)

    /** Sets the accessibility role ([RcRole.Button], [RcRole.Checkbox], [RcRole.Image], etc.). */
    public fun role(role: RcRole)

    /** Sets the component text content. */
    public fun text(text: String)

    /** Sets the component text content using a remote string reference. */
    public fun text(text: RcText)

    /** Sets the state description, e.g., "checked" or "disabled". */
    public fun stateDescription(text: String)

    /** Sets the state description using a remote string reference. */
    public fun stateDescription(text: RcText)

    /**
     * Sets the merge mode ([RcSemanticsMode.Set], [RcSemanticsMode.ClearAndSet],
     * [RcSemanticsMode.Merge]).
     */
    public fun mode(mode: RcSemanticsMode)

    /** Sets whether the component is enabled. */
    public fun enabled(value: Boolean)

    /** Sets whether the component is clickable. */
    public fun clickable(value: Boolean)
}

/** Scope for custom layout and measurement computations on the player. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed interface RcLayoutScope {
    /** Read/write X position of the component. */
    public var x: RcFloat

    /** Read/write Y position of the component. */
    public var y: RcFloat

    /** Read/write width dimension of the component. */
    public var width: RcFloat

    /** Read/write height dimension of the component. */
    public var height: RcFloat

    /** Read-only width of the parent layout container. */
    public val parentWidth: RcFloat

    /** Read-only height of the parent layout container. */
    public val parentHeight: RcFloat
}

/** Internal bridge for platform-specific bitmap handles (e.g. Android `Bitmap`). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcScope.remotePlatformBitmap(image: Any): RcImage =
    (this as RcScopeImpl).remotePlatformBitmap(image)

/** Internal bridge for named platform-specific bitmap handles (e.g. Android `Bitmap`). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcScope.remoteNamedPlatformBitmap(name: String, image: Any): RcImage =
    (this as RcScopeImpl).remoteNamedPlatformBitmap(name, image)

/** Emits a root layout container when building raw RemoteCompose documents internally. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcScope.RcRoot(content: RcScope.() -> Unit = {}) {
    (this as RcScopeImpl).RcRoot(content)
}

/**
 * Starts a global section. Prefer [RcScope.global], which guarantees a matched [endGlobal] call.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcScope.beginGlobal() {
    (this as RcScopeImpl).beginGlobal()
}

/** Ends a global section started with [beginGlobal]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcScope.endGlobal() {
    (this as RcScopeImpl).endGlobal()
}

/** Executes a block of raw canvas operations. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcScope.canvasOperations(block: RcCanvasScope.() -> Unit) {
    (this as RcScopeImpl).canvasOperations(block)
}

/** Conditionally executes [content] if `op(a, b)` holds on the player. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <S : RcScope> S.conditionalOperations(
    op: RcConditionOp,
    a: RcFloat,
    b: RcFloat,
    content: S.() -> Unit,
) {
    (this as RcScopeImpl).runConditionalOperations(this, op, a, b, content)
}
