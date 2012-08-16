/*
 * Copyright (C) 2011 The Android Open Source Project
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
#include "rsMatrix4x4.h"
#include "rsMatrix3x3.h"
#include "rsMatrix2x2.h"
#include "rsRuntime.h"

#include "utils/Timers.h"
#include "rsdCore.h"
#include "rsdBcc.h"

#include "rsdRuntime.h"
#include "rsdPath.h"
#include "rsdAllocation.h"

#include <time.h>

using namespace android;
using namespace android::renderscript;

#define GET_TLS()  ScriptTLSStruct * tls = \
    (ScriptTLSStruct *)pthread_getspecific(rsdgThreadTLSKey); \
    Context * rsc = tls->mContext; \
    ScriptC * sc = (ScriptC *) tls->mScript

typedef float float2 __attribute__((ext_vector_type(2)));
typedef float float3 __attribute__((ext_vector_type(3)));
typedef float float4 __attribute__((ext_vector_type(4)));
typedef char char2 __attribute__((ext_vector_type(2)));
typedef char char3 __attribute__((ext_vector_type(3)));
typedef char char4 __attribute__((ext_vector_type(4)));
typedef unsigned char uchar2 __attribute__((ext_vector_type(2)));
typedef unsigned char uchar3 __attribute__((ext_vector_type(3)));
typedef unsigned char uchar4 __attribute__((ext_vector_type(4)));
typedef short short2 __attribute__((ext_vector_type(2)));
typedef short short3 __attribute__((ext_vector_type(3)));
typedef short short4 __attribute__((ext_vector_type(4)));
typedef unsigned short ushort2 __attribute__((ext_vector_type(2)));
typedef unsigned short ushort3 __attribute__((ext_vector_type(3)));
typedef unsigned short ushort4 __attribute__((ext_vector_type(4)));
typedef int32_t int2 __attribute__((ext_vector_type(2)));
typedef int32_t int3 __attribute__((ext_vector_type(3)));
typedef int32_t int4 __attribute__((ext_vector_type(4)));
typedef uint32_t uint2 __attribute__((ext_vector_type(2)));
typedef uint32_t uint3 __attribute__((ext_vector_type(3)));
typedef uint32_t uint4 __attribute__((ext_vector_type(4)));
typedef long long long2 __attribute__((ext_vector_type(2)));
typedef long long long3 __attribute__((ext_vector_type(3)));
typedef long long long4 __attribute__((ext_vector_type(4)));
typedef unsigned long long ulong2 __attribute__((ext_vector_type(2)));
typedef unsigned long long ulong3 __attribute__((ext_vector_type(3)));
typedef unsigned long long ulong4 __attribute__((ext_vector_type(4)));


//////////////////////////////////////////////////////////////////////////////
// Allocation
//////////////////////////////////////////////////////////////////////////////


static void SC_AllocationSyncAll2(Allocation *a, RsAllocationUsageType source) {
    GET_TLS();
    rsrAllocationSyncAll(rsc, sc, a, source);
}

static void SC_AllocationSyncAll(Allocation *a) {
    GET_TLS();
    rsrAllocationSyncAll(rsc, sc, a, RS_ALLOCATION_USAGE_SCRIPT);
}

static void SC_AllocationCopy1DRange(Allocation *dstAlloc,
                                     uint32_t dstOff,
                                     uint32_t dstMip,
                                     uint32_t count,
                                     Allocation *srcAlloc,
                                     uint32_t srcOff, uint32_t srcMip) {
    GET_TLS();
    rsrAllocationCopy1DRange(rsc, dstAlloc, dstOff, dstMip, count,
                             srcAlloc, srcOff, srcMip);
}

static void SC_AllocationCopy2DRange(Allocation *dstAlloc,
                                     uint32_t dstXoff, uint32_t dstYoff,
                                     uint32_t dstMip, uint32_t dstFace,
                                     uint32_t width, uint32_t height,
                                     Allocation *srcAlloc,
                                     uint32_t srcXoff, uint32_t srcYoff,
                                     uint32_t srcMip, uint32_t srcFace) {
    GET_TLS();
    rsrAllocationCopy2DRange(rsc, dstAlloc,
                             dstXoff, dstYoff, dstMip, dstFace,
                             width, height,
                             srcAlloc,
                             srcXoff, srcYoff, srcMip, srcFace);
}

static void SC_AllocationIoSend(Allocation *alloc) {
    GET_TLS();
    rsdAllocationIoSend(rsc, alloc);
}


static void SC_AllocationIoReceive(Allocation *alloc) {
    GET_TLS();
    rsdAllocationIoReceive(rsc, alloc);
}



//////////////////////////////////////////////////////////////////////////////
// Context
//////////////////////////////////////////////////////////////////////////////

static void SC_BindTexture(ProgramFragment *pf, uint32_t slot, Allocation *a) {
    GET_TLS();
    rsrBindTexture(rsc, sc, pf, slot, a);
}

static void SC_BindVertexConstant(ProgramVertex *pv, uint32_t slot, Allocation *a) {
    GET_TLS();
    rsrBindConstant(rsc, sc, pv, slot, a);
}

static void SC_BindFragmentConstant(ProgramFragment *pf, uint32_t slot, Allocation *a) {
    GET_TLS();
    rsrBindConstant(rsc, sc, pf, slot, a);
}

static void SC_BindSampler(ProgramFragment *pf, uint32_t slot, Sampler *s) {
    GET_TLS();
    rsrBindSampler(rsc, sc, pf, slot, s);
}

static void SC_BindProgramStore(ProgramStore *ps) {
    GET_TLS();
    rsrBindProgramStore(rsc, sc, ps);
}

static void SC_BindProgramFragment(ProgramFragment *pf) {
    GET_TLS();
    rsrBindProgramFragment(rsc, sc, pf);
}

static void SC_BindProgramVertex(ProgramVertex *pv) {
    GET_TLS();
    rsrBindProgramVertex(rsc, sc, pv);
}

static void SC_BindProgramRaster(ProgramRaster *pr) {
    GET_TLS();
    rsrBindProgramRaster(rsc, sc, pr);
}

static void SC_BindFrameBufferObjectColorTarget(Allocation *a, uint32_t slot) {
    GET_TLS();
    rsrBindFrameBufferObjectColorTarget(rsc, sc, a, slot);
}

static void SC_BindFrameBufferObjectDepthTarget(Allocation *a) {
    GET_TLS();
    rsrBindFrameBufferObjectDepthTarget(rsc, sc, a);
}

static void SC_ClearFrameBufferObjectColorTarget(uint32_t slot) {
    GET_TLS();
    rsrClearFrameBufferObjectColorTarget(rsc, sc, slot);
}

static void SC_ClearFrameBufferObjectDepthTarget(Context *, Script *) {
    GET_TLS();
    rsrClearFrameBufferObjectDepthTarget(rsc, sc);
}

static void SC_ClearFrameBufferObjectTargets(Context *, Script *) {
    GET_TLS();
    rsrClearFrameBufferObjectTargets(rsc, sc);
}


//////////////////////////////////////////////////////////////////////////////
// VP
//////////////////////////////////////////////////////////////////////////////

static void SC_VpLoadProjectionMatrix(const rsc_Matrix *m) {
    GET_TLS();
    rsrVpLoadProjectionMatrix(rsc, sc, m);
}

static void SC_VpLoadModelMatrix(const rsc_Matrix *m) {
    GET_TLS();
    rsrVpLoadModelMatrix(rsc, sc, m);
}

static void SC_VpLoadTextureMatrix(const rsc_Matrix *m) {
    GET_TLS();
    rsrVpLoadTextureMatrix(rsc, sc, m);
}

static void SC_PfConstantColor(ProgramFragment *pf, float r, float g, float b, float a) {
    GET_TLS();
    rsrPfConstantColor(rsc, sc, pf, r, g, b, a);
}

static void SC_VpGetProjectionMatrix(rsc_Matrix *m) {
    GET_TLS();
    rsrVpGetProjectionMatrix(rsc, sc, m);
}


//////////////////////////////////////////////////////////////////////////////
// Drawing
//////////////////////////////////////////////////////////////////////////////

static void SC_DrawQuadTexCoords(float x1, float y1, float z1, float u1, float v1,
                                 float x2, float y2, float z2, float u2, float v2,
                                 float x3, float y3, float z3, float u3, float v3,
                                 float x4, float y4, float z4, float u4, float v4) {
    GET_TLS();
    rsrDrawQuadTexCoords(rsc, sc,
                         x1, y1, z1, u1, v1,
                         x2, y2, z2, u2, v2,
                         x3, y3, z3, u3, v3,
                         x4, y4, z4, u4, v4);
}

static void SC_DrawQuad(float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        float x4, float y4, float z4) {
    GET_TLS();
    rsrDrawQuad(rsc, sc, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
}

static void SC_DrawSpriteScreenspace(float x, float y, float z, float w, float h) {
    GET_TLS();
    rsrDrawSpriteScreenspace(rsc, sc, x, y, z, w, h);
}

static void SC_DrawRect(float x1, float y1, float x2, float y2, float z) {
    GET_TLS();
    rsrDrawRect(rsc, sc, x1, y1, x2, y2, z);
}

static void SC_DrawPath(Path *p) {
    GET_TLS();
    //rsrDrawPath(rsc, sc, p);
    rsdPathDraw(rsc, p);
}

static void SC_DrawMesh(Mesh *m) {
    GET_TLS();
    rsrDrawMesh(rsc, sc, m);
}

static void SC_DrawMeshPrimitive(Mesh *m, uint32_t primIndex) {
    GET_TLS();
    rsrDrawMeshPrimitive(rsc, sc, m, primIndex);
}

static void SC_DrawMeshPrimitiveRange(Mesh *m, uint32_t primIndex, uint32_t start, uint32_t len) {
    GET_TLS();
    rsrDrawMeshPrimitiveRange(rsc, sc, m, primIndex, start, len);
}

static void SC_MeshComputeBoundingBox(Mesh *m,
                               float *minX, float *minY, float *minZ,
                               float *maxX, float *maxY, float *maxZ) {
    GET_TLS();
    rsrMeshComputeBoundingBox(rsc, sc, m, minX, minY, minZ, maxX, maxY, maxZ);
}



//////////////////////////////////////////////////////////////////////////////
//
//////////////////////////////////////////////////////////////////////////////


static void SC_Color(float r, float g, float b, float a) {
    GET_TLS();
    rsrColor(rsc, sc, r, g, b, a);
}

static void SC_Finish() {
    GET_TLS();
    rsdGLFinish(rsc);
}

static void SC_ClearColor(float r, float g, float b, float a) {
    GET_TLS();
    rsrPrepareClear(rsc, sc);
    rsdGLClearColor(rsc, r, g, b, a);
}

static void SC_ClearDepth(float v) {
    GET_TLS();
    rsrPrepareClear(rsc, sc);
    rsdGLClearDepth(rsc, v);
}

static uint32_t SC_GetWidth() {
    GET_TLS();
    return rsrGetWidth(rsc, sc);
}

static uint32_t SC_GetHeight() {
    GET_TLS();
    return rsrGetHeight(rsc, sc);
}

static void SC_DrawTextAlloc(Allocation *a, int x, int y) {
    GET_TLS();
    rsrDrawTextAlloc(rsc, sc, a, x, y);
}

static void SC_DrawText(const char *text, int x, int y) {
    GET_TLS();
    rsrDrawText(rsc, sc, text, x, y);
}

static void SC_MeasureTextAlloc(Allocation *a,
                         int32_t *left, int32_t *right, int32_t *top, int32_t *bottom) {
    GET_TLS();
    rsrMeasureTextAlloc(rsc, sc, a, left, right, top, bottom);
}

static void SC_MeasureText(const char *text,
                    int32_t *left, int32_t *right, int32_t *top, int32_t *bottom) {
    GET_TLS();
    rsrMeasureText(rsc, sc, text, left, right, top, bottom);
}

static void SC_BindFont(Font *f) {
    GET_TLS();
    rsrBindFont(rsc, sc, f);
}

static void SC_FontColor(float r, float g, float b, float a) {
    GET_TLS();
    rsrFontColor(rsc, sc, r, g, b, a);
}



//////////////////////////////////////////////////////////////////////////////
//
//////////////////////////////////////////////////////////////////////////////

static void SC_SetObject(ObjectBase **dst, ObjectBase * src) {
    GET_TLS();
    rsrSetObject(rsc, sc, dst, src);
}

static void SC_ClearObject(ObjectBase **dst) {
    GET_TLS();
    rsrClearObject(rsc, sc, dst);
}

static bool SC_IsObject(const ObjectBase *src) {
    GET_TLS();
    return rsrIsObject(rsc, sc, src);
}




static const Allocation * SC_GetAllocation(const void *ptr) {
    GET_TLS();
    return rsdScriptGetAllocationForPointer(rsc, sc, ptr);
}

static void SC_ForEach_SAA(Script *target,
                            Allocation *in,
                            Allocation *out) {
    GET_TLS();
    rsrForEach(rsc, sc, target, in, out, NULL, 0, NULL);
}

static void SC_ForEach_SAAU(Script *target,
                            Allocation *in,
                            Allocation *out,
                            const void *usr) {
    GET_TLS();
    rsrForEach(rsc, sc, target, in, out, usr, 0, NULL);
}

static void SC_ForEach_SAAUS(Script *target,
                             Allocation *in,
                             Allocation *out,
                             const void *usr,
                             const RsScriptCall *call) {
    GET_TLS();
    rsrForEach(rsc, sc, target, in, out, usr, 0, call);
}

static void SC_ForEach_SAAUL(Script *target,
                             Allocation *in,
                             Allocation *out,
                             const void *usr,
                             uint32_t usrLen) {
    GET_TLS();
    rsrForEach(rsc, sc, target, in, out, usr, usrLen, NULL);
}

static void SC_ForEach_SAAULS(Script *target,
                              Allocation *in,
                              Allocation *out,
                              const void *usr,
                              uint32_t usrLen,
                              const RsScriptCall *call) {
    GET_TLS();
    rsrForEach(rsc, sc, target, in, out, usr, usrLen, call);
}



//////////////////////////////////////////////////////////////////////////////
// Time routines
//////////////////////////////////////////////////////////////////////////////

static float SC_GetDt() {
    GET_TLS();
    return rsrGetDt(rsc, sc);
}

time_t SC_Time(time_t *timer) {
    GET_TLS();
    return rsrTime(rsc, sc, timer);
}

tm* SC_LocalTime(tm *local, time_t *timer) {
    GET_TLS();
    return rsrLocalTime(rsc, sc, local, timer);
}

int64_t SC_UptimeMillis() {
    GET_TLS();
    return rsrUptimeMillis(rsc, sc);
}

int64_t SC_UptimeNanos() {
    GET_TLS();
    return rsrUptimeNanos(rsc, sc);
}

//////////////////////////////////////////////////////////////////////////////
// Message routines
//////////////////////////////////////////////////////////////////////////////

static uint32_t SC_ToClient2(int cmdID, void *data, int len) {
    GET_TLS();
    return rsrToClient(rsc, sc, cmdID, data, len);
}

static uint32_t SC_ToClient(int cmdID) {
    GET_TLS();
    return rsrToClient(rsc, sc, cmdID, NULL, 0);
}

static uint32_t SC_ToClientBlocking2(int cmdID, void *data, int len) {
    GET_TLS();
    return rsrToClientBlocking(rsc, sc, cmdID, data, len);
}

static uint32_t SC_ToClientBlocking(int cmdID) {
    GET_TLS();
    return rsrToClientBlocking(rsc, sc, cmdID, NULL, 0);
}

int SC_divsi3(int a, int b) {
    return a / b;
}

int SC_modsi3(int a, int b) {
    return a % b;
}

unsigned int SC_udivsi3(unsigned int a, unsigned int b) {
    return a / b;
}

unsigned int SC_umodsi3(unsigned int a, unsigned int b) {
    return a % b;
}

static void SC_debugF(const char *s, float f) {
    ALOGD("%s %f, 0x%08x", s, f, *((int *) (&f)));
}
static void SC_debugFv2(const char *s, float f1, float f2) {
    ALOGD("%s {%f, %f}", s, f1, f2);
}
static void SC_debugFv3(const char *s, float f1, float f2, float f3) {
    ALOGD("%s {%f, %f, %f}", s, f1, f2, f3);
}
static void SC_debugFv4(const char *s, float f1, float f2, float f3, float f4) {
    ALOGD("%s {%f, %f, %f, %f}", s, f1, f2, f3, f4);
}
static void SC_debugF2(const char *s, float2 f) {
    ALOGD("%s {%f, %f}", s, f.x, f.y);
}
static void SC_debugF3(const char *s, float3 f) {
    ALOGD("%s {%f, %f, %f}", s, f.x, f.y, f.z);
}
static void SC_debugF4(const char *s, float4 f) {
    ALOGD("%s {%f, %f, %f, %f}", s, f.x, f.y, f.z, f.w);
}
static void SC_debugD(const char *s, double d) {
    ALOGD("%s %f, 0x%08llx", s, d, *((long long *) (&d)));
}
static void SC_debugFM4v4(const char *s, const float *f) {
    ALOGD("%s {%f, %f, %f, %f", s, f[0], f[4], f[8], f[12]);
    ALOGD("%s  %f, %f, %f, %f", s, f[1], f[5], f[9], f[13]);
    ALOGD("%s  %f, %f, %f, %f", s, f[2], f[6], f[10], f[14]);
    ALOGD("%s  %f, %f, %f, %f}", s, f[3], f[7], f[11], f[15]);
}
static void SC_debugFM3v3(const char *s, const float *f) {
    ALOGD("%s {%f, %f, %f", s, f[0], f[3], f[6]);
    ALOGD("%s  %f, %f, %f", s, f[1], f[4], f[7]);
    ALOGD("%s  %f, %f, %f}",s, f[2], f[5], f[8]);
}
static void SC_debugFM2v2(const char *s, const float *f) {
    ALOGD("%s {%f, %f", s, f[0], f[2]);
    ALOGD("%s  %f, %f}",s, f[1], f[3]);
}
static void SC_debugI8(const char *s, char c) {
    ALOGD("%s %hhd  0x%hhx", s, c, (unsigned char)c);
}
static void SC_debugC2(const char *s, char2 c) {
    ALOGD("%s {%hhd, %hhd}  0x%hhx 0x%hhx", s, c.x, c.y, (unsigned char)c.x, (unsigned char)c.y);
}
static void SC_debugC3(const char *s, char3 c) {
    ALOGD("%s {%hhd, %hhd, %hhd}  0x%hhx 0x%hhx 0x%hhx", s, c.x, c.y, c.z, (unsigned char)c.x, (unsigned char)c.y, (unsigned char)c.z);
}
static void SC_debugC4(const char *s, char4 c) {
    ALOGD("%s {%hhd, %hhd, %hhd, %hhd}  0x%hhx 0x%hhx 0x%hhx 0x%hhx", s, c.x, c.y, c.z, c.w, (unsigned char)c.x, (unsigned char)c.y, (unsigned char)c.z, (unsigned char)c.w);
}
static void SC_debugU8(const char *s, unsigned char c) {
    ALOGD("%s %hhu  0x%hhx", s, c, c);
}
static void SC_debugUC2(const char *s, uchar2 c) {
    ALOGD("%s {%hhu, %hhu}  0x%hhx 0x%hhx", s, c.x, c.y, c.x, c.y);
}
static void SC_debugUC3(const char *s, uchar3 c) {
    ALOGD("%s {%hhu, %hhu, %hhu}  0x%hhx 0x%hhx 0x%hhx", s, c.x, c.y, c.z, c.x, c.y, c.z);
}
static void SC_debugUC4(const char *s, uchar4 c) {
    ALOGD("%s {%hhu, %hhu, %hhu, %hhu}  0x%hhx 0x%hhx 0x%hhx 0x%hhx", s, c.x, c.y, c.z, c.w, c.x, c.y, c.z, c.w);
}
static void SC_debugI16(const char *s, short c) {
    ALOGD("%s %hd  0x%hx", s, c, c);
}
static void SC_debugS2(const char *s, short2 c) {
    ALOGD("%s {%hd, %hd}  0x%hx 0x%hx", s, c.x, c.y, c.x, c.y);
}
static void SC_debugS3(const char *s, short3 c) {
    ALOGD("%s {%hd, %hd, %hd}  0x%hx 0x%hx 0x%hx", s, c.x, c.y, c.z, c.x, c.y, c.z);
}
static void SC_debugS4(const char *s, short4 c) {
    ALOGD("%s {%hd, %hd, %hd, %hd}  0x%hx 0x%hx 0x%hx 0x%hx", s, c.x, c.y, c.z, c.w, c.x, c.y, c.z, c.w);
}
static void SC_debugU16(const char *s, unsigned short c) {
    ALOGD("%s %hu  0x%hx", s, c, c);
}
static void SC_debugUS2(const char *s, ushort2 c) {
    ALOGD("%s {%hu, %hu}  0x%hx 0x%hx", s, c.x, c.y, c.x, c.y);
}
static void SC_debugUS3(const char *s, ushort3 c) {
    ALOGD("%s {%hu, %hu, %hu}  0x%hx 0x%hx 0x%hx", s, c.x, c.y, c.z, c.x, c.y, c.z);
}
static void SC_debugUS4(const char *s, ushort4 c) {
    ALOGD("%s {%hu, %hu, %hu, %hu}  0x%hx 0x%hx 0x%hx 0x%hx", s, c.x, c.y, c.z, c.w, c.x, c.y, c.z, c.w);
}
static void SC_debugI32(const char *s, int32_t i) {
    ALOGD("%s %d  0x%x", s, i, i);
}
static void SC_debugI2(const char *s, int2 i) {
    ALOGD("%s {%d, %d}  0x%x 0x%x", s, i.x, i.y, i.x, i.y);
}
static void SC_debugI3(const char *s, int3 i) {
    ALOGD("%s {%d, %d, %d}  0x%x 0x%x 0x%x", s, i.x, i.y, i.z, i.x, i.y, i.z);
}
static void SC_debugI4(const char *s, int4 i) {
    ALOGD("%s {%d, %d, %d, %d}  0x%x 0x%x 0x%x 0x%x", s, i.x, i.y, i.z, i.w, i.x, i.y, i.z, i.w);
}
static void SC_debugU32(const char *s, uint32_t i) {
    ALOGD("%s %u  0x%x", s, i, i);
}
static void SC_debugUI2(const char *s, uint2 i) {
    ALOGD("%s {%u, %u}  0x%x 0x%x", s, i.x, i.y, i.x, i.y);
}
static void SC_debugUI3(const char *s, uint3 i) {
    ALOGD("%s {%u, %u, %u}  0x%x 0x%x 0x%x", s, i.x, i.y, i.z, i.x, i.y, i.z);
}
static void SC_debugUI4(const char *s, uint4 i) {
    ALOGD("%s {%u, %u, %u, %u}  0x%x 0x%x 0x%x 0x%x", s, i.x, i.y, i.z, i.w, i.x, i.y, i.z, i.w);
}
static void SC_debugLL64(const char *s, long long ll) {
    ALOGD("%s %lld  0x%llx", s, ll, ll);
}
static void SC_debugL2(const char *s, long2 ll) {
    ALOGD("%s {%lld, %lld}  0x%llx 0x%llx", s, ll.x, ll.y, ll.x, ll.y);
}
static void SC_debugL3(const char *s, long3 ll) {
    ALOGD("%s {%lld, %lld, %lld}  0x%llx 0x%llx 0x%llx", s, ll.x, ll.y, ll.z, ll.x, ll.y, ll.z);
}
static void SC_debugL4(const char *s, long4 ll) {
    ALOGD("%s {%lld, %lld, %lld, %lld}  0x%llx 0x%llx 0x%llx 0x%llx", s, ll.x, ll.y, ll.z, ll.w, ll.x, ll.y, ll.z, ll.w);
}
static void SC_debugULL64(const char *s, unsigned long long ll) {
    ALOGD("%s %llu  0x%llx", s, ll, ll);
}
static void SC_debugUL2(const char *s, ulong2 ll) {
    ALOGD("%s {%llu, %llu}  0x%llx 0x%llx", s, ll.x, ll.y, ll.x, ll.y);
}
static void SC_debugUL3(const char *s, ulong3 ll) {
    ALOGD("%s {%llu, %llu, %llu}  0x%llx 0x%llx 0x%llx", s, ll.x, ll.y, ll.z, ll.x, ll.y, ll.z);
}
static void SC_debugUL4(const char *s, ulong4 ll) {
    ALOGD("%s {%llu, %llu, %llu, %llu}  0x%llx 0x%llx 0x%llx 0x%llx", s, ll.x, ll.y, ll.z, ll.w, ll.x, ll.y, ll.z, ll.w);
}
static void SC_debugP(const char *s, const void *p) {
    ALOGD("%s %p", s, p);
}


//////////////////////////////////////////////////////////////////////////////
// Stub implementation
//////////////////////////////////////////////////////////////////////////////

// llvm name mangling ref
//  <builtin-type> ::= v  # void
//                 ::= b  # bool
//                 ::= c  # char
//                 ::= a  # signed char
//                 ::= h  # unsigned char
//                 ::= s  # short
//                 ::= t  # unsigned short
//                 ::= i  # int
//                 ::= j  # unsigned int
//                 ::= l  # long
//                 ::= m  # unsigned long
//                 ::= x  # long long, __int64
//                 ::= y  # unsigned long long, __int64
//                 ::= f  # float
//                 ::= d  # double

static RsdSymbolTable gSyms[] = {
    { "memset", (void *)&memset, true },
    { "memcpy", (void *)&memcpy, true },

    // Refcounting
    { "_Z11rsSetObjectP10rs_elementS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP10rs_element", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject10rs_element", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP7rs_typeS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP7rs_type", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject7rs_type", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP13rs_allocationS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP13rs_allocation", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject13rs_allocation", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP10rs_samplerS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP10rs_sampler", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject10rs_sampler", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP9rs_scriptS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP9rs_script", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject9rs_script", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP7rs_pathS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP7rs_path", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject7rs_path", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP7rs_meshS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP7rs_mesh", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject7rs_mesh", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP19rs_program_fragmentS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP19rs_program_fragment", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject19rs_program_fragment", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP17rs_program_vertexS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP17rs_program_vertex", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject17rs_program_vertex", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP17rs_program_rasterS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP17rs_program_raster", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject17rs_program_raster", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP16rs_program_storeS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP16rs_program_store", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject16rs_program_store", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP7rs_fontS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP7rs_font", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject7rs_font", (void *)&SC_IsObject, true },

    // Allocation ops
    { "_Z21rsAllocationMarkDirty13rs_allocation", (void *)&SC_AllocationSyncAll, true },
    { "_Z20rsgAllocationSyncAll13rs_allocation", (void *)&SC_AllocationSyncAll, false },
    { "_Z20rsgAllocationSyncAll13rs_allocationj", (void *)&SC_AllocationSyncAll2, false },
    { "_Z20rsgAllocationSyncAll13rs_allocation24rs_allocation_usage_type", (void *)&SC_AllocationSyncAll2, false },
    { "_Z15rsGetAllocationPKv", (void *)&SC_GetAllocation, true },
    { "_Z18rsAllocationIoSend13rs_allocation", (void *)&SC_AllocationIoSend, false },
    { "_Z21rsAllocationIoReceive13rs_allocation", (void *)&SC_AllocationIoReceive, false },
    { "_Z23rsAllocationCopy1DRange13rs_allocationjjjS_jj", (void *)&SC_AllocationCopy1DRange, false },
    { "_Z23rsAllocationCopy2DRange13rs_allocationjjj26rs_allocation_cubemap_facejjS_jjjS0_", (void *)&SC_AllocationCopy2DRange, false },

    // Messaging

    { "_Z14rsSendToClienti", (void *)&SC_ToClient, false },
    { "_Z14rsSendToClientiPKvj", (void *)&SC_ToClient2, false },
    { "_Z22rsSendToClientBlockingi", (void *)&SC_ToClientBlocking, false },
    { "_Z22rsSendToClientBlockingiPKvj", (void *)&SC_ToClientBlocking2, false },

    { "_Z22rsgBindProgramFragment19rs_program_fragment", (void *)&SC_BindProgramFragment, false },
    { "_Z19rsgBindProgramStore16rs_program_store", (void *)&SC_BindProgramStore, false },
    { "_Z20rsgBindProgramVertex17rs_program_vertex", (void *)&SC_BindProgramVertex, false },
    { "_Z20rsgBindProgramRaster17rs_program_raster", (void *)&SC_BindProgramRaster, false },
    { "_Z14rsgBindSampler19rs_program_fragmentj10rs_sampler", (void *)&SC_BindSampler, false },
    { "_Z14rsgBindTexture19rs_program_fragmentj13rs_allocation", (void *)&SC_BindTexture, false },
    { "_Z15rsgBindConstant19rs_program_fragmentj13rs_allocation", (void *)&SC_BindFragmentConstant, false },
    { "_Z15rsgBindConstant17rs_program_vertexj13rs_allocation", (void *)&SC_BindVertexConstant, false },

    { "_Z36rsgProgramVertexLoadProjectionMatrixPK12rs_matrix4x4", (void *)&SC_VpLoadProjectionMatrix, false },
    { "_Z31rsgProgramVertexLoadModelMatrixPK12rs_matrix4x4", (void *)&SC_VpLoadModelMatrix, false },
    { "_Z33rsgProgramVertexLoadTextureMatrixPK12rs_matrix4x4", (void *)&SC_VpLoadTextureMatrix, false },

    { "_Z35rsgProgramVertexGetProjectionMatrixP12rs_matrix4x4", (void *)&SC_VpGetProjectionMatrix, false },

    { "_Z31rsgProgramFragmentConstantColor19rs_program_fragmentffff", (void *)&SC_PfConstantColor, false },

    { "_Z11rsgGetWidthv", (void *)&SC_GetWidth, false },
    { "_Z12rsgGetHeightv", (void *)&SC_GetHeight, false },


    { "_Z11rsgDrawRectfffff", (void *)&SC_DrawRect, false },
    { "_Z11rsgDrawQuadffffffffffff", (void *)&SC_DrawQuad, false },
    { "_Z20rsgDrawQuadTexCoordsffffffffffffffffffff", (void *)&SC_DrawQuadTexCoords, false },
    { "_Z24rsgDrawSpriteScreenspacefffff", (void *)&SC_DrawSpriteScreenspace, false },

    { "_Z11rsgDrawMesh7rs_mesh", (void *)&SC_DrawMesh, false },
    { "_Z11rsgDrawMesh7rs_meshj", (void *)&SC_DrawMeshPrimitive, false },
    { "_Z11rsgDrawMesh7rs_meshjjj", (void *)&SC_DrawMeshPrimitiveRange, false },
    { "_Z25rsgMeshComputeBoundingBox7rs_meshPfS0_S0_S0_S0_S0_", (void *)&SC_MeshComputeBoundingBox, false },

    { "_Z11rsgDrawPath7rs_path", (void *)&SC_DrawPath, false },

    { "_Z13rsgClearColorffff", (void *)&SC_ClearColor, false },
    { "_Z13rsgClearDepthf", (void *)&SC_ClearDepth, false },

    { "_Z11rsgDrawTextPKcii", (void *)&SC_DrawText, false },
    { "_Z11rsgDrawText13rs_allocationii", (void *)&SC_DrawTextAlloc, false },
    { "_Z14rsgMeasureTextPKcPiS1_S1_S1_", (void *)&SC_MeasureText, false },
    { "_Z14rsgMeasureText13rs_allocationPiS0_S0_S0_", (void *)&SC_MeasureTextAlloc, false },

    { "_Z11rsgBindFont7rs_font", (void *)&SC_BindFont, false },
    { "_Z12rsgFontColorffff", (void *)&SC_FontColor, false },

    { "_Z18rsgBindColorTarget13rs_allocationj", (void *)&SC_BindFrameBufferObjectColorTarget, false },
    { "_Z18rsgBindDepthTarget13rs_allocation", (void *)&SC_BindFrameBufferObjectDepthTarget, false },
    { "_Z19rsgClearColorTargetj", (void *)&SC_ClearFrameBufferObjectColorTarget, false },
    { "_Z19rsgClearDepthTargetv", (void *)&SC_ClearFrameBufferObjectDepthTarget, false },
    { "_Z24rsgClearAllRenderTargetsv", (void *)&SC_ClearFrameBufferObjectTargets, false },

    { "_Z9rsForEach9rs_script13rs_allocationS0_", (void *)&SC_ForEach_SAA, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKv", (void *)&SC_ForEach_SAAU, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKvPK16rs_script_call_t", (void *)&SC_ForEach_SAAUS, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKvj", (void *)&SC_ForEach_SAAUL, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKvjPK16rs_script_call_t", (void *)&SC_ForEach_SAAULS, true },

    // time
    { "_Z6rsTimePi", (void *)&SC_Time, true },
    { "_Z11rsLocaltimeP5rs_tmPKi", (void *)&SC_LocalTime, true },
    { "_Z14rsUptimeMillisv", (void*)&SC_UptimeMillis, true },
    { "_Z13rsUptimeNanosv", (void*)&SC_UptimeNanos, true },
    { "_Z7rsGetDtv", (void*)&SC_GetDt, false },

    // misc
    { "_Z5colorffff", (void *)&SC_Color, false },
    { "_Z9rsgFinishv", (void *)&SC_Finish, false },

    // Debug
    { "_Z7rsDebugPKcf", (void *)&SC_debugF, true },
    { "_Z7rsDebugPKcff", (void *)&SC_debugFv2, true },
    { "_Z7rsDebugPKcfff", (void *)&SC_debugFv3, true },
    { "_Z7rsDebugPKcffff", (void *)&SC_debugFv4, true },
    { "_Z7rsDebugPKcDv2_f", (void *)&SC_debugF2, true },
    { "_Z7rsDebugPKcDv3_f", (void *)&SC_debugF3, true },
    { "_Z7rsDebugPKcDv4_f", (void *)&SC_debugF4, true },
    { "_Z7rsDebugPKcd", (void *)&SC_debugD, true },
    { "_Z7rsDebugPKcPK12rs_matrix4x4", (void *)&SC_debugFM4v4, true },
    { "_Z7rsDebugPKcPK12rs_matrix3x3", (void *)&SC_debugFM3v3, true },
    { "_Z7rsDebugPKcPK12rs_matrix2x2", (void *)&SC_debugFM2v2, true },
    { "_Z7rsDebugPKcc", (void *)&SC_debugI8, true },
    { "_Z7rsDebugPKcDv2_c", (void *)&SC_debugC2, true },
    { "_Z7rsDebugPKcDv3_c", (void *)&SC_debugC3, true },
    { "_Z7rsDebugPKcDv4_c", (void *)&SC_debugC4, true },
    { "_Z7rsDebugPKch", (void *)&SC_debugU8, true },
    { "_Z7rsDebugPKcDv2_h", (void *)&SC_debugUC2, true },
    { "_Z7rsDebugPKcDv3_h", (void *)&SC_debugUC3, true },
    { "_Z7rsDebugPKcDv4_h", (void *)&SC_debugUC4, true },
    { "_Z7rsDebugPKcs", (void *)&SC_debugI16, true },
    { "_Z7rsDebugPKcDv2_s", (void *)&SC_debugS2, true },
    { "_Z7rsDebugPKcDv3_s", (void *)&SC_debugS3, true },
    { "_Z7rsDebugPKcDv4_s", (void *)&SC_debugS4, true },
    { "_Z7rsDebugPKct", (void *)&SC_debugU16, true },
    { "_Z7rsDebugPKcDv2_t", (void *)&SC_debugUS2, true },
    { "_Z7rsDebugPKcDv3_t", (void *)&SC_debugUS3, true },
    { "_Z7rsDebugPKcDv4_t", (void *)&SC_debugUS4, true },
    { "_Z7rsDebugPKci", (void *)&SC_debugI32, true },
    { "_Z7rsDebugPKcDv2_i", (void *)&SC_debugI2, true },
    { "_Z7rsDebugPKcDv3_i", (void *)&SC_debugI3, true },
    { "_Z7rsDebugPKcDv4_i", (void *)&SC_debugI4, true },
    { "_Z7rsDebugPKcj", (void *)&SC_debugU32, true },
    { "_Z7rsDebugPKcDv2_j", (void *)&SC_debugUI2, true },
    { "_Z7rsDebugPKcDv3_j", (void *)&SC_debugUI3, true },
    { "_Z7rsDebugPKcDv4_j", (void *)&SC_debugUI4, true },
    // Both "long" and "unsigned long" need to be redirected to their
    // 64-bit counterparts, since we have hacked Slang to use 64-bit
    // for "long" on Arm (to be similar to Java).
    { "_Z7rsDebugPKcl", (void *)&SC_debugLL64, true },
    { "_Z7rsDebugPKcDv2_l", (void *)&SC_debugL2, true },
    { "_Z7rsDebugPKcDv3_l", (void *)&SC_debugL3, true },
    { "_Z7rsDebugPKcDv4_l", (void *)&SC_debugL4, true },
    { "_Z7rsDebugPKcm", (void *)&SC_debugULL64, true },
    { "_Z7rsDebugPKcDv2_m", (void *)&SC_debugUL2, true },
    { "_Z7rsDebugPKcDv3_m", (void *)&SC_debugUL3, true },
    { "_Z7rsDebugPKcDv4_m", (void *)&SC_debugUL4, true },
    { "_Z7rsDebugPKcx", (void *)&SC_debugLL64, true },
    { "_Z7rsDebugPKcDv2_x", (void *)&SC_debugL2, true },
    { "_Z7rsDebugPKcDv3_x", (void *)&SC_debugL3, true },
    { "_Z7rsDebugPKcDv4_x", (void *)&SC_debugL4, true },
    { "_Z7rsDebugPKcy", (void *)&SC_debugULL64, true },
    { "_Z7rsDebugPKcDv2_y", (void *)&SC_debugUL2, true },
    { "_Z7rsDebugPKcDv3_y", (void *)&SC_debugUL3, true },
    { "_Z7rsDebugPKcDv4_y", (void *)&SC_debugUL4, true },
    { "_Z7rsDebugPKcPKv", (void *)&SC_debugP, true },

    { NULL, NULL, false }
};


void* rsdLookupRuntimeStub(void* pContext, char const* name) {
    ScriptC *s = (ScriptC *)pContext;
    RsdSymbolTable *syms = gSyms;
    const RsdSymbolTable *sym = rsdLookupSymbolMath(name);

    if (!sym) {
        while (syms->mPtr) {
            if (!strcmp(syms->mName, name)) {
                sym = syms;
            }
            syms++;
        }
    }

    if (sym) {
        s->mHal.info.isThreadable &= sym->threadable;
        return sym->mPtr;
    }
    ALOGE("ScriptC sym lookup failed for %s", name);
    return NULL;
}


