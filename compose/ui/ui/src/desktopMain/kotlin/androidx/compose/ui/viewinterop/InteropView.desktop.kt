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

package androidx.compose.ui.viewinterop

/**
 * On Desktop, it's [java.awt.Component], but it's impossible to match via expect/actual typealias
 * regular class and abstract class.
 * See https://youtrack.jetbrains.com/issue/KT-48734
 */
actual typealias InteropView = Any // java.awt.Component

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal actual typealias InteropViewGroup = java.awt.Container

internal val InteropView.asAwtComponent
    get() = this as java.awt.Component
