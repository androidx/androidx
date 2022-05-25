/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.viewfinder.internal.quirk;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Inject quirks for unit tests.
 *
 * <p> Used with the test version of {@link DeviceQuirks} to test the behavior of quirks.
 */
public class QuirkInjector {

    static final List<Quirk> INJECTED_QUIRKS = new ArrayList<>();

    /**
     * Inject a quirk. The injected quirk will be loaded by {@link DeviceQuirks}.
     */
    public static void inject(@NonNull Quirk quirk) {
        INJECTED_QUIRKS.add(quirk);
    }

    /**
     * Clears all injected quirks.
     */
    public static void clear() {
        INJECTED_QUIRKS.clear();
    }
}
