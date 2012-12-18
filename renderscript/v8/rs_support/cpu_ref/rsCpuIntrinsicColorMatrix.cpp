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


class RsdCpuScriptIntrinsicColorMatrix : public RsdCpuScriptIntrinsic {
public:
    virtual void populateScript(Script *);

    virtual void setGlobalVar(uint32_t slot, const void *data, size_t dataLength);

    virtual ~RsdCpuScriptIntrinsicColorMatrix();
    RsdCpuScriptIntrinsicColorMatrix(RsdCpuReferenceImpl *ctx, const Script *s, const Element *e);

protected:
    float fp[16];
    short ip[16];

    static void kernel4x4(const RsForEachStubParamStruct *p,
                          uint32_t xstart, uint32_t xend,
                          uint32_t instep, uint32_t outstep);
    static void kernel3x3(const RsForEachStubParamStruct *p,
                          uint32_t xstart, uint32_t xend,
                          uint32_t instep, uint32_t outstep);
    static void kernelDot(const RsForEachStubParamStruct *p,
                          uint32_t xstart, uint32_t xend,
                          uint32_t instep, uint32_t outstep);
};

}
}


void RsdCpuScriptIntrinsicColorMatrix::setGlobalVar(uint32_t slot, const void *data,
                                                    size_t dataLength) {
    rsAssert(slot == 0);
    memcpy (fp, data, dataLength);
    for(int ct=0; ct < 16; ct++) {
        ip[ct] = (short)(fp[ct] * 255.f + 0.5f);
    }

    mRootPtr = &kernel4x4;
    if ((ip[3] == 0) && (ip[7] == 0) && (ip[11] == 0) &&
        (ip[12] == 0) && (ip[13] == 0) && (ip[14] == 0) && (ip[15] == 255)) {
        mRootPtr = &kernel3x3;

        if ((ip[0] == ip[1]) && (ip[0] == ip[2]) &&
            (ip[4] == ip[5]) && (ip[4] == ip[6]) &&
            (ip[8] == ip[9]) && (ip[8] == ip[10])) {
            mRootPtr = &kernelDot;
        }
    }
}

extern "C" void rsdIntrinsicColorMatrix4x4_K(void *dst, const void *src, const short *coef, uint32_t count);
extern "C" void rsdIntrinsicColorMatrix3x3_K(void *dst, const void *src, const short *coef, uint32_t count);
extern "C" void rsdIntrinsicColorMatrixDot_K(void *dst, const void *src, const short *coef, uint32_t count);

static void One(const RsForEachStubParamStruct *p, uchar4 *out,
                const uchar4 *py, const float* coeff) {
    float4 i = convert_float4(py[0]);

    float4 sum;
    sum.x = i.x * coeff[0] +
            i.y * coeff[4] +
            i.z * coeff[8] +
            i.w * coeff[12];
    sum.y = i.x * coeff[1] +
            i.y * coeff[5] +
            i.z * coeff[9] +
            i.w * coeff[13];
    sum.z = i.x * coeff[2] +
            i.y * coeff[6] +
            i.z * coeff[10] +
            i.w * coeff[14];
    sum.w = i.x * coeff[3] +
            i.y * coeff[7] +
            i.z * coeff[11] +
            i.w * coeff[15];

    sum.x = sum.x < 0 ? 0 : (sum.x > 255 ? 255 : sum.x);
    sum.y = sum.y < 0 ? 0 : (sum.y > 255 ? 255 : sum.y);
    sum.z = sum.z < 0 ? 0 : (sum.z > 255 ? 255 : sum.z);
    sum.w = sum.w < 0 ? 0 : (sum.w > 255 ? 255 : sum.w);

    *out = convert_uchar4(sum);
}

