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

package com.android.tools.build.jetifier.processor.transform.bytecode

import com.android.tools.build.jetifier.processor.transform.bytecode.asm.rewriteIfMethodSignature
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SignatureMappingTest {
    @Test
    fun remapSignatureString_inKotlinMetadata() {
        val typesMap = mapOf(
            "android/old/Foo" to "androidx/fancy/Foo",
            "android/old/Bar" to "androidx/fancy/Bar",
        )

        fun assertRewrite(oldSignature: String, newSignature: String) =
            assertThat(rewriteIfMethodSignature(oldSignature) { type -> typesMap[type] ?: type })
                .isEqualTo(newSignature)

        assertRewrite("()V", "()V")
        assertRewrite("(Landroid/old/Foo;)V", "(Landroidx/fancy/Foo;)V")
        assertRewrite("()Landroid/old/Foo;", "()Landroidx/fancy/Foo;")

        assertRewrite("(Landroid/unmapped/Foo;)V", "(Landroid/unmapped/Foo;)V")
        assertRewrite(
            "(Landroid/old/Bar;JLandroid/unmapped/Foo;)V",
            "(Landroidx/fancy/Bar;JLandroid/unmapped/Foo;)V"
        )
        assertRewrite(
            "(JLandroid/unmapped/Foo;I[[I)Landroid/old/Bar;",
            "(JLandroid/unmapped/Foo;I[[I)Landroidx/fancy/Bar;"
        )
        assertRewrite("([Landroid/old/Foo;)V", "([Landroidx/fancy/Foo;)V")
        assertRewrite("()[[Landroid/old/Foo;", "()[[Landroidx/fancy/Foo;")
    }
}