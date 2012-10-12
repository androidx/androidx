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
    ObjectBaseRef<Allocation> lut;
};

static void LUT_Bind(const Context *dc, const Script *script,
                             void * intrinsicData, uint32_t slot, Allocation *data) {
    ConvolveParams *cp = (ConvolveParams *)intrinsicData;
    rsAssert(slot == 0);
    cp->lut.set(data);
}

static void LUT_uchar4(const RsForEachStubParamStruct *p,
                                    uint32_t xstart, uint32_t xend,
                                    uint32_t instep, uint32_t outstep) {
    ConvolveParams *cp = (ConvolveParams *)p->usr;
    uchar4 *out = (uchar4 *)p->out;
    uchar4 *in = (uchar4 *)p->in;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    in += xstart;
    out += xstart;

    DrvAllocation *din = (DrvAllocation *)cp->lut->mHal.drv;
    const uchar *tr = (const uchar *)din->lod[0].mallocPtr;
    const uchar *tg = &tr[256];
    const uchar *tb = &tg[256];
    const uchar *ta = &tb[256];

    while (x1 < x2) {
        uchar4 p = *in;
        uchar4 o = {tr[p.x], tg[p.y], tb[p.z], ta[p.w]};
        *out = o;
        in++;
        out++;
        x1++;
    }
}

void * rsdIntrinsic_InitLUT(const android::renderscript::Context *dc,
                                    android::renderscript::Script *script,
                                    RsdIntriniscFuncs_t *funcs) {

    script->mHal.info.exportedVariableCount = 1;
    funcs->bind = LUT_Bind;
    funcs->root = LUT_uchar4;
    ConvolveParams *cp = (ConvolveParams *)calloc(1, sizeof(ConvolveParams));
    return cp;
}


