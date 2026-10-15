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

package androidx.health.connect.client.records.metadata;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Checks that Java callers can construct a {@link Device} with any number of arguments. */
@RunWith(JUnit4.class)
public class DeviceJavaInteropTest {
    @Test
    public void constructors_everyArity_compileAndSetProperties() {
        Device typeOnly = new Device(Device.TYPE_WATCH);
        Device withManufacturer = new Device(Device.TYPE_WATCH, "Google");
        Device withModel = new Device(Device.TYPE_WATCH, "Google", "Pixel Watch 3");
        Device withDisplayName =
                new Device(Device.TYPE_WATCH, "Google", "Pixel Watch 3", "Alex's watch");

        assertThat(typeOnly.getManufacturer()).isNull();
        assertThat(withManufacturer.getManufacturer()).isEqualTo("Google");
        assertThat(withModel.getModel()).isEqualTo("Pixel Watch 3");
        assertThat(withModel.getDisplayName()).isNull();
        assertThat(withDisplayName.getDisplayName()).isEqualTo("Alex's watch");
    }
}
