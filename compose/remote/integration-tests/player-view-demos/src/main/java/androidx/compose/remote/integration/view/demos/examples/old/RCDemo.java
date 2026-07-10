/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.integration.view.demos.examples.old;

import android.annotation.SuppressLint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Base class for RemoteCompose demos.
 */
@SuppressLint("RestrictedApiAndroidX")
public class RCDemo {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();
    boolean mPrivateConstructorForUtilityClassWorkAround = true;

    protected RCDemo() {
    }

    /**
     * Interface for drawing on a canvas.
     */
    public interface Canvas {
        /**
         * Draws on the provided {@link RemoteComposeWriterAndroid}.
         * @param rc the writer.
         */
        void draw(@NonNull RemoteComposeWriterAndroid rc);
    }

    /**
     * Creates a demo canvas with the specified name and drawing logic.
     * @param name the name of the demo.
     * @param ui the drawing logic.
     * @return a {@link RemoteComposeWriterAndroid} containing the drawing commands.
     */
    protected static @NonNull RemoteComposeWriterAndroid demoCanvas(@NonNull String name,
            @NonNull Canvas ui) {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(300, 300, name, 6, 0,
                sPlatform);
        rc.root(() -> {
            rc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            ui.draw(rc);
            rc.endCanvas();
            rc.endBox();
        });
        return rc;
    }
}
