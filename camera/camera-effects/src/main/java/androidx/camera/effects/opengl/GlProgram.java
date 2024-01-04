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

import static androidx.camera.effects.opengl.Utils.checkGlErrorOrThrow;
import static androidx.camera.effects.opengl.Utils.checkLocationOrThrow;
import static androidx.camera.effects.opengl.Utils.createFloatBuffer;
import static androidx.core.util.Preconditions.checkState;

import android.opengl.GLES20;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;

import java.nio.FloatBuffer;

/**
 * A base class that represents an OpenGL program.
 */
@RequiresApi(21)
public abstract class GlProgram {

    private static final String TAG = "GlProgram";

    static final String POSITION_ATTRIBUTE = "aPosition";
    static final String TEXTURE_ATTRIBUTE = "aTextureCoord";
    static final String TEXTURE_COORDINATES = "vTextureCoord";
    static final String INPUT_SAMPLER = "samplerInputTexture";

    // Used with {@Link #POSITION_ATTRIBUTE}
    private static final FloatBuffer VERTEX_BUFFER = createFloatBuffer(new float[]{
            -1.0f, -1.0f, // 0 bottom left
            1.0f, -1.0f, // 1 bottom right
            -1.0f, 1.0f, // 2 top left
            1.0f, 1.0f   // 3 top right
    });

    // Used with {@Link #TEXTURE_ATTRIBUTE}
    private static final FloatBuffer TEXTURE_BUFFER = createFloatBuffer(new float[]{
            0.0f, 0.0f, // 0 bottom left
            1.0f, 0.0f, // 1 bottom right
            0.0f, 1.0f, // 2 top left
            1.0f, 1.0f  // 3 top right
    });

    // The size of VERTEX_BUFFER and TEXTURE_BUFFER.
    static final int VERTEX_SIZE = 4;

    int mProgramHandle = -1;

    private final String mVertexShader;
    private final String mFragmentShader;

    GlProgram(@NonNull String vertexShader, @NonNull String programShader) {
        mVertexShader = vertexShader;
        mFragmentShader = programShader;
    }

    /**
     * Initializes this program.
     */
    void init() {
        checkState(!isInitialized(), "Program already initialized.");
        mProgramHandle = createProgram(mVertexShader, mFragmentShader);
        use();
        configure();
    }

    /**
     * Configures this program.
     *
     * <p>This base method configures attributes that are common to all programs. Each subclass
     * should override this method to add its own attributes.
     */
    @CallSuper
    protected void configure() {
        checkInitialized();

        // Configure the vertex of the 3D object (a quadrilateral).
        int positionLoc = GLES20.glGetAttribLocation(mProgramHandle, POSITION_ATTRIBUTE);
        checkLocationOrThrow(positionLoc, POSITION_ATTRIBUTE);
        GLES20.glEnableVertexAttribArray(positionLoc);
        checkGlErrorOrThrow("glEnableVertexAttribArray");
        int coordsPerVertex = 2;
        int vertexStride = 0;
        GLES20.glVertexAttribPointer(positionLoc, coordsPerVertex, GLES20.GL_FLOAT, false,
                vertexStride, VERTEX_BUFFER);
        checkGlErrorOrThrow("glVertexAttribPointer");

        // Configure the coordinate of the texture.
        int texCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, TEXTURE_ATTRIBUTE);
        checkLocationOrThrow(texCoordLoc, TEXTURE_ATTRIBUTE);
        GLES20.glEnableVertexAttribArray(texCoordLoc);
        checkGlErrorOrThrow("glEnableVertexAttribArray");
        int coordsPerTex = 2;
        int texStride = 0;
        GLES20.glVertexAttribPointer(texCoordLoc, coordsPerTex, GLES20.GL_FLOAT, false,
                texStride, TEXTURE_BUFFER);
        checkGlErrorOrThrow("glVertexAttribPointer");
    }

    /**
     * Uses this program.
     */
    @CallSuper
    protected final void use() {
        checkInitialized();
        GLES20.glUseProgram(mProgramHandle);
        checkGlErrorOrThrow("glUseProgram");
    }

    /**
     * Deletes this program and clears the state.
     *
     * <p>Subclasses should override this method to delete their own resources.
     */
    @CallSuper
    protected void release() {
        if (isInitialized()) {
            GLES20.glDeleteProgram(mProgramHandle);
            checkGlErrorOrThrow("glDeleteProgram");
            mProgramHandle = -1;
        }
    }

    private void checkInitialized() {
        checkState(isInitialized(), "Program not initialized");
    }

    private boolean isInitialized() {
        return mProgramHandle != -1;
    }

    private int createProgram(String vertexShaderStr, String fragmentShaderStr) {
        int vertexShader = -1, fragmentShader = -1, program = -1;
        try {
            program = GLES20.glCreateProgram();
            checkGlErrorOrThrow("glCreateProgram");

            vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderStr);
            GLES20.glAttachShader(program, vertexShader);
            checkGlErrorOrThrow("glAttachShader");

            fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderStr);
            GLES20.glAttachShader(program, fragmentShader);
            checkGlErrorOrThrow("glAttachShader");

            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                throw new IllegalStateException(
                        "Could not link program: " + GLES20.glGetProgramInfoLog(program));
            }
            return program;
        } catch (IllegalStateException | IllegalArgumentException e) {
            if (vertexShader != -1) {
                GLES20.glDeleteShader(vertexShader);
            }
            if (fragmentShader != -1) {
                GLES20.glDeleteShader(fragmentShader);
            }
            if (program != -1) {
                GLES20.glDeleteProgram(program);
            }
            throw e;
        }
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlErrorOrThrow("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Logger.w(TAG, "Could not compile shader: " + source);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException(
                    "Could not compile shader type " + shaderType + ":" + GLES20.glGetShaderInfoLog(
                            shader)
            );
        }
        return shader;
    }
}
