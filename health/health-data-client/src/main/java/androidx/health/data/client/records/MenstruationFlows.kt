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

/** How heavy the user's menstruation flow was. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public object MenstruationFlows {
    const val SPOTTING = "spotting"
    const val LIGHT = "light"
    const val MEDIUM = "medium"
    const val HEAVY = "heavy"
}

/**
 * How heavy the user's menstruation flow was.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            MenstruationFlows.SPOTTING,
            MenstruationFlows.LIGHT,
            MenstruationFlows.MEDIUM,
            MenstruationFlows.HEAVY,
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class MenstruationFlow
