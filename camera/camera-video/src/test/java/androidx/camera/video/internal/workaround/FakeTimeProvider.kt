/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.video.internal.workaround

import androidx.annotation.RequiresApi
import androidx.camera.video.internal.encoder.TimeProvider
import java.util.concurrent.TimeUnit

/**
 * A fake TimeProvider implementation.
 */
@RequiresApi(21)
class FakeTimeProvider(var uptimeNs: Long = 0L, var realtimeNs: Long = 0L) : TimeProvider {

    override fun uptimeUs() = TimeUnit.NANOSECONDS.toMicros(uptimeNs)

    override fun realtimeUs() = TimeUnit.NANOSECONDS.toMicros(realtimeNs)
}
