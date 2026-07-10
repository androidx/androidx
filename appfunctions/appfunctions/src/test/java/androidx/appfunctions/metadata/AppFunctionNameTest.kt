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

package androidx.appfunctions.metadata

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionNameTest {

    @Test
    fun equalsAndHashCode() {
        val name1 = AppFunctionName("pkg", "func")
        val name2 = AppFunctionName("pkg", "func")
        val name3 = AppFunctionName("pkg2", "func")
        val name4 = AppFunctionName("pkg", "func2")

        assertThat(name1).isEqualTo(name2)
        assertThat(name1.hashCode()).isEqualTo(name2.hashCode())
        assertThat(name1).isNotEqualTo(name3)
        assertThat(name1.hashCode()).isNotEqualTo(name3.hashCode())
        assertThat(name1).isNotEqualTo(name4)
        assertThat(name1.hashCode()).isNotEqualTo(name4.hashCode())
    }
}
