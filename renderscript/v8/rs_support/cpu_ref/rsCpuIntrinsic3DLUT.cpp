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


class RsdCpuScriptIntrinsic3DLUT : public RsdCpuScriptIntrinsic {
public:
    virtual void populateScript(Script *);
    virtual void invokeFreeChildren();

    virtual void setGlobalObj(uint32_t slot, ObjectBase *data);

    virtual ~RsdCpuScriptIntrinsic3DLUT();
    RsdCpuScriptIntrinsic3DLUT(RsdCpuReferenceImpl *ctx, const Script *s, const Element *e);

protected:
    ObjectBaseRef<Allocation> mLUT;

    static void kernel(const RsForEachStubParamStruct *p,
                       uint32_t xstart, uint32_t xend,
                       uint32_t instep, uint32_t outstep);
};

}
}


void RsdCpuScriptIntrinsic3DLUT::setGlobalObj(uint32_t slot, ObjectBase *data) {
    rsAssert(slot == 0);
    mLUT.set(static_cast<Allocation *>(data));
}

extern "C" void rsdIntrinsic3DLUT_K(void *dst, const void *src, const void *lut,
                                    size_t lut_stride_y, size_t lut_stride_z,
                                    uint32_t count, const void *constants);


void RsdCpuScriptIntrinsic3DLUT::kernel(const RsForEachStubParamStruct *p,
                                      uint32_t xstart, uint32_t xend,
                                      uint32_t instep, uint32_t outstep) {
// FIXME(srhines)!!!! Temporary WAR for non-neon arm crash in clang.
#if defined(ARCH_ARM_HAVE_NEON)
    RsdCpuScriptIntrinsic3DLUT *cp = (RsdCpuScriptIntrinsic3DLUT *)p->usr;

    uchar4 *out = (uchar4 *)p->out;
    uchar4 *in = (uchar4 *)p->in;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    const uchar *bp = (const uchar *)cp->mLUT->mHal.drvState.lod[0].mallocPtr;

    int4 dims = {
        cp->mLUT->mHal.drvState.lod[0].dimX,
        cp->mLUT->mHal.drvState.lod[0].dimY,
        cp->mLUT->mHal.drvState.lod[0].dimZ,
        0
    };
    const float4 m = (float4)(1.f / 255.f) * convert_float4(dims - 1);
    const int4 coordMul = convert_int4(m * (float4)0x8000);
    const size_t stride_y = cp->mLUT->mHal.drvState.lod[0].stride;
    const size_t stride_z = stride_y * cp->mLUT->mHal.drvState.lod[0].dimY;

    //ALOGE("strides %zu %zu", stride_y, stride_z);

    while (x1 < x2) {
#if defined(ARCH_ARM_HAVE_NEON)
        int32_t len = (x2 - x1 - 1) >> 1;
        if(len > 0) {
            const short neon_constants[] = {
                coordMul.x, coordMul.y, coordMul.z, 0,
                0, 0, 0, 0xffff,

            };

            rsdIntrinsic3DLUT_K(out, in, bp, stride_y, stride_z, len, neon_constants);
            x1 += len << 1;
            out += len << 1;
            in += len << 1;
        }

#endif

        int4 baseCoord = convert_int4(*in) * coordMul;
        int4 coord1 = baseCoord >> (int4)15;
        //int4 coord2 = min(coord1 + 1, gDims - 1);

        int4 weight2 = baseCoord & 0x7fff;
        int4 weight1 = (int4)0x8000 - weight2;

        //ALOGE("coord1      %08x %08x %08x %08x", coord1.x, coord1.y, coord1.z, coord1.w);
        const uchar *bp2 = bp + (coord1.x * 4) + (coord1.y * stride_y) + (coord1.z * stride_z);
        const uchar4 *pt_00 = (const uchar4 *)&bp2[0];
        const uchar4 *pt_10 = (const uchar4 *)&bp2[stride_y];
        const uchar4 *pt_01 = (const uchar4 *)&bp2[stride_z];
        const uchar4 *pt_11 = (const uchar4 *)&bp2[stride_y + stride_z];

        uint4 v000 = convert_uint4(pt_00[0]);
        uint4 v100 = convert_uint4(pt_00[1]);
        uint4 v010 = convert_uint4(pt_10[0]);
        uint4 v110 = convert_uint4(pt_10[1]);
        uint4 v001 = convert_uint4(pt_01[0]);
        uint4 v101 = convert_uint4(pt_01[1]);
        uint4 v011 = convert_uint4(pt_11[0]);
        uint4 v111 = convert_uint4(pt_11[1]);

        uint4 yz00 = ((v000 * weight1.x) + (v100 * weight2.x)) >> (int4)7;
        uint4 yz10 = ((v010 * weight1.x) + (v110 * weight2.x)) >> (int4)7;
        uint4 yz01 = ((v001 * weight1.x) + (v101 * weight2.x)) >> (int4)7;
        uint4 yz11 = ((v011 * weight1.x) + (v111 * weight2.x)) >> (int4)7;

        uint4 z0 = ((yz00 * weight1.y) + (yz10 * weight2.y)) >> (int4)15;
        uint4 z1 = ((yz01 * weight1.y) + (yz11 * weight2.y)) >> (int4)15;

        uint4 v = ((z0 * weight1.z) + (z1 * weight2.z)) >> (int4)15;
        uint4 v2 = (v + 0x7f) >> (int4)8;

        uchar4 ret = convert_uchar4(v2);
        ret.a = in->a;

        #if 0
        if (!x1) {
            ALOGE("in          %08x %08x %08x %08x", in->r, in->g, in->b, in->a);
            ALOGE("baseCoord   %08x %08x %08x %08x", baseCoord.x, baseCoord.y, baseCoord.z, baseCoord.w);
            ALOGE("coord1      %08x %08x %08x %08x", coord1.x, coord1.y, coord1.z, coord1.w);
            ALOGE("weight1     %08x %08x %08x %08x", weight1.x, weight1.y, weight1.z, weight1.w);
            ALOGE("weight2     %08x %08x %08x %08x", weight2.x, weight2.y, weight2.z, weight2.w);

            ALOGE("v000        %08x %08x %08x %08x", v000.x, v000.y, v000.z, v000.w);
            ALOGE("v100        %08x %08x %08x %08x", v100.x, v100.y, v100.z, v100.w);
            ALOGE("yz00        %08x %08x %08x %08x", yz00.x, yz00.y, yz00.z, yz00.w);
            ALOGE("z0          %08x %08x %08x %08x", z0.x, z0.y, z0.z, z0.w);

            ALOGE("v           %08x %08x %08x %08x", v.x, v.y, v.z, v.w);
            ALOGE("v2          %08x %08x %08x %08x", v2.x, v2.y, v2.z, v2.w);
        }
        #endif
        *out = ret;


        in++;
        out++;
        x1++;
    }
#endif
}

RsdCpuScriptIntrinsic3DLUT::RsdCpuScriptIntrinsic3DLUT(RsdCpuReferenceImpl *ctx,
                                                     const Script *s, const Element *e)
            : RsdCpuScriptIntrinsic(ctx, s, e, RS_SCRIPT_INTRINSIC_ID_3DLUT) {

    mRootPtr = &kernel;
}

RsdCpuScriptIntrinsic3DLUT::~RsdCpuScriptIntrinsic3DLUT() {
}

void RsdCpuScriptIntrinsic3DLUT::populateScript(Script *s) {
    s->mHal.info.exportedVariableCount = 1;
}

void RsdCpuScriptIntrinsic3DLUT::invokeFreeChildren() {
    mLUT.clear();
}


RsdCpuScriptImpl * rsdIntrinsic_3DLUT(RsdCpuReferenceImpl *ctx,
                                    const Script *s, const Element *e) {

    return new RsdCpuScriptIntrinsic3DLUT(ctx, s, e);
}


