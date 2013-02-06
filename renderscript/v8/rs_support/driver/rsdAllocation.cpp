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
#include "rsdAllocation.h"

#include "rsAllocation.h"

#include "system/window.h"
#include "ui/Rect.h"
#include "ui/GraphicBufferMapper.h"

#ifndef RS_COMPATIBILITY_LIB
#include "rsdFrameBufferObj.h"
#include "gui/GLConsumer.h"
#include "hardware/gralloc.h"

#include <GLES/gl.h>
#include <GLES2/gl2.h>
#include <GLES/glext.h>
#endif

using namespace android;
using namespace android::renderscript;


#ifndef RS_COMPATIBILITY_LIB
const static GLenum gFaceOrder[] = {
    GL_TEXTURE_CUBE_MAP_POSITIVE_X,
    GL_TEXTURE_CUBE_MAP_NEGATIVE_X,
    GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
    GL_TEXTURE_CUBE_MAP_NEGATIVE_Y,
    GL_TEXTURE_CUBE_MAP_POSITIVE_Z,
    GL_TEXTURE_CUBE_MAP_NEGATIVE_Z
};

GLenum rsdTypeToGLType(RsDataType t) {
    switch (t) {
    case RS_TYPE_UNSIGNED_5_6_5:    return GL_UNSIGNED_SHORT_5_6_5;
    case RS_TYPE_UNSIGNED_5_5_5_1:  return GL_UNSIGNED_SHORT_5_5_5_1;
    case RS_TYPE_UNSIGNED_4_4_4_4:  return GL_UNSIGNED_SHORT_4_4_4_4;

    //case RS_TYPE_FLOAT_16:      return GL_HALF_FLOAT;
    case RS_TYPE_FLOAT_32:      return GL_FLOAT;
    case RS_TYPE_UNSIGNED_8:    return GL_UNSIGNED_BYTE;
    case RS_TYPE_UNSIGNED_16:   return GL_UNSIGNED_SHORT;
    case RS_TYPE_SIGNED_8:      return GL_BYTE;
    case RS_TYPE_SIGNED_16:     return GL_SHORT;
    default:    break;
    }
    return 0;
}

GLenum rsdKindToGLFormat(RsDataKind k) {
    switch (k) {
    case RS_KIND_PIXEL_L: return GL_LUMINANCE;
    case RS_KIND_PIXEL_A: return GL_ALPHA;
    case RS_KIND_PIXEL_LA: return GL_LUMINANCE_ALPHA;
    case RS_KIND_PIXEL_RGB: return GL_RGB;
    case RS_KIND_PIXEL_RGBA: return GL_RGBA;
    case RS_KIND_PIXEL_DEPTH: return GL_DEPTH_COMPONENT16;
    default: break;
    }
    return 0;
}
#endif

uint8_t *GetOffsetPtr(const android::renderscript::Allocation *alloc,
                      uint32_t xoff, uint32_t yoff, uint32_t lod,
                      RsAllocationCubemapFace face) {
    uint8_t *ptr = (uint8_t *)alloc->mHal.drvState.lod[lod].mallocPtr;
    ptr += face * alloc->mHal.drvState.faceOffset;
    ptr += yoff * alloc->mHal.drvState.lod[lod].stride;
    ptr += xoff * alloc->mHal.state.elementSizeBytes;
    return ptr;
}


static void Update2DTexture(const Context *rsc, const Allocation *alloc, const void *ptr,
                            uint32_t xoff, uint32_t yoff, uint32_t lod,
                            RsAllocationCubemapFace face, uint32_t w, uint32_t h) {
#ifndef RS_COMPATIBILITY_LIB
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    rsAssert(drv->textureID);
    RSD_CALL_GL(glBindTexture, drv->glTarget, drv->textureID);
    RSD_CALL_GL(glPixelStorei, GL_UNPACK_ALIGNMENT, 1);
    GLenum t = GL_TEXTURE_2D;
    if (alloc->mHal.state.hasFaces) {
        t = gFaceOrder[face];
    }
    RSD_CALL_GL(glTexSubImage2D, t, lod, xoff, yoff, w, h, drv->glFormat, drv->glType, ptr);
#endif
}


