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


class RsdCpuScriptIntrinsicConvolve3x3 : public RsdCpuScriptIntrinsic {
public:
    virtual void populateScript(Script *);
    virtual void invokeFreeChildren();

    virtual void setGlobalVar(uint32_t slot, const void *data, size_t dataLength);
    virtual void setGlobalObj(uint32_t slot, ObjectBase *data);

    virtual ~RsdCpuScriptIntrinsicConvolve3x3();
    RsdCpuScriptIntrinsicConvolve3x3(RsdCpuReferenceImpl *ctx, const Script *s, const Element *);

protected:
    float mFp[16];
    short mIp[16];
    ObjectBaseRef<const Allocation> mAlloc;
    ObjectBaseRef<const Element> mElement;

    static void kernel(const RsForEachStubParamStruct *p,
                       uint32_t xstart, uint32_t xend,
                       uint32_t instep, uint32_t outstep);
};

}
}


void RsdCpuScriptIntrinsicConvolve3x3::setGlobalObj(uint32_t slot, ObjectBase *data) {
    rsAssert(slot == 1);
    mAlloc.set(static_cast<Allocation *>(data));
}

void RsdCpuScriptIntrinsicConvolve3x3::setGlobalVar(uint32_t slot, const void *data,
                                                    size_t dataLength) {
    rsAssert(slot == 0);
    memcpy (&mFp, data, dataLength);
    for(int ct=0; ct < 9; ct++) {
        mIp[ct] = (short)(mFp[ct] * 255.f + 0.5f);
    }
}

extern "C" void rsdIntrinsicConvolve3x3_K(void *dst, const void *y0, const void *y1,
                                          const void *y2, const short *coef, uint32_t count);


static void ConvolveOne(const RsForEachStubParamStruct *p, uint32_t x, uchar4 *out,
                        const uchar4 *py0, const uchar4 *py1, const uchar4 *py2,
                        const float* coeff) {

    uint32_t x1 = rsMax((int32_t)x-1, 0);
    uint32_t x2 = rsMin((int32_t)x+1, (int32_t)p->dimX-1);

    float4 px = convert_float4(py0[x1]) * coeff[0] +
                convert_float4(py0[x]) * coeff[1] +
                convert_float4(py0[x2]) * coeff[2] +
                convert_float4(py1[x1]) * coeff[3] +
                convert_float4(py1[x]) * coeff[4] +
                convert_float4(py1[x2]) * coeff[5] +
                convert_float4(py2[x1]) * coeff[6] +
                convert_float4(py2[x]) * coeff[7] +
                convert_float4(py2[x2]) * coeff[8];

    px = clamp(px, 0.f, 255.f);
    uchar4 o = {(uchar)px.x, (uchar)px.y, (uchar)px.z, (uchar)px.w};
    *out = o;
}

void RsdCpuScriptIntrinsicConvolve3x3::kernel(const RsForEachStubParamStruct *p,
                                              uint32_t xstart, uint32_t xend,
                                              uint32_t instep, uint32_t outstep) {
    RsdCpuScriptIntrinsicConvolve3x3 *cp = (RsdCpuScriptIntrinsicConvolve3x3 *)p->usr;

    if (!cp->mAlloc.get()) {
        ALOGE("Convolve3x3 executed without input, skipping");
        return;
    }
    const uchar *pin = (const uchar *)cp->mAlloc->mHal.drvState.lod[0].mallocPtr;
    const size_t stride = cp->mAlloc->mHal.drvState.lod[0].stride;

    uint32_t y1 = rsMin((int32_t)p->y + 1, (int32_t)(p->dimY-1));
    uint32_t y2 = rsMax((int32_t)p->y - 1, 0);
    const uchar4 *py0 = (const uchar4 *)(pin + stride * y2);
    const uchar4 *py1 = (const uchar4 *)(pin + stride * p->y);
    const uchar4 *py2 = (const uchar4 *)(pin + stride * y1);

    uchar4 *out = (uchar4 *)p->out;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;
    if(x1 == 0) {
        ConvolveOne(p, 0, out, py0, py1, py2, cp->mFp);
        x1 ++;
        out++;
    }

    if(x2 > x1) {
#if defined(ARCH_ARM_HAVE_NEON)
        int32_t len = (x2 - x1 - 1) >> 1;
        if(len > 0) {
            rsdIntrinsicConvolve3x3_K(out, &py0[x1-1], &py1[x1-1], &py2[x1-1], cp->mIp, len);
            x1 += len << 1;
            out += len << 1;
        }
#endif

        while(x1 != x2) {
            ConvolveOne(p, x1, out, py0, py1, py2, cp->mFp);
            out++;
            x1++;
        }
    }
}

RsdCpuScriptIntrinsicConvolve3x3::RsdCpuScriptIntrinsicConvolve3x3(
            RsdCpuReferenceImpl *ctx, const Script *s, const Element *e)
            : RsdCpuScriptIntrinsic(ctx, s, e, RS_SCRIPT_INTRINSIC_ID_CONVOLVE_3x3) {

    mRootPtr = &kernel;
    for(int ct=0; ct < 9; ct++) {
        mFp[ct] = 1.f / 9.f;
        mIp[ct] = (short)(mFp[ct] * 255.f + 0.5f);
    }
}

RsdCpuScriptIntrinsicConvolve3x3::~RsdCpuScriptIntrinsicConvolve3x3() {
}

void RsdCpuScriptIntrinsicConvolve3x3::populateScript(Script *s) {
    s->mHal.info.exportedVariableCount = 2;
}

void RsdCpuScriptIntrinsicConvolve3x3::invokeFreeChildren() {
    mAlloc.clear();
}


RsdCpuScriptImpl * rsdIntrinsic_Convolve3x3(RsdCpuReferenceImpl *ctx, const Script *s, const Element *e) {

    return new RsdCpuScriptIntrinsicConvolve3x3(ctx, s, e);
}


