/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.appcompat.lint.res

import androidx.appcompat.lint.Stubs
import androidx.appcompat.res.ColorStateListLoadingDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.manifest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class ColorStateListLoadingDetectorTest {
    @Test
    fun testCustomGetColorStateList() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.content.res.ColorStateList
            import android.os.Bundle
            import androidx.appcompat.app.AppCompatActivity
            import androidx.appcompat.content.res.AppCompatResources

            class ResourceLoader {
                private fun getColorStateList(resourceId: Int): ColorStateList {
                    return AppCompatResources.getColorStateList(CustomActivity.this, resourceId)
                }
            }

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                   ResourceLoader().getColorStateList(R.color.color_state_list)
                }
            }
            """
        ).indented().within("src")

        // We expect a clean Lint run since the call to getColorStateList in activity's onCreate
        // is on our own custom inner class
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            Stubs.APPCOMPAT_RESOURCES,
            Stubs.COLOR_STATE_LIST,
            customActivity
        ).issues(ColorStateListLoadingDetector.NOT_USING_COMPAT_LOADING)
            .addTestModes(TestMode.DEFAULT, TestMode.PARTIAL)
            .run()
            .expectClean()
    }

    @Test
    fun testCoreGetColorStateListApi24() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.os.Bundle
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    getResources().getColorStateList(R.color.color_state_list)
                }
            }
            """
        ).indented().within("src")

        // Manifest that sets min sdk to 24
        val manifest = manifest(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example">
                    <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="29" />
                    <application
                        android:hardwareAccelerated="true"
                        android:icon="@android:drawable/ic_delete"
                        android:label="Sample app"
                        android:allowBackup="false"
                        android:supportsRtl="true"
                        android:theme="@style/Theme.AppCompat">
                        <activity android:name=".CustomActivity"/>
                    </application>
                </manifest>
            """.trimIndent()
        )

        // We expect the call to Resources.getColorStateList to be flagged to use ContextCompat
        // loading
        /* ktlint-disable max-line-length */
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            Stubs.COLOR_STATE_LIST,
            manifest,
            customActivity
        ).issues(ColorStateListLoadingDetector.NOT_USING_COMPAT_LOADING)
            .addTestModes(TestMode.DEFAULT, TestMode.PARTIAL)
            .run()
            .expect(
                """
src/com/example/CustomActivity.kt:8: Warning: Use ContextCompat.getColorStateList() [UseCompatLoadingForColorStateLists]
        getResources().getColorStateList(R.color.color_state_list)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testCoreGetColorStateListApi14() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.os.Bundle
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    getResources().getColorStateList(R.color.color_state_list)
                }
            }
            """
        ).indented().within("src")

        // Manifest that sets min sdk to 14
        val manifest = manifest(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example">
                    <uses-sdk android:minSdkVersion="14" android:targetSdkVersion="29" />
                    <application
                        android:hardwareAccelerated="true"
                        android:icon="@android:drawable/ic_delete"
                        android:label="Sample app"
                        android:allowBackup="false"
                        android:supportsRtl="true"
                        android:theme="@style/Theme.AppCompat">
                        <activity android:name=".CustomActivity"/>
                    </application>
                </manifest>
            """.trimIndent()
        )

        // We expect the call to Resources.getColorStateList to be flagged to use AppCompatResources
        // loading
        /* ktlint-disable max-line-length */
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            Stubs.COLOR_STATE_LIST,
            manifest,
            customActivity
        ).issues(ColorStateListLoadingDetector.NOT_USING_COMPAT_LOADING)
            .addTestModes(TestMode.DEFAULT, TestMode.PARTIAL)
            .run()
            .expect(
                """
src/com/example/CustomActivity.kt:8: Warning: Use AppCompatResources.getColorStateList() [UseCompatLoadingForColorStateLists]
        getResources().getColorStateList(R.color.color_state_list)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }
}
