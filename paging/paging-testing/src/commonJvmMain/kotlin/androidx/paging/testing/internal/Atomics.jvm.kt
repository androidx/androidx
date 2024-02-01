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
@file:Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
package androidx.paging.testing.internal

internal actual typealias AtomicInt = java.util.concurrent.atomic.AtomicInteger

internal actual typealias AtomicBoolean = java.util.concurrent.atomic.AtomicBoolean

internal actual typealias AtomicRef<T> = java.util.concurrent.atomic.AtomicReference<T>