#ifndef RS_COMPATIBILITY_LIB
static void Upload2DTexture(const Context *rsc, const Allocation *alloc, bool isFirstUpload) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    RSD_CALL_GL(glBindTexture, drv->glTarget, drv->textureID);
    RSD_CALL_GL(glPixelStorei, GL_UNPACK_ALIGNMENT, 1);

    uint32_t faceCount = 1;
    if (alloc->mHal.state.hasFaces) {
        faceCount = 6;
    }

    rsdGLCheckError(rsc, "Upload2DTexture 1 ");
    for (uint32_t face = 0; face < faceCount; face ++) {
        for (uint32_t lod = 0; lod < alloc->mHal.state.type->getLODCount(); lod++) {
            const uint8_t *p = GetOffsetPtr(alloc, 0, 0, lod, (RsAllocationCubemapFace)face);

            GLenum t = GL_TEXTURE_2D;
            if (alloc->mHal.state.hasFaces) {
                t = gFaceOrder[face];
            }

            if (isFirstUpload) {
                RSD_CALL_GL(glTexImage2D, t, lod, drv->glFormat,
                             alloc->mHal.state.type->getLODDimX(lod),
                             alloc->mHal.state.type->getLODDimY(lod),
                             0, drv->glFormat, drv->glType, p);
            } else {
                RSD_CALL_GL(glTexSubImage2D, t, lod, 0, 0,
                                alloc->mHal.state.type->getLODDimX(lod),
                                alloc->mHal.state.type->getLODDimY(lod),
                                drv->glFormat, drv->glType, p);
            }
        }
    }

    if (alloc->mHal.state.mipmapControl == RS_ALLOCATION_MIPMAP_ON_SYNC_TO_TEXTURE) {
        RSD_CALL_GL(glGenerateMipmap, drv->glTarget);
    }
    rsdGLCheckError(rsc, "Upload2DTexture");
}
#endif

static void UploadToTexture(const Context *rsc, const Allocation *alloc) {
#ifndef RS_COMPATIBILITY_LIB
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_IO_INPUT) {
        if (!drv->textureID) {
            RSD_CALL_GL(glGenTextures, 1, &drv->textureID);
        }
        return;
    }

    if (!drv->glType || !drv->glFormat) {
        return;
    }

    if (!alloc->mHal.drvState.lod[0].mallocPtr) {
        return;
    }

    bool isFirstUpload = false;

    if (!drv->textureID) {
        RSD_CALL_GL(glGenTextures, 1, &drv->textureID);
        isFirstUpload = true;
    }

    Upload2DTexture(rsc, alloc, isFirstUpload);

    if (!(alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_SCRIPT)) {
        if (alloc->mHal.drvState.lod[0].mallocPtr) {
            free(alloc->mHal.drvState.lod[0].mallocPtr);
            alloc->mHal.drvState.lod[0].mallocPtr = NULL;
        }
    }
    rsdGLCheckError(rsc, "UploadToTexture");
#endif
}

static void AllocateRenderTarget(const Context *rsc, const Allocation *alloc) {
#ifndef RS_COMPATIBILITY_LIB
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    if (!drv->glFormat) {
        return;
    }

    if (!drv->renderTargetID) {
        RSD_CALL_GL(glGenRenderbuffers, 1, &drv->renderTargetID);

        if (!drv->renderTargetID) {
            // This should generally not happen
            ALOGE("allocateRenderTarget failed to gen mRenderTargetID");
            rsc->dumpDebug();
            return;
        }
        RSD_CALL_GL(glBindRenderbuffer, GL_RENDERBUFFER, drv->renderTargetID);
        RSD_CALL_GL(glRenderbufferStorage, GL_RENDERBUFFER, drv->glFormat,
                    alloc->mHal.drvState.lod[0].dimX, alloc->mHal.drvState.lod[0].dimY);
    }
    rsdGLCheckError(rsc, "AllocateRenderTarget");
#endif
}

