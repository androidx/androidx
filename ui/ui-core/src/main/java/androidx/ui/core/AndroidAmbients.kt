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
import androidx.compose.Composable
import androidx.compose.NeverEqual
import androidx.compose.Providers
import androidx.compose.ambientOf
import androidx.compose.getValue
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.compose.staticAmbientOf
import androidx.lifecycle.LifecycleOwner
import androidx.ui.platform.AndroidUriHandler
import kotlinx.coroutines.Dispatchers

/**
 * The Android [Configuration]. The [Configuration] is useful for determining how to organize the
 * UI.
 */
val ConfigurationAmbient = ambientOf<Configuration>(NeverEqual)

/**
 * Provides a [Context] that can be used by Android applications.
 */
val ContextAmbient = staticAmbientOf<Context>()

/**
 * The ambient containing the current [LifecycleOwner].
 */
val LifecycleOwnerAmbient = staticAmbientOf<LifecycleOwner>()

/**
 * Don't use this
 * @suppress
 */
// TODO(b/139866476): The Owner should not be exposed via ambient
@Deprecated(message = "This will be removed as of b/139866476")
val OwnerAmbient = staticAmbientOf<Owner>()

@Composable
internal fun ProvideAndroidAmbients(owner: AndroidOwner, content: @Composable () -> Unit) {
    val context = owner.view.context
    var configuration by state(NeverEqual) {
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

    Providers(
        ConfigurationAmbient provides configuration,
        ContextAmbient provides context,
        LifecycleOwnerAmbient provides requireNotNull(owner.lifecycleOwner),
        @Suppress("DEPRECATION")
        OwnerAmbient provides owner
    ) {
        ProvideCommonAmbients(
            owner = owner,
            uriHandler = uriHandler,
            coroutineContext = Dispatchers.Main,
            content = content
        )
    }
}
