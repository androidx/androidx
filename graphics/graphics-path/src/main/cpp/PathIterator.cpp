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

#include "PathIterator.h"

int PathIterator::count() noexcept {
    int count = 0;
    const Verb* verbs = mVerbs;
    const Point* points = mPoints;
    const float* conicWeights = mConicWeights;

    for (int i = 0; i < mCount; i++) {
        Verb verb = *(mDirection == VerbDirection::Forward ? verbs++ : --verbs);
        switch (verb) {
            case Verb::Move:
            case Verb::Line:
                points += 1;
                count++;
                break;
            case Verb::Quadratic:
                points += 2;
                count++;
                break;
            case Verb::Conic:
                points += 2;
                count++;
                break;
            case Verb::Cubic:
                points += 3;
                count++;
                break;
            case Verb::Close:
            case Verb::Done:
                count++;
                break;
        }
    }

    return count;
}

Verb PathIterator::next(Point points[4]) noexcept {
    if (mIndex <= 0) {
        return Verb::Done;
    }
    mIndex--;

    Verb verb = *(mDirection == VerbDirection::Forward ? mVerbs++ : --mVerbs);
    switch (verb) {
        case Verb::Move:
            points[0] = mPoints[0];
            mPoints += 1;
            break;
        case Verb::Line:
            points[0] = mPoints[-1];
            points[1] = mPoints[0];
            mPoints += 1;
            break;
        case Verb::Quadratic:
            points[0] = mPoints[-1];
            points[1] = mPoints[0];
            points[2] = mPoints[1];
            mPoints += 2;
            break;
        case Verb::Conic:
            points[0] = mPoints[-1];
            points[1] = mPoints[0];
            points[2] = mPoints[1];
            points[3].x = *mConicWeights;
            points[3].y = *mConicWeights;
            mConicWeights++;
            mPoints += 2;
            break;
        case Verb::Cubic:
            points[0] = mPoints[-1];
            points[1] = mPoints[0];
            points[2] = mPoints[1];
            points[3] = mPoints[2];
            mPoints += 3;
            break;
        case Verb::Close:
        case Verb::Done:
            break;
    }

    return verb;
}
