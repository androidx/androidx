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



#include "rsCpuCore.h"

#include "rsCpuScript.h"
//#include "rsdRuntime.h"
//#include "rsdAllocation.h"
//#include "rsCpuIntrinsics.h"


#include "utils/Vector.h"
#include "utils/Timers.h"
#include "utils/StopWatch.h"

#include <dlfcn.h>
#include <stdio.h>
#include <string.h>


namespace android {
namespace renderscript {


#define MAXLINE 500
#define MAKE_STR_HELPER(S) #S
#define MAKE_STR(S) MAKE_STR_HELPER(S)
#define EXPORT_VAR_STR "exportVarCount: "
#define EXPORT_VAR_STR_LEN strlen(EXPORT_VAR_STR)
#define EXPORT_FUNC_STR "exportFuncCount: "
#define EXPORT_FUNC_STR_LEN strlen(EXPORT_FUNC_STR)
#define EXPORT_FOREACH_STR "exportForEachCount: "
#define EXPORT_FOREACH_STR_LEN strlen(EXPORT_FOREACH_STR)
#define OBJECT_SLOT_STR "objectSlotCount: "
#define OBJECT_SLOT_STR_LEN strlen(OBJECT_SLOT_STR)

// Copy up to a newline or size chars from str -> s, updating str
// Returns s when successful and NULL when '\0' is finally reached.
static char* strgets(char *s, int size, const char **ppstr) {
    if (!ppstr || !*ppstr || **ppstr == '\0' || size < 1) {
        return NULL;
    }

    int i;
    for (i = 0; i < (size - 1); i++) {
        s[i] = **ppstr;
        (*ppstr)++;
        if (s[i] == '\0') {
            return s;
        } else if (s[i] == '\n') {
            s[i+1] = '\0';
            return s;
        }
    }

    // size has been exceeded.
    s[i] = '\0';

    return s;
}


RsdCpuScriptImpl::RsdCpuScriptImpl(RsdCpuReferenceImpl *ctx, const Script *s) {
    mCtx = ctx;
    mScript = s;

    mScriptSO = NULL;
    mRoot = NULL;
    mRootExpand = NULL;
    mInit = NULL;
    mFreeChildren = NULL;
    mInvokeFunctions = NULL;
    mForEachFunctions = NULL;

    mFieldAddress = NULL;
    mFieldIsObject = NULL;
    mForEachSignatures = NULL;

    mBoundAllocs = NULL;
    mIntrinsicData = NULL;
    mIsThreadable = true;
}


bool RsdCpuScriptImpl::init(char const *resName, char const *cacheDir,
                            uint8_t const *bitcode, size_t bitcodeSize,
                            uint32_t flags) {
    //ALOGE("rsdScriptCreate %p %p %p %p %i %i %p", rsc, resName, cacheDir, bitcode, bitcodeSize, flags, lookupFunc);
    //ALOGE("rsdScriptInit %p %p", rsc, script);

    mCtx->lockMutex();

    #if 0
    bcc::RSExecutable *exec;
    const bcc::RSInfo *info;

    mCompilerContext = NULL;
    mCompilerDriver = NULL;
    mExecutable = NULL;

    mCompilerContext = new bcc::BCCContext();
    if (mCompilerContext == NULL) {
        ALOGE("bcc: FAILS to create compiler context (out of memory)");
        mCtx->unlockMutex();
        return false;
    }

    mCompilerDriver = new bcc::RSCompilerDriver();
    if (mCompilerDriver == NULL) {
        ALOGE("bcc: FAILS to create compiler driver (out of memory)");
        mCtx->unlockMutex();
        return false;
    }

    mCompilerDriver->setRSRuntimeLookupFunction(lookupRuntimeStub);
    mCompilerDriver->setRSRuntimeLookupContext(this);

    exec = mCompilerDriver->build(*mCompilerContext, cacheDir, resName,
                                  (const char *)bitcode, bitcodeSize, NULL);

    if (exec == NULL) {
        ALOGE("bcc: FAILS to prepare executable for '%s'", resName);
        mCtx->unlockMutex();
        return false;
    }

    mExecutable = exec;

    exec->setThreadable(mIsThreadable);
    if (!exec->syncInfo()) {
        ALOGW("bcc: FAILS to synchronize the RS info file to the disk");
    }

    mRoot = reinterpret_cast<int (*)()>(exec->getSymbolAddress("root"));
    mRootExpand =
        reinterpret_cast<int (*)()>(exec->getSymbolAddress("root.expand"));
    mInit = reinterpret_cast<void (*)()>(exec->getSymbolAddress("init"));
    mFreeChildren =
        reinterpret_cast<void (*)()>(exec->getSymbolAddress(".rs.dtor"));


    info = &mExecutable->getInfo();
    if (info->getExportVarNames().size()) {
        mBoundAllocs = new Allocation *[info->getExportVarNames().size()];
        memset(mBoundAllocs, 0, sizeof(void *) * info->getExportVarNames().size());
    }

    #endif

    String8 scriptSOName(cacheDir);
    scriptSOName = scriptSOName.getPathDir();
    scriptSOName.appendPath("lib");
    scriptSOName.append("/lib");
    scriptSOName.append(resName);
    scriptSOName.append(".so");

    //script->mHal.drv = drv;

    ALOGE("Opening up shared object: %s", scriptSOName.string());
    mScriptSO = dlopen(scriptSOName.string(), RTLD_NOW | RTLD_LOCAL);
    if (mScriptSO == NULL) {
        ALOGE("Unable to open shared library (%s): %s",
              scriptSOName.string(), dlerror());
        goto error;
    }

    if (mScriptSO) {
        char line[MAXLINE];
        mRoot = (RootFunc_t) dlsym(mScriptSO, "root");
        if (mRoot) {
            ALOGE("Found root(): %p", mRoot);
        }
        mRootExpand = (RootFunc_t) dlsym(mScriptSO, "root.expand");
        if (mRootExpand) {
            ALOGE("Found root.expand(): %p", mRootExpand);
        }
        mInit = (InvokeFunc_t) dlsym(mScriptSO, "init");
        if (mInit) {
            ALOGE("Found init(): %p", mInit);
        }
        mFreeChildren = (InvokeFunc_t) dlsym(mScriptSO, ".rs.dtor");
        if (mFreeChildren) {
            ALOGE("Found .rs.dtor(): %p", mFreeChildren);
        }

        const char *rsInfo = (const char *) dlsym(mScriptSO, ".rs.info");
        if (rsInfo) {
            ALOGE("Found .rs.info(): %p - %s", rsInfo, rsInfo);
        }

        size_t varCount = 0;
        if (strgets(line, MAXLINE, &rsInfo) == NULL) {
            goto error;
        }
        if (sscanf(line, EXPORT_VAR_STR "%zu", &varCount) != 1) {
            ALOGE("Invalid export var count!: %s", line);
            goto error;
        }

        mExportedVariableCount = varCount;
        ALOGE("varCount: %zu", varCount);
        if (varCount > 0) {
            // Start by creating/zeroing this member, since we don't want to
            // accidentally clean up invalid pointers later (if we error out).
            mFieldIsObject = new bool[varCount];
            if (mFieldIsObject == NULL) {
                goto error;
            }
            memset(mFieldIsObject, 0, varCount * sizeof(*mFieldIsObject));
            mFieldAddress = new void*[varCount];
            if (mFieldAddress == NULL) {
                goto error;
            }
            for (size_t i = 0; i < varCount; ++i) {
                if (strgets(line, MAXLINE, &rsInfo) == NULL) {
                    goto error;
                }
                char *c = strrchr(line, '\n');
                if (c) {
                    *c = '\0';
                }
                mFieldAddress[i] = dlsym(mScriptSO, line);
                if (mFieldAddress[i] == NULL) {
                    ALOGE("Failed to find variable address for %s: %s",
                          line, dlerror());
                    // Not a critical error if we don't find a global variable.
                }
                else {
                    ALOGE("Found variable %s at %p", line,
                          mFieldAddress[i]);
                }
            }
        }

        size_t funcCount = 0;
        if (strgets(line, MAXLINE, &rsInfo) == NULL) {
            goto error;
        }
        if (sscanf(line, EXPORT_FUNC_STR "%zu", &funcCount) != 1) {
            ALOGE("Invalid export func count!: %s", line);
            goto error;
        }

        mExportedFunctionCount = funcCount;
        ALOGE("funcCount: %zu", funcCount);

        if (funcCount > 0) {
            mInvokeFunctions = new InvokeFunc_t[funcCount];
            if (mInvokeFunctions == NULL) {
                goto error;
            }
            for (size_t i = 0; i < funcCount; ++i) {
                if (strgets(line, MAXLINE, &rsInfo) == NULL) {
                    goto error;
                }
                char *c = strrchr(line, '\n');
                if (c) {
                    *c = '\0';
                }

                mInvokeFunctions[i] = (InvokeFunc_t) dlsym(mScriptSO, line);
                if (mInvokeFunctions[i] == NULL) {
                    ALOGE("Failed to get function address for %s(): %s",
                          line, dlerror());
                    goto error;
                }
                else {
                    ALOGE("Found InvokeFunc_t %s at %p", line, mInvokeFunctions[i]);
                }
            }
        }

        size_t forEachCount = 0;
        if (strgets(line, MAXLINE, &rsInfo) == NULL) {
            goto error;
        }
        if (sscanf(line, EXPORT_FOREACH_STR "%zu", &forEachCount) != 1) {
            ALOGE("Invalid export forEach count!: %s", line);
            goto error;
        }

        if (forEachCount > 0) {

            mForEachSignatures = new uint32_t[forEachCount];
            if (mForEachSignatures == NULL) {
                goto error;
            }
            mForEachFunctions = new ForEachFunc_t[forEachCount];
            if (mForEachFunctions == NULL) {
                goto error;
            }
            for (size_t i = 0; i < forEachCount; ++i) {
                unsigned int tmpSig = 0;
                char tmpName[MAXLINE];

                if (strgets(line, MAXLINE, &rsInfo) == NULL) {
                    goto error;
                }
                if (sscanf(line, "%u - %" MAKE_STR(MAXLINE) "s",
                           &tmpSig, tmpName) != 2) {
                    ALOGE("Invalid export forEach!: %s", line);
                    goto error;
                }

                // Lookup the expanded ForEach kernel.
                strncat(tmpName, ".expand", MAXLINE-1-strlen(tmpName));
                mForEachSignatures[i] = tmpSig;
                mForEachFunctions[i] =
                        (ForEachFunc_t) dlsym(mScriptSO, tmpName);
                if (mForEachFunctions[i] == NULL) {
                    ALOGE("Failed to find forEach function address for %s: %s",
                          tmpName, dlerror());
                    // Ignore missing root.expand functions.
                    // root() is always specified at location 0.
                    if (i != 0) {
                        goto error;
                    }
                }
                else {
                    ALOGE("Found forEach %s at %p", tmpName, mForEachFunctions[i]);
                }
            }
        }

        size_t objectSlotCount = 0;
        if (strgets(line, MAXLINE, &rsInfo) == NULL) {
            goto error;
        }
        if (sscanf(line, OBJECT_SLOT_STR "%zu", &objectSlotCount) != 1) {
            ALOGE("Invalid object slot count!: %s", line);
            goto error;
        }

        if (objectSlotCount > 0) {
            rsAssert(varCount > 0);
            for (size_t i = 0; i < objectSlotCount; ++i) {
                uint32_t varNum = 0;
                if (strgets(line, MAXLINE, &rsInfo) == NULL) {
                    goto error;
                }
                if (sscanf(line, "%u", &varNum) != 1) {
                    ALOGE("Invalid object slot!: %s", line);
                    goto error;
                }

                if (varNum < varCount) {
                    mFieldIsObject[varNum] = true;
                }
            }
        }

        if (varCount > 0) {
            mBoundAllocs = new Allocation *[varCount];
            memset(mBoundAllocs, 0, varCount * sizeof(*mBoundAllocs));
        }

        if (mScriptSO == (void*)1) {
            //rsdLookupRuntimeStub(script, "acos");
        }
    }

    mCtx->unlockMutex();
    return true;

error:

    mCtx->unlockMutex();
    delete[] mInvokeFunctions;
    delete[] mForEachFunctions;
    delete[] mFieldAddress;
    delete[] mFieldIsObject;
    delete[] mForEachSignatures;
    delete[] mBoundAllocs;
    if (mScriptSO) {
        dlclose(mScriptSO);
    }
    return false;
}

void RsdCpuScriptImpl::populateScript(Script *script) {
    // Copy info over to runtime
    script->mHal.info.exportedFunctionCount = mExportedFunctionCount;
    script->mHal.info.exportedVariableCount = mExportedVariableCount;
    script->mHal.info.exportedPragmaCount = 0;
    script->mHal.info.exportedPragmaKeyList = 0;
    script->mHal.info.exportedPragmaValueList = 0;

    // Bug, need to stash in metadata
    if (mRootExpand) {
        script->mHal.info.root = mRootExpand;
    } else {
        script->mHal.info.root = mRoot;
    }
}


typedef void (*rs_t)(const void *, void *, const void *, uint32_t, uint32_t, uint32_t, uint32_t);

void RsdCpuScriptImpl::forEachMtlsSetup(const Allocation * ain, Allocation * aout,
                                        const void * usr, uint32_t usrLen,
                                        const RsScriptCall *sc,
                                        MTLaunchStruct *mtls) {

    memset(mtls, 0, sizeof(MTLaunchStruct));

    if (ain) {
        mtls->fep.dimX = ain->getType()->getDimX();
        mtls->fep.dimY = ain->getType()->getDimY();
        mtls->fep.dimZ = ain->getType()->getDimZ();
        //mtls->dimArray = ain->getType()->getDimArray();
    } else if (aout) {
        mtls->fep.dimX = aout->getType()->getDimX();
        mtls->fep.dimY = aout->getType()->getDimY();
        mtls->fep.dimZ = aout->getType()->getDimZ();
        //mtls->dimArray = aout->getType()->getDimArray();
    } else {
        mCtx->getContext()->setError(RS_ERROR_BAD_SCRIPT, "rsForEach called with null allocations");
        return;
    }

    if (!sc || (sc->xEnd == 0)) {
        mtls->xEnd = mtls->fep.dimX;
    } else {
        rsAssert(sc->xStart < mtls->fep.dimX);
        rsAssert(sc->xEnd <= mtls->fep.dimX);
        rsAssert(sc->xStart < sc->xEnd);
        mtls->xStart = rsMin(mtls->fep.dimX, sc->xStart);
        mtls->xEnd = rsMin(mtls->fep.dimX, sc->xEnd);
        if (mtls->xStart >= mtls->xEnd) return;
    }

    if (!sc || (sc->yEnd == 0)) {
        mtls->yEnd = mtls->fep.dimY;
    } else {
        rsAssert(sc->yStart < mtls->fep.dimY);
        rsAssert(sc->yEnd <= mtls->fep.dimY);
        rsAssert(sc->yStart < sc->yEnd);
        mtls->yStart = rsMin(mtls->fep.dimY, sc->yStart);
        mtls->yEnd = rsMin(mtls->fep.dimY, sc->yEnd);
        if (mtls->yStart >= mtls->yEnd) return;
    }

    mtls->xEnd = rsMax((uint32_t)1, mtls->xEnd);
    mtls->yEnd = rsMax((uint32_t)1, mtls->yEnd);
    mtls->zEnd = rsMax((uint32_t)1, mtls->zEnd);
    mtls->arrayEnd = rsMax((uint32_t)1, mtls->arrayEnd);

    rsAssert(!ain || (ain->getType()->getDimZ() == 0));

    mtls->rsc = mCtx;
    mtls->ain = ain;
    mtls->aout = aout;
    mtls->fep.usr = usr;
    mtls->fep.usrLen = usrLen;
    mtls->mSliceSize = 1;
    mtls->mSliceNum = 0;

    mtls->fep.ptrIn = NULL;
    mtls->fep.eStrideIn = 0;
    mtls->isThreadable = mIsThreadable;

    if (ain) {
        mtls->fep.ptrIn = (const uint8_t *)ain->mHal.drvState.lod[0].mallocPtr;
        mtls->fep.eStrideIn = ain->getType()->getElementSizeBytes();
        mtls->fep.yStrideIn = ain->mHal.drvState.lod[0].stride;
    }

    mtls->fep.ptrOut = NULL;
    mtls->fep.eStrideOut = 0;
    if (aout) {
        mtls->fep.ptrOut = (uint8_t *)aout->mHal.drvState.lod[0].mallocPtr;
        mtls->fep.eStrideOut = aout->getType()->getElementSizeBytes();
        mtls->fep.yStrideOut = aout->mHal.drvState.lod[0].stride;
    }
}


void RsdCpuScriptImpl::invokeForEach(uint32_t slot,
                                     const Allocation * ain,
                                     Allocation * aout,
                                     const void * usr,
                                     uint32_t usrLen,
                                     const RsScriptCall *sc) {

    MTLaunchStruct mtls;
    forEachMtlsSetup(ain, aout, usr, usrLen, sc, &mtls);
    forEachKernelSetup(slot, &mtls);

    RsdCpuScriptImpl * oldTLS = mCtx->setTLS(this);
    mCtx->launchThreads(ain, aout, sc, &mtls);
    mCtx->setTLS(oldTLS);
}

void RsdCpuScriptImpl::forEachKernelSetup(uint32_t slot, MTLaunchStruct *mtls) {

    mtls->script = this;
    mtls->fep.slot = slot;
    mtls->kernel = reinterpret_cast<ForEachFunc_t>(mForEachFunctions[slot]);
    rsAssert(mtls->kernel != NULL);
    mtls->sig = mForEachSignatures[slot];
}

int RsdCpuScriptImpl::invokeRoot() {
    RsdCpuScriptImpl * oldTLS = mCtx->setTLS(this);
    int ret = mRoot();
    mCtx->setTLS(oldTLS);
    return ret;
}

void RsdCpuScriptImpl::invokeInit() {
    if (mInit) {
        mInit();
    }
}

void RsdCpuScriptImpl::invokeFreeChildren() {
    if (mFreeChildren) {
        mFreeChildren();
    }
}

void RsdCpuScriptImpl::invokeFunction(uint32_t slot, const void *params,
                                      size_t paramLength) {
    //ALOGE("invoke %p %p %i %p %i", dc, script, slot, params, paramLength);

    RsdCpuScriptImpl * oldTLS = mCtx->setTLS(this);
    reinterpret_cast<void (*)(const void *, uint32_t)>(
        mInvokeFunctions[slot])(params, paramLength);
    mCtx->setTLS(oldTLS);
}

void RsdCpuScriptImpl::setGlobalVar(uint32_t slot, const void *data, size_t dataLength) {
    //rsAssert(!script->mFieldIsObject[slot]);
    //ALOGE("setGlobalVar %p %p %i %p %i", dc, script, slot, data, dataLength);

    //if (mIntrinsicID) {
        //mIntrinsicFuncs.setVar(dc, script, drv->mIntrinsicData, slot, data, dataLength);
        //return;
    //}

    int32_t *destPtr = reinterpret_cast<int32_t *>(mFieldAddress[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    memcpy(destPtr, data, dataLength);
}

void RsdCpuScriptImpl::setGlobalVarWithElemDims(uint32_t slot, const void *data, size_t dataLength,
                                                const Element *elem,
                                                const size_t *dims, size_t dimLength) {

    int32_t *destPtr = reinterpret_cast<int32_t *>(mFieldAddress[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    // We want to look at dimension in terms of integer components,
    // but dimLength is given in terms of bytes.
    dimLength /= sizeof(int);

    // Only a single dimension is currently supported.
    rsAssert(dimLength == 1);
    if (dimLength == 1) {
        // First do the increment loop.
        size_t stride = elem->getSizeBytes();
        const char *cVal = reinterpret_cast<const char *>(data);
        for (size_t i = 0; i < dims[0]; i++) {
            elem->incRefs(cVal);
            cVal += stride;
        }

        // Decrement loop comes after (to prevent race conditions).
        char *oldVal = reinterpret_cast<char *>(destPtr);
        for (size_t i = 0; i < dims[0]; i++) {
            elem->decRefs(oldVal);
            oldVal += stride;
        }
    }

    memcpy(destPtr, data, dataLength);
}

void RsdCpuScriptImpl::setGlobalBind(uint32_t slot, Allocation *data) {

    //rsAssert(!script->mFieldIsObject[slot]);
    //ALOGE("setGlobalBind %p %p %i %p", dc, script, slot, data);

    int32_t *destPtr = reinterpret_cast<int32_t *>(mFieldAddress[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    void *ptr = NULL;
    mBoundAllocs[slot] = data;
    if(data) {
        ptr = data->mHal.drvState.lod[0].mallocPtr;
    }
    memcpy(destPtr, &ptr, sizeof(void *));
}

void RsdCpuScriptImpl::setGlobalObj(uint32_t slot, ObjectBase *data) {

    //rsAssert(script->mFieldIsObject[slot]);
    //ALOGE("setGlobalObj %p %p %i %p", dc, script, slot, data);

    //if (mIntrinsicID) {
        //mIntrinsicFuncs.setVarObj(dc, script, drv->mIntrinsicData, slot, alloc);
        //return;
    //}

    int32_t *destPtr = reinterpret_cast<int32_t *>(mFieldAddress[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    rsrSetObject(mCtx->getContext(), (ObjectBase **)destPtr, data);
}

RsdCpuScriptImpl::~RsdCpuScriptImpl() {
    for (size_t i = 0; i < mExportedVariableCount; ++i) {
        if (mFieldIsObject[i]) {
            if (mFieldAddress[i] != NULL) {
                ObjectBase **obj_addr =
                    reinterpret_cast<ObjectBase **>(mFieldAddress[i]);
                rsrClearObject(mCtx->getContext(), obj_addr);
            }
        }
    }

    if (mInvokeFunctions) delete[] mInvokeFunctions;
    if (mForEachFunctions) delete[] mForEachFunctions;
    if (mFieldAddress) delete[] mFieldAddress;
    if (mFieldIsObject) delete[] mFieldIsObject;
    if (mForEachSignatures) delete[] mForEachSignatures;
    if (mBoundAllocs) delete[] mBoundAllocs;
    if (mScriptSO) {
        dlclose(mScriptSO);
    }
}

Allocation * RsdCpuScriptImpl::getAllocationForPointer(const void *ptr) const {
    if (!ptr) {
        return NULL;
    }

    for (uint32_t ct=0; ct < mScript->mHal.info.exportedVariableCount; ct++) {
        Allocation *a = mBoundAllocs[ct];
        if (!a) continue;
        if (a->mHal.drvState.lod[0].mallocPtr == ptr) {
            return a;
        }
    }
    ALOGE("rsGetAllocation, failed to find %p", ptr);
    return NULL;
}


}
}
