/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.ui.core

/**
 * This class defines the set of signal options which can be set on a [SessionObserverFactory] which
 * is associated with a [SandboxedUiAdapter].
 */
public class SandboxedUiAdapterSignalOptions {
    public companion object {
        /**
         * When this signal option is set, information about the geometry of the UI container
         * hosting the [SandboxedUiAdapter.Session] will be sent through
         * [SessionObserver.onUiContainerChanged].
         *
         * The set of information that will be collected is:
         * [SandboxedSdkViewUiInfo.uiContainerWidth], [SandboxedSdkViewUiInfo.uiContainerHeight],
         * [SandboxedSdkViewUiInfo.onScreenGeometry] and
         * [SandboxedSdkViewUiInfo.uiContainerOpacityHint].
         */
        public const val GEOMETRY: String = "geometry"

        /**
         * When this signal option is set, information about obstructions on the UI container
         * hosting the [SandboxedUiAdapter.Session] will be sent through
         * [SessionObserver.onUiContainerChanged].
         *
         * The information that will be collected is [SandboxedSdkViewUiInfo.obstructedGeometry].
         */
        public const val OBSTRUCTIONS: String = "obstructions"
    }
}
