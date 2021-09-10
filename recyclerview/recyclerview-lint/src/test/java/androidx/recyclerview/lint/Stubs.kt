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

package androidx.recyclerview.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile

object Stubs {
    val VIEW: TestFile = kotlin(
        "android/view/View.kt",
        """
            package android.view;

            open class View {
                fun <T: View> findViewById(id: Int) : T? = TODO()
            }
        """
    ).indented().within("src")

    val RECYCLER_VIEW: TestFile = kotlin(
        "androidx/recyclerview/widget/RecyclerView.kt",
        """
        package androidx.recyclerview.widget

        open class RecyclerView : View() {
            fun setHasFixedSize(hasFixedSize: Boolean) {

            }
        }
    """
    ).indented().within("src")
}
