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

package androidx.credentials.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CredentialManagerMutableContextDetectorTest : LintDetectorTest() {

    override fun getDetector() = CredentialManagerMutableContextDetector()

    override fun getIssues() = listOf(CredentialManagerMutableContextDetector.ISSUE)

    private val credentialManagerStub =
        kotlin(
            """
        package androidx.credentials
        import android.content.Context

        class CredentialManager {
            fun getCredential(context: Context, request: Any) {}
        }
        """
        )

    @Test
    fun testRawActivityContextThrowsError() {
        lint()
            .files(
                credentialManagerStub,
                kotlin(
                        """
                package com.example
                import androidx.credentials.CredentialManager
                import android.app.Activity

                fun doLogin(activity: Activity) {
                    val manager = CredentialManager()
                    manager.getCredential(activity, Any())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:7: Warning: Use a MutableContextWrapper instead of a raw Activity for credential operations to properly handle activity configuration changes. [CredManMutableContext]
                    manager.getCredential(activity, Any())
                                          ~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testMutableContextWrapperPasses() {
        lint()
            .files(
                credentialManagerStub,
                kotlin(
                        """
                package com.example
                import androidx.credentials.CredentialManager
                import android.app.Activity
                import android.content.MutableContextWrapper

                fun doLogin(activity: Activity) {
                    val manager = CredentialManager()
                    val wrapper = MutableContextWrapper(activity)
                    manager.getCredential(wrapper, Any())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testMutableContextWrapperAsGenericContextPasses() {
        lint()
            .files(
                credentialManagerStub,
                kotlin(
                        """
                package com.example
                import androidx.credentials.CredentialManager
                import android.app.Activity
                import android.content.Context
                import android.content.MutableContextWrapper

                fun doLogin(activity: Activity) {
                    val manager = CredentialManager()
                    val context: Context = MutableContextWrapper(activity)
                    manager.getCredential(context, Any())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }
}
