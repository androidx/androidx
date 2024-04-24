/*
 * Copyright 2020 The Android Open Source Project
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

#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <EGL/eglplatform.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <jni.h>

#include <cassert>
#include <iomanip>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

namespace {
    auto constexpr LOG_TAG = "OpenGLRendererJni";

    std::string GLErrorString(GLenum error) {
        switch (error) {
            case GL_NO_ERROR:
                return "GL_NO_ERROR";
            case GL_INVALID_ENUM:
                return "GL_INVALID_ENUM";
            case GL_INVALID_VALUE:
                return "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION:
                return "GL_INVALID_OPERATION";
            case GL_STACK_OVERFLOW_KHR:
                return "GL_STACK_OVERFLOW";
            case GL_STACK_UNDERFLOW_KHR:
                return "GL_STACK_UNDERFLOW";
            case GL_OUT_OF_MEMORY:
                return "GL_OUT_OF_MEMORY";
            case GL_INVALID_FRAMEBUFFER_OPERATION:
                return "GL_INVALID_FRAMEBUFFER_OPERATION";
            default: {
                std::ostringstream oss;
                oss << "<Unknown GL Error 0x" << std::setfill('0') <<
                    std::setw(4) << std::right << std::hex << error << ">";
                return oss.str();
            }
        }
    }

    std::string EGLErrorString(EGLenum error) {
        switch (error) {
            case EGL_SUCCESS:
                return "EGL_SUCCESS";
            case EGL_NOT_INITIALIZED:
                return "EGL_NOT_INITIALIZED";
            case EGL_BAD_ACCESS:
                return "EGL_BAD_ACCESS";
            case EGL_BAD_ALLOC:
                return "EGL_BAD_ALLOC";
            case EGL_BAD_ATTRIBUTE:
                return "EGL_BAD_ATTRIBUTE";
            case EGL_BAD_CONTEXT:
                return "EGL_BAD_CONTEXT";
            case EGL_BAD_CONFIG:
                return "EGL_BAD_CONFIG";
            case EGL_BAD_CURRENT_SURFACE:
                return "EGL_BAD_CURRENT_SURFACE";
            case EGL_BAD_DISPLAY:
                return "EGL_BAD_DISPLAY";
            case EGL_BAD_SURFACE:
                return "EGL_BAD_SURFACE";
            case EGL_BAD_MATCH:
                return "EGL_BAD_MATCH";
            case EGL_BAD_PARAMETER:
                return "EGL_BAD_PARAMETER";
            case EGL_BAD_NATIVE_PIXMAP:
                return "EGL_BAD_NATIVE_PIXMAP";
            case EGL_BAD_NATIVE_WINDOW:
                return "EGL_BAD_NATIVE_WINDOW";
            case EGL_CONTEXT_LOST:
                return "EGL_CONTEXT_LOST";
            default: {
                std::ostringstream oss;
                oss << "<Unknown EGL Error 0x" << std::setfill('0') <<
                    std::setw(4) << std::right << std::hex << error << ">";
                return oss.str();
            }
        }
    }
}

#ifdef NDEBUG
#define CHECK_GL(gl_func) [&]() { return gl_func; }()
#else
namespace {
    class CheckGlErrorOnExit {
    public:
        explicit CheckGlErrorOnExit(std::string glFunStr, unsigned int lineNum) :
                mGlFunStr(std::move(glFunStr)),
                mLineNum(lineNum) {}

        ~CheckGlErrorOnExit() {
            GLenum err = glGetError();
            if (err != GL_NO_ERROR) {
                __android_log_assert(nullptr, LOG_TAG, "OpenGL Error: %s at %s [%s:%d]",
                                     GLErrorString(err).c_str(), mGlFunStr.c_str(), __FILE__,
                                     mLineNum);
            }
        }

        CheckGlErrorOnExit(const CheckGlErrorOnExit &) = delete;

        CheckGlErrorOnExit &operator=(const CheckGlErrorOnExit &) = delete;

    private:
        std::string mGlFunStr;
        unsigned int mLineNum;
    };  // class CheckGlErrorOnExit
}   // namespace
#define CHECK_GL(glFunc)                                                    \
  [&]() {                                                                   \
    auto assertOnExit = CheckGlErrorOnExit(#glFunc, __LINE__);              \
    return glFunc;                                                          \
  }()
#endif

namespace {
    // Must be kept in sync with constants with same name in OpenGLRenderer.java
    enum RendererDynamicRange : jint {
        RENDERER_DYN_RNG_SDR = 1,     // Equivalent to DynamicRange.ENCODING_SDR
        RENDERER_DYN_RNG_HDR_HLG = 3  // Equivalent to DynamicRange.ENCODING_HLG
    };

    constexpr char VERTEX_SHADER_SRC[] = R"SRC(
      attribute vec4 position;
      attribute vec4 texCoords;
      uniform mat4 mvpTransform;
      uniform mat4 texTransform;
      varying vec2 fragCoord;
      void main() {
        fragCoord = (texTransform * texCoords).xy;
        gl_Position = mvpTransform * position;
      }
)SRC";

    constexpr char FRAGMENT_SHADER_SRC[] = R"SRC(
      #extension GL_OES_EGL_image_external : require
      precision mediump float;
      uniform samplerExternalOES sampler;
      varying vec2 fragCoord;
      void main() {
        gl_FragColor = texture2D(sampler, fragCoord);
      }
)SRC";

    constexpr char HDR_VERTEX_SHADER_SRC[] =
R"SRC(#version 300 es
      in vec4 position;
      in vec4 texCoords;
      uniform mat4 mvpTransform;
      uniform mat4 texTransform;
      out vec2 fragCoord;
      void main() {
        fragCoord = (texTransform * texCoords).xy;
        gl_Position = mvpTransform * position;
      }
)SRC";

    constexpr char HDR_FRAGMENT_SHADER_SRC[] =
R"SRC(#version 300 es
      #extension GL_OES_EGL_image_external : require
      #extension GL_EXT_YUV_target : require
      precision mediump float;
      uniform __samplerExternal2DY2YEXT sampler;
      in vec2 fragCoord;
      out vec4 outColor;

      vec3 yuvToRgb(vec3 yuv) {
        const vec3 yuvOffset = vec3(0.0625, 0.5, 0.5);
        const mat3 yuvToRgbColorTransform = mat3(
          1.1689f, 1.1689f, 1.1689f,
          0.0000f, -0.1881f, 2.1502f,
          1.6853f, -0.6530f, 0.0000f
        );
        return clamp(yuvToRgbColorTransform * (yuv - yuvOffset), 0.0, 1.0);
      }

      void main() {
        vec3 srcYuv = texture(sampler, fragCoord).xyz;
        outColor = vec4(yuvToRgb(srcYuv), 1.0);
      }
)SRC";

    struct NativeContext {
        EGLDisplay display;
        EGLConfig config;
        EGLContext context;
        std::pair<ANativeWindow *, EGLSurface> windowSurface;
        EGLSurface pbufferSurface;
        GLuint program;
        GLint positionHandle;
        GLint texCoordsHandle;
        GLint samplerHandle;
        GLint mvpTransformHandle;
        GLint texTransformHandle;
        GLuint textureId;
        bool supportsHdr;

        NativeContext()
                : display(EGL_NO_DISPLAY),
                  config(nullptr),
                  context(EGL_NO_CONTEXT),
                  windowSurface(std::make_pair(nullptr, EGL_NO_SURFACE)),
                  pbufferSurface(EGL_NO_SURFACE),
                  program(0),
                  positionHandle(-1),
                  texCoordsHandle(1),
                  samplerHandle(-1),
                  mvpTransformHandle(-1),
                  texTransformHandle(-1),
                  textureId(0),
                  supportsHdr(false) {}
    };

    const char *ShaderTypeString(GLenum shaderType) {
        switch (shaderType) {
            case GL_VERTEX_SHADER:
                return "GL_VERTEX_SHADER";
            case GL_FRAGMENT_SHADER:
                return "GL_FRAGMENT_SHADER";
            default:
                return "<Unknown shader type>";
        }
    }

    // Returns a handle to the shader
    GLuint CompileShader(GLenum shaderType, const char *shaderSrc) {
        GLuint shader = CHECK_GL(glCreateShader(shaderType));
        assert(shader);
        CHECK_GL(glShaderSource(shader, 1, &shaderSrc, /*length=*/nullptr));
        CHECK_GL(glCompileShader(shader));
        GLint compileStatus = 0;
        CHECK_GL(glGetShaderiv(shader, GL_COMPILE_STATUS, &compileStatus));
        if (!compileStatus) {
            GLint logLength = 0;
            CHECK_GL(glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &logLength));
            std::vector<char> logBuffer(logLength);
            if (logLength > 0) {
                CHECK_GL(glGetShaderInfoLog(shader, logLength, /*length=*/nullptr,
                                            &logBuffer[0]));
            }
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                                "Unable to compile %s shader:\n %s.",
                                ShaderTypeString(shaderType),
                                logLength > 0 ? &logBuffer[0] : "(unknown error)");
            CHECK_GL(glDeleteShader(shader));
            shader = 0;
        }
        assert(shader);
        return shader;
    }

    // Returns a handle to the output program
    GLuint CreateGlProgram(RendererDynamicRange dynamicRange) {

        GLuint vertexShader = CompileShader(GL_VERTEX_SHADER,
                                            dynamicRange != RENDERER_DYN_RNG_SDR
                                            ? HDR_VERTEX_SHADER_SRC : VERTEX_SHADER_SRC);
        assert(vertexShader);

        GLuint fragmentShader = CompileShader(GL_FRAGMENT_SHADER,
                                              dynamicRange != RENDERER_DYN_RNG_SDR
                                              ? HDR_FRAGMENT_SHADER_SRC : FRAGMENT_SHADER_SRC);
        assert(fragmentShader);

        GLuint program = CHECK_GL(glCreateProgram());
        assert(program);
        CHECK_GL(glAttachShader(program, vertexShader));
        CHECK_GL(glAttachShader(program, fragmentShader));
        CHECK_GL(glLinkProgram(program));
        GLint linkStatus = 0;
        CHECK_GL(glGetProgramiv(program, GL_LINK_STATUS, &linkStatus));
        if (!linkStatus) {
            GLint logLength = 0;
            CHECK_GL(glGetProgramiv(program, GL_INFO_LOG_LENGTH, &logLength));
            std::vector<char> logBuffer(logLength);
            if (logLength > 0) {
                CHECK_GL(glGetProgramInfoLog(program, logLength, /*length=*/nullptr,
                                             &logBuffer[0]));
            }
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                                "Unable to link program:\n %s.",
                                logLength > 0 ? &logBuffer[0] : "(unknown error)");
            CHECK_GL(glDeleteProgram(program));
            program = 0;
        }
        assert(program);
        return program;
    }

    void DestroySurface(NativeContext *nativeContext) {
        if (nativeContext->windowSurface.first) {
            eglMakeCurrent(nativeContext->display, nativeContext->pbufferSurface,
                           nativeContext->pbufferSurface, nativeContext->context);
            eglDestroySurface(nativeContext->display,
                              nativeContext->windowSurface.second);
            nativeContext->windowSurface.second = nullptr;
            ANativeWindow_release(nativeContext->windowSurface.first);
            nativeContext->windowSurface.first = nullptr;
        }
    }

    void ThrowException(JNIEnv *env, const char *exceptionName, const char *msg) {
        jclass exClass = env->FindClass(exceptionName);
        assert(exClass != nullptr);

        [[maybe_unused]] jint throwSuccess = env->ThrowNew(exClass, msg);
        assert(throwSuccess == JNI_OK);
    }

    bool InitContext(JNIEnv *env, NativeContext *nativeContext,
                      RendererDynamicRange dynamicRange,
                      int bitDepth) {
        EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        assert(eglDisplay != EGL_NO_DISPLAY);

        nativeContext->display = eglDisplay;

        EGLint majorVer;
        EGLint minorVer;
        EGLBoolean initSuccess = eglInitialize(eglDisplay, &majorVer, &minorVer);
        if (initSuccess != EGL_TRUE) {
            ThrowException(env, "java/lang/RuntimeException",
                           "EGL Error: eglInitialize failed.");
            return false;
        }

        // Print debug EGL information
        const char *eglVendorString = eglQueryString(eglDisplay, EGL_VENDOR);
        const char *eglVersionString = eglQueryString(eglDisplay, EGL_VERSION);
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "EGL Initialized [Vendor: %s, Version: %s]",
                            eglVendorString == nullptr ? "Unknown" : eglVendorString,
                            eglVersionString == nullptr
                            ? "Unknown" : eglVersionString);

        int renderType = dynamicRange != RENDERER_DYN_RNG_SDR
                ? EGL_OPENGL_ES3_BIT : EGL_OPENGL_ES2_BIT;

        // TODO(b/319277249): It will crash on older Samsung devices for HDR video 10-bi
        //  because EGLExt.EGL_RECORDABLE_ANDROID is only supported from OneUI 6.1. We need to
        //  check by GPU Driver version when new OS is release.
        int recordableAndroid = dynamicRange != RENDERER_DYN_RNG_SDR
                                ? EGL_DONT_CARE : EGL_TRUE;
        int configAttribs[] = { EGL_RED_SIZE, bitDepth,
                                EGL_GREEN_SIZE, bitDepth,
                                EGL_BLUE_SIZE, bitDepth,
                                EGL_ALPHA_SIZE, 32 - (bitDepth * 3),
                                EGL_DEPTH_SIZE, 0,
                                EGL_STENCIL_SIZE, 0,
                                EGL_RENDERABLE_TYPE,renderType,
                                EGL_SURFACE_TYPE,EGL_WINDOW_BIT | EGL_PBUFFER_BIT,
                                EGL_RECORDABLE_ANDROID,recordableAndroid,
                                EGL_NONE};
        EGLConfig eglConfig;
        EGLint numConfigs;
        EGLint configSize = 1;
        EGLBoolean chooseConfigSuccess =
                eglChooseConfig(eglDisplay, static_cast<EGLint *>(configAttribs), &eglConfig,
                                configSize, &numConfigs);
        if (chooseConfigSuccess != EGL_TRUE) {
            ThrowException(env, "java/lang/IllegalArgumentException",
                           "EGL Error: eglChooseConfig failed. ");
            return false;
        }

        assert(numConfigs > 0);

        nativeContext->config = eglConfig;

        int clientVer = dynamicRange != RENDERER_DYN_RNG_SDR ? 3 : 2;
        int contextAttribs[] = {EGL_CONTEXT_CLIENT_VERSION, clientVer, EGL_NONE};
        EGLContext eglContext = eglCreateContext(
                eglDisplay, eglConfig, EGL_NO_CONTEXT, static_cast<EGLint *>(contextAttribs));
        assert(eglContext != EGL_NO_CONTEXT);

        nativeContext->context = eglContext;

        // Create 1x1 pixmap to use as a surface until one is set.
        int pbufferAttribs[] = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};
        EGLSurface eglPbuffer =
                eglCreatePbufferSurface(eglDisplay, eglConfig, pbufferAttribs);
        assert(eglPbuffer != EGL_NO_SURFACE);

        nativeContext->pbufferSurface = eglPbuffer;

        eglMakeCurrent(eglDisplay, eglPbuffer, eglPbuffer, eglContext);

        //Print debug OpenGL information
        const GLubyte *glVendorString = CHECK_GL(glGetString(GL_VENDOR));
        const GLubyte *glVersionString = CHECK_GL(glGetString(GL_VERSION));
        const GLubyte *glslVersionString = CHECK_GL(glGetString(GL_SHADING_LANGUAGE_VERSION));
        const GLubyte *glRendererString = CHECK_GL(glGetString(GL_RENDERER));
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "OpenGL Initialized [Vendor: %s, "
                                                        "Version: %s, GLSL Version: %s, Renderer: "
                                                        "%s]",
                            glVendorString == nullptr ? "Unknown" : (const char *) glVendorString,
                            glVersionString == nullptr ? "Unknown" : (const char *) glVersionString,
                            glslVersionString == nullptr ? "Unknown"
                                                         : (const char *) glslVersionString,
                            glRendererString == nullptr ? "Unknown"
                                                        : (const char *) glRendererString);

        // Check for YUV target extension
        const char *glExtensions =
                reinterpret_cast<const char*>(CHECK_GL(glGetString(GL_EXTENSIONS)));
        bool hasYuvExtension = (strstr(glExtensions, "GL_EXT_YUV_target") != nullptr);

        // Check if OpenGL version is ES3.0 or greater
        GLuint major, minor;
        sscanf(reinterpret_cast<const char*>(glVersionString), "%d.%d", &major, &minor);
        GLuint verNum = (major * 100 + minor * 10);

        nativeContext->supportsHdr = hasYuvExtension && verNum >= 300;

        nativeContext->program = CreateGlProgram(dynamicRange);
        assert(nativeContext->program);

        nativeContext->positionHandle =
                CHECK_GL(glGetAttribLocation(nativeContext->program, "position"));
        assert(nativeContext->positionHandle != -1);

        nativeContext->texCoordsHandle =
                CHECK_GL(glGetAttribLocation(nativeContext->program, "texCoords"));
        assert(nativeContext->texCoordsHandle != -1);

        nativeContext->samplerHandle =
                CHECK_GL(glGetUniformLocation(nativeContext->program, "sampler"));
        assert(nativeContext->samplerHandle != -1);

        nativeContext->mvpTransformHandle =
                CHECK_GL(glGetUniformLocation(nativeContext->program, "mvpTransform"));
        assert(nativeContext->mvpTransformHandle != -1);

        nativeContext->texTransformHandle =
                CHECK_GL(glGetUniformLocation(nativeContext->program, "texTransform"));
        assert(nativeContext->texTransformHandle != -1);

        CHECK_GL(glGenTextures(1, &(nativeContext->textureId)));

        return nativeContext;
    }

    void ConnectOutputSurface(NativeContext *nativeContext, ANativeWindow *nativeWindow,
                              RendererDynamicRange dynamicRange) {
        std::vector<GLint> surfaceAttribs;
        const char* eglExtensions = eglQueryString(nativeContext->display, EGL_EXTENSIONS);
        if (dynamicRange == RENDERER_DYN_RNG_HDR_HLG) {
            if (strstr(eglExtensions, "EGL_EXT_gl_colorspace_bt2020_hlg") != nullptr) {
                surfaceAttribs.push_back(EGL_GL_COLORSPACE);
                surfaceAttribs.push_back(EGL_GL_COLORSPACE_BT2020_HLG_EXT);
            } else {
                __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
                                    "Dynamic range uses HLG encoding, but device does not "
                                    "support EGL_EXT_gl_colorspace_bt2020_hlg. Fallback to default "
                                    "colorspace.");
            }
            // TODO(b/303675500): Add path for PQ (EGL_EXT_gl_colorspace_bt2020_pq) output for
            //  HDR10/HDR10+
        }
        surfaceAttribs.push_back(EGL_NONE);

        EGLSurface surface =
                eglCreateWindowSurface(nativeContext->display, nativeContext->config,
                                       nativeWindow, &surfaceAttribs[0]);
        assert(surface != EGL_NO_SURFACE);

        nativeContext->windowSurface = std::make_pair(nativeWindow, surface);

        eglMakeCurrent(nativeContext->display, surface, surface,
                       nativeContext->context);

        CHECK_GL(glViewport(0, 0, ANativeWindow_getWidth(nativeWindow),
                            ANativeWindow_getHeight(nativeWindow)));
        CHECK_GL(glScissor(0, 0, ANativeWindow_getWidth(nativeWindow),
                           ANativeWindow_getHeight(nativeWindow)));
    }

    void ClearContext(NativeContext *nativeContext) {
        if (nativeContext->program) {
            CHECK_GL(glDeleteProgram(nativeContext->program));
            nativeContext->program = 0;
        }

        DestroySurface(nativeContext);

        if (nativeContext->pbufferSurface != EGL_NO_SURFACE) {
            eglDestroySurface(nativeContext->display, nativeContext->pbufferSurface);
            nativeContext->pbufferSurface = EGL_NO_SURFACE;
        }

        if (nativeContext->display != EGL_NO_DISPLAY) {
            eglMakeCurrent(nativeContext->display, EGL_NO_SURFACE, EGL_NO_SURFACE,
                           EGL_NO_CONTEXT);
        }

        if (nativeContext->context != EGL_NO_CONTEXT) {
            eglDestroyContext(nativeContext->display, nativeContext->context);
            nativeContext->context = EGL_NO_CONTEXT;
        }

        if (nativeContext->display != EGL_NO_DISPLAY) {
            eglTerminate(nativeContext->display);
            nativeContext->display = EGL_NO_DISPLAY;
        }
    }

}  // namespace

