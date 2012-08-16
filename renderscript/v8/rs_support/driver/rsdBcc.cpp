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

#include "rsdCore.h"
#include "rsdBcc.h"
#include "rsdRuntime.h"
#include "rsdAllocation.h"


#include <bcinfo/MetadataExtractor.h>

#include "rsContext.h"
#include "rsElement.h"
#include "rsScriptC.h"

#include "utils/Vector.h"
#include "utils/Timers.h"
#include "utils/StopWatch.h"
#include "utils/String8.h"

#include <dlfcn.h>

using namespace android;
using namespace android::renderscript;

struct DrvScript {
    RootFunc_t mRoot;
    RootFunc_t mRootExpand;
    InvokeFunc_t mInit;
    InvokeFunc_t mFreeChildren;


    InvokeFunc_t *mInvokeFunctions;
    ForEachFunc_t *mForEachFunctions;
    void **mFieldAddress;
    bool *mFieldIsObject;
    uint32_t *mForEachSignatures;

    Allocation **mBoundAllocs;
};

typedef void (*outer_foreach_t)(
    const android::renderscript::RsForEachStubParamStruct *,
    uint32_t x1, uint32_t x2,
    uint32_t instep, uint32_t outstep);

static Script * setTLS(Script *sc) {
    ScriptTLSStruct * tls = (ScriptTLSStruct *)pthread_getspecific(rsdgThreadTLSKey);
    rsAssert(tls);
    Script *old = tls->mScript;
    tls->mScript = sc;
    return old;
}