void RsdCpuScriptIntrinsicColorMatrix::kernel4x4(const RsForEachStubParamStruct *p,
                                                 uint32_t xstart, uint32_t xend,
                                                 uint32_t instep, uint32_t outstep) {
    RsdCpuScriptIntrinsicColorMatrix *cp = (RsdCpuScriptIntrinsicColorMatrix *)p->usr;
    uchar4 *out = (uchar4 *)p->out;
    uchar4 *in = (uchar4 *)p->in;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    if(x2 > x1) {
#if defined(ARCH_ARM_HAVE_NEON)
        int32_t len = (x2 - x1) >> 2;
        if(len > 0) {
            rsdIntrinsicColorMatrix4x4_K(out, in, cp->ip, len);
            x1 += len << 2;
            out += len << 2;
            in += len << 2;
        }
#endif

        while(x1 != x2) {
            One(p, out++, in++, cp->fp);
            x1++;
        }
    }
}

void RsdCpuScriptIntrinsicColorMatrix::kernel3x3(const RsForEachStubParamStruct *p,
                                                 uint32_t xstart, uint32_t xend,
                                                 uint32_t instep, uint32_t outstep) {
    RsdCpuScriptIntrinsicColorMatrix *cp = (RsdCpuScriptIntrinsicColorMatrix *)p->usr;
    uchar4 *out = (uchar4 *)p->out;
    uchar4 *in = (uchar4 *)p->in;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    if(x2 > x1) {
#if defined(ARCH_ARM_HAVE_NEON)
        int32_t len = (x2 - x1) >> 2;
        if(len > 0) {
            rsdIntrinsicColorMatrix3x3_K(out, in, cp->ip, len);
            x1 += len << 2;
            out += len << 2;
            in += len << 2;
        }
#endif

        while(x1 != x2) {
            One(p, out++, in++, cp->fp);
            x1++;
        }
    }
}

void RsdCpuScriptIntrinsicColorMatrix::kernelDot(const RsForEachStubParamStruct *p,
                                                 uint32_t xstart, uint32_t xend,
                                                 uint32_t instep, uint32_t outstep) {
    RsdCpuScriptIntrinsicColorMatrix *cp = (RsdCpuScriptIntrinsicColorMatrix *)p->usr;
    uchar4 *out = (uchar4 *)p->out;
    uchar4 *in = (uchar4 *)p->in;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    if(x2 > x1) {
#if defined(ARCH_ARM_HAVE_NEON)
        int32_t len = (x2 - x1) >> 2;
        if(len > 0) {
            rsdIntrinsicColorMatrixDot_K(out, in, cp->ip, len);
            x1 += len << 2;
            out += len << 2;
            in += len << 2;
        }
#endif

        while(x1 != x2) {
            One(p, out++, in++, cp->fp);
            x1++;
        }
    }
}


RsdCpuScriptIntrinsicColorMatrix::RsdCpuScriptIntrinsicColorMatrix(
            RsdCpuReferenceImpl *ctx, const Script *s, const Element *e)
            : RsdCpuScriptIntrinsic(ctx, s, e, RS_SCRIPT_INTRINSIC_ID_COLOR_MATRIX) {

    const static float defaultMatrix[] = {
        1.f, 0.f, 0.f, 0.f,
        0.f, 1.f, 0.f, 0.f,
        0.f, 0.f, 1.f, 0.f,
        0.f, 0.f, 0.f, 1.f
    };
    setGlobalVar(0, defaultMatrix, sizeof(defaultMatrix));
}

RsdCpuScriptIntrinsicColorMatrix::~RsdCpuScriptIntrinsicColorMatrix() {
}

void RsdCpuScriptIntrinsicColorMatrix::populateScript(Script *s) {
    s->mHal.info.exportedVariableCount = 1;
}

RsdCpuScriptImpl * rsdIntrinsic_ColorMatrix(RsdCpuReferenceImpl *ctx,
                                            const Script *s, const Element *e) {

    return new RsdCpuScriptIntrinsicColorMatrix(ctx, s, e);
}



