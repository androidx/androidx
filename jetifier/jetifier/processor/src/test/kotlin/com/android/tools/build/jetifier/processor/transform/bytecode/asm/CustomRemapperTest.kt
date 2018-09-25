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

package com.android.tools.build.jetifier.processor.transform.bytecode.asm

import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.processor.transform.bytecode.CoreRemapper
import com.google.common.truth.Truth
import org.junit.Test

class CustomRemapperTest {

    class FakeCoreRemapper : CoreRemapper {

        override fun rewriteType(type: JavaType): JavaType {
            return JavaType(type.fullName + "/test")
        }

        override fun rewriteString(value: String): String {
            return value
        }
    }

    @Test
    fun remapConstantPoolReference() {
        val remapper = CustomRemapper(FakeCoreRemapper())

        Truth
            .assertThat(remapper.mapValue("LHello/world;"))
            .isEqualTo("LHello/world/test;")

        Truth
            .assertThat(remapper.mapValue("LHello.world;"))
            .isEqualTo("LHello.world.test;")
    }

    @Test
    fun remapArrayOfConstantPoolReferences() {
        val remapper = CustomRemapper(FakeCoreRemapper())

        Truth
            .assertThat(remapper.mapValue("LHello/world;LHello2/world2;"))
            .isEqualTo("LHello/world/test;LHello2/world2/test;")

        Truth
            .assertThat(remapper.mapValue("LHello.world;LHello2.world2;"))
            .isEqualTo("LHello.world.test;LHello2.world2.test;")
    }

    @Test
    fun remapPoolReference_mixedSymbols_shouldSkip() {
        val remapper = CustomRemapper(FakeCoreRemapper())

        Truth
            .assertThat(remapper.mapValue("LHello/world.mixed;"))
            .isEqualTo("LHello/world.mixed;")

        Truth
            .assertThat(remapper.mapValue("LHello.world/mixed;"))
            .isEqualTo("LHello.world/mixed;")
    }
}