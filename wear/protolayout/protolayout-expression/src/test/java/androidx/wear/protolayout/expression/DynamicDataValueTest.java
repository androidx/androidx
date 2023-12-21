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

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class DynamicDataValueTest {
  @Test
  public void boolDynamicDataValue() {
    DynamicDataValue<DynamicBool> boolDynamicDataValue = DynamicDataValue.fromBool(true);

    assertThat(boolDynamicDataValue.toDynamicDataValueProto().getBoolVal().getValue()).isTrue();
  }

  @Test
  public void colorDynamicDataValue() {
    DynamicDataValue<DynamicColor> colorDynamicDataValue = DynamicDataValue.fromColor(0xff00ff00);

    assertThat(colorDynamicDataValue.toDynamicDataValueProto().getColorVal().getArgb())
        .isEqualTo(0xff00ff00);
  }

  @Test
  public void floatDynamicDataValue() {
    DynamicDataValue<DynamicFloat> floatDynamicDataValue = DynamicDataValue.fromFloat(42.42f);

    assertThat(floatDynamicDataValue.toDynamicDataValueProto().getFloatVal().getValue())
        .isWithin(0.0001f)
        .of(42.42f);
  }

  @Test
  public void intDynamicDataValue() {
    DynamicDataValue<DynamicInt32> intDynamicDataValue = DynamicDataValue.fromInt(42);

    assertThat(intDynamicDataValue.toDynamicDataValueProto().getInt32Val().getValue())
            .isEqualTo(42);
  }

  @Test
  public void stringDynamicDataValue() {
    DynamicDataValue<DynamicString> stringDynamicDataValue =
            DynamicDataValue.fromString("constant-value");

    assertThat(stringDynamicDataValue.toDynamicDataValueProto().getStringVal().getValue())
        .isEqualTo("constant-value");
  }
}
