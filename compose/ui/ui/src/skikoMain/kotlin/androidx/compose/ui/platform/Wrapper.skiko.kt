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
package androidx.compose.ui.platform

import androidx.compose.runtime.*
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.RootNodeOwner


// aosp/3732987 removed this from commonMain, so copy a previous version here for now
// TODO: https://youtrack.jetbrains.com/issue/CMP-9304
@Suppress("DEPRECATION")
@Composable
private fun ProvideCommonCompositionLocals(
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
        LocalFontLoader providesDefault owner.fontLoader,
        LocalFontFamilyResolver providesDefault owner.fontFamilyResolver,
        LocalHapticFeedback provides owner.hapticFeedBack,
        LocalInputModeManager provides owner.inputModeManager,
        LocalLayoutDirection provides owner.layoutDirection,
        LocalTextInputService provides owner.textInputService,
        LocalSoftwareKeyboardController provides owner.softwareKeyboardController,
        LocalTextToolbar provides owner.textToolbar,
        LocalUriHandler provides uriHandler,
        LocalViewConfiguration provides owner.viewConfiguration,
        LocalWindowInfo provides owner.windowInfo,
        LocalPointerIconService provides owner.pointerIconService,
        LocalGraphicsContext provides owner.graphicsContext,
        LocalRetainedValuesStore provides owner.retainedValuesStore,
        content = content,
    )
}

/**
 * Composes the given composable into [RootNodeOwner]
 *
 * @param parent The parent composition reference to coordinate scheduling of composition updates
 *        If null then default root composition will be used.
 * @param getCompositionLocalContext getter for retrieving the top-level composition local context.
 * Can be backed by `mutableStateOf` to dynamically change top-level locals.
 * @param content A `@Composable` function declaring the UI contents
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun RootNodeOwner.setContent(
    parent: CompositionContext,
    getCompositionLocalContext: () -> CompositionLocalContext? = { null },
    content: @Composable () -> Unit
): Composition {
    val composition = Composition(DefaultUiApplier(owner.root), parent)
    composition.setContent {
        getCompositionLocalContext().provide {
            ProvideCommonCompositionLocals(
                owner = owner,
                uriHandler = remember { PlatformUriHandler() },
                content = content
            )
        }
    }
    return composition
}

@Composable
private fun CompositionLocalContext?.provide(content: @Composable () -> Unit) {
    if (this != null) {
        CompositionLocalProvider(this, content = content)
    } else {
        content()
    }
}