static void UploadToBufferObject(const Context *rsc, const Allocation *alloc) {
#ifndef RS_COMPATIBILITY_LIB
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    rsAssert(!alloc->mHal.state.type->getDimY());
    rsAssert(!alloc->mHal.state.type->getDimZ());

    //alloc->mHal.state.usageFlags |= RS_ALLOCATION_USAGE_GRAPHICS_VERTEX;

    if (!drv->bufferID) {
        RSD_CALL_GL(glGenBuffers, 1, &drv->bufferID);
    }
    if (!drv->bufferID) {
        ALOGE("Upload to buffer object failed");
        drv->uploadDeferred = true;
        return;
    }
    RSD_CALL_GL(glBindBuffer, drv->glTarget, drv->bufferID);
    RSD_CALL_GL(glBufferData, drv->glTarget, alloc->mHal.state.type->getSizeBytes(),
                 alloc->mHal.drvState.lod[0].mallocPtr, GL_DYNAMIC_DRAW);
    RSD_CALL_GL(glBindBuffer, drv->glTarget, 0);
    rsdGLCheckError(rsc, "UploadToBufferObject");
#endif
}

static size_t AllocationBuildPointerTable(const Context *rsc, const Allocation *alloc,
        const Type *type, uint8_t *ptr) {
    alloc->mHal.drvState.lod[0].dimX = type->getDimX();
    alloc->mHal.drvState.lod[0].dimY = type->getDimY();
    alloc->mHal.drvState.lod[0].dimZ = type->getDimZ();
    alloc->mHal.drvState.lod[0].mallocPtr = 0;
    // Stride needs to be 16-byte aligned too!
    size_t stride = alloc->mHal.drvState.lod[0].dimX * type->getElementSizeBytes();
    alloc->mHal.drvState.lod[0].stride = rsRound(stride, 16);
    alloc->mHal.drvState.lodCount = type->getLODCount();
    alloc->mHal.drvState.faceCount = type->getDimFaces();

    size_t offsets[Allocation::MAX_LOD];
    memset(offsets, 0, sizeof(offsets));

    size_t o = alloc->mHal.drvState.lod[0].stride * rsMax(alloc->mHal.drvState.lod[0].dimY, 1u) *
            rsMax(alloc->mHal.drvState.lod[0].dimZ, 1u);
    if(alloc->mHal.drvState.lodCount > 1) {
        uint32_t tx = alloc->mHal.drvState.lod[0].dimX;
        uint32_t ty = alloc->mHal.drvState.lod[0].dimY;
        uint32_t tz = alloc->mHal.drvState.lod[0].dimZ;
        for (uint32_t lod=1; lod < alloc->mHal.drvState.lodCount; lod++) {
            alloc->mHal.drvState.lod[lod].dimX = tx;
            alloc->mHal.drvState.lod[lod].dimY = ty;
            alloc->mHal.drvState.lod[lod].dimZ = tz;
            alloc->mHal.drvState.lod[lod].stride =
                    rsRound(tx * type->getElementSizeBytes(), 16);
            offsets[lod] = o;
            o += alloc->mHal.drvState.lod[lod].stride * rsMax(ty, 1u) * rsMax(tz, 1u);
            if (tx > 1) tx >>= 1;
            if (ty > 1) ty >>= 1;
            if (tz > 1) tz >>= 1;
        }
    }
    alloc->mHal.drvState.faceOffset = o;

    alloc->mHal.drvState.lod[0].mallocPtr = ptr;
    for (uint32_t lod=1; lod < alloc->mHal.drvState.lodCount; lod++) {
        alloc->mHal.drvState.lod[lod].mallocPtr = ptr + offsets[lod];
    }

    size_t allocSize = alloc->mHal.drvState.faceOffset;
    if(alloc->mHal.drvState.faceCount) {
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

    uint8_t * ptr = NULL;
    if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_IO_OUTPUT) {
    } else if (alloc->mHal.state.userProvidedPtr != NULL) {
        // user-provided allocation
        // limitations: no faces, no LOD, USAGE_SCRIPT only
        if (alloc->mHal.state.usageFlags != (RS_ALLOCATION_USAGE_SCRIPT | RS_ALLOCATION_USAGE_SHARED)) {
            ALOGE("Can't use user-allocated buffers if usage is not USAGE_SCRIPT and USAGE_SHARED");
            return false;
        }
        if (alloc->getType()->getDimLOD() || alloc->getType()->getDimFaces()) {
            ALOGE("User-allocated buffers must not have multiple faces or LODs");
            return false;
        }
        ptr = (uint8_t*)alloc->mHal.state.userProvidedPtr;
    } else {
        // We align all allocations to a 16-byte boundary.
        ptr = (uint8_t *)memalign(16, allocSize);
        if (!ptr) {
            alloc->mHal.drv = NULL;
            free(drv);
            return false;
        }
        if (forceZero) {
            memset(ptr, 0, allocSize);
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

#ifndef RS_COMPATIBILITY_LIB
    drv->glType = rsdTypeToGLType(alloc->mHal.state.type->getElement()->getComponent().getType());
    drv->glFormat = rsdKindToGLFormat(alloc->mHal.state.type->getElement()->getComponent().getKind());
#else
    drv->glType = 0;
    drv->glFormat = 0;
#endif

    if (alloc->mHal.state.usageFlags & ~RS_ALLOCATION_USAGE_SCRIPT) {
        drv->uploadDeferred = true;
    }


    drv->readBackFBO = NULL;

    return true;
}

void rsdAllocationDestroy(const Context *rsc, Allocation *alloc) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

#ifndef RS_COMPATIBILITY_LIB
    if (drv->bufferID) {
        // Causes a SW crash....
        //ALOGV(" mBufferID %i", mBufferID);
        //glDeleteBuffers(1, &mBufferID);
        //mBufferID = 0;
    }
    if (drv->textureID) {
        RSD_CALL_GL(glDeleteTextures, 1, &drv->textureID);
        drv->textureID = 0;
    }
    if (drv->renderTargetID) {
        RSD_CALL_GL(glDeleteRenderbuffers, 1, &drv->renderTargetID);
        drv->renderTargetID = 0;
    }
#endif

    if (alloc->mHal.drvState.lod[0].mallocPtr) {
        // don't free user-allocated ptrs
        if (!(alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_SHARED)) {
            free(alloc->mHal.drvState.lod[0].mallocPtr);
        }
        alloc->mHal.drvState.lod[0].mallocPtr = NULL;
    }

#ifndef RS_COMPATIBILITY_LIB
    if (drv->readBackFBO != NULL) {
        delete drv->readBackFBO;
        drv->readBackFBO = NULL;
    }
#endif

    free(drv);
    alloc->mHal.drv = NULL;
}

void rsdAllocationResize(const Context *rsc, const Allocation *alloc,
                         const Type *newType, bool zeroNew) {
    const uint32_t oldDimX = alloc->mHal.drvState.lod[0].dimX;
    const uint32_t dimX = newType->getDimX();

    // can't resize Allocations with user-allocated buffers
    if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_SHARED) {
        ALOGE("Resize cannot be called on a USAGE_SHARED allocation");
        return;
    }
    void * oldPtr = alloc->mHal.drvState.lod[0].mallocPtr;
    // Calculate the object size
    size_t s = AllocationBuildPointerTable(rsc, alloc, newType, NULL);
    uint8_t *ptr = (uint8_t *)realloc(oldPtr, s);
    // Build the relative pointer tables.
    size_t verifySize = AllocationBuildPointerTable(rsc, alloc, newType, ptr);
    if(s != verifySize) {
        rsAssert(!"Size mismatch");
    }


    if (dimX > oldDimX) {
        uint32_t stride = alloc->mHal.state.elementSizeBytes;
        memset(((uint8_t *)alloc->mHal.drvState.lod[0].mallocPtr) + stride * oldDimX,
                 0, stride * (dimX - oldDimX));
    }
}

