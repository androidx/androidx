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
import static org.junit.Assert.assertThrows;

import androidx.camera.core.Configuration.Option;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class OptionsBundleRobolectricTest {

  private static final Option<Object> OPTION_1 = Option.create("option.1", Object.class);
  private static final Option<Object> OPTION_1_A = Option.create("option.1.a", Object.class);
  private static final Option<Object> OPTION_2 = Option.create("option.2", Object.class);
  private static final Option<Object> OPTION_MISSING =
      Option.create("option.missing", Object.class);

  private static final Object VALUE_1 = new Object();
  private static final Object VALUE_1_A = new Object();
  private static final Object VALUE_2 = new Object();
  private static final Object VALUE_MISSING = new Object();

  private OptionsBundle allOpts;

  @Before
  public void setUp() {
    MutableOptionsBundle mutOpts = MutableOptionsBundle.create();
    mutOpts.insertOption(OPTION_1, VALUE_1);
    mutOpts.insertOption(OPTION_1_A, VALUE_1_A);
    mutOpts.insertOption(OPTION_2, VALUE_2);

    allOpts = OptionsBundle.from(mutOpts);
  }

  @Test
  public void canRetrieveValue() {
    assertThat(allOpts.retrieveOption(OPTION_1)).isSameAs(VALUE_1);
    assertThat(allOpts.retrieveOption(OPTION_1_A)).isSameAs(VALUE_1_A);
    assertThat(allOpts.retrieveOption(OPTION_2)).isSameAs(VALUE_2);
  }

  @Test
  public void willReturnDefault_ifOptionIsMissing() {
    Object value = allOpts.retrieveOption(OPTION_MISSING, VALUE_MISSING);
    assertThat(value).isSameAs(VALUE_MISSING);
  }

  @Test
  public void willReturnStoredValue_whenGivenDefault() {
    Object value = allOpts.retrieveOption(OPTION_1, VALUE_MISSING);
    assertThat(value).isSameAs(VALUE_1);
  }

  @Test
  public void canListOptions() {
    Set<Option<?>> list = allOpts.listOptions();
    for (Option<?> opt : list) {
      assertThat(opt).isAnyOf(OPTION_1, OPTION_1_A, OPTION_2);
    }

    assertThat(list).hasSize(3);
  }

  @Test
  public void canCreateCopyOptionsBundle() {
    OptionsBundle copyBundle = OptionsBundle.from(allOpts);

    assertThat(copyBundle.containsOption(OPTION_1)).isTrue();
    assertThat(copyBundle.containsOption(OPTION_1_A)).isTrue();
    assertThat(copyBundle.containsOption(OPTION_2)).isTrue();
  }

  @Test
  public void canFindPartialIds() {
    allOpts.findOptions(
        "option.1",
        option -> {
          assertThat(option).isAnyOf(OPTION_1, OPTION_1_A);
          return true;
        });
  }

  @Test
  public void canStopSearchingAfterFirstMatch() {
    AtomicInteger count = new AtomicInteger();
    allOpts.findOptions(
        "option",
        option -> {
          count.getAndIncrement();
          return false;
        });

    assertThat(count.get()).isEqualTo(1);
  }

  @Test
  public void canGetZeroResults_fromFind() {
    AtomicInteger count = new AtomicInteger();
    allOpts.findOptions(
        "invalid_find_string",
        option -> {
          count.getAndIncrement();
          return false;
        });

    assertThat(count.get()).isEqualTo(0);
  }

  @Test
  public void canRetrieveValue_fromFindLambda() {
    AtomicReference<Object> value = new AtomicReference<>(VALUE_MISSING);
    allOpts.findOptions(
        "option.2",
        option -> {
          value.set(allOpts.retrieveOption(option));
          return true;
        });

    assertThat(value.get()).isSameAs(VALUE_2);
  }

  @Test
  public void retrieveMissingOption_willThrow() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          allOpts.retrieveOption(OPTION_MISSING);
        });
  }
}
