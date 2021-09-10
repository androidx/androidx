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

import org.junit.Test;

public class VersionTest {

    @Test
    public void testVersionEqual() {
        Version version1 = Version.create(1, 0, 0, "test");
        Version version1_patch = Version.create(1, 0, 1, "test");
        Version version1_minor = Version.create(1, 1, 0, "test");
        Version version1_description = Version.create(1, 0, 0, "description");

        Version version2 = Version.create(2, 0, 0, "test");

        assertThat(version1.equals(version1_description)).isTrue();
        assertThat(version1.equals(version1_patch)).isFalse();
        assertThat(version1.equals(version1_minor)).isFalse();
        assertThat(version1.equals(version2)).isFalse();

        assertThat(version1.compareTo(version1_patch)).isLessThan(0);
        assertThat(version1.compareTo(version1_description)).isEqualTo(0);
        assertThat(version1.compareTo(version1_minor)).isLessThan(0);
        assertThat(version1.compareTo(version2)).isLessThan(0);

        assertThat(version1.compareTo(1)).isEqualTo(0);
        assertThat(version1.compareTo(2)).isLessThan(0);
        assertThat(version1.compareTo(0)).isGreaterThan(0);

        assertThat(version1.compareTo(1, 0)).isEqualTo(0);
        assertThat(version1.compareTo(1, 1)).isLessThan(0);
        assertThat(version1_minor.compareTo(1, 0)).isGreaterThan(0);

        assertThat(version1.compareTo(2, 0)).isLessThan(0);
    }

    @Test
    public void testParseStringVersion() {

        Version version1 = Version.parse("1.2.3-description");
        assertThat(version1).isNotNull();
        assertThat(version1.getMajor()).isEqualTo(1);
        assertThat(version1.getMinor()).isEqualTo(2);
        assertThat(version1.getPatch()).isEqualTo(3);
        assertThat(version1.getDescription()).isEqualTo("description");

        Version version2 = Version.parse("4.5.6");
        assertThat(version2).isNotNull();
        assertThat(version2.getDescription()).isEqualTo("");

        Version version3 = Version.parse("01.002.0003");
        assertThat(version3).isNotNull();
        assertThat(version3.getMajor()).isEqualTo(1);
        assertThat(version3.getMinor()).isEqualTo(2);
        assertThat(version3.getPatch()).isEqualTo(3);


        // Test invalid input version string.
        assertThat(Version.parse("1.0")).isNull();
        assertThat(Version.parse("1. 0.0")).isNull();
        assertThat(Version.parse("1..0")).isNull();
        assertThat(Version.parse("1.0.a")).isNull();
        assertThat(Version.parse("1.0.0.")).isNull();
        assertThat(Version.parse("1.0.0.description")).isNull();

        assertThat(Version.parse("1.0.0.0")).isNull();
        assertThat(Version.parse("1.0.-0")).isNull();
        assertThat(Version.parse("1.0.-0")).isNull();
        assertThat(Version.parse("(1.0.0)")).isNull();
        assertThat(Version.parse(" 1.0.0 ")).isNull();
    }
}
