/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.mpp.demo.bug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen

val SelectionContainerCrash = Screen.Example("SelectionContainerCrash") {
    //TODO: This bug is already fixed in latest androidx-main branch.
    // Related CL: https://android-review.googlesource.com/c/platform/frameworks/support/+/2616177/3/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/text/selection/SelectionAdjustment.kt
    // Check and close this bug after merging androidx-main on jb-main branch.
    // https://youtrack.jetbrains.com/issue/COMPOSE-478/fix-iOS-SelectionContainer
    // https://github.com/JetBrains/compose-multiplatform/issues/3718
    SelectionContainer {
        Column {
            Text("aaa")
            Text("")
            Text("bbb")
        }
    }
}
