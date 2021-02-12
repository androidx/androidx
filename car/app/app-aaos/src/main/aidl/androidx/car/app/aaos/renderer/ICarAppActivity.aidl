/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.aaos.renderer;

import androidx.car.app.aaos.renderer.IBackButtonListener;
import androidx.car.app.aaos.renderer.IInputConnectionListener;
import androidx.car.app.aaos.renderer.ILifecycleListener;
import androidx.car.app.aaos.renderer.IRotaryEventListener;
import androidx.car.app.aaos.renderer.surface.ISurfaceListener;
import androidx.car.app.aaos.renderer.surface.SurfacePackageCompat;

/**
 * An interface to let renderer service communicate with the car activity.
 *
 * @hide
 */
oneway interface ICarAppActivity {
    /** Sets the surface package. */
    void setSurfacePackage(in SurfacePackageCompat surfacePackageCompat) = 1;

    /** Registers the listener to get callbacks for surface events. */
    void setSurfaceListener(ISurfaceListener listener) = 2;

    /** Registers the listener to get callbacks for lifecyle events. */
    void setLifecycleListener(ILifecycleListener listener) = 3;

    /** Registers the listener to get callbacks for back button events. */
    void setBackButtonListener(IBackButtonListener listener) = 4;

    /** Notifies to start the input, i.e. to show the keyboard. */
    void onStartInput() = 5;

    /** Notifies to stop the input, i.e. to hide the keyboard. */
    void onStopInput() = 6;

    /**
     * Registers the listener for input connection.
     *
     * This listener can be used to establish an input connection with the host.
     */
    void setInputConnectionListener(IInputConnectionListener listener) = 7;

    /** Registers the listener to get rotary events. */
    void setRotaryEventListener(IRotaryEventListener listener) = 8;

    /** Sends the Intent to be used to start a car app. */
    void startCarApp(in Intent intent) = 9;

    /** Requests the activity to finish itself. */
    void finishCarApp() = 10;
}
