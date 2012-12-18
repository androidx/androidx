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


class RsdCpuScriptIntrinsicLUT : public RsdCpuScriptIntrinsic {
public:
    virtual void populateScript(Script *);
    virtual void invokeFreeChildren();

    virtual void setGlobalObj(uint32_t slot, ObjectBase *data);

    virtual ~RsdCpuScriptIntrinsicLUT();
    RsdCpuScriptIntrinsicLUT(RsdCpuReferenceImpl *ctx, const Script *s, const Element *e);

protected:
    ObjectBaseRef<Allocation> lut;

    static void kernel(const RsForEachStubParamStruct *p,
                       uint32_t xstart, uint32_t xend,
                       uint32_t instep, uint32_t outstep);
};

}
}


void RsdCpuScriptIntrinsicLUT::setGlobalObj(uint32_t slot, ObjectBase *data) {
    rsAssert(slot == 0);
    lut.set(static_cast<Allocation *>(data));
}


void RsdCpuScriptIntrinsicLUT::kernel(const RsForEachStubParamStruct *p,
                                      uint32_t xstart, uint32_t xend,
                                      uint32_t instep, uint32_t outstep) {
    RsdCpuScriptIntrinsicLUT *cp = (RsdCpuScriptIntrinsicLUT *)p->usr;

    uchar4 *out = (uchar4 *)p->out;
    uchar4 *in = (uchar4 *)p->in;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    const uchar *tr = (const uchar *)cp->lut->mHal.drvState.lod[0].mallocPtr;
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

RsdCpuScriptIntrinsicLUT::RsdCpuScriptIntrinsicLUT(RsdCpuReferenceImpl *ctx,
                                                   const Script *s, const Element *e)
            : RsdCpuScriptIntrinsic(ctx, s, e, RS_SCRIPT_INTRINSIC_ID_LUT) {

    mRootPtr = &kernel;
}

RsdCpuScriptIntrinsicLUT::~RsdCpuScriptIntrinsicLUT() {
}

void RsdCpuScriptIntrinsicLUT::populateScript(Script *s) {
    s->mHal.info.exportedVariableCount = 1;
}

void RsdCpuScriptIntrinsicLUT::invokeFreeChildren() {
    lut.clear();
}


RsdCpuScriptImpl * rsdIntrinsic_LUT(RsdCpuReferenceImpl *ctx,
                                    const Script *s, const Element *e) {

    return new RsdCpuScriptIntrinsicLUT(ctx, s, e);
}


