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

package androidx.appcompat.lint.view

import androidx.appcompat.lint.Stubs
import androidx.appcompat.view.OnClickXmlDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.manifest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Ignore
import org.junit.Test

class OnClickXmlDetectorTest {
    companion object {
        val layoutWithCoreClick = LintDetectorTest.xml(
            "layout/view_with_click.xml",
            """
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <Button
        android:id="@+id/button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:onClick="myButtonClick"
        android:text="Click!" />
</LinearLayout>
        """
        ).indented().within("res")

        val layoutWithDataBindingClick = LintDetectorTest.xml(
            "layout/view_with_click.xml",
            """
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <Button
        android:id="@+id/button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:onClick="@{handler::handleClick}"
        android:text="Click!" />
</LinearLayout>
        """
        ).indented().within("res")

        val activityWithClick = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.os.Bundle
            import android.view.View
            import android.widget.Toast
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    setContentView(R.layout.view_with_click) 
                }

                public fun myButtonClick(view: View) {
                    Toast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show()
                }
            }
            """
        ).indented().within("src")
    }

    @Ignore("b/187524984")
    @Test
    fun testCoreOnClickApi14() {
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

        // We expect the android:onClick to be flagged on pre-24 min SDK
        /* ktlint-disable max-line-length */
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            Stubs.COLOR_STATE_LIST,
            manifest,
            layoutWithCoreClick,
            activityWithClick
        ).issues(OnClickXmlDetector.USING_ON_CLICK_IN_XML)
            .run()
            .expect(
                """
res/layout/view_with_click.xml:10: Warning: Use databinding or explicit wiring of click listener in code [UsingOnClickInXml]
        android:onClick="myButtonClick"
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Ignore("b/187524984")
    @Test
    fun testCoreOnClickApi23() {
        // Manifest that sets min sdk to 23
        val manifest = manifest(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.example">
                    <uses-sdk android:minSdkVersion="23" android:targetSdkVersion="29" />
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

        // We expect the android:onClick to not be flagged on 24+ min SDK
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            Stubs.COLOR_STATE_LIST,
            manifest,
            layoutWithCoreClick,
            activityWithClick
        ).issues(OnClickXmlDetector.USING_ON_CLICK_IN_XML)
            .run()
            .expectClean()
    }

    @Test
    fun testDataBindingOnClickApi14() {
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

        // We expect the android:onClick that uses databinding syntax to not be flagged, even
        // on min SDK 14
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            Stubs.COLOR_STATE_LIST,
            manifest,
            layoutWithDataBindingClick,
            activityWithClick
        ).issues(OnClickXmlDetector.USING_ON_CLICK_IN_XML)
            .run()
            .expectClean()
    }
}
