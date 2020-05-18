/*
 * Copyright 2019 The Android Open Source Project
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

import static androidx.camera.core.impl.Config.OptionPriority.ALWAYS_OVERRIDE;
import static androidx.camera.core.impl.Config.OptionPriority.OPTIONAL;
import static androidx.camera.core.impl.Config.OptionPriority.REQUIRED;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.Config.Option;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class OptionsBundleTest {

    private static final Option<Object> OPTION_1 = Option.create("option.1", Object.class);
    private static final Option<Object> OPTION_1_A = Option.create("option.1.a", Object.class);
    private static final Option<Object> OPTION_2 = Option.create("option.2", Object.class);
    private static final Option<List<Integer>> OPTION_INTEGER_LIST = Option.create(
            "option.int_list", List.class);
    private static final Option<Object> OPTION_NULL_VALUE = Option.create("option.NullVaule",
            Object.class);
    private static final Option<Object> OPTION_MISSING =
            Option.create("option.missing", Object.class);

    private static final Object VALUE_1 = new Object();
    private static final Object VALUE_1_A = new Object();
    private static final Object VALUE_2 = new Object();
    private static final List<Integer> VALUE_INTEGER_LIST = new ArrayList<>();
    private static final Object VALUE_MISSING = new Object();

    static {
        VALUE_INTEGER_LIST.add(1);
        VALUE_INTEGER_LIST.add(2);
        VALUE_INTEGER_LIST.add(3);
    }


    private OptionsBundle mAllOpts;

    @Before
    public void setUp() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();
        // insert multiple values with priroties to ensure it is still working for regular
        // operations.
        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_2);
        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_1);
        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_1_A);


        mutOpts.insertOption(OPTION_1_A, VALUE_1_A);
        mutOpts.insertOption(OPTION_2, VALUE_2);
        mutOpts.insertOption(OPTION_INTEGER_LIST, VALUE_INTEGER_LIST);
        mutOpts.insertOption(OPTION_NULL_VALUE, null);

        mAllOpts = OptionsBundle.from(mutOpts);
    }

    @Test
    public void canRetrieveValue() {
        assertThat(mAllOpts.retrieveOption(OPTION_1)).isSameInstanceAs(VALUE_1);
        assertThat(mAllOpts.retrieveOption(OPTION_1_A)).isSameInstanceAs(VALUE_1_A);
        assertThat(mAllOpts.retrieveOption(OPTION_2)).isSameInstanceAs(VALUE_2);
    }

    @Test
    public void canRetrieveListOfIntegers() {
        List<Integer> list = mAllOpts.retrieveOption(OPTION_INTEGER_LIST);
        assertThat(list).isSameInstanceAs(VALUE_INTEGER_LIST);
    }

    @Test
    public void canRetrieveNullOptionValue() {
        assertThat(mAllOpts.retrieveOption(OPTION_NULL_VALUE)).isNull();
        assertThat(mAllOpts.retrieveOption(OPTION_NULL_VALUE, VALUE_MISSING)).isNull();
    }

    @Test
    public void willReturnDefault_ifOptionIsMissing() {
        Object value = mAllOpts.retrieveOption(OPTION_MISSING, VALUE_MISSING);
        assertThat(value).isSameInstanceAs(VALUE_MISSING);
    }

    @Test
    public void willReturnStoredValue_whenGivenDefault() {
        Object value = mAllOpts.retrieveOption(OPTION_1, VALUE_MISSING);
        assertThat(value).isSameInstanceAs(VALUE_1);
    }

    @Test
    public void canListOptions() {
        Set<Option<?>> list = mAllOpts.listOptions();
        for (Option<?> opt : list) {
            assertThat(opt).isAnyOf(OPTION_1, OPTION_1_A, OPTION_2, OPTION_INTEGER_LIST,
                    OPTION_NULL_VALUE);
        }

        assertThat(list).hasSize(5);
    }

    @Test
    public void canCreateCopyOptionsBundle() {
        OptionsBundle copyBundle = OptionsBundle.from(mAllOpts);

        assertThat(copyBundle.containsOption(OPTION_1)).isTrue();
        assertThat(copyBundle.containsOption(OPTION_1_A)).isTrue();
        assertThat(copyBundle.containsOption(OPTION_2)).isTrue();

        assertThat(copyBundle.getPriorities(OPTION_1))
                .containsExactly(REQUIRED, ALWAYS_OVERRIDE, OPTIONAL);
        assertThat(copyBundle.retrieveOptionWithPriority(OPTION_1, REQUIRED))
                .isEqualTo(VALUE_2);
        assertThat(copyBundle.retrieveOptionWithPriority(OPTION_1, ALWAYS_OVERRIDE))
                .isEqualTo(VALUE_1);
        assertThat(copyBundle.retrieveOptionWithPriority(OPTION_1, OPTIONAL))
                .isEqualTo(VALUE_1_A);
    }

    @Test
    public void canFindPartialIds() {
        mAllOpts.findOptions(
                "option.1",
                new Config.OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(@NonNull Option<?> option) {
                        assertThat(option).isAnyOf(OPTION_1, OPTION_1_A);
                        return true;
                    }
                });
    }

    @Test
    public void canStopSearchingAfterFirstMatch() {
        final AtomicInteger count = new AtomicInteger();
        mAllOpts.findOptions(
                "option",
                new Config.OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(@NonNull Option<?> option) {
                        count.getAndIncrement();
                        return false;
                    }
                });

        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    public void canGetZeroResults_fromFind() {
        final AtomicInteger count = new AtomicInteger();
        mAllOpts.findOptions(
                "invalid_find_string",
                new Config.OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(@NonNull Option<?> option) {
                        count.getAndIncrement();
                        return false;
                    }
                });

        assertThat(count.get()).isEqualTo(0);
    }

    @Test
    public void canRetrieveValue_fromFindLambda() {
        final AtomicReference<Object> value = new AtomicReference<>(VALUE_MISSING);
        mAllOpts.findOptions(
                "option.2",
                new Config.OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(@NonNull Option<?> option) {
                        value.set(mAllOpts.retrieveOption(option));
                        return true;
                    }
                });

        assertThat(value.get()).isSameInstanceAs(VALUE_2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void retrieveMissingOption_willThrow() {
        // Should throw IllegalArgumentException
        mAllOpts.retrieveOption(OPTION_MISSING);
    }

    @Test
    public void canRetrieveOptionWithPriority() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();
        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_2);
        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_1);
        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_1_A);

        OptionsBundle config = OptionsBundle.from(mutOpts);
        assertThat(config.retrieveOptionWithPriority(OPTION_1, REQUIRED)).isEqualTo(VALUE_2);
        assertThat(config.retrieveOptionWithPriority(OPTION_1, ALWAYS_OVERRIDE)).isEqualTo(VALUE_1);
        assertThat(config.retrieveOptionWithPriority(OPTION_1, OPTIONAL)).isEqualTo(VALUE_1_A);
    }

    @Test(expected = IllegalArgumentException.class)
    public void retrieveOptionWithPriority_noSuchPriority_willThrow() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();
        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_1);
        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_1_A);

        OptionsBundle config = OptionsBundle.from(mutOpts);
        config.retrieveOptionWithPriority(OPTION_1, REQUIRED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void retrieveOptionWithPriority_noSuchOption_willThrow() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();
        mutOpts.insertOption(OPTION_1, VALUE_1);

        OptionsBundle config = OptionsBundle.from(mutOpts);
        config.retrieveOptionWithPriority(OPTION_2, OPTIONAL);
    }

    @Test
    public void canGetPriorites() {
        assertThat(mAllOpts.getPriorities(OPTION_1))
                .containsExactly(ALWAYS_OVERRIDE, OPTIONAL, REQUIRED);
    }

    @Test
    public void canGetPriorites_empty() {
        Option<Object> newOption = Option.create("option.new", Object.class);
        assertThat(mAllOpts.getPriorities(newOption)).isEmpty();
    }

    @Test
    public void canGetOptionPriority() {
        assertThat(mAllOpts.getOptionPriority(OPTION_1)).isEqualTo(ALWAYS_OVERRIDE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOptionPriority_willThrow() {
        Option<Object> newOption = Option.create("option.new", Object.class);
        mAllOpts.getOptionPriority(newOption);
    }

}
