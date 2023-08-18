/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.effects.opengl;

import static androidx.camera.effects.opengl.GlProgram.VERTEX_SIZE;

import android.opengl.EGL14;
import android.opengl.GLES20;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Utility methods for OpenGL.
 */
@RequiresApi(21)
class Utils {

    private static final String TAG = "GlUtils";

    private static final int SIZEOF_FLOAT = 4;

    private Utils() {
    }

    static void checkEglErrorOrLog(@NonNull String op) {
        try {
            checkEglErrorOrThrow(op);
        } catch (IllegalStateException e) {
            Logger.e(TAG, e.toString(), e);
        }
    }

    static void drawArrays(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        GLES20.glScissor(0, 0, width, height);

        // Draw the external texture to the intermediate texture.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_SIZE);
        checkGlErrorOrThrow("glDrawArrays");
    }

    static FloatBuffer createFloatBuffer(float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    static void checkGlErrorOrThrow(@NonNull String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            throw new IllegalStateException(op + ": GL error 0x" + Integer.toHexString(error));
        }
    }

    static void checkEglErrorOrThrow(@NonNull String op) {
        int error = EGL14.eglGetError();
        if (error != EGL14.EGL_SUCCESS) {
            throw new IllegalStateException(op + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    static void checkLocationOrThrow(int location, @NonNull String label) {
        if (location < 0) {
            throw new IllegalStateException("Unable to locate '" + label + "' in program");
        }
    }
}
