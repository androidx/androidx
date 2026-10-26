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

/** The CompositionLocal to provide communication with platform accessibility service. */
public val LocalAccessibilityManager: ProvidableCompositionLocal<AccessibilityManager?> =
    staticCompositionLocalWithComputedDefaultOf {
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
public val LocalAutofill: ProvidableCompositionLocal<Autofill?> =
    staticCompositionLocalWithComputedDefaultOf {
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
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.autofillTree
    }

/**
 * The CompositionLocal that can be used to trigger autofill actions. Eg. [AutofillManager.commit].
 */
public val LocalAutofillManager: ProvidableCompositionLocal<AutofillManager?> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.autofillManager
    }

/** The CompositionLocal to provide communication with platform clipboard service. */
@Deprecated(
    "Use LocalClipboard instead which supports suspend functions",
    ReplaceWith("LocalClipboard", "androidx.compose.ui.platform.LocalClipboard"),
)
public val LocalClipboardManager: ProvidableCompositionLocal<ClipboardManager> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.clipboardManager
    }

/** The CompositionLocal to provide communication with platform clipboard service. */
public val LocalClipboard: ProvidableCompositionLocal<Clipboard> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.clipboard
    }

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
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.graphicsContext
    }

/**
 * Provides the [Density] to be used to transform between
 * [density-independent pixel units (DP)][androidx.compose.ui.unit.Dp] and pixel units or
 * [scale-independent pixel units (SP)][androidx.compose.ui.unit.TextUnit] and pixel units. This is
 * typically used when a [DP][androidx.compose.ui.unit.Dp] is provided and it must be converted in
 * the body of [Layout] or [DrawModifier].
 */
public val LocalDensity: ProvidableCompositionLocal<Density> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.density
    }

/** The CompositionLocal that can be used to control focus within Compose. */
public val LocalFocusManager: ProvidableCompositionLocal<FocusManager> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.focusOwner
    }

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
    staticCompositionLocalWithComputedDefaultOf {
        @Suppress("DEPRECATION") LocalOwner.currentValue.fontLoader
    }

/** The CompositionLocal for compose font resolution from FontFamily. */
public val LocalFontFamilyResolver:
    ProvidableCompositionLocal<androidx.compose.ui.text.font.FontFamily.Resolver> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.fontFamilyResolver
    }

/** The CompositionLocal to provide haptic feedback to the user. */
public val LocalHapticFeedback:
    ProvidableCompositionLocal<androidx.compose.ui.hapticfeedback.HapticFeedback> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.hapticFeedBack
    }

/**
 * The CompositionLocal to provide an instance of InputModeManager which controls the current input
 * mode.
 */
public val LocalInputModeManager:
    ProvidableCompositionLocal<androidx.compose.ui.input.InputModeManager> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.inputModeManager
    }

/** The CompositionLocal to provide the layout direction. */
public val LocalLayoutDirection:
    ProvidableCompositionLocal<androidx.compose.ui.unit.LayoutDirection> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.layoutDirection
    }

/** The providable CompositionLocal to provide the locale list. This list can never be empty. */
@get:VisibleForTesting
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalProvidableLocaleList: ProvidableCompositionLocal<LocaleList> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.localeList
    }

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
    staticCompositionLocalWithComputedDefaultOf {
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
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.softwareKeyboardController
    }

/** The CompositionLocal to provide text-related toolbar. */
public val LocalTextToolbar: ProvidableCompositionLocal<TextToolbar> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.textToolbar
    }

/** The CompositionLocal to provide functionality related to URL, e.g. open URI. */
public val LocalUriHandler: ProvidableCompositionLocal<UriHandler> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.uriHandler
    }

/** The CompositionLocal that provides the ViewConfiguration. */
public val LocalViewConfiguration: ProvidableCompositionLocal<ViewConfiguration> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.viewConfiguration
    }

/**
 * The CompositionLocal that provides information about the window that hosts the current [Owner].
 */
public val LocalWindowInfo: ProvidableCompositionLocal<WindowInfo> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.windowInfo
    }

/**
 * The CompositionLocal to provide [TaskDispatchers] for running coroutines and asynchronous tasks
 * inside a Compose UI hierarchy.
 *
 * @sample androidx.compose.ui.samples.TaskDispatchersSample
 */
public val LocalTaskDispatchers: ProvidableCompositionLocal<TaskDispatchers> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.taskDispatchers
    }

/**
 * The CompositionLocal to provide platform sound effects.
 *
 * This is used to trigger sounds on user interaction, like clicks. To enable, disable, or customize
 * sound interaction scopes, utilize `SoundEffectOnInteraction`.
 *
 * @sample androidx.compose.ui.samples.InteractionSoundSamples
 * @see SoundEffect
 */
public val LocalSoundEffect: ProvidableCompositionLocal<SoundEffect> =
    staticCompositionLocalWithComputedDefaultOf {
        LocalOwner.currentValue.soundEffect
    }

/** The CompositionLocal containing the current [LifecycleOwner]. */
@Deprecated(
    "Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.",
    ReplaceWith("androidx.lifecycle.compose.LocalLifecycleOwner"),
)
public expect val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner>

internal val LocalPointerIconService = staticCompositionLocalWithComputedDefaultOf {
    LocalOwner.currentValue.pointerIconService
}

/** @see LocalScrollCaptureInProgress */
internal val LocalProvidableScrollCaptureInProgress = compositionLocalOf { false }

/**
 * The [GraphicsResourceCache] of the [Owner] hosting this composition, or null if the owner doesn't
 * support graphics resource sharing.
 */
internal val LocalGraphicsResourceCache = staticCompositionLocalWithComputedDefaultOf {
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

@ExperimentalComposeUiApi
@Composable
internal fun ProvideCommonCompositionLocals(owner: Owner, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalOwner provides owner,
        LocalRetainedValuesStore provides owner.retainedValuesStore,
        LocalDensity provides owner.density,
        LocalLayoutDirection provides owner.layoutDirection,
        LocalFocusManager provides owner.focusOwner,
        LocalWindowInfo provides owner.windowInfo,
        LocalViewConfiguration provides owner.viewConfiguration,
        LocalFontFamilyResolver providesDefault owner.fontFamilyResolver,
        LocalGraphicsContext provides owner.graphicsContext,
        LocalClipboard provides owner.clipboard,
        LocalProvidableLocaleList provides owner.localeList,
        content = content,
    )
}

internal fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
