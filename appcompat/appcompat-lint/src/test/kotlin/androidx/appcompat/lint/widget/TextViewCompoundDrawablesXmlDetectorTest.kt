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
        app:drawableStartCompat="@android:drawable/ic_delete"
        app:drawableLeftCompat="@android:drawable/ic_delete"
        app:drawableEndCompat="@android:drawable/ic_delete"
        app:drawableRightCompat="@android:drawable/ic_delete"
        app:drawableTopCompat="@android:drawable/ic_delete"
        app:drawableBottomCompat="@android:drawable/ic_delete"
        app:drawableTint="@android:color/black"
        app:drawableTintMode="src_in" />
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

    // Helper function to verify that our Lint rule works for a single compound drawable
    // attribute. We have 8 such attributes, and the test logic is highly repetitive:
    // 1. Create a single TextView with the attribute in the android: namespace
    // 2. Run the rule
    // 3. Verify that the attribute definition is flagged and the suggestion is to use app:
    // 4. And that the fix deletes the android: attribute and adds the app: one
    private fun verifyCompoundDrawableLintPass(
        androidAttrName: String,
        appAttrName: String,
        attrValue: String
    ) {
        val originalAttrDefinition = "android:$androidAttrName=\"$attrValue\""

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
        $originalAttrDefinition />
</LinearLayout>
        """
        ).indented().within("res")

        // The highlight part (~~~~~) that marks the problematic part of the XML is
        // dynamic in length, dependning on the attribute name and value
        val highlight = "~".repeat(originalAttrDefinition.length)

        // We expect the definition of the text view to be flagged since it is using
        // android: namespaced compound drawables attributes. We also expect a matching
        // fix to replace the matching attributes to the app: namespace, retaining the same values
        /* ktlint-disable max-line-length */
        lint().files(
            layout
        ).issues(TextViewCompoundDrawablesXmlDetector.NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_ATTRS)
            .run()
            .expect("""
res/layout-v23/text_view.xml:9: Warning: Use app:$appAttrName instead of android:$androidAttrName [UseCompatTextViewDrawableXml]
        $originalAttrDefinition />
        $highlight
0 errors, 1 warnings
         """.trimIndent())
            .expectFixDiffs("""
Fix for res/layout-v23/text_view.xml line 9: Set $appAttrName="$attrValue":
@@ -3 +3
+     xmlns:app="http://schemas.android.com/apk/res-auto"
@@ -10 +11
-         android:$androidAttrName="$attrValue" />
+         app:$appAttrName="$attrValue" />
            """.trimIndent())
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testUsingAndroidCompoundDrawableStartAttribute() {
        verifyCompoundDrawableLintPass("drawableStart", "drawableStartCompat",
            "@android:drawable/ic_delete")
    }

    @Test
    fun testUsingAndroidCompoundDrawableLeftAttribute() {
        verifyCompoundDrawableLintPass("drawableLeft", "drawableLeftCompat",
            "@android:drawable/ic_delete")
    }

    @Test
    fun testUsingAndroidCompoundDrawableEndAttribute() {
        verifyCompoundDrawableLintPass("drawableEnd", "drawableEndCompat",
            "@android:drawable/ic_delete")
    }

    @Test
    fun testUsingAndroidCompoundDrawableRightAttribute() {
        verifyCompoundDrawableLintPass("drawableRight", "drawableRightCompat",
            "@android:drawable/ic_delete")
    }

    @Test
    fun testUsingAndroidCompoundDrawableTopAttribute() {
        verifyCompoundDrawableLintPass("drawableTop", "drawableTopCompat",
            "@android:drawable/ic_delete")
    }

    @Test
    fun testUsingAndroidCompoundDrawableBottomAttribute() {
        verifyCompoundDrawableLintPass("drawableBottom", "drawableBottomCompat",
            "@android:drawable/ic_delete")
    }

    @Test
    fun testUsingAndroidCompoundDrawableTintAttribute() {
        verifyCompoundDrawableLintPass("drawableTint", "drawableTint", "@android:color/black")
    }

    @Test
    fun testUsingAndroidCompoundDrawableTintModeAttribute() {
        verifyCompoundDrawableLintPass("drawableTintMode", "drawableTintMode", "src_in")
    }
}
