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

package androidx.camera.lifecycle

import android.app.Application
import android.content.Context
import android.content.ContextWrapper

internal class TestAppContextWrapper(
    base: Context,
    private val app: Application? = null,
    private val deviceId: Int = DEVICE_ID_DEFAULT,
    private val attributionTag: String? = null,
) : ContextWrapper(base) {

    override fun getApplicationContext(): Context {
        return app ?: this
    }

    override fun createAttributionContext(attributionTag: String?): Context {
        return TestAppContextWrapper(this, app, deviceId, attributionTag)
    }

    override fun createDeviceContext(deviceId: Int): Context {
        return TestAppContextWrapper(this, app, deviceId, attributionTag)
    }

    override fun getDeviceId(): Int = deviceId

    override fun getAttributionTag(): String? = attributionTag
}
