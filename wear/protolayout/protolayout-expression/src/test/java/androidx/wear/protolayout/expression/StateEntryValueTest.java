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

import androidx.wear.protolayout.expression.StateEntryBuilders.StateEntryValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class StateEntryValueTest {
  @Test
  public void boolStateEntryValue() {
    StateEntryValue boolStateEntryValue = StateEntryValue.fromBool(true);

    assertThat(boolStateEntryValue.toStateEntryValueProto().getBoolVal().getValue()).isTrue();
  }

  @Test
  public void colorStateEntryValue() {
    StateEntryValue colorStateEntryValue = StateEntryValue.fromColor(0xff00ff00);

    assertThat(colorStateEntryValue.toStateEntryValueProto().getColorVal().getArgb())
        .isEqualTo(0xff00ff00);
  }

  @Test
  public void floatStateEntryValue() {
    StateEntryValue floatStateEntryValue = StateEntryValue.fromFloat(42.42f);

    assertThat(floatStateEntryValue.toStateEntryValueProto().getFloatVal().getValue())
        .isWithin(0.0001f)
        .of(42.42f);
  }

  @Test
  public void intStateEntryValue() {
    StateEntryValue intStateEntryValue = StateEntryValue.fromInt(42);

    assertThat(intStateEntryValue.toStateEntryValueProto().getInt32Val().getValue()).isEqualTo(42);
  }

  @Test
  public void stringStateEntryValue() {
    StateEntryValue stringStateEntryValue = StateEntryValue.fromString("constant-value");

    assertThat(stringStateEntryValue.toStateEntryValueProto().getStringVal().getValue())
        .isEqualTo("constant-value");
  }
}
