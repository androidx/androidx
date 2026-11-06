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

package androidx.compose.remote.creation.compose.shaders

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3.Companion.createIdentity

/** A remote representation of a shader that can be serialized and applied to a paint bundle. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class RemoteShader {
    /**
     * Applies this shader to the [paintBundle] using the provided [creationState].
     *
     * @param creationState The state of the remote creation session.
     * @param paintBundle The paint bundle to configure with this shader.
     */
    public abstract fun apply(creationState: RemoteComposeCreationState, paintBundle: PaintBundle)

    /**
     * The [RemoteMatrix3x3] to apply to the shader. Note not all profiles will support shader
     * rotation.
     */
    public open var remoteMatrix3x3: RemoteMatrix3x3 = createIdentity()
}
