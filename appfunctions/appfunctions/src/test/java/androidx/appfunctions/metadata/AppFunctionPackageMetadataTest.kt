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

class AppFunctionPackageMetadataTest {

    @Test
    fun equalsAndHashCode() {
        val components =
            AppFunctionComponentsMetadata(
                dataTypes = mapOf("test" to AppFunctionStringTypeMetadata(true))
            )
        val pkg1 = AppFunctionPackageMetadata("pkg", components)
        val pkg2 = AppFunctionPackageMetadata("pkg", components)
        val pkg3 = AppFunctionPackageMetadata("pkg2", components)
        val pkg4 = AppFunctionPackageMetadata("pkg", AppFunctionComponentsMetadata())

        assertThat(pkg1).isEqualTo(pkg2)
        assertThat(pkg1.hashCode()).isEqualTo(pkg2.hashCode())
        assertThat(pkg1).isNotEqualTo(pkg3)
        assertThat(pkg1.hashCode()).isNotEqualTo(pkg3.hashCode())
        assertThat(pkg1).isNotEqualTo(pkg4)
        assertThat(pkg1.hashCode()).isNotEqualTo(pkg4.hashCode())
    }

    @Test
    fun toString_containsAllFields() {
        val components =
            AppFunctionComponentsMetadata(
                dataTypes = mapOf("test" to AppFunctionStringTypeMetadata(true))
            )
        val pkg = AppFunctionPackageMetadata("pkg", components)

        val toString = pkg.toString()

        assertThat(toString).contains("packageName='pkg'")
        assertThat(toString).contains("appFunctions=[]")
        assertThat(toString).contains("components=$components")
    }
}
