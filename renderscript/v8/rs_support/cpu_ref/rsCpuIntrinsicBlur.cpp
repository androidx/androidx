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


class RsdCpuScriptIntrinsicBlur : public RsdCpuScriptIntrinsic {
public:
    virtual void populateScript(Script *);
    virtual void invokeFreeChildren();

    virtual void setGlobalVar(uint32_t slot, const void *data, size_t dataLength);
    virtual void setGlobalObj(uint32_t slot, ObjectBase *data);

    virtual ~RsdCpuScriptIntrinsicBlur();
    RsdCpuScriptIntrinsicBlur(RsdCpuReferenceImpl *ctx, const Script *s, const Element *e);

protected:
    float mFp[104];
    short mIp[104];
    void **mScratch;
    size_t *mScratchSize;
    float mRadius;
    int mIradius;
    ObjectBaseRef<Allocation> mAlloc;

    static void kernelU4(const RsForEachStubParamStruct *p,
                         uint32_t xstart, uint32_t xend,
                         uint32_t instep, uint32_t outstep);
    static void kernelU1(const RsForEachStubParamStruct *p,
                         uint32_t xstart, uint32_t xend,
                         uint32_t instep, uint32_t outstep);
    void ComputeGaussianWeights();
};

}
}


void RsdCpuScriptIntrinsicBlur::ComputeGaussianWeights() {
    memset(mFp, 0, sizeof(mFp));
    memset(mIp, 0, sizeof(mIp));

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
    float sigma = 0.4f * mRadius + 0.6f;

    // Now compute the coefficients. We will store some redundant values to save
    // some math during the blur calculations precompute some values
    float coeff1 = 1.0f / (sqrtf(2.0f * pi) * sigma);
    float coeff2 = - 1.0f / (2.0f * sigma * sigma);

    float normalizeFactor = 0.0f;
    float floatR = 0.0f;
    int r;
    mIradius = (float)ceil(mRadius) + 0.5f;
    for (r = -mIradius; r <= mIradius; r ++) {
        floatR = (float)r;
        mFp[r + mIradius] = coeff1 * powf(e, floatR * floatR * coeff2);
        normalizeFactor += mFp[r + mIradius];
    }

    //Now we need to normalize the weights because all our coefficients need to add up to one
    normalizeFactor = 1.0f / normalizeFactor;
    for (r = -mIradius; r <= mIradius; r ++) {
        mFp[r + mIradius] *= normalizeFactor;
        mIp[r + mIradius] = (short)(mIp[r + mIradius] * 32768);
    }
}

void RsdCpuScriptIntrinsicBlur::setGlobalObj(uint32_t slot, ObjectBase *data) {
    rsAssert(slot == 1);
    mAlloc.set(static_cast<Allocation *>(data));
}

void RsdCpuScriptIntrinsicBlur::setGlobalVar(uint32_t slot, const void *data, size_t dataLength) {
    rsAssert(slot == 0);
    mRadius = ((const float *)data)[0];
    ComputeGaussianWeights();
}



