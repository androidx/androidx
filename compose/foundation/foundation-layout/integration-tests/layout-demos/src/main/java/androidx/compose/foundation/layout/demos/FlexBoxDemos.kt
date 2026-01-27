/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.layout.demos

import androidx.compose.foundation.layout.demos.flexbox.FlexBoxAlignContentDemo
import androidx.compose.foundation.layout.demos.flexbox.FlexBoxAlignItemsDemo
import androidx.compose.foundation.layout.demos.flexbox.FlexBoxAlignSelfDemo
import androidx.compose.foundation.layout.demos.flexbox.FlexBoxAnimatedDirectionDemo
import androidx.compose.foundation.layout.demos.flexbox.FlexBoxAnimatedOrderDemo
import androidx.compose.foundation.layout.demos.flexbox.FlexBoxFlexDemo
import androidx.compose.foundation.layout.demos.flexbox.FlexBoxIntrinsicDemo
import androidx.compose.foundation.layout.demos.flexbox.FlexBoxJustifyContentDemo
import androidx.compose.foundation.layout.demos.flexbox.SimpleColumnFlexBox
import androidx.compose.foundation.layout.demos.flexbox.SimpleRowFlexBox
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory

val FlexBoxDemos =
    DemoCategory(
        "FlexBox",
        listOf(
            ComposableDemo("FlexDirection.Row") { SimpleRowFlexBox() },
            ComposableDemo("FlexDirection.Column") { SimpleColumnFlexBox() },
            ComposableDemo("JustifyContent") { FlexBoxJustifyContentDemo() },
            ComposableDemo("AlignContent") { FlexBoxAlignContentDemo() },
            ComposableDemo("AlignItems") { FlexBoxAlignItemsDemo() },
            ComposableDemo("AlignSelf") { FlexBoxAlignSelfDemo() },
            ComposableDemo("Flexible") { FlexBoxFlexDemo() },
            ComposableDemo("Animated Direction") { FlexBoxAnimatedDirectionDemo() },
            ComposableDemo("Animated Order") { FlexBoxAnimatedOrderDemo() },
            ComposableDemo("Intrinsic") { FlexBoxIntrinsicDemo() },
        ),
    )
