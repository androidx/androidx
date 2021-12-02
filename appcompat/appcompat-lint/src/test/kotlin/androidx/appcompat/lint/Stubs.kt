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

package androidx.appcompat.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.xml
import com.android.tools.lint.checks.infrastructure.TestFile

object Stubs {
    val APPCOMPAT_ACTIVITY: TestFile = LintDetectorTest.kotlin(
        "androidx/appcompat/app/AppCompatActivity.kt",
        """
                package androidx.appcompat.app
                import android.app.Activity
                open class AppCompatActivity: Activity()
            """
    )
        .indented().within("src")

    val APPCOMPAT_RESOURCES: TestFile = LintDetectorTest.kotlin(
        "androidx/appcompat/content/res/AppCompatActivity.kt",
        """
                package androidx.appcompat.content.res
                import android.content.Context
                import android.content.res.ColorStateList
                class AppCompatResources {
                    companion object {
                        fun getColorStateList(content: Context, resId: Int) : ColorStateList {
                            val result: ColorStateList = TODO("Stub")
                            return result
                        }
                    }
                }
            """
    )
        .indented().within("src")

    val CONTEXT_COMPAT: TestFile = LintDetectorTest.java(
        "androidx/core/content/ContextCompat.java",
        """
                package androidx.core.content;
                public class ContextCompat {
                    protected ContextCompat() {}
                    public static Drawable getDrawable(@NonNull Context context, @DrawableRes int id) {
                        throw new Exception();
                    }
                }
            """
    ).indented().within("src")

    val COLOR_STATE_LIST: TestFile = xml(
        "color/color_state_list.xml",
        """
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <item app:alpha="?android:disabledAlpha"
          android:color="#FF0000"
          android:state_enabled="false"/>
    <item android:color="#FF0000"/>
</selector>
        """
    ).indented().within("res")
}