extern "C" {
JNIEXPORT jlong JNICALL
Java_androidx_camera_integration_core_OpenGLRenderer_initContext(
        JNIEnv *env, jclass clazz) {

    auto *nativeContext = new NativeContext();
    if (InitContext(env, nativeContext, RENDERER_DYN_RNG_SDR, /*bitDepth=*/8)) {
        return reinterpret_cast<jlong>(nativeContext);
    } else {
        return 0;
    }
}

JNIEXPORT jboolean JNICALL
Java_androidx_camera_integration_core_OpenGLRenderer_setWindowSurface(
        JNIEnv *env, jclass clazz, jlong context, jobject jsurface,
        jint jdynamicRange) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);
    auto dynamicRange = static_cast<RendererDynamicRange>(jdynamicRange);

    // Destroy previously connected surface
    DestroySurface(nativeContext);

    // Null surface may have just been passed in to destroy previous surface.
    if (!jsurface) {
        return JNI_FALSE;
    }

    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, jsurface);
    if (nativeWindow == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to set window surface: Unable to "
                                                        "acquire native window.");
        return JNI_FALSE;
    }

    ConnectOutputSurface(nativeContext, nativeWindow, dynamicRange);

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_androidx_camera_integration_core_OpenGLRenderer_getTexName(
        JNIEnv *env, jclass clazz, jlong context) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);
    return nativeContext->textureId;
}

