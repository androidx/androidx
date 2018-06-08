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

class JavaTypeTest {
    @Test fun javaType_testFromDotVersion() {
        val type = JavaType.fromDotVersion("test.MyClass.FIELD")

        Truth.assertThat(type.fullName).isEqualTo("test/MyClass/FIELD")
    }

    @Test fun javaType_testParent() {
        val type = JavaType.fromDotVersion("test.MyClass.FIELD")
        val result = type.getParentType().toDotNotation()

        Truth.assertThat(result).isEqualTo("test.MyClass")
    }

    @Test fun javaType_testParent_identity() {
        val type = JavaType.fromDotVersion("test")
        val result = type.getParentType().toDotNotation()

        Truth.assertThat(result).isEqualTo("test")
    }

    @Test fun javaType_remapeWithNewRootType() {
        val type = JavaType.fromDotVersion("test.MyClass\$Inner")
        val remapWith = JavaType.fromDotVersion("hello.NewClass")

        Truth.assertThat(type.remapWithNewRootType(remapWith).toDotNotation())
                .isEqualTo("hello.NewClass\$Inner")
    }
}