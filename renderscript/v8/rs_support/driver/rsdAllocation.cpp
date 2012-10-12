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
//#include "rsdFrameBufferObj.h"

#include "rsAllocation.h"

#include "system/window.h"
/*#include "hardware/gralloc.h"
#include "ui/Rect.h"
#include "ui/GraphicBufferMapper.h"
#include "gui/SurfaceTexture.h"*/

//#include <GLES/gl.h>
//#include <GLES2/gl2.h>
//#include <GLES/glext.h>

using namespace android;
using namespace android::renderscript;


uint8_t *GetOffsetPtr(const android::renderscript::Allocation *alloc,
                      uint32_t xoff, uint32_t yoff, uint32_t lod,
                      RsAllocationCubemapFace face) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    uint8_t *ptr = (uint8_t *)drv->lod[lod].mallocPtr;
    ptr += face * drv->faceOffset;
    ptr += yoff * drv->lod[lod].stride;
    ptr += xoff * alloc->mHal.state.elementSizeBytes;
    return ptr;
}


static void Update2DTexture(const Context *rsc, const Allocation *alloc, const void *ptr,
                            uint32_t xoff, uint32_t yoff, uint32_t lod,
                            RsAllocationCubemapFace face, uint32_t w, uint32_t h) {
}


static void UploadToTexture(const Context *rsc, const Allocation *alloc) {
}

static void AllocateRenderTarget(const Context *rsc, const Allocation *alloc) {
}

static void UploadToBufferObject(const Context *rsc, const Allocation *alloc) {
}

static size_t AllocationBuildPointerTable(const Context *rsc, const Allocation *alloc,
        const Type *type, uint8_t *ptr) {

    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    drv->lod[0].dimX = type->getDimX();
    drv->lod[0].dimY = type->getDimY();
    drv->lod[0].mallocPtr = 0;
    drv->lod[0].stride = drv->lod[0].dimX * type->getElementSizeBytes();
    drv->lodCount = type->getLODCount();
    drv->faceCount = type->getDimFaces();

    size_t offsets[Allocation::MAX_LOD];
    memset(offsets, 0, sizeof(offsets));

    size_t o = drv->lod[0].stride * rsMax(drv->lod[0].dimY, 1u) * rsMax(drv->lod[0].dimZ, 1u);
    if(drv->lodCount > 1) {
        uint32_t tx = drv->lod[0].dimX;
        uint32_t ty = drv->lod[0].dimY;
        uint32_t tz = drv->lod[0].dimZ;
        for (uint32_t lod=1; lod < drv->lodCount; lod++) {
            drv->lod[lod].dimX = tx;
            drv->lod[lod].dimY = ty;
            drv->lod[lod].dimZ = tz;
            drv->lod[lod].stride = tx * type->getElementSizeBytes();
            offsets[lod] = o;
            o += drv->lod[lod].stride * rsMax(ty, 1u) * rsMax(tz, 1u);
            if (tx > 1) tx >>= 1;
            if (ty > 1) ty >>= 1;
            if (tz > 1) tz >>= 1;
        }
    }
    drv->faceOffset = o;

    drv->lod[0].mallocPtr = ptr;
    for (uint32_t lod=1; lod < drv->lodCount; lod++) {
        drv->lod[lod].mallocPtr = ptr + offsets[lod];
    }
    alloc->mHal.drvState.strideLOD0 = drv->lod[0].stride;
    alloc->mHal.drvState.mallocPtrLOD0 = ptr;

    size_t allocSize = drv->faceOffset;
    if(drv->faceCount) {
        allocSize *= 6;
    }

    return allocSize;
}

