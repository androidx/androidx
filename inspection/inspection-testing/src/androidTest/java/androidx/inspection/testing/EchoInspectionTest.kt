/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.inspection.testing

import androidx.inspection.testing.echo.ECHO_INSPECTION_ID
import androidx.inspection.testing.echo.TickleManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class EchoInspectionTest {

    @Test
    fun pingPongTest() = runBlocking {
        val inspectorTester = InspectorTester(ECHO_INSPECTION_ID)
        assertThat(inspectorTester.channel.isEmpty).isTrue()
        fakeCallCodeInApp()
        val event1 = inspectorTester.channel.receive()
        assertThat(event1).isEqualTo(byteArrayOf(1))
        val message = byteArrayOf(1, 2, 3)
        assertThat(inspectorTester.sendCommand(message)).isEqualTo(message)
        fakeCallCodeInApp()
        val event2 = inspectorTester.channel.receive()
        assertThat(event2).isEqualTo(byteArrayOf(2))
        inspectorTester.dispose()
    }

    internal fun fakeCallCodeInApp() = TickleManager.tickle()
}