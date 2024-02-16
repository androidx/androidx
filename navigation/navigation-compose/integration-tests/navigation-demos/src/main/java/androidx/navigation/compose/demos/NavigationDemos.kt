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

package androidx.navigation.compose.demos

import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory

val NavigationDemos = DemoCategory(
    "Navigation",
    listOf(
        ComposableDemo("Basic Nav Demo") { BasicNavDemo() },
        ComposableDemo("Nested Nav Start Destination Demo") { NestNavStartDestinationDemo() },
        ComposableDemo("Nested Nav In Graph Demo") { NestNavInGraphDemo() },
        ComposableDemo("Bottom Bar Nav Demo") { BottomBarNavDemo() },
        ComposableDemo("Navigation with Args") { NavWithArgsDemo() },
        ComposableDemo("Navigation by DeepLink") { NavByDeepLinkDemo() },
        ComposableDemo("Navigation PopUpTo") { NavPopUpToDemo() },
        ComposableDemo("Navigation SingleTop") { NavSingleTopDemo() },
        ComposableDemo("Size Transform Demo") { SizeTransformDemo() }
    )
)
