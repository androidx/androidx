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

#ifndef RSD_CPU_SCRIPT_INTRINSIC_H
#define RSD_CPU_SCRIPT_INTRINSIC_H

#include "rsCpuScript.h"


namespace android {
namespace renderscript {


class RsdCpuScriptIntrinsic : public RsdCpuScriptImpl {
public:
    virtual void populateScript(Script *) = 0;

    virtual void invokeFunction(uint32_t slot, const void *params, size_t paramLength);
    virtual int invokeRoot();
    virtual void invokeForEach(uint32_t slot,
                       const Allocation * ain,
                       Allocation * aout,
                       const void * usr,
                       uint32_t usrLen,
                       const RsScriptCall *sc);
    virtual void forEachKernelSetup(uint32_t slot, MTLaunchStruct *mtls);
    virtual void invokeInit();
    virtual void invokeFreeChildren();

    virtual void setGlobalVar(uint32_t slot, const void *data, size_t dataLength);
    virtual void setGlobalVarWithElemDims(uint32_t slot, const void *data, size_t dataLength,
                                  const Element *e, const size_t *dims, size_t dimLength);
    virtual void setGlobalBind(uint32_t slot, Allocation *data);
    virtual void setGlobalObj(uint32_t slot, ObjectBase *data);

    virtual ~RsdCpuScriptIntrinsic();
    RsdCpuScriptIntrinsic(RsdCpuReferenceImpl *ctx, const Script *s, const Element *,
                          RsScriptIntrinsicID iid);

protected:
    RsScriptIntrinsicID mID;
    outer_foreach_t mRootPtr;
    ObjectBaseRef<const Element> mElement;

};



}
}

#endif
