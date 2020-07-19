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

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.animation.rootAnimationClockFactory
import androidx.compose.Composable
import androidx.compose.ExperimentalComposeApi
import androidx.compose.Providers
import androidx.compose.ambientOf
import androidx.compose.getValue
import androidx.compose.neverEqualPolicy
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.compose.staticAmbientOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.ui.platform.AndroidUriHandler
import androidx.ui.platform.UriHandler
import androidx.compose.runtime.savedinstancestate.UiSavedStateRegistryAmbient
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/**
 * The Android [Configuration]. The [Configuration] is useful for determining how to organize the
 * UI.
 */
val ConfigurationAmbient = ambientOf<Configuration>(
    @OptIn(ExperimentalComposeApi::class)
    neverEqualPolicy()
)

/**
 * Provides a [Context] that can be used by Android applications.
 */
val ContextAmbient = staticAmbientOf<Context>()

/**
 * The ambient containing the current [LifecycleOwner].
 */
val LifecycleOwnerAmbient = staticAmbientOf<LifecycleOwner>()

/**
 * The ambient containing the current Compose [View].
 */
val ViewAmbient = staticAmbientOf<View>()

/**
 * The ambient containing the current [ViewModelStoreOwner].
 */
val ViewModelStoreOwnerAmbient = staticAmbientOf<ViewModelStoreOwner>()

@Composable
internal fun ProvideAndroidAmbients(owner: AndroidOwner, content: @Composable () -> Unit) {
    val context = owner.view.context
    var configuration by state(
        @OptIn(ExperimentalComposeApi::class)
        neverEqualPolicy()
    ) {
        context.applicationContext.resources.configuration
    }
    // onConfigurationChange is the correct hook to update configuration, however it is
    // possible that the configuration object itself may come from a wrapped
    // context / themed activity, and may not actually reflect the system. So instead we
    // use this hook to grab the applicationContext's configuration, which accurately
    // reflects the state of the application / system.
    owner.configurationChangeObserver = {
        configuration = context.applicationContext.resources.configuration
    }

    val uriHandler = remember { AndroidUriHandler(context) }
    val viewTreeOwners = owner.viewTreeOwners ?: throw IllegalStateException(
        "Called when the ViewTreeOwnersAvailability is not yet in Available state"
    )

    Providers(
        ConfigurationAmbient provides configuration,
        ContextAmbient provides context,
        LifecycleOwnerAmbient provides viewTreeOwners.lifecycleOwner,
        ViewAmbient provides owner.view,
        ViewModelStoreOwnerAmbient provides viewTreeOwners.viewModelStoreOwner
    ) {
        ProvideCommonAmbients(
            owner = owner,
            uriHandler = uriHandler,
            coroutineContext = Dispatchers.Main,
            content = content
        )
    }
}

// TODO(igotti): move back to Ambients.kt once Owner will be commonized.
@Composable
@OptIn(androidx.animation.InternalAnimationApi::class)
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