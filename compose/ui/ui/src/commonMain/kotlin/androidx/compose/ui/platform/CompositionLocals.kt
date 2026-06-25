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
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.staticCompositionLocalWithComputedDefaultOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.node.Owner
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.lifecycle.LifecycleOwner

internal val LocalOwner = staticCompositionLocalOf<Owner> { noLocalProvidedFor("LocalOwner") }

@Suppress("BanInlineOptIn", "NullAnnotationGroup")
@OptIn(ExperimentalComposeUiApi::class)
private inline fun <T : Any> computedDefaultOf(
    name: String,
    crossinline compute: CompositionLocalAccessorScope.() -> T,
): ProvidableCompositionLocal<T> =
    if (androidx.compose.ui.ComposeUiFlags.isMinimalistLocalsEnabled) {
        staticCompositionLocalWithComputedDefaultOf { compute() }
    } else {
        staticCompositionLocalOf { noLocalProvidedFor(name) }
    }

@Suppress("BanInlineOptIn", "NullAnnotationGroup")
@OptIn(ExperimentalComposeUiApi::class)
private inline fun <T : Any> computedNullableDefaultOf(
    crossinline compute: CompositionLocalAccessorScope.() -> T?
): ProvidableCompositionLocal<T?> =
    if (androidx.compose.ui.ComposeUiFlags.isMinimalistLocalsEnabled) {
        staticCompositionLocalWithComputedDefaultOf { compute() }
    } else {
        staticCompositionLocalOf { null }
    }

/** The CompositionLocal to provide communication with platform accessibility service. */
val LocalAccessibilityManager = computedNullableDefaultOf {
    LocalOwner.currentValue.accessibilityManager
}

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
val LocalAutofill = computedNullableDefaultOf { LocalOwner.currentValue.autofill }

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
    computedDefaultOf("LocalAutofillTree") { LocalOwner.currentValue.autofillTree }

/**
 * The CompositionLocal that can be used to trigger autofill actions. Eg. [AutofillManager.commit].
 */
val LocalAutofillManager = computedNullableDefaultOf { LocalOwner.currentValue.autofillManager }

/** The CompositionLocal to provide communication with platform clipboard service. */
@Deprecated(
    "Use LocalClipboard instead which supports suspend functions",
    ReplaceWith("LocalClipboard", "androidx.compose.ui.platform.LocalClipboard"),
)
val LocalClipboardManager =
    computedDefaultOf("LocalClipboardManager") { LocalOwner.currentValue.clipboardManager }

/** The CompositionLocal to provide communication with platform clipboard service. */
val LocalClipboard = computedDefaultOf("LocalClipboard") { LocalOwner.currentValue.clipboard }

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
    computedDefaultOf("LocalGraphicsContext") { LocalOwner.currentValue.graphicsContext }

/**
 * Provides the [Density] to be used to transform between
 * [density-independent pixel units (DP)][androidx.compose.ui.unit.Dp] and pixel units or
 * [scale-independent pixel units (SP)][androidx.compose.ui.unit.TextUnit] and pixel units. This is
 * typically used when a [DP][androidx.compose.ui.unit.Dp] is provided and it must be converted in
 * the body of [Layout] or [DrawModifier].
 */
val LocalDensity = computedDefaultOf("LocalDensity") { LocalOwner.currentValue.density }

/** The CompositionLocal that can be used to control focus within Compose. */
val LocalFocusManager =
    computedDefaultOf<FocusManager>("LocalFocusManager") { LocalOwner.currentValue.focusOwner }

/** The CompositionLocal to provide platform font loading methods. */
@Suppress("DEPRECATION")
@Deprecated(
    "LocalFontLoader is replaced with LocalFontFamilyResolver",
    replaceWith = ReplaceWith("LocalFontFamilyResolver"),
)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalFontLoader =
    computedDefaultOf("LocalFontLoader") {
        @Suppress("DEPRECATION") LocalOwner.currentValue.fontLoader
    }

/** The CompositionLocal for compose font resolution from FontFamily. */
val LocalFontFamilyResolver =
    computedDefaultOf("LocalFontFamilyResolver") { LocalOwner.currentValue.fontFamilyResolver }

/** The CompositionLocal to provide haptic feedback to the user. */
val LocalHapticFeedback =
    computedDefaultOf("LocalHapticFeedback") { LocalOwner.currentValue.hapticFeedBack }

/**
 * The CompositionLocal to provide an instance of InputModeManager which controls the current input
 * mode.
 */
val LocalInputModeManager =
    computedDefaultOf("LocalInputModeManager") { LocalOwner.currentValue.inputModeManager }

