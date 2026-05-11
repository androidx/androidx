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

package androidx.xr.runtime

/**
 * Represents a specific type of scene signal that can be enabled in the session configuration.
 *
 * @param id The integer value corresponding to this signal type.
 */
@PreviewSpatialApi
@ExperimentalSceneSignalApi
public class SceneSignalType private constructor(public val id: Int) {
    public companion object {
        /**
         * Scene signal type for conversation detection. Use this object to enable conversation
         * detection in [Config].
         */
        @JvmField public val CONVERSATION: SceneSignalType = SceneSignalType(0)
    }
}
