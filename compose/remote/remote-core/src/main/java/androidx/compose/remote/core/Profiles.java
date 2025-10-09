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

package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;

/** List of supported Profiles */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Profiles {
    private Profiles() {}

    ////////////////////////////////////////
    // Available profiles
    ////////////////////////////////////////

    /**
     * Baseline profile, for a given version this contains the standard (baseline) set of supported
     * features/operations. Operations in the baseline profile should be assume to be supported by
     * by players without prior arrangement.
     */
    public static final int PROFILE_BASELINE = 0x0;

    // Additive profiles
    /**
     * Experimental profile, the supported set of additional operations beyond [PROFILE_BASELINE] is
     * not strongly defined, it may include features only supported in a fork of the Creation API
     * and specific Players.
     */
    public static final int PROFILE_EXPERIMENTAL = 0x1;

    /**
     * Deprecated profile, operations which have been deprecated after some version and should
     * not be used. These operations will have been removed from [PROFILE_BASELINE] after some
     * version.
     */
    public static final int PROFILE_DEPRECATED = 0x2;

    /**
     * OEM Profile, this is typically reserved for additional features for a specific Player on a
     * particular device, with agreement via some external mechanism.
     */
    public static final int PROFILE_OEM = 0x4;

    /**
     * Low Power Profile, this may be a subset of [PROFILE_BASELINE], with a set of features
     * appropriate for low devices or low power states.
     */
    public static final int PROFILE_LOW_POWER = 0x8;

    // Intersected profiles
    /**
     * Launcher Widget Profile, this is an extended set of operations beyond [PROFILE_BASELINE],
     * known to be supported in the Android Platform Player.
     */
    public static final int PROFILE_WIDGETS = 0x100;

    /**
     * AndroidX Profile, this is an extended set of operations beyond [PROFILE_BASELINE], known
     * to be supported in the AndroidX Player.
     */
    public static final int PROFILE_ANDROIDX = 0x200;

    /**
     * Android Native Profile, this is an extended set of operations beyond [PROFILE_BASELINE],
     * known to be supported in a non-public Android Native Player.
     */
    public static final int PROFILE_ANDROID_NATIVE = 0x400;

    /**
     * Wear Widgets (Tiles) Profile, this is an selected set of operations within
     * [PROFILE_BASELINE], required to support the intended feature set of Wear Widgets only.
     */
    public static final int PROFILE_WEAR_WIDGETS = 0x800;
}