static void rsdAllocationSyncFromFBO(const Context *rsc, const Allocation *alloc) {
#ifndef RS_COMPATIBILITY_LIB
    if (!alloc->getIsScript()) {
        return; // nothing to sync
    }

    RsdHal *dc = (RsdHal *)rsc->mHal.drv;
    RsdFrameBufferObj *lastFbo = dc->gl.currentFrameBuffer;

    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    if (!drv->textureID && !drv->renderTargetID) {
        return; // nothing was rendered here yet, so nothing to sync
    }
    if (drv->readBackFBO == NULL) {
        drv->readBackFBO = new RsdFrameBufferObj();
        drv->readBackFBO->setColorTarget(drv, 0);
        drv->readBackFBO->setDimensions(alloc->getType()->getDimX(),
                                        alloc->getType()->getDimY());
    }

    // Bind the framebuffer object so we can read back from it
    drv->readBackFBO->setActive(rsc);

    // Do the readback
    RSD_CALL_GL(glReadPixels, 0, 0, alloc->mHal.drvState.lod[0].dimX,
                alloc->mHal.drvState.lod[0].dimY,
                drv->glFormat, drv->glType, alloc->mHal.drvState.lod[0].mallocPtr);

    // Revert framebuffer to its original
    lastFbo->setActive(rsc);
#endif
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
#ifndef RS_COMPATIBILITY_LIB
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    UploadToTexture(rsc, alloc);
    return drv->textureID;
#else
    return 0;
#endif
}

