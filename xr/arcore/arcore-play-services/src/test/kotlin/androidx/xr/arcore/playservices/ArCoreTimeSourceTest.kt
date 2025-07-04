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

package androidx.xr.arcore.playservices

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArCoreTimeSourceTest {

    private lateinit var underTest: ArCoreTimeSource

    @Before
    fun beforeTest() {
        underTest = ArCoreTimeSource()
    }

    @Test
    fun read_usesTheArCoreClock() {
        val updateTime = 1000L
        val mark = underTest.markNow()
        checkNotNull(mark)
        check(mark.elapsedNow().inWholeNanoseconds == 0L)

        underTest.update(updateTime)
        assertThat(mark.elapsedNow().inWholeNanoseconds).isEqualTo(updateTime)
    }
}
