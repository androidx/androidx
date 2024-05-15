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

package androidx.wear.protolayout.expression;

import static com.google.common.truth.Truth.assertThat;

import androidx.wear.protolayout.expression.VersionBuilders.VersionInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class VersionInfoTest {
    @Test
    public void versionInfo() {
        int major = 10;
        int minor = 20;

        VersionInfo versionInfo = new VersionInfo.Builder().setMajor(major).setMinor(minor).build();

        assertThat(versionInfo.toProto().getMajor()).isEqualTo(major);
        assertThat(versionInfo.toProto().getMinor()).isEqualTo(minor);
    }

    @Test
    public void versionInfoComparison() {
        VersionInfo v1_0 = new VersionInfo.Builder().setMajor(1).setMinor(0).build();
        VersionInfo v1_1 = new VersionInfo.Builder().setMajor(1).setMinor(1).build();
        VersionInfo v2_0 = new VersionInfo.Builder().setMajor(2).setMinor(0).build();
        VersionInfo v2_1 = new VersionInfo.Builder().setMajor(2).setMinor(1).build();
        List<VersionInfo> versions = Arrays.asList(v2_1, v2_0, v1_1, v2_0, v1_0);

        Collections.sort(versions);

        assertThat(versions).containsExactly(v1_0, v1_1, v2_0, v2_0, v2_1).inOrder();
    }
}
