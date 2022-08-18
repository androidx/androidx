/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.apicompiler.parser

import androidx.privacysandbox.tools.apicompiler.util.checkSourceFails
import androidx.room.compiler.processing.util.Source
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApiValidatorTest {
    @Test
    fun privateInterface_fails() {
        checkSourceFails(
            serviceInterface("private interface MySdk")
        ).containsError("Error in com.mysdk.MySdk: annotated interfaces should be public.")
    }

    @Test
    fun interfaceWithProperties_fails() {
        checkSourceFails(
            serviceInterface(
                """public interface MySdk {
                    |   val x: Int
                    |}
                """.trimMargin()
            )
        ).containsExactlyErrors(
            "Error in com.mysdk.MySdk: annotated interfaces cannot declare properties."
        )
    }

    @Test
    fun interfaceWithCompanionObject_fails() {
        checkSourceFails(
            serviceInterface(
                """interface MySdk {
                    |   companion object {
                    |       fun foo() {}
                    |   }
                    |}
                """.trimMargin()
            )
        ).containsExactlyErrors(
            "Error in com.mysdk.MySdk: annotated interfaces cannot declare companion objects."
        )
    }

    @Test
    fun interfaceWithInvalidModifier_fails() {
        checkSourceFails(
            serviceInterface(
                """external fun interface MySdk {
                    |   suspend fun apply()
                    |}
                """.trimMargin()
            )
        ).containsExactlyErrors(
            "Error in com.mysdk.MySdk: annotated interface contains invalid modifiers (external, " +
                "fun)."
        )
    }

    @Test
    fun interfaceWithGenerics_fails() {
        checkSourceFails(
            serviceInterface(
                """interface MySdk<T, U> {
                    |   suspend fun getT(): T
                    |}
                """.trimMargin()
            )
        ).containsExactlyErrors(
            "Error in com.mysdk.MySdk: annotated interfaces cannot declare type parameters (T, U)."
        )
    }

    @Test
    fun methodWithImplementation_fails() {
        checkSourceFails(serviceMethod("suspend fun foo(): Int = 1")).containsExactlyErrors(
            "Error in com.mysdk.MySdk.foo: method cannot have default implementation."
        )
    }

    @Test
    fun methodWithGenerics_fails() {
        checkSourceFails(serviceMethod("suspend fun <T> foo(): T")).containsExactlyErrors(
            "Error in com.mysdk.MySdk.foo: method cannot declare type parameters (<T>)."
        )
    }

    @Test
    fun methodWithInvalidModifiers_fails() {
        checkSourceFails(serviceMethod("suspend inline fun foo()")).containsExactlyErrors(
            "Error in com.mysdk.MySdk.foo: method contains invalid modifiers (inline)."
        )
    }

    @Test
    fun nonSuspendMethod_fails() {
        checkSourceFails(serviceMethod("fun foo()")).containsExactlyErrors(
            "Error in com.mysdk.MySdk.foo: method should be a suspend function."
        )
    }

    private fun serviceInterface(declaration: String) = Source.kotlin(
        "com/mysdk/MySdk.kt", """
            package com.mysdk
            import androidx.privacysandbox.tools.PrivacySandboxService
            @PrivacySandboxService
            $declaration
        """
    )

    private fun serviceMethod(declaration: String) = serviceInterface(
        """
            interface MySdk {
                $declaration
            }
        """
    )
}