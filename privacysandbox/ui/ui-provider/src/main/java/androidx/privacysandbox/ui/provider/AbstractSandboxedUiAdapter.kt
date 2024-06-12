/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.provider

import android.content.res.Configuration
import androidx.privacysandbox.ui.core.SandboxedUiAdapter

/**
 * An abstract class that implements [SandboxedUiAdapter] while abstracting away methods that do not
 * need to be implemented by a UI provider.
 *
 * UI providers should use this class rather than implementing [SandboxedUiAdapter] directly.
 */
abstract class AbstractSandboxedUiAdapter : SandboxedUiAdapter {

    /**
     * An abstract class that implements [SandboxedUiAdapter.Session] so that a UI provider does not
     * need to implement the entire interface.
     *
     * UI providers should use this class rather than implementing [SandboxedUiAdapter.Session].
     */
    abstract class AbstractSession : SandboxedUiAdapter.Session {

        override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {}

        override fun notifyResized(width: Int, height: Int) {}

        override fun notifyConfigurationChanged(configuration: Configuration) {}
    }
}
