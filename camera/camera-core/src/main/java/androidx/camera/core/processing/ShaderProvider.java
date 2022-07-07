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

/** A provider that supplies OpenGL shader code. */
interface ShaderProvider {

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
     *           gl_FragColor = vec4(sampleColor.r * 0.493 + sampleColor. g * 0.769 +
     *              sampleColor.b * 0.289, sampleColor.r * 0.449 + sampleColor.g * 0.686 +
     *              sampleColor.b * 0.268, sampleColor.r * 0.272 + sampleColor.g * 0.534 +
     *              sampleColor.b * 0.131, 1.0);
     *         }
     * }</pre>
     *
     * @param samplerVarName the variable name of the samplerExternalOES.
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
