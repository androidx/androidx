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

#ifndef RS_COMPATIBILITY_LIB
void rsrBindTexture(Context *, ProgramFragment *, uint32_t slot, Allocation *);
void rsrBindConstant(Context *, ProgramFragment *, uint32_t slot, Allocation *);
void rsrBindConstant(Context *, ProgramVertex*, uint32_t slot, Allocation *);
void rsrBindSampler(Context *, ProgramFragment *, uint32_t slot, Sampler *);
void rsrBindProgramStore(Context *, ProgramStore *);
void rsrBindProgramFragment(Context *, ProgramFragment *);
void rsrBindProgramVertex(Context *, ProgramVertex *);
void rsrBindProgramRaster(Context *, ProgramRaster *);
void rsrBindFrameBufferObjectColorTarget(Context *, Allocation *, uint32_t slot);
void rsrBindFrameBufferObjectDepthTarget(Context *, Allocation *);
void rsrClearFrameBufferObjectColorTarget(Context *, uint32_t slot);
void rsrClearFrameBufferObjectDepthTarget(Context *);
void rsrClearFrameBufferObjectTargets(Context *);

//////////////////////////////////////////////////////////////////////////////
// VP
//////////////////////////////////////////////////////////////////////////////

void rsrVpLoadProjectionMatrix(Context *, const rsc_Matrix *m);
void rsrVpLoadModelMatrix(Context *, const rsc_Matrix *m);
void rsrVpLoadTextureMatrix(Context *, const rsc_Matrix *m);
void rsrPfConstantColor(Context *, ProgramFragment *, float r, float g, float b, float a);
void rsrVpGetProjectionMatrix(Context *, rsc_Matrix *m);

//////////////////////////////////////////////////////////////////////////////
// Drawing
//////////////////////////////////////////////////////////////////////////////

void rsrDrawPath(Context *, Path *);
void rsrDrawMesh(Context *, Mesh *);
void rsrDrawMeshPrimitive(Context *, Mesh *, uint32_t primIndex);
void rsrDrawMeshPrimitiveRange(Context *, Mesh *,
                               uint32_t primIndex, uint32_t start, uint32_t len);
void rsrMeshComputeBoundingBox(Context *, Mesh *,
                               float *minX, float *minY, float *minZ,
                               float *maxX, float *maxY, float *maxZ);


//////////////////////////////////////////////////////////////////////////////
//
//////////////////////////////////////////////////////////////////////////////


void rsrColor(Context *, float r, float g, float b, float a);
#endif

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

#ifndef RS_COMPATIBILITY_LIB
void rsrPrepareClear(Context *);
uint32_t rsrGetWidth(Context *);
uint32_t rsrGetHeight(Context *);
void rsrDrawTextAlloc(Context *, Allocation *, int x, int y);
void rsrDrawText(Context *, const char *text, int x, int y);
void rsrSetMetrics(Context *, Font::Rect *metrics,
                   int32_t *left, int32_t *right, int32_t *top, int32_t *bottom);
void rsrMeasureTextAlloc(Context *, Allocation *,
                         int32_t *left, int32_t *right, int32_t *top, int32_t *bottom);
void rsrMeasureText(Context *, const char *text,
                    int32_t *left, int32_t *right, int32_t *top, int32_t *bottom);
void rsrBindFont(Context *, Font *);
void rsrFontColor(Context *, float r, float g, float b, float a);
#endif

//////////////////////////////////////////////////////////////////////////////
// Time routines
//////////////////////////////////////////////////////////////////////////////

float rsrGetDt(Context *, const Script *sc);
time_t rsrTime(Context *, time_t *timer);
tm* rsrLocalTime(Context *, tm *local, time_t *timer);
int64_t rsrUptimeMillis(Context *);
int64_t rsrUptimeNanos(Context *);

//////////////////////////////////////////////////////////////////////////////
// Message routines
//////////////////////////////////////////////////////////////////////////////

uint32_t rsrToClient(Context *, int cmdID, void *data, int len);
uint32_t rsrToClientBlocking(Context *, int cmdID, void *data, int len);

//////////////////////////////////////////////////////////////////////////////
//
//////////////////////////////////////////////////////////////////////////////

void rsrSetObject(const Context *, ObjectBase **dst, ObjectBase * src);
void rsrClearObject(const Context *, ObjectBase **dst);
bool rsrIsObject(const Context *, const ObjectBase *src);

void rsrAllocationIncRefs(const Context *, const Allocation *, void *ptr,
                          size_t elementCount, size_t startOffset);
void rsrAllocationDecRefs(const Context *, const Allocation *, void *ptr,
                          size_t elementCount, size_t startOffset);


void rsrAllocationSyncAll(Context *, Allocation *a, RsAllocationUsageType source);


void rsrForEach(Context *, Script *target,
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
