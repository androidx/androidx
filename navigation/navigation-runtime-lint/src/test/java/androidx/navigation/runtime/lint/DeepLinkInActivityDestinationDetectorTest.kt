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

package androidx.navigation.runtime.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DeepLinkInActivityDestinationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = DeepLinkInActivityDestinationDetector()

    override fun getIssues(): MutableList<Issue> {
        return mutableListOf(DeepLinkInActivityDestinationDetector.DeepLinkInActivityDestination)
    }

    @Test
    fun expectPass() {
        lint()
            .files(
                xml(
                    "res/navigation/nav_main.xml",
                    """
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_main"
    app:startDestination="@id/fragment_main"
    >

    <fragment
        android:id="@+id/fragment_main"
        android:name="com.example.deeplink.MainFragment"
        >
        <deepLink app:uri="www.example.com" />
    </fragment>

    <activity
        android:id="@+id/activity_deep_link"
        android:name="com.example.deeplink.Activity"
        />

</navigation>
            """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun expectFail() {
        lint()
            .files(
                xml(
                    "res/navigation/nav_main.xml",
                    """
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_main"
    app:startDestination="@id/fragment_main"
    >

    <fragment
        android:id="@+id/fragment_main"
        android:name="com.example.deeplink.MainFragment"
        />

    <activity
        android:id="@+id/activity_deep_link"
        android:name="com.example.deeplink.DeepLinkActivity"
        >
        <deepLink app:uri="www.example.com" />
    </activity>

</navigation>
            """
                )
            )
            .skipTestModes(TestMode.SUPPRESSIBLE) // b/257336973
            .run()
            .expect(
                """
res/navigation/nav_main.xml:17: Warning: Do not attach a <deeplink> to an <activity> destination. Attach the deeplink directly to the second activity or the start destination of a nav host in the second activity instead. [DeepLinkInActivityDestination]
        <deepLink app:uri="www.example.com" />
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun expectCleanSuppress() {
        lint()
            .files(
                xml(
                    "res/navigation/nav_main.xml",
                    """
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/nav_main"
    app:startDestination="@id/fragment_main"
    >

    <fragment
        android:id="@+id/fragment_main"
        android:name="com.example.deeplink.MainFragment"
        />

    <activity
        android:id="@+id/activity_deep_link"
        android:name="com.example.deeplink.DeepLinkActivity"
        >
        <deepLink tools:ignore="DeepLinkInActivityDestination" app:uri="www.example.com" />
    </activity>

</navigation>
            """
                )
            )
            .run()
            .expectClean()
    }
}
