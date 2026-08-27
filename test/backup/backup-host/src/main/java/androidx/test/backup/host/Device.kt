/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

/**
 * Qualifier annotation used for multi-device orchestration. Instructs the JUnit 5 extension which
 * connected device characteristics to resolve.
 *
 * @property role The requested role of the device in the multi-device test setup. Defaults to "".
 * @property api The requested SDK API level (e.g. 31, 34). Defaults to 0 (any matches).
 * @property serial The specific hardware/emulator serial number to match. Defaults to "" (any
 *   match).
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
public annotation class Device(
    public val role: String = "",
    public val api: Int = 0,
    public val serial: String = "",
)
