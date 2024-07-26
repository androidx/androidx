/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.mpp.demo.components

import androidx.compose.mpp.demo.Screen
import androidx.compose.mpp.demo.components.dialog.Dialogs
import androidx.compose.mpp.demo.components.material.AlertDialogExample
import androidx.compose.mpp.demo.components.material.DropdownMenuExample
import androidx.compose.mpp.demo.components.material3.AlertDialog3Example
import androidx.compose.mpp.demo.components.material3.BottomSheetScaffoldExample
import androidx.compose.mpp.demo.components.material3.DateTimePickerExample
import androidx.compose.mpp.demo.components.material3.DropdownMenu3Example
import androidx.compose.mpp.demo.components.material3.ModalBottomSheet3Example
import androidx.compose.mpp.demo.components.material3.ModalNavigationDrawerExample
import androidx.compose.mpp.demo.components.material3.SearchBarExample
import androidx.compose.mpp.demo.components.material3.WindowSizeClassExample
import androidx.compose.mpp.demo.components.popup.Popups
import androidx.compose.mpp.demo.textfield.TextFields

private val MaterialComponents = Screen.Selection(
    "material",
    Screen.Example("AlertDialog") { AlertDialogExample() },
    Screen.Example("DropdownMenu") { DropdownMenuExample() },
)

private val Material3Components = Screen.Selection(
    "material3",
    Screen.Example("AlertDialog") { AlertDialog3Example() },
    Screen.Example("BottomSheetScaffold") { BottomSheetScaffoldExample() },
    Screen.Example("Date & Time Pickers") { DateTimePickerExample() },
    Screen.Example("DropdownMenu") { DropdownMenu3Example() },
    Screen.Example("ModalBottomSheet") { ModalBottomSheet3Example() },
    Screen.Example("ModalNavigationDrawer") { ModalNavigationDrawerExample() },
    Screen.Example("SearchBar") { SearchBarExample() },
    Screen.Example("WindowSizeClass") { WindowSizeClassExample() },
)

val Components = Screen.Selection(
    "Components",
    Popups,
    Dialogs,
    TextFields,
    LazyLayouts,
    MaterialComponents,
    Material3Components,
    Screen.Example("NestedScroll") { NestedScrollExample() },
    Screen.Example("Selection") { SelectionExample() },
    Screen.Example("Pager") { PagerExample() },
)