bool rsdAllocationInit(const Context *rsc, Allocation *alloc, bool forceZero) {
    DrvAllocation *drv = (DrvAllocation *)calloc(1, sizeof(DrvAllocation));
    if (!drv) {
        return false;
    }
    alloc->mHal.drv = drv;

    // Calculate the object size.
    size_t allocSize = AllocationBuildPointerTable(rsc, alloc, alloc->getType(), NULL);

    ALOGE("alloc usage %i", alloc->mHal.state.usageFlags);

    uint8_t * ptr = NULL;
    if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_IO_OUTPUT) {
    } else {

        ptr = (uint8_t *)malloc(allocSize);
        if (!ptr) {
            free(drv);
            return false;
        }
    }
    // Build the pointer tables
    size_t verifySize = AllocationBuildPointerTable(rsc, alloc, alloc->getType(), ptr);
    if(allocSize != verifySize) {
        rsAssert(!"Size mismatch");
    }

    drv->glTarget = GL_NONE;
    if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_GRAPHICS_TEXTURE) {
        if (alloc->mHal.state.hasFaces) {
            drv->glTarget = GL_TEXTURE_CUBE_MAP;
        } else {
            drv->glTarget = GL_TEXTURE_2D;
        }
    } else {
        if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_GRAPHICS_VERTEX) {
            drv->glTarget = GL_ARRAY_BUFFER;
        }
    }

    drv->glType = 0;
    drv->glFormat = 0;

    if (forceZero && ptr) {
        memset(ptr, 0, alloc->mHal.state.type->getSizeBytes());
    }

    if (alloc->mHal.state.usageFlags & ~RS_ALLOCATION_USAGE_SCRIPT) {
        drv->uploadDeferred = true;
    }


    drv->readBackFBO = NULL;

    return true;
}

void rsdAllocationDestroy(const Context *rsc, Allocation *alloc) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    if (alloc->mHal.drvState.mallocPtrLOD0) {
        free(alloc->mHal.drvState.mallocPtrLOD0);
        alloc->mHal.drvState.mallocPtrLOD0 = NULL;
    }
    free(drv);
    alloc->mHal.drv = NULL;
}

void rsdAllocationResize(const Context *rsc, const Allocation *alloc,
                         const Type *newType, bool zeroNew) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    void * oldPtr = drv->lod[0].mallocPtr;
    // Calculate the object size
    size_t s = AllocationBuildPointerTable(rsc, alloc, newType, NULL);
    uint8_t *ptr = (uint8_t *)realloc(oldPtr, s);
    // Build the relative pointer tables.
    size_t verifySize = AllocationBuildPointerTable(rsc, alloc, newType, ptr);
    if(s != verifySize) {
        rsAssert(!"Size mismatch");
    }

    const uint32_t oldDimX = alloc->mHal.state.dimensionX;
    const uint32_t dimX = newType->getDimX();

    if (dimX > oldDimX) {
        uint32_t stride = alloc->mHal.state.elementSizeBytes;
        memset(((uint8_t *)alloc->mHal.drvState.mallocPtrLOD0) + stride * oldDimX,
                 0, stride * (dimX - oldDimX));
    }
}

static void rsdAllocationSyncFromFBO(const Context *rsc, const Allocation *alloc) {
}


void rsdAllocationSyncAll(const Context *rsc, const Allocation *alloc,
                         RsAllocationUsageType src) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    if (src == RS_ALLOCATION_USAGE_GRAPHICS_RENDER_TARGET) {
        if(!alloc->getIsRenderTarget()) {
            rsc->setError(RS_ERROR_FATAL_DRIVER,
                          "Attempting to sync allocation from render target, "
                          "for non-render target allocation");
        } else if (alloc->getType()->getElement()->getKind() != RS_KIND_PIXEL_RGBA) {
            rsc->setError(RS_ERROR_FATAL_DRIVER, "Cannot only sync from RGBA"
                                                 "render target");
        } else {
            rsdAllocationSyncFromFBO(rsc, alloc);
        }
        return;
    }

    rsAssert(src == RS_ALLOCATION_USAGE_SCRIPT);

    if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_GRAPHICS_TEXTURE) {
        UploadToTexture(rsc, alloc);
    } else {
        if ((alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_GRAPHICS_RENDER_TARGET) &&
            !(alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_IO_OUTPUT)) {
            AllocateRenderTarget(rsc, alloc);
        }
    }
    if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_GRAPHICS_VERTEX) {
        UploadToBufferObject(rsc, alloc);
    }

    drv->uploadDeferred = false;
}

void rsdAllocationMarkDirty(const Context *rsc, const Allocation *alloc) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    drv->uploadDeferred = true;
}

