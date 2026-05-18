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

package androidx.core.locationbutton.testing

import android.annotation.SuppressLint
import android.app.permissionui.LocationButtonSession
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.view.SurfaceControlViewHost
import android.view.View

/**
 * A fake implementation of `LocationButtonSession` for testing purposes. Allows simulating session
 * operations.
 */
@SuppressLint("NewApi")
internal class TestLocationButtonSession(
    context: Context,
    hostToken: IBinder,
    displayId: Int,
    width: Int,
    height: Int,
    private val onClose: () -> Unit = {},
) : LocationButtonSession {
    private var closed = false

    private val view = View(context)
    private val host: SurfaceControlViewHost

    init {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val display =
            requireNotNull(displayManager?.getDisplay(displayId)) {
                "The displayId provided did not result in a valid display."
            }
        host = SurfaceControlViewHost(context, display, hostToken)
        host.setView(view, width, height)
    }

    override fun close() {
        closed = true
        host.release()
        onClose()
    }

    override fun getSurfacePackage(): SurfaceControlViewHost.SurfacePackage {
        return checkNotNull(host.surfacePackage) { "SurfacePackage is null." }
    }

    override fun resize(width: Int, height: Int) {
        host.relayout(width, height)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {}

    override fun changeConfiguration(configuration: Configuration) {
        view.dispatchConfigurationChanged(configuration)
    }

    override fun setCornerRadius(radius: Float) {}

    override fun setPressedCornerRadius(radius: Float) {}

    override fun setTextColor(color: Int) {}

    override fun setBackgroundColor(color: Int) {}

    override fun setIconTint(color: Int) {}

    override fun setStrokeColor(color: Int) {}

    override fun setStrokeWidth(width: Int) {}

    override fun setTextType(textType: Int) {}
}
