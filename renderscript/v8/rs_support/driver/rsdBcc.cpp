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
#include "rsdIntrinsics.h"

#include "rsContext.h"
#include "rsElement.h"
#include "rsScriptC.h"

#include "utils/Vector.h"
#include "utils/Timers.h"
#include "utils/StopWatch.h"
#include "utils/String8.h"

#include <dlfcn.h>
#include <stdio.h>
#include <string.h>

using namespace android;
using namespace android::renderscript;

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

    String8 scriptInfoName(cacheDir);
    scriptInfoName = scriptInfoName.getPathDir();
    scriptInfoName.appendPath("lib/");
    scriptInfoName.append(resName);
    scriptInfoName.append(".bcinfo");

    void *scriptSO = NULL;
    FILE *fp = NULL;
    DrvScript *drv = (DrvScript *)calloc(1, sizeof(DrvScript));
    if (drv == NULL) {
        goto error;
    }
    script->mHal.drv = drv;

    ALOGE("Opening up info object: %s", scriptInfoName.string());

    fp = fopen(scriptInfoName.string(), "r");
    if (!fp) {
        ALOGE("Unable to open info file: %s", scriptInfoName.string());
        goto error;
    }

    ALOGE("Opening up shared object: %s", scriptSOName.string());
    scriptSO = dlopen(scriptSOName.string(), RTLD_NOW | RTLD_LOCAL);
    if (scriptSO == NULL) {
        ALOGE("Unable to open shared library (%s): %s",
              scriptSOName.string(), dlerror());
        goto error;
    }
    drv->mScriptSO = scriptSO;

    if (scriptSO) {
        char line[MAXLINE];
        drv->mScriptSO = scriptSO;
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

        size_t varCount = 0;
        if (fgets(line, MAXLINE, fp) == NULL) {
            goto error;
        }
        if (sscanf(line, EXPORT_VAR_STR "%zu", &varCount) != 1) {
            ALOGE("Invalid export var count!: %s", line);
            goto error;
        }

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
            for (size_t i = 0; i < varCount; ++i) {
                if (fgets(line, MAXLINE, fp) == NULL) {
                    goto error;
                }
                char *c = strrchr(line, '\n');
                if (c) {
                    *c = '\0';
                }
                drv->mFieldAddress[i] = dlsym(scriptSO, line);
                if (drv->mFieldAddress[i] == NULL) {
                    ALOGE("Failed to find variable address for %s: %s",
                          line, dlerror());
                    // Not a critical error if we don't find a global variable.
                }
                else {
                    ALOGE("Found variable %s at %p", line,
                          drv->mFieldAddress[i]);
                }
            }
        }

        size_t funcCount = 0;
        if (fgets(line, MAXLINE, fp) == NULL) {
            goto error;
        }
        if (sscanf(line, EXPORT_FUNC_STR "%zu", &funcCount) != 1) {
            ALOGE("Invalid export func count!: %s", line);
            goto error;
        }

        script->mHal.info.exportedFunctionCount = funcCount;
        ALOGE("funcCount: %zu", funcCount);

        if (funcCount > 0) {
            drv->mInvokeFunctions = new InvokeFunc_t[funcCount];
            if (drv->mInvokeFunctions == NULL) {
                goto error;
            }
            for (size_t i = 0; i < funcCount; ++i) {
                if (fgets(line, MAXLINE, fp) == NULL) {
                    goto error;
                }
                char *c = strrchr(line, '\n');
                if (c) {
                    *c = '\0';
                }

                drv->mInvokeFunctions[i] =
                        (InvokeFunc_t) dlsym(scriptSO, line);
                if (drv->mInvokeFunctions[i] == NULL) {
                    ALOGE("Failed to get function address for %s(): %s",
                          line, dlerror());
                    goto error;
                }
                else {
                    ALOGE("Found InvokeFunc_t %s at %p", line,
                          drv->mInvokeFunctions[i]);
                }
            }
        }

        size_t forEachCount = 0;
        if (fgets(line, MAXLINE, fp) == NULL) {
            goto error;
        }
        if (sscanf(line, EXPORT_FOREACH_STR "%zu", &forEachCount) != 1) {
            ALOGE("Invalid export forEach count!: %s", line);
            goto error;
        }

        if (forEachCount > 0) {

            drv->mForEachSignatures = new uint32_t[forEachCount];
            if (drv->mForEachSignatures == NULL) {
                goto error;
            }
            drv->mForEachFunctions = new ForEachFunc_t[forEachCount];
            if (drv->mForEachFunctions == NULL) {
                goto error;
            }
            for (size_t i = 0; i < forEachCount; ++i) {
                unsigned int tmpSig = 0;
                char tmpName[MAXLINE];

                if (fgets(line, MAXLINE, fp) == NULL) {
                    goto error;
                }
                if (sscanf(line, "%u - %" MAKE_STR(MAXLINE) "s",
                           &tmpSig, tmpName) != 2) {
                    ALOGE("Invalid export forEach!: %s", line);
                    goto error;
                }

                drv->mForEachSignatures[i] = tmpSig;
                drv->mForEachFunctions[i] =
                        (ForEachFunc_t) dlsym(scriptSO, tmpName);
                if (drv->mForEachFunctions[i] == NULL) {
                    ALOGE("Failed to find forEach function address for %s: %s",
                          tmpName, dlerror());
                    goto error;
                }
                else {
                    // TODO - Maybe add ForEachExpandPass to .so creation and
                    // then lookup the ".expand" version of these kernels
                    // instead.
                    ALOGE("Found forEach %s at %p", tmpName,
                          drv->mForEachFunctions[i]);
                }
            }
        }

        size_t objectSlotCount = 0;
        if (fgets(line, MAXLINE, fp) == NULL) {
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
                if (fgets(line, MAXLINE, fp) == NULL) {
                    goto error;
                }
                if (sscanf(line, "%u", &varNum) != 1) {
                    ALOGE("Invalid object slot!: %s", line);
                    goto error;
                }

                if (varNum < varCount) {
                    drv->mFieldIsObject[varNum] = true;
                }
            }
        }

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

    fclose(fp);
    pthread_mutex_unlock(&rsdgInitMutex);
    return true;

