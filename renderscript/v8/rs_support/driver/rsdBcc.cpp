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

#include "rsdBcc.h"
#include "rsdAllocation.h"

#include "rsContext.h"
#include "rsElement.h"
#include "rsScriptC.h"

#include "utils/Vector.h"
#include "utils/Timers.h"
#include "utils/StopWatch.h"

using namespace android;
using namespace android::renderscript;


bool rsdScriptInit(const Context *rsc,
                     ScriptC *script,
                     char const *resName,
                     char const *cacheDir,
                     uint8_t const *bitcode,
                     size_t bitcodeSize,
                     uint32_t flags) {
    RsdHal *dc = (RsdHal *)rsc->mHal.drv;
    RsdCpuReference::CpuScript * cs = dc->mCpuRef->createScript(script, resName, cacheDir,
                                                                bitcode, bitcodeSize, flags);
    if (cs == NULL) {
        return false;
    }
    script->mHal.drv = cs;
    cs->populateScript(script);
    return true;
}

bool rsdInitIntrinsic(const Context *rsc, Script *s, RsScriptIntrinsicID iid, Element *e) {
    RsdHal *dc = (RsdHal *)rsc->mHal.drv;
    RsdCpuReference::CpuScript * cs = dc->mCpuRef->createIntrinsic(s, iid, e);
    if (cs == NULL) {
        return false;
    }
    s->mHal.drv = cs;
    cs->populateScript(s);
    return true;
}

void rsdScriptInvokeForEach(const Context *rsc,
                            Script *s,
                            uint32_t slot,
                            const Allocation * ain,
                            Allocation * aout,
                            const void * usr,
                            uint32_t usrLen,
                            const RsScriptCall *sc) {

    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)s->mHal.drv;
    cs->invokeForEach(slot, ain, aout, usr, usrLen, sc);
}


int rsdScriptInvokeRoot(const Context *dc, Script *s) {
    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)s->mHal.drv;
    return cs->invokeRoot();
}

void rsdScriptInvokeInit(const Context *dc, Script *s) {
    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)s->mHal.drv;
    cs->invokeInit();
}

void rsdScriptInvokeFreeChildren(const Context *dc, Script *s) {
    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)s->mHal.drv;
    cs->invokeFreeChildren();
}

void rsdScriptInvokeFunction(const Context *dc, Script *s,
                            uint32_t slot,
                            const void *params,
                            size_t paramLength) {
    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)s->mHal.drv;
    cs->invokeFunction(slot, params, paramLength);
}

void rsdScriptSetGlobalVar(const Context *dc, const Script *s,
                           uint32_t slot, void *data, size_t dataLength) {
    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)s->mHal.drv;
    cs->setGlobalVar(slot, data, dataLength);
}

void rsdScriptSetGlobalVarWithElemDims(const Context *dc, const Script *s,
                                       uint32_t slot, void *data, size_t dataLength,
                                       const android::renderscript::Element *elem,
                                       const size_t *dims, size_t dimLength) {
    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)s->mHal.drv;
    cs->setGlobalVarWithElemDims(slot, data, dataLength, elem, dims, dimLength);
}

void rsdScriptSetGlobalBind(const Context *dc, const Script *s, uint32_t slot, Allocation *data) {
    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)s->mHal.drv;
    cs->setGlobalBind(slot, data);
}

void rsdScriptSetGlobalObj(const Context *dc, const Script *s, uint32_t slot, ObjectBase *data) {
    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)s->mHal.drv;
    cs->setGlobalObj(slot, data);
}

void rsdScriptDestroy(const Context *dc, Script *s) {
    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)s->mHal.drv;
    delete cs;
    s->mHal.drv = NULL;
}


Allocation * rsdScriptGetAllocationForPointer(const android::renderscript::Context *dc,
                                              const android::renderscript::Script *sc,
                                              const void *ptr) {
    RsdCpuReference::CpuScript *cs = (RsdCpuReference::CpuScript *)sc->mHal.drv;
    return cs->getAllocationForPointer(ptr);
}

