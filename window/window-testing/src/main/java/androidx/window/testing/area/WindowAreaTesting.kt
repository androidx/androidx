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
@file:JvmName("TestWindowArea")

package androidx.window.testing.area

import android.graphics.Rect
import android.os.Binder
import android.os.IBinder
import androidx.window.area.WindowArea
import androidx.window.area.WindowAreaToken
import androidx.window.layout.WindowMetrics
import androidx.window.testing.layout.TestWindowMetrics

/**
 * Creates an [WindowArea] instance for testing. The default values are a default [WindowMetrics]
 * object, a type of [WindowArea.Type.TYPE_REAR_FACING] and a default [IBinder] token.
 *
 * @param metrics The [WindowMetrics] the [WindowArea] should represent.
 * @param type Indicates what type this [WindowArea] is.
 * @param token Identifying token for this [WindowArea]
 * @return A [WindowArea] instance for testing.
 */
@Suppress("FunctionName")
@JvmName("createTestWindowArea")
@JvmOverloads
public fun WindowArea(
    metrics: WindowMetrics = TestWindowMetrics(bounds = Rect()),
    type: WindowArea.Type = WindowArea.Type.TYPE_REAR_FACING,
    token: IBinder = Binder(),
): WindowArea = WindowArea(metrics, type, WindowAreaToken.fromBinder(token), HashMap())
