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


#include "rsdCore.h"
#include "rsdIntrinsics.h"
#include "rsdAllocation.h"

#include "rsdIntrinsicInlines.h"

using namespace android;
using namespace android::renderscript;

struct ConvolveParams {
    float fp[16];
    short ip[16];
    ObjectBaseRef<Allocation> alloc;
};

static void Convolve3x3_Bind(const Context *dc, const Script *script,
                             void * intrinsicData, uint32_t slot, Allocation *data) {
    ConvolveParams *cp = (ConvolveParams *)intrinsicData;
    rsAssert(slot == 1);
    cp->alloc.set(data);
}

static void Convolve3x3_SetVar(const Context *dc, const Script *script, void * intrinsicData,
                               uint32_t slot, void *data, size_t dataLength) {
    ConvolveParams *cp = (ConvolveParams *)intrinsicData;

    rsAssert(slot == 0);
    memcpy (cp->fp, data, dataLength);
    for(int ct=0; ct < 9; ct++) {
        cp->ip[ct] = (short)(cp->fp[ct] * 255.f + 0.5f);
    }
}

extern "C" void rsdIntrinsicConvolve3x3_K(void *dst, const void *y0, const void *y1, const void *y2, const short *coef, uint32_t count);


static void ConvolveOne(const RsForEachStubParamStruct *p, uint32_t x, uchar4 *out,
                        const uchar4 *py0, const uchar4 *py1, const uchar4 *py2,
                        const float* coeff) {

    uint32_t x1 = rsMax((int32_t)x-1, 0);
    uint32_t x2 = rsMin((int32_t)x+1, (int32_t)p->dimX);

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

static void Convolve3x3_uchar4(const RsForEachStubParamStruct *p,
                                    uint32_t xstart, uint32_t xend,
                                    uint32_t instep, uint32_t outstep) {
    ConvolveParams *cp = (ConvolveParams *)p->usr;
    DrvAllocation *din = (DrvAllocation *)cp->alloc->mHal.drv;
    const uchar *pin = (const uchar *)din->lod[0].mallocPtr;

    uint32_t y1 = rsMin((int32_t)p->y + 1, (int32_t)(p->dimY-1));
    uint32_t y2 = rsMax((int32_t)p->y - 1, 0);
    const uchar4 *py0 = (const uchar4 *)(pin + din->lod[0].stride * y2);
    const uchar4 *py1 = (const uchar4 *)(pin + din->lod[0].stride * p->y);
    const uchar4 *py2 = (const uchar4 *)(pin + din->lod[0].stride * y1);

    uchar4 *out = (uchar4 *)p->out;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;
    if(x1 == 0) {
        ConvolveOne(p, 0, out, py0, py1, py2, cp->fp);
        x1 ++;
        out++;
    }

    if(x2 > x1) {
#if defined(ARCH_ARM_HAVE_NEON)
        int32_t len = (x2 - x1 - 1) >> 1;
        if(len > 0) {
            rsdIntrinsicConvolve3x3_K(out, &py0[x1-1], &py1[x1-1], &py2[x1-1], cp->ip, len);
            x1 += len << 1;
            out += len << 1;
        }
#endif

        while(x1 != x2) {
            ConvolveOne(p, x1, out, py0, py1, py2, cp->fp);
            out++;
            x1++;
        }
    }
}

void * rsdIntrinsic_InitConvolve3x3(const android::renderscript::Context *dc,
                                    android::renderscript::Script *script,
                                    RsdIntriniscFuncs_t *funcs) {

    script->mHal.info.exportedVariableCount = 2;
    funcs->bind = Convolve3x3_Bind;
    funcs->setVar = Convolve3x3_SetVar;
    funcs->root = Convolve3x3_uchar4;

    ConvolveParams *cp = (ConvolveParams *)calloc(1, sizeof(ConvolveParams));
    for(int ct=0; ct < 9; ct++) {
        cp->fp[ct] = 1.f / 9.f;
        cp->ip[ct] = (short)(cp->fp[ct] * 255.f + 0.5f);
    }
    return cp;
}


