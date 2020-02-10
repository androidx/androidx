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

package androidx.appcompat.lint.res

import androidx.appcompat.res.ImageViewTintDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class ImageViewTintDetectorTest {
    @Test
    fun testIncompleteColorStateList() {
        val layout = LintDetectorTest.xml(
            "layout/image_view.xml",
            """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    <ImageView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:src="@android:drawable/ic_delete"
        android:tint="#FF0000" />
</LinearLayout>
        """
        ).indented().within("res")

        // We expect the definition of the image view to be flagged since it has
        // android:tint instead of app:tint. We also expect a matching
        // fix to replace android:tint with app:tint, retaining the same value
        lint().files(
            layout
        ).issues(ImageViewTintDetector.USING_ANDROID_TINT)
            .run()
            .expect("""
res/layout/image_view.xml:10: Error: Must use app:tint instead of android:tint [UseAppTint]
        android:tint="#FF0000" />
        ~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
         """.trimIndent())
            .expectFixDiffs("""
Fix for res/layout/image_view.xml line 10: Set tint="#FF0000":
@@ -3 +3
+     xmlns:app="http://schemas.android.com/apk/res-auto"
@@ -11 +12
-         android:tint="#FF0000" />
+         app:tint="#FF0000" />
            """.trimIndent())
    }
}
