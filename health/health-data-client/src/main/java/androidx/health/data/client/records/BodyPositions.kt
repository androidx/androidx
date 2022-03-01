/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.data.client.records

import androidx.annotation.StringDef

/** The user's body position when a health measurement is taken. */
public object BodyPositions {
    const val STANDING_UP = "standing_up"
    const val SITTING_DOWN = "sitting_down"
    const val LYING_DOWN = "lying_down"
    const val RECLINING = "reclining"
}

/**
 * The user's body position when a health measurement is taken.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            BodyPositions.STANDING_UP,
            BodyPositions.SITTING_DOWN,
            BodyPositions.LYING_DOWN,
            BodyPositions.RECLINING,
        ]
)
annotation class BodyPosition
