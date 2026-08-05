/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.tokens

import kotlin.jvm.JvmInline

@JvmInline internal value class MotionSchemeToken(val id: Int)

internal object MotionSchemeKeyTokens {
    val DefaultSpatial = MotionSchemeToken(0)
    val FastSpatial = MotionSchemeToken(1)
    val SlowSpatial = MotionSchemeToken(2)
    val DefaultEffects = MotionSchemeToken(3)
    val FastEffects = MotionSchemeToken(4)
    val SlowEffects = MotionSchemeToken(5)
}