#ifndef RS_COMPATIBILITY_LIB
static bool IoGetBuffer(const Context *rsc, Allocation *alloc, ANativeWindow *nw) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    int32_t r = native_window_dequeue_buffer_and_wait(nw, &drv->wndBuffer);
    if (r) {
        rsc->setError(RS_ERROR_DRIVER, "Error getting next IO output buffer.");
        return false;
    }

    // Must lock the whole surface
    GraphicBufferMapper &mapper = GraphicBufferMapper::get();
    Rect bounds(drv->wndBuffer->width, drv->wndBuffer->height);

    void *dst = NULL;
    mapper.lock(drv->wndBuffer->handle,
            GRALLOC_USAGE_SW_READ_NEVER | GRALLOC_USAGE_SW_WRITE_OFTEN,
            bounds, &dst);
    alloc->mHal.drvState.lod[0].mallocPtr = dst;
    alloc->mHal.drvState.lod[0].stride = drv->wndBuffer->stride * alloc->mHal.state.elementSizeBytes;
    rsAssert((alloc->mHal.drvState.lod[0].stride & 0xf) == 0);

    return true;
}
#endif

void rsdAllocationSetSurfaceTexture(const Context *rsc, Allocation *alloc, ANativeWindow *nw) {
#ifndef RS_COMPATIBILITY_LIB
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    //ALOGE("rsdAllocationSetSurfaceTexture %p  %p", alloc, nw);

    if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_GRAPHICS_RENDER_TARGET) {
        //TODO finish support for render target + script
        drv->wnd = nw;
        return;
    }


    // Cleanup old surface if there is one.
    if (alloc->mHal.state.wndSurface) {
        ANativeWindow *old = alloc->mHal.state.wndSurface;
        GraphicBufferMapper &mapper = GraphicBufferMapper::get();
        mapper.unlock(drv->wndBuffer->handle);
        old->queueBuffer(old, drv->wndBuffer, -1);
    }

    if (nw != NULL) {
        int32_t r;
        uint32_t flags = 0;
        if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_SCRIPT) {
            flags |= GRALLOC_USAGE_SW_READ_RARELY | GRALLOC_USAGE_SW_WRITE_OFTEN;
        }
        if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_GRAPHICS_RENDER_TARGET) {
            flags |= GRALLOC_USAGE_HW_RENDER;
        }

        r = native_window_set_usage(nw, flags);
        if (r) {
            rsc->setError(RS_ERROR_DRIVER, "Error setting IO output buffer usage.");
            return;
        }

        r = native_window_set_buffers_dimensions(nw, alloc->mHal.drvState.lod[0].dimX,
                                                 alloc->mHal.drvState.lod[0].dimY);
        if (r) {
            rsc->setError(RS_ERROR_DRIVER, "Error setting IO output buffer dimensions.");
            return;
        }

        r = native_window_set_buffer_count(nw, 3);
        if (r) {
            rsc->setError(RS_ERROR_DRIVER, "Error setting IO output buffer count.");
            return;
        }

        IoGetBuffer(rsc, alloc, nw);
    }
#endif
}

void rsdAllocationIoSend(const Context *rsc, Allocation *alloc) {
#ifndef RS_COMPATIBILITY_LIB
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    ANativeWindow *nw = alloc->mHal.state.wndSurface;

    if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_GRAPHICS_RENDER_TARGET) {
        RsdHal *dc = (RsdHal *)rsc->mHal.drv;
        RSD_CALL_GL(eglSwapBuffers, dc->gl.egl.display, dc->gl.egl.surface);
        return;
    }

    if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_SCRIPT) {
        GraphicBufferMapper &mapper = GraphicBufferMapper::get();
        mapper.unlock(drv->wndBuffer->handle);
        int32_t r = nw->queueBuffer(nw, drv->wndBuffer, -1);
        if (r) {
            rsc->setError(RS_ERROR_DRIVER, "Error sending IO output buffer.");
            return;
        }

        IoGetBuffer(rsc, alloc, nw);
    }
