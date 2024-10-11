/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.activity.compose.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LocalContextCastIssueDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = LocalContextCastIssueDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(LocalContextCastIssueDetector.ContextCastToActivity)

    val LocalActivityStub =
        bytecodeStub(
            "LocalActivity.kt",
            "androidx/activity/compose",
            0xfc1de7e2,
            """
            package androidx.activity.compose

            import android.app.Activity
            import androidx.compose.runtime.compositionLocalOf

            val LocalActivity = compositionLocalOf<Activity>()
        """
                .trimIndent(),
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2XLTQrCMBAF4BFBcFwUAoKCK5dS4hmKy7r0AiGNMJA/kqnU
                2xticOPAbN77HgCsAWBVfgvtsMej8lMKNC1SaaYX8Vvq4GLIRnT3oJUdWjwy
                Sjz8dEMyzZ7JGSFuNSCm4Ouu+Cue/vxMMlrFz5Cc6IZvW30ugz3uipNmUS5a
                IzYPk3nkM1zgA8GrRwe+AAAA
                """,
            """
                androidx/activity/compose/LocalActivityKt.class:
                H4sIAAAAAAAA/61STW/TQBB967itY9I2LdCPlM+0QAJSN0UgJFJVqipVskgT
                RFEvPW1sN9o0sSt7HZVbfgu/gI8L4oAijvwoxKyTgJrCpcKSd2fezns7Mzs/
                fn79BuAZHjOUReBFofTOuXCV7En1jrth9yyMfV4LXdHZHaGv1AwYQ74teoJ3
                RNDijWbbdwnNENry1YVohhelcu239FgxSgIluz5/HYU96Ylmx99LT6SSYZAK
                VBkaV2Nuj0lcnJ3xcSLVHVJcr4VRi7d91YyEDGIugiBUQjNjXg9VPenoe2cn
                Knh+pfxzyMLOwsA1Bmvb7chAqh2GTKl8xPDkn4qTOrrd8wwFdwJvnGx4/olI
                OoqhXaqdhopu4O1el58kgTssaX9kVapObfK5qld7lRwWsGgjj+sMB//5dRbG
                VRz4SnhCCcKMbi9DE8r0ktULGNipNgw6PJfaqpDlbTFsDvpz9qBvGyvG8LeM
                4nJ+0C9Yi+aiUTEq7Pv7aYsiCqaVyZua9ZRh6e8J0SxfGIPNU2p09lC2AqGS
                yGdYezOs1Ql6MpZU6e6fYWIw90KPguZrMvDrSbfpR291NxjswzCJXH9famd1
                pHF0SQFbNDlmWm5BDxJ5G7po3MQD2qcJt1J/FVPkZfCQvFuE6s/8hNyHlPto
                FKvXIX/mAt/CLObI1uwi0v7CZib7ghufkft4ScNAKVVZR5n2l4Qu0f3Lx8g4
                WHGw6lC2aw4lctvBHdw9BotxD/ePMRUjG8OOUYy1Pf0LK8v1wXoEAAA=
                """
        )

    val LocalContextStub =
        bytecodeStub(
            "AndroidLocals.kt",
            "androidx/compose/ui/platform",
            0xac8d2426,
            """
            package androidx.compose.ui.platform

            import android.content.Context
            import androidx.compose.runtime.staticCompositionLocalOf

            val LocalContext = staticCompositionLocalOf<Context>()
           """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uOSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFRJyBgtklmTm5/nkJyfmeJdw6XPJYKgvzdQryEksScsvyhXid4TI
                gtUXAzWIcnED1emlViTmFuSkCrGFpBaXeJcoMWgxAACzjsPdkAAAAA==
                """,
            """
                androidx/compose/ui/platform/AndroidLocalsKt.class:
                H4sIAAAAAAAA/61Ty07bQBQ94xhw3JQE+oBAHxRoG9oKh6pVpQYhISQkq+Eh
                qNiwmthONMGZQfY4Ysm39Av62FRdVKjLflTVO05QBYgNqiV77uucuWfm+vef
                Hz8BvMELhldchokS4YkXqN6xSiMvE95xzHVbJT1vfZBsqoDH6Qc9BsZQ6fI+
                92IuO95OqxsFFC0wlDuRzss2lNTRiWZ4V1tqXiFPMqlFL/J2E9UXIW/F0Uae
                EVoomeMbDHs3Q66egwhDPUjtDXtprBHpQlMlHa8b6VbChUw9LqXS3IBTb1vp
                7Sw2W5cuanh7IwUlFOEWYeEWg7MaxEIKvcZQqC0dMLy8lvEyjznvMsNcatoM
                Lmd32oth1OZZTG12a80jpWkfr9vvee1MBgNdm0Or3vCbl2+tcbPbKWECky4q
                uMOw+/9vaeJcyFakecg1p5jV6xdoXJn5FM0HDOzIGBYlT4Sx6mSFKwzLZ6fj
                7tmpa01bg9ex5qcqZ6czzqQ9adWtOvv1adShihnbKVRsg3rNUL22Jxr3C//A
                8hEdd3FfdCTXWRIxzO4NFPuyL1JBetf/zRWDvaFCKio3hYy2s14rSj6aM2Fw
                91WWBNGmME51yHFwhQErNEV2rnjGDBV5i0Y37uEpraMUd3K/ihHyCnhG3gOK
                msf+itLnHPt8WAuMDfFjF/AObmOcbIOeR37EcJnNvuPuN5S+XOGwUMtZFrBE
                63uK3qf9pw5R8DHto+pTt7M+NfLQxyM8PgRLMYcnhxhJUUzhpphPjT36F/cv
                7rmKBAAA
                """
        )

    val ContextStub =
        bytecodeStub(
            "Context.kt",
            "android/content",
            0x7e746f3f,
            """
            package android.content

            class Context()
        """
                .trimIndent(),
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uOSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFRJyBgtklmTm5/nkJyfmeJdw6XPJYKgvzdQryEksScsvyhXid4TI
                gtUXAzWIcnED1emlViTmFuSkCrGFpBaXeJcoMWgxAACzjsPdkAAAAA==
                """,
            """
                android/content/Context.class:
                H4sIAAAAAAAA/3VRy04CMRQ9twODjigPH4CvtbpwgLjTmCiJCQlqooYNq8JM
                tDw6CVMIS77FP3Bl4sIQl36U8XZk6+b0PO5tb9vvn49PAGc4IJSkDsaRCvxe
                pE2ojd+w68xkQIR8X06lP5T62b/v9sMeuw7BvVBamUuCc3TcziIN10MKGULK
                vKiYUGn9s+c5odAaRGaotH8bGhlII9kTo6nD45CFVQsg0ID9mbKqyiyoEQ4X
                c88TZeGJPLPFvLyY10WVrtNfr67IC1tVJ9vrLY87HRieqREFISHXUjq8m4y6
                4fhJdofsFFtRTw7bcqysXpreYzQZ98IbZUXlYaKNGoVtFStOr7SOjDQq0jFq
                EHzl5aj2BRjLrPxEA+mTd6y8MRGoMLqJ6WCXMftXgFV4Sb6XYAn7yY8Q1jjL
                duA0sd7ERhM55Jmi0EQRmx1QjC1scx7Di7ETw/0FYlRJz84BAAA=
                """
        )

    @Test
    fun errors() {
        lint()
            .files(
                kotlin(
                    """
                package com.example

                import android.app.Activity
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.platform.LocalContext

                class MyActivity: Activity()

                @Composable
                fun Test() {
                    val activity: Activity = LocalContext.current as Activity
                    val activity2 = LocalContext.current as? Activity
                    val activity3 = LocalContext.current as? MyActivity
                }
            """
                        .trimIndent()
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                LocalContextStub,
                ContextStub
            )
            .run()
            .expect(
                """
                src/com/example/MyActivity.kt:11: Error: LocalContext should not be cast to Activity, use LocalActivity instead [ContextCastToActivity]
                    val activity: Activity = LocalContext.current as Activity
                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/MyActivity.kt:12: Error: LocalContext should not be cast to Activity, use LocalActivity instead [ContextCastToActivity]
                    val activity2 = LocalContext.current as? Activity
                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/MyActivity.kt:13: Error: LocalContext should not be cast to Activity, use LocalActivity instead [ContextCastToActivity]
                    val activity3 = LocalContext.current as? MyActivity
                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                3 errors, 0 warnings
            """
                    .trimIndent()
            )
    }

    @Test
    fun clean() {
        lint()
            .files(
                kotlin(
                    """
                package com.example

                import android.app.Activity
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.LocalActivity
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.platform.LocalContext
                
                @Composable
                fun Test() {
                    val context = LocalContext.current
                    val activity: Activity = LocalActivity.current as Activity
                    val activity2 = LocalContext.current as ComponentActivity
                }
                """
                        .trimIndent()
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                LocalActivityStub,
                LocalContextStub,
                ContextStub,
                COMPONENT_ACTIVITY
            )
            .run()
            .expectClean()
    }
}
