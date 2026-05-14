#include <jni.h>
#include <android/hardware_buffer_jni.h>
#include <android/hardware_buffer.h>
#include <android/log.h>
#define EGL_EGLEXT_PROTOTYPES
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <vector>
#include <cstring>
#include <dlfcn.h>

#define LOG_TAG "NativeHWBufferJNI"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

/**
 * Maps a PRIVATE format AHardwareBuffer to a CPU-accessible RGBA byte array using EGL and GLES.
 *
 * This implementation uses EGLImageKHR to wrap the hardware buffer as a GLES texture,
 * renders it to a Framebuffer Object (FBO), and then reads the pixels back using glReadPixels.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_androidx_camera_testing_impl_NativeHardwareBufferConverter_nativeConvertToRgba(
        JNIEnv* env, jclass clazz, jobject jHardwareBuffer, jbyteArray jRgbaArray) {
    void* libandroid = dlopen("libandroid.so", RTLD_NOW);
    if (!libandroid) {
        ALOGE("Failed to load libandroid.so");
        return JNI_FALSE;
    }

    typedef AHardwareBuffer* (*AHardwareBuffer_fromHardwareBuffer_t)(JNIEnv*, jobject);
    typedef void (*AHardwareBuffer_describe_t)(const AHardwareBuffer*, AHardwareBuffer_Desc*);

    auto fromHardwareBuffer = (AHardwareBuffer_fromHardwareBuffer_t)dlsym(
            libandroid, "AHardwareBuffer_fromHardwareBuffer");
    auto describe = (AHardwareBuffer_describe_t)dlsym(libandroid, "AHardwareBuffer_describe");

    struct Cleanup {
        void* lib;
        ~Cleanup() {
            if (lib) dlclose(lib);
        }
    } cleanup = {libandroid};

    if (!fromHardwareBuffer || !describe) {
        ALOGE("Failed to load AHardwareBuffer functions");
        return JNI_FALSE;
    }

    AHardwareBuffer* buffer = fromHardwareBuffer(env, jHardwareBuffer);
    if (!buffer) {
        return JNI_FALSE;
    }

    AHardwareBuffer_Desc desc;
    describe(buffer, &desc);

    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(display, nullptr, nullptr);

    EGLint configAttribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_NONE
    };

    EGLConfig config;
    EGLint numConfigs;
    eglChooseConfig(display, configAttribs, &config, 1, &numConfigs);

    EGLint contextAttribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };
    EGLContext context = eglCreateContext(display, config, EGL_NO_CONTEXT, contextAttribs);

    EGLint pbufferAttribs[] = {
        EGL_WIDTH, 1,
        EGL_HEIGHT, 1,
        EGL_NONE
    };
    EGLSurface surface = eglCreatePbufferSurface(display, config, pbufferAttribs);

    eglMakeCurrent(display, surface, surface, context);

    PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC eglGetNativeClientBufferANDROID =
            (PFNEGLGETNATIVECLIENTBUFFERANDROIDPROC)eglGetProcAddress(
                    "eglGetNativeClientBufferANDROID");
    if (!eglGetNativeClientBufferANDROID) {
        ALOGE("eglGetNativeClientBufferANDROID not supported");
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroySurface(display, surface);
        eglDestroyContext(display, context);
        return JNI_FALSE;
    }

    EGLClientBuffer clientBuffer = eglGetNativeClientBufferANDROID(buffer);
    EGLint eglImageAttribs[] = { EGL_IMAGE_PRESERVED_KHR, EGL_TRUE, EGL_NONE };

    PFNEGLCREATEIMAGEKHRPROC eglCreateImageKHR =
            (PFNEGLCREATEIMAGEKHRPROC)eglGetProcAddress("eglCreateImageKHR");
    PFNEGLDESTROYIMAGEKHRPROC eglDestroyImageKHR =
            (PFNEGLDESTROYIMAGEKHRPROC)eglGetProcAddress("eglDestroyImageKHR");
    PFNGLEGLIMAGETARGETTEXTURE2DOESPROC glEGLImageTargetTexture2DOES =
            (PFNGLEGLIMAGETARGETTEXTURE2DOESPROC)eglGetProcAddress("glEGLImageTargetTexture2DOES");

    if (!eglCreateImageKHR || !glEGLImageTargetTexture2DOES) {
        ALOGE("EGLImage extensions not supported");
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroySurface(display, surface);
        eglDestroyContext(display, context);
        return JNI_FALSE;
    }

    EGLImageKHR eglImage = eglCreateImageKHR(
            display, EGL_NO_CONTEXT, EGL_NATIVE_BUFFER_ANDROID, clientBuffer, eglImageAttribs);
    if (eglImage == EGL_NO_IMAGE_KHR) {
        ALOGE("Failed to create EGLImage");
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroySurface(display, surface);
        eglDestroyContext(display, context);
        return JNI_FALSE;
    }

    GLuint texture;
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture);
    glEGLImageTargetTexture2DOES(GL_TEXTURE_EXTERNAL_OES, eglImage);

    GLuint fbo, colorTex;
    glGenFramebuffers(1, &fbo);
    glBindFramebuffer(GL_FRAMEBUFFER, fbo);

    glGenTextures(1, &colorTex);
    glBindTexture(GL_TEXTURE_2D, colorTex);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, desc.width, desc.height, 0, GL_RGBA,
                 GL_UNSIGNED_BYTE, nullptr);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);

    const char* vertexShaderSrc =
        "attribute vec4 aPosition;\n"
        "attribute vec2 aTexCoord;\n"
        "varying vec2 vTexCoord;\n"
        "void main() {\n"
        "  gl_Position = aPosition;\n"
        "  vTexCoord = aTexCoord;\n"
        "}\n";

    const char* fragmentShaderSrc =
        "#extension GL_OES_EGL_image_external : require\n"
        "precision mediump float;\n"
        "varying vec2 vTexCoord;\n"
        "uniform samplerExternalOES sTexture;\n"
        "void main() {\n"
        "  gl_FragColor = texture2D(sTexture, vTexCoord);\n"
        "}\n";

    GLuint vs = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vs, 1, &vertexShaderSrc, nullptr);
    glCompileShader(vs);

    GLuint fs = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fs, 1, &fragmentShaderSrc, nullptr);
    glCompileShader(fs);

    GLuint program = glCreateProgram();
    glAttachShader(program, vs);
    glAttachShader(program, fs);
    glLinkProgram(program);
    glUseProgram(program);

    GLfloat vertices[] = {
        -1.0f, -1.0f, 0.0f, 0.0f,
         1.0f, -1.0f, 1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f, 1.0f,
         1.0f,  1.0f, 1.0f, 1.0f
    };

    GLint posLoc = glGetAttribLocation(program, "aPosition");
    GLint texLoc = glGetAttribLocation(program, "aTexCoord");

    glEnableVertexAttribArray(posLoc);
    glVertexAttribPointer(posLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), vertices);

    glEnableVertexAttribArray(texLoc);
    glVertexAttribPointer(texLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), vertices + 2);

    glViewport(0, 0, desc.width, desc.height);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    jsize arraySize = env->GetArrayLength(jRgbaArray);
    jboolean result = JNI_TRUE;
    if (arraySize >= desc.width * desc.height * 4) {
        std::vector<GLubyte> pixels(desc.width * desc.height * 4);
        glReadPixels(0, 0, desc.width, desc.height, GL_RGBA, GL_UNSIGNED_BYTE, pixels.data());
        env->SetByteArrayRegion(jRgbaArray, 0, pixels.size(), (jbyte*)pixels.data());
    } else {
        ALOGE("jRgbaArray is smaller than required size");
        result = JNI_FALSE;
    }

    glDeleteProgram(program);
    glDeleteShader(vs);
    glDeleteShader(fs);
    glDeleteFramebuffers(1, &fbo);
    glDeleteTextures(1, &colorTex);
    glDeleteTextures(1, &texture);

    if (eglDestroyImageKHR) {
        eglDestroyImageKHR(display, eglImage);
    }

    eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroySurface(display, surface);
    eglDestroyContext(display, context);

    return result;
}
