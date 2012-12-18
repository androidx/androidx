/*
 * Copyright (C) 2011 The Android Open Source Project
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

#include "rsCpuCore.h"
#include "rsCpuScript.h"
#include "rsCpuScriptGroup.h"

#include <bcc/BCCContext.h>
#include <bcc/Renderscript/RSCompilerDriver.h>
#include <bcc/Renderscript/RSExecutable.h>
#include <bcc/Renderscript/RSInfo.h>

#include "rsScript.h"
#include "rsScriptGroup.h"
#include "rsCpuScriptGroup.h"
//#include "rsdBcc.h"
//#include "rsdAllocation.h"

using namespace android;
using namespace android::renderscript;

CpuScriptGroupImpl::CpuScriptGroupImpl(RsdCpuReferenceImpl *ctx, const ScriptGroup *sg) {
    mCtx = ctx;
    mSG = sg;
}

CpuScriptGroupImpl::~CpuScriptGroupImpl() {

}

bool CpuScriptGroupImpl::init() {
    return true;
}

void CpuScriptGroupImpl::setInput(const ScriptKernelID *kid, Allocation *a) {
}

void CpuScriptGroupImpl::setOutput(const ScriptKernelID *kid, Allocation *a) {
}


typedef void (*ScriptGroupRootFunc_t)(const RsForEachStubParamStruct *p,
                                      uint32_t xstart, uint32_t xend,
                                      uint32_t instep, uint32_t outstep);

void CpuScriptGroupImpl::scriptGroupRoot(const RsForEachStubParamStruct *p,
                                         uint32_t xstart, uint32_t xend,
                                         uint32_t instep, uint32_t outstep) {


    const ScriptList *sl = (const ScriptList *)p->usr;
    RsForEachStubParamStruct *mp = (RsForEachStubParamStruct *)p;
    const void *oldUsr = p->usr;

    for(size_t ct=0; ct < sl->count; ct++) {
        ScriptGroupRootFunc_t func;
        func = (ScriptGroupRootFunc_t)sl->fnPtrs[ct];
        mp->usr = sl->usrPtrs[ct];

        mp->ptrIn = NULL;
        mp->in = NULL;
        mp->ptrOut = NULL;
        mp->out = NULL;

        if (sl->ins[ct]) {
            mp->ptrIn = (const uint8_t *)sl->ins[ct]->mHal.drvState.lod[0].mallocPtr;
            mp->in = mp->ptrIn;
            if (sl->inExts[ct]) {
                mp->in = mp->ptrIn + sl->ins[ct]->mHal.drvState.lod[0].stride * p->y;
            } else {
                if (sl->ins[ct]->mHal.drvState.lod[0].dimY > p->lid) {
                    mp->in = mp->ptrIn + sl->ins[ct]->mHal.drvState.lod[0].stride * p->lid;
                }
            }
        }

        if (sl->outs[ct]) {
            mp->ptrOut = (uint8_t *)sl->outs[ct]->mHal.drvState.lod[0].mallocPtr;
            mp->out = mp->ptrOut;
            if (sl->outExts[ct]) {
                mp->out = mp->ptrOut + sl->outs[ct]->mHal.drvState.lod[0].stride * p->y;
            } else {
                if (sl->outs[ct]->mHal.drvState.lod[0].dimY > p->lid) {
                    mp->out = mp->ptrOut + sl->outs[ct]->mHal.drvState.lod[0].stride * p->lid;
                }
            }
        }

        //ALOGE("kernel %i %p,%p  %p,%p", ct, mp->ptrIn, mp->in, mp->ptrOut, mp->out);
        func(p, xstart, xend, instep, outstep);
    }
    //ALOGE("script group root");

    //ConvolveParams *cp = (ConvolveParams *)p->usr;

    mp->usr = oldUsr;
}



void CpuScriptGroupImpl::execute() {
    Vector<Allocation *> ins;
    Vector<bool> inExts;
    Vector<Allocation *> outs;
    Vector<bool> outExts;
    Vector<const ScriptKernelID *> kernels;
    bool fieldDep = false;

    for (size_t ct=0; ct < mSG->mNodes.size(); ct++) {
        ScriptGroup::Node *n = mSG->mNodes[ct];
        Script *s = n->mKernels[0]->mScript;

        //ALOGE("node %i, order %i, in %i out %i", (int)ct, n->mOrder, (int)n->mInputs.size(), (int)n->mOutputs.size());

        for (size_t ct2=0; ct2 < n->mInputs.size(); ct2++) {
            if (n->mInputs[ct2]->mDstField.get() && n->mInputs[ct2]->mDstField->mScript) {
                //ALOGE("field %p %zu", n->mInputs[ct2]->mDstField->mScript, n->mInputs[ct2]->mDstField->mSlot);
                s->setVarObj(n->mInputs[ct2]->mDstField->mSlot, n->mInputs[ct2]->mAlloc.get());
            }
        }

        for (size_t ct2=0; ct2 < n->mKernels.size(); ct2++) {
            const ScriptKernelID *k = n->mKernels[ct2];
            Allocation *ain = NULL;
            Allocation *aout = NULL;
            bool inExt = false;
            bool outExt = false;

            for (size_t ct3=0; ct3 < n->mInputs.size(); ct3++) {
                if (n->mInputs[ct3]->mDstKernel.get() == k) {
                    ain = n->mInputs[ct3]->mAlloc.get();
                    //ALOGE(" link in %p", ain);
                }
            }
            for (size_t ct3=0; ct3 < mSG->mInputs.size(); ct3++) {
                if (mSG->mInputs[ct3]->mKernel == k) {
                    ain = mSG->mInputs[ct3]->mAlloc.get();
                    inExt = true;
                    //ALOGE(" io in %p", ain);
                }
            }

            for (size_t ct3=0; ct3 < n->mOutputs.size(); ct3++) {
                if (n->mOutputs[ct3]->mSource.get() == k) {
                    aout = n->mOutputs[ct3]->mAlloc.get();
                    if(n->mOutputs[ct3]->mDstField.get() != NULL) {
                        fieldDep = true;
                    }
                    //ALOGE(" link out %p", aout);
                }
            }
            for (size_t ct3=0; ct3 < mSG->mOutputs.size(); ct3++) {
                if (mSG->mOutputs[ct3]->mKernel == k) {
                    aout = mSG->mOutputs[ct3]->mAlloc.get();
                    outExt = true;
                    //ALOGE(" io out %p", aout);
                }
            }

            if ((k->mHasKernelOutput == (aout != NULL)) &&
                (k->mHasKernelInput == (ain != NULL))) {
                ins.add(ain);
                inExts.add(inExt);
                outs.add(aout);
                outExts.add(outExt);
                kernels.add(k);
            }
        }

    }

    MTLaunchStruct mtls;

    if(fieldDep) {
        for (size_t ct=0; ct < ins.size(); ct++) {
            Script *s = kernels[ct]->mScript;
            RsdCpuScriptImpl *si = (RsdCpuScriptImpl *)mCtx->lookupScript(s);
            uint32_t slot = kernels[ct]->mSlot;

            si->forEachMtlsSetup(ins[ct], outs[ct], NULL, 0, NULL, &mtls);
            si->forEachKernelSetup(slot, &mtls);
            mCtx->launchThreads(ins[ct], outs[ct], NULL, &mtls);
        }
    } else {
        ScriptList sl;
        sl.ins = ins.array();
        sl.outs = outs.array();
        sl.kernels = kernels.array();
        sl.count = kernels.size();

        Vector<const void *> usrPtrs;
        Vector<const void *> fnPtrs;
        Vector<uint32_t> sigs;
        for (size_t ct=0; ct < kernels.size(); ct++) {
            Script *s = kernels[ct]->mScript;
            RsdCpuScriptImpl *si = (RsdCpuScriptImpl *)mCtx->lookupScript(s);

            si->forEachKernelSetup(kernels[ct]->mSlot, &mtls);
            fnPtrs.add((void *)mtls.kernel);
            usrPtrs.add(mtls.fep.usr);
            sigs.add(mtls.fep.usrLen);
        }
        sl.sigs = sigs.array();
        sl.usrPtrs = usrPtrs.array();
        sl.fnPtrs = fnPtrs.array();
        sl.inExts = inExts.array();
        sl.outExts = outExts.array();

        Script *s = kernels[0]->mScript;
        RsdCpuScriptImpl *si = (RsdCpuScriptImpl *)mCtx->lookupScript(s);
        si->forEachMtlsSetup(ins[0], outs[0], NULL, 0, NULL, &mtls);
        mtls.script = NULL;
        mtls.kernel = (void (*)())&scriptGroupRoot;
        mtls.fep.usr = &sl;
        mCtx->launchThreads(ins[0], outs[0], NULL, &mtls);
    }
}


