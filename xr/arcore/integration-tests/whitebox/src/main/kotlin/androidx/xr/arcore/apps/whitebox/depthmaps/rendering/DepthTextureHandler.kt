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

package androidx.xr.arcore.apps.whitebox.depthmaps.rendering

import android.opengl.GLES30.GL_CLAMP_TO_EDGE
import android.opengl.GLES30.GL_FLOAT
import android.opengl.GLES30.GL_LINEAR
import android.opengl.GLES30.GL_R32F
import android.opengl.GLES30.GL_RED
import android.opengl.GLES30.GL_TEXTURE_2D
import android.opengl.GLES30.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES30.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES30.GL_TEXTURE_WRAP_S
import android.opengl.GLES30.GL_TEXTURE_WRAP_T
import android.opengl.GLES30.glBindTexture
import android.opengl.GLES30.glGenTextures
import android.opengl.GLES30.glTexImage2D
import android.opengl.GLES30.glTexParameteri
import androidx.xr.arcore.DepthMap
import androidx.xr.arcore.apps.whitebox.depthmaps.DepthMode

class DepthTextureHandler {

    public var depthTextureId: Int = -1
        private set

    /**
     * Creates and initializes the depth texture. This method needs to be called on a thread with a
     * EGL context attached.
     */
    public fun createOnGlThread() {
        val textureIds: IntArray = IntArray(1)
        glGenTextures(1, textureIds, 0)
        depthTextureId = textureIds[0]
        glBindTexture(GL_TEXTURE_2D, depthTextureId)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
    }

    public fun updateDepthTexture(depthMapState: DepthMap.State, depthMode: DepthMode) {
        glBindTexture(GL_TEXTURE_2D, depthTextureId)
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_R32F,
            depthMapState.width,
            depthMapState.height,
            0,
            GL_RED,
            GL_FLOAT,
            when (depthMode) {
                DepthMode.RAW -> depthMapState.rawDepthMap
                DepthMode.SMOOTH -> depthMapState.smoothDepthMap
            },
        )
    }
}
