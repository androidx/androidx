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

#include "rsdCore.h"

#include <bcc/BCCContext.h>
#include <bcc/Renderscript/RSCompilerDriver.h>
#include <bcc/Renderscript/RSExecutable.h>
#include <bcc/Renderscript/RSInfo.h>

#include "rsScript.h"
#include "rsScriptGroup.h"
#include "rsdScriptGroup.h"
#include "rsdBcc.h"

using namespace android;
using namespace android::renderscript;


bool rsdScriptGroupInit(const android::renderscript::Context *rsc,
                        const android::renderscript::ScriptGroup *sg) {
    return true;
}

void rsdScriptGroupSetInput(const android::renderscript::Context *rsc,
                            const android::renderscript::ScriptGroup *sg,
                            const android::renderscript::ScriptKernelID *kid,
                            android::renderscript::Allocation *) {
}

void rsdScriptGroupSetOutput(const android::renderscript::Context *rsc,
                             const android::renderscript::ScriptGroup *sg,
                             const android::renderscript::ScriptKernelID *kid,
                             android::renderscript::Allocation *) {
}

void rsdScriptGroupExecute(const android::renderscript::Context *rsc,
                           const android::renderscript::ScriptGroup *sg) {

    Vector<Allocation *> ins;
    Vector<Allocation *> outs;
    Vector<const ScriptKernelID *> kernels;

    for (size_t ct=0; ct < sg->mNodes.size(); ct++) {
        ScriptGroup::Node *n = sg->mNodes[ct];
        //ALOGE("node %i, order %i, in %i out %i", (int)ct, n->mOrder, (int)n->mInputs.size(), (int)n->mOutputs.size());

        for (size_t ct2=0; ct2 < n->mKernels.size(); ct2++) {
            const ScriptKernelID *k = n->mKernels[ct2];
            Allocation *ain = NULL;
            Allocation *aout = NULL;

            for (size_t ct3=0; ct3 < n->mInputs.size(); ct3++) {
                if (n->mInputs[ct3]->mDstKernel.get() == k) {
                    ain = n->mInputs[ct3]->mAlloc.get();
                    //ALOGE(" link in %p", ain);
                }
            }
            for (size_t ct3=0; ct3 < sg->mInputs.size(); ct3++) {
                if (sg->mInputs[ct3]->mKernel == k) {
                    ain = sg->mInputs[ct3]->mAlloc.get();
                    //ALOGE(" io in %p", ain);
                }
            }

            for (size_t ct3=0; ct3 < n->mOutputs.size(); ct3++) {
                if (n->mOutputs[ct3]->mSource.get() == k) {
                    aout = n->mOutputs[ct3]->mAlloc.get();
                    //ALOGE(" link out %p", aout);
                }
            }
            for (size_t ct3=0; ct3 < sg->mOutputs.size(); ct3++) {
                if (sg->mOutputs[ct3]->mKernel == k) {
                    aout = sg->mOutputs[ct3]->mAlloc.get();
                    //ALOGE(" io out %p", aout);
                }
            }

            ins.add(ain);
            outs.add(aout);
            kernels.add(k);
        }

    }

    RsdHal * dc = (RsdHal *)rsc->mHal.drv;
    MTLaunchStruct mtls;
    for (size_t ct=0; ct < ins.size(); ct++) {

        Script *s = kernels[ct]->mScript;
        DrvScript *drv = (DrvScript *)s->mHal.drv;
        uint32_t slot = kernels[ct]->mSlot;

        rsdScriptInvokeForEachMtlsSetup(rsc, ins[ct], outs[ct], NULL, 0, NULL, &mtls);
        mtls.script = s;
        mtls.fep.slot = slot;

        if (drv->mIntrinsicID) {
            mtls.kernel = (void (*)())drv->mIntrinsicFuncs.root;
            mtls.fep.usr = drv->mIntrinsicData;
        } else {
    	    mtls.kernel = drv->mForEachFunctions[slot];
            rsAssert(mtls.kernel != NULL);
            mtls.sig = drv->mForEachSignatures[slot];
        }

//        typedef void (*outer_foreach_t)(
  //          const android::renderscript::RsForEachStubParamStruct *,
    //        uint32_t x1, uint32_t x2,
      //      uint32_t instep, uint32_t outstep);
        //outer_foreach_t fn = (outer_foreach_t) mtls->kernel;

        rsdScriptLaunchThreads(rsc, s, slot, ins[ct], outs[ct], NULL, 0, NULL, &mtls);
    }

}

void rsdScriptGroupDestroy(const android::renderscript::Context *rsc,
                           const android::renderscript::ScriptGroup *sg) {
}


