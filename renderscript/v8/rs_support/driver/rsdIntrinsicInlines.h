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



typedef uint8_t uchar;
typedef uint16_t ushort;
typedef uint32_t uint;

typedef float float2 __attribute__((ext_vector_type(2)));
typedef float float3 __attribute__((ext_vector_type(3)));
typedef float float4 __attribute__((ext_vector_type(4)));
typedef uchar uchar2 __attribute__((ext_vector_type(2)));
typedef uchar uchar3 __attribute__((ext_vector_type(3)));
typedef uchar uchar4 __attribute__((ext_vector_type(4)));
typedef ushort ushort2 __attribute__((ext_vector_type(2)));
typedef ushort ushort3 __attribute__((ext_vector_type(3)));
typedef ushort ushort4 __attribute__((ext_vector_type(4)));
typedef uint uint2 __attribute__((ext_vector_type(2)));
typedef uint uint3 __attribute__((ext_vector_type(3)));
typedef uint uint4 __attribute__((ext_vector_type(4)));
typedef char char2 __attribute__((ext_vector_type(2)));
typedef char char3 __attribute__((ext_vector_type(3)));
typedef char char4 __attribute__((ext_vector_type(4)));
typedef short short2 __attribute__((ext_vector_type(2)));
typedef short short3 __attribute__((ext_vector_type(3)));
typedef short short4 __attribute__((ext_vector_type(4)));
typedef int int2 __attribute__((ext_vector_type(2)));
typedef int int3 __attribute__((ext_vector_type(3)));
typedef int int4 __attribute__((ext_vector_type(4)));
typedef long long2 __attribute__((ext_vector_type(2)));
typedef long long3 __attribute__((ext_vector_type(3)));
typedef long long4 __attribute__((ext_vector_type(4)));

enum IntrinsicEnums {
    INTRINSIC_UNDEFINED,
    INTRINSIC_CONVOLVE_3x3,
    INTRINXIC_COLORMATRIX

};

static inline int4 convert_int4(uchar4 i) {
    int4 f4 = {i.x, i.y, i.z, i.w};
    return f4;
}

static inline short4 convert_short4(uchar4 i) {
    short4 f4 = {i.x, i.y, i.z, i.w};
    return f4;
}

static inline float4 convert_float4(uchar4 i) {
    float4 f4 = {i.x, i.y, i.z, i.w};
    return f4;
}

static inline uchar4 convert_uchar4(short4 i) {
    uchar4 f4 = {(uchar)i.x, (uchar)i.y, (uchar)i.z, (uchar)i.w};
    return f4;
}

static inline uchar4 convert_uchar4(int4 i) {
    uchar4 f4 = {(uchar)i.x, (uchar)i.y, (uchar)i.z, (uchar)i.w};
    return f4;
}

static inline uchar4 convert_uchar4(float4 i) {
    uchar4 f4 = {(uchar)i.x, (uchar)i.y, (uchar)i.z, (uchar)i.w};
    return f4;
}


static inline int4 clamp(int4 amount, int low, int high) {
    int4 r;
    r.x = amount.x < low ? low : (amount.x > high ? high : amount.x);
    r.y = amount.y < low ? low : (amount.y > high ? high : amount.y);
    r.z = amount.z < low ? low : (amount.z > high ? high : amount.z);
    r.w = amount.w < low ? low : (amount.w > high ? high : amount.w);
    return r;
}

static inline float4 clamp(float4 amount, float low, float high) {
    float4 r;
    r.x = amount.x < low ? low : (amount.x > high ? high : amount.x);
    r.y = amount.y < low ? low : (amount.y > high ? high : amount.y);
    r.z = amount.z < low ? low : (amount.z > high ? high : amount.z);
    r.w = amount.w < low ? low : (amount.w > high ? high : amount.w);
    return r;
}