JNIEXPORT jboolean JNICALL
Java_androidx_camera_integration_core_OpenGLRenderer_renderTexture(
        JNIEnv *env, jclass clazz, jlong context, jlong timestampNs,
        jfloatArray jmvpTransformArray, jboolean mvpDirty, jfloatArray jtexTransformArray) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);

    // We use two triangles drawn with GL_TRIANGLE_STRIP to create the surface which will be
    // textured with the camera frame. This could also be done with a quad (GL_QUADS) on a
    // different version of OpenGL or with a scaled single triangle in which we would inscribe
    // the camera texture.
    //
    //                       (-1,-1)         (1,-1)
    //                          +---------------+
    //                          | \_            |
    //                          |    \_         |
    //                          |       +       |
    //                          |         \_    |
    //                          |            \_ |
    //                          +---------------+
    //                       (-1,1)           (1,1)
    constexpr GLfloat vertices[] = {
            -1.0f, 1.0f, // Lower-left
            1.0f, 1.0f, // Lower-right
            -1.0f, -1.0f, // Upper-left (notice order here. We're drawing triangles, not a quad.)
            1.0f, -1.0f  // Upper-right
    };
    constexpr GLfloat texCoords[] = {
            0.0f, 0.0f, // Lower-left
            1.0f, 0.0f, // Lower-right
            0.0f, 1.0f, // Upper-left (order must match the vertices)
            1.0f, 1.0f,  // Upper-right
    };

    GLint vertexComponents = 2;
    GLenum vertexType = GL_FLOAT;
    GLboolean normalized = GL_FALSE;
    GLsizei vertexStride = 0;
    CHECK_GL(glVertexAttribPointer(nativeContext->positionHandle,
                                   vertexComponents, vertexType, normalized,
                                   vertexStride, vertices));
    CHECK_GL(glEnableVertexAttribArray(nativeContext->positionHandle));

    CHECK_GL(glVertexAttribPointer(nativeContext->texCoordsHandle,
                                   vertexComponents, vertexType, normalized,
                                   vertexStride, texCoords));
    CHECK_GL(glEnableVertexAttribArray(nativeContext->texCoordsHandle));

    CHECK_GL(glUseProgram(nativeContext->program));

    GLsizei numMatrices = 1;
    GLboolean transpose = GL_FALSE;
    // Only re-upload MVP to GPU if it is dirty
    if (mvpDirty) {
        GLfloat *mvpTransformArray =
                env->GetFloatArrayElements(jmvpTransformArray, nullptr);
        CHECK_GL(glUniformMatrix4fv(nativeContext->mvpTransformHandle, numMatrices,
                                    transpose, mvpTransformArray));
        env->ReleaseFloatArrayElements(jmvpTransformArray, mvpTransformArray,
                                       JNI_ABORT);
    }

    CHECK_GL(glUniform1i(nativeContext->samplerHandle, 0));

    numMatrices = 1;
    transpose = GL_FALSE;
    GLfloat *texTransformArray =
            env->GetFloatArrayElements(jtexTransformArray, nullptr);
    CHECK_GL(glUniformMatrix4fv(nativeContext->texTransformHandle, numMatrices,
                                transpose, texTransformArray));
    env->ReleaseFloatArrayElements(jtexTransformArray, texTransformArray,
                                   JNI_ABORT);

    CHECK_GL(glBindTexture(GL_TEXTURE_EXTERNAL_OES, nativeContext->textureId));

    // Required to use a left-handed coordinate system in order to match our world-space
    //
    //                    ________+x
    //                  /|
    //                 / |
    //              +z/  |
    //                   | +y
    //
    glFrontFace(GL_CW);

    // This will typically fail if the EGL surface has been detached abnormally. In that case we
    // will return JNI_FALSE below.
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    // Check that all GL operations completed successfully. If not, log an error and return.
    GLenum glError = glGetError();
    if (glError != GL_NO_ERROR) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                            "Failed to draw frame due to OpenGL error: %s",
                            GLErrorString(glError).c_str());
        return JNI_FALSE;
    }

