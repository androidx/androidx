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

#ifndef PATH_PATH_ITERATOR_H
#define PATH_PATH_ITERATOR_H

#include "Path.h"
#include "Conic.h"

class PathIterator {
public:
    enum class VerbDirection : uint8_t  {
        Forward, // API >=30
        Backward // API < 30
    };

    PathIterator(
            Point* points,
            Verb* verbs,
            float* conicWeights,
            int count,
            VerbDirection direction
    ) noexcept
            : mPoints(points),
              mVerbs(verbs),
              mConicWeights(conicWeights),
              mIndex(count),
              mCount(count),
              mDirection(direction) {
    }

    int rawCount() const noexcept { return mCount; }

    int count() noexcept;

    bool hasNext() const noexcept { return mIndex > 0; }

    Verb peek() const noexcept {
        auto verbs = mDirection == VerbDirection::Forward ? mVerbs : mVerbs - 1;
        return mIndex > 0 ? *verbs : Verb::Done;
    }

    Verb next(Point points[4]) noexcept;

private:
    const Point* mPoints;
    const Verb* mVerbs;
    const float* mConicWeights;
    int mIndex;
    const int mCount;
    const VerbDirection mDirection;
    ConicConverter mConverter;
};

#endif //PATH_PATH_ITERATOR_H
