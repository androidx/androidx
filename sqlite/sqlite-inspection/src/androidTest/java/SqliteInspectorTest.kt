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

package androidx.sqlite.inspection

import androidx.inspection.testing.InspectorTester
import androidx.sqlite.inspection.SqliteInspectorProtocol.SampleCommand
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class SqliteInspectorTest {
    @Test
    fun test_basic_proto() {
        val command = SampleCommand.newBuilder().setParam1("p1").setParam2("p2").build()
        val commandBytes = command.toByteArray()
        val commandBack = SampleCommand.parseFrom(commandBytes)
        Truth.assertThat(commandBack).isEqualTo(command)
    }

    @Test
    fun test_basic_inject() = runBlocking {
        val inspectorTester = InspectorTester(SqliteInspectorFactory.SQLITE_INSPECTOR_ID)
        // no crash means the inspector was successfully injected
        Truth.assertThat(inspectorTester.channel.isEmpty).isTrue()
        inspectorTester.dispose()
    }
}
