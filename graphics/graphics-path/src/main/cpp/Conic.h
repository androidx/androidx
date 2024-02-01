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

#ifndef PATH_CONIC_H
#define PATH_CONIC_H

#include "Path.h"

constexpr int kMaxConicToQuadCount = 5;
constexpr int kMaxQuadraticCount = 1 << kMaxConicToQuadCount;

int conicToQuadratics(
        const Point conicPoints[3], Point *quadraticPoints, int bufferSize,
        float weight, float tolerance
) noexcept;

class ConicConverter {
public:
    ConicConverter() noexcept { }

    const Point* toQuadratics(const Point points[3], float weight, float tolerance = 0.25f) noexcept;

    int quadraticCount() const noexcept { return mQuadraticCount; }

    const Point* quadratics() const noexcept {
        return mQuadraticCount > 0 ? mStorage : nullptr;
    }

private:
    int mQuadraticCount = 0;
    Point mStorage[1 + 2 * kMaxQuadraticCount];
};

struct Conic {
    Conic() noexcept { }

    Conic(Point p0, Point p1, Point p2, float weight) noexcept {
        points[0] = p0;
        points[1] = p1;
        points[2] = p2;
        this->weight = weight;
    }

    void split(Conic* __restrict__ dst) const noexcept;
    int computeQuadraticCount(float tolerance) const noexcept;
    int splitIntoQuadratics(Point dstPoints[], int count) const noexcept;

    Point points[3];
    float weight;
};

#endif //PATH_CONIC_H
