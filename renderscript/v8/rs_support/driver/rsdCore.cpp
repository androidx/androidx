/*
 * Copyright (C) 2011-2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "../cpu_ref/rsd_cpu.h"

#include "rsdCore.h"
#include "rsdAllocation.h"
#include "rsdBcc.h"
#include "rsdSampler.h"
#include "rsdScriptGroup.h"

#include <malloc.h>
#include "rsContext.h"

#include <sys/types.h>
#include <sys/resource.h>
#include <sched.h>
#include <cutils/properties.h>
#include <sys/syscall.h>
#include <string.h>

using namespace android;
using namespace android::renderscript;

static void Shutdown(Context *rsc);
static void SetPriority(const Context *rsc, int32_t priority);

static RsdHalFunctions FunctionTable = {
    NULL,
    NULL,
    NULL,
    NULL,

    Shutdown,
    NULL,
    SetPriority,
    {
        rsdScriptInit,
        rsdInitIntrinsic,
        rsdScriptInvokeFunction,
        rsdScriptInvokeRoot,
        rsdScriptInvokeForEach,
        rsdScriptInvokeInit,
        rsdScriptInvokeFreeChildren,
        rsdScriptSetGlobalVar,
        rsdScriptSetGlobalVarWithElemDims,
        rsdScriptSetGlobalBind,
        rsdScriptSetGlobalObj,
        rsdScriptDestroy
    },

    {
        rsdAllocationInit,
        rsdAllocationDestroy,
        rsdAllocationResize,
        rsdAllocationSyncAll,
        rsdAllocationMarkDirty,
        NULL,
        NULL,
        NULL,
        NULL,
        rsdAllocationData1D,
        rsdAllocationData2D,
        rsdAllocationData3D,
        rsdAllocationRead1D,
        rsdAllocationRead2D,
        rsdAllocationRead3D,
        rsdAllocationLock1D,
        rsdAllocationUnlock1D,
        rsdAllocationData1D_alloc,
        rsdAllocationData2D_alloc,
        rsdAllocationData3D_alloc,
        rsdAllocationElementData1D,
        rsdAllocationElementData2D,
        rsdAllocationGenerateMipmaps
    },


    {
        NULL,
        NULL,
        NULL
    },

    {
        NULL,
        NULL,
        NULL
    },

    {
        NULL,
        NULL,
        NULL
    },

    {
        NULL,
        NULL,
        NULL
    },

    {
        NULL,
        NULL,
        NULL
    },

    {
        NULL,
        NULL,
        NULL,
        NULL
    },

    {
        rsdSamplerInit,
        rsdSamplerDestroy
    },

    {
        NULL,
        NULL,
        NULL
    },

    {
        rsdScriptGroupInit,
        rsdScriptGroupSetInput,
        rsdScriptGroupSetOutput,
        rsdScriptGroupExecute,
        rsdScriptGroupDestroy
    }


};

extern const RsdCpuReference::CpuSymbol * rsdLookupRuntimeStub(Context * pContext, char const* name);

static RsdCpuReference::CpuScript * LookupScript(Context *, const Script *s) {
    return (RsdCpuReference::CpuScript *)s->mHal.drv;
}

extern "C" bool rsdHalInit(RsContext c, uint32_t version_major,
                           uint32_t version_minor) {
    Context *rsc = (Context*) c;
    rsc->mHal.funcs = FunctionTable;

    RsdHal *dc = (RsdHal *)calloc(1, sizeof(RsdHal));
    if (!dc) {
        ALOGE("Calloc for driver hal failed.");
        return false;
    }
    rsc->mHal.drv = dc;

    dc->mCpuRef = RsdCpuReference::create((Context *)c, version_major, version_minor,
                                          &rsdLookupRuntimeStub, &LookupScript);
    if (!dc->mCpuRef) {
        ALOGE("RsdCpuReference::create for driver hal failed.");
        free(dc);
        return false;
    }

    return true;
}


void SetPriority(const Context *rsc, int32_t priority) {
    RsdHal *dc = (RsdHal *)rsc->mHal.drv;

    dc->mCpuRef->setPriority(priority);

    if (dc->mHasGraphics) {
        //rsdGLSetPriority(rsc, priority);
    }
}

void Shutdown(Context *rsc) {
    RsdHal *dc = (RsdHal *)rsc->mHal.drv;
    delete dc->mCpuRef;
    rsc->mHal.drv = NULL;
}

