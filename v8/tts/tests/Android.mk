LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_SRC_FILES := $(call all-java-files-under, java)
#LOCAL_SDK_VERSION := cur
LOCAL_STATIC_JAVA_LIBRARIES := mockito-target android-support-v8-tts
LOCAL_JAVA_LIBRARIES := android.test.runner
LOCAL_PACKAGE_NAME := android-support-v8-tts-tests

include $(BUILD_PACKAGE)