bool rsdScriptInit(const Context *rsc,
                     ScriptC *script,
                     char const *resName,
                     char const *cacheDir,
                     uint8_t const *bitcode,
                     size_t bitcodeSize,
                     uint32_t flags) {
    //ALOGE("rsdScriptCreate %p %p %p %p %i %i %p", rsc, resName, cacheDir, bitcode, bitcodeSize, flags, lookupFunc);
    //ALOGE("rsdScriptInit %p %p", rsc, script);

    pthread_mutex_lock(&rsdgInitMutex);

    String8 scriptSOName(cacheDir);
    scriptSOName = scriptSOName.getPathDir();
    scriptSOName.appendPath("lib");
    scriptSOName.append("/lib");
    scriptSOName.append(resName);
    scriptSOName.append(".so");
    ALOGE("Opening up shared object: %s", scriptSOName.string());
    void *scriptSO = dlopen(scriptSOName.string(), RTLD_NOW | RTLD_LOCAL);
    if (scriptSO == NULL) {
        ALOGE("Unable to open shared library (%s): %s",
              scriptSOName.string(), dlerror());
        return false;
    }

    void *scriptSORoot = dlsym(scriptSO, "root");
    if (scriptSORoot != NULL) {
        ALOGE("Root function found at %p", scriptSORoot);
    }

    bcinfo::MetadataExtractor ME((const char *)bitcode, bitcodeSize);
    if (!ME.extract()) {
        ALOGE("Failed to extract bitcode metadata: %s.bc", resName);
        return false;
    }

    const char* coreLib = "/system/lib/libclcore.bc";
    DrvScript *drv = (DrvScript *)calloc(1, sizeof(DrvScript));
    if (drv == NULL) {
        goto error;
    }
    script->mHal.drv = drv;


    if (scriptSO) {
        drv->mRoot = (RootFunc_t) dlsym(scriptSO, "root");
        if (drv->mRoot) {
            ALOGE("Found root(): %p", drv->mRoot);
        }
        drv->mRootExpand = (RootFunc_t) dlsym(scriptSO, "root.expand");
        if (drv->mRootExpand) {
            ALOGE("Found root.expand(): %p", drv->mRootExpand);
        }
        drv->mInit = (InvokeFunc_t) dlsym(scriptSO, "init");
        if (drv->mInit) {
            ALOGE("Found init(): %p", drv->mInit);
        }
        drv->mFreeChildren = (InvokeFunc_t) dlsym(scriptSO, ".rs.dtor");
        if (drv->mFreeChildren) {
            ALOGE("Found .rs.dtor(): %p", drv->mFreeChildren);
        }

        size_t funcCount = ME.getExportFuncCount();
        script->mHal.info.exportedFunctionCount = funcCount;
        ALOGE("funcCount: %zu", funcCount);

        if (funcCount > 0) {
            drv->mInvokeFunctions = new InvokeFunc_t[funcCount];
            if (drv->mInvokeFunctions == NULL) {
                goto error;
            }
            const char **funcNameList = ME.getExportFuncNameList();
            for (size_t i = 0; i < funcCount; ++i) {
                drv->mInvokeFunctions[i] =
                        (InvokeFunc_t) dlsym(scriptSO, funcNameList[i]);
                if (drv->mInvokeFunctions[i] == NULL) {
                    ALOGE("Failed to get function address for %s(): %s",
                          funcNameList[i], dlerror());
                    goto error;
                }
                else {
                    ALOGE("Found InvokeFunc_t %s at %p", funcNameList[i],
                          drv->mInvokeFunctions[i]);
                }
            }
        }

        size_t varCount = ME.getExportVarCount();
        script->mHal.info.exportedVariableCount = varCount;
        ALOGE("varCount: %zu", varCount);
        if (varCount > 0) {
            // Start by creating/zeroing this member, since we don't want to
            // accidentally clean up invalid pointers later (if we error out).
            drv->mFieldIsObject = new bool[varCount];
            if (drv->mFieldIsObject == NULL) {
                goto error;
            }
            memset(drv->mFieldIsObject, 0,
                   varCount * sizeof(*drv->mFieldIsObject));
            drv->mFieldAddress = new void*[varCount];
            if (drv->mFieldAddress == NULL) {
                goto error;
            }
            const char **varNameList = ME.getExportVarNameList();
            for (size_t i = 0; i < varCount; ++i) {
                drv->mFieldAddress[i] = dlsym(scriptSO, varNameList[i]);
                if (drv->mFieldAddress[i] == NULL) {
                    ALOGE("Failed to find variable address for %s: %s",
                          varNameList[i], dlerror());
                // Not a critical error if we don't find a global variable.
                }
                else {
                    ALOGE("Found variable %s at %p", varNameList[i],
                          drv->mFieldAddress[i]);
                }
            }
        }

        size_t objectSlotCount = ME.getObjectSlotCount();
        if (objectSlotCount > 0) {
            rsAssert(varCount > 0);
            const uint32_t *objectSlotList = ME.getObjectSlotList();
            for (size_t i = 0; i < objectSlotCount; ++i) {
                uint32_t varNum = objectSlotList[i];
                if (varNum < varCount) {
                    drv->mFieldIsObject[objectSlotList[i]] = true;
                }
            }
        }

        size_t forEachCount = ME.getExportForEachSignatureCount();
        if (forEachCount > 0) {
            const uint32_t *sigList = ME.getExportForEachSignatureList();
            const char **nameList = ME.getExportForEachNameList();

            drv->mForEachSignatures = new uint32_t[forEachCount];
            if (drv->mForEachSignatures == NULL) {
                goto error;
            }
            drv->mForEachFunctions = new ForEachFunc_t[forEachCount];
            if (drv->mForEachFunctions == NULL) {
                goto error;
            }
            for (size_t i = 0; i < forEachCount; ++i) {
                drv->mForEachSignatures[i] = sigList[i];
                drv->mForEachFunctions[i] =
                        (ForEachFunc_t) dlsym(scriptSO, nameList[i]);
                if (drv->mForEachFunctions[i] == NULL) {
                    ALOGE("Failed to find forEach function address for %s: %s",
                          nameList[i], dlerror());
                    goto error;
                }
                else {
                    // TODO - Maybe add ForEachExpandPass to .so creation and
                    // then lookup the ".expand" version of these kernels
                    // instead.
                    ALOGE("Found forEach %s at %p", nameList[i],
                          drv->mForEachFunctions[i]);
                }
            }
        }

        // TODO - Do we care about pragmas at all in this layer of FS?
        script->mHal.info.exportedPragmaCount = ME.getPragmaCount();
        ALOGE("pragmaCount: %zu", script->mHal.info.exportedPragmaCount);
        script->mHal.info.exportedPragmaCount = 0;

        if (drv->mRootExpand) {
            script->mHal.info.root = drv->mRootExpand;
        } else {
            script->mHal.info.root = drv->mRoot;
        }

        if (varCount > 0) {
            drv->mBoundAllocs = new Allocation *[varCount];
            memset(drv->mBoundAllocs, 0, varCount * sizeof(*drv->mBoundAllocs));
        }

        script->mHal.info.isThreadable = true;

        if (scriptSO == (void*)1) {
            rsdLookupRuntimeStub(script, "acos");
        }
    }

    pthread_mutex_unlock(&rsdgInitMutex);
    return true;

error:

    pthread_mutex_unlock(&rsdgInitMutex);
    if (drv) {
        delete[] drv->mInvokeFunctions;
        delete[] drv->mForEachFunctions;
        delete[] drv->mFieldAddress;
        delete[] drv->mFieldIsObject;
        delete[] drv->mForEachSignatures;
        delete[] drv->mBoundAllocs;
        free(drv);
    }
    script->mHal.drv = NULL;
    return false;

}