/** The CompositionLocal to provide the layout direction. */
val LocalLayoutDirection =
    computedDefaultOf("LocalLayoutDirection") { LocalOwner.currentValue.layoutDirection }

/** The providable CompositionLocal to provide the locale list. This list can never be empty. */
@get:VisibleForTesting
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalProvidableLocaleList =
    computedDefaultOf("LocalProvidableLocaleList") { LocalOwner.currentValue.localeList }

/** The CompositionLocal to provide the locale list. This list will never be empty. */
val LocalLocaleList: CompositionLocal<LocaleList>
    get() = LocalProvidableLocaleList

/** The CompositionLocal to provide the locale. */
val LocalLocale: CompositionLocal<Locale> = compositionLocalWithComputedDefaultOf {
    LocalLocaleList.currentValue.first()
}

/** The CompositionLocal to provide communication with platform text input service. */
@Deprecated("Use PlatformTextInputModifierNode instead.")
val LocalTextInputService = computedNullableDefaultOf { LocalOwner.currentValue.textInputService }

/**
 * The [CompositionLocal] to provide a [SoftwareKeyboardController] that can control the current
 * software keyboard.
 *
 * Will be null if the software keyboard cannot be controlled.
 */
val LocalSoftwareKeyboardController = computedNullableDefaultOf {
    LocalOwner.currentValue.softwareKeyboardController
}

/** The CompositionLocal to provide text-related toolbar. */
val LocalTextToolbar = computedDefaultOf("LocalTextToolbar") { LocalOwner.currentValue.textToolbar }

/** The CompositionLocal to provide functionality related to URL, e.g. open URI. */
val LocalUriHandler = staticCompositionLocalWithComputedDefaultOf {
    LocalOwner.currentValue.uriHandler
}

/** The CompositionLocal that provides the ViewConfiguration. */
val LocalViewConfiguration =
    computedDefaultOf("LocalViewConfiguration") { LocalOwner.currentValue.viewConfiguration }

/**
 * The CompositionLocal that provides information about the window that hosts the current [Owner].
 */
val LocalWindowInfo = computedDefaultOf("LocalWindowInfo") { LocalOwner.currentValue.windowInfo }

/**
 * The CompositionLocal to provide platform sound effects.
 *
 * This is used to trigger sounds on user interaction, like clicks. To enable, disable, or customize
 * sound interaction scopes, utilize `SoundEffectOnInteraction`.
 *
 * @sample androidx.compose.ui.samples.InteractionSoundSamples
 * @see SoundEffect
 */
@Suppress("NullAnnotationGroup")
@OptIn(ExperimentalComposeUiApi::class)
val LocalSoundEffect: ProvidableCompositionLocal<SoundEffect> =
    if (androidx.compose.ui.ComposeUiFlags.isMinimalistLocalsEnabled) {
        staticCompositionLocalWithComputedDefaultOf { LocalOwner.currentValue.soundEffect }
    } else {
        staticCompositionLocalOf { NoSoundEffect }
    }

/** The CompositionLocal containing the current [LifecycleOwner]. */
@Deprecated(
    "Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.",
    ReplaceWith("androidx.lifecycle.compose.LocalLifecycleOwner"),
)
expect val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner>

internal val LocalPointerIconService = computedNullableDefaultOf {
    LocalOwner.currentValue.pointerIconService
}

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

@Suppress("NullAnnotationGroup")
@ExperimentalComposeUiApi
@Composable
internal fun ProvideCommonCompositionLocals(owner: Owner, content: @Composable () -> Unit) {
    if (androidx.compose.ui.ComposeUiFlags.isMinimalistLocalsEnabled) {
        CompositionLocalProvider(
            LocalOwner provides owner,
            LocalRetainedValuesStore provides owner.retainedValuesStore,
            content = content,
        )
    } else {
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
            LocalInputModeManager providesComputed { owner.inputModeManager },
            LocalLayoutDirection provides owner.layoutDirection,
            LocalTextInputService providesComputed { owner.textInputService },
            LocalSoftwareKeyboardController providesComputed { owner.softwareKeyboardController },
            LocalTextToolbar providesComputed { owner.textToolbar },
            LocalUriHandler provides owner.uriHandler,
            LocalViewConfiguration provides owner.viewConfiguration,
            LocalWindowInfo provides owner.windowInfo,
            LocalPointerIconService providesComputed { owner.pointerIconService },
            LocalGraphicsContext provides owner.graphicsContext,
            LocalRetainedValuesStore provides owner.retainedValuesStore,
            LocalProvidableLocaleList provides owner.localeList,
            content = content,
        )
    }
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
