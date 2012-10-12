
LOCAL_PATH:=$(call my-dir)
rs_base_CFLAGS := -Werror -Wall -Wno-unused-parameter -Wno-unused-variable \
		  -Wno-overloaded-virtual
ifeq ($(TARGET_BUILD_PDK), true)
  rs_base_CFLAGS += -D__RS_PDK__
endif

# Build rsg-generator ====================
include $(CLEAR_VARS)

LOCAL_MODULE := rsg-generator_support

# These symbols are normally defined by BUILD_XXX, but we need to define them
# here so that local-intermediates-dir works.

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
intermediates := $(local-intermediates-dir)
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES:= \
    spec.l \
    rsg_generator.c

include $(BUILD_HOST_EXECUTABLE)

# TODO: This should go into build/core/config.mk
RSG_GENERATOR_SUPPORT:=$(LOCAL_BUILT_MODULE)

include $(CLEAR_VARS)
LOCAL_CLANG := true
LOCAL_MODULE := libRSSupport
LOCAL_SDK_VERSION := $(rs_base_SDK_VERSION)

LOCAL_MODULE_CLASS := SHARED_LIBRARIES
intermediates:= $(local-intermediates-dir)

# Generate custom headers

GEN := $(addprefix $(intermediates)/, \
            rsgApiStructs.h \
            rsgApiFuncDecl.h \
        )

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL = $(RSG_GENERATOR_SUPPORT) $< $@ <$(PRIVATE_PATH)/rs.spec
$(GEN) : $(RSG_GENERATOR_SUPPORT) $(LOCAL_PATH)/rs.spec
$(GEN): $(intermediates)/%.h : $(LOCAL_PATH)/%.h.rsg
	$(transform-generated-source)

# used in jni/Android.mk
rs_generated_source += $(GEN)
LOCAL_GENERATED_SOURCES += $(GEN)

# Generate custom source files

GEN := $(addprefix $(intermediates)/, \
            rsgApi.cpp \
            rsgApiReplay.cpp \
        )

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL = $(RSG_GENERATOR_SUPPORT) $< $@ <$(PRIVATE_PATH)/rs.spec
$(GEN) : $(RSG_GENERATOR_SUPPORT) $(LOCAL_PATH)/rs.spec
$(GEN): $(intermediates)/%.cpp : $(LOCAL_PATH)/%.cpp.rsg
	$(transform-generated-source)

# used in jni/Android.mk
rs_generated_source += $(GEN)

LOCAL_GENERATED_SOURCES += $(GEN)

LOCAL_SRC_FILES:= \
	rsAdapter.cpp \
	rsAllocation.cpp \
	rsComponent.cpp \
	rsContext.cpp \
	rsDevice.cpp \
	rsElement.cpp \
	rsFifoSocket.cpp \
	rsObjectBase.cpp \
	rsMatrix2x2.cpp \
	rsMatrix3x3.cpp \
	rsMatrix4x4.cpp \
	rsMutex.cpp \
	rsSampler.cpp \
	rsScript.cpp \
	rsScriptC.cpp \
	rsScriptC_Lib.cpp \
	rsScriptGroup.cpp \
	rsScriptIntrinsic.cpp \
	rsSignal.cpp \
	rsStream.cpp \
	rsThreadIO.cpp \
	rsType.cpp \
	driver/rsdAllocation.cpp \
	driver/rsdBcc.cpp \
	driver/rsdCore.cpp \
	driver/rsdRuntimeMath.cpp \
	driver/rsdRuntimeStubs.cpp \
	driver/rsdSampler.cpp \
	driver/rsdIntrinsics.cpp \
	driver/rsdIntrinsicBlend.cpp \
	driver/rsdIntrinsicBlur.cpp \
	driver/rsdIntrinsicConvolve3x3.cpp \
	driver/rsdIntrinsicConvolve5x5.cpp \
	driver/rsdIntrinsicLUT.cpp \
	driver/rsdIntrinsicColorMatrix.cpp \
	driver/rsdIntrinsicYuvToRGB.cpp \
	driver/rsdScriptGroup.cpp



LOCAL_SHARED_LIBRARIES += libcutils libutils libdl

LOCAL_C_INCLUDES += system/core/include
LOCAL_C_INCLUDES += frameworks/native/include
LOCAL_C_INCLUDES += external/clang/lib/Headers
LOCAL_C_INCLUDES += frameworks/compile/libbcc/include

LOCAL_CFLAGS += $(rs_base_CFLAGS)

LOCAL_LDLIBS := -lpthread -ldl -lm
LOCAL_MODULE:= libRSSupport
LOCAL_SDK_VERSION := $(rs_base_SDK_VERSION)
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
