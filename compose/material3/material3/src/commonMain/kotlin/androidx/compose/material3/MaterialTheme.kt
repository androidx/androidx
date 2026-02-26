/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MotionScheme.Companion.standard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Material Theming refers to the customization of your Material Design app to better reflect your
 * product’s brand.
 *
 * Material components such as [Button] and [Checkbox] use values provided here when retrieving
 * default values.
 *
 * All values may be set by providing this component with the [colorScheme][ColorScheme],
 * [typography][Typography] and [shapes][Shapes] attributes. Use this to configure the overall theme
 * of elements within this MaterialTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent MaterialTheme. This allows using a MaterialTheme at the top of
 * your application, and then separate MaterialTheme(s) for different screens / parts of your UI,
 * overriding only the parts of the theme definition that need to change.
 *
 * @sample androidx.compose.material3.samples.MaterialThemeSample
 * @param colorScheme A complete definition of the Material Color theme for this hierarchy
 * @param shapes A set of corner shapes to be used as this hierarchy's shape system
 * @param typography A set of text styles to be used as this hierarchy's typography system
 * @param content The content inheriting this theme
 */
@Composable
fun MaterialTheme(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit,
) =
    MaterialTheme(
        colorScheme = colorScheme,
        motionScheme = MaterialTheme.motionScheme,
        shapes = shapes,
        typography = typography,
        content = content,
    )

/**
 * Material Theming refers to the customization of your Material Design app to better reflect your
 * product’s brand.
 *
 * Material components such as [Button] and [Checkbox] use values provided here when retrieving
 * default values.
 *
 * All values may be set by providing this component with the [colorScheme][ColorScheme],
 * [typography][Typography] attributes. Use this to configure the overall theme of elements within
 * this MaterialTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent MaterialTheme. This allows using a MaterialTheme at the top of
 * your application, and then separate MaterialTheme(s) for different screens / parts of your UI,
 * overriding only the parts of the theme definition that need to change.
 *
 * @param colorScheme A complete definition of the Material Color theme for this hierarchy
 * @param motionScheme A complete definition of the Material Motion scheme for this hierarchy
 * @param shapes A set of corner shapes to be used as this hierarchy's shape system
 * @param typography A set of text styles to be used as this hierarchy's typography system
 */
@Composable
fun MaterialTheme(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    motionScheme: MotionScheme = MaterialTheme.motionScheme,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit,
) {
    val theme =
        MaterialTheme.Values(
            colorScheme = colorScheme,
            motionScheme = motionScheme,
            shapes = shapes,
            typography = typography,
        )
    val rippleIndication = ripple()
    val selectionColors = rememberTextSelectionColors(colorScheme)
    CompositionLocalProvider(
        _localMaterialTheme provides theme,
        LocalIndication provides rippleIndication,
        LocalTextSelectionColors provides selectionColors,
    ) {
        EnsurePrecisionPointerListenersRegistered {
            ProvideTextStyle(value = typography.bodyLarge, content = content)
        }
    }
}

/**
 * Contains functions to access the current theme values provided at the call site's position in the
 * hierarchy.
 */
object MaterialTheme {

    /**
     * Retrieves the current [ColorScheme] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material3.samples.ThemeColorSample
     */
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = LocalMaterialTheme.current.colorScheme

    /**
     * Retrieves the current [Typography] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material3.samples.ThemeTextStyleSample
     */
    val typography: Typography
        @Composable @ReadOnlyComposable get() = LocalMaterialTheme.current.typography

    /**
     * Retrieves the current [Shapes] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material3.samples.ThemeShapeSample
     */
    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = LocalMaterialTheme.current.shapes

    /** Retrieves the current [MotionScheme] at the call site's position in the hierarchy. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val motionScheme: MotionScheme
        @Composable @ReadOnlyComposable get() = LocalMaterialTheme.current.motionScheme

    /**
     * [CompositionLocal] providing [MaterialThemeSubsystems] throughout the hierarchy. You can use
     * properties in the companion object to access specific subsystems, for example [colorScheme].
     * To provide a new value for this, use [MaterialTheme]. This API is exposed to allow retrieving
     * values from inside CompositionLocalConsumerModifierNode implementations - in most cases you
     * should use [colorScheme] and other properties directly.
     */
    val LocalMaterialTheme: CompositionLocal<Values>
        get() = _localMaterialTheme

