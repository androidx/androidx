#include <android/native_window.h>
#include <android/log.h>

#include "rsCompatibilityLib.h"

#include "rsdCore.h"
#include "rsdAllocation.h"
#include "rsAllocation.h"

#define LOG_API(...)

using namespace android;
using namespace android::renderscript;

static bool IoGetBuffer(const Context *rsc, Allocation *alloc, ANativeWindow *nw) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    // Must lock the whole surface
    if(drv->wndBuffer == NULL) {
        drv->wndBuffer = new ANativeWindow_Buffer;
    }
    int32_t r = ANativeWindow_lock(nw, drv->wndBuffer, NULL);
    if (r) {
        LOG_API("Error Locking IO output buffer.");
        return false;
    }

    void *dst = drv->wndBuffer->bits;
    alloc->mHal.drvState.lod[0].mallocPtr = dst;
    alloc->mHal.drvState.lod[0].stride = drv->wndBuffer->stride * alloc->mHal.state.elementSizeBytes;
    return true;
}

extern "C" void rscAllocationSetSurface(RsContext rscR, RsAllocation allocR, ANativeWindow *nw) {
    Context *rsc = (Context *)rscR;
    Allocation *alloc = (Allocation *)allocR;
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;

    // Cleanup old surface if there is one.
    if (drv->wndSurface) {
        ANativeWindow *old = drv->wndSurface;
        ANativeWindow_unlockAndPost(old);
        drv->wndSurface = NULL;
        ANativeWindow_release(old);
        old = NULL;
    }

    if (nw != NULL) {
        int32_t r;
        r = ANativeWindow_setBuffersGeometry(nw, alloc->mHal.drvState.lod[0].dimX,
                                                 alloc->mHal.drvState.lod[0].dimY,
                                                 WINDOW_FORMAT_RGBA_8888);
        if (r) {
            LOG_API("Error setting IO output buffer geometry.");
            goto errorcmp;
        }

        IoGetBuffer(rsc, alloc, nw);
        drv->wndSurface = nw;
    }

    return;

 errorcmp:

    if (nw) {
        nw = NULL;
    }

}

extern "C" void rscAllocationDestroy(const Context *rsc, Allocation *alloc) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    if (alloc->mHal.drvState.lod[0].mallocPtr) {
        // don't free user-allocated ptrs or IO_OUTPUT buffers
        if (!(drv->useUserProvidedPtr) &&
            !(alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_IO_INPUT) &&
            !(alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_IO_OUTPUT)) {
                free(alloc->mHal.drvState.lod[0].mallocPtr);
        }
        alloc->mHal.drvState.lod[0].mallocPtr = NULL;
    }

    if ((alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_IO_OUTPUT) &&
        (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_SCRIPT)) {
        ANativeWindow *nw = drv->wndSurface;
        if (nw) {
            //If we have an attached surface, need to release it.
            ANativeWindow_unlockAndPost(nw);
            drv->wndSurface = NULL;
            ANativeWindow_release(nw);
            nw = NULL;
        }
    }
}

extern "C" void rscAllocationIoSend(const Context *rsc, Allocation *alloc) {
    DrvAllocation *drv = (DrvAllocation *)alloc->mHal.drv;
    ANativeWindow *nw = drv->wndSurface;
    if (nw) {
        if (alloc->mHal.state.usageFlags & RS_ALLOCATION_USAGE_SCRIPT) {
            int32_t r = ANativeWindow_unlockAndPost(nw);
            if (r) {
                LOG_API("Error sending IO output buffer.");
                return;
            }
            IoGetBuffer(rsc, alloc, nw);
        }
    } else {
        LOG_API("Sent IO buffer with no attached surface.");
        return;
    }
}