int32_t rsdAllocationInitSurfaceTexture(const Context *rsc, const Allocation *alloc) {
  return 0;
}

void rsdAllocationSetSurfaceTexture(const Context *rsc, Allocation *alloc, ANativeWindow *nw) {
}

void rsdAllocationIoSend(const Context *rsc, Allocation *alloc) {
}

void rsdAllocationIoReceive(const Context *rsc, Allocation *alloc) {
}


void rsdAllocationData1D(const Context *rsc, const Allocation *alloc,
                         uint32_t xoff, uint32_t lod, uint32_t count,
                         const void *data, size_t sizeBytes) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    const uint32_t eSize = alloc->mHal.state.type->getElementSizeBytes();
    uint8_t * ptr = GetOffsetPtr(alloc, xoff, 0, 0, RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X);
    uint32_t size = count * eSize;

    if (alloc->mHal.state.hasReferences) {
        alloc->incRefs(data, count);
        alloc->decRefs(ptr, count);
    }

    memcpy(ptr, data, size);
    drv->uploadDeferred = true;
}

void rsdAllocationData2D(const Context *rsc, const Allocation *alloc,
                         uint32_t xoff, uint32_t yoff, uint32_t lod, RsAllocationCubemapFace face,
                         uint32_t w, uint32_t h, const void *data, size_t sizeBytes) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    uint32_t lineSize = eSize * w;

    if (drv->lod[0].mallocPtr) {
        const uint8_t *src = static_cast<const uint8_t *>(data);
        uint8_t *dst = GetOffsetPtr(alloc, xoff, yoff, lod, face);

        for (uint32_t line=yoff; line < (yoff+h); line++) {
            if (alloc->mHal.state.hasReferences) {
                alloc->incRefs(src, w);
                alloc->decRefs(dst, w);
            }
            memcpy(dst, src, lineSize);
            src += lineSize;
            dst += drv->lod[lod].stride;
        }
        drv->uploadDeferred = true;
    } else {
        Update2DTexture(rsc, alloc, data, xoff, yoff, lod, face, w, h);
    }
}

void rsdAllocationData3D(const Context *rsc, const Allocation *alloc,
                         uint32_t xoff, uint32_t yoff, uint32_t zoff,
                         uint32_t lod, RsAllocationCubemapFace face,
                         uint32_t w, uint32_t h, uint32_t d, const void *data, uint32_t sizeBytes) {

}

void rsdAllocationRead1D(const Context *rsc, const Allocation *alloc,
                         uint32_t xoff, uint32_t lod, uint32_t count,
                         void *data, size_t sizeBytes) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    const uint32_t eSize = alloc->mHal.state.type->getElementSizeBytes();
    const uint8_t * ptr = GetOffsetPtr(alloc, xoff, 0, 0, RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X);
    memcpy(data, ptr, count * eSize);
}

void rsdAllocationRead2D(const Context *rsc, const Allocation *alloc,
                         uint32_t xoff, uint32_t yoff, uint32_t lod, RsAllocationCubemapFace face,
                         uint32_t w, uint32_t h, void *data, size_t sizeBytes) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    uint32_t lineSize = eSize * w;

    if (drv->lod[0].mallocPtr) {
        uint8_t *dst = static_cast<uint8_t *>(data);
        const uint8_t *src = GetOffsetPtr(alloc, xoff, yoff, lod, face);

        for (uint32_t line=yoff; line < (yoff+h); line++) {
            memcpy(dst, src, lineSize);
            dst += lineSize;
            src += drv->lod[lod].stride;
        }
    } else {
        ALOGE("Add code to readback from non-script memory");
    }
}

void rsdAllocationRead3D(const Context *rsc, const Allocation *alloc,
                         uint32_t xoff, uint32_t yoff, uint32_t zoff,
                         uint32_t lod, RsAllocationCubemapFace face,
                         uint32_t w, uint32_t h, uint32_t d, void *data, uint32_t sizeBytes) {

}

void * rsdAllocationLock1D(const android::renderscript::Context *rsc,
                          const android::renderscript::Allocation *alloc) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    return drv->lod[0].mallocPtr;
}

