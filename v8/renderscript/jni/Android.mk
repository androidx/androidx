LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CLANG := true
LOCAL_SDK_VERSION := 8

LOCAL_SRC_FILES:= \
    android_renderscript_RenderScript.cpp

LOCAL_SHARED_LIBRARIES := \
        libjnigraphics

LOCAL_STATIC_LIBRARIES := \
        libcutils \
        libRSDispatch


LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
	frameworks/rs \
	frameworks/rs/cpp

LOCAL_CFLAGS += -Wno-unused-parameter -U_FORTIFY_SOURCE

LOCAL_MODULE:= librsjni
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := libRSSupport
LOCAL_32_BIT_ONLY := true

LOCAL_LDFLAGS += -ldl

include $(BUILD_SHARED_LIBRARY)
