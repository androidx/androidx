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

package androidx.appfunctions

import androidx.appfunctions.metadata.AppFunctionName
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ObserveAppFunctionsEventTest {

    @Test
    fun appFunctionPackageChange_equalsAndHashCode() {
        val change1 = ObserveAppFunctionsEvent.MetadataChanged(setOf("pkg1", "pkg2"))
        val change2 = ObserveAppFunctionsEvent.MetadataChanged(setOf("pkg2", "pkg1"))
        val change3 = ObserveAppFunctionsEvent.MetadataChanged(setOf("pkg1"))

        assertThat(change1).isEqualTo(change2)
        assertThat(change1.hashCode()).isEqualTo(change2.hashCode())

        assertThat(change1).isNotEqualTo(change3)
        assertThat(change1.hashCode()).isNotEqualTo(change3.hashCode())
    }

    @Test
    fun appFunctionStateChange_equalsAndHashCode() {
        val name1 = AppFunctionName("pkg1", "func1")
        val name2 = AppFunctionName("pkg1", "func2")

        val change1 = ObserveAppFunctionsEvent.StatesChanged(setOf(name1, name2))
        val change2 = ObserveAppFunctionsEvent.StatesChanged(setOf(name2, name1))
        val change3 = ObserveAppFunctionsEvent.StatesChanged(setOf(name1))

        assertThat(change1).isEqualTo(change2)
        assertThat(change1.hashCode()).isEqualTo(change2.hashCode())

        assertThat(change1).isNotEqualTo(change3)
        assertThat(change1.hashCode()).isNotEqualTo(change3.hashCode())
    }
}
