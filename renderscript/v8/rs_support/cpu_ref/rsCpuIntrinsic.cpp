/*
 * Copyright (C) 2012 The Android Open Source Project
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


#include "rsCpuIntrinsic.h"

using namespace android;
using namespace android::renderscript;

RsdCpuScriptIntrinsic::RsdCpuScriptIntrinsic(RsdCpuReferenceImpl *ctx, const Script *s,
                                             const Element *e, RsScriptIntrinsicID iid)
        : RsdCpuScriptImpl(ctx, s) {

    mID = iid;
    mElement.set(e);
}

RsdCpuScriptIntrinsic::~RsdCpuScriptIntrinsic() {
}

void RsdCpuScriptIntrinsic::invokeFunction(uint32_t slot, const void *params, size_t paramLength) {
    mCtx->getContext()->setError(RS_ERROR_FATAL_DRIVER,
                                 "Unexpected RsdCpuScriptIntrinsic::invokeFunction");
}

int RsdCpuScriptIntrinsic::invokeRoot() {
    mCtx->getContext()->setError(RS_ERROR_FATAL_DRIVER,
                                 "Unexpected RsdCpuScriptIntrinsic::invokeRoot");
    return 0;
}

void RsdCpuScriptIntrinsic::invokeInit() {
    mCtx->getContext()->setError(RS_ERROR_FATAL_DRIVER,
                                 "Unexpected RsdCpuScriptIntrinsic::invokeInit");
}

void RsdCpuScriptIntrinsic::setGlobalVar(uint32_t slot, const void *data, size_t dataLength) {
    mCtx->getContext()->setError(RS_ERROR_FATAL_DRIVER,
                                 "Unexpected RsdCpuScriptIntrinsic::setGlobalVar");
}

void RsdCpuScriptIntrinsic::setGlobalVarWithElemDims(uint32_t slot, const void *data,
                                                     size_t dataLength, const Element *e,
                                                     const size_t *dims, size_t dimLength) {
    mCtx->getContext()->setError(RS_ERROR_FATAL_DRIVER,
                                 "Unexpected RsdCpuScriptIntrinsic::setGlobalVarWithElemDims");
}

void RsdCpuScriptIntrinsic::setGlobalBind(uint32_t slot, Allocation *data) {
    mCtx->getContext()->setError(RS_ERROR_FATAL_DRIVER,
                                 "Unexpected RsdCpuScriptIntrinsic::setGlobalBind");
}

void RsdCpuScriptIntrinsic::setGlobalObj(uint32_t slot, ObjectBase *data) {
    mCtx->getContext()->setError(RS_ERROR_FATAL_DRIVER,
                                 "Unexpected RsdCpuScriptIntrinsic::setGlobalObj");
}

void RsdCpuScriptIntrinsic::invokeFreeChildren() {
}


void RsdCpuScriptIntrinsic::invokeForEach(uint32_t slot,
                                          const Allocation * ain,
                                          Allocation * aout,
                                          const void * usr,
                                          uint32_t usrLen,
                                          const RsScriptCall *sc) {

    MTLaunchStruct mtls;
    forEachMtlsSetup(ain, aout, usr, usrLen, sc, &mtls);
    mtls.script = this;
    mtls.fep.slot = slot;

    mtls.kernel = (void (*)())mRootPtr;
    mtls.fep.usr = this;

    RsdCpuScriptImpl * oldTLS = mCtx->setTLS(this);
    mCtx->launchThreads(ain, aout, sc, &mtls);
    mCtx->setTLS(oldTLS);
}

void RsdCpuScriptIntrinsic::forEachKernelSetup(uint32_t slot, MTLaunchStruct *mtls) {

    mtls->script = this;
    mtls->fep.slot = slot;
    mtls->kernel = (void (*)())mRootPtr;
    mtls->fep.usr = this;
}



