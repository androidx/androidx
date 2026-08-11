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
internal inline fun <T : Any> computedDefaultOf(
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
public val LocalAccessibilityManager: ProvidableCompositionLocal<AccessibilityManager?> =
    computedNullableDefaultOf {
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
public val LocalAutofill: ProvidableCompositionLocal<Autofill?> = computedNullableDefaultOf {
    LocalOwner.currentValue.autofill
}

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
public val LocalAutofillTree: ProvidableCompositionLocal<AutofillTree> =
    computedDefaultOf("LocalAutofillTree") { LocalOwner.currentValue.autofillTree }

/**
 * The CompositionLocal that can be used to trigger autofill actions. Eg. [AutofillManager.commit].
 */
public val LocalAutofillManager: ProvidableCompositionLocal<AutofillManager?> =
    computedNullableDefaultOf {
        LocalOwner.currentValue.autofillManager
    }

/** The CompositionLocal to provide communication with platform clipboard service. */
@Deprecated(
    "Use LocalClipboard instead which supports suspend functions",
    ReplaceWith("LocalClipboard", "androidx.compose.ui.platform.LocalClipboard"),
)
public val LocalClipboardManager: ProvidableCompositionLocal<ClipboardManager> =
    computedDefaultOf("LocalClipboardManager") { LocalOwner.currentValue.clipboardManager }

/** The CompositionLocal to provide communication with platform clipboard service. */
public val LocalClipboard: ProvidableCompositionLocal<Clipboard> =
    computedDefaultOf("LocalClipboard") { LocalOwner.currentValue.clipboard }

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
public val LocalGraphicsContext: ProvidableCompositionLocal<GraphicsContext> =
    computedDefaultOf("LocalGraphicsContext") { LocalOwner.currentValue.graphicsContext }

/**
 * Provides the [Density] to be used to transform between
 * [density-independent pixel units (DP)][androidx.compose.ui.unit.Dp] and pixel units or
 * [scale-independent pixel units (SP)][androidx.compose.ui.unit.TextUnit] and pixel units. This is
 * typically used when a [DP][androidx.compose.ui.unit.Dp] is provided and it must be converted in
 * the body of [Layout] or [DrawModifier].
 */
public val LocalDensity: ProvidableCompositionLocal<Density> =
    computedDefaultOf("LocalDensity") { LocalOwner.currentValue.density }

/** The CompositionLocal that can be used to control focus within Compose. */
public val LocalFocusManager: ProvidableCompositionLocal<FocusManager> =
    computedDefaultOf<FocusManager>("LocalFocusManager") { LocalOwner.currentValue.focusOwner }

/** The CompositionLocal to provide platform font loading methods. */
@Suppress("DEPRECATION")
@Deprecated(
    "LocalFontLoader is replaced with LocalFontFamilyResolver",
    replaceWith = ReplaceWith("LocalFontFamilyResolver"),
)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalFontLoader:
    ProvidableCompositionLocal<
        @Suppress("DEPRECATION")
        androidx.compose.ui.text.font.Font.ResourceLoader
    > =
    computedDefaultOf("LocalFontLoader") {
        @Suppress("DEPRECATION") LocalOwner.currentValue.fontLoader
    }

/** The CompositionLocal for compose font resolution from FontFamily. */
public val LocalFontFamilyResolver:
    ProvidableCompositionLocal<androidx.compose.ui.text.font.FontFamily.Resolver> =
    computedDefaultOf("LocalFontFamilyResolver") { LocalOwner.currentValue.fontFamilyResolver }

/** The CompositionLocal to provide haptic feedback to the user. */
public val LocalHapticFeedback:
    ProvidableCompositionLocal<androidx.compose.ui.hapticfeedback.HapticFeedback> =
    computedDefaultOf("LocalHapticFeedback") { LocalOwner.currentValue.hapticFeedBack }

/**
 * The CompositionLocal to provide an instance of InputModeManager which controls the current input
 * mode.
 */
public val LocalInputModeManager:
    ProvidableCompositionLocal<androidx.compose.ui.input.InputModeManager> =
    computedDefaultOf("LocalInputModeManager") { LocalOwner.currentValue.inputModeManager }

/** The CompositionLocal to provide the layout direction. */
public val LocalLayoutDirection:
    ProvidableCompositionLocal<androidx.compose.ui.unit.LayoutDirection> =
    computedDefaultOf("LocalLayoutDirection") { LocalOwner.currentValue.layoutDirection }

/** The providable CompositionLocal to provide the locale list. This list can never be empty. */
@get:VisibleForTesting
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalProvidableLocaleList: ProvidableCompositionLocal<LocaleList> =
    computedDefaultOf("LocalProvidableLocaleList") { LocalOwner.currentValue.localeList }

/** The CompositionLocal to provide the locale list. This list will never be empty. */
public val LocalLocaleList: CompositionLocal<LocaleList>
    get() = LocalProvidableLocaleList

/** The CompositionLocal to provide the locale. */
public val LocalLocale: CompositionLocal<Locale> = compositionLocalWithComputedDefaultOf {
    LocalLocaleList.currentValue.first()
}

/** The CompositionLocal to provide communication with platform text input service. */
@Deprecated("Use PlatformTextInputModifierNode instead.")
public val LocalTextInputService:
    ProvidableCompositionLocal<androidx.compose.ui.text.input.TextInputService?> =
    computedNullableDefaultOf {
        LocalOwner.currentValue.textInputService
    }

/**
 * The [CompositionLocal] to provide a [SoftwareKeyboardController] that can control the current
 * software keyboard.
 *
 * Will be null if the software keyboard cannot be controlled.
 */
public val LocalSoftwareKeyboardController:
    ProvidableCompositionLocal<SoftwareKeyboardController?> =
    computedNullableDefaultOf {
        LocalOwner.currentValue.softwareKeyboardController
    }

/** The CompositionLocal to provide text-related toolbar. */
public val LocalTextToolbar: ProvidableCompositionLocal<TextToolbar> =
    computedDefaultOf("LocalTextToolbar") { LocalOwner.currentValue.textToolbar }

/** The CompositionLocal to provide functionality related to URL, e.g. open URI. */
public val LocalUriHandler: ProvidableCompositionLocal<UriHandler> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.uriHandler
    }

/** The CompositionLocal that provides the ViewConfiguration. */
public val LocalViewConfiguration: ProvidableCompositionLocal<ViewConfiguration> =
    computedDefaultOf("LocalViewConfiguration") { LocalOwner.currentValue.viewConfiguration }

/**
 * The CompositionLocal that provides information about the window that hosts the current [Owner].
 */
public val LocalWindowInfo: ProvidableCompositionLocal<WindowInfo> =
    computedDefaultOf("LocalWindowInfo") { LocalOwner.currentValue.windowInfo }

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
public val LocalSoundEffect: ProvidableCompositionLocal<SoundEffect> =
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
public expect val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner>

internal val LocalPointerIconService = computedNullableDefaultOf {
    LocalOwner.currentValue.pointerIconService
}

/** @see LocalScrollCaptureInProgress */
internal val LocalProvidableScrollCaptureInProgress = compositionLocalOf { false }

/**
 * The [GraphicsResourceCache] of the [Owner] hosting this composition, or null if the owner doesn't
 * support graphics resource sharing.
 */
internal val LocalGraphicsResourceCache = computedNullableDefaultOf {
    LocalOwner.currentValue.graphicsResourceCache
}

/**
 * True when the system is currently capturing the contents of a scrollable in this compose view or
 * any parent compose view.
 */
public val LocalScrollCaptureInProgress: CompositionLocal<Boolean>
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
public val LocalCursorBlinkEnabled: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf {
    true
}

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

internal fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