#endif
}

void rsdAllocationIoReceive(const Context *rsc, Allocation *alloc) {
#ifndef RS_COMPATIBILITY_LIB
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    alloc->mHal.state.surfaceTexture->updateTexImage();
#endif
}


void rsdAllocationData1D(const Context *rsc, const Allocation *alloc,
                         uint32_t xoff, uint32_t lod, uint32_t count,
                         const void *data, size_t sizeBytes) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    const uint32_t eSize = alloc->mHal.state.type->getElementSizeBytes();
    uint8_t * ptr = GetOffsetPtr(alloc, xoff, 0, 0, RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X);
    uint32_t size = count * eSize;

    if (ptr != data) {
        // Skip the copy if we are the same allocation. This can arise from
        // our Bitmap optimization, where we share the same storage.
        if (alloc->mHal.state.hasReferences) {
            alloc->incRefs(data, count);
            alloc->decRefs(ptr, count);
        }
        memcpy(ptr, data, size);
    }
    drv->uploadDeferred = true;
}

void rsdAllocationData2D(const Context *rsc, const Allocation *alloc,
                         uint32_t xoff, uint32_t yoff, uint32_t lod, RsAllocationCubemapFace face,
                         uint32_t w, uint32_t h, const void *data, size_t sizeBytes, size_t stride) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    uint32_t lineSize = eSize * w;
    if (!stride) {
        stride = lineSize;
    }

    if (alloc->mHal.drvState.lod[0].mallocPtr) {
        const uint8_t *src = static_cast<const uint8_t *>(data);
        uint8_t *dst = GetOffsetPtr(alloc, xoff, yoff, lod, face);
        if (dst == src) {
            // Skip the copy if we are the same allocation. This can arise from
            // our Bitmap optimization, where we share the same storage.
            drv->uploadDeferred = true;
            return;
        }

        for (uint32_t line=yoff; line < (yoff+h); line++) {
            if (alloc->mHal.state.hasReferences) {
                alloc->incRefs(src, w);
                alloc->decRefs(dst, w);
            }
            memcpy(dst, src, lineSize);
            src += stride;
            dst += alloc->mHal.drvState.lod[lod].stride;
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
    const uint32_t eSize = alloc->mHal.state.type->getElementSizeBytes();
    const uint8_t * ptr = GetOffsetPtr(alloc, xoff, 0, 0, RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X);
    if (data != ptr) {
        // Skip the copy if we are the same allocation. This can arise from
        // our Bitmap optimization, where we share the same storage.
        memcpy(data, ptr, count * eSize);
    }
}

void rsdAllocationRead2D(const Context *rsc, const Allocation *alloc,
                                uint32_t xoff, uint32_t yoff, uint32_t lod, RsAllocationCubemapFace face,
                                uint32_t w, uint32_t h, void *data, size_t sizeBytes, size_t stride) {
    uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    uint32_t lineSize = eSize * w;
    if (!stride) {
        stride = lineSize;
    }

    if (alloc->mHal.drvState.lod[0].mallocPtr) {
        uint8_t *dst = static_cast<uint8_t *>(data);
        const uint8_t *src = GetOffsetPtr(alloc, xoff, yoff, lod, face);
        if (dst == src) {
            // Skip the copy if we are the same allocation. This can arise from
            // our Bitmap optimization, where we share the same storage.
            return;
        }

        for (uint32_t line=yoff; line < (yoff+h); line++) {
            memcpy(dst, src, lineSize);
            dst += stride;
            src += alloc->mHal.drvState.lod[lod].stride;
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
    return alloc->mHal.drvState.lod[0].mallocPtr;
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
    uint32_t w = alloc->mHal.drvState.lod[lod + 1].dimX;
    uint32_t h = alloc->mHal.drvState.lod[lod + 1].dimY;

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
    uint32_t w = alloc->mHal.drvState.lod[lod + 1].dimX;
    uint32_t h = alloc->mHal.drvState.lod[lod + 1].dimY;

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
    uint32_t w = alloc->mHal.drvState.lod[lod + 1].dimX;
    uint32_t h = alloc->mHal.drvState.lod[lod + 1].dimY;

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
    if(!alloc->mHal.drvState.lod[0].mallocPtr) {
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


