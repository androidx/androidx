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

#include "rsContext.h"
#include <time.h>

using namespace android;
using namespace android::renderscript;

ScriptGroup::ScriptGroup(Context *rsc) : ObjectBase(rsc) {
}

ScriptGroup::~ScriptGroup() {
    if (mRSC->mHal.funcs.scriptgroup.destroy) {
        mRSC->mHal.funcs.scriptgroup.destroy(mRSC, this);
    }

    for (size_t ct=0; ct < mLinks.size(); ct++) {
        delete mLinks[ct];
    }
}

ScriptGroup::IO::IO(const ScriptKernelID *kid) {
    mKernel = kid;
}

ScriptGroup::Node::Node(Script *s) {
    mScript = s;
    mSeen = false;
    mOrder = 0;
}

ScriptGroup::Node * ScriptGroup::findNode(Script *s) const {
    //ALOGE("find %p   %i", s, (int)mNodes.size());
    for (size_t ct=0; ct < mNodes.size(); ct++) {
        Node *n = mNodes[ct];
        for (size_t ct2=0; ct2 < n->mKernels.size(); ct2++) {
            if (n->mKernels[ct2]->mScript == s) {
                return n;
            }
        }
    }
    return NULL;
}

bool ScriptGroup::calcOrderRecurse(Node *n, int depth) {
    n->mSeen = true;
    if (n->mOrder < depth) {
        n->mOrder = depth;
    }
    bool ret = true;
    for (size_t ct=0; ct < n->mOutputs.size(); ct++) {
        const Link *l = n->mOutputs[ct];
        Node *nt = NULL;
        if (l->mDstField.get()) {
            nt = findNode(l->mDstField->mScript);
        } else {
            nt = findNode(l->mDstKernel->mScript);
        }
        if (nt->mSeen) {
            return false;
        }
        ret &= calcOrderRecurse(nt, n->mOrder + 1);
    }
    return ret;
}

static int CompareNodeForSort(ScriptGroup::Node *const* lhs,
                              ScriptGroup::Node *const* rhs) {
    if (lhs[0]->mOrder > rhs[0]->mOrder) {
        return 1;
    }
    return 0;
}


bool ScriptGroup::calcOrder() {
    // Make nodes
    for (size_t ct=0; ct < mKernels.size(); ct++) {
        const ScriptKernelID *k = mKernels[ct].get();
        //ALOGE(" kernel %i, %p  s=%p", (int)ct, k, mKernels[ct]->mScript);
        Node *n = findNode(k->mScript);
        //ALOGE("    n = %p", n);
        if (n == NULL) {
            n = new Node(k->mScript);
            mNodes.add(n);
        }
        n->mKernels.add(k);
    }

    // add links
    //ALOGE("link count %i", (int)mLinks.size());
    for (size_t ct=0; ct < mLinks.size(); ct++) {
        Link *l = mLinks[ct];
        //ALOGE("link  %i %p", (int)ct, l);
        Node *n = findNode(l->mSource->mScript);
        //ALOGE("link n %p", n);
        n->mOutputs.add(l);

        if (l->mDstKernel.get()) {
            //ALOGE("l->mDstKernel.get() %p", l->mDstKernel.get());
            n = findNode(l->mDstKernel->mScript);
            //ALOGE("  n1 %p", n);
            n->mInputs.add(l);
        } else {
            n = findNode(l->mDstField->mScript);
            //ALOGE("  n2 %p", n);
            n->mInputs.add(l);
        }
    }

    //ALOGE("node count %i", (int)mNodes.size());
    // Order nodes
    bool ret = true;
    for (size_t ct=0; ct < mNodes.size(); ct++) {
        Node *n = mNodes[ct];
        if (n->mInputs.size() == 0) {
            for (size_t ct2=0; ct2 < mNodes.size(); ct2++) {
                mNodes[ct2]->mSeen = false;
            }
            ret &= calcOrderRecurse(n, 0);
        }
    }

    for (size_t ct=0; ct < mKernels.size(); ct++) {
        const ScriptKernelID *k = mKernels[ct].get();
        const Node *n = findNode(k->mScript);

        if (k->mHasKernelOutput) {
            bool found = false;
            for (size_t ct2=0; ct2 < n->mOutputs.size(); ct2++) {
                if (n->mOutputs[ct2]->mSource.get() == k) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                //ALOGE("add io out %p", k);
                mOutputs.add(new IO(k));
            }
        }

        if (k->mHasKernelInput) {
            bool found = false;
            for (size_t ct2=0; ct2 < n->mInputs.size(); ct2++) {
                if (n->mInputs[ct2]->mDstKernel.get() == k) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                //ALOGE("add io in %p", k);
                mInputs.add(new IO(k));
            }
        }
    }

    // sort
    mNodes.sort(&CompareNodeForSort);

    return ret;
}

