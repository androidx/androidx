/*
 * Copyright 2022 The Android Open Source Project
 * Copyright (C) 2006 The Android Open Source Project
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

#include "Conic.h"

#include "scalar.h"

#include "math/vec2.h"

#include <cmath>
#include <cstring>

using namespace filament::math;

constexpr int kMaxConicToQuadCount = 5;

constexpr bool isFinite(const Point points[], int count) noexcept {
    return isFinite(&points[0].x, count << 1);
}

constexpr bool isFinite(const Point& point) noexcept {
    float a = 0.0f;
    a *= point.x;
    a *= point.y;
    return a == 0.0f;
}

constexpr Point toPoint(const float2& v) noexcept {
    return { .x = v.x, .y = v.y };
}

constexpr float2 fromPoint(const Point& v) noexcept {
    return float2{v.x, v.y};
}

int conicToQuadratics(
    const Point conicPoints[3], Point *quadraticPoints, int bufferSize,
    float weight, float tolerance
) noexcept {
    Conic conic(conicPoints[0], conicPoints[1], conicPoints[2], weight);

    int count = conic.computeQuadraticCount(tolerance);
    int quadraticCount = 1 << count;
    if (quadraticCount > bufferSize) {
        // Buffer not large enough; return necessary size to resize and try again
        return quadraticCount;
    }
    quadraticCount = conic.splitIntoQuadratics(quadraticPoints, count);

    return quadraticCount;
}

int Conic::computeQuadraticCount(float tolerance) const noexcept {
    if (tolerance <= 0.0f || !isFinite(tolerance) || !isFinite(points, 3)) return 0;

    float a = weight - 1.0f;
    float k = a / (4.0f * (2.0f + a));
    float x = k * (points[0].x - 2.0f * points[1].x + points[2].x);
    float y = k * (points[0].y - 2.0f * points[1].y + points[2].y);

    float error = std::sqrtf(x * x + y * y);
    int count = 0;
    for ( ; count < kMaxConicToQuadCount; count++) {
        if (error <= tolerance) break;
        error *= 0.25f;
    }

    return count;
}

static Point* subdivide(const Conic& src, Point pts[], int level) {
    if (level == 0) {
        memcpy(pts, &src.points[1], 2 * sizeof(Point));
        return pts + 2;
    } else {
        Conic dst[2];
        src.split(dst);
        const float startY = src.points[0].y;
        const float endY = src.points[2].y;
        if (between(startY, src.points[1].y, endY)) {
            float midY = dst[0].points[2].y;
            if (!between(startY, midY, endY)) {
                float closerY = tabs(midY - startY) < tabs(midY - endY) ? startY : endY;
                dst[0].points[2].y = dst[1].points[0].y = closerY;
            }
            if (!between(startY, dst[0].points[1].y, dst[0].points[2].y)) {
                dst[0].points[1].y = startY;
            }
            if (!between(dst[1].points[0].y, dst[1].points[1].y, endY)) {
                dst[1].points[1].y = endY;
            }
        }
        --level;
        pts = subdivide(dst[0], pts, level);
        return subdivide(dst[1], pts, level);
    }
}

void Conic::split(Conic* __restrict__ dst) const noexcept {
    float2 scale{1.0f / (1.0f + weight)};
    float newW = std::sqrtf(0.5f + weight * 0.5f);

    float2 p0 = fromPoint(points[0]);
    float2 p1 = fromPoint(points[1]);
    float2 p2 = fromPoint(points[2]);
    float2 ww(weight);

    float2 wp1 = ww * p1;
    float2 m = (p0 + (wp1 + wp1) + p2) * scale * float2(0.5f);
    Point pt = toPoint(m);
    if (!isFinite(pt)) {
        double w_d = weight;
        double w_2 = w_d * 2.0;
        double scale_half = 1.0 / (1.0 + w_d) * 0.5;
        pt.x = float((points[0].x + w_2 * points[1].x + points[2].x) * scale_half);
        pt.y = float((points[0].y + w_2 * points[1].y + points[2].y) * scale_half);
    }
    dst[0].points[0] = points[0];
    dst[0].points[1] = toPoint((p0 + wp1) * scale);
    dst[0].points[2] = dst[1].points[0] = pt;
    dst[1].points[1] = toPoint((wp1 + p2) * scale);
    dst[1].points[2] = points[2];

    dst[0].weight = dst[1].weight = newW;
}

int Conic::splitIntoQuadratics(Point dstPoints[], int count) const noexcept {
    *dstPoints = points[0];

    if (count >= kMaxConicToQuadCount) {
        Conic dst[2];
        split(dst);

        if (equals(dst[0].points[1], dst[0].points[2]) &&
                equals(dst[1].points[0], dst[1].points[1])) {
            dstPoints[1] = dstPoints[2] = dstPoints[3] = dst[0].points[1];
            dstPoints[4] = dst[1].points[2];
            count = 1;
            goto commonFinitePointCheck;
        }
    }

    subdivide(*this, dstPoints + 1, count);

commonFinitePointCheck:
    const int quadCount = 1 << count;
    const int pointCount = 2 * quadCount + 1;

    if (!isFinite(dstPoints, pointCount)) {
        for (int i = 1; i < pointCount - 1; ++i) {
            dstPoints[i] = points[1];
        }
    }

    return quadCount;
}