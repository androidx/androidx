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

package androidx.compose.remote.integration.view.demos.examples

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

@SuppressLint("RestrictedApiAndroidX")
class RideShare {
    private var carLogo: Bitmap? = null
    private var carDriver: Bitmap? = null
    private var carIcon: Bitmap? = null

    /**
     * Stores the bitmaps needed to draw the ride share notification.
     *
     * Should be called before rideShare() is called.
     */
    public fun setBitmaps(carLogo: Bitmap, carDriver: Bitmap, carIcon: Bitmap) {
        this.carLogo = carLogo
        this.carDriver = carDriver
        this.carIcon = carIcon
    }

    /**
     * Draws a ride share notification.
     *
     * setBitmaps() should be called before this is called. In case it is not, this method will draw
     * as much as it can without having the bitmaps.
     */
    @SuppressLint("RestrictedApiAndroidX")
    public fun rideShare(): RemoteComposeContext {
        val platform = AndroidxRcPlatformServices()
        val rcDoc = RemoteComposeWriterAndroid(500, 500, "sd", 6, PROFILE_ANDROIDX, platform)

        rcDoc.getPainter().setColor(Color.BLACK).commit()
        rcDoc.drawRect(0f, 0f, 500f, 500f)

        rcDoc.getPainter().setColor(Color.WHITE).commit()
        rcDoc.drawLine(20f, 440f, 480f, 440f)

        if (carLogo != null) {
            rcDoc.save()

            // Upper left bitmap.
            rcDoc.translate(0f, 0f)
            rcDoc.drawBitmap(carLogo!!, "car_logo")

            // Upper right bitmap.
            rcDoc.translate(240f, 20f)
            rcDoc.drawBitmap(carDriver!!, "car_driver")

            // Bottom center bitmap.
            rcDoc.translate(-60f, 380f)
            rcDoc.drawBitmap(carIcon!!, "car_icon")

            rcDoc.restore()
        }

        rcDoc.translate(0f, 250f)

        // Center text.
        val cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL)
        rcDoc.getPainter().setTextSize(40f).setColor(Color.WHITE).commit()
        rcDoc.drawTextAnchored("Pickup in 4 min", cx, 0f, 0f, 0f, ANCHOR_MONOSPACE_MEASURE)
        rcDoc.drawTextAnchored("Red Toyota Camry", cx, 40f, 0f, 0f, ANCHOR_MONOSPACE_MEASURE)
        rcDoc.drawTextAnchored("ABC123", cx, 80f, 0f, 0f, ANCHOR_MONOSPACE_MEASURE)

        return RemoteComposeContext(rcDoc)
    }
}
