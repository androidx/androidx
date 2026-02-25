/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.examples;

import static androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX;

import android.annotation.SuppressLint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

public class SmallAnimated {
    private SmallAnimated() {
    }

    @SuppressLint("RestrictedApiAndroidX")
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    /**
     * Small animated demo
     *
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull RemoteComposeWriter small() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 7,
                PROFILE_ANDROIDX, sPlatform);
        int id = rc.createTextFromFloat(Rc.Time.CONTINUOUS_SEC, 2, 2, 0);
        rc.getPainter().setTextSize(120f).commit();
        rc.drawTextAnchored(id, 0, 0, -1, 1, 0);
        return rc;
    }
}
