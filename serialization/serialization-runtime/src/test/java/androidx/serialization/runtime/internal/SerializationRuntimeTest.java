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

package androidx.serialization.runtime.internal;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * Unit tests for {@link SerializationRuntime}.
 */
public final class SerializationRuntimeTest {
    @Test
    public void testDefaultInstanceOf() {
        assertThat(SerializationRuntime.defaultInstanceOf(FooSerializer.INSTANCE))
                .isInstanceOf(Foo.class);

        assertThat(SerializationRuntime.defaultInstanceOf(FooSerializer.INSTANCE).getBar())
                .isEqualTo(0);
    }

    @Test
    public void testGetListFactory() {
        assertThat(SerializationRuntime.getListFactory().create(0))
                .isSameInstanceAs(Collections.emptyList());

        assertThat(SerializationRuntime.getListFactory().create(1))
                .isInstanceOf(ArrayList.class);
    }

    @Test
    public void testGetSetFactory() {
        assertThat(SerializationRuntime.getSetFactory().create(0))
                .isSameInstanceAs(Collections.emptySet());

        assertThat(SerializationRuntime.getSetFactory().create(1))
                .isInstanceOf(LinkedHashSet.class);
    }

    private static final class Foo {
        private int mBar;

        Foo(int bar) {
            mBar = bar;
        }

        public int getBar() {
            return mBar;
        }
    }

    private static final class FooSerializer implements SerializerV1<Foo> {
        @NonNull
        public static final FooSerializer INSTANCE = new FooSerializer();

        @Override
        public void encode(@NonNull EncoderV1 encoder, @NonNull Foo message) {
            encoder.encodeInt32(1, message.getBar());
        }

        @NonNull
        @Override
        public Foo decode(@NonNull DecoderV1 decoder, @Nullable Foo mergeFrom) {
            int bar = 0;

            while (decoder.hasNextField()) {
                if (decoder.nextFieldId() == 1) {
                    bar = decoder.decodeInt32();
                }
            }

            return new Foo(bar);
        }
    }
}