void rsdAllocationUnlock1D(const android::renderscript::Context *rsc,
                          const android::renderscript::Allocation *alloc) {

}

void rsdAllocationData1D_alloc(const android::renderscript::Context *rsc,
                               const android::renderscript::Allocation *dstAlloc,
                               uint32_t dstXoff, uint32_t dstLod, uint32_t count,
                               const android::renderscript::Allocation *srcAlloc,
                               uint32_t srcXoff, uint32_t srcLod) {
}


void rsdAllocationData2D_alloc_script(const android::renderscript::Context *rsc,
                                      const android::renderscript::Allocation *dstAlloc,
                                      uint32_t dstXoff, uint32_t dstYoff, uint32_t dstLod,
                                      RsAllocationCubemapFace dstFace, uint32_t w, uint32_t h,
                                      const android::renderscript::Allocation *srcAlloc,
                                      uint32_t srcXoff, uint32_t srcYoff, uint32_t srcLod,
                                      RsAllocationCubemapFace srcFace) {
    uint32_t elementSize = dstAlloc->getType()->getElementSizeBytes();
    for (uint32_t i = 0; i < h; i ++) {
        uint8_t *dstPtr = GetOffsetPtr(dstAlloc, dstXoff, dstYoff + i, dstLod, dstFace);
        uint8_t *srcPtr = GetOffsetPtr(srcAlloc, srcXoff, srcYoff + i, srcLod, srcFace);
        memcpy(dstPtr, srcPtr, w * elementSize);

        //ALOGE("COPIED dstXoff(%u), dstYoff(%u), dstLod(%u), dstFace(%u), w(%u), h(%u), srcXoff(%u), srcYoff(%u), srcLod(%u), srcFace(%u)",
        //     dstXoff, dstYoff, dstLod, dstFace, w, h, srcXoff, srcYoff, srcLod, srcFace);
    }
}

void rsdAllocationData2D_alloc(const android::renderscript::Context *rsc,
                               const android::renderscript::Allocation *dstAlloc,
                               uint32_t dstXoff, uint32_t dstYoff, uint32_t dstLod,
                               RsAllocationCubemapFace dstFace, uint32_t w, uint32_t h,
                               const android::renderscript::Allocation *srcAlloc,
                               uint32_t srcXoff, uint32_t srcYoff, uint32_t srcLod,
                               RsAllocationCubemapFace srcFace) {
    if (!dstAlloc->getIsScript() && !srcAlloc->getIsScript()) {
        rsc->setError(RS_ERROR_FATAL_DRIVER, "Non-script allocation copies not "
                                             "yet implemented.");
        return;
    }
    rsdAllocationData2D_alloc_script(rsc, dstAlloc, dstXoff, dstYoff,
                                     dstLod, dstFace, w, h, srcAlloc,
                                     srcXoff, srcYoff, srcLod, srcFace);
}

void rsdAllocationData3D_alloc(const android::renderscript::Context *rsc,
                               const android::renderscript::Allocation *dstAlloc,
                               uint32_t dstXoff, uint32_t dstYoff, uint32_t dstZoff,
                               uint32_t dstLod, RsAllocationCubemapFace dstFace,
                               uint32_t w, uint32_t h, uint32_t d,
                               const android::renderscript::Allocation *srcAlloc,
                               uint32_t srcXoff, uint32_t srcYoff, uint32_t srcZoff,
                               uint32_t srcLod, RsAllocationCubemapFace srcFace) {
}

void rsdAllocationElementData1D(const Context *rsc, const Allocation *alloc,
                                uint32_t x,
                                const void *data, uint32_t cIdx, uint32_t sizeBytes) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    uint8_t * ptr = GetOffsetPtr(alloc, x, 0, 0, RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X);

    const Element * e = alloc->mHal.state.type->getElement()->getField(cIdx);
    ptr += alloc->mHal.state.type->getElement()->getFieldOffsetBytes(cIdx);

    if (alloc->mHal.state.hasReferences) {
        e->incRefs(data);
        e->decRefs(ptr);
    }

    memcpy(ptr, data, sizeBytes);
    drv->uploadDeferred = true;
}

