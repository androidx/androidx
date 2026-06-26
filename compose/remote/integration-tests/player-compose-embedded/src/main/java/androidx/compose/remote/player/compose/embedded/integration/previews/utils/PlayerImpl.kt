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

package androidx.compose.remote.player.compose.embedded.integration.previews.utils

/** Selects which player implementation renders the captured document in the preview. */
public enum class PlayerImpl {
    /** The legacy player, RemoteDocumentPlayer wrapping the legacy View player. */
    JAVA,
    /** The new experimental Compose player, RcPlayer. */
    COMPOSE,
}
