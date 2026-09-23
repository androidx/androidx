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

package androidx.compose.runtime.tracing.internal

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import androidx.annotation.RestrictTo

/**
 * Internal-only receiver that is used to determine whether recomposition tracing was enabled.
 * Practically, it is used as a persistent boolean flag that keeps its state between app restarts to
 * make sure we can use recomposition tracing during app startup.
 *
 * The enabled state is cleared on reinstall of the app, which is also a desired property.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RecompositionTracingEnabledReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // This receiver only exists for initializer to know if it has been
        // enabled via PackageManager.getComponentEnabledSetting(...).
    }

    internal companion object {
        fun enable(context: Context) = setEnabledSetting(context, true)

        fun disable(context: Context) = setEnabledSetting(context, false)

        private fun setEnabledSetting(context: Context, enabled: Boolean) {
            context.packageManager.setComponentEnabledSetting(
                context.componentName,
                if (enabled) COMPONENT_ENABLED_STATE_ENABLED else COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }

        fun isEnabled(context: Context): Boolean =
            context.packageManager.getComponentEnabledSetting(context.componentName) ==
                COMPONENT_ENABLED_STATE_ENABLED

        private val Context.componentName
            get() = ComponentName(this, RecompositionTracingEnabledReceiver::class.java.name)
    }
}
