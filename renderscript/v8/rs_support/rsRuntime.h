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
#include "rsScriptC.h"

#include "utils/Timers.h"

#include <time.h>

namespace android {
namespace renderscript {


//////////////////////////////////////////////////////////////////////////////
// Context
//////////////////////////////////////////////////////////////////////////////

void rsrAllocationSyncAll(Context *, Script *, Allocation *);

void rsrAllocationCopy1DRange(Context *, Allocation *dstAlloc,
                              uint32_t dstOff,
                              uint32_t dstMip,
                              uint32_t count,
                              Allocation *srcAlloc,
                              uint32_t srcOff, uint32_t srcMip);
void rsrAllocationCopy2DRange(Context *, Allocation *dstAlloc,
                              uint32_t dstXoff, uint32_t dstYoff,
                              uint32_t dstMip, uint32_t dstFace,
                              uint32_t width, uint32_t height,
                              Allocation *srcAlloc,
                              uint32_t srcXoff, uint32_t srcYoff,
                              uint32_t srcMip, uint32_t srcFace);

//////////////////////////////////////////////////////////////////////////////
// Time routines
//////////////////////////////////////////////////////////////////////////////

float rsrGetDt(Context *, Script *);
time_t rsrTime(Context *, Script *, time_t *timer);
tm* rsrLocalTime(Context *, Script *, tm *local, time_t *timer);
int64_t rsrUptimeMillis(Context *, Script *);
int64_t rsrUptimeNanos(Context *, Script *);

//////////////////////////////////////////////////////////////////////////////
// Message routines
//////////////////////////////////////////////////////////////////////////////

uint32_t rsrToClient(Context *, Script *, int cmdID, void *data, int len);
uint32_t rsrToClientBlocking(Context *, Script *, int cmdID, void *data, int len);

//////////////////////////////////////////////////////////////////////////////
//
//////////////////////////////////////////////////////////////////////////////

void rsrSetObject(const Context *, const Script *, ObjectBase **dst, ObjectBase * src);
void rsrClearObject(const Context *, const Script *, ObjectBase **dst);
bool rsrIsObject(const Context *, const Script *, const ObjectBase *src);

void rsrAllocationIncRefs(const Context *, const Allocation *, void *ptr,
                          size_t elementCount, size_t startOffset);
void rsrAllocationDecRefs(const Context *, const Allocation *, void *ptr,
                          size_t elementCount, size_t startOffset);


uint32_t rsrToClient(Context *, Script *, int cmdID, void *data, int len);
uint32_t rsrToClientBlocking(Context *, Script *, int cmdID, void *data, int len);

void rsrAllocationMarkDirty(Context *, Script *, RsAllocation a);
void rsrAllocationSyncAll(Context *, Script *, Allocation *a, RsAllocationUsageType source);


void rsrForEach(Context *, Script *, Script *target,
                Allocation *in,
                Allocation *out,
                const void *usr,
                 uint32_t usrBytes,
                const RsScriptCall *call);


//////////////////////////////////////////////////////////////////////////////
// Heavy math functions
//////////////////////////////////////////////////////////////////////////////


void rsrMatrixSet(rs_matrix4x4 *m, uint32_t row, uint32_t col, float v);
float rsrMatrixGet(const rs_matrix4x4 *m, uint32_t row, uint32_t col);
void rsrMatrixSet(rs_matrix3x3 *m, uint32_t row, uint32_t col, float v);
float rsrMatrixGet(const rs_matrix3x3 *m, uint32_t row, uint32_t col);
void rsrMatrixSet(rs_matrix2x2 *m, uint32_t row, uint32_t col, float v);
float rsrMatrixGet(const rs_matrix2x2 *m, uint32_t row, uint32_t col);
void rsrMatrixLoadIdentity_4x4(rs_matrix4x4 *m);
void rsrMatrixLoadIdentity_3x3(rs_matrix3x3 *m);
void rsrMatrixLoadIdentity_2x2(rs_matrix2x2 *m);
void rsrMatrixLoad_4x4_f(rs_matrix4x4 *m, const float *v);
void rsrMatrixLoad_3x3_f(rs_matrix3x3 *m, const float *v);
void rsrMatrixLoad_2x2_f(rs_matrix2x2 *m, const float *v);
void rsrMatrixLoad_4x4_4x4(rs_matrix4x4 *m, const rs_matrix4x4 *v);
void rsrMatrixLoad_4x4_3x3(rs_matrix4x4 *m, const rs_matrix3x3 *v);
void rsrMatrixLoad_4x4_2x2(rs_matrix4x4 *m, const rs_matrix2x2 *v);
void rsrMatrixLoad_3x3_3x3(rs_matrix3x3 *m, const rs_matrix3x3 *v);
void rsrMatrixLoad_2x2_2x2(rs_matrix2x2 *m, const rs_matrix2x2 *v);
void rsrMatrixLoadRotate(rs_matrix4x4 *m, float rot, float x, float y, float z);
void rsrMatrixLoadScale(rs_matrix4x4 *m, float x, float y, float z);
void rsrMatrixLoadTranslate(rs_matrix4x4 *m, float x, float y, float z);
void rsrMatrixLoadMultiply_4x4_4x4_4x4(rs_matrix4x4 *m, const rs_matrix4x4 *lhs,
                                       const rs_matrix4x4 *rhs);
void rsrMatrixMultiply_4x4_4x4(rs_matrix4x4 *m, const rs_matrix4x4 *rhs);
void rsrMatrixLoadMultiply_3x3_3x3_3x3(rs_matrix3x3 *m, const rs_matrix3x3 *lhs,
                                       const rs_matrix3x3 *rhs);
void rsrMatrixMultiply_3x3_3x3(rs_matrix3x3 *m, const rs_matrix3x3 *rhs);
void rsrMatrixLoadMultiply_2x2_2x2_2x2(rs_matrix2x2 *m, const rs_matrix2x2 *lhs,
                                       const rs_matrix2x2 *rhs);
void rsrMatrixMultiply_2x2_2x2(rs_matrix2x2 *m, const rs_matrix2x2 *rhs);
void rsrMatrixRotate(rs_matrix4x4 *m, float rot, float x, float y, float z);
void rsrMatrixScale(rs_matrix4x4 *m, float x, float y, float z);
void rsrMatrixTranslate(rs_matrix4x4 *m, float x, float y, float z);
void rsrMatrixLoadOrtho(rs_matrix4x4 *m, float left, float right,
                        float bottom, float top, float near, float far);
void rsrMatrixLoadFrustum(rs_matrix4x4 *m, float left, float right,
                          float bottom, float top, float near, float far);
void rsrMatrixLoadPerspective(rs_matrix4x4* m, float fovy, float aspect, float near, float far);

// Returns true if the matrix was successfully inversed
bool rsrMatrixInverse_4x4(rs_matrix4x4 *m);
// Returns true if the matrix was successfully inversed
bool rsrMatrixInverseTranspose_4x4(rs_matrix4x4 *m);

void rsrMatrixTranspose_4x4(rs_matrix4x4 *m);
void rsrMatrixTranspose_3x3(rs_matrix3x3 *m);
void rsrMatrixTranspose_2x2(rs_matrix2x2 *m);

}
}