typedef struct {
    Context *rsc;
    Script *script;
    ForEachFunc_t kernel;
    uint32_t sig;
    const Allocation * ain;
    Allocation * aout;
    const void * usr;
    size_t usrLen;

    uint32_t mSliceSize;
    volatile int mSliceNum;

    const uint8_t *ptrIn;
    uint32_t eStrideIn;
    uint8_t *ptrOut;
    uint32_t eStrideOut;

    uint32_t yStrideIn;
    uint32_t yStrideOut;

    uint32_t xStart;
    uint32_t xEnd;
    uint32_t yStart;
    uint32_t yEnd;
    uint32_t zStart;
    uint32_t zEnd;
    uint32_t arrayStart;
    uint32_t arrayEnd;

    uint32_t dimX;
    uint32_t dimY;
    uint32_t dimZ;
    uint32_t dimArray;
} MTLaunchStruct;
typedef void (*rs_t)(const void *, void *, const void *, uint32_t, uint32_t, uint32_t, uint32_t);

static void wc_xy(void *usr, uint32_t idx) {
    MTLaunchStruct *mtls = (MTLaunchStruct *)usr;
    RsForEachStubParamStruct p;
    memset(&p, 0, sizeof(p));
    p.usr = mtls->usr;
    p.usr_len = mtls->usrLen;
    RsdHal * dc = (RsdHal *)mtls->rsc->mHal.drv;
    uint32_t sig = mtls->sig;

    rs_t bare_fn = (rs_t) mtls->kernel;
    while (1) {
        uint32_t slice = (uint32_t)android_atomic_inc(&mtls->mSliceNum);
        uint32_t yStart = mtls->yStart + slice * mtls->mSliceSize;
        uint32_t yEnd = yStart + mtls->mSliceSize;
        yEnd = rsMin(yEnd, mtls->yEnd);
        if (yEnd <= yStart) {
            return;
        }

        //ALOGE("usr idx %i, x %i,%i  y %i,%i", idx, mtls->xStart, mtls->xEnd, yStart, yEnd);
        //ALOGE("usr ptr in %p,  out %p", mtls->ptrIn, mtls->ptrOut);
        for (p.y = yStart; p.y < yEnd; p.y++) {
            p.out = mtls->ptrOut + (mtls->yStrideOut * p.y);
            p.in = mtls->ptrIn + (mtls->yStrideIn * p.y);
            for (uint32_t x = mtls->xStart; x < mtls->xEnd; ++x) {
                bare_fn(p.in, p.out, p.usr, x, p.y, 0, 0);
                p.in = (char *)(p.in) + mtls->eStrideIn;
                p.out = (char *)(p.out) + mtls->eStrideOut;
            }
        }
    }
}