// Only attempt to set presentation time if EGL_EGLEXT_PROTOTYPES is defined.
// Otherwise, we'll ignore the timestamp.
#ifdef EGL_EGLEXT_PROTOTYPES
    eglPresentationTimeANDROID(nativeContext->display,
                               nativeContext->windowSurface.second, timestampNs);
#endif  // EGL_EGLEXT_PROTOTYPES
    EGLBoolean swapped = eglSwapBuffers(nativeContext->display,
                                        nativeContext->windowSurface.second);
    if (!swapped) {
        EGLenum eglError = eglGetError();
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                            "Failed to swap buffers with EGL error: %s",
                            EGLErrorString(eglError).c_str());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_androidx_camera_integration_core_OpenGLRenderer_closeContext(
        JNIEnv *env, jclass clazz, jlong context) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);

    ClearContext(nativeContext);

    delete nativeContext;
}

JNIEXPORT void JNICALL
Java_androidx_camera_integration_core_OpenGLRenderer_updateRenderedDynamicRange(
        JNIEnv *env, jclass clazz, jlong context, jint jdynamicRange, jint bitDepth) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);
    auto dynamicRange = static_cast<RendererDynamicRange>(jdynamicRange);

    auto *nativeWindow = nativeContext->windowSurface.first;
    if (nativeWindow != nullptr) {
        ANativeWindow_acquire(nativeWindow);
    }
    ClearContext(nativeContext);

    InitContext(env, nativeContext, dynamicRange, bitDepth);
    if (nativeWindow != nullptr) {
        ConnectOutputSurface(nativeContext, nativeWindow, dynamicRange);
    }
}

JNIEXPORT jboolean JNICALL
Java_androidx_camera_integration_core_OpenGLRenderer_supportsHdr(
        JNIEnv *env, jclass clazz, jlong context) {
    auto *nativeContext = reinterpret_cast<NativeContext *>(context);
    return nativeContext->supportsHdr ? JNI_TRUE : JNI_TRUE;
}
}  // extern "C"

#undef CHECK_GL
