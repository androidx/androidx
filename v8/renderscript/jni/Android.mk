LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CLANG := true
LOCAL_SDK_VERSION := 14

LOCAL_SRC_FILES:= \
    android_rscompat_usage_io.cpp \
    android_rscompat_usage_io_driver.cpp

LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
	frameworks/rs \
	frameworks/rs/cpp \
	frameworks/rs/driver

LOCAL_CFLAGS += -Wno-unused-parameter -U_FORTIFY_SOURCE
LOCAL_CFLAGS += -DRS_COMPATIBILITY_LIB -std=c++11

LOCAL_MODULE:= libRSSupportIO
LOCAL_MODULE_TAGS := optional

LOCAL_LDLIBS += -landroid
LOCAL_NDK_STL_VARIANT := stlport_static
include $(BUILD_SHARED_LIBRARY)

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

LOCAL_CFLAGS += -Wno-unused-parameter -U_FORTIFY_SOURCE -std=c++11

LOCAL_MODULE:= librsjni
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := libRSSupport

LOCAL_LDFLAGS += -ldl -llog

include $(BUILD_SHARED_LIBRARY)
