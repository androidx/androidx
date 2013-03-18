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

#ifndef RSD_BCC_H
#define RSD_BCC_H

#include <rs_hal.h>
#include <rsRuntime.h>

#include "rsCpuCore.h"

namespace bcc {
    class BCCContext;
    class RSCompilerDriver;
    class RSExecutable;
}

namespace android {
namespace renderscript {



class RsdCpuScriptImpl : public RsdCpuReferenceImpl::CpuScript {
public:
    typedef void (*outer_foreach_t)(
        const RsForEachStubParamStruct *,
        uint32_t x1, uint32_t x2,
        uint32_t instep, uint32_t outstep);
#ifdef RS_COMPATIBILITY_LIB
    typedef void (* InvokeFunc_t)(void);
    typedef void (* ForEachFunc_t)(void);
    typedef int (* RootFunc_t)(void);
    typedef void (*WorkerCallback_t)(void *usr, uint32_t idx);
#endif

    bool init(char const *resName, char const *cacheDir,
              uint8_t const *bitcode, size_t bitcodeSize, uint32_t flags);
    virtual void populateScript(Script *);

    virtual void invokeFunction(uint32_t slot, const void *params, size_t paramLength);
    virtual int invokeRoot();
    virtual void invokeForEach(uint32_t slot,
                       const Allocation * ain,
                       Allocation * aout,
                       const void * usr,
                       uint32_t usrLen,
                       const RsScriptCall *sc);
    virtual void invokeInit();
    virtual void invokeFreeChildren();

    virtual void setGlobalVar(uint32_t slot, const void *data, size_t dataLength);
    virtual void setGlobalVarWithElemDims(uint32_t slot, const void *data, size_t dataLength,
                                  const Element *e, const size_t *dims, size_t dimLength);
    virtual void setGlobalBind(uint32_t slot, Allocation *data);
    virtual void setGlobalObj(uint32_t slot, ObjectBase *data);


    virtual ~RsdCpuScriptImpl();
    RsdCpuScriptImpl(RsdCpuReferenceImpl *ctx, const Script *s);

    const Script * getScript() {return mScript;}

    void forEachMtlsSetup(const Allocation * ain, Allocation * aout,
                          const void * usr, uint32_t usrLen,
                          const RsScriptCall *sc, MTLaunchStruct *mtls);
    virtual void forEachKernelSetup(uint32_t slot, MTLaunchStruct *mtls);


    const RsdCpuReference::CpuSymbol * lookupSymbolMath(const char *sym);
    static void * lookupRuntimeStub(void* pContext, char const* name);

    virtual Allocation * getAllocationForPointer(const void *ptr) const;

#ifndef RS_COMPATIBILITY_LIB
    virtual  void * getRSExecutable() { return mExecutable; }
#endif

protected:
    RsdCpuReferenceImpl *mCtx;
    const Script *mScript;

#ifndef RS_COMPATIBILITY_LIB
    int (*mRoot)();
    int (*mRootExpand)();
    void (*mInit)();
    void (*mFreeChildren)();

    bcc::BCCContext *mCompilerContext;
    bcc::RSCompilerDriver *mCompilerDriver;
    bcc::RSExecutable *mExecutable;
#else
    void *mScriptSO;
    RootFunc_t mRoot;
    RootFunc_t mRootExpand;
    InvokeFunc_t mInit;
    InvokeFunc_t mFreeChildren;
    InvokeFunc_t *mInvokeFunctions;
    ForEachFunc_t *mForEachFunctions;

    void **mFieldAddress;
    bool *mFieldIsObject;
    uint32_t *mForEachSignatures;

    // for populate script
    //int mVersionMajor;
    //int mVersionMinor;
    size_t mExportedVariableCount;
    size_t mExportedFunctionCount;
#endif

    Allocation **mBoundAllocs;
    void * mIntrinsicData;
    bool mIsThreadable;

};


Allocation * rsdScriptGetAllocationForPointer(
                        const Context *dc,
                        const Script *script,
                        const void *);



}
}

#endif
