/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.camera.core.Config.Option;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class OptionTest {

    private static final String OPTION_1_ID = "option.1";

    private static final Object TOKEN = new Object();

    @Test
    public void canCreateOption_andRetrieveId() {
        Option<Integer> option = Option.create(OPTION_1_ID, Integer.class);
        assertThat(option.getId()).isEqualTo(OPTION_1_ID);
    }

    @Test
    public void canCreateOption_fromClass_andRetrieveClass() {
        Option<Integer> option = Option.create(OPTION_1_ID, Integer.class);
        assertThat(option.getValueClass()).isEqualTo(Integer.class);
    }

    @Test
    public void canCreateOption_fromPrimitiveClass_andRetrievePrimitiveClass() {
        Option<Integer> option = Option.create(OPTION_1_ID, int.class);
        assertThat(option.getValueClass()).isEqualTo(int.class);
    }

    @Test
    public void canCreateOption_fromGenericClass_andAssignFromNarrowClass() {
        List<Integer> intList = new ArrayList<>();
        Option<List<Integer>> option = Option.create(OPTION_1_ID, List.class);
        assertThat(intList.getClass()).isAssignableTo(option.getValueClass());
    }

    @Test
    public void canCreateOption_fromGenericClass() {
        Option<List<Integer>> option = Option.create(OPTION_1_ID, List.class);
        assertThat(option).isNotNull();
    }

    @Test
    public void canCreateOption_withNullToken() {
        Option<Integer> option = Option.create(OPTION_1_ID, Integer.class);
        assertThat(option.getToken()).isNull();
    }

    @Test
    public void canCreateOption_withToken() {
        Option<Integer> option = Option.create(OPTION_1_ID, Integer.class, TOKEN);
        assertThat(option.getToken()).isSameInstanceAs(TOKEN);
    }

    @Test
    public void canRetrieveOption_fromMap_usingSeparateOptionInstances() {
        Option<Integer> option = Option.create(OPTION_1_ID, Integer.class);
        Option<Integer> optionCopy = Option.create(OPTION_1_ID, Integer.class);

        Map<Option<?>, Object> map = new HashMap<>();
        map.put(option, 1);

        assertThat(map).containsKey(optionCopy);
        assertThat(map.get(optionCopy)).isEqualTo(1);
    }
}
