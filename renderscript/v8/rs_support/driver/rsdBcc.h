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


bool rsdScriptInit(const android::renderscript::Context *, android::renderscript::ScriptC *,
                   char const *resName, char const *cacheDir,
                   uint8_t const *bitcode, size_t bitcodeSize, uint32_t flags);
bool rsdInitIntrinsic(const android::renderscript::Context *rsc,
                      android::renderscript::Script *s,
                      RsScriptIntrinsicID iid,
                      android::renderscript::Element *e);

void rsdScriptInvokeFunction(const android::renderscript::Context *dc,
                             android::renderscript::Script *script,
                             uint32_t slot,
                             const void *params,
                             size_t paramLength);

void rsdScriptInvokeForEach(const android::renderscript::Context *rsc,
                            android::renderscript::Script *s,
                            uint32_t slot,
                            const android::renderscript::Allocation * ain,
                            android::renderscript::Allocation * aout,
                            const void * usr,
                            uint32_t usrLen,
                            const RsScriptCall *sc);

int rsdScriptInvokeRoot(const android::renderscript::Context *dc,
                        android::renderscript::Script *script);
void rsdScriptInvokeInit(const android::renderscript::Context *dc,
                         android::renderscript::Script *script);
void rsdScriptInvokeFreeChildren(const android::renderscript::Context *dc,
                                 android::renderscript::Script *script);

void rsdScriptSetGlobalVar(const android::renderscript::Context *,
                           const android::renderscript::Script *,
                           uint32_t slot, void *data, size_t dataLen);
void rsdScriptSetGlobalVarWithElemDims(const android::renderscript::Context *,
                                       const android::renderscript::Script *,
                                       uint32_t slot, void *data,
                                       size_t dataLength,
                                       const android::renderscript::Element *,
                                       const size_t *dims,
                                       size_t dimLength);
void rsdScriptSetGlobalBind(const android::renderscript::Context *,
                            const android::renderscript::Script *,
                            uint32_t slot, android::renderscript::Allocation *data);
void rsdScriptSetGlobalObj(const android::renderscript::Context *,
                           const android::renderscript::Script *,
                           uint32_t slot, android::renderscript::ObjectBase *data);

void rsdScriptSetGlobal(const android::renderscript::Context *dc,
                        const android::renderscript::Script *script,
                        uint32_t slot,
                        void *data,
                        size_t dataLength);
void rsdScriptGetGlobal(const android::renderscript::Context *dc,
                        const android::renderscript::Script *script,
                        uint32_t slot,
                        void *data,
                        size_t dataLength);
void rsdScriptDestroy(const android::renderscript::Context *dc,
                      android::renderscript::Script *script);

android::renderscript::Allocation * rsdScriptGetAllocationForPointer(
                        const android::renderscript::Context *dc,
                        const android::renderscript::Script *script,
                        const void *);


typedef void (*outer_foreach_t)(
    const android::renderscript::RsForEachStubParamStruct *,
    uint32_t x1, uint32_t x2,
    uint32_t instep, uint32_t outstep);

typedef struct RsdIntriniscFuncs_rec {

    void (*bind)(const android::renderscript::Context *dc,
                 const android::renderscript::Script *script,
                 void * intrinsicData,
                 uint32_t slot, android::renderscript::Allocation *data);
    void (*setVar)(const android::renderscript::Context *dc,
                   const android::renderscript::Script *script,
                   void * intrinsicData,
                   uint32_t slot, void *data, size_t dataLength);
    void (*root)(const android::renderscript::RsForEachStubParamStruct *,
                 uint32_t x1, uint32_t x2, uint32_t instep, uint32_t outstep);

    void (*destroy)(const android::renderscript::Context *dc,
                    const android::renderscript::Script *script,
                    void * intrinsicData);
} RsdIntriniscFuncs_t;

struct DrvScript {
    RsScriptIntrinsicID mIntrinsicID;
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

    android::renderscript::Allocation **mBoundAllocs;
    RsdIntriniscFuncs_t mIntrinsicFuncs;
    void * mIntrinsicData;
};

typedef struct {
    android::renderscript::RsForEachStubParamStruct fep;
    uint32_t cpuIdx;

} MTThreadStuct;

typedef struct {
    android::renderscript::RsForEachStubParamStruct fep;

    android::renderscript::Context *rsc;
    android::renderscript::Script *script;
    ForEachFunc_t kernel;
    uint32_t sig;
    const android::renderscript::Allocation * ain;
    android::renderscript::Allocation * aout;

    uint32_t mSliceSize;
    volatile int mSliceNum;

    uint32_t xStart;
    uint32_t xEnd;
    uint32_t yStart;
    uint32_t yEnd;
    uint32_t zStart;
    uint32_t zEnd;
    uint32_t arrayStart;
    uint32_t arrayEnd;
} MTLaunchStruct;

void rsdScriptLaunchThreads(const android::renderscript::Context *rsc,
                            android::renderscript::Script *s,
                            uint32_t slot,
                            const android::renderscript::Allocation * ain,
                            android::renderscript::Allocation * aout,
                            const void * usr,
                            uint32_t usrLen,
                            const RsScriptCall *sc,
                            MTLaunchStruct *mtls);

void rsdScriptInvokeForEachMtlsSetup(const android::renderscript::Context *rsc,
                                     const android::renderscript::Allocation * ain,
                                     android::renderscript::Allocation * aout,
                                     const void * usr,
                                     uint32_t usrLen,
                                     const RsScriptCall *sc,
                                     MTLaunchStruct *mtls);




#endif
