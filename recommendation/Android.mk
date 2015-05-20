LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := 21

LOCAL_SRC_FILES := $(call all-java-files-under, src)


LOCAL_JAVA_LIBRARIES := \
    android-support-v4

LOCAL_MODULE := android-support-recommendation

include $(BUILD_STATIC_JAVA_LIBRARY)

# ===========================================================
# Common Droiddoc vars
recommendation.docs.src_files := \
    $(call all-java-files-under, src) \
    $(call all-html-files-under, src)
recommendation.docs.java_libraries := \
    framework \
    android-support-v4 \
    android-support-recommendation

# Documentation
# ===========================================================
include $(CLEAR_VARS)

LOCAL_MODULE := android-support-recommendation
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(recommendation.docs.src_files)

LOCAL_SDK_VERSION := 21
LOCAL_IS_HOST_MODULE := false
LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := build/tools/droiddoc/templates-sdk

LOCAL_JAVA_LIBRARIES := $(recommendation.docs.java_libraries)

LOCAL_DROIDDOC_OPTIONS := \
    -offlinemode \
    -hdf android.whichdoc offline \
    -federate Android http://developer.android.com \
    -federationapi Android prebuilts/sdk/api/21.txt \
    -hide 113

include $(BUILD_DROIDDOC)


# API Check
# ---------------------------------------------
support_module := $(LOCAL_MODULE)
support_module_api_dir := $(LOCAL_PATH)/api
support_module_src_files := $(recommendation.docs.src_files)
support_module_java_libraries := $(recommendation.docs.java_libraries)
support_module_java_packages := android.support.app.recommendation*
include $(SUPPORT_API_CHECK)
