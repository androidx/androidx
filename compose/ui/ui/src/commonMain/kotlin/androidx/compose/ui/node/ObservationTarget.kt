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

package androidx.compose.ui.node

import androidx.compose.ui.platform.ExperimentalComposeUiHostApi

/**
 * A target of snapshot read observation, passed to `ComposeUiHost.observeReads`.
 *
 * Hosts that observe reads on behalf of a Compose hierarchy can use [isObservationValid] to discard
 * observations whose target has been removed, for example with `SnapshotStateObserver.clearIf {
 * !(it as ObservationTarget).isObservationValid }`.
 */
@ExperimentalComposeUiHostApi
public interface ObservationTarget {
    /**
     * `true` while the target is part of the hierarchy. `false` once it has been removed, after
     * which its observations are no longer needed and can be discarded.
     */
    public val isObservationValid: Boolean
}
