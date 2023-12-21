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

package androidx.camera.extensions.internal;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ClientVersionTest {
    @Test
    public void testCurrentVersion_shouldNotEmpty() {
        assertNotNull(ClientVersion.getCurrentVersion().getVersion());
    }

    @Test
    public void testSetCurrentVersion() {
        ClientVersion.setCurrentVersion(new ClientVersion("1.9.0"));
        assertThat(ClientVersion.getCurrentVersion().toVersionString()).isEqualTo("1.9.0");
    }

    @Test
    public void testIsMinimumCompatibleVersion() {
        ClientVersion.setCurrentVersion(new ClientVersion("1.2.0"));
        assertThat(ClientVersion.isMinimumCompatibleVersion(Version.parse("1.1.0")))
                .isTrue();
        assertThat(ClientVersion.isMinimumCompatibleVersion(Version.parse("1.2.0")))
                .isTrue();
        assertThat(ClientVersion.isMinimumCompatibleVersion(Version.parse("1.3.0")))
                .isFalse();
    }
}
