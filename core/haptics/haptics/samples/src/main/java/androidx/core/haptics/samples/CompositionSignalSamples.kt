/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.haptics.samples

import androidx.annotation.Sampled
import androidx.core.haptics.signal.CompositionSignal.Companion.compositionOf
import androidx.core.haptics.signal.CompositionSignal.Companion.off
import androidx.core.haptics.signal.CompositionSignal.Companion.quickFall
import androidx.core.haptics.signal.CompositionSignal.Companion.slowRise
import androidx.core.haptics.signal.CompositionSignal.Companion.thud

/**
 * Sample showing how to create a composition signal with scaled effects and off atoms.
 */
@Sampled
fun CompositionSignalOfScaledEffectsAndOff() {
    compositionOf(
        slowRise().withAmplitudeScale(0.7f),
        quickFall().withAmplitudeScale(0.7f),
        off(durationMillis = 50),
        thud(),
    )
}
