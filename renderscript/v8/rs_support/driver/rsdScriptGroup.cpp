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

#include "rsdCore.h"
#include "../cpu_ref/rsd_cpu.h"


#include "rsScript.h"
#include "rsScriptGroup.h"
#include "rsdScriptGroup.h"
#include "rsdBcc.h"
#include "rsdAllocation.h"

using namespace android;
using namespace android::renderscript;


bool rsdScriptGroupInit(const Context *rsc, ScriptGroup *sg) {
    RsdHal *dc = (RsdHal *)rsc->mHal.drv;

    sg->mHal.drv = dc->mCpuRef->createScriptGroup(sg);
    return sg->mHal.drv != NULL;
}

void rsdScriptGroupSetInput(const Context *rsc, const ScriptGroup *sg,
                            const ScriptKernelID *kid, Allocation *) {
}

void rsdScriptGroupSetOutput(const Context *rsc, const ScriptGroup *sg,
                             const ScriptKernelID *kid, Allocation *) {
}

void rsdScriptGroupExecute(const Context *rsc, const ScriptGroup *sg) {
    RsdCpuReference::CpuScriptGroup *sgi = (RsdCpuReference::CpuScriptGroup *)sg->mHal.drv;
    sgi->execute();
}

void rsdScriptGroupDestroy(const Context *rsc, const ScriptGroup *sg) {
    RsdCpuReference::CpuScriptGroup *sgi = (RsdCpuReference::CpuScriptGroup *)sg->mHal.drv;
    delete sgi;
}


