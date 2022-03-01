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

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef

/**
 * Types of activity event. They can be either explicitly requested by a user or auto-detected by a
 * tracking app.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public object ActivityEventTypes {
    /**
     * Explicit pause during an workout, requested by the user (by clicking a pause button in the
     * session UI). Movement happening during pause should not contribute to session metrics.
     */
    const val PAUSE = "pause"
    /**
     * Auto-detected periods of rest during an workout. There should be no user movement detected
     * during rest and any movement detected should finish rest event.
     */
    const val REST = "rest"
}

/**
 * Types of activity event. They can be either explicitly requested by a user or auto-detected by a
 * tracking app.
 *
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            ActivityEventTypes.PAUSE,
            ActivityEventTypes.REST,
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class ActivityEventType
