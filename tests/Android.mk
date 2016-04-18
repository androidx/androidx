LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_SDK_VERSION := $(SUPPORT_CURRENT_SDK_VERSION)

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-test \
    android-support-v4 \
    mockito-target

LOCAL_PACKAGE_NAME := AndroidSupportTests

include $(BUILD_PACKAGE)
