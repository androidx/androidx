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

package androidx.camera.core.impl;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class QuirksTest {

    @Test
    public void returnInstanceOfExistentQuirk() {
        final Quirk1 quirk1 = new Quirk1();
        final Quirk2 quirk2 = new Quirk2();

        final List<Quirk> allQuirks = new ArrayList<>();
        allQuirks.add(quirk1);
        allQuirks.add(quirk2);

        final Quirks quirks = new Quirks(allQuirks);

        assertThat(quirks.get(Quirk1.class)).isEqualTo(quirk1);
        assertThat(quirks.get(Quirk2.class)).isEqualTo(quirk2);
    }

    @Test
    public void returnNullForInexistentQuirk() {
        final Quirk1 quirk1 = new Quirk1();
        final Quirk2 quirk2 = new Quirk2();

        final List<Quirk> allQuirks = new ArrayList<>();
        allQuirks.add(quirk1);
        allQuirks.add(quirk2);

        final Quirks quirks = new Quirks(allQuirks);

        assertThat(quirks.get(Quirk3.class)).isNull();
    }

    static class Quirk1 implements Quirk {
    }

    static class Quirk2 implements Quirk {
    }

    static class Quirk3 implements Quirk {
    }
}
