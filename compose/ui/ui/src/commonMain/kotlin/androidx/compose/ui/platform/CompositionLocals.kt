/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.platform

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LocalRetainScope
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.Owner
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.LifecycleOwner

/** The CompositionLocal to provide communication with platform accessibility service. */
val LocalAccessibilityManager = staticCompositionLocalOf<AccessibilityManager?> { null }

/**
 * The CompositionLocal that can be used to trigger autofill actions. Eg.
 * [Autofill.requestAutofillForNode].
 */
@Deprecated(
    """
        Use the new semantics-based Autofill APIs androidx.compose.ui.autofill.ContentType and
        androidx.compose.ui.autofill.ContentDataType instead.
        """
)
val LocalAutofill = staticCompositionLocalOf<Autofill?> { null }

/**
 * The CompositionLocal that can be used to add [AutofillNode][import
 * androidx.compose.ui.autofill.AutofillNode]s to the autofill tree. The [AutofillTree] is a
 * temporary data structure that will be replaced by Autofill Semantics (b/138604305).
 */
@Deprecated(
    """
        Use the new semantics-based Autofill APIs androidx.compose.ui.autofill.ContentType and
        androidx.compose.ui.autofill.ContentDataType instead.
        """
)
val LocalAutofillTree =
    staticCompositionLocalOf<AutofillTree> { noLocalProvidedFor("LocalAutofillTree") }

/**
 * The CompositionLocal that can be used to trigger autofill actions. Eg. [AutofillManager.commit].
 */
val LocalAutofillManager =
    staticCompositionLocalOf<AutofillManager?> { noLocalProvidedFor("LocalAutofillManager") }

/** The CompositionLocal to provide communication with platform clipboard service. */
@Deprecated(
    "Use LocalClipboard instead which supports suspend functions",
    ReplaceWith("LocalClipboard", "androidx.compose.ui.platform.LocalClipboard"),
)
val LocalClipboardManager =
    staticCompositionLocalOf<ClipboardManager> { noLocalProvidedFor("LocalClipboardManager") }

/** The CompositionLocal to provide communication with platform clipboard service. */
val LocalClipboard = staticCompositionLocalOf<Clipboard> { noLocalProvidedFor("LocalClipboard") }

/**
 * The CompositionLocal to provide access to a [GraphicsContext] instance for creation of
 * [GraphicsLayer]s.
 *
 * Consumers that access this Local directly and call [GraphicsContext.createGraphicsLayer] are
 * responsible for calling [GraphicsContext.releaseGraphicsLayer].
 *
 * It is recommended that consumers invoke [rememberGraphicsLayer][import
 * androidx.compose.ui.graphics.rememberGraphicsLayer] instead to ensure that a [GraphicsLayer] is
 * released when the corresponding composable is disposed.
 */
val LocalGraphicsContext =
    staticCompositionLocalOf<GraphicsContext> { noLocalProvidedFor("LocalGraphicsContext") }

/**
 * Provides the [Density] to be used to transform between
 * [density-independent pixel units (DP)][androidx.compose.ui.unit.Dp] and pixel units or
 * [scale-independent pixel units (SP)][androidx.compose.ui.unit.TextUnit] and pixel units. This is
 * typically used when a [DP][androidx.compose.ui.unit.Dp] is provided and it must be converted in
 * the body of [Layout] or [DrawModifier].
 */
val LocalDensity = staticCompositionLocalOf<Density> { noLocalProvidedFor("LocalDensity") }

/** The CompositionLocal that can be used to control focus within Compose. */
val LocalFocusManager =
    staticCompositionLocalOf<FocusManager> { noLocalProvidedFor("LocalFocusManager") }

/** The CompositionLocal to provide platform font loading methods. */
@Suppress("DEPRECATION")
@Deprecated(
    "LocalFontLoader is replaced with LocalFontFamilyResolver",
    replaceWith = ReplaceWith("LocalFontFamilyResolver"),
)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalFontLoader =
    staticCompositionLocalOf<Font.ResourceLoader> { noLocalProvidedFor("LocalFontLoader") }

/** The CompositionLocal for compose font resolution from FontFamily. */
val LocalFontFamilyResolver =
    staticCompositionLocalOf<FontFamily.Resolver> { noLocalProvidedFor("LocalFontFamilyResolver") }

/** The CompositionLocal to provide haptic feedback to the user. */
val LocalHapticFeedback =
    staticCompositionLocalOf<HapticFeedback> { noLocalProvidedFor("LocalHapticFeedback") }

