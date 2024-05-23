/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.extensions.internal.compat.quirk;

import android.os.Build;

import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 * Bug Id: b/245226085,
 * Description: When enabling Extensions on devices that implement the Basic Extender. If
 * onDisableSession is invoked within 50ms after onEnableSession, It could cause crashes like
 * surface abandoned.
 * Device(s): All Samsung devices
 */
public class CrashWhenOnDisableTooSoon implements Quirk {
    static boolean load() {
        return Build.BRAND.equalsIgnoreCase("SAMSUNG");
    }
}