    /**
     * A read-only `CompositionLocal` that provides the current [MotionScheme] to Material 3
     * components.
     *
     * The motion scheme is typically supplied by [MaterialTheme.motionScheme] and can be overridden
     * for specific UI subtrees by wrapping it with another [MaterialTheme].
     *
     * This API is exposed to allow retrieving motion values from inside
     * `CompositionLocalConsumerModifierNode` implementations, but in most cases it's recommended to
     * read the motion values from [MaterialTheme.motionScheme].
     */
    @Suppress("ExperimentalPropertyAnnotation")
    @ExperimentalMaterial3ExpressiveApi
    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Use [LocalMaterialTheme.current.motionScheme] instead",
    )
    val LocalMotionScheme: CompositionLocal<MotionScheme>
        get() = compositionLocalWithComputedDefaultOf {
            LocalMaterialTheme.currentValue.motionScheme
        }

    /**
     * Material 3 contains different theme subsystems to allow visual customization across a UI
     * hierarchy.
     *
     * Components use properties provided here when retrieving default values.
     *
     * @property colorScheme [ColorScheme] used by material components
     * @property typography [Typography] used by material components
     * @property shapes [Shapes] used by material components
     * @property motionScheme [MotionScheme] used by material components
     */
    @Immutable
    class Values(
        val colorScheme: ColorScheme = lightColorScheme(),
        val typography: Typography = Typography(),
        val shapes: Shapes = Shapes(),
        val motionScheme: MotionScheme = standard(),
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Values

            if (colorScheme != other.colorScheme) return false
            if (typography != other.typography) return false
            if (shapes != other.shapes) return false
            if (motionScheme != other.motionScheme) return false

            return true
        }

        override fun hashCode(): Int {
            var result = colorScheme.hashCode()
            result = 31 * result + typography.hashCode()
            result = 31 * result + shapes.hashCode()
            result = 31 * result + motionScheme.hashCode()
            return result
        }

        override fun toString(): String {
            return "Values(colorScheme=$colorScheme, " +
                "typography=$typography, shapes=$shapes, motionScheme=$motionScheme)"
        }
    }
}

/**
 * Material Expressive Theming refers to the customization of your Material Design app to better
 * reflect your product’s brand.
 *
 * Material components such as [Button] and [Checkbox] use values provided here when retrieving
 * default values.
 *
 * All values may be set by providing this component with the [colorScheme][ColorScheme],
 * [typography][Typography], [shapes][Shapes] attributes. Use this to configure the overall theme of
 * elements within this MaterialTheme.
 *
 * Any values that are not set will fall back to the defaults. To inherit the current value from the
 * theme, pass them into subsequent calls and override only the parts of the theme definition that
 * need to change.
 *
 * Alternatively, only call this function at the top of your application, and then call
 * [MaterialTheme] to specify separate MaterialTheme(s) for different screens / parts of your UI,
 * overriding only the parts of the theme definition that need to change.
 *
 * @sample androidx.compose.material3.samples.MaterialExpressiveThemeSample
 * @param colorScheme A complete definition of the Material Color theme for this hierarchy
 * @param motionScheme A complete definition of the Material motion theme for this hierarchy
 * @param shapes A set of corner shapes to be used as this hierarchy's shape system
 * @param typography A set of text styles to be used as this hierarchy's typography system
 * @param content The content inheriting this theme
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun MaterialExpressiveTheme(
    colorScheme: ColorScheme? = null,
    motionScheme: MotionScheme? = null,
    shapes: Shapes? = null,
    typography: Typography? = null,
    content: @Composable () -> Unit,
) {
    if (LocalUsingExpressiveTheme.current) {
        MaterialTheme(
            colorScheme = colorScheme ?: MaterialTheme.colorScheme,
            motionScheme = motionScheme ?: MaterialTheme.motionScheme,
            typography = typography ?: MaterialTheme.typography,
            shapes = shapes ?: MaterialTheme.shapes,
            content = content,
        )
    } else {
        CompositionLocalProvider(LocalUsingExpressiveTheme provides true) {
            MaterialTheme(
                colorScheme = colorScheme ?: expressiveLightColorScheme(),
                motionScheme = motionScheme ?: MotionScheme.expressive(),
                shapes = shapes ?: Shapes(),
                // TODO: replace with calls to Expressive typography default
                typography = typography ?: Typography(),
                content = content,
            )
        }
    }
}

internal val LocalUsingExpressiveTheme = staticCompositionLocalOf { false }

@Composable
/*@VisibleForTesting*/
internal fun rememberTextSelectionColors(colorScheme: ColorScheme): TextSelectionColors {
    val primaryColor = colorScheme.primary
    return remember(primaryColor) {
        TextSelectionColors(
            handleColor = primaryColor,
            backgroundColor = primaryColor.copy(alpha = TextSelectionBackgroundOpacity),
        )
    }
}

/*@VisibleForTesting*/
internal const val TextSelectionBackgroundOpacity = 0.4f

/** Use [MaterialTheme.LocalMaterialTheme] to access this publicly. */
@Suppress("CompositionLocalNaming")
private val _localMaterialTheme: ProvidableCompositionLocal<MaterialTheme.Values> =
    staticCompositionLocalOf {
        MaterialTheme.Values()
    }
