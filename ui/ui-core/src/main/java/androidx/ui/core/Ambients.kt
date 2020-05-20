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

package androidx.ui.core

import androidx.animation.AnimationClockObservable
import androidx.animation.rootAnimationClockFactory
import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.ambientOf
import androidx.compose.remember
import androidx.compose.staticAmbientOf
import androidx.ui.autofill.Autofill
import androidx.ui.autofill.AutofillTree
import androidx.ui.core.clipboard.ClipboardManager
import androidx.ui.core.hapticfeedback.HapticFeedback
import androidx.ui.core.texttoolbar.TextToolbar
import androidx.ui.input.TextInputService
import androidx.ui.platform.UriHandler
import androidx.ui.savedinstancestate.UiSavedStateRegistryAmbient
import androidx.ui.text.font.Font
import androidx.ui.unit.Density
import kotlin.coroutines.CoroutineContext

/**
 * The default animation clock used for animations when an explicit clock isn't provided.
 */
val AnimationClockAmbient = staticAmbientOf<AnimationClockObservable>()

/**
 * The ambient that can be used to trigger autofill actions. Eg. [Autofill.requestAutofillForNode].
 */
val AutofillAmbient = staticAmbientOf<Autofill?>()

/**
 * The ambient that can be used to add
 * [AutofillNode][import androidx.ui.autofill.AutofillNode]s to the autofill tree. The
 * [AutofillTree] is a temporary data structure that will be replaced by Autofill Semantics
 * (b/138604305).
 */
val AutofillTreeAmbient = staticAmbientOf<AutofillTree>()

/**
 * The ambient to provide communication with platform clipboard service.
 */
val ClipboardManagerAmbient = staticAmbientOf<ClipboardManager>()

/**
 * Don't use this.
 * @suppress
 */
@Deprecated(message = "This will be replaced with something more appropriate when suspend works.")
val CoroutineContextAmbient = ambientOf<CoroutineContext>()

/**
 * Provides the [Density] to be used to transform between [density-independent pixel
 * units (DP)][androidx.ui.unit.Dp] and [pixel units][androidx.ui.unit.Px] or
 * [scale-independent pixel units (SP)][androidx.ui.unit.TextUnit] and
 * [pixel units][androidx.ui.unit.Px]. This is typically used when a [DP][androidx.ui.unit.Dp]
 * is provided and it must be converted in the body of [Layout] or [DrawModifier].
 */
val DensityAmbient = staticAmbientOf<Density>()

/**
 * The ambient to provide platform font loading methods.
 *
 * Use [androidx.ui.res.fontResource] instead.
 * @suppress
 */
val FontLoaderAmbient = staticAmbientOf<Font.ResourceLoader>()

/**
 * The ambient to provide haptic feedback to the user.
 */
val HapticFeedBackAmbient = staticAmbientOf<HapticFeedback>()

/**
 * The ambient to provide communication with platform text input service.
 */
val TextInputServiceAmbient = staticAmbientOf<TextInputService?>()

/**
 * The ambient to provide text-related toolbar.
 */
val TextToolbarAmbient = staticAmbientOf<TextToolbar>()

/**
 * The ambient to provide functionality related to URL, e.g. open URI.
 */
val UriHandlerAmbient = staticAmbientOf<UriHandler>()

@Composable
internal fun ProvideCommonAmbients(
    owner: Owner,
    uriHandler: UriHandler,
    coroutineContext: CoroutineContext,
    content: @Composable () -> Unit
) {
    val rootAnimationClock = remember { rootAnimationClockFactory() }

    Providers(
        AnimationClockAmbient provides rootAnimationClock,
        AutofillAmbient provides owner.autofill,
        AutofillTreeAmbient provides owner.autofillTree,
        ClipboardManagerAmbient provides owner.clipboardManager,
        @Suppress("DEPRECATION")
        CoroutineContextAmbient provides coroutineContext,
        DensityAmbient provides owner.density,
        FontLoaderAmbient provides owner.fontLoader,
        HapticFeedBackAmbient provides owner.hapticFeedBack,
        TextInputServiceAmbient provides owner.textInputService,
        TextToolbarAmbient provides owner.textToolbar,
        UiSavedStateRegistryAmbient provides owner.savedStateRegistry,
        UriHandlerAmbient provides uriHandler,
        children = content
    )
}
