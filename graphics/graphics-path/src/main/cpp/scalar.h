/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef PATH_SCALAR_H
#define PATH_SCALAR_H

union floatIntUnion {
    float   value;
    int32_t signBitInt;
};

static inline int32_t float2Bits(float x) noexcept {
    floatIntUnion data; // NOLINT(cppcoreguidelines-pro-type-member-init)
    data.value = x;
    return data.signBitInt;
}

constexpr bool isFloatFinite(int32_t bits) noexcept {
    constexpr int32_t kFloatBitsExponentMask = 0x7F800000;
    return (bits & kFloatBitsExponentMask) != kFloatBitsExponentMask;
}

static inline bool isFinite(float v) noexcept {
    return isFloatFinite(float2Bits(v));
}

#pragma clang diagnostic push
#pragma ide diagnostic ignored "cppcoreguidelines-narrowing-conversions"
static bool canNormalize(float dx, float dy) noexcept {
    return (isFinite(dx) && isFinite(dy)) && (dx || dy);
}
#pragma clang diagnostic pop

static bool equals(const Point& p1, const Point& p2) noexcept {
    return !canNormalize(p1.x - p2.x, p1.y - p2.y);
}

constexpr bool isFinite(const float array[], int count) noexcept {
    float prod = 0.0f;
    for (int i = 0; i < count; i++) {
        prod *= array[i];
    }
    return prod == 0.0f;
}

template<typename T>
constexpr T tabs(T value) noexcept {
    if (value < 0) {
        value = -value;
    }
    return value;
}

constexpr bool between(float a, float b, float c) noexcept {
    return (a - b) * (c - b) <= 0.0f;
}

#endif //PATH_SCALAR_H
