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

package androidx.camera.common;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class DiscreteRotationJvmTest {

    @Test
    public void testRoundingJvm() {
        // JVM consumers can use DiscreteRotation.round directly and get raw int values
        assertThat(DiscreteRotation.round(45)).isEqualTo(90);
        assertThat(DiscreteRotation.round(44)).isEqualTo(0);
        assertThat(DiscreteRotation.round(315)).isEqualTo(0);
        assertThat(DiscreteRotation.round(-45)).isEqualTo(0);
        assertThat(DiscreteRotation.round(-46)).isEqualTo(270);
    }

    @Test
    public void testStaticJvmAccess() {
        // Test that JVM consumers can call the static demangled methods directly on the outer class
        int rotation = DiscreteRotation.from(90);
        assertThat(rotation).isEqualTo(90);

        int rounded = DiscreteRotation.round(45);
        assertThat(rounded).isEqualTo(90);
    }
}
