
LOCAL_PATH:=frameworks/rs
rs_base_CFLAGS := -Werror -Wall -Wno-unused-parameter -Wno-unused-variable \
		  -Wno-overloaded-virtual -DRS_COMPATIBILITY_LIB

ifeq ($(ARCH_ARM_HAVE_NEON),true)
rs_base_CFLAGS += -DARCH_ARM_HAVE_NEON
endif

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
LOCAL_SDK_VERSION := 8

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
	rsCompatibilityLib.cpp \
	rsComponent.cpp \
	rsContext.cpp \
	rsCppUtils.cpp \
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
	driver/rsdRuntimeStubs.cpp \
	driver/rsdSampler.cpp \
	driver/rsdScriptGroup.cpp \
	cpu_ref/rsCpuCore.cpp \
	cpu_ref/rsCpuScript.cpp \
	cpu_ref/rsCpuRuntimeMath.cpp \
	cpu_ref/rsCpuRuntimeStubs.cpp \
	cpu_ref/rsCpuScriptGroup.cpp \
	cpu_ref/rsCpuIntrinsic.cpp \
	cpu_ref/rsCpuIntrinsic3DLUT.cpp \
	cpu_ref/rsCpuIntrinsicBlend.cpp \
	cpu_ref/rsCpuIntrinsicBlur.cpp \
	cpu_ref/rsCpuIntrinsicColorMatrix.cpp \
	cpu_ref/rsCpuIntrinsicConvolve3x3.cpp \
	cpu_ref/rsCpuIntrinsicConvolve5x5.cpp \
	cpu_ref/rsCpuIntrinsicHistogram.cpp \
	cpu_ref/rsCpuIntrinsicLUT.cpp \
	cpu_ref/rsCpuIntrinsicYuvToRGB.cpp \
	cpu_ref/rsCpuRuntimeMathFuncs.cpp

ifeq ($(ARCH_ARM_HAVE_ARMV7A),true)
LOCAL_CFLAGS += -DARCH_ARM_HAVE_VFP
LOCAL_ASFLAGS := -mfpu=neon
LOCAL_SRC_FILES += \
	cpu_ref/rsCpuIntrinsics_neon.S \
	cpu_ref/rsCpuIntrinsics_neon_ColorMatrix.S
endif

LOCAL_LDFLAGS += -llog -ldl
LOCAL_NDK_STL_VARIANT := stlport_static

LOCAL_C_INCLUDES += external/clang/lib/Headers
LOCAL_C_INCLUDES += frameworks/compile/libbcc/include


LOCAL_CFLAGS += $(rs_base_CFLAGS)

LOCAL_LDLIBS := -lpthread -ldl -lm
LOCAL_MODULE:= libRSSupport
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
