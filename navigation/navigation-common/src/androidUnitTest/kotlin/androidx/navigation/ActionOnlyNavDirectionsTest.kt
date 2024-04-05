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

package androidx.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ActionOnlyNavDirectionsTest {

    @Test
    fun testEquals() {
        assertThat(ActionOnlyNavDirections(1))
            .isEqualTo(ActionOnlyNavDirections(1))
        assertThat(ActionOnlyNavDirections(1))
            .isNotEqualTo(ActionOnlyNavDirections(2))
    }

    @Test
    fun testHashCode() {
        assertThat(ActionOnlyNavDirections(1).hashCode())
            .isEqualTo(ActionOnlyNavDirections(1).hashCode())
        assertThat(ActionOnlyNavDirections(1).hashCode())
            .isNotEqualTo(ActionOnlyNavDirections(2).hashCode())
    }
}
