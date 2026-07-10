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
package androidx.compose.remote.integration.demos.accessibility.platform;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.compose.remote.player.view.accessibility.RemoteComposeAccessibilityRegistrar;

/**
 * This class is the entry point for finding the AccessibilityDelegate for a RemoteCompose document.
 */
@RequiresApi(api = Build.VERSION_CODES.BAKLAVA)
@SuppressLint("RestrictedApiAndroidX")
public class RemoteComposeTouchHelper {
    /** Get the platform specific accessibility delegate registrar */
    public static final RemoteComposeAccessibilityRegistrar REGISTRAR =
            new PlatformRemoteComposeAccessibilityRegistrar();
}
