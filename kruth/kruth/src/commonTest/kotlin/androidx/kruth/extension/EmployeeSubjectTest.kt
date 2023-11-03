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
package androidx.kruth.extension

import androidx.kruth.extension.Employee.Location
import androidx.kruth.extension.EmployeeSubject.Companion.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

class EmployeeSubjectTest {

    private val employee = Employee("jdoe", "37802", "John Doe", Location.NYC, false)

    @Test
    fun usernameEquals() {
        assertThat(employee).hasUsername("jdoe")
    }

    @Test
    fun idEquals() {
        assertThat(employee).hasId("37802")
    }

    @Test
    fun nameEquals() {
        assertThat(employee).hasName("John Doe")
    }

    @Test
    fun locationEqualsPasses() {
        assertThat(employee).hasLocation(Location.NYC)
    }

    @Test
    fun locationEqualsFails() {
        assertFailsWith<AssertionError> {
            assertThat(employee).hasLocation(Location.MTV)
        }
    }

    @Test
    fun isCeoFalsePasses() {
        assertThat(employee).isNotCeo()
    }

    @Test
    fun isCeoTrueFails() {
        assertFailsWith<AssertionError> {
            assertThat(employee).isCeo()
        }
    }
}
