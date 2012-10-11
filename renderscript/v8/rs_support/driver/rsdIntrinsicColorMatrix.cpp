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
    bool use3x3;
    bool useDot;
};

static void ColorMatrix_SetVar(const Context *dc, const Script *script, void * intrinsicData,
                               uint32_t slot, void *data, size_t dataLength) {
    ConvolveParams *cp = (ConvolveParams *)intrinsicData;

    rsAssert(slot == 0);
    memcpy (cp->fp, data, dataLength);
    for(int ct=0; ct < 16; ct++) {
        cp->ip[ct] = (short)(cp->fp[ct] * 255.f + 0.5f);
    }

    if ((cp->ip[3] == 0) && (cp->ip[7] == 0) && (cp->ip[11] == 0) &&
        (cp->ip[12] == 0) && (cp->ip[13] == 0) && (cp->ip[14] == 0) &&
        (cp->ip[15] == 255)) {
        cp->use3x3 = true;

        if ((cp->ip[0] == cp->ip[1]) && (cp->ip[0] == cp->ip[2]) &&
            (cp->ip[4] == cp->ip[5]) && (cp->ip[4] == cp->ip[6]) &&
            (cp->ip[8] == cp->ip[9]) && (cp->ip[8] == cp->ip[10])) {
            cp->useDot = true;
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

static void ColorMatrix_uchar4(const RsForEachStubParamStruct *p,
                                    uint32_t xstart, uint32_t xend,
                                    uint32_t instep, uint32_t outstep) {
    ConvolveParams *cp = (ConvolveParams *)p->usr;
    uchar4 *out = (uchar4 *)p->out;
    uchar4 *in = (uchar4 *)p->in;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    in += xstart;
    out += xstart;

    if(x2 > x1) {
#if defined(ARCH_ARM_HAVE_NEON)
        int32_t len = (x2 - x1) >> 2;
        if(len > 0) {
            if (cp->use3x3) {
                if (cp->useDot) {
                    rsdIntrinsicColorMatrixDot_K(out, in, cp->ip, len);
                } else {
                    rsdIntrinsicColorMatrix3x3_K(out, in, cp->ip, len);
                }
            } else {
                rsdIntrinsicColorMatrix4x4_K(out, in, cp->ip, len);
            }
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

void * rsdIntrinsic_InitColorMatrix(const android::renderscript::Context *dc,
                                    android::renderscript::Script *script,
                                    RsdIntriniscFuncs_t *funcs) {

    script->mHal.info.exportedVariableCount = 1;
    funcs->setVar = ColorMatrix_SetVar;
    funcs->root = ColorMatrix_uchar4;

    ConvolveParams *cp = (ConvolveParams *)calloc(1, sizeof(ConvolveParams));
    cp->fp[0] = 1.f;
    cp->fp[5] = 1.f;
    cp->fp[10] = 1.f;
    cp->fp[15] = 1.f;
    for(int ct=0; ct < 16; ct++) {
        cp->ip[ct] = (short)(cp->fp[ct] * 255.f + 0.5f);
    }
    return cp;
}


