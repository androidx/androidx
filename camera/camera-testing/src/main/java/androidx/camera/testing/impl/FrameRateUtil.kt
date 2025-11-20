/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.testing.impl

import android.util.Range

public object FrameRateUtil {
    public const val FPS_30: Int = 30
    public const val FPS_120: Int = 120
    public const val FPS_240: Int = 240
    public const val FPS_480: Int = 480

    public val FPS_30_30: Range<Int> = Range(FPS_30, FPS_30)
    public val FPS_30_120: Range<Int> = Range(FPS_30, FPS_120)
    public val FPS_120_120: Range<Int> = Range(FPS_120, FPS_120)
    public val FPS_30_240: Range<Int> = Range(FPS_30, FPS_240)
    public val FPS_240_240: Range<Int> = Range(FPS_240, FPS_240)
    public val FPS_30_480: Range<Int> = Range(FPS_30, FPS_480)
    public val FPS_480_480: Range<Int> = Range(FPS_480, FPS_480)
}
