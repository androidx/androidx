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

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Provide data that can be read from the [Measurable] children.
 *
 * A containing [Layout] sometimes needs to mark children with attributes that can later
 * be read during layout. [data] is assigned to the [Measurable.parentData] to be read.
 * Normally [ParentData] is completely controlled by the containing Layout. For example,
 * [Flex] is used like this:
 *
 *     Flex(...) {
 *         expanded(2f) {
 *             Text(...)
 *         }
 *         inflexible {
 *             Center {Text(...)}
 *         }
 *     }
 *
 * The Flex component internally adds a ParentData to mark the expanded and inflexible
 * children so that they can later be read during layout. Conceptually, Flex will treat it
 * like this:
 *
 *     Flex(...) {
 *         ParentData(value = FlexInfo(FlexFit.Tight, 2f)) {
 *             Text(...)
 *         }
 *         ParentData(value = FlexInfo(FlexFit.Loose, 0f)) {
 *             Center {Text(...)}
 *         }
 *     }
 *
 * Flex then reads the [Measurable.parentData] and can determine which elements are
 * expanded, which are inflexible and which are flexible.
 */
@Composable
fun ParentData(data: Any, @Children children: @Composable() () -> Unit) {
    <DataNode key=ParentDataKey value=data>
        <children/>
    </DataNode>
}

internal val ParentDataKey = DataNodeKey<Any>("Compose:ParentData")