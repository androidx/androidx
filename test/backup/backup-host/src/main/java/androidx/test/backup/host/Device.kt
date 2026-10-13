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
 * A [BackupRestoreController] parameter that asks for a serial number, through [serial], a binding
 * of [role] or a system property, gets the device with that serial number. The other controller
 * parameters of a method or constructor get, in the order of the parameters, the first online
 * device by serial number that matches them and that no other parameter asks for or got before
 * them. When every device that matches a parameter is taken, the parameter shares the first one.
 *
 * @property role The role of the device in a multi-device test, such as `"source"` or `"target"`.
 *   The system property `androidx.test.backup.device.serial.<role>` binds the role to the device
 *   with that serial number, whatever [serial] and the other device properties ask for. Without the
 *   property, the role doesn't change which device is selected. Defaults to "".
 * @property api The requested SDK API level (e.g. 31, 34). Defaults to 0 (any matches).
 * @property serial The specific hardware/emulator serial number to match, or a name that the system
 *   property `androidx.test.backup.device.serial.<serial>` binds to a serial number. The system
 *   properties `androidx.test.backup.device.serials` and `androidx.test.backup.device.serial` take
 *   precedence over a literal serial number. Defaults to "" (any match).
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
public annotation class Device(
    public val role: String = "",
    public val api: Int = 0,
    public val serial: String = "",
)
