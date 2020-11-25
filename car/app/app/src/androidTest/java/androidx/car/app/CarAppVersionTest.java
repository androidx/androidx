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

package androidx.car.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.CarAppVersion.ReleaseSuffix;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link CarAppVersion}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class CarAppVersionTest {

    @Test
    public void majorVersion() {
        CarAppVersion hostVersion = CarAppVersion.create(2, 0, 0);
        CarAppVersion clientVersion = CarAppVersion.create(1, 0, 0);

        assertThat(hostVersion.isGreaterOrEqualTo(clientVersion)).isTrue();
        assertThat(clientVersion.isGreaterOrEqualTo(hostVersion)).isFalse();
    }

    @Test
    public void minorVersion() {
        CarAppVersion hostVersion = CarAppVersion.create(2, 1, 0);
        CarAppVersion clientVersion = CarAppVersion.create(2, 0, 0);

        assertThat(hostVersion.isGreaterOrEqualTo(clientVersion)).isTrue();
        assertThat(clientVersion.isGreaterOrEqualTo(hostVersion)).isFalse();
    }

    @Test
    public void patchVersion() {
        CarAppVersion hostVersion = CarAppVersion.create(3, 2, 1);
        CarAppVersion clientVersion = CarAppVersion.create(3, 2, 0);

        assertThat(hostVersion.isGreaterOrEqualTo(clientVersion)).isTrue();
        assertThat(clientVersion.isGreaterOrEqualTo(hostVersion)).isFalse();
    }

    @Test
    public void eapVersion_requiresExactMatch() {
        CarAppVersion hostVersion = CarAppVersion.create(3, 2, 1, ReleaseSuffix.RELEASE_SUFFIX_EAP,
                1);

        CarAppVersion mismatchedClientVersion = CarAppVersion.create(4, 3, 2);
        assertThat(hostVersion.isGreaterOrEqualTo(mismatchedClientVersion)).isFalse();
        assertThat(mismatchedClientVersion.isGreaterOrEqualTo(hostVersion)).isFalse();

        CarAppVersion matchedClientVersion =
                CarAppVersion.create(3, 2, 1, ReleaseSuffix.RELEASE_SUFFIX_EAP, 1);
        assertThat(hostVersion.isGreaterOrEqualTo(matchedClientVersion)).isTrue();
        assertThat(matchedClientVersion.isGreaterOrEqualTo(hostVersion)).isTrue();
    }

    @Test
    public void stableVersion_compatibleWithAllBeta() {
        CarAppVersion hostVersion = CarAppVersion.create(3, 2, 1);

        CarAppVersion clientVersion1 =
                CarAppVersion.create(3, 2, 1, ReleaseSuffix.RELEASE_SUFFIX_BETA, 1);
        assertThat(hostVersion.isGreaterOrEqualTo(clientVersion1)).isTrue();
        assertThat(clientVersion1.isGreaterOrEqualTo(hostVersion)).isFalse();

        CarAppVersion clientVersion2 =
                CarAppVersion.create(3, 2, 1, ReleaseSuffix.RELEASE_SUFFIX_BETA, 2);
        assertThat(hostVersion.isGreaterOrEqualTo(clientVersion2)).isTrue();
        assertThat(clientVersion2.isGreaterOrEqualTo(hostVersion)).isFalse();
    }

    @Test
    public void versionString_malformed_multipleHyphens() {
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2.3-eap.4-5"));
    }

    @Test
    public void versionString_malformed_mainVersionIncorrectNumbers() {
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1"));
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1."));
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2"));
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2."));
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2.3.4"));
    }

    @Test
    public void versionString_malformed_invalidNumberFormat() {
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2.3c-eap.4"));
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2.3-eap.4c"));
    }

    @Test
    public void versionString_malformed_incorrectReleaseSuffix() {
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2.3-"));
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2.3-eap"));
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2.3-eap."));
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2.3-eap.4."));
        assertThrows(MalformedVersionException.class, () -> CarAppVersion.of("1.2.3-eaP.4"));
    }

    @Test
    public void versionString() throws MalformedVersionException {
        String version1 = "1.2.3";
        String version2 = "1.2.3-eap.0";
        String version3 = "1.2.3-beta.1";

        assertThat(CarAppVersion.of(version1).toString()).isEqualTo(version1);
        assertThat(CarAppVersion.of(version2).toString()).isEqualTo(version2);
        assertThat(CarAppVersion.of(version3).toString()).isEqualTo(version3);
    }
}
