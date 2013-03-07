/*
 * Copyright (C) 2011-2012 The Android Open Source Project
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

//#include "rsdPath.h"
#include "rsdAllocation.h"

#include <time.h>

using namespace android;
using namespace android::renderscript;

typedef float float2 __attribute__((ext_vector_type(2)));
typedef float float3 __attribute__((ext_vector_type(3)));
typedef float float4 __attribute__((ext_vector_type(4)));
typedef double double2 __attribute__((ext_vector_type(2)));
typedef double double3 __attribute__((ext_vector_type(3)));
typedef double double4 __attribute__((ext_vector_type(4)));
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

typedef uint8_t uchar;
typedef uint16_t ushort;
typedef uint32_t uint;
typedef uint64_t ulong;
#define OPAQUETYPE(t) \
typedef struct { const int* const p; } __attribute__((packed, aligned(4))) t;

OPAQUETYPE(rs_element)
OPAQUETYPE(rs_type)
OPAQUETYPE(rs_allocation)
OPAQUETYPE(rs_sampler)
OPAQUETYPE(rs_script)
OPAQUETYPE(rs_script_call)
#undef OPAQUETYPE

typedef struct {
    int tm_sec;     ///< seconds
    int tm_min;     ///< minutes
    int tm_hour;    ///< hours
    int tm_mday;    ///< day of the month
    int tm_mon;     ///< month
    int tm_year;    ///< year
    int tm_wday;    ///< day of the week
    int tm_yday;    ///< day of the year
    int tm_isdst;   ///< daylight savings time
} rs_tm;

//////////////////////////////////////////////////////////////////////////////
// Allocation
//////////////////////////////////////////////////////////////////////////////


static void SC_AllocationSyncAll2(Allocation *a, RsAllocationUsageType source) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrAllocationSyncAll(rsc, a, source);
}

static void SC_AllocationSyncAll(Allocation *a) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrAllocationSyncAll(rsc, a, RS_ALLOCATION_USAGE_SCRIPT);
}

static void SC_AllocationCopy1DRange(Allocation *dstAlloc,
                                     uint32_t dstOff,
                                     uint32_t dstMip,
                                     uint32_t count,
                                     Allocation *srcAlloc,
                                     uint32_t srcOff, uint32_t srcMip) {
    Context *rsc = RsdCpuReference::getTlsContext();
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
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrAllocationCopy2DRange(rsc, dstAlloc,
                             dstXoff, dstYoff, dstMip, dstFace,
                             width, height,
                             srcAlloc,
                             srcXoff, srcYoff, srcMip, srcFace);
}


//////////////////////////////////////////////////////////////////////////////
//
//////////////////////////////////////////////////////////////////////////////

static void SC_SetObject(ObjectBase **dst, ObjectBase * src) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrSetObject(rsc, dst, src);
}

static void SC_ClearObject(ObjectBase **dst) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrClearObject(rsc, dst);
}

static bool SC_IsObject(const ObjectBase *src) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrIsObject(rsc, src);
}

bool rsIsObject(rs_element src) {
    return SC_IsObject((ObjectBase*)src.p);
}


static const Allocation * SC_GetAllocation(const void *ptr) {
    Context *rsc = RsdCpuReference::getTlsContext();
    const Script *sc = RsdCpuReference::getTlsScript();
    return rsdScriptGetAllocationForPointer(rsc, sc, ptr);
}

const Allocation * rsGetAllocation(const void *ptr) {
    return SC_GetAllocation(ptr);
}

static void SC_ForEach_SAA(Script *target,
                            Allocation *in,
                            Allocation *out) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrForEach(rsc, target, in, out, NULL, 0, NULL);
}

static void SC_ForEach_SAAU(Script *target,
                            Allocation *in,
                            Allocation *out,
                            const void *usr) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrForEach(rsc, target, in, out, usr, 0, NULL);
}

static void SC_ForEach_SAAUS(Script *target,
                             Allocation *in,
                             Allocation *out,
                             const void *usr,
                             const RsScriptCall *call) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrForEach(rsc, target, in, out, usr, 0, call);
}

void __attribute__((overloadable)) rsForEach(rs_script script,
                                             rs_allocation in,
                                             rs_allocation out,
                                             const void *usr,
                                             const rs_script_call *call) {
    return SC_ForEach_SAAUS((Script *)script.p, (Allocation*)in.p, (Allocation*)out.p, usr, (RsScriptCall*)call);
}


static void SC_ForEach_SAAUL(Script *target,
                             Allocation *in,
                             Allocation *out,
                             const void *usr,
                             uint32_t usrLen) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrForEach(rsc, target, in, out, usr, usrLen, NULL);
}

static void SC_ForEach_SAAULS(Script *target,
                              Allocation *in,
                              Allocation *out,
                              const void *usr,
                              uint32_t usrLen,
                              const RsScriptCall *call) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrForEach(rsc, target, in, out, usr, usrLen, call);
}

void __attribute__((overloadable)) rsForEach(rs_script script,
                                             rs_allocation in,
                                             rs_allocation out,
                                             const void *usr,
                                             uint32_t usrLen,
                                             const rs_script_call *call) {
    return SC_ForEach_SAAULS((Script *)script.p, (Allocation*)in.p, (Allocation*)out.p, usr, usrLen, (RsScriptCall*)call);
}



//////////////////////////////////////////////////////////////////////////////
// Time routines
//////////////////////////////////////////////////////////////////////////////

static float SC_GetDt() {
    Context *rsc = RsdCpuReference::getTlsContext();
    const Script *sc = RsdCpuReference::getTlsScript();
    return rsrGetDt(rsc, sc);
}

static int SC_Time(int *timer) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrTime(rsc, (long*)timer);
}

int rsTime(int *timer) {
    return SC_Time(timer);
}

tm* SC_LocalTime(tm *local, time_t *timer) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrLocalTime(rsc, local, timer);
}

rs_tm* rsLocaltime(rs_tm* local, const int *timer) {
    return (rs_tm*)(SC_LocalTime((tm*)local, (long*)timer));
}

int64_t rsUptimeMillis() {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrUptimeMillis(rsc);
}

int64_t SC_UptimeNanos() {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrUptimeNanos(rsc);
}

//////////////////////////////////////////////////////////////////////////////
// Message routines
//////////////////////////////////////////////////////////////////////////////

static uint32_t SC_ToClient2(int cmdID, void *data, int len) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrToClient(rsc, cmdID, data, len);
}

static uint32_t SC_ToClient(int cmdID) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrToClient(rsc, cmdID, NULL, 0);
}

uint32_t rsSendToClientBlocking2(int cmdID, void *data, int len) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrToClientBlocking(rsc, cmdID, data, len);
}

uint32_t rsSendToClientBlocking(int cmdID) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrToClientBlocking(rsc, cmdID, NULL, 0);
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

static void * ElementAt1D(Allocation *a, RsDataType dt, uint32_t vecSize, uint32_t x) {
    Context *rsc = RsdCpuReference::getTlsContext();
    const Type *t = a->getType();
    const Element *e = t->getElement();

    char buf[256];
    if (x >= t->getLODDimX(0)) {
        sprintf(buf, "Out range ElementAt X %i of %i", x, t->getLODDimX(0));
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    if (vecSize != e->getVectorSize()) {
        sprintf(buf, "Vector size mismatch for ElementAt %i of %i", vecSize, e->getVectorSize());
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    if (dt != e->getType()) {
        sprintf(buf, "Data type mismatch for ElementAt %i of %i", dt, e->getType());
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    uint8_t *p = (uint8_t *)a->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = e->getSizeBytes();
    return &p[(eSize * x)];
}

static void * ElementAt2D(Allocation *a, RsDataType dt, uint32_t vecSize, uint32_t x, uint32_t y) {
    Context *rsc = RsdCpuReference::getTlsContext();
    const Type *t = a->getType();
    const Element *e = t->getElement();

    char buf[256];
    if (x >= t->getLODDimX(0)) {
        sprintf(buf, "Out range ElementAt X %i of %i", x, t->getLODDimX(0));
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    if (y >= t->getLODDimY(0)) {
        sprintf(buf, "Out range ElementAt Y %i of %i", y, t->getLODDimY(0));
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    if (vecSize != e->getVectorSize()) {
        sprintf(buf, "Vector size mismatch for ElementAt %i of %i", vecSize, e->getVectorSize());
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    if (dt != e->getType()) {
        sprintf(buf, "Data type mismatch for ElementAt %i of %i", dt, e->getType());
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    uint8_t *p = (uint8_t *)a->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = e->getSizeBytes();
    const uint32_t stride = a->mHal.drvState.lod[0].stride;
    return &p[(eSize * x) + (y * stride)];
}

static void * ElementAt3D(Allocation *a, RsDataType dt, uint32_t vecSize, uint32_t x, uint32_t y, uint32_t z) {
    Context *rsc = RsdCpuReference::getTlsContext();
    const Type *t = a->getType();
    const Element *e = t->getElement();

    char buf[256];
    if (x >= t->getLODDimX(0)) {
        sprintf(buf, "Out range ElementAt X %i of %i", x, t->getLODDimX(0));
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    if (y >= t->getLODDimY(0)) {
        sprintf(buf, "Out range ElementAt Y %i of %i", y, t->getLODDimY(0));
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    if (z >= t->getLODDimZ(0)) {
        sprintf(buf, "Out range ElementAt Z %i of %i", z, t->getLODDimZ(0));
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    if (vecSize != e->getVectorSize()) {
        sprintf(buf, "Vector size mismatch for ElementAt %i of %i", vecSize, e->getVectorSize());
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    if (dt != e->getType()) {
        sprintf(buf, "Data type mismatch for ElementAt %i of %i", dt, e->getType());
        rsc->setError(RS_ERROR_FATAL_UNKNOWN, buf);
        return NULL;
    }

    uint8_t *p = (uint8_t *)a->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = e->getSizeBytes();
    const uint32_t stride = a->mHal.drvState.lod[0].stride;
    return &p[(eSize * x) + (y * stride)];
}

#define ELEMENT_AT(T, DT, VS)                                               \
    static void SC_SetElementAt1_##T(Allocation *a, T val, uint32_t x) {           \
        void *r = ElementAt1D(a, DT, VS, x);                            \
        if (r != NULL) ((T *)r)[0] = val;                               \
        else ALOGE("Error from %s", __PRETTY_FUNCTION__);               \
    }                                                                   \
    static void SC_SetElementAt2_##T(Allocation * a, T val, uint32_t x, uint32_t y) { \
        void *r = ElementAt2D(a, DT, VS, x, y);            \
        if (r != NULL) ((T *)r)[0] = val;                               \
        else ALOGE("Error from %s", __PRETTY_FUNCTION__);               \
    }                                                                   \
    static void SC_SetElementAt3_##T(Allocation * a, T val, uint32_t x, uint32_t y, uint32_t z) { \
        void *r = ElementAt3D(a, DT, VS, x, y, z);         \
        if (r != NULL) ((T *)r)[0] = val;                               \
        else ALOGE("Error from %s", __PRETTY_FUNCTION__);               \
    }                                                                   \
    static T SC_GetElementAt1_##T(Allocation * a, uint32_t x) {                  \
        void *r = ElementAt1D(a, DT, VS, x);               \
        if (r != NULL) return ((T *)r)[0];                              \
        ALOGE("Error from %s", __PRETTY_FUNCTION__);                    \
        return 0;                                                       \
    }                                                                   \
    static T SC_GetElementAt2_##T(Allocation * a, uint32_t x, uint32_t y) {      \
        void *r = ElementAt2D(a, DT, VS, x, y);            \
        if (r != NULL) return ((T *)r)[0];                              \
        ALOGE("Error from %s", __PRETTY_FUNCTION__);                    \
        return 0;                                                       \
    }                                                                   \
    static T SC_GetElementAt3_##T(Allocation * a, uint32_t x, uint32_t y, uint32_t z) { \
        void *r = ElementAt3D(a, DT, VS, x, y, z);         \
        if (r != NULL) return ((T *)r)[0];                              \
        ALOGE("Error from %s", __PRETTY_FUNCTION__);                    \
        return 0;                                                       \
    }

ELEMENT_AT(char, RS_TYPE_SIGNED_8, 1)
ELEMENT_AT(char2, RS_TYPE_SIGNED_8, 2)
ELEMENT_AT(char3, RS_TYPE_SIGNED_8, 3)
ELEMENT_AT(char4, RS_TYPE_SIGNED_8, 4)
ELEMENT_AT(uchar, RS_TYPE_UNSIGNED_8, 1)
ELEMENT_AT(uchar2, RS_TYPE_UNSIGNED_8, 2)
ELEMENT_AT(uchar3, RS_TYPE_UNSIGNED_8, 3)
ELEMENT_AT(uchar4, RS_TYPE_UNSIGNED_8, 4)
ELEMENT_AT(short, RS_TYPE_SIGNED_16, 1)
ELEMENT_AT(short2, RS_TYPE_SIGNED_16, 2)
ELEMENT_AT(short3, RS_TYPE_SIGNED_16, 3)
ELEMENT_AT(short4, RS_TYPE_SIGNED_16, 4)
ELEMENT_AT(ushort, RS_TYPE_UNSIGNED_16, 1)
ELEMENT_AT(ushort2, RS_TYPE_UNSIGNED_16, 2)
ELEMENT_AT(ushort3, RS_TYPE_UNSIGNED_16, 3)
ELEMENT_AT(ushort4, RS_TYPE_UNSIGNED_16, 4)
ELEMENT_AT(int, RS_TYPE_SIGNED_32, 1)
ELEMENT_AT(int2, RS_TYPE_SIGNED_32, 2)
ELEMENT_AT(int3, RS_TYPE_SIGNED_32, 3)
ELEMENT_AT(int4, RS_TYPE_SIGNED_32, 4)
ELEMENT_AT(uint, RS_TYPE_UNSIGNED_32, 1)
ELEMENT_AT(uint2, RS_TYPE_UNSIGNED_32, 2)
ELEMENT_AT(uint3, RS_TYPE_UNSIGNED_32, 3)
ELEMENT_AT(uint4, RS_TYPE_UNSIGNED_32, 4)
ELEMENT_AT(long, RS_TYPE_SIGNED_64, 1)
ELEMENT_AT(long2, RS_TYPE_SIGNED_64, 2)
ELEMENT_AT(long3, RS_TYPE_SIGNED_64, 3)
ELEMENT_AT(long4, RS_TYPE_SIGNED_64, 4)
ELEMENT_AT(ulong, RS_TYPE_UNSIGNED_64, 1)
ELEMENT_AT(ulong2, RS_TYPE_UNSIGNED_64, 2)
ELEMENT_AT(ulong3, RS_TYPE_UNSIGNED_64, 3)
ELEMENT_AT(ulong4, RS_TYPE_UNSIGNED_64, 4)
ELEMENT_AT(float, RS_TYPE_FLOAT_32, 1)
ELEMENT_AT(float2, RS_TYPE_FLOAT_32, 2)
ELEMENT_AT(float3, RS_TYPE_FLOAT_32, 3)
ELEMENT_AT(float4, RS_TYPE_FLOAT_32, 4)
ELEMENT_AT(double, RS_TYPE_FLOAT_64, 1)
ELEMENT_AT(double2, RS_TYPE_FLOAT_64, 2)
ELEMENT_AT(double3, RS_TYPE_FLOAT_64, 3)
ELEMENT_AT(double4, RS_TYPE_FLOAT_64, 4)

#undef ELEMENT_AT

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

static RsdCpuReference::CpuSymbol gSyms[] = {
    { "memset", (void *)&memset, true },
    { "memcpy", (void *)&memcpy, true },
    // Debug runtime
    { "_Z20rsGetElementAt_uchar13rs_allocationcj", (void *)&SC_GetElementAt1_uchar, true },
    { "_Z21rsGetElementAt_uchar213rs_allocationj", (void *)&SC_GetElementAt1_uchar2, true },
    { "_Z21rsGetElementAt_uchar313rs_allocationj", (void *)&SC_GetElementAt1_uchar3, true },
    { "_Z21rsGetElementAt_uchar413rs_allocationj", (void *)&SC_GetElementAt1_uchar4, true },
    { "_Z20rsGetElementAt_uchar13rs_allocationjj", (void *)&SC_GetElementAt2_uchar, true },
    { "_Z21rsGetElementAt_uchar213rs_allocationjj", (void *)&SC_GetElementAt2_uchar2, true },
    { "_Z21rsGetElementAt_uchar313rs_allocationjj", (void *)&SC_GetElementAt2_uchar3, true },
    { "_Z21rsGetElementAt_uchar413rs_allocationjj", (void *)&SC_GetElementAt2_uchar4, true },
    { "_Z20rsGetElementAt_uchar13rs_allocationjjj", (void *)&SC_GetElementAt3_uchar, true },
    { "_Z21rsGetElementAt_uchar213rs_allocationjjj", (void *)&SC_GetElementAt3_uchar2, true },
    { "_Z21rsGetElementAt_uchar313rs_allocationjjj", (void *)&SC_GetElementAt3_uchar3, true },
    { "_Z21rsGetElementAt_uchar413rs_allocationjjj", (void *)&SC_GetElementAt3_uchar4, true },

    { "_Z19rsGetElementAt_char13rs_allocationj", (void *)&SC_GetElementAt1_char, true },
    { "_Z20rsGetElementAt_char213rs_allocationj", (void *)&SC_GetElementAt1_char2, true },
    { "_Z20rsGetElementAt_char313rs_allocationj", (void *)&SC_GetElementAt1_char3, true },
    { "_Z20rsGetElementAt_char413rs_allocationj", (void *)&SC_GetElementAt1_char4, true },
    { "_Z19rsGetElementAt_char13rs_allocationjj", (void *)&SC_GetElementAt2_char, true },
    { "_Z20rsGetElementAt_char213rs_allocationjj", (void *)&SC_GetElementAt2_char2, true },
    { "_Z20rsGetElementAt_char313rs_allocationjj", (void *)&SC_GetElementAt2_char3, true },
    { "_Z20rsGetElementAt_char413rs_allocationjj", (void *)&SC_GetElementAt2_char4, true },
    { "_Z19rsGetElementAt_char13rs_allocationjjj", (void *)&SC_GetElementAt3_char, true },
    { "_Z20rsGetElementAt_char213rs_allocationjjj", (void *)&SC_GetElementAt3_char2, true },
    { "_Z20rsGetElementAt_char313rs_allocationjjj", (void *)&SC_GetElementAt3_char3, true },
    { "_Z20rsGetElementAt_char413rs_allocationjjj", (void *)&SC_GetElementAt3_char4, true },

    { "_Z21rsGetElementAt_ushort13rs_allocationcj", (void *)&SC_GetElementAt1_ushort, true },
    { "_Z22rsGetElementAt_ushort213rs_allocationj", (void *)&SC_GetElementAt1_ushort2, true },
    { "_Z22rsGetElementAt_ushort313rs_allocationj", (void *)&SC_GetElementAt1_ushort3, true },
    { "_Z22rsGetElementAt_ushort413rs_allocationj", (void *)&SC_GetElementAt1_ushort4, true },
    { "_Z21rsGetElementAt_ushort13rs_allocationjj", (void *)&SC_GetElementAt2_ushort, true },
    { "_Z22rsGetElementAt_ushort213rs_allocationjj", (void *)&SC_GetElementAt2_ushort2, true },
    { "_Z22rsGetElementAt_ushort313rs_allocationjj", (void *)&SC_GetElementAt2_ushort3, true },
    { "_Z22rsGetElementAt_ushort413rs_allocationjj", (void *)&SC_GetElementAt2_ushort4, true },
    { "_Z21rsGetElementAt_ushort13rs_allocationjjj", (void *)&SC_GetElementAt3_ushort, true },
    { "_Z22rsGetElementAt_ushort213rs_allocationjjj", (void *)&SC_GetElementAt3_ushort2, true },
    { "_Z22rsGetElementAt_ushort313rs_allocationjjj", (void *)&SC_GetElementAt3_ushort3, true },
    { "_Z22rsGetElementAt_ushort413rs_allocationjjj", (void *)&SC_GetElementAt3_ushort4, true },

    { "_Z20rsGetElementAt_short13rs_allocationj", (void *)&SC_GetElementAt1_short, true },
    { "_Z21rsGetElementAt_short213rs_allocationj", (void *)&SC_GetElementAt1_short2, true },
    { "_Z21rsGetElementAt_short313rs_allocationj", (void *)&SC_GetElementAt1_short3, true },
    { "_Z21rsGetElementAt_short413rs_allocationj", (void *)&SC_GetElementAt1_short4, true },
    { "_Z20rsGetElementAt_short13rs_allocationjj", (void *)&SC_GetElementAt2_short, true },
    { "_Z21rsGetElementAt_short213rs_allocationjj", (void *)&SC_GetElementAt2_short2, true },
    { "_Z21rsGetElementAt_short313rs_allocationjj", (void *)&SC_GetElementAt2_short3, true },
    { "_Z21rsGetElementAt_short413rs_allocationjj", (void *)&SC_GetElementAt2_short4, true },
    { "_Z20rsGetElementAt_short13rs_allocationjjj", (void *)&SC_GetElementAt3_short, true },
    { "_Z21rsGetElementAt_short213rs_allocationjjj", (void *)&SC_GetElementAt3_short2, true },
    { "_Z21rsGetElementAt_short313rs_allocationjjj", (void *)&SC_GetElementAt3_short3, true },
    { "_Z21rsGetElementAt_short413rs_allocationjjj", (void *)&SC_GetElementAt3_short4, true },

    { "_Z19rsGetElementAt_uint13rs_allocationcj", (void *)&SC_GetElementAt1_uint, true },
    { "_Z20rsGetElementAt_uint213rs_allocationj", (void *)&SC_GetElementAt1_uint2, true },
    { "_Z20rsGetElementAt_uint313rs_allocationj", (void *)&SC_GetElementAt1_uint3, true },
    { "_Z20rsGetElementAt_uint413rs_allocationj", (void *)&SC_GetElementAt1_uint4, true },
    { "_Z19rsGetElementAt_uint13rs_allocationjj", (void *)&SC_GetElementAt2_uint, true },
    { "_Z20rsGetElementAt_uint213rs_allocationjj", (void *)&SC_GetElementAt2_uint2, true },
    { "_Z20rsGetElementAt_uint313rs_allocationjj", (void *)&SC_GetElementAt2_uint3, true },
    { "_Z20rsGetElementAt_uint413rs_allocationjj", (void *)&SC_GetElementAt2_uint4, true },
    { "_Z19rsGetElementAt_uint13rs_allocationjjj", (void *)&SC_GetElementAt3_uint, true },
    { "_Z20rsGetElementAt_uint213rs_allocationjjj", (void *)&SC_GetElementAt3_uint2, true },
    { "_Z20rsGetElementAt_uint313rs_allocationjjj", (void *)&SC_GetElementAt3_uint3, true },
    { "_Z20rsGetElementAt_uint413rs_allocationjjj", (void *)&SC_GetElementAt3_uint4, true },

    { "_Z18rsGetElementAt_int13rs_allocationj", (void *)&SC_GetElementAt1_int, true },
    { "_Z19rsGetElementAt_int213rs_allocationj", (void *)&SC_GetElementAt1_int2, true },
    { "_Z19rsGetElementAt_int313rs_allocationj", (void *)&SC_GetElementAt1_int3, true },
    { "_Z19rsGetElementAt_int413rs_allocationj", (void *)&SC_GetElementAt1_int4, true },
    { "_Z18rsGetElementAt_int13rs_allocationjj", (void *)&SC_GetElementAt2_int, true },
    { "_Z19rsGetElementAt_int213rs_allocationjj", (void *)&SC_GetElementAt2_int2, true },
    { "_Z19rsGetElementAt_int313rs_allocationjj", (void *)&SC_GetElementAt2_int3, true },
    { "_Z19rsGetElementAt_int413rs_allocationjj", (void *)&SC_GetElementAt2_int4, true },
    { "_Z18rsGetElementAt_int13rs_allocationjjj", (void *)&SC_GetElementAt3_int, true },
    { "_Z19rsGetElementAt_int213rs_allocationjjj", (void *)&SC_GetElementAt3_int2, true },
    { "_Z19rsGetElementAt_int313rs_allocationjjj", (void *)&SC_GetElementAt3_int3, true },
    { "_Z19rsGetElementAt_int413rs_allocationjjj", (void *)&SC_GetElementAt3_int4, true },

    { "_Z20rsGetElementAt_ulong13rs_allocationcj", (void *)&SC_GetElementAt1_ulong, true },
    { "_Z21rsGetElementAt_ulong213rs_allocationj", (void *)&SC_GetElementAt1_ulong2, true },
    { "_Z21rsGetElementAt_ulong313rs_allocationj", (void *)&SC_GetElementAt1_ulong3, true },
    { "_Z21rsGetElementAt_ulong413rs_allocationj", (void *)&SC_GetElementAt1_ulong4, true },
    { "_Z20rsGetElementAt_ulong13rs_allocationjj", (void *)&SC_GetElementAt2_ulong, true },
    { "_Z21rsGetElementAt_ulong213rs_allocationjj", (void *)&SC_GetElementAt2_ulong2, true },
    { "_Z21rsGetElementAt_ulong313rs_allocationjj", (void *)&SC_GetElementAt2_ulong3, true },
    { "_Z21rsGetElementAt_ulong413rs_allocationjj", (void *)&SC_GetElementAt2_ulong4, true },
    { "_Z20rsGetElementAt_ulong13rs_allocationjjj", (void *)&SC_GetElementAt3_ulong, true },
    { "_Z21rsGetElementAt_ulong213rs_allocationjjj", (void *)&SC_GetElementAt3_ulong2, true },
    { "_Z21rsGetElementAt_ulong313rs_allocationjjj", (void *)&SC_GetElementAt3_ulong3, true },
    { "_Z21rsGetElementAt_ulong413rs_allocationjjj", (void *)&SC_GetElementAt3_ulong4, true },

    { "_Z19rsGetElementAt_long13rs_allocationj", (void *)&SC_GetElementAt1_long, true },
    { "_Z20rsGetElementAt_long213rs_allocationj", (void *)&SC_GetElementAt1_long2, true },
    { "_Z20rsGetElementAt_long313rs_allocationj", (void *)&SC_GetElementAt1_long3, true },
    { "_Z20rsGetElementAt_long413rs_allocationj", (void *)&SC_GetElementAt1_long4, true },
    { "_Z19rsGetElementAt_long13rs_allocationjj", (void *)&SC_GetElementAt2_long, true },
    { "_Z20rsGetElementAt_long213rs_allocationjj", (void *)&SC_GetElementAt2_long2, true },
    { "_Z20rsGetElementAt_long313rs_allocationjj", (void *)&SC_GetElementAt2_long3, true },
    { "_Z20rsGetElementAt_long413rs_allocationjj", (void *)&SC_GetElementAt2_long4, true },
    { "_Z19rsGetElementAt_long13rs_allocationjjj", (void *)&SC_GetElementAt3_long, true },
    { "_Z20rsGetElementAt_long213rs_allocationjjj", (void *)&SC_GetElementAt3_long2, true },
    { "_Z20rsGetElementAt_long313rs_allocationjjj", (void *)&SC_GetElementAt3_long3, true },
    { "_Z20rsGetElementAt_long413rs_allocationjjj", (void *)&SC_GetElementAt3_long4, true },

    { "_Z20rsGetElementAt_float13rs_allocationcj", (void *)&SC_GetElementAt1_float, true },
    { "_Z21rsGetElementAt_float213rs_allocationj", (void *)&SC_GetElementAt1_float2, true },
    { "_Z21rsGetElementAt_float313rs_allocationj", (void *)&SC_GetElementAt1_float3, true },
    { "_Z21rsGetElementAt_float413rs_allocationj", (void *)&SC_GetElementAt1_float4, true },
    { "_Z20rsGetElementAt_float13rs_allocationjj", (void *)&SC_GetElementAt2_float, true },
    { "_Z21rsGetElementAt_float213rs_allocationjj", (void *)&SC_GetElementAt2_float2, true },
    { "_Z21rsGetElementAt_float313rs_allocationjj", (void *)&SC_GetElementAt2_float3, true },
    { "_Z21rsGetElementAt_float413rs_allocationjj", (void *)&SC_GetElementAt2_float4, true },
    { "_Z20rsGetElementAt_float13rs_allocationjjj", (void *)&SC_GetElementAt3_float, true },
    { "_Z21rsGetElementAt_float213rs_allocationjjj", (void *)&SC_GetElementAt3_float2, true },
    { "_Z21rsGetElementAt_float313rs_allocationjjj", (void *)&SC_GetElementAt3_float3, true },
    { "_Z21rsGetElementAt_float413rs_allocationjjj", (void *)&SC_GetElementAt3_float4, true },

    { "_Z21rsGetElementAt_double13rs_allocationcj", (void *)&SC_GetElementAt1_double, true },
    { "_Z22rsGetElementAt_double213rs_allocationj", (void *)&SC_GetElementAt1_double2, true },
    { "_Z22rsGetElementAt_double313rs_allocationj", (void *)&SC_GetElementAt1_double3, true },
    { "_Z22rsGetElementAt_double413rs_allocationj", (void *)&SC_GetElementAt1_double4, true },
    { "_Z21rsGetElementAt_double13rs_allocationjj", (void *)&SC_GetElementAt2_double, true },
    { "_Z22rsGetElementAt_double213rs_allocationjj", (void *)&SC_GetElementAt2_double2, true },
    { "_Z22rsGetElementAt_double313rs_allocationjj", (void *)&SC_GetElementAt2_double3, true },
    { "_Z22rsGetElementAt_double413rs_allocationjj", (void *)&SC_GetElementAt2_double4, true },
    { "_Z21rsGetElementAt_double13rs_allocationjjj", (void *)&SC_GetElementAt3_double, true },
    { "_Z22rsGetElementAt_double213rs_allocationjjj", (void *)&SC_GetElementAt3_double2, true },
    { "_Z22rsGetElementAt_double313rs_allocationjjj", (void *)&SC_GetElementAt3_double3, true },
    { "_Z22rsGetElementAt_double413rs_allocationjjj", (void *)&SC_GetElementAt3_double4, true },



    { "_Z20rsSetElementAt_uchar13rs_allocationhj", (void *)&SC_SetElementAt1_uchar, true },
    { "_Z21rsSetElementAt_uchar213rs_allocationDv2_hj", (void *)&SC_SetElementAt1_uchar2, true },
    { "_Z21rsSetElementAt_uchar313rs_allocationDv3_hj", (void *)&SC_SetElementAt1_uchar3, true },
    { "_Z21rsSetElementAt_uchar413rs_allocationDv4_hj", (void *)&SC_SetElementAt1_uchar4, true },
    { "_Z20rsSetElementAt_uchar13rs_allocationhjj", (void *)&SC_SetElementAt2_uchar, true },
    { "_Z21rsSetElementAt_uchar213rs_allocationDv2_hjj", (void *)&SC_SetElementAt2_uchar2, true },
    { "_Z21rsSetElementAt_uchar313rs_allocationDv3_hjj", (void *)&SC_SetElementAt2_uchar3, true },
    { "_Z21rsSetElementAt_uchar413rs_allocationDv4_hjj", (void *)&SC_SetElementAt2_uchar4, true },
    { "_Z20rsSetElementAt_uchar13rs_allocationhjjj", (void *)&SC_SetElementAt3_uchar, true },
    { "_Z21rsSetElementAt_uchar213rs_allocationDv2_hjjj", (void *)&SC_SetElementAt3_uchar2, true },
    { "_Z21rsSetElementAt_uchar313rs_allocationDv3_hjjj", (void *)&SC_SetElementAt3_uchar3, true },
    { "_Z21rsSetElementAt_uchar413rs_allocationDv4_hjjj", (void *)&SC_SetElementAt3_uchar4, true },

    { "_Z19rsSetElementAt_char13rs_allocationcj", (void *)&SC_SetElementAt1_char, true },
    { "_Z20rsSetElementAt_char213rs_allocationDv2_cj", (void *)&SC_SetElementAt1_char2, true },
    { "_Z20rsSetElementAt_char313rs_allocationDv3_cj", (void *)&SC_SetElementAt1_char3, true },
    { "_Z20rsSetElementAt_char413rs_allocationDv4_cj", (void *)&SC_SetElementAt1_char4, true },
    { "_Z19rsSetElementAt_char13rs_allocationcjj", (void *)&SC_SetElementAt2_char, true },
    { "_Z20rsSetElementAt_char213rs_allocationDv2_cjj", (void *)&SC_SetElementAt2_char2, true },
    { "_Z20rsSetElementAt_char313rs_allocationDv3_cjj", (void *)&SC_SetElementAt2_char3, true },
    { "_Z20rsSetElementAt_char413rs_allocationDv4_cjj", (void *)&SC_SetElementAt2_char4, true },
    { "_Z19rsSetElementAt_char13rs_allocationcjjj", (void *)&SC_SetElementAt3_char, true },
    { "_Z20rsSetElementAt_char213rs_allocationDv2_cjjj", (void *)&SC_SetElementAt3_char2, true },
    { "_Z20rsSetElementAt_char313rs_allocationDv3_cjjj", (void *)&SC_SetElementAt3_char3, true },
    { "_Z20rsSetElementAt_char413rs_allocationDv4_cjjj", (void *)&SC_SetElementAt3_char4, true },

    { "_Z21rsSetElementAt_ushort13rs_allocationht", (void *)&SC_SetElementAt1_ushort, true },
    { "_Z22rsSetElementAt_ushort213rs_allocationDv2_tj", (void *)&SC_SetElementAt1_ushort2, true },
    { "_Z22rsSetElementAt_ushort313rs_allocationDv3_tj", (void *)&SC_SetElementAt1_ushort3, true },
    { "_Z22rsSetElementAt_ushort413rs_allocationDv4_tj", (void *)&SC_SetElementAt1_ushort4, true },
    { "_Z21rsSetElementAt_ushort13rs_allocationtjj", (void *)&SC_SetElementAt2_ushort, true },
    { "_Z22rsSetElementAt_ushort213rs_allocationDv2_tjj", (void *)&SC_SetElementAt2_ushort2, true },
    { "_Z22rsSetElementAt_ushort313rs_allocationDv3_tjj", (void *)&SC_SetElementAt2_ushort3, true },
    { "_Z22rsSetElementAt_ushort413rs_allocationDv4_tjj", (void *)&SC_SetElementAt2_ushort4, true },
    { "_Z21rsSetElementAt_ushort13rs_allocationtjjj", (void *)&SC_SetElementAt3_ushort, true },
    { "_Z22rsSetElementAt_ushort213rs_allocationDv2_tjjj", (void *)&SC_SetElementAt3_ushort2, true },
    { "_Z22rsSetElementAt_ushort313rs_allocationDv3_tjjj", (void *)&SC_SetElementAt3_ushort3, true },
    { "_Z22rsSetElementAt_ushort413rs_allocationDv4_tjjj", (void *)&SC_SetElementAt3_ushort4, true },

    { "_Z20rsSetElementAt_short13rs_allocationsj", (void *)&SC_SetElementAt1_short, true },
    { "_Z21rsSetElementAt_short213rs_allocationDv2_sj", (void *)&SC_SetElementAt1_short2, true },
    { "_Z21rsSetElementAt_short313rs_allocationDv3_sj", (void *)&SC_SetElementAt1_short3, true },
    { "_Z21rsSetElementAt_short413rs_allocationDv4_sj", (void *)&SC_SetElementAt1_short4, true },
    { "_Z20rsSetElementAt_short13rs_allocationsjj", (void *)&SC_SetElementAt2_short, true },
    { "_Z21rsSetElementAt_short213rs_allocationDv2_sjj", (void *)&SC_SetElementAt2_short2, true },
    { "_Z21rsSetElementAt_short313rs_allocationDv3_sjj", (void *)&SC_SetElementAt2_short3, true },
    { "_Z21rsSetElementAt_short413rs_allocationDv4_sjj", (void *)&SC_SetElementAt2_short4, true },
    { "_Z20rsSetElementAt_short13rs_allocationsjjj", (void *)&SC_SetElementAt3_short, true },
    { "_Z21rsSetElementAt_short213rs_allocationDv2_sjjj", (void *)&SC_SetElementAt3_short2, true },
    { "_Z21rsSetElementAt_short313rs_allocationDv3_sjjj", (void *)&SC_SetElementAt3_short3, true },
    { "_Z21rsSetElementAt_short413rs_allocationDv4_sjjj", (void *)&SC_SetElementAt3_short4, true },

    { "_Z19rsSetElementAt_uint13rs_allocationjj", (void *)&SC_SetElementAt1_uint, true },
    { "_Z20rsSetElementAt_uint213rs_allocationDv2_jj", (void *)&SC_SetElementAt1_uint2, true },
    { "_Z20rsSetElementAt_uint313rs_allocationDv3_jj", (void *)&SC_SetElementAt1_uint3, true },
    { "_Z20rsSetElementAt_uint413rs_allocationDv4_jj", (void *)&SC_SetElementAt1_uint4, true },
    { "_Z19rsSetElementAt_uint13rs_allocationjjj", (void *)&SC_SetElementAt2_uint, true },
    { "_Z20rsSetElementAt_uint213rs_allocationDv2_jjj", (void *)&SC_SetElementAt2_uint2, true },
    { "_Z20rsSetElementAt_uint313rs_allocationDv3_jjj", (void *)&SC_SetElementAt2_uint3, true },
    { "_Z20rsSetElementAt_uint413rs_allocationDv4_jjj", (void *)&SC_SetElementAt2_uint4, true },
    { "_Z19rsSetElementAt_uint13rs_allocationjjjj", (void *)&SC_SetElementAt3_uint, true },
    { "_Z20rsSetElementAt_uint213rs_allocationDv2_jjjj", (void *)&SC_SetElementAt3_uint2, true },
    { "_Z20rsSetElementAt_uint313rs_allocationDv3_jjjj", (void *)&SC_SetElementAt3_uint3, true },
    { "_Z20rsSetElementAt_uint413rs_allocationDv4_jjjj", (void *)&SC_SetElementAt3_uint4, true },

    { "_Z19rsSetElementAt_int13rs_allocationij", (void *)&SC_SetElementAt1_int, true },
    { "_Z19rsSetElementAt_int213rs_allocationDv2_ij", (void *)&SC_SetElementAt1_int2, true },
    { "_Z19rsSetElementAt_int313rs_allocationDv3_ij", (void *)&SC_SetElementAt1_int3, true },
    { "_Z19rsSetElementAt_int413rs_allocationDv4_ij", (void *)&SC_SetElementAt1_int4, true },
    { "_Z18rsSetElementAt_int13rs_allocationijj", (void *)&SC_SetElementAt2_int, true },
    { "_Z19rsSetElementAt_int213rs_allocationDv2_ijj", (void *)&SC_SetElementAt2_int2, true },
    { "_Z19rsSetElementAt_int313rs_allocationDv3_ijj", (void *)&SC_SetElementAt2_int3, true },
    { "_Z19rsSetElementAt_int413rs_allocationDv4_ijj", (void *)&SC_SetElementAt2_int4, true },
    { "_Z18rsSetElementAt_int13rs_allocationijjj", (void *)&SC_SetElementAt3_int, true },
    { "_Z19rsSetElementAt_int213rs_allocationDv2_ijjj", (void *)&SC_SetElementAt3_int2, true },
    { "_Z19rsSetElementAt_int313rs_allocationDv3_ijjj", (void *)&SC_SetElementAt3_int3, true },
    { "_Z19rsSetElementAt_int413rs_allocationDv4_ijjj", (void *)&SC_SetElementAt3_int4, true },

    { "_Z20rsSetElementAt_ulong13rs_allocationmt", (void *)&SC_SetElementAt1_ulong, true },
    { "_Z21rsSetElementAt_ulong213rs_allocationDv2_mj", (void *)&SC_SetElementAt1_ulong2, true },
    { "_Z21rsSetElementAt_ulong313rs_allocationDv3_mj", (void *)&SC_SetElementAt1_ulong3, true },
    { "_Z21rsSetElementAt_ulong413rs_allocationDv4_mj", (void *)&SC_SetElementAt1_ulong4, true },
    { "_Z20rsSetElementAt_ulong13rs_allocationmjj", (void *)&SC_SetElementAt2_ulong, true },
    { "_Z21rsSetElementAt_ulong213rs_allocationDv2_mjj", (void *)&SC_SetElementAt2_ulong2, true },
    { "_Z21rsSetElementAt_ulong313rs_allocationDv3_mjj", (void *)&SC_SetElementAt2_ulong3, true },
    { "_Z21rsSetElementAt_ulong413rs_allocationDv4_mjj", (void *)&SC_SetElementAt2_ulong4, true },
    { "_Z20rsSetElementAt_ulong13rs_allocationmjjj", (void *)&SC_SetElementAt3_ulong, true },
    { "_Z21rsSetElementAt_ulong213rs_allocationDv2_mjjj", (void *)&SC_SetElementAt3_ulong2, true },
    { "_Z21rsSetElementAt_ulong313rs_allocationDv3_mjjj", (void *)&SC_SetElementAt3_ulong3, true },
    { "_Z21rsSetElementAt_ulong413rs_allocationDv4_mjjj", (void *)&SC_SetElementAt3_ulong4, true },

    { "_Z19rsSetElementAt_long13rs_allocationlj", (void *)&SC_SetElementAt1_long, true },
    { "_Z20rsSetElementAt_long213rs_allocationDv2_lj", (void *)&SC_SetElementAt1_long2, true },
    { "_Z20rsSetElementAt_long313rs_allocationDv3_lj", (void *)&SC_SetElementAt1_long3, true },
    { "_Z20rsSetElementAt_long413rs_allocationDv4_lj", (void *)&SC_SetElementAt1_long4, true },
    { "_Z19rsSetElementAt_long13rs_allocationljj", (void *)&SC_SetElementAt2_long, true },
    { "_Z20rsSetElementAt_long213rs_allocationDv2_ljj", (void *)&SC_SetElementAt2_long2, true },
    { "_Z20rsSetElementAt_long313rs_allocationDv3_ljj", (void *)&SC_SetElementAt2_long3, true },
    { "_Z20rsSetElementAt_long413rs_allocationDv4_ljj", (void *)&SC_SetElementAt2_long4, true },
    { "_Z19rsSetElementAt_long13rs_allocationljjj", (void *)&SC_SetElementAt3_long, true },
    { "_Z20rsSetElementAt_long213rs_allocationDv2_ljjj", (void *)&SC_SetElementAt3_long2, true },
    { "_Z20rsSetElementAt_long313rs_allocationDv3_ljjj", (void *)&SC_SetElementAt3_long3, true },
    { "_Z20rsSetElementAt_long413rs_allocationDv4_ljjj", (void *)&SC_SetElementAt3_long4, true },

    { "_Z20rsSetElementAt_float13rs_allocationft", (void *)&SC_SetElementAt1_float, true },
    { "_Z21rsSetElementAt_float213rs_allocationDv2_fj", (void *)&SC_SetElementAt1_float2, true },
    { "_Z21rsSetElementAt_float313rs_allocationDv3_fj", (void *)&SC_SetElementAt1_float3, true },
    { "_Z21rsSetElementAt_float413rs_allocationDv4_fj", (void *)&SC_SetElementAt1_float4, true },
    { "_Z20rsSetElementAt_float13rs_allocationfjj", (void *)&SC_SetElementAt2_float, true },
    { "_Z21rsSetElementAt_float213rs_allocationDv2_fjj", (void *)&SC_SetElementAt2_float2, true },
    { "_Z21rsSetElementAt_float313rs_allocationDv3_fjj", (void *)&SC_SetElementAt2_float3, true },
    { "_Z21rsSetElementAt_float413rs_allocationDv4_fjj", (void *)&SC_SetElementAt2_float4, true },
    { "_Z20rsSetElementAt_float13rs_allocationfjjj", (void *)&SC_SetElementAt3_float, true },
    { "_Z21rsSetElementAt_float213rs_allocationDv2_fjjj", (void *)&SC_SetElementAt3_float2, true },
    { "_Z21rsSetElementAt_float313rs_allocationDv3_fjjj", (void *)&SC_SetElementAt3_float3, true },
    { "_Z21rsSetElementAt_float413rs_allocationDv4_fjjj", (void *)&SC_SetElementAt3_float4, true },

    { "_Z21rsSetElementAt_double13rs_allocationdt", (void *)&SC_SetElementAt1_double, true },
    { "_Z22rsSetElementAt_double213rs_allocationDv2_dj", (void *)&SC_SetElementAt1_double2, true },
    { "_Z22rsSetElementAt_double313rs_allocationDv3_dj", (void *)&SC_SetElementAt1_double3, true },
    { "_Z22rsSetElementAt_double413rs_allocationDv4_dj", (void *)&SC_SetElementAt1_double4, true },
    { "_Z21rsSetElementAt_double13rs_allocationdjj", (void *)&SC_SetElementAt2_double, true },
    { "_Z22rsSetElementAt_double213rs_allocationDv2_djj", (void *)&SC_SetElementAt2_double2, true },
    { "_Z22rsSetElementAt_double313rs_allocationDv3_djj", (void *)&SC_SetElementAt2_double3, true },
    { "_Z22rsSetElementAt_double413rs_allocationDv4_djj", (void *)&SC_SetElementAt2_double4, true },
    { "_Z21rsSetElementAt_double13rs_allocationdjjj", (void *)&SC_SetElementAt3_double, true },
    { "_Z22rsSetElementAt_double213rs_allocationDv2_djjj", (void *)&SC_SetElementAt3_double2, true },
    { "_Z22rsSetElementAt_double313rs_allocationDv3_djjj", (void *)&SC_SetElementAt3_double3, true },
    { "_Z22rsSetElementAt_double413rs_allocationDv4_djjj", (void *)&SC_SetElementAt3_double4, true },


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
    { "_Z23rsAllocationCopy1DRange13rs_allocationjjjS_jj", (void *)&SC_AllocationCopy1DRange, false },
    { "_Z23rsAllocationCopy2DRange13rs_allocationjjj26rs_allocation_cubemap_facejjS_jjjS0_", (void *)&SC_AllocationCopy2DRange, false },

    // Messaging

    { "_Z14rsSendToClienti", (void *)&SC_ToClient, false },
    { "_Z14rsSendToClientiPKvj", (void *)&SC_ToClient2, false },
    { "_Z22rsSendToClientBlockingi", (void *)&rsSendToClientBlocking, false },
    { "_Z22rsSendToClientBlockingiPKvj", (void *)&rsSendToClientBlocking2, false },

    { "_Z9rsForEach9rs_script13rs_allocationS0_", (void *)&SC_ForEach_SAA, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKv", (void *)&SC_ForEach_SAAU, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKvPK16rs_script_call_t", (void *)&SC_ForEach_SAAUS, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKvj", (void *)&SC_ForEach_SAAUL, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKvjPK16rs_script_call_t", (void *)&SC_ForEach_SAAULS, true },

    // time
    { "_Z6rsTimePi", (void *)&rsTime, true },
    { "_Z11rsLocaltimeP5rs_tmPKi", (void *)&rsLocaltime, true },
    { "_Z14rsUptimeMillisv", (void*)&rsUptimeMillis, true },
    { "_Z13rsUptimeNanosv", (void*)&SC_UptimeNanos, true },
    { "_Z7rsGetDtv", (void*)&SC_GetDt, false },

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

#define CLEAR_SET_OBJ(t) \
void __attribute__((overloadable)) rsClearObject(t *dst) { \
    return SC_ClearObject((ObjectBase**) dst); \
} \
void __attribute__((overloadable)) rsSetObject(t *dst, t src) { \
    return SC_SetObject((ObjectBase**) dst, (ObjectBase*) src.p); \
}

CLEAR_SET_OBJ(rs_element)
CLEAR_SET_OBJ(rs_type)
CLEAR_SET_OBJ(rs_allocation)
CLEAR_SET_OBJ(rs_sampler)
CLEAR_SET_OBJ(rs_script)
#undef CLEAR_SET_OBJ

// TODO: allocation ops, messaging, time

void rsDebug(const char *s, float f) {
    SC_debugF(s, f);
}

void rsDebug(const char *s, float f1, float f2) {
    SC_debugFv2(s, f1, f2);
}

void rsDebug(const char *s, float f1, float f2, float f3) {
    SC_debugFv3(s, f1, f2, f3);
}

void rsDebug(const char *s, float f1, float f2, float f3, float f4) {
    SC_debugFv4(s, f1, f2, f3, f4);
}

void rsDebug(const char *s, float2 f) {
    SC_debugF2(s, f);
}

void rsDebug(const char *s, float3 f) {
    SC_debugF3(s, f);
}

void rsDebug(const char *s, float4 f) {
    SC_debugF4(s, f);
}

void rsDebug(const char *s, double d) {
    SC_debugD(s, d);
}

void rsDebug(const char *s, rs_matrix4x4 *m) {
    SC_debugFM4v4(s, (float *) m);
}

void rsDebug(const char *s, rs_matrix3x3 *m) {
    SC_debugFM4v4(s, (float *) m);
}

void rsDebug(const char *s, rs_matrix2x2 *m) {
    SC_debugFM4v4(s, (float *) m);
}

void rsDebug(const char *s, char c) {
    SC_debugI8(s, c);
}

void rsDebug(const char *s, char2 c) {
    SC_debugC2(s, c);
}

void rsDebug(const char *s, char3 c) {
    SC_debugC3(s, c);
}

void rsDebug(const char *s, char4 c) {
    SC_debugC4(s, c);
}

void rsDebug(const char *s, unsigned char c) {
    SC_debugU8(s, c);
}

void rsDebug(const char *s, uchar2 c) {
    SC_debugUC2(s, c);
}

void rsDebug(const char *s, uchar3 c) {
    SC_debugUC3(s, c);
}

void rsDebug(const char *s, uchar4 c) {
    SC_debugUC4(s, c);
}

void rsDebug(const char *s, short c) {
    SC_debugI16(s, c);
}

void rsDebug(const char *s, short2 c) {
    SC_debugS2(s, c);
}

void rsDebug(const char *s, short3 c) {
    SC_debugS3(s, c);
}

void rsDebug(const char *s, short4 c) {
    SC_debugS4(s, c);
}

void rsDebug(const char *s, unsigned short c) {
    SC_debugU16(s, c);
}

void rsDebug(const char *s, ushort2 c) {
    SC_debugUS2(s, c);
}

void rsDebug(const char *s, ushort3 c) {
    SC_debugUS3(s, c);
}

void rsDebug(const char *s, ushort4 c) {
    SC_debugUS4(s, c);
}

void rsDebug(const char *s, int c) {
    SC_debugI32(s, c);
}

void rsDebug(const char *s, int2 c) {
    SC_debugI2(s, c);
}

void rsDebug(const char *s, int3 c) {
    SC_debugI3(s, c);
}

void rsDebug(const char *s, int4 c) {
    SC_debugI4(s, c);
}

void rsDebug(const char *s, unsigned int c) {
    SC_debugU32(s, c);
}

void rsDebug(const char *s, uint2 c) {
    SC_debugUI2(s, c);
}

void rsDebug(const char *s, uint3 c) {
    SC_debugUI3(s, c);
}

void rsDebug(const char *s, uint4 c) {
    SC_debugUI4(s, c);
}

void rsDebug(const char *s, long c) {
    SC_debugLL64(s, c);
}

void rsDebug(const char *s, long long c) {
    SC_debugLL64(s, c);
}

void rsDebug(const char *s, long2 c) {
    SC_debugL2(s, c);
}

void rsDebug(const char *s, long3 c) {
    SC_debugL3(s, c);
}

void rsDebug(const char *s, long4 c) {
    SC_debugL4(s, c);
}

void rsDebug(const char *s, unsigned long c) {
    SC_debugULL64(s, c);
}

void rsDebug(const char *s, unsigned long long c) {
    SC_debugULL64(s, c);
}

void rsDebug(const char *s, ulong2 c) {
    SC_debugUL2(s, c);
}

void rsDebug(const char *s, ulong3 c) {
    SC_debugUL3(s, c);
}

void rsDebug(const char *s, ulong4 c) {
    SC_debugUL4(s, c);
}

void rsDebug(const char *s, const void *p) {
    SC_debugP(s, p);
}


extern const RsdCpuReference::CpuSymbol * rsdLookupRuntimeStub(Context * pContext, char const* name) {
    ScriptC *s = (ScriptC *)pContext;
    const RsdCpuReference::CpuSymbol *syms = gSyms;
    const RsdCpuReference::CpuSymbol *sym = NULL;

    if (!sym) {
        while (syms->fnPtr) {
            if (!strcmp(syms->name, name)) {
                return syms;
            }
            syms++;
        }
    }

    return NULL;
}


