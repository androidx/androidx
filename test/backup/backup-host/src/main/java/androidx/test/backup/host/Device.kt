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

import androidx.annotation.IntRange

/**
 * Qualifier annotation used for multi-device orchestration. Instructs the JUnit 5 extension which
 * connected device characteristics to resolve.
 *
 * A [BackupRestoreController] parameter that asks for a serial number, through [serial], a binding
 * of [role] or a system property, gets the device with that serial number. The other controller
 * parameters of a method or constructor each get a matching online device of their own when there
 * are enough: in the order of the parameters, each takes the first free one by serial number, and
 * an earlier parameter moves to another device that matches it if that frees one. A parameter that
 * can't get a device of its own shares the first device that matches it.
 *
 * A device matches when its API level is [api], if set, and from [minApi] to [maxApi]: an exact
 * level such as `api = 34` matches the same devices as `minApi = 34, maxApi = 34`. The system
 * properties `androidx.test.backup.device.apis`, a comma-separated list with an API level for each
 * controller parameter, and `androidx.test.backup.device.api`, one API level for all of them,
 * replace [api] but not [minApi] and [maxApi]. So for parameters whose ranges don't overlap, use
 * the list. In both, 0 means any API level, as it does for [api]. A negative value, a [minApi]
 * above [maxApi], or an [api] or system property outside them fails the test with an
 * [IllegalArgumentException].
 *
 * @property role The role of the device in a multi-device test, such as `"source"` or `"target"`.
 *   The system property `androidx.test.backup.device.serial.<role>` binds the role to the device
 *   with that serial number, whatever [serial] and the other device properties ask for. Without the
 *   property, the role doesn't change which device is selected. Defaults to "".
 * @property api The API level that matches, exactly, such as 31 or 34. Defaults to 0 (any API
 *   level).
 * @property serial The specific hardware/emulator serial number to match, or a name that the system
 *   property `androidx.test.backup.device.serial.<serial>` binds to a serial number. The system
 *   properties `androidx.test.backup.device.serials` and `androidx.test.backup.device.serial` take
 *   precedence over a literal serial number. Defaults to "" (any match).
 * @property minApi The lowest API level that matches. Defaults to 0 (no lower bound).
 * @property maxApi The highest API level that matches. Defaults to `Integer.MAX_VALUE` (no upper
 *   bound).
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
public annotation class Device(
    public val role: String = "",
    @get:IntRange(from = 0) public val api: Int = 0,
    public val serial: String = "",
    @get:IntRange(from = 0) public val minApi: Int = 0,
    @get:IntRange(from = 0) public val maxApi: Int = Integer.MAX_VALUE,
)
