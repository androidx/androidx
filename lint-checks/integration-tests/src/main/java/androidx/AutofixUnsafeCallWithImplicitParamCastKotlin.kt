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
package androidx

import android.app.Notification
import androidx.annotation.RequiresApi

/**
 * Tests to ensure the generated lint fix does not leave in implicit casts from a new argument type.
 */
@Suppress("unused")
class AutofixUnsafeCallWithImplicitParamCastKotlin {
    /**
     * This uses the Notification.MessagingStyle type, but setBuilder is defined on
     * Notification.Style, and the two classes were introduced in different API levels.
     */
    @RequiresApi(24)
    fun castReceiver(style: Notification.MessagingStyle, builder: Notification.Builder?) {
        style.setBuilder(builder)
    }

    /**
     * This uses Notification.CarExtender, but extend is defined with Notification.Extender as a
     * parameter, and the two classes were introduced at different API levels.
     */
    @RequiresApi(23)
    fun castParameter(builder: Notification.Builder, extender: Notification.CarExtender?) {
        builder.extend(extender)
    }
}
