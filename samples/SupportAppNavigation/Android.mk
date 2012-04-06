LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := samples tests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := SupportAppNavigation

LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
