LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_USE_AAPT2 := true

LOCAL_MODULE_TAGS := samples tests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := SupportVectorDrawable

LOCAL_STATIC_ANDROID_LIBRARIES := android-support-vectordrawable android-support-v4

LOCAL_SDK_VERSION := current

LOCAL_MIN_SDK_VERSION := 7

LOCAL_AAPT_FLAGS += --no-version-vectors

include $(BUILD_PACKAGE)

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