ScriptGroup * ScriptGroup::create(Context *rsc,
                           ScriptKernelID ** kernels, size_t kernelsSize,
                           ScriptKernelID ** src, size_t srcSize,
                           ScriptKernelID ** dstK, size_t dstKSize,
                           ScriptFieldID  ** dstF, size_t dstFSize,
                           const Type ** type, size_t typeSize) {

    size_t kernelCount = kernelsSize / sizeof(ScriptKernelID *);
    size_t linkCount = typeSize / sizeof(Type *);

    //ALOGE("ScriptGroup::create kernels=%i  links=%i", (int)kernelCount, (int)linkCount);


    // Start by counting unique kernel sources

    ScriptGroup *sg = new ScriptGroup(rsc);

    sg->mKernels.reserve(kernelCount);
    for (size_t ct=0; ct < kernelCount; ct++) {
        sg->mKernels.add(kernels[ct]);
    }

    sg->mLinks.reserve(linkCount);
    for (size_t ct=0; ct < linkCount; ct++) {
        Link *l = new Link();
        l->mType = type[ct];
        l->mSource = src[ct];
        l->mDstField = dstF[ct];
        l->mDstKernel = dstK[ct];
        sg->mLinks.add(l);
    }

    sg->calcOrder();

    // allocate links
    for (size_t ct=0; ct < sg->mNodes.size(); ct++) {
        const Node *n = sg->mNodes[ct];
        for (size_t ct2=0; ct2 < n->mOutputs.size(); ct2++) {
            Link *l = n->mOutputs[ct2];
            if (l->mAlloc.get()) {
                continue;
            }
            const ScriptKernelID *k = l->mSource.get();

            Allocation * alloc = Allocation::createAllocation(rsc,
                    l->mType.get(), RS_ALLOCATION_USAGE_SCRIPT);
            l->mAlloc = alloc;

            for (size_t ct3=ct2+1; ct3 < n->mOutputs.size(); ct3++) {
                if (n->mOutputs[ct3]->mSource.get() == l->mSource.get()) {
                    n->mOutputs[ct3]->mAlloc = alloc;
                }
            }
        }
    }

    if (rsc->mHal.funcs.scriptgroup.init) {
        rsc->mHal.funcs.scriptgroup.init(rsc, sg);
    }
    return sg;
}

void ScriptGroup::setInput(Context *rsc, ScriptKernelID *kid, Allocation *a) {
    for (size_t ct=0; ct < mInputs.size(); ct++) {
        if (mInputs[ct]->mKernel == kid) {
            mInputs[ct]->mAlloc = a;

            if (rsc->mHal.funcs.scriptgroup.setInput) {
                rsc->mHal.funcs.scriptgroup.setInput(rsc, this, kid, a);
            }
            return;
        }
    }
    rsAssert(!"ScriptGroup:setInput kid not found");
}