static void OneVU4(const RsForEachStubParamStruct *p, float4 *out, int32_t x, int32_t y,
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

static void OneVU1(const RsForEachStubParamStruct *p, float *out, int32_t x, int32_t y,
                   const uchar *ptrIn, int iStride, const float* gPtr, int iradius) {

    const uchar *pi = ptrIn + x;

    float blurredPixel = 0;
    for (int r = -iradius; r <= iradius; r ++) {
        int validY = rsMax((y + r), 0);
        validY = rsMin(validY, (int)(p->dimY - 1));
        float pf = (float)pi[validY * iStride];
        blurredPixel += pf * gPtr[0];
        gPtr++;
    }

    out[0] = blurredPixel;
}

extern "C" void rsdIntrinsicBlurVFU4_K(void *dst, const void *pin, int stride, const void *gptr, int rct, int x1, int ct);
extern "C" void rsdIntrinsicBlurHFU4_K(void *dst, const void *pin, const void *gptr, int rct, int x1, int ct);
extern "C" void rsdIntrinsicBlurHFU1_K(void *dst, const void *pin, const void *gptr, int rct, int x1, int ct);

static void OneVFU4(float4 *out,
                    const uchar *ptrIn, int iStride, const float* gPtr, int ct,
                    int x1, int x2) {

#if defined(ARCH_ARM_HAVE_NEON)
    {
        int t = (x2 - x1);
        t &= ~1;
        if(t) {
            rsdIntrinsicBlurVFU4_K(out, ptrIn, iStride, gPtr, ct, x1, x1 + t);
        }
        x1 += t;
    }
#endif

    while(x2 > x1) {
        const uchar *pi = ptrIn;
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
        ptrIn++;
    }
}

static void OneVFU1(float *out,
                    const uchar *ptrIn, int iStride, const float* gPtr, int ct, int x1, int x2) {

    int len = x2 - x1;

    while((x2 > x1) && (((int)ptrIn) & 0x3)) {
        const uchar *pi = ptrIn;
        float blurredPixel = 0;
        const float* gp = gPtr;

        for (int r = 0; r < ct; r++) {
            float pf = (float)pi[0];
            blurredPixel += pf * gp[0];
            pi += iStride;
            gp++;
        }
        out[0] = blurredPixel;
        x1++;
        out++;
        ptrIn++;
    }

#if defined(ARCH_ARM_HAVE_NEON)
    {
        int t = (x2 - x1) >> 2;
        t &= ~1;
        if(t) {
            rsdIntrinsicBlurVFU4_K(out, ptrIn, iStride, gPtr, ct, 0, t << 2);
            len -= t << 2;
            ptrIn += t << 2;
            out += t << 2;
        }
    }
#endif

    while(len) {
        const uchar *pi = ptrIn;
        float blurredPixel = 0;
        const float* gp = gPtr;

        for (int r = 0; r < ct; r++) {
            float pf = (float)pi[0];
            blurredPixel += pf * gp[0];
            pi += iStride;
            gp++;
        }
        out[0] = blurredPixel;
        len--;
        out++;
        ptrIn++;
    }
}

static void OneHU4(const RsForEachStubParamStruct *p, uchar4 *out, int32_t x,
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

static void OneHU1(const RsForEachStubParamStruct *p, uchar *out, int32_t x,
                   const float *ptrIn, const float* gPtr, int iradius) {

    float blurredPixel = 0;
    for (int r = -iradius; r <= iradius; r ++) {
        int validX = rsMax((x + r), 0);
        validX = rsMin(validX, (int)(p->dimX - 1));
        float pf = ptrIn[validX];
        blurredPixel += pf * gPtr[0];
        gPtr++;
    }

    out[0] = (uchar)blurredPixel;
}


void RsdCpuScriptIntrinsicBlur::kernelU4(const RsForEachStubParamStruct *p,
                                         uint32_t xstart, uint32_t xend,
                                         uint32_t instep, uint32_t outstep) {

    float stackbuf[4 * 2048];
    float *buf = &stackbuf[0];
    RsdCpuScriptIntrinsicBlur *cp = (RsdCpuScriptIntrinsicBlur *)p->usr;
    if (!cp->mAlloc.get()) {
        ALOGE("Blur executed without input, skipping");
        return;
    }
    const uchar *pin = (const uchar *)cp->mAlloc->mHal.drvState.lod[0].mallocPtr;
    const size_t stride = cp->mAlloc->mHal.drvState.lod[0].stride;

    uchar4 *out = (uchar4 *)p->out;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    if (p->dimX > 2048) {
        if ((p->dimX > cp->mScratchSize[p->lid]) || !cp->mScratch[p->lid]) {
            cp->mScratch[p->lid] = realloc(cp->mScratch[p->lid], p->dimX * 16);
            cp->mScratchSize[p->lid] = p->dimX;
        }
        buf = (float *)cp->mScratch[p->lid];
    }
    float4 *fout = (float4 *)buf;
    int y = p->y;
    if ((y > cp->mIradius) && (y < ((int)p->dimY - cp->mIradius))) {
        const uchar *pi = pin + (y - cp->mIradius) * stride;
        OneVFU4(fout, pi, stride, cp->mFp, cp->mIradius * 2 + 1, x1, x2);
    } else {
        while(x2 > x1) {
            OneVU4(p, fout, x1, y, pin, stride, cp->mFp, cp->mIradius);
            fout++;
            x1++;
        }
    }

    x1 = xstart;
    while ((x1 < (uint32_t)cp->mIradius) && (x1 < x2)) {
        OneHU4(p, out, x1, (float4 *)buf, cp->mFp, cp->mIradius);
        out++;
        x1++;
    }
#if defined(ARCH_ARM_HAVE_NEON)
    if ((x1 + cp->mIradius) < x2) {
        rsdIntrinsicBlurHFU4_K(out, ((float4 *)buf) - cp->mIradius, cp->mFp,
                               cp->mIradius * 2 + 1, x1, x2 - cp->mIradius);
        out += (x2 - cp->mIradius) - x1;
        x1 = x2 - cp->mIradius;
    }
#endif
    while(x2 > x1) {
        OneHU4(p, out, x1, (float4 *)buf, cp->mFp, cp->mIradius);
        out++;
        x1++;
    }
}

void RsdCpuScriptIntrinsicBlur::kernelU1(const RsForEachStubParamStruct *p,
                                         uint32_t xstart, uint32_t xend,
                                         uint32_t instep, uint32_t outstep) {
    float buf[4 * 2048];
    RsdCpuScriptIntrinsicBlur *cp = (RsdCpuScriptIntrinsicBlur *)p->usr;
    if (!cp->mAlloc.get()) {
        ALOGE("Blur executed without input, skipping");
        return;
    }
    const uchar *pin = (const uchar *)cp->mAlloc->mHal.drvState.lod[0].mallocPtr;
    const size_t stride = cp->mAlloc->mHal.drvState.lod[0].stride;

    uchar *out = (uchar *)p->out;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    float *fout = (float *)buf;
    int y = p->y;
    if ((y > cp->mIradius) && (y < ((int)p->dimY - cp->mIradius))) {
        const uchar *pi = pin + (y - cp->mIradius) * stride;
        OneVFU1(fout, pi, stride, cp->mFp, cp->mIradius * 2 + 1, x1, x2);
    } else {
        while(x2 > x1) {
            OneVU1(p, fout, x1, y, pin, stride, cp->mFp, cp->mIradius);
            fout++;
            x1++;
        }
    }

    x1 = xstart;
    while ((x1 < x2) &&
           ((x1 < (uint32_t)cp->mIradius) || (((int)out) & 0x3))) {
        OneHU1(p, out, x1, buf, cp->mFp, cp->mIradius);
        out++;
        x1++;
    }
#if defined(ARCH_ARM_HAVE_NEON)
    if ((x1 + cp->mIradius) < x2) {
        uint32_t len = x2 - (x1 + cp->mIradius);
        len &= ~3;
        rsdIntrinsicBlurHFU1_K(out, ((float *)buf) - cp->mIradius, cp->mFp,
                               cp->mIradius * 2 + 1, x1, x1 + len);
        out += len;
        x1 += len;
    }
#endif
    while(x2 > x1) {
        OneHU1(p, out, x1, buf, cp->mFp, cp->mIradius);
        out++;
        x1++;
    }
}

RsdCpuScriptIntrinsicBlur::RsdCpuScriptIntrinsicBlur(RsdCpuReferenceImpl *ctx,
                                                     const Script *s, const Element *e)
            : RsdCpuScriptIntrinsic(ctx, s, e, RS_SCRIPT_INTRINSIC_ID_BLUR) {

    mRootPtr = NULL;
    if (e->getType() == RS_TYPE_UNSIGNED_8) {
        switch (e->getVectorSize()) {
        case 1:
            mRootPtr = &kernelU1;
            break;
        case 4:
            mRootPtr = &kernelU4;
            break;
        }
    }
    rsAssert(mRootPtr);
    mRadius = 5;

    mScratch = new void *[mCtx->getThreadCount()];
    mScratchSize = new size_t[mCtx->getThreadCount()];

    ComputeGaussianWeights();
}

RsdCpuScriptIntrinsicBlur::~RsdCpuScriptIntrinsicBlur() {
    uint32_t threads = mCtx->getThreadCount();
    if (mScratch) {
        for (size_t i = 0; i < threads; i++) {
            if (mScratch[i]) {
                free(mScratch[i]);
            }
        }
        delete []mScratch;
    }
    if (mScratchSize) {
        delete []mScratchSize;
    }
}

void RsdCpuScriptIntrinsicBlur::populateScript(Script *s) {
    s->mHal.info.exportedVariableCount = 2;
}

void RsdCpuScriptIntrinsicBlur::invokeFreeChildren() {
    mAlloc.clear();
}


RsdCpuScriptImpl * rsdIntrinsic_Blur(RsdCpuReferenceImpl *ctx, const Script *s, const Element *e) {

    return new RsdCpuScriptIntrinsicBlur(ctx, s, e);
}


