LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_SRC_FILES := $(call all-java-files-under, java)

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
LOCAL_JAVA_LIBRARIES := android.test.runner
LOCAL_PACKAGE_NAME := AndroidSupportTests

include $(BUILD_PACKAGE)
