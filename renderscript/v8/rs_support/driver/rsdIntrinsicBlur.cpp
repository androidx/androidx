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
    float fp[104];
    short ip[104];
    float radius;
    int iradius;
    ObjectBaseRef<Allocation> alloc;
};

static void ComputeGaussianWeights(ConvolveParams *cp) {
    // Compute gaussian weights for the blur
    // e is the euler's number
    float e = 2.718281828459045f;
    float pi = 3.1415926535897932f;
    // g(x) = ( 1 / sqrt( 2 * pi ) * sigma) * e ^ ( -x^2 / 2 * sigma^2 )
    // x is of the form [-radius .. 0 .. radius]
    // and sigma varies with radius.
    // Based on some experimental radius values and sigma's
    // we approximately fit sigma = f(radius) as
    // sigma = radius * 0.4  + 0.6
    // The larger the radius gets, the more our gaussian blur
    // will resemble a box blur since with large sigma
    // the gaussian curve begins to lose its shape
    float sigma = 0.4f * cp->radius + 0.6f;

    // Now compute the coefficients. We will store some redundant values to save
    // some math during the blur calculations precompute some values
    float coeff1 = 1.0f / (sqrtf(2.0f * pi) * sigma);
    float coeff2 = - 1.0f / (2.0f * sigma * sigma);

    float normalizeFactor = 0.0f;
    float floatR = 0.0f;
    int r;
    cp->iradius = (float)ceil(cp->radius) + 0.5f;
    for (r = -cp->iradius; r <= cp->iradius; r ++) {
        floatR = (float)r;
        cp->fp[r + cp->iradius] = coeff1 * powf(e, floatR * floatR * coeff2);
        normalizeFactor += cp->fp[r + cp->iradius];
    }

    //Now we need to normalize the weights because all our coefficients need to add up to one
    normalizeFactor = 1.0f / normalizeFactor;
    for (r = -cp->iradius; r <= cp->iradius; r ++) {
        cp->fp[r + cp->iradius] *= normalizeFactor;
        cp->ip[r + cp->iradius] = (short)(cp->ip[r + cp->iradius] * 32768);
    }
}

static void Blur_Bind(const Context *dc, const Script *script,
                             void * intrinsicData, uint32_t slot, Allocation *data) {
    ConvolveParams *cp = (ConvolveParams *)intrinsicData;
    rsAssert(slot == 1);
    cp->alloc.set(data);
}

static void Blur_SetVar(const Context *dc, const Script *script, void * intrinsicData,
                               uint32_t slot, void *data, size_t dataLength) {
    ConvolveParams *cp = (ConvolveParams *)intrinsicData;
    rsAssert(slot == 0);

    cp->radius = ((const float *)data)[0];
    ComputeGaussianWeights(cp);
}



static void OneV(const RsForEachStubParamStruct *p, float4 *out, int32_t x, int32_t y,
                 const uchar *ptrIn, int iStride, const float* gPtr, int iradius) {

    const uchar *pi = ptrIn + x*4;

    float4 blurredPixel = 0;
    for (int r = -iradius; r <= iradius; r ++) {
        int validY = rsMax((y + r), 0);
        validY = rsMin(validY, (int)(p->dimY - 1));
        const uchar4 *pvy = (const uchar4 *)&pi[validY * iStride];
        float4 pf = convert_float4(pvy[0]);
        blurredPixel += pf * gPtr[0];
        gPtr++;
    }

    out->xyzw = blurredPixel;
}

extern "C" void rsdIntrinsicBlurVF_K(void *dst, const void *pin, int stride, const void *gptr, int rct, int x1, int x2);
extern "C" void rsdIntrinsicBlurHF_K(void *dst, const void *pin, const void *gptr, int rct, int x1, int x2);

static void OneVF(float4 *out,
                  const uchar *ptrIn, int iStride, const float* gPtr, int ct,
                  int x1, int x2) {

#if defined(ARCH_ARM_HAVE_NEON)
    {
        int t = (x2 - x1);
        t &= ~1;
        if(t) {
            rsdIntrinsicBlurVF_K(out, ptrIn, iStride, gPtr, ct, x1, x1 + t);
        }
        x1 += t;
    }
#endif

    while(x2 > x1) {
        const uchar *pi = ptrIn + x1 * 4;
        float4 blurredPixel = 0;
        const float* gp = gPtr;

        for (int r = 0; r < ct; r++) {
            float4 pf = convert_float4(((const uchar4 *)pi)[0]);
            blurredPixel += pf * gp[0];
            pi += iStride;
            gp++;
        }
        out->xyzw = blurredPixel;
        x1++;
        out++;
    }
}

static void OneH(const RsForEachStubParamStruct *p, uchar4 *out, int32_t x,
                const float4 *ptrIn, const float* gPtr, int iradius) {

    float4 blurredPixel = 0;
    for (int r = -iradius; r <= iradius; r ++) {
        int validX = rsMax((x + r), 0);
        validX = rsMin(validX, (int)(p->dimX - 1));
        float4 pf = ptrIn[validX];
        blurredPixel += pf * gPtr[0];
        gPtr++;
    }

    out->xyzw = convert_uchar4(blurredPixel);
}


static void Blur_uchar4(const RsForEachStubParamStruct *p,
                                    uint32_t xstart, uint32_t xend,
                                    uint32_t instep, uint32_t outstep) {
    float buf[4 * 2048];
    ConvolveParams *cp = (ConvolveParams *)p->usr;
    DrvAllocation *din = (DrvAllocation *)cp->alloc->mHal.drv;
    const uchar *pin = (const uchar *)din->lod[0].mallocPtr;

    uchar4 *out = (uchar4 *)p->out;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    float4 *fout = (float4 *)buf;
    int y = p->y;
    if ((y > cp->iradius) && (y < ((int)p->dimY - cp->iradius))) {
        const uchar *pi = pin + (y - cp->iradius) * din->lod[0].stride;
        OneVF(fout, pi, din->lod[0].stride, cp->fp, cp->iradius * 2 + 1, x1, x2);
    } else {
        while(x2 > x1) {
            OneV(p, fout, x1, y, pin, din->lod[0].stride, cp->fp, cp->iradius);
            fout++;
            x1++;
        }
    }

    x1 = xstart;
    while ((x1 < (uint32_t)cp->iradius) && (x1 < x2)) {
        OneH(p, out, x1, (float4 *)buf, cp->fp, cp->iradius);
        out++;
        x1++;
    }
#if defined(ARCH_ARM_HAVE_NEON)
    if ((x1 + cp->iradius) < x2) {
        rsdIntrinsicBlurHF_K(out, ((float4 *)buf) - cp->iradius, cp->fp, cp->iradius * 2 + 1, x1, x2 - cp->iradius);
        out += (x2 - cp->iradius) - x1;
        x1 = x2 - cp->iradius;
    }
#endif
    while(x2 > x1) {
        OneH(p, out, x1, (float4 *)buf, cp->fp, cp->iradius);
        out++;
        x1++;
    }

}

void * rsdIntrinsic_InitBlur(const android::renderscript::Context *dc,
                                    android::renderscript::Script *script,
                                    RsdIntriniscFuncs_t *funcs) {

    script->mHal.info.exportedVariableCount = 2;
    funcs->bind = Blur_Bind;
    funcs->setVar = Blur_SetVar;
    funcs->root = Blur_uchar4;

    ConvolveParams *cp = (ConvolveParams *)calloc(1, sizeof(ConvolveParams));
    cp->radius = 5;
    ComputeGaussianWeights(cp);
    return cp;
}