static void wc_x(void *usr, uint32_t idx) {
    MTLaunchStruct *mtls = (MTLaunchStruct *)usr;
    RsForEachStubParamStruct p;
    memset(&p, 0, sizeof(p));
    p.usr = mtls->usr;
    p.usr_len = mtls->usrLen;
    RsdHal * dc = (RsdHal *)mtls->rsc->mHal.drv;
    uint32_t sig = mtls->sig;

    rs_t bare_fn = (rs_t) mtls->kernel;
    while (1) {
        uint32_t slice = (uint32_t)android_atomic_inc(&mtls->mSliceNum);
        uint32_t xStart = mtls->xStart + slice * mtls->mSliceSize;
        uint32_t xEnd = xStart + mtls->mSliceSize;
        xEnd = rsMin(xEnd, mtls->xEnd);
        if (xEnd <= xStart) {
            return;
        }

        //ALOGE("usr slice %i idx %i, x %i,%i", slice, idx, xStart, xEnd);
        //ALOGE("usr ptr in %p,  out %p", mtls->ptrIn, mtls->ptrOut);

        p.out = mtls->ptrOut + (mtls->eStrideOut * xStart);
        p.in = mtls->ptrIn + (mtls->eStrideIn * xStart);
        for (uint32_t x = mtls->xStart; x < mtls->xEnd; ++x) {
            bare_fn(p.in, p.out, p.usr, x, 0, 0, 0);
            p.in = (char *)(p.in) + mtls->eStrideIn;
            p.out = (char *)(p.out) + mtls->eStrideOut;
        }
    }
}

