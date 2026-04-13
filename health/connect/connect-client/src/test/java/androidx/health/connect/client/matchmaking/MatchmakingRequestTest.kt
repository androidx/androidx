/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.health.connect.client.matchmaking

import androidx.health.connect.client.ExperimentalMatchmakingApi
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMatchmakingApi::class)
@RunWith(AndroidJUnit4::class)
class MatchmakingRequestTest {

    @Test
    fun validRequest_success() {
        MatchmakingRequest(
            recordTypes = setOf(StepsRecord::class),
            includedDataSources = setOf(DataOrigin("com.example.app")),
        )
    }

    @Test
    fun validRequest_excludedDataSources_success() {
        MatchmakingRequest(
            recordTypes = setOf(StepsRecord::class),
            excludedDataSources = setOf(DataOrigin("com.example.app")),
        )
    }

    @Test
    fun bothIncludedAndExcludedDataSourcesSet_throws() {
        assertFailsWith<IllegalArgumentException> {
            MatchmakingRequest(
                recordTypes = setOf(StepsRecord::class),
                includedDataSources = setOf(DataOrigin("com.example.app1")),
                excludedDataSources = setOf(DataOrigin("com.example.app2")),
            )
        }
    }
}
