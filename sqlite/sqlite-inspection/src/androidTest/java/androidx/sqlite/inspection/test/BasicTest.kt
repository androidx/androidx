/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection.test

import androidx.sqlite.inspection.SqliteInspectorProtocol
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTest {
    @get:Rule
    val testEnvironment = SqliteInspectorTestEnvironment()

    @Test
    fun test_basic_proto() {
        val command = MessageFactory.createTrackDatabasesCommand()

        val commandBytes = command.toByteArray()
        assertThat(commandBytes).isNotEmpty()

        val commandBack = SqliteInspectorProtocol.Command.parseFrom(commandBytes)
        assertThat(commandBack).isEqualTo(command)
    }

    @Test
    fun test_basic_inject() {
        // no crash means the inspector was successfully injected
        testEnvironment.assertNoQueuedEvents()
    }

    @Test
    fun test_unset_command() = runBlocking {
        testEnvironment.sendCommand(SqliteInspectorProtocol.Command.getDefaultInstance())
            .let { response ->
                assertThat(response.hasErrorOccurred()).isEqualTo(true)
                assertThat(response.errorOccurred.content.message)
                    .contains("Unrecognised command type: ONEOF_NOT_SET")
            }
    }
}
