/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.processing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * A provider that supplies OpenGL shader code.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ShaderProvider {

    /**
     * Creates the fragment shader code with the given variable names.
     *
     * <p>The provider must use the variable names to construct the shader code, or it will fail
     * to create the OpenGL program when the provider is used. For example:
     * <pre>{@code
     *         #extension GL_OES_EGL_image_external : require
     *         precision mediump float;
     *         uniform samplerExternalOES {$samplerVarName};
     *         varying vec2 {$fragCoordsVarName};
     *         void main() {
     *           vec4 sampleColor = texture2D({$samplerVarName}, {$fragCoordsVarName});
     *           gl_FragColor = vec4(
     *               sampleColor.r * 0.5 + sampleColor.g * 0.8 + sampleColor.b * 0.3,
     *               sampleColor.r * 0.4 + sampleColor.g * 0.7 + sampleColor.b * 0.2,
     *               sampleColor.r * 0.3 + sampleColor.g * 0.5 + sampleColor.b * 0.1,
     *               1.0);
     *         }
     * }</pre>
     *
     * @param samplerVarName    the variable name of the samplerExternalOES.
     * @param fragCoordsVarName the variable name of the fragment coordinates.
     * @return the shader code. Return null to use the default shader.
     */
    @Nullable
    default String createFragmentShader(
            @NonNull String samplerVarName,
            @NonNull String fragCoordsVarName) {
        return null;
    }

    /** A default provider that will use the default shader code without any effect. */
    ShaderProvider DEFAULT = new ShaderProvider() {
        // Use default implementation.
    };
}