error:

    fclose(fp);
    pthread_mutex_unlock(&rsdgInitMutex);
    if (drv) {
        delete[] drv->mInvokeFunctions;
        delete[] drv->mForEachFunctions;
        delete[] drv->mFieldAddress;
        delete[] drv->mFieldIsObject;
        delete[] drv->mForEachSignatures;
        delete[] drv->mBoundAllocs;
        if (drv->mScriptSO) {
            dlclose(drv->mScriptSO);
        }
        free(drv);
    }
    script->mHal.drv = NULL;
    return false;

}

bool rsdInitIntrinsic(const Context *rsc, Script *s, RsScriptIntrinsicID iid, Element *e) {
    pthread_mutex_lock(&rsdgInitMutex);

    DrvScript *drv = (DrvScript *)calloc(1, sizeof(DrvScript));
    if (drv == NULL) {
        goto error;
    }
    s->mHal.drv = drv;
    drv->mIntrinsicID = iid;
    drv->mIntrinsicData = rsdIntrinsic_Init(rsc, s, iid, &drv->mIntrinsicFuncs);
    s->mHal.info.isThreadable = true;

    pthread_mutex_unlock(&rsdgInitMutex);
    return true;

error:
    pthread_mutex_unlock(&rsdgInitMutex);
    return false;
}

typedef void (*rs_t)(const void *, void *, const void *, uint32_t, uint32_t, uint32_t, uint32_t);

static void wc_xy(void *usr, uint32_t idx) {
    MTLaunchStruct *mtls = (MTLaunchStruct *)usr;
    RsForEachStubParamStruct p;
    memcpy(&p, &mtls->fep, sizeof(p));
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
            p.out = mtls->fep.ptrOut + (mtls->fep.yStrideOut * p.y);
            p.in = mtls->fep.ptrIn + (mtls->fep.yStrideIn * p.y);
            for (uint32_t x = mtls->xStart; x < mtls->xEnd; ++x) {
                bare_fn(p.in, p.out, p.usr, x, p.y, 0, 0);
                p.in = (char *)(p.in) + mtls->fep.eStrideIn;
                p.out = (char *)(p.out) + mtls->fep.eStrideOut;
            }
        }
    }
}

static void wc_x(void *usr, uint32_t idx) {
    MTLaunchStruct *mtls = (MTLaunchStruct *)usr;
    RsForEachStubParamStruct p;
    memcpy(&p, &mtls->fep, sizeof(p));
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

        p.out = mtls->fep.ptrOut + (mtls->fep.eStrideOut * xStart);
        p.in = mtls->fep.ptrIn + (mtls->fep.eStrideIn * xStart);
        for (uint32_t x = mtls->xStart; x < mtls->xEnd; ++x) {
            bare_fn(p.in, p.out, p.usr, x, 0, 0, 0);
            p.in = (char *)(p.in) + mtls->fep.eStrideIn;
            p.out = (char *)(p.out) + mtls->fep.eStrideOut;
        }
    }
}

