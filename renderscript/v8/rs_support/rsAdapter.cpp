
/*
 * Copyright (C) 2009 The Android Open Source Project
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

#include "rsContext.h"
#include "rsAdapter.h"

using namespace android;
using namespace android::renderscript;

Adapter1D::Adapter1D(Context *rsc) : ObjectBase(rsc) {
    reset();
}

Adapter1D::Adapter1D(Context *rsc, Allocation *a) : ObjectBase(rsc) {
    reset();
    setAllocation(a);
}

void Adapter1D::reset() {
    mY = 0;
    mZ = 0;
    mLOD = 0;
    mFace = RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X;
}

void Adapter1D::data(Context *rsc, uint32_t x, uint32_t count, const void *data, size_t sizeBytes) {
    mAllocation->data(rsc, x, mY, mLOD, mFace, count, 1, data, sizeBytes, 0);
}

void Adapter1D::serialize(Context *rsc, OStream *stream) const {
}

Adapter1D *Adapter1D::createFromStream(Context *rsc, IStream *stream) {
    return NULL;
}

namespace android {
namespace renderscript {

RsAdapter1D rsi_Adapter1DCreate(Context *rsc) {
    Adapter1D *a = new Adapter1D(rsc);
    a->incUserRef();
    return a;
}

void rsi_Adapter1DBindAllocation(Context *rsc, RsAdapter1D va, RsAllocation valloc) {
    Adapter1D * a = static_cast<Adapter1D *>(va);
    Allocation * alloc = static_cast<Allocation *>(valloc);
    a->setAllocation(alloc);
}

void rsi_Adapter1DSetConstraint(Context *rsc, RsAdapter1D va, RsDimension dim, uint32_t value) {
    Adapter1D * a = static_cast<Adapter1D *>(va);
    switch (dim) {
    case RS_DIMENSION_X:
        rsAssert(!"Cannot contrain X in an 1D adapter");
        return;
    case RS_DIMENSION_Y:
        a->setY(value);
        break;
    case RS_DIMENSION_Z:
        a->setZ(value);
        break;
    case RS_DIMENSION_LOD:
        a->setLOD(value);
        break;
    case RS_DIMENSION_FACE:
        a->setFace((RsAllocationCubemapFace)value);
        break;
    default:
        rsAssert(!"Unimplemented constraint");
        return;
    }
}

}
}

//////////////////////////

Adapter2D::Adapter2D(Context *rsc) : ObjectBase(rsc) {
    reset();
}

Adapter2D::Adapter2D(Context *rsc, Allocation *a) : ObjectBase(rsc) {
    reset();
    setAllocation(a);
}

void Adapter2D::reset() {
    mZ = 0;
    mLOD = 0;
    mFace = RS_ALLOCATION_CUBEMAP_FACE_POSITIVE_X;
}


void Adapter2D::data(Context *rsc, uint32_t x, uint32_t y, uint32_t w, uint32_t h,
                     const void *data, size_t sizeBytes) {
    mAllocation->data(rsc, x, y, mLOD, mFace, w, h, data, sizeBytes, 0);
}


void Adapter2D::serialize(Context *rsc, OStream *stream) const {
}

Adapter2D *Adapter2D::createFromStream(Context *rsc, IStream *stream) {
    return NULL;
}


namespace android {
namespace renderscript {

RsAdapter2D rsi_Adapter2DCreate(Context *rsc) {
    Adapter2D *a = new Adapter2D(rsc);
    a->incUserRef();
    return a;
}

void rsi_Adapter2DBindAllocation(Context *rsc, RsAdapter2D va, RsAllocation valloc) {
    Adapter2D * a = static_cast<Adapter2D *>(va);
    Allocation * alloc = static_cast<Allocation *>(valloc);
    a->setAllocation(alloc);
}

void rsi_Adapter2DSetConstraint(Context *rsc, RsAdapter2D va, RsDimension dim, uint32_t value) {
    Adapter2D * a = static_cast<Adapter2D *>(va);
    switch (dim) {
    case RS_DIMENSION_X:
        rsAssert(!"Cannot contrain X in an 2D adapter");
        return;
    case RS_DIMENSION_Y:
        rsAssert(!"Cannot contrain Y in an 2D adapter");
        break;
    case RS_DIMENSION_Z:
        a->setZ(value);
        break;
    case RS_DIMENSION_LOD:
        a->setLOD(value);
        break;
    case RS_DIMENSION_FACE:
        a->setFace((RsAllocationCubemapFace)value);
        break;
    default:
        rsAssert(!"Unimplemented constraint");
        return;
    }
}


}
}
