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
@file:JvmName("HapticAttributesUtils")

package androidx.core.haptics.extensions

import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import androidx.annotation.RequiresApi
import androidx.core.haptics.HapticAttributes
import androidx.core.haptics.impl.HapticAttributesConverter

/** Returns a [VibrationAttributes] with mapped supported fields. */
@RequiresApi(Build.VERSION_CODES.R)
public fun HapticAttributes.toVibrationAttributes(): VibrationAttributes =
    HapticAttributesConverter.toVibrationAttributes(this)

/** Returns an [AudioAttributes] with mapped supported fields. */
public fun HapticAttributes.toAudioAttributes(): AudioAttributes =
    HapticAttributesConverter.toAudioAttributes(this)
