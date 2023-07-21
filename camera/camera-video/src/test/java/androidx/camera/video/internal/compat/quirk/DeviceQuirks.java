/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.video.internal.compat.quirk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.Quirks;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests version of main/.../DeviceQuirks.java, which provides device specific quirks, used for
 * device specific workarounds.
 * <p>
 * In main/.../DeviceQuirks, Device quirks are loaded the first time a device workaround is
 * encountered, and remain in memory until the process is killed. When running tests, this means
 * that the same device quirks are used for all the tests. This causes an issue when tests modify
 * device properties (using Robolectric for instance). Instead of force-reloading the device
 * quirks in every test that uses a device workaround, this class internally reloads the quirks
 * every time a device workaround is needed.
 */
public class DeviceQuirks {

    private DeviceQuirks() {
    }

    /** Returns all video specific quirks loaded on the current device. */
    @NonNull
    public static Quirks getAll() {
        return new Quirks(DeviceQuirksLoader.loadQuirks());
    }

    /**
     * Retrieves a specific device {@link Quirk} instance given its type.
     *
     * @param quirkClass The type of device quirk to retrieve.
     * @return A device {@link Quirk} instance of the provided type, or {@code null} if it isn't
     * found.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Quirk> T get(@NonNull final Class<T> quirkClass) {
        final List<Quirk> quirks = DeviceQuirksLoader.loadQuirks();
        for (final Quirk quirk : quirks) {
            if (quirk.getClass() == quirkClass) {
                return (T) quirk;
            }
        }
        return null;
    }

    /**
     * Retrieves all device {@link Quirk} instances that are or inherit the given type.
     *
     * @param quirkClass The super type of device quirk to retrieve.
     * @return A device {@link Quirk} list of the provided type. An empty list is returned if it
     * isn't found.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T extends Quirk> List<T> getAll(@NonNull Class<T> quirkClass) {
        List<Quirk> quirks = DeviceQuirksLoader.loadQuirks();
        List<T> list = new ArrayList<>();
        for (Quirk quirk : quirks) {
            if (quirkClass.isAssignableFrom(quirk.getClass())) {
                list.add((T) quirk);
            }
        }
        return list;
    }
}