void rsdScriptInvokeForEach(const Context *rsc,
                            Script *s,
                            uint32_t slot,
                            const Allocation * ain,
                            Allocation * aout,
                            const void * usr,
                            uint32_t usrLen,
                            const RsScriptCall *sc) {

    RsdHal * dc = (RsdHal *)rsc->mHal.drv;

    MTLaunchStruct mtls;
    memset(&mtls, 0, sizeof(mtls));

    //ALOGE("for each script %p  in %p   out %p", s, ain, aout);

    DrvScript *drv = (DrvScript *)s->mHal.drv;
    //rsAssert(slot < drv->mExecutable->getExportForeachFuncAddrs().size());
    mtls.kernel = drv->mForEachFunctions[slot];
    rsAssert(mtls.kernel != NULL);
    mtls.sig = drv->mForEachSignatures[slot];

    if (ain) {
        mtls.dimX = ain->getType()->getDimX();
        mtls.dimY = ain->getType()->getDimY();
        mtls.dimZ = ain->getType()->getDimZ();
        //mtls.dimArray = ain->getType()->getDimArray();
    } else if (aout) {
        mtls.dimX = aout->getType()->getDimX();
        mtls.dimY = aout->getType()->getDimY();
        mtls.dimZ = aout->getType()->getDimZ();
        //mtls.dimArray = aout->getType()->getDimArray();
    } else {
        rsc->setError(RS_ERROR_BAD_SCRIPT, "rsForEach called with null allocations");
        return;
    }

    if (!sc || (sc->xEnd == 0)) {
        mtls.xEnd = mtls.dimX;
    } else {
        rsAssert(sc->xStart < mtls.dimX);
        rsAssert(sc->xEnd <= mtls.dimX);
        rsAssert(sc->xStart < sc->xEnd);
        mtls.xStart = rsMin(mtls.dimX, sc->xStart);
        mtls.xEnd = rsMin(mtls.dimX, sc->xEnd);
        if (mtls.xStart >= mtls.xEnd) return;
    }

    if (!sc || (sc->yEnd == 0)) {
        mtls.yEnd = mtls.dimY;
    } else {
        rsAssert(sc->yStart < mtls.dimY);
        rsAssert(sc->yEnd <= mtls.dimY);
        rsAssert(sc->yStart < sc->yEnd);
        mtls.yStart = rsMin(mtls.dimY, sc->yStart);
        mtls.yEnd = rsMin(mtls.dimY, sc->yEnd);
        if (mtls.yStart >= mtls.yEnd) return;
    }

    mtls.xEnd = rsMax((uint32_t)1, mtls.xEnd);
    mtls.yEnd = rsMax((uint32_t)1, mtls.yEnd);
    mtls.zEnd = rsMax((uint32_t)1, mtls.zEnd);
    mtls.arrayEnd = rsMax((uint32_t)1, mtls.arrayEnd);

    rsAssert(!ain || (ain->getType()->getDimZ() == 0));

    Context *mrsc = (Context *)rsc;
    Script * oldTLS = setTLS(s);

    mtls.rsc = mrsc;
    mtls.ain = ain;
    mtls.aout = aout;
    mtls.script = s;
    mtls.usr = usr;
    mtls.usrLen = usrLen;
    mtls.mSliceSize = 10;
    mtls.mSliceNum = 0;

    mtls.ptrIn = NULL;
    mtls.eStrideIn = 0;
    if (ain) {
        DrvAllocation *aindrv = (DrvAllocation *)ain->mHal.drv;
        mtls.ptrIn = (const uint8_t *)aindrv->lod[0].mallocPtr;
        mtls.eStrideIn = ain->getType()->getElementSizeBytes();
        mtls.yStrideIn = aindrv->lod[0].stride;
    }

    mtls.ptrOut = NULL;
    mtls.eStrideOut = 0;
    if (aout) {
        DrvAllocation *aoutdrv = (DrvAllocation *)aout->mHal.drv;
        mtls.ptrOut = (uint8_t *)aoutdrv->lod[0].mallocPtr;
        mtls.eStrideOut = aout->getType()->getElementSizeBytes();
        mtls.yStrideOut = aoutdrv->lod[0].stride;
    }

    if ((dc->mWorkers.mCount > 1) && s->mHal.info.isThreadable && !dc->mInForEach) {
        dc->mInForEach = true;
        if (mtls.dimY > 1) {
            mtls.mSliceSize = mtls.dimY / (dc->mWorkers.mCount * 4);
            if(mtls.mSliceSize < 1) {
                mtls.mSliceSize = 1;
            }

            rsdLaunchThreads(mrsc, wc_xy, &mtls);
        } else {
            mtls.mSliceSize = mtls.dimX / (dc->mWorkers.mCount * 4);
            if(mtls.mSliceSize < 1) {
                mtls.mSliceSize = 1;
            }

            rsdLaunchThreads(mrsc, wc_x, &mtls);
        }
        dc->mInForEach = false;

        //ALOGE("launch 1");
    } else {
        RsForEachStubParamStruct p;
        memset(&p, 0, sizeof(p));
        p.usr = mtls.usr;
        p.usr_len = mtls.usrLen;
        uint32_t sig = mtls.sig;

        //ALOGE("launch 3");
        rs_t bare_fn = (rs_t) mtls.kernel;
        for (p.ar[0] = mtls.arrayStart; p.ar[0] < mtls.arrayEnd; p.ar[0]++) {
            for (p.z = mtls.zStart; p.z < mtls.zEnd; p.z++) {
                for (p.y = mtls.yStart; p.y < mtls.yEnd; p.y++) {
                    uint32_t offset = mtls.dimX * mtls.dimY * mtls.dimZ * p.ar[0] +
                                      mtls.dimX * mtls.dimY * p.z +
                                      mtls.dimX * p.y;
                    p.out = mtls.ptrOut + (mtls.eStrideOut * offset);
                    p.in = mtls.ptrIn + (mtls.eStrideIn * offset);
                    for (uint32_t x = mtls.xStart; x < mtls.xEnd; ++x) {
                        // TODO verify the z/ar are correct for this sort of
                        // function signature. Honestly, we could probably
                        // always pass "0, 0", since I don't believe llvm-rs-cc
                        // allows kernels like this.
                        bare_fn(p.in, p.out, p.usr, x, p.y, p.z, p.ar[0]);
                        p.in = (char *)(p.in) + mtls.eStrideIn;
                        p.out = (char *)(p.out) + mtls.eStrideOut;
                    }
                }
            }
        }
    }

    setTLS(oldTLS);
}


int rsdScriptInvokeRoot(const Context *dc, Script *script) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    Script * oldTLS = setTLS(script);
    int ret = drv->mRoot();
    setTLS(oldTLS);

    return ret;
}

void rsdScriptInvokeInit(const Context *dc, Script *script) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    if (drv->mInit) {
        drv->mInit();
    }
}

void rsdScriptInvokeFreeChildren(const Context *dc, Script *script) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    if (drv->mFreeChildren) {
        drv->mFreeChildren();
    }
}

