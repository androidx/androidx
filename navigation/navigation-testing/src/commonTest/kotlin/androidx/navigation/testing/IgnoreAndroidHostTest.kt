/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation.testing

/**
 * Excludes the annotated test from execution on **AndroidHostTest** (Robolectric/Unit tests). Note
 * that while it skips host-side tests, the test will still be executed on **AndroidDeviceTest**
 * (Instrumented/Emulator tests).
 *
 * Use this when a test relies on native APIs or hardware features that are unavailable in a
 * JVM-hosted Android environment.
 */
internal expect annotation class IgnoreAndroidHostTest()
