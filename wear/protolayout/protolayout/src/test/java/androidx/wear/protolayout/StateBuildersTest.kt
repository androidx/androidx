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
package androidx.wear.protolayout

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import androidx.wear.protolayout.expression.boolAppDataKey
import androidx.wear.protolayout.expression.dynamicDataMapOf
import androidx.wear.protolayout.expression.intAppDataKey
import androidx.wear.protolayout.expression.mapTo
import androidx.wear.protolayout.expression.stringAppDataKey
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StateBuildersTest {
    @Test
    fun emptyState() {
        val state = StateBuilders.State.Builder().build()

        assertThat(state.getKeyToValueMapping()).isEmpty()
    }

    @Test
    fun additionalState() {
        val boolKey = boolAppDataKey("boolValue")
        val stringKey = stringAppDataKey(("stringValue"))

        val state =
            StateBuilders.State.Builder()
                .addKeyToValueMapping(boolKey, DynamicDataValue.fromBool(true))
                .addKeyToValueMapping(stringKey, DynamicDataValue.fromString("string"))
                .build()
        assertThat(state.getKeyToValueMapping()).hasSize(2)
        assertThat(state.getKeyToValueMapping()[boolKey]?.toDynamicDataValueProto())
            .isEqualTo(DynamicDataValue.fromBool(true).toDynamicDataValueProto())
        assertThat(state.getKeyToValueMapping()[stringKey]?.toDynamicDataValueProto())
            .isEqualTo(DynamicDataValue.fromString("string").toDynamicDataValueProto())
    }

    @Test
    fun useDynamicDataMap() {
        val boolKey = boolAppDataKey("boolValue")
        val stringKey = stringAppDataKey("stringValue")

        val state =
            StateBuilders.State.Builder()
                .addToStateMap(boolKey mapTo false, stringKey mapTo "---")
                .setStateMap(dynamicDataMapOf(boolKey mapTo true, stringKey mapTo "string"))
                .build()
        assertThat(state.stateMap.size).isEqualTo(2)
        assertThat(state.stateMap[boolKey]).isEqualTo(true)
        assertThat(state.stateMap[stringKey]).isEqualTo("string")
    }

    @Test
    fun usePairs() {
        val boolKey = boolAppDataKey("boolValue")
        val stringKey = stringAppDataKey("stringValue")

        val state =
            StateBuilders.State.Builder()
                .addToStateMap(boolKey mapTo false, stringKey mapTo "---")
                .setStateMap(boolKey mapTo true, stringKey mapTo "string")
                .build()
        assertThat(state.stateMap.size).isEqualTo(2)
        assertThat(state.stateMap[boolKey]).isEqualTo(true)
        assertThat(state.stateMap[stringKey]).isEqualTo("string")
    }

    @Test
    fun buildState_stateTooLarge_throws() {
        val builder = StateBuilders.State.Builder()
        val maxStateEntryCount = StateBuilders.State.getMaxStateEntryCount()
        for (i in 0 until maxStateEntryCount) {
            builder.addKeyToValueMapping<DynamicInt32>(
                intAppDataKey(i.toString()),
                DynamicDataValue.fromInt(0)
            )
        }
        assertFailsWith<IllegalStateException> {
            builder.addKeyToValueMapping<DynamicInt32>(
                intAppDataKey((maxStateEntryCount + 1).toString()),
                DynamicDataValue.fromInt(0)
            )
        }
    }

    @Test
    fun buildState_stateSizeIsMaximum_buildSuccessfully() {
        val builder = StateBuilders.State.Builder()
        for (i in 0 until StateBuilders.State.getMaxStateEntryCount()) {
            builder.addKeyToValueMapping<DynamicInt32>(
                intAppDataKey(i.toString()),
                DynamicDataValue.fromInt(0)
            )
        }
        assertThat(builder.build().getKeyToValueMapping()).hasSize(30)
    }
}
