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

import androidx.kruth.ComparableSubject
import androidx.kruth.Fact.Companion.simpleFact
import androidx.kruth.FailureMetadata
import androidx.kruth.StringSubject
import androidx.kruth.Subject
import androidx.kruth.assertAbout
import androidx.kruth.requireNonNull

class EmployeeSubject(
    metadata: FailureMetadata = FailureMetadata(),
    actual: Employee?,
) : Subject<Employee>(actual = actual, metadata = metadata) {

    companion object {
        // User-defined entry point
        fun assertThat(employee: Employee): EmployeeSubject {
            return assertAbout(::EmployeeSubject).that(employee)
        }
    }

    // User-defined test assertion SPI below this point
    fun hasName(name: String) {
        requireNonNull(actual) { "Expected to have a $name, but was null" }
        name().isEqualTo(name)
    }

    fun hasUsername(username: String) {
        requireNonNull(actual) { "Expected to have a $username, but was null" }
        username().isEqualTo(username)
    }

    fun hasId(id: String) {
        requireNonNull(actual) { "Expected to have an $id, but was null" }
        id().isEqualTo(id)
    }

    fun hasLocation(location: Employee.Location) {
        requireNonNull(actual) { "Expected to have a $location, but was null" }
        location().isEqualTo(location)
    }

    fun isCeo() {
        requireNonNull(actual) { "Expected to be CEO, but was null" }
        if (!actual!!.isCeo) {
            failWithActual(simpleFact("expected to be CEO"))
        }
    }

    fun isNotCeo() {
        requireNonNull(actual) { "Expected to not be CEO, but was null" }
        if (actual!!.isCeo) {
            failWithActual(simpleFact("expected to not be CEO"))
        }
    }

    // Chained subjects methods below this point
    private fun name(): StringSubject {
        return check().that(actual?.name)
    }

    private fun username(): StringSubject {
        return check().that(actual?.username)
    }

    private fun id(): StringSubject {
        return check().that(actual?.id)
    }

    private fun location(): ComparableSubject<Employee.Location> {
        return check().that(actual?.location)
    }
}
