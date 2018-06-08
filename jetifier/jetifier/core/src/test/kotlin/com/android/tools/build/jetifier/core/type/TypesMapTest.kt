/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.core.type

import com.google.common.truth.Truth
import org.junit.Test

class TypesMapTest {

    @Test fun typesMap_mapSimpleType() {
        testRewrites(
            map = listOf(
                "test.Class" to "test2.Class2"
            ),
            from = "test.Class",
            expected = "test2.Class2"
        )
    }

    @Test fun typesMap_mapNestedType() {
        testRewrites(
            map = listOf(
                "test.Class" to "test2.Class2"
            ),
            from = "test.Class\$Inner",
                expected = "test2.Class2\$Inner"
        )
    }

    @Test fun typesMap_mapDoubleNestedType() {
        testRewrites(
            map = listOf(
                "test.Class" to "test2.Class2"
            ),
            from = "test.Class\$Inner\$1",
            expected = "test2.Class2\$Inner\$1"
        )
    }

    @Test fun typesMap_mapNotFound_returnsNull() {
        val typesMap = TypesMap.EMPTY
        val result = typesMap.mapType(JavaType.fromDotVersion("test.Class"))
        Truth.assertThat(result).isNull()
    }

    private fun testRewrites(map: List<Pair<String, String>>, from: String, expected: String) {
        val typesMap = TypesMap(map
            .map { JavaType.fromDotVersion(it.first) to JavaType.fromDotVersion(it.second) }
            .toMap())
        val result = typesMap.mapType(JavaType.fromDotVersion(from))
        Truth.assertThat(result).isEqualTo(JavaType.fromDotVersion(expected))
    }
}