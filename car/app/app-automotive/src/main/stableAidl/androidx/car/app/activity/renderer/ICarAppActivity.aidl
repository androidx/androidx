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

package androidx.car.app.activity.renderer;

import android.os.Bundle;
import android.content.Intent;
import android.view.inputmethod.EditorInfo;

import androidx.car.app.activity.renderer.IInsetsListener;
import androidx.car.app.activity.renderer.IRendererCallback;
import androidx.car.app.activity.renderer.surface.ISurfaceListener;
import androidx.car.app.serialization.Bundleable;

/**
 * An interface to let renderer service communicate with the car activity.
 *
 * @hide
 */
oneway interface ICarAppActivity {
    /** Sets the surface package. */
    void setSurfacePackage(in Bundleable surfacePackage) = 1;

    /** Registers the listener to get callbacks for surface events. */
    void setSurfaceListener(ISurfaceListener listener) = 2;

    /** Registers the callback to get callbacks for renderer events. */
    void registerRendererCallback(IRendererCallback callback) = 3;

    /** Notifies to start the input, i.e. to show the keyboard. */
    void onStartInput() = 4;

    /** Notifies to stop the input, i.e. to hide the keyboard. */
    void onStopInput() = 5;

    /** Sends the Intent to be used to start a car app. */
    void startCarApp(in Intent intent) = 6;

    /** Requests the activity to finish itself. */
    void finishCarApp() = 7;

    /** Notifies that there has been a selection update for the currently active input. */
    void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd) = 8;

    /** Registers the listener to get insets updates. */
    void setInsetsListener(IInsetsListener listener) = 9;

    /** Entrypoint for host to call assistant */
    void showAssist(in Bundle args) = 10;
}