void rsdScriptInvokeForEachMtlsSetup(const Context *rsc,
                                     const Allocation * ain,
                                     Allocation * aout,
                                     const void * usr,
                                     uint32_t usrLen,
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
        rsc->setError(RS_ERROR_BAD_SCRIPT, "rsForEach called with null allocations");
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

    Context *mrsc = (Context *)rsc;
    mtls->rsc = mrsc;
    mtls->ain = ain;
    mtls->aout = aout;
    mtls->fep.usr = usr;
    mtls->fep.usrLen = usrLen;
    mtls->mSliceSize = 10;
    mtls->mSliceNum = 0;

    mtls->fep.ptrIn = NULL;
    mtls->fep.eStrideIn = 0;

    if (ain) {
        DrvAllocation *aindrv = (DrvAllocation *)ain->mHal.drv;
        mtls->fep.ptrIn = (const uint8_t *)aindrv->lod[0].mallocPtr;
        mtls->fep.eStrideIn = ain->getType()->getElementSizeBytes();
        mtls->fep.yStrideIn = aindrv->lod[0].stride;
    }

    mtls->fep.ptrOut = NULL;
    mtls->fep.eStrideOut = 0;
    if (aout) {
        DrvAllocation *aoutdrv = (DrvAllocation *)aout->mHal.drv;
        mtls->fep.ptrOut = (uint8_t *)aoutdrv->lod[0].mallocPtr;
        mtls->fep.eStrideOut = aout->getType()->getElementSizeBytes();
        mtls->fep.yStrideOut = aoutdrv->lod[0].stride;
    }
}

void rsdScriptLaunchThreads(const Context *rsc,
                            Script *s,
                            uint32_t slot,
                            const Allocation * ain,
                            Allocation * aout,
                            const void * usr,
                            uint32_t usrLen,
                            const RsScriptCall *sc,
                            MTLaunchStruct *mtls) {

    Script * oldTLS = setTLS(s);
    Context *mrsc = (Context *)rsc;
    RsdHal * dc = (RsdHal *)mtls->rsc->mHal.drv;

    if ((dc->mWorkers.mCount > 1) && s->mHal.info.isThreadable && !dc->mInForEach) {
        dc->mInForEach = true;
        if (mtls->fep.dimY > 1) {
            mtls->mSliceSize = mtls->fep.dimY / (dc->mWorkers.mCount * 4);
            if(mtls->mSliceSize < 1) {
                mtls->mSliceSize = 1;
            }

            rsdLaunchThreads(mrsc, wc_xy, mtls);
        } else {
            mtls->mSliceSize = mtls->fep.dimX / (dc->mWorkers.mCount * 4);
            if(mtls->mSliceSize < 1) {
                mtls->mSliceSize = 1;
            }

            rsdLaunchThreads(mrsc, wc_x, mtls);
        }
        dc->mInForEach = false;

        //ALOGE("launch 1");
    } else {
        RsForEachStubParamStruct p;
        memcpy(&p, &mtls->fep, sizeof(p));
        uint32_t sig = mtls->sig;

        //ALOGE("launch 3");
        outer_foreach_t fn = (outer_foreach_t) mtls->kernel;
        for (p.ar[0] = mtls->arrayStart; p.ar[0] < mtls->arrayEnd; p.ar[0]++) {
            for (p.z = mtls->zStart; p.z < mtls->zEnd; p.z++) {
                for (p.y = mtls->yStart; p.y < mtls->yEnd; p.y++) {
                    uint32_t offset = mtls->fep.dimY * mtls->fep.dimZ * p.ar[0] +
                                      mtls->fep.dimY * p.z + p.y;
                    p.out = mtls->fep.ptrOut + (mtls->fep.yStrideOut * offset);
                    p.in = mtls->fep.ptrIn + (mtls->fep.yStrideIn * offset);
                    fn(&p, mtls->xStart, mtls->xEnd, mtls->fep.eStrideIn, mtls->fep.eStrideOut);
                }
            }
        }
    }

    setTLS(oldTLS);
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
    rsdScriptInvokeForEachMtlsSetup(rsc, ain, aout, usr, usrLen, sc, &mtls);
    mtls.script = s;
    mtls.fep.slot = slot;

    DrvScript *drv = (DrvScript *)s->mHal.drv;
    if (drv->mIntrinsicID) {
        mtls.kernel = (void (*)())drv->mIntrinsicFuncs.root;
        mtls.fep.usr = drv->mIntrinsicData;
    } else {
		mtls.kernel = drv->mForEachFunctions[slot];
        rsAssert(mtls.kernel != NULL);
        mtls.sig = drv->mForEachSignatures[slot];
    }


    rsdScriptLaunchThreads(rsc, s, slot, ain, aout, usr, usrLen, sc, &mtls);
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

    if (drv->mIntrinsicID) {
        drv->mIntrinsicFuncs.setVar(dc, script, drv->mIntrinsicData, slot, data, dataLength);
        return;
    }

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

    if (drv->mIntrinsicID) {
        drv->mIntrinsicFuncs.bind(dc, script, drv->mIntrinsicData, slot, data);
        return;
    }

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
    if (drv->mScriptSO) {
        dlclose(drv->mScriptSO);
    }
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