void rsdScriptInvokeFunction(const Context *dc, Script *script,
                            uint32_t slot,
                            const void *params,
                            size_t paramLength) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;
    //ALOGE("invoke %p %p %i %p %i", dc, script, slot, params, paramLength);

    Script * oldTLS = setTLS(script);
    reinterpret_cast<void (*)(const void *, uint32_t)>(
        drv->mInvokeFunctions[slot])(params, paramLength);
    setTLS(oldTLS);
}

void rsdScriptSetGlobalVar(const Context *dc, const Script *script,
                           uint32_t slot, void *data, size_t dataLength) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;
    //rsAssert(!script->mFieldIsObject[slot]);
    //ALOGE("setGlobalVar %p %p %i %p %i", dc, script, slot, data, dataLength);

    int32_t *destPtr = reinterpret_cast<int32_t *>(drv->mFieldAddress[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    memcpy(destPtr, data, dataLength);
}

void rsdScriptSetGlobalVarWithElemDims(
        const android::renderscript::Context *dc,
        const android::renderscript::Script *script,
        uint32_t slot, void *data, size_t dataLength,
        const android::renderscript::Element *elem,
        const size_t *dims, size_t dimLength) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    int32_t *destPtr = reinterpret_cast<int32_t *>(drv->mFieldAddress[slot]);
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
        char *cVal = reinterpret_cast<char *>(data);
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

void rsdScriptSetGlobalBind(const Context *dc, const Script *script, uint32_t slot, Allocation *data) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    //rsAssert(!script->mFieldIsObject[slot]);
    //ALOGE("setGlobalBind %p %p %i %p", dc, script, slot, data);

    int32_t *destPtr = reinterpret_cast<int32_t *>(drv->mFieldAddress[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    void *ptr = NULL;
    drv->mBoundAllocs[slot] = data;
    if(data) {
        DrvAllocation *allocDrv = (DrvAllocation *)data->mHal.drv;
        ptr = allocDrv->lod[0].mallocPtr;
    }
    memcpy(destPtr, &ptr, sizeof(void *));
}

void rsdScriptSetGlobalObj(const Context *dc, const Script *script, uint32_t slot, ObjectBase *data) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;
    //rsAssert(script->mFieldIsObject[slot]);
    //ALOGE("setGlobalObj %p %p %i %p", dc, script, slot, data);

    int32_t *destPtr = reinterpret_cast<int32_t *>(drv->mFieldAddress[slot]);
    if (!destPtr) {
        //ALOGV("Calling setVar on slot = %i which is null", slot);
        return;
    }

    rsrSetObject(dc, script, (ObjectBase **)destPtr, data);
}

void rsdScriptDestroy(const Context *dc, Script *script) {
    DrvScript *drv = (DrvScript *)script->mHal.drv;

    if (drv == NULL) {
        return;
    }

    for (size_t i = 0; i < script->mHal.info.exportedVariableCount; ++i) {
        if (drv->mFieldIsObject[i]) {
            if (drv->mFieldAddress[i] != NULL) {
                ObjectBase **obj_addr =
                    reinterpret_cast<ObjectBase **>(drv->mFieldAddress[i]);
                rsrClearObject(dc, script, obj_addr);
            }
        }
    }

    delete[] drv->mInvokeFunctions;
    delete[] drv->mForEachFunctions;
    delete[] drv->mFieldAddress;
    delete[] drv->mFieldIsObject;
    delete[] drv->mForEachSignatures;
    delete[] drv->mBoundAllocs;
    free(drv);
    script->mHal.drv = NULL;
}

Allocation * rsdScriptGetAllocationForPointer(const android::renderscript::Context *dc,
                                              const android::renderscript::Script *sc,
                                              const void *ptr) {
    DrvScript *drv = (DrvScript *)sc->mHal.drv;
    if (!ptr) {
        return NULL;
    }

    for (uint32_t ct=0; ct < sc->mHal.info.exportedVariableCount; ct++) {
        Allocation *a = drv->mBoundAllocs[ct];
        if (!a) continue;
        DrvAllocation *adrv = (DrvAllocation *)a->mHal.drv;
        if (adrv->lod[0].mallocPtr == ptr) {
            return a;
        }
    }
    ALOGE("rsGetAllocation, failed to find %p", ptr);
    return NULL;
}

