/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.proto.TypesProto;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TypeBuildersTest {
    private static final String STATE_KEY = "state-key";
    private static final TypeBuilders.StringProp STRING_PROP =
            new TypeBuilders.StringProp.Builder("string")
                    .setDynamicValue(
                            DynamicBuilders.DynamicString.from(new AppDataKey<>(STATE_KEY)))
                    .build();

    private static final TypeBuilders.FloatProp FLOAT_PROP =
            new TypeBuilders.FloatProp.Builder(12f)
                    .setDynamicValue(DynamicBuilders.DynamicFloat.from(new AppDataKey<>(STATE_KEY)))
                    .build();
    private static final TypeBuilders.BoolProp BOOL_PROP =
            new TypeBuilders.BoolProp.Builder(true)
                    .setDynamicValue(DynamicBuilders.DynamicBool.from(new AppDataKey<>(STATE_KEY)))
                    .build();

    @Test
    public void stringPropSupportsDynamicString() {
        TypesProto.StringProp stringPropProto = STRING_PROP.toProto();

        assertThat(stringPropProto.getValue()).isEqualTo(STRING_PROP.getValue());
        assertThat(stringPropProto.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @SuppressWarnings("deprecation") // Intentionally no static value.
    @Test
    public void stringProp_withoutStaticValue_throws() {
        assertThrows(IllegalStateException.class, new TypeBuilders.StringProp.Builder()
                .setDynamicValue(
                        DynamicBuilders.DynamicString.from(new AppDataKey<>(STATE_KEY)))::build);
    }

    @Test
    public void spanSetText_throwsWhenSetToDynamicValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LayoutElementBuilders.SpanText.Builder().setText(STRING_PROP));
    }

    @Test
    public void arcTextSetText_throwsWhenSetToDynamicValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LayoutElementBuilders.ArcText.Builder().setText(STRING_PROP));
    }

    @Test
    public void floatPropSupportsDynamicFloat() {
        TypesProto.FloatProp floatPropProto = FLOAT_PROP.toProto();

        assertThat(floatPropProto.getValue()).isEqualTo(FLOAT_PROP.getValue());
        assertThat(floatPropProto.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @SuppressWarnings("deprecation") // Intentionally no static value.
    @Test
    public void floatProp_withoutStaticValue_throws() {
        assertThrows(IllegalStateException.class, new TypeBuilders.FloatProp.Builder()
                .setDynamicValue(
                        DynamicBuilders.DynamicFloat.from(new AppDataKey<>(STATE_KEY)))::build);
    }

    @Test
    public void boolPropSupportsDynamicBool() {
        TypesProto.BoolProp boolPropProto = BOOL_PROP.toProto();

        assertThat(boolPropProto.getValue()).isEqualTo(BOOL_PROP.getValue());
        assertThat(boolPropProto.getDynamicValue().getStateSource().getSourceKey())
                .isEqualTo(STATE_KEY);
    }

    @SuppressWarnings("deprecation") // Intentionally no static value.
    @Test
    public void boolProp_withoutStaticValue_throws() {
        assertThrows(IllegalStateException.class, new TypeBuilders.BoolProp.Builder()
                .setDynamicValue(
                        DynamicBuilders.DynamicBool.from(new AppDataKey<>(STATE_KEY)))::build);
    }
}
