/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.processing.util;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.processing.ShaderProvider;
import androidx.core.util.Preconditions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for OpenGL ES.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class GLUtils {

    /** Unknown version information. */
    public static final String VERSION_UNKNOWN = "0.0";

    public static final String TAG = "GLUtils";

    public static final int EGL_GL_COLORSPACE_KHR = 0x309D;
    public static final int EGL_GL_COLORSPACE_BT2020_HLG_EXT = 0x3540;

    public static final String VAR_TEXTURE_COORD = "vTextureCoord";
    public static final String VAR_TEXTURE = "sTexture";
    public static final String VAR_TEXTURE_YUV = "sTextureYuv";
    // SAMPLER_SELECTOR_UNKNOWN must be 0 to correctly initialize HDR shader uniform selector
    public static final int SAMPLER_SELECTOR_UNKNOWN = 0;
    public static final int SAMPLER_SELECTOR_DEFAULT = 1;
    public static final int SAMPLER_SELECTOR_YUV = 2;
    public static final int PIXEL_STRIDE = 4;
    public static final int[] EMPTY_ATTRIBS = {EGL14.EGL_NONE};
    public static final int[] HLG_SURFACE_ATTRIBS = {
            EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_HLG_EXT,
            EGL14.EGL_NONE
    };

    public static final String DEFAULT_VERTEX_SHADER = String.format(Locale.US,
            "uniform mat4 uTexMatrix;\n"
                    + "uniform mat4 uTransMatrix;\n"
                    + "attribute vec4 aPosition;\n"
                    + "attribute vec4 aTextureCoord;\n"
                    + "varying vec2 %s;\n"
                    + "void main() {\n"
                    + "    gl_Position = uTransMatrix * aPosition;\n"
                    + "    %s = (uTexMatrix * aTextureCoord).xy;\n"
                    + "}\n", VAR_TEXTURE_COORD, VAR_TEXTURE_COORD);

    public static final String HDR_VERTEX_SHADER = String.format(Locale.US,
            "#version 300 es\n"
                    + "in vec4 aPosition;\n"
                    + "in vec4 aTextureCoord;\n"
                    + "uniform mat4 uTexMatrix;\n"
                    + "uniform mat4 uTransMatrix;\n"
                    + "out vec2 %s;\n"
                    + "void main() {\n"
                    + "  gl_Position = uTransMatrix * aPosition;\n"
                    + "  %s = (uTexMatrix * aTextureCoord).xy;\n"
                    + "}\n", VAR_TEXTURE_COORD, VAR_TEXTURE_COORD);

    public static final String DEFAULT_FRAGMENT_SHADER = String.format(Locale.US,
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "varying vec2 %s;\n"
                    + "uniform samplerExternalOES %s;\n"
                    + "uniform float uAlphaScale;\n"
                    + "void main() {\n"
                    + "    vec4 src = texture2D(%s, %s);\n"
                    + "    gl_FragColor = vec4(src.rgb, src.a * uAlphaScale);\n"
                    + "}\n", VAR_TEXTURE_COORD, VAR_TEXTURE, VAR_TEXTURE, VAR_TEXTURE_COORD);

    public static final String HDR_FRAGMENT_SHADER = String.format(Locale.US,
            "#version 300 es\n"
                    + "#extension GL_OES_EGL_image_external_essl3 : require\n"
                    + "#extension GL_EXT_YUV_target : require\n"
                    + "precision mediump float;\n"
                    + "uniform samplerExternalOES %s;\n"
                    + "uniform __samplerExternal2DY2YEXT %s;\n"
                    + "uniform int uSamplerSelector;\n"
                    + "uniform float uAlphaScale;\n"
                    + "in vec2 %s;\n"
                    + "out vec4 outColor;\n"
                    + "\n"
                    + "vec3 yuvToRgb(vec3 yuv) {\n"
                    + "  const vec3 yuvOffset = vec3(0.0625, 0.5, 0.5);\n"
                    + "  const mat3 yuvToRgbColorTransform = mat3(\n"
                    + "    1.1689f, 1.1689f, 1.1689f,\n"
                    + "    0.0000f, -0.1881f, 2.1502f,\n"
                    + "    1.6853f, -0.6530f, 0.0000f\n"
                    + "  );\n"
                    + "  return clamp(yuvToRgbColorTransform * (yuv - yuvOffset), 0.0, 1.0);\n"
                    + "}\n"
                    + "\n"
                    + "void main() {\n"
                    + "  vec4 src = vec4(0.0);\n"
                    + "  if (uSamplerSelector == %d) {\n"
                    + "    src = texture(%s, %s);\n"
                    + "  } else if (uSamplerSelector == %d) {\n"
                    + "    vec3 srcYuv = texture(%s, %s).xyz;\n"
                    + "    src = vec4(yuvToRgb(srcYuv), 1.0);\n"
                    + "  }\n"
                    + "  outColor = vec4(src.rgb, src.a * uAlphaScale);\n"
                    + "}",
            VAR_TEXTURE,
            VAR_TEXTURE_YUV,
            VAR_TEXTURE_COORD,
            SAMPLER_SELECTOR_DEFAULT,
            VAR_TEXTURE,
            VAR_TEXTURE_COORD,
            SAMPLER_SELECTOR_YUV,
            VAR_TEXTURE_YUV,
            VAR_TEXTURE_COORD
    );

    public static final float[] VERTEX_COORDS = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,    // 1 bottom right
            -1.0f, 1.0f,   // 2 top left
            1.0f, 1.0f,    // 3 top right
    };
    public static final FloatBuffer VERTEX_BUF = createFloatBuffer(VERTEX_COORDS);

    public static final float[] TEX_COORDS = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };
    public static final FloatBuffer TEX_BUF = createFloatBuffer(TEX_COORDS);

    public static final int SIZEOF_FLOAT = 4;
    public static final OutputSurface NO_OUTPUT_SURFACE =
            OutputSurface.of(EGL14.EGL_NO_SURFACE, 0, 0);

    public enum InputFormat {
        UNKNOWN(SAMPLER_SELECTOR_UNKNOWN),
        DEFAULT(SAMPLER_SELECTOR_DEFAULT),
        YUV(SAMPLER_SELECTOR_YUV);

        private final int mSamplerSelector;

        InputFormat(int samplerSelector) {
            mSamplerSelector = samplerSelector;
        }

        public int getSamplerSelector() {
            return mSamplerSelector;
        }
    }

    private GLUtils() {
    }

    /**
     * Creates an {@link EGLSurface}.
     */
    @NonNull
    public static EGLSurface createWindowSurface(@NonNull EGLDisplay eglDisplay,
            @NonNull EGLConfig eglConfig, @NonNull Surface surface, @NonNull int[] surfaceAttrib) {
        // Create a window surface, and attach it to the Surface we received.
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface,
                surfaceAttrib, /*offset=*/0);
        checkEglErrorOrThrow("eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new IllegalStateException("surface was null");
        }
        return eglSurface;
    }

    /**
     * Creates the vertex or fragment shader.
     */
    public static int loadShader(int shaderType, @NonNull String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlErrorOrThrow("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, /*offset=*/0);
        if (compiled[0] == 0) {
            Logger.w(TAG, "Could not compile shader: " + source);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException(
                    "Could not compile shader type " + shaderType + ":" + GLES20.glGetShaderInfoLog(
                            shader));
        }
        return shader;
    }

    /**
     * Queries the {@link EGLSurface} information.
     */
    public static int querySurface(@NonNull EGLDisplay eglDisplay, @NonNull EGLSurface eglSurface,
            int what) {
        int[] value = new int[1];
        EGL14.eglQuerySurface(eglDisplay, eglSurface, what, value, /*offset=*/0);
        return value[0];
    }

    /**
     * Gets the size of {@link EGLSurface}.
     */
    @NonNull
    public static Size getSurfaceSize(@NonNull EGLDisplay eglDisplay,
            @NonNull EGLSurface eglSurface) {
        int width = querySurface(eglDisplay, eglSurface, EGL14.EGL_WIDTH);
        int height = querySurface(eglDisplay, eglSurface, EGL14.EGL_HEIGHT);
        return new Size(width, height);
    }

    /**
     * Creates a {@link FloatBuffer}.
     */
    @NonNull
    public static FloatBuffer createFloatBuffer(@NonNull float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    /**
     * Creates a new EGL pixel buffer surface.
     */
    @SuppressWarnings("SameParameterValue") // currently hard code width/height with 1/1
    @NonNull
    public static EGLSurface createPBufferSurface(@NonNull EGLDisplay eglDisplay,
            @NonNull EGLConfig eglConfig, int width, int height) {
        int[] surfaceAttrib = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttrib,
                /*offset=*/0);
        checkEglErrorOrThrow("eglCreatePbufferSurface");
        if (eglSurface == null) {
            throw new IllegalStateException("surface was null");
        }
        return eglSurface;
    }

    /**
     * Creates a program object based on shaders.
     */
    public static int createProgram(@NonNull DynamicRange dynamicRange,
            @NonNull ShaderProvider shaderProvider) {
        int vertexShader = -1;
        int fragmentShader = -1;
        int program = -1;
        try {
            vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
                    dynamicRange.is10BitHdr() ? HDR_VERTEX_SHADER : DEFAULT_VERTEX_SHADER);
            fragmentShader = loadFragmentShader(dynamicRange, shaderProvider);
            program = GLES20.glCreateProgram();
            checkGlErrorOrThrow("glCreateProgram");
            GLES20.glAttachShader(program, vertexShader);
            checkGlErrorOrThrow("glAttachShader");
            GLES20.glAttachShader(program, fragmentShader);
            checkGlErrorOrThrow("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, /*offset=*/0);
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

    /**
     * Creates a texture.
     */
    public static int createTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlErrorOrThrow("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texId);
        checkGlErrorOrThrow("glBindTexture " + texId);

        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlErrorOrThrow("glTexParameter");
        return texId;
    }

    /**
     * Gets texture required image unites.
     */
    public static int getTexNumUnits() {
        // Collect the required texture units for GL_TEXTURE_EXTERNAL_OES so we know
        // which texture unit we can use to bind the YUV sampler. The documentation says
        // GL_TEXTURE_EXTERNAL_OES can only use a maximum of 3 texture units, so default
        // to that if we can't query the required units
        int requiredUnits = -1;
        try {
            int[] texParams = new int[1];
            GLES30.glGetTexParameteriv(
                    GL_TEXTURE_EXTERNAL_OES,
                    GLES11Ext.GL_REQUIRED_TEXTURE_IMAGE_UNITS_OES,
                    texParams, 0
            );
            checkGlErrorOrThrow("glGetTexParameteriv");
            if (texParams[0] >= 0 && texParams[0] <= 3) {
                requiredUnits = texParams[0];
            } else {
                Log.e(TAG, "Query for GL_REQUIRED_TEXTURE_IMAGE_UNITS_OES returned out of"
                        + " bounds size: " + texParams[0] + ". Defaulting to 3.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to query GL_REQUIRED_TEXTURE_IMAGE_UNITS_OES", e);
        }

        return requiredUnits != -1 ? requiredUnits : 3;
    }

    /**
     * Creates a 4x4 identity matrix.
     */
    @NonNull
    public static float[] create4x4IdentityMatrix() {
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, /* smOffset= */ 0);
        return matrix;
    }

    /**
     * Checks the location error.
     */
    public static void checkLocationOrThrow(int location, @NonNull String label) {
        if (location < 0) {
            throw new IllegalStateException("Unable to locate '" + label + "' in program");
        }
    }

    /**
     * Checks the egl error and throw.
     */
    public static void checkEglErrorOrThrow(@NonNull String op) {
        int error = EGL14.eglGetError();
        if (error != EGL14.EGL_SUCCESS) {
            throw new IllegalStateException(op + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    /**
     * Checks the gl error and throw.
     */
    public static void checkGlErrorOrThrow(@NonNull String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            throw new IllegalStateException(op + ": GL error 0x" + Integer.toHexString(error));
        }
    }

    /**
     * Checks the egl error and log.
     */
    public static void checkEglErrorOrLog(@NonNull String op) {
        try {
            checkEglErrorOrThrow(op);
        } catch (IllegalStateException e) {
            Logger.e(TAG, e.toString(), e);
        }
    }

    /**
     * Checks the initialization status.
     */
    public static void checkInitializedOrThrow(@NonNull AtomicBoolean initialized,
            boolean shouldInitialized) {
        boolean result = shouldInitialized == initialized.get();
        String message = shouldInitialized ? "OpenGlRenderer is not initialized"
                : "OpenGlRenderer is already initialized";
        Preconditions.checkState(result, message);
    }

    /**
     * Checks the gl thread.
     */
    public static void checkGlThreadOrThrow(@Nullable Thread thread) {
        Preconditions.checkState(thread == Thread.currentThread(),
                "Method call must be called on the GL thread.");
    }

    /**
     * Gets the gl version number.
     */
    @NonNull
    public static String getGlVersionNumber() {
        // Logic adapted from CTS Egl14Utils:
        // https://cs.android.com/android/platform/superproject/+/master:cts/tests/tests/opengl/src/android/opengl/cts/Egl14Utils.java;l=46;drc=1c705168ab5118c42e5831cd84871d51ff5176d1
        String glVersion = GLES20.glGetString(GLES20.GL_VERSION);
        Pattern pattern = Pattern.compile("OpenGL ES ([0-9]+)\\.([0-9]+).*");
        Matcher matcher = pattern.matcher(glVersion);
        if (matcher.find()) {
            String major = Preconditions.checkNotNull(matcher.group(1));
            String minor = Preconditions.checkNotNull(matcher.group(2));
            return major + "." + minor;
        }
        return VERSION_UNKNOWN;
    }

    /**
     * Chooses the surface attributes for HDR 10bit.
     */
    @NonNull
    public static int[] chooseSurfaceAttrib(@NonNull String eglExtensions,
            @NonNull DynamicRange dynamicRange) {
        int[] attribs = EMPTY_ATTRIBS;
        if (dynamicRange.getEncoding() == DynamicRange.ENCODING_HLG) {
            if (eglExtensions.contains("EGL_EXT_gl_colorspace_bt2020_hlg")) {
                attribs = HLG_SURFACE_ATTRIBS;
            } else {
                Logger.w(TAG, "Dynamic range uses HLG encoding, but "
                        + "device does not support EGL_EXT_gl_colorspace_bt2020_hlg."
                        + "Fallback to default colorspace.");
            }
        }
        // TODO(b/303675500): Add path for PQ (EGL_EXT_gl_colorspace_bt2020_pq) output for
        //  HDR10/HDR10+
        return attribs;
    }

    /**
     * Generates framebuffer object.
     */
    public static int generateFbo() {
        int[] fbos = new int[1];
        GLES20.glGenFramebuffers(1, fbos, 0);
        checkGlErrorOrThrow("glGenFramebuffers");
        return fbos[0];
    }

    /**
     * Generates texture.
     */
    public static int generateTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlErrorOrThrow("glGenTextures");
        return textures[0];
    }

    /**
     * Deletes texture.
     */
    public static void deleteTexture(int texture) {
        int[] textures = {texture};
        GLES20.glDeleteTextures(1, textures, 0);
        checkGlErrorOrThrow("glDeleteTextures");
    }

    /**
     * Deletes framebuffer object.
     */
    public static void deleteFbo(int fbo) {
        int[] fbos = {fbo};
        GLES20.glDeleteFramebuffers(1, fbos, 0);
        checkGlErrorOrThrow("glDeleteFramebuffers");
    }

    private static int loadFragmentShader(@NonNull DynamicRange dynamicRange,
            @NonNull ShaderProvider shaderProvider) {
        if (shaderProvider == ShaderProvider.DEFAULT) {
            return loadShader(GLES20.GL_FRAGMENT_SHADER,
                    dynamicRange.is10BitHdr() ? HDR_FRAGMENT_SHADER : DEFAULT_FRAGMENT_SHADER);
        } else {
            // Throw IllegalArgumentException if the shader provider can not provide a valid
            // fragment shader.
            String source;
            try {
                source = shaderProvider.createFragmentShader(VAR_TEXTURE, VAR_TEXTURE_COORD);
                // A simple check to workaround custom shader doesn't contain required variable.
                // See b/241193761.
                if (source == null || !source.contains(VAR_TEXTURE_COORD) || !source.contains(
                        VAR_TEXTURE)) {
                    throw new IllegalArgumentException("Invalid fragment shader");
                }
                return loadShader(GLES20.GL_FRAGMENT_SHADER, source);
            } catch (Throwable t) {
                if (t instanceof IllegalArgumentException) {
                    throw t;
                }
                throw new IllegalArgumentException("Unable to compile fragment shader", t);
            }
        }
    }
}
