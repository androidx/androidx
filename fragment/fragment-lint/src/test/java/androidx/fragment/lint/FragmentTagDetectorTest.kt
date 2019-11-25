/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.fragment.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FragmentTagDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = FragmentTagDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(FragmentTagDetector.ISSUE)

    @Test
    fun expectPass() {
        lint().files(
            xml("res/layout/layout.xml", """
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
              android:layout_width="match_parent"
              android:layout_height="match_parent">
    <androidx.fragment.app.FragmentContainerView
              android:name="androidx.fragment.app.Test'$'InflatedFragment"
              android:id="@+id/child_fragment"
              android:layout_width="match_parent"
              android:layout_height="match_parent" />
</FrameLayout>
            """))
            .run()
            .expectClean()
    }

    @Test
    fun expectFail() {
        lint().files(
            xml("res/layout/layout.xml", """
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
              android:layout_width="match_parent"
              android:layout_height="match_parent">
    <fragment android:name="androidx.fragment.app.Test'$'InflatedFragment"
              android:id="@+id/child_fragment"
              android:layout_width="match_parent"
              android:layout_height="match_parent" />
</FrameLayout>
            """))
            .run()
            .expect("""
res/layout/layout.xml:5: Warning: Replace the <fragment> tag with FragmentContainerView. [FragmentTagUsage]
    <fragment android:name="androidx.fragment.app.Test'$'InflatedFragment"
     ~~~~~~~~
0 errors, 1 warnings
            """)
    }

    @Test
    fun expectFix() {
        lint().files(
            xml("res/layout/layout.xml", """
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
              android:layout_width="match_parent"
              android:layout_height="match_parent">
    <fragment android:name="androidx.fragment.app.Test'$'InflatedFragment"
              android:id="@+id/child_fragment"
              android:layout_width="match_parent"
              android:layout_height="match_parent" />
</FrameLayout>
            """))
            .run()
            .expect("""
res/layout/layout.xml:5: Warning: Replace the <fragment> tag with FragmentContainerView. [FragmentTagUsage]
    <fragment android:name="androidx.fragment.app.Test'$'InflatedFragment"
     ~~~~~~~~
0 errors, 1 warnings
            """)
            .checkFix(null, xml("res/layout/layout.xml", """
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
              android:layout_width="match_parent"
              android:layout_height="match_parent">
    <androidx.fragment.app.FragmentContainerView android:name="androidx.fragment.app.Test'$'InflatedFragment"
              android:id="@+id/child_fragment"
              android:layout_width="match_parent"
              android:layout_height="match_parent" />
</FrameLayout>
            """))
    }
}
