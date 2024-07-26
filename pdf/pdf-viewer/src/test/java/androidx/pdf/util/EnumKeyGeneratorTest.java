/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.test.filters.SmallTest;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/** Unit tests for {@link EnumKeyGenerator}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class EnumKeyGeneratorTest {

    enum Animal {
        LION,
        TURTLE,
        RACCOON;
    }

    @Test
    public void testCreateKey() {
        List<Animal> enums = Lists.newArrayList(Animal.LION);
        assertThat(EnumKeyGenerator.createKey(enums)).isEqualTo("LION_");

        enums = Lists.newArrayList(Animal.TURTLE);
        assertThat(EnumKeyGenerator.createKey(enums)).isEqualTo("TURTLE_");
    }

    @Test
    public void testCreateKey_null() {
        assertThat(EnumKeyGenerator.createKey(null)).isEmpty();
    }

    @Test
    public void testCreateKey_empty() {
        assertThat(EnumKeyGenerator.createKey(new ArrayList<Animal>())).isEmpty();
    }

    @Test
    public void testCreateKey_orderDoesNotMatter() {
        List<Animal> enums = Lists.newArrayList(Animal.TURTLE, Animal.RACCOON);
        assertThat(EnumKeyGenerator.createKey(enums)).isEqualTo("TURTLE_RACCOON_");

        enums = Lists.newArrayList(Animal.RACCOON, Animal.TURTLE);
        assertThat(EnumKeyGenerator.createKey(enums)).isEqualTo("TURTLE_RACCOON_");
    }

    @Test
    public void testCreateKey_duplicatesDoNotMatter() {
        List<Animal> enums = Lists.newArrayList(Animal.TURTLE, Animal.RACCOON, Animal.RACCOON);
        assertThat(EnumKeyGenerator.createKey(enums)).isEqualTo("TURTLE_RACCOON_");

        enums =
                Lists.newArrayList(
                        Animal.TURTLE,
                        Animal.RACCOON,
                        Animal.TURTLE,
                        Animal.LION,
                        Animal.RACCOON,
                        Animal.RACCOON);
        assertThat(EnumKeyGenerator.createKey(enums)).isEqualTo("LION_TURTLE_RACCOON_");
    }
}
