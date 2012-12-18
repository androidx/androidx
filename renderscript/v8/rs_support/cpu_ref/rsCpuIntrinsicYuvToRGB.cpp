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


#include "rsCpuIntrinsic.h"
#include "rsCpuIntrinsicInlines.h"

using namespace android;
using namespace android::renderscript;

namespace android {
namespace renderscript {


class RsdCpuScriptIntrinsicYuvToRGB : public RsdCpuScriptIntrinsic {
public:
    virtual void populateScript(Script *);
    virtual void invokeFreeChildren();

    virtual void setGlobalObj(uint32_t slot, ObjectBase *data);

    virtual ~RsdCpuScriptIntrinsicYuvToRGB();
    RsdCpuScriptIntrinsicYuvToRGB(RsdCpuReferenceImpl *ctx, const Script *s, const Element *e);

protected:
    ObjectBaseRef<Allocation> alloc;

    static void kernel(const RsForEachStubParamStruct *p,
                       uint32_t xstart, uint32_t xend,
                       uint32_t instep, uint32_t outstep);
};

}
}


void RsdCpuScriptIntrinsicYuvToRGB::setGlobalObj(uint32_t slot, ObjectBase *data) {
    rsAssert(slot == 0);
    alloc.set(static_cast<Allocation *>(data));
}




static uchar4 rsYuvToRGBA_uchar4(uchar y, uchar u, uchar v) {
    short Y = ((short)y) - 16;
    short U = ((short)u) - 128;
    short V = ((short)v) - 128;

    short4 p;
    p.r = (Y * 298 + V * 409 + 128) >> 8;
    p.g = (Y * 298 - U * 100 - V * 208 + 128) >> 8;
    p.b = (Y * 298 + U * 516 + 128) >> 8;
    p.a = 255;
    if(p.r < 0) {
        p.r = 0;
    }
    if(p.r > 255) {
        p.r = 255;
    }
    if(p.g < 0) {
        p.g = 0;
    }
    if(p.g > 255) {
        p.g = 255;
    }
    if(p.b < 0) {
        p.b = 0;
    }
    if(p.b > 255) {
        p.b = 255;
    }

    return (uchar4){p.r, p.g, p.b, p.a};
}


static short YuvCoeff[] = {
    298, 409, -100, 516,   -208, 255, 0, 0,
    16, 16, 16, 16,        16, 16, 16, 16,
    128, 128, 128, 128, 128, 128, 128, 128,
    298, 298, 298, 298, 298, 298, 298, 298,
    255, 255, 255, 255, 255, 255, 255, 255


};

extern "C" void rsdIntrinsicYuv_K(void *dst, const uchar *Y, const uchar *uv, uint32_t count, const short *param);

void RsdCpuScriptIntrinsicYuvToRGB::kernel(const RsForEachStubParamStruct *p,
                                           uint32_t xstart, uint32_t xend,
                                           uint32_t instep, uint32_t outstep) {
    RsdCpuScriptIntrinsicYuvToRGB *cp = (RsdCpuScriptIntrinsicYuvToRGB *)p->usr;
    if (!cp->alloc.get()) {
        ALOGE("YuvToRGB executed without input, skipping");
        return;
    }
    const uchar *pin = (const uchar *)cp->alloc->mHal.drvState.lod[0].mallocPtr;
    const size_t stride = cp->alloc->mHal.drvState.lod[0].stride;

    const uchar *Y = pin + (p->y * p->dimX);
    const uchar *uv = pin + (p->dimX * p->dimY);
    uv += (p->y>>1) * p->dimX;

    uchar4 *out = (uchar4 *)p->out;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    if(x2 > x1) {
#if defined(ARCH_ARM_HAVE_NEON)
        int32_t len = (x2 - x1 - 1) >> 3;
        if(len > 0) {
            rsdIntrinsicYuv_K(out, Y, uv, len, YuvCoeff);
            x1 += len << 3;
            out += len << 3;
        }
#endif

       // ALOGE("y %i  %i  %i", p->y, x1, x2);
        while(x1 < x2) {
            uchar u = uv[(x1 & 0xffffe) + 1];
            uchar v = uv[(x1 & 0xffffe) + 0];
            *out = rsYuvToRGBA_uchar4(Y[x1], u, v);
            out++;
            x1++;
            *out = rsYuvToRGBA_uchar4(Y[x1], u, v);
            out++;
            x1++;
        }
    }
}

RsdCpuScriptIntrinsicYuvToRGB::RsdCpuScriptIntrinsicYuvToRGB(
            RsdCpuReferenceImpl *ctx, const Script *s, const Element *e)
            : RsdCpuScriptIntrinsic(ctx, s, e, RS_SCRIPT_INTRINSIC_ID_YUV_TO_RGB) {

    mRootPtr = &kernel;
}

RsdCpuScriptIntrinsicYuvToRGB::~RsdCpuScriptIntrinsicYuvToRGB() {
}

void RsdCpuScriptIntrinsicYuvToRGB::populateScript(Script *s) {
    s->mHal.info.exportedVariableCount = 1;
}

void RsdCpuScriptIntrinsicYuvToRGB::invokeFreeChildren() {
    alloc.clear();
}


RsdCpuScriptImpl * rsdIntrinsic_YuvToRGB(RsdCpuReferenceImpl *ctx,
                                         const Script *s, const Element *e) {
    return new RsdCpuScriptIntrinsicYuvToRGB(ctx, s, e);
}


