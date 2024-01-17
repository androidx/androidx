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

package androidx.work.impl.testutils

import androidx.work.Clock

/**
 * A test implementation of Clock that defaults to the system clock for compatibility with existing
 * tests, but allows an override to be set.
 */
class TestOverrideClock : Clock {
    @JvmField
    var currentTimeMillis: Long = Long.MAX_VALUE

    override fun currentTimeMillis(): Long {
        return if (currentTimeMillis == Long.MAX_VALUE) System.currentTimeMillis()
        else currentTimeMillis
    }
}
