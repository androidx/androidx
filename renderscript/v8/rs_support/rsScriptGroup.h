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

#ifndef ANDROID_RS_SCRIPT_GROUP_H
#define ANDROID_RS_SCRIPT_GROUP_H

#include "rsAllocation.h"
#include "rsScript.h"


// ---------------------------------------------------------------------------
namespace android {
namespace renderscript {

class ProgramVertex;
class ProgramFragment;
class ProgramRaster;
class ProgramStore;

class ScriptGroup : public ObjectBase {
public:
    Vector<ObjectBaseRef<ScriptKernelID> > mKernels;

    class Link {
    public:
        ObjectBaseRef<const ScriptKernelID> mSource;
        ObjectBaseRef<const ScriptKernelID> mDstKernel;
        ObjectBaseRef<const ScriptFieldID> mDstField;
        ObjectBaseRef<const Type> mType;
        ObjectBaseRef<Allocation> mAlloc;
        Link();
        ~Link();
    };

    class Node {
    public:
        Node(Script *);

        Vector<const ScriptKernelID *> mKernels;
        Vector<Link *> mOutputs;
        Vector<Link *> mInputs;
        bool mSeen;
        int mOrder;
        Script *mScript;
    };

    class IO {
    public:
        IO(const ScriptKernelID *);

        const ScriptKernelID *mKernel;
        ObjectBaseRef<Allocation> mAlloc;
    };

    Vector<Link *> mLinks;
    Vector<Node *> mNodes;
    Vector<IO *> mInputs;
    Vector<IO *> mOutputs;

    struct Hal {
        void * drv;

        struct DriverInfo {
        };
        DriverInfo info;
    };
    Hal mHal;

    static ScriptGroup * create(Context *rsc,
                           ScriptKernelID ** kernels, size_t kernelsSize,
                           ScriptKernelID ** src, size_t srcSize,
                           ScriptKernelID ** dstK, size_t dstKSize,
                           ScriptFieldID ** dstF, size_t dstFSize,
                           const Type ** type, size_t typeSize);

    virtual void serialize(Context *rsc, OStream *stream) const;
    virtual RsA3DClassID getClassId() const;

    void execute(Context *rsc);
    void setInput(Context *rsc, ScriptKernelID *kid, Allocation *a);
    void setOutput(Context *rsc, ScriptKernelID *kid, Allocation *a);


protected:
    virtual ~ScriptGroup();
    bool mInitialized;


private:
    bool calcOrderRecurse(Node *n, int depth);
    bool calcOrder();
    Node * findNode(Script *s) const;

    ScriptGroup(Context *);
};


}
}
#endif

