/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.foundation

internal object ErrorMessages {
    val SizeAlreadyExists = "<Layout> can only be used once within a <MeasureBox>"
    val NoSizeAfterLayout = "<MeasureBox> requires one <Layout> element"
    val OnlyComponents = "Don't know how to add a non-composable element to the hierarchy"
    val NoMovingSingleElements = "Cannot move elements that contain a maximum of one child"
    val NoChild = "There is no child in this node"
    val ChildrenUnsupported = "Draw does not have children"
}