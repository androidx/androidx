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


class RsdCpuScriptIntrinsicConvolve5x5 : public RsdCpuScriptIntrinsic {
public:
    virtual void populateScript(Script *);
    virtual void invokeFreeChildren();

    virtual void setGlobalVar(uint32_t slot, const void *data, size_t dataLength);
    virtual void setGlobalObj(uint32_t slot, ObjectBase *data);

    virtual ~RsdCpuScriptIntrinsicConvolve5x5();
    RsdCpuScriptIntrinsicConvolve5x5(RsdCpuReferenceImpl *ctx, const Script *s, const Element *e);

protected:
    float fp[28];
    short ip[28];
    ObjectBaseRef<Allocation> alloc;


    static void kernel(const RsForEachStubParamStruct *p,
                       uint32_t xstart, uint32_t xend,
                       uint32_t instep, uint32_t outstep);


};

}
}

void RsdCpuScriptIntrinsicConvolve5x5::setGlobalObj(uint32_t slot, ObjectBase *data) {
    rsAssert(slot == 1);
    alloc.set(static_cast<Allocation *>(data));
}

void RsdCpuScriptIntrinsicConvolve5x5::setGlobalVar(uint32_t slot,
                                                    const void *data, size_t dataLength) {
    rsAssert(slot == 0);
    memcpy (&fp, data, dataLength);
    for(int ct=0; ct < 25; ct++) {
        ip[ct] = (short)(fp[ct] * 255.f + 0.5f);
    }
}


static void One(const RsForEachStubParamStruct *p, uint32_t x, uchar4 *out,
                const uchar4 *py0, const uchar4 *py1, const uchar4 *py2, const uchar4 *py3, const uchar4 *py4,
                const float* coeff) {

    uint32_t x0 = rsMax((int32_t)x-2, 0);
    uint32_t x1 = rsMax((int32_t)x-1, 0);
    uint32_t x2 = x;
    uint32_t x3 = rsMin((int32_t)x+1, (int32_t)(p->dimX-1));
    uint32_t x4 = rsMin((int32_t)x+2, (int32_t)(p->dimX-1));

    float4 px = convert_float4(py0[x0]) * coeff[0] +
                convert_float4(py0[x1]) * coeff[1] +
                convert_float4(py0[x2]) * coeff[2] +
                convert_float4(py0[x3]) * coeff[3] +
                convert_float4(py0[x4]) * coeff[4] +

                convert_float4(py1[x0]) * coeff[5] +
                convert_float4(py1[x1]) * coeff[6] +
                convert_float4(py1[x2]) * coeff[7] +
                convert_float4(py1[x3]) * coeff[8] +
                convert_float4(py1[x4]) * coeff[9] +

                convert_float4(py2[x0]) * coeff[10] +
                convert_float4(py2[x1]) * coeff[11] +
                convert_float4(py2[x2]) * coeff[12] +
                convert_float4(py2[x3]) * coeff[13] +
                convert_float4(py2[x4]) * coeff[14] +

                convert_float4(py3[x0]) * coeff[15] +
                convert_float4(py3[x1]) * coeff[16] +
                convert_float4(py3[x2]) * coeff[17] +
                convert_float4(py3[x3]) * coeff[18] +
                convert_float4(py3[x4]) * coeff[19] +

                convert_float4(py4[x0]) * coeff[20] +
                convert_float4(py4[x1]) * coeff[21] +
                convert_float4(py4[x2]) * coeff[22] +
                convert_float4(py4[x3]) * coeff[23] +
                convert_float4(py4[x4]) * coeff[24];

    px = clamp(px, 0.f, 255.f);
    uchar4 o = {(uchar)px.x, (uchar)px.y, (uchar)px.z, (uchar)px.w};
    *out = o;
}

extern "C" void rsdIntrinsicConvolve5x5_K(void *dst, const void *y0, const void *y1,
                                          const void *y2, const void *y3, const void *y4,
                                          const short *coef, uint32_t count);

void RsdCpuScriptIntrinsicConvolve5x5::kernel(const RsForEachStubParamStruct *p,
                                              uint32_t xstart, uint32_t xend,
                                              uint32_t instep, uint32_t outstep) {
    RsdCpuScriptIntrinsicConvolve5x5 *cp = (RsdCpuScriptIntrinsicConvolve5x5 *)p->usr;
    if (!cp->alloc.get()) {
        ALOGE("Convolve5x5 executed without input, skipping");
        return;
    }
    const uchar *pin = (const uchar *)cp->alloc->mHal.drvState.lod[0].mallocPtr;
    const size_t stride = cp->alloc->mHal.drvState.lod[0].stride;

    uint32_t y0 = rsMax((int32_t)p->y-2, 0);
    uint32_t y1 = rsMax((int32_t)p->y-1, 0);
    uint32_t y2 = p->y;
    uint32_t y3 = rsMin((int32_t)p->y+1, (int32_t)(p->dimY-1));
    uint32_t y4 = rsMin((int32_t)p->y+2, (int32_t)(p->dimY-1));

    const uchar4 *py0 = (const uchar4 *)(pin + stride * y0);
    const uchar4 *py1 = (const uchar4 *)(pin + stride * y1);
    const uchar4 *py2 = (const uchar4 *)(pin + stride * y2);
    const uchar4 *py3 = (const uchar4 *)(pin + stride * y3);
    const uchar4 *py4 = (const uchar4 *)(pin + stride * y4);

    uchar4 *out = (uchar4 *)p->out;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    while((x1 < x2) && (x1 < 2)) {
        One(p, x1, out, py0, py1, py2, py3, py4, cp->fp);
        out++;
        x1++;
    }

#if defined(ARCH_ARM_HAVE_NEON)
    if((x1 + 3) < x2) {
        uint32_t len = (x2 - x1 - 3) >> 1;
        rsdIntrinsicConvolve5x5_K(out, py0, py1, py2, py3, py4, cp->ip, len);
        out += len << 1;
        x1 += len << 1;
    }
#endif

    while(x1 < x2) {
        One(p, x1, out, py0, py1, py2, py3, py4, cp->fp);
        out++;
        x1++;
    }
}


RsdCpuScriptIntrinsicConvolve5x5::RsdCpuScriptIntrinsicConvolve5x5(
            RsdCpuReferenceImpl *ctx, const Script *s, const Element *e)
            : RsdCpuScriptIntrinsic(ctx, s, e, RS_SCRIPT_INTRINSIC_ID_CONVOLVE_5x5) {

    mRootPtr = &kernel;
    for(int ct=0; ct < 9; ct++) {
        fp[ct] = 1.f / 25.f;
        ip[ct] = (short)(fp[ct] * 255.f + 0.5f);
    }
}

RsdCpuScriptIntrinsicConvolve5x5::~RsdCpuScriptIntrinsicConvolve5x5() {
}

void RsdCpuScriptIntrinsicConvolve5x5::populateScript(Script *s) {
    s->mHal.info.exportedVariableCount = 2;
}

void RsdCpuScriptIntrinsicConvolve5x5::invokeFreeChildren() {
    alloc.clear();
}


RsdCpuScriptImpl * rsdIntrinsic_Convolve5x5(RsdCpuReferenceImpl *ctx,
                                            const Script *s, const Element *e) {

    return new RsdCpuScriptIntrinsicConvolve5x5(ctx, s, e);
}