void rsdAllocationElementData2D(const Context *rsc, const Allocation *alloc,
                                uint32_t x, uint32_t y,
                                const void *data, uint32_t cIdx, uint32_t sizeBytes) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    uint8_t * ptr = GetOffsetPtr(alloc, x, y, 0, RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X);

    const Element * e = alloc->mHal.state.type->getElement()->getField(cIdx);
    ptr += alloc->mHal.state.type->getElement()->getFieldOffsetBytes(cIdx);

    if (alloc->mHal.state.hasReferences) {
        e->incRefs(data);
        e->decRefs(ptr);
    }

    memcpy(ptr, data, sizeBytes);
    drv->uploadDeferred = true;
}

static void mip565(const Allocation *alloc, int lod, RsAllocationCubemapFace face) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    uint32_t w = drv->lod[lod + 1].dimX;
    uint32_t h = drv->lod[lod + 1].dimY;

    for (uint32_t y=0; y < h; y++) {
        uint16_t *oPtr = (uint16_t *)GetOffsetPtr(alloc, 0, y, lod + 1, face);
        const uint16_t *i1 = (uint16_t *)GetOffsetPtr(alloc, 0, y*2, lod, face);
        const uint16_t *i2 = (uint16_t *)GetOffsetPtr(alloc, 0, y*2+1, lod, face);

        for (uint32_t x=0; x < w; x++) {
            *oPtr = rsBoxFilter565(i1[0], i1[1], i2[0], i2[1]);
            oPtr ++;
            i1 += 2;
            i2 += 2;
        }
    }
}

static void mip8888(const Allocation *alloc, int lod, RsAllocationCubemapFace face) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    uint32_t w = drv->lod[lod + 1].dimX;
    uint32_t h = drv->lod[lod + 1].dimY;

    for (uint32_t y=0; y < h; y++) {
        uint32_t *oPtr = (uint32_t *)GetOffsetPtr(alloc, 0, y, lod + 1, face);
        const uint32_t *i1 = (uint32_t *)GetOffsetPtr(alloc, 0, y*2, lod, face);
        const uint32_t *i2 = (uint32_t *)GetOffsetPtr(alloc, 0, y*2+1, lod, face);

        for (uint32_t x=0; x < w; x++) {
            *oPtr = rsBoxFilter8888(i1[0], i1[1], i2[0], i2[1]);
            oPtr ++;
            i1 += 2;
            i2 += 2;
        }
    }
}

static void mip8(const Allocation *alloc, int lod, RsAllocationCubemapFace face) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    uint32_t w = drv->lod[lod + 1].dimX;
    uint32_t h = drv->lod[lod + 1].dimY;

    for (uint32_t y=0; y < h; y++) {
        uint8_t *oPtr = GetOffsetPtr(alloc, 0, y, lod + 1, face);
        const uint8_t *i1 = GetOffsetPtr(alloc, 0, y*2, lod, face);
        const uint8_t *i2 = GetOffsetPtr(alloc, 0, y*2+1, lod, face);

        for (uint32_t x=0; x < w; x++) {
            *oPtr = (uint8_t)(((uint32_t)i1[0] + i1[1] + i2[0] + i2[1]) * 0.25f);
            oPtr ++;
            i1 += 2;
            i2 += 2;
        }
    }
}

void rsdAllocationGenerateMipmaps(const Context *rsc, const Allocation *alloc) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    if(!drv->lod[0].mallocPtr) {
        return;
    }
    uint32_t numFaces = alloc->getType()->getDimFaces() ? 6 : 1;
    for (uint32_t face = 0; face < numFaces; face ++) {
        for (uint32_t lod=0; lod < (alloc->getType()->getLODCount() -1); lod++) {
            switch (alloc->getType()->getElement()->getSizeBits()) {
            case 32:
                mip8888(alloc, lod, (RsAllocationCubemapFace)face);
                break;
            case 16:
                mip565(alloc, lod, (RsAllocationCubemapFace)face);
                break;
            case 8:
                mip8(alloc, lod, (RsAllocationCubemapFace)face);
                break;
            }
        }
    }
}