/**
 * The CompositionLocal to provide an instance of InputModeManager which controls the current input
 * mode.
 */
val LocalInputModeManager =
    staticCompositionLocalOf<InputModeManager> { noLocalProvidedFor("LocalInputManager") }

/** The CompositionLocal to provide the layout direction. */
val LocalLayoutDirection =
    staticCompositionLocalOf<LayoutDirection> { noLocalProvidedFor("LocalLayoutDirection") }

/** The CompositionLocal to provide communication with platform text input service. */
@Deprecated("Use PlatformTextInputModifierNode instead.")
val LocalTextInputService = staticCompositionLocalOf<TextInputService?> { null }

/**
 * The [CompositionLocal] to provide a [SoftwareKeyboardController] that can control the current
 * software keyboard.
 *
 * Will be null if the software keyboard cannot be controlled.
 */
val LocalSoftwareKeyboardController = staticCompositionLocalOf<SoftwareKeyboardController?> { null }

/** The CompositionLocal to provide text-related toolbar. */
val LocalTextToolbar =
    staticCompositionLocalOf<TextToolbar> { noLocalProvidedFor("LocalTextToolbar") }

/** The CompositionLocal to provide functionality related to URL, e.g. open URI. */
val LocalUriHandler = staticCompositionLocalOf<UriHandler> { noLocalProvidedFor("LocalUriHandler") }

/** The CompositionLocal that provides the ViewConfiguration. */
val LocalViewConfiguration =
    staticCompositionLocalOf<ViewConfiguration> { noLocalProvidedFor("LocalViewConfiguration") }

/**
 * The CompositionLocal that provides information about the window that hosts the current [Owner].
 */
val LocalWindowInfo = staticCompositionLocalOf<WindowInfo> { noLocalProvidedFor("LocalWindowInfo") }

/** The CompositionLocal containing the current [LifecycleOwner]. */
@Deprecated(
    "Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.",
    ReplaceWith("androidx.lifecycle.compose.LocalLifecycleOwner"),
)
expect val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner>

internal val LocalPointerIconService = staticCompositionLocalOf<PointerIconService?> { null }

/** @see LocalScrollCaptureInProgress */
internal val LocalProvidableScrollCaptureInProgress = compositionLocalOf { false }

/**
 * True when the system is currently capturing the contents of a scrollable in this compose view or
 * any parent compose view.
 */
val LocalScrollCaptureInProgress: CompositionLocal<Boolean>
    get() = LocalProvidableScrollCaptureInProgress

/**
 * Text cursor blinking
 * - _true_ normal cursor behavior (interactive blink)
 * - _false_ never blink (always on)
 *
 * The default of _true_ is the user-expected system behavior for Text editing.
 *
 * Typically you should not set _false_ outside of screenshot tests without also providing a
 * `cursorBrush` to `BasicTextField` to implement a custom design
 */
val LocalCursorBlinkEnabled: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }

@ExperimentalComposeUiApi
@Composable
internal fun ProvideCommonCompositionLocals(
    owner: Owner,
    uriHandler: UriHandler,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAccessibilityManager provides owner.accessibilityManager,
        LocalAutofill provides owner.autofill,
        LocalAutofillManager provides owner.autofillManager,
        LocalAutofillTree provides owner.autofillTree,
        LocalClipboardManager provides owner.clipboardManager,
        LocalClipboard provides owner.clipboard,
        LocalDensity provides owner.density,
        LocalFocusManager provides owner.focusOwner,
        @Suppress("DEPRECATION") LocalFontLoader providesDefault
            @Suppress("DEPRECATION") owner.fontLoader,
        LocalFontFamilyResolver providesDefault owner.fontFamilyResolver,
        LocalHapticFeedback provides owner.hapticFeedBack,
        LocalInputModeManager provides owner.inputModeManager,
        LocalLayoutDirection provides owner.layoutDirection,
        LocalRetainScope provides owner.retainScope,
        LocalTextInputService provides owner.textInputService,
        LocalSoftwareKeyboardController provides owner.softwareKeyboardController,
        LocalTextToolbar provides owner.textToolbar,
        LocalUriHandler provides uriHandler,
        LocalViewConfiguration provides owner.viewConfiguration,
        LocalWindowInfo provides owner.windowInfo,
        LocalPointerIconService provides owner.pointerIconService,
        LocalGraphicsContext provides owner.graphicsContext,
        content = content,
    )
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
