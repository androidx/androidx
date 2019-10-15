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
package androidx.ui.core

import androidx.compose.Composable

/**
 * Provide data for the parent of a [Layout], which can then be read from the
 * corresponding [Measurable].
 *
 * A containing [Layout] sometimes needs to mark children with attributes that can later
 * be read during layout. [data] is assigned to the [Measurable.parentData] to be read.
 * Normally [ParentData] is completely controlled by the containing Layout. For example,
 * Row and Column layout models use parent data to access the flex value of their children
 * during measurement (though that is achieved using the Inflexible and Flexible modifiers,
 * rather than using this widget).
 *
 * Example usage:
 * @sample androidx.ui.framework.samples.ParentDataSample
 *
 */
@Composable
inline fun ParentData(data: Any, crossinline children: @Composable() () -> Unit) {
    DataNode(key = ParentDataKey, value = data) {
        children()
    }
}
