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

#ifndef RSD_SCRIPT_GROUP_H
#define RSD_SCRIPT_GROUP_H

#include "rsd_cpu.h"

namespace android {
namespace renderscript {


class CpuScriptGroupImpl : public RsdCpuReference::CpuScriptGroup {
public:
    virtual void setInput(const ScriptKernelID *kid, Allocation *);
    virtual void setOutput(const ScriptKernelID *kid, Allocation *);
    virtual void execute();
    virtual ~CpuScriptGroupImpl();

    CpuScriptGroupImpl(RsdCpuReferenceImpl *ctx, const ScriptGroup *sg);
    bool init();

    static void scriptGroupRoot(const RsForEachStubParamStruct *p,
                                uint32_t xstart, uint32_t xend,
                                uint32_t instep, uint32_t outstep);

protected:
    struct ScriptList {
        size_t count;
        Allocation *const* ins;
        bool const* inExts;
        Allocation *const* outs;
        bool const* outExts;
        const void *const* usrPtrs;
        size_t const *usrSizes;
        uint32_t const *sigs;
        const void *const* fnPtrs;

        const ScriptKernelID *const* kernels;
    };
    ScriptList mSl;
    const ScriptGroup *mSG;
    RsdCpuReferenceImpl *mCtx;
};

}
}

#endif // RSD_SCRIPT_GROUP_H
