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

#ifndef PATH_PATH_H
#define PATH_PATH_H

#include <stdint.h>

// The following structures declare the minimum we need + a marker (generationId) to
// validate the data during debugging. There may be more fields in the Skia structures
// but we just ignore them for now. Some fields declared in older API levels (isFinite
// for instance) may not show up in the declarations for newer API levels if the field
// still exist but was moved after the data we need.

enum class Verb : uint8_t {
    Move,
    Line,
    Quadratic,
    Conic,
    Cubic,
    Close,
    Done
};

struct Point {
    float x;
    float y;
};

struct PathRef21 {
    __unused intptr_t pointer;      // Virtual tables
    __unused int32_t refCount;
    __unused float left;
    __unused float top;
    __unused float right;
    __unused float bottom;
    __unused uint8_t segmentMask;    // Some of the unused fields are in a different order in 22/23
    __unused uint8_t boundsIsDirty;
    __unused uint8_t isFinite;
    __unused uint8_t isOval;
             Point* points;
             Verb* verbs;
             int verbCount;
    __unused int pointCount;
    __unused size_t freeSpace;
             float* conicWeights;
    __unused int conicWeightsReserve;
    __unused int conicWeightsCount;
    __unused uint32_t generationId;
};

struct PathRef24 {
    __unused intptr_t pointer;
    __unused int32_t refCount;
    __unused float left;
    __unused float top;
    __unused float right;
    __unused float bottom;
             Point* points;
             Verb* verbs;
             int verbCount;
    __unused int pointCount;
    __unused size_t freeSpace;
             float* conicWeights;
    __unused int conicWeightsReserve;
    __unused int conicWeightsCount;
    __unused uint32_t generationId;
};

struct PathRef26 {
    __unused int32_t refCount;
    __unused float left;
    __unused float top;
    __unused float right;
    __unused float bottom;
             Point* points;
             Verb* verbs;
             int verbCount;
    __unused int pointCount;
    __unused size_t freeSpace;
             float* conicWeights;
    __unused int conicWeightsReserve;
    __unused int conicWeightsCount;
    __unused uint32_t generationId;
};

struct PathRef30 {
    __unused int32_t refCount;
    __unused float left;
    __unused float top;
    __unused float right;
    __unused float bottom;
             Point* points;
    __unused int pointReserve;
    __unused int pointCount;
             Verb* verbs;
    __unused int verbReserve;
             int verbCount;
             float* conicWeights;
    __unused int conicWeightsReserve;
    __unused int conicWeightsCount;
    __unused uint32_t generationId;
};

struct Path {
    PathRef21* pathRef;
};

#endif //PATH_PATH_H
