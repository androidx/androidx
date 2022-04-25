/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.complications

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KProperty

@RunWith(SharedRobolectricTestRunner::class)
public class SystemDataSourcesTest {

    @Test
    fun dataSourceIdsAreUnique() {
        val valuesMap = HashMap<Int, String>()
        SystemDataSources.Companion::class.members.forEach { member ->
            if (member !is KProperty) {
                // Only consider properties.
                return
            }
            // Must be final and const.
            assertWithMessage("${member.name} should be final")
                .that(member.isFinal)
                .isTrue()
            assertWithMessage("${member.name} should be const")
                .that(member.isConst)
                .isTrue()
            when (val value = member.getter.call(SystemDataSources.Companion)) {
                is Int -> {
                    valuesMap[value]?.let {
                        fail("${member.name} duplicates value of ${valuesMap[value]}")
                    }
                    valuesMap[value] = member.name
                }
                else -> fail("${member.name} should be an int")
            }
        }
    }
}