void ScriptGroup::setOutput(Context *rsc, ScriptKernelID *kid, Allocation *a) {
    for (size_t ct=0; ct < mOutputs.size(); ct++) {
        if (mOutputs[ct]->mKernel == kid) {
            mOutputs[ct]->mAlloc = a;

            if (rsc->mHal.funcs.scriptgroup.setOutput) {
                rsc->mHal.funcs.scriptgroup.setOutput(rsc, this, kid, a);
            }
            return;
        }
    }
    rsAssert(!"ScriptGroup:setOutput kid not found");
}

void ScriptGroup::execute(Context *rsc) {
    //ALOGE("ScriptGroup::execute");
    if (rsc->mHal.funcs.scriptgroup.execute) {
        rsc->mHal.funcs.scriptgroup.execute(rsc, this);
        return;
    }

    for (size_t ct=0; ct < mNodes.size(); ct++) {
        Node *n = mNodes[ct];
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
            for (size_t ct3=0; ct3 < mInputs.size(); ct3++) {
                if (mInputs[ct3]->mKernel == k) {
                    ain = mInputs[ct3]->mAlloc.get();
                    //ALOGE(" io in %p", ain);
                }
            }

            for (size_t ct3=0; ct3 < n->mOutputs.size(); ct3++) {
                if (n->mOutputs[ct3]->mSource.get() == k) {
                    aout = n->mOutputs[ct3]->mAlloc.get();
                    //ALOGE(" link out %p", aout);
                }
            }
            for (size_t ct3=0; ct3 < mOutputs.size(); ct3++) {
                if (mOutputs[ct3]->mKernel == k) {
                    aout = mOutputs[ct3]->mAlloc.get();
                    //ALOGE(" io out %p", aout);
                }
            }

            n->mScript->runForEach(rsc, k->mSlot, ain, aout, NULL, 0);
        }

    }

}

void ScriptGroup::serialize(Context *rsc, OStream *stream) const {
}

RsA3DClassID ScriptGroup::getClassId() const {
    return RS_A3D_CLASS_ID_SCRIPT_GROUP;
}

ScriptGroup::Link::Link() {
}

ScriptGroup::Link::~Link() {
}

namespace android {
namespace renderscript {


RsScriptGroup rsi_ScriptGroupCreate(Context *rsc,
                           RsScriptKernelID * kernels, size_t kernelsSize,
                           RsScriptKernelID * src, size_t srcSize,
                           RsScriptKernelID * dstK, size_t dstKSize,
                           RsScriptFieldID * dstF, size_t dstFSize,
                           const RsType * type, size_t typeSize) {


    return ScriptGroup::create(rsc,
                               (ScriptKernelID **) kernels, kernelsSize,
                               (ScriptKernelID **) src, srcSize,
                               (ScriptKernelID **) dstK, dstKSize,
                               (ScriptFieldID  **) dstF, dstFSize,
                               (const Type **) type, typeSize);
}


void rsi_ScriptGroupSetInput(Context *rsc, RsScriptGroup sg, RsScriptKernelID kid,
        RsAllocation alloc) {
    //ALOGE("rsi_ScriptGroupSetInput");
    ScriptGroup *s = (ScriptGroup *)sg;
    s->setInput(rsc, (ScriptKernelID *)kid, (Allocation *)alloc);
}

void rsi_ScriptGroupSetOutput(Context *rsc, RsScriptGroup sg, RsScriptKernelID kid,
        RsAllocation alloc) {
    //ALOGE("rsi_ScriptGroupSetOutput");
    ScriptGroup *s = (ScriptGroup *)sg;
    s->setOutput(rsc, (ScriptKernelID *)kid, (Allocation *)alloc);
}

void rsi_ScriptGroupExecute(Context *rsc, RsScriptGroup sg) {
    //ALOGE("rsi_ScriptGroupExecute");
    ScriptGroup *s = (ScriptGroup *)sg;
    s->execute(rsc);
}

}
}

