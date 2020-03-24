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

package androidx.appcompat.lint.widget

import androidx.appcompat.widget.TextViewCompoundDrawablesXmlDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class TextViewCompoundDrawablesXmlDetectorTest {
    @Test
    fun testUsingAppCompoundDrawableAttributes() {
        val layout = LintDetectorTest.xml(
            "layout-v23/text_view.xml",
            """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:drawableStartCompat="@android:drawable/ic_delete" />
</LinearLayout>
        """
        ).indented().within("res")

        // We expect the definition of the text view to not be flagged
        lint().files(
            layout
        ).issues(TextViewCompoundDrawablesXmlDetector.NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_ATTRS)
            .run()
            .expectClean()
    }

    @Test
    fun testUsingAndroidCompoundDrawableAttributes() {
        val layout = LintDetectorTest.xml(
            "layout-v23/text_view.xml",
            """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:drawableStart="@android:drawable/ic_delete" />
</LinearLayout>
        """
        ).indented().within("res")

        // We expect the definition of the text view to be flagged since it is using
        // android: namespaced compound drawables attributes. We also expect a matching
        // fix to replace the matching attributes to the app: namespace, retaining the same values
        /* ktlint-disable max-line-length */
        lint().files(
            layout
        ).issues(TextViewCompoundDrawablesXmlDetector.NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_ATTRS)
            .run()
            .expect("""
res/layout-v23/text_view.xml:9: Warning: Use app:drawableStartCompat instead of android:drawableStart [UseCompatTextViewDrawableXml]
        android:drawableStart="@android:drawable/ic_delete" />
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
         """.trimIndent())
            .expectFixDiffs("""
Fix for res/layout-v23/text_view.xml line 9: Set drawableStartCompat="@android:drawable/ic_delete":
@@ -3 +3
+     xmlns:app="http://schemas.android.com/apk/res-auto"
@@ -10 +11
-         android:drawableStart="@android:drawable/ic_delete" />
+         app:drawableStartCompat="@android:drawable/ic_delete" />
            """.trimIndent())
        /* ktlint-enable max-line-length */
    }
}
