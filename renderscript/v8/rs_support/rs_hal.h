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

#ifndef RS_HAL_H
#define RS_HAL_H

#include <rsDefines.h>

struct ANativeWindow;

namespace android {
namespace renderscript {

class Context;
class ObjectBase;
class Element;
class Type;
class Allocation;
class Script;
class ScriptKernelID;
class ScriptFieldID;
class ScriptMethodID;
class ScriptC;
class ScriptGroup;
class Path;
class Program;
class ProgramStore;
class ProgramRaster;
class ProgramVertex;
class ProgramFragment;
class Mesh;
class Sampler;
class FBOCache;

typedef void *(*RsHalSymbolLookupFunc)(void *usrptr, char const *symbolName);

typedef struct {
    const void *in;
    void *out;
    const void *usr;
    size_t usrLen;
    uint32_t x;
    uint32_t y;
    uint32_t z;
    uint32_t lod;
    RsAllocationCubemapFace face;
    uint32_t ar[16];

    uint32_t dimX;
    uint32_t dimY;
    uint32_t dimZ;
    uint32_t dimArray;

    const uint8_t *ptrIn;
    uint8_t *ptrOut;
    uint32_t eStrideIn;
    uint32_t eStrideOut;
    uint32_t yStrideIn;
    uint32_t yStrideOut;
    uint32_t slot;
} RsForEachStubParamStruct;

/**
 * Script management functions
 */
typedef struct {
    bool (*initGraphics)(const Context *);
    void (*shutdownGraphics)(const Context *);
    bool (*setSurface)(const Context *, uint32_t w, uint32_t h, RsNativeWindow);
    void (*swap)(const Context *);

    void (*shutdownDriver)(Context *);
    void (*getVersion)(unsigned int *major, unsigned int *minor);
    void (*setPriority)(const Context *, int32_t priority);



    struct {
        bool (*init)(const Context *rsc, ScriptC *s,
                     char const *resName,
                     char const *cacheDir,
                     uint8_t const *bitcode,
                     size_t bitcodeSize,
                     uint32_t flags);
        bool (*initIntrinsic)(const Context *rsc, Script *s,
                              RsScriptIntrinsicID iid,
                              Element *e);

        void (*invokeFunction)(const Context *rsc, Script *s,
                               uint32_t slot,
                               const void *params,
                               size_t paramLength);
        int (*invokeRoot)(const Context *rsc, Script *s);
        void (*invokeForEach)(const Context *rsc,
                              Script *s,
                              uint32_t slot,
                              const Allocation * ain,
                              Allocation * aout,
                              const void * usr,
                              uint32_t usrLen,
                              const RsScriptCall *sc);
        void (*invokeInit)(const Context *rsc, Script *s);
        void (*invokeFreeChildren)(const Context *rsc, Script *s);

        void (*setGlobalVar)(const Context *rsc, const Script *s,
                             uint32_t slot,
                             void *data,
                             size_t dataLength);
        void (*setGlobalVarWithElemDims)(const Context *rsc, const Script *s,
                                         uint32_t slot,
                                         void *data,
                                         size_t dataLength,
                                         const Element *e,
                                         const size_t *dims,
                                         size_t dimLength);
        void (*setGlobalBind)(const Context *rsc, const Script *s,
                              uint32_t slot,
                              Allocation *data);
        void (*setGlobalObj)(const Context *rsc, const Script *s,
                             uint32_t slot,
                             ObjectBase *data);

        void (*destroy)(const Context *rsc, Script *s);
    } script;

    struct {
        bool (*init)(const Context *rsc, Allocation *alloc, bool forceZero);
        void (*destroy)(const Context *rsc, Allocation *alloc);

        void (*resize)(const Context *rsc, const Allocation *alloc, const Type *newType,
                       bool zeroNew);
        void (*syncAll)(const Context *rsc, const Allocation *alloc, RsAllocationUsageType src);
        void (*markDirty)(const Context *rsc, const Allocation *alloc);

        int32_t (*initSurfaceTexture)(const Context *rsc, const Allocation *alloc);
        void (*setSurfaceTexture)(const Context *rsc, Allocation *alloc, ANativeWindow *sur);
        void (*ioSend)(const Context *rsc, Allocation *alloc);
        void (*ioReceive)(const Context *rsc, Allocation *alloc);

        void (*data1D)(const Context *rsc, const Allocation *alloc,
                       uint32_t xoff, uint32_t lod, uint32_t count,
                       const void *data, size_t sizeBytes);
        void (*data2D)(const Context *rsc, const Allocation *alloc,
                       uint32_t xoff, uint32_t yoff, uint32_t lod,
                       RsAllocationCubemapFace face, uint32_t w, uint32_t h,
                       const void *data, size_t sizeBytes);
        void (*data3D)(const Context *rsc, const Allocation *alloc,
                       uint32_t xoff, uint32_t yoff, uint32_t zoff,
                       uint32_t lod, RsAllocationCubemapFace face,
                       uint32_t w, uint32_t h, uint32_t d, const void *data, size_t sizeBytes);

        void (*read1D)(const Context *rsc, const Allocation *alloc,
                       uint32_t xoff, uint32_t lod, uint32_t count,
                       void *data, size_t sizeBytes);
        void (*read2D)(const Context *rsc, const Allocation *alloc,
                       uint32_t xoff, uint32_t yoff, uint32_t lod,
                       RsAllocationCubemapFace face, uint32_t w, uint32_t h,
                       void *data, size_t sizeBytes);
        void (*read3D)(const Context *rsc, const Allocation *alloc,
                       uint32_t xoff, uint32_t yoff, uint32_t zoff,
                       uint32_t lod, RsAllocationCubemapFace face,
                       uint32_t w, uint32_t h, uint32_t d, void *data, size_t sizeBytes);

        // Lock and unlock make a 1D region of memory available to the CPU
        // for direct access by pointer.  Once unlock is called control is
        // returned to the SOC driver.
        void * (*lock1D)(const Context *rsc, const Allocation *alloc);
        void (*unlock1D)(const Context *rsc, const Allocation *alloc);

        // Allocation to allocation copies
        void (*allocData1D)(const Context *rsc,
                            const Allocation *dstAlloc,
                            uint32_t dstXoff, uint32_t dstLod, uint32_t count,
                            const Allocation *srcAlloc, uint32_t srcXoff, uint32_t srcLod);
        void (*allocData2D)(const Context *rsc,
                            const Allocation *dstAlloc,
                            uint32_t dstXoff, uint32_t dstYoff, uint32_t dstLod,
                            RsAllocationCubemapFace dstFace, uint32_t w, uint32_t h,
                            const Allocation *srcAlloc,
                            uint32_t srcXoff, uint32_t srcYoff, uint32_t srcLod,
                            RsAllocationCubemapFace srcFace);
        void (*allocData3D)(const Context *rsc,
                            const Allocation *dstAlloc,
                            uint32_t dstXoff, uint32_t dstYoff, uint32_t dstZoff,
                            uint32_t dstLod, RsAllocationCubemapFace dstFace,
                            uint32_t w, uint32_t h, uint32_t d,
                            const Allocation *srcAlloc,
                            uint32_t srcXoff, uint32_t srcYoff, uint32_t srcZoff,
                            uint32_t srcLod, RsAllocationCubemapFace srcFace);

        void (*elementData1D)(const Context *rsc, const Allocation *alloc, uint32_t x,
                              const void *data, uint32_t elementOff, size_t sizeBytes);
        void (*elementData2D)(const Context *rsc, const Allocation *alloc, uint32_t x, uint32_t y,
                              const void *data, uint32_t elementOff, size_t sizeBytes);

        void (*generateMipmaps)(const Context *rsc, const Allocation *alloc);
    } allocation;

    struct {
        bool (*init)(const Context *rsc, const ProgramStore *ps);
        void (*setActive)(const Context *rsc, const ProgramStore *ps);
        void (*destroy)(const Context *rsc, const ProgramStore *ps);
    } store;

    struct {
        bool (*init)(const Context *rsc, const ProgramRaster *ps);
        void (*setActive)(const Context *rsc, const ProgramRaster *ps);
        void (*destroy)(const Context *rsc, const ProgramRaster *ps);
    } raster;

    struct {
        bool (*init)(const Context *rsc, const ProgramVertex *pv,
                     const char* shader, size_t shaderLen,
                     const char** textureNames, size_t textureNamesCount,
                     const size_t *textureNamesLength);
        void (*setActive)(const Context *rsc, const ProgramVertex *pv);
        void (*destroy)(const Context *rsc, const ProgramVertex *pv);
    } vertex;

    struct {
        bool (*init)(const Context *rsc, const ProgramFragment *pf,
                     const char* shader, size_t shaderLen,
                     const char** textureNames, size_t textureNamesCount,
                     const size_t *textureNamesLength);
        void (*setActive)(const Context *rsc, const ProgramFragment *pf);
        void (*destroy)(const Context *rsc, const ProgramFragment *pf);
    } fragment;

    struct {
        bool (*init)(const Context *rsc, const Mesh *m);
        void (*draw)(const Context *rsc, const Mesh *m, uint32_t primIndex, uint32_t start, uint32_t len);
        void (*destroy)(const Context *rsc, const Mesh *m);
    } mesh;

    struct {
        bool (*initStatic)(const Context *rsc, const Path *m, const Allocation *vtx, const Allocation *loops);
        bool (*initDynamic)(const Context *rsc, const Path *m);
        void (*draw)(const Context *rsc, const Path *m);
        void (*destroy)(const Context *rsc, const Path *m);
    } path;

    struct {
        bool (*init)(const Context *rsc, const Sampler *m);
        void (*destroy)(const Context *rsc, const Sampler *m);
    } sampler;

    struct {
        bool (*init)(const Context *rsc, const FBOCache *fb);
        void (*setActive)(const Context *rsc, const FBOCache *fb);
        void (*destroy)(const Context *rsc, const FBOCache *fb);
    } framebuffer;

    struct {
        bool (*init)(const Context *rsc, const ScriptGroup *sg);
        void (*setInput)(const Context *rsc, const ScriptGroup *sg,
                         const ScriptKernelID *kid, Allocation *);
        void (*setOutput)(const Context *rsc, const ScriptGroup *sg,
                          const ScriptKernelID *kid, Allocation *);
        void (*execute)(const Context *rsc, const ScriptGroup *sg);
        void (*destroy)(const Context *rsc, const ScriptGroup *sg);
    } scriptgroup;

} RsdHalFunctions;


}
}

#ifdef __cplusplus
extern "C" {
#endif

bool rsdHalInit(RsContext, uint32_t version_major, uint32_t version_minor);

#ifdef __cplusplus
}
#endif

#endif

