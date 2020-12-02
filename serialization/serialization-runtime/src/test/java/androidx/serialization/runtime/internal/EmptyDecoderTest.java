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

import java.util.NoSuchElementException;

/**
 * Unit tests for {@link EmptyDecoder}.
 */
public final class EmptyDecoderTest {
    @NonNull
    private static final EmptyDecoder INSTANCE = EmptyDecoder.INSTANCE;

    @Test
    public void testHasNextField() {
        assertThat(INSTANCE.hasNextField()).isFalse();
    }

    @Test
    public void testNextFieldId() {
        assertThat(runCatching(new Runnable() {
            @Override
            public void run() {
                INSTANCE.nextFieldId();
            }
        })).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testDecodeMessage() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeMessage(SERIALIZER, null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedMessage() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedMessage(
                        SERIALIZER,
                        null,
                        SerializationRuntime.getListFactory()
                );
            }
        });
    }

    @Test
    public void testDecodeEnum() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeEnum(ENUM_SERIALIZER);
            }
        });
    }

    @Test
    public void testDecodeRepeatedEnum() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedEnum(
                        ENUM_SERIALIZER,
                        null,
                        SerializationRuntime.<TestEnum>getListFactory()
                );
            }
        });
    }

    @Test
    public void testDecodeBool() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeBool();
            }
        });
    }

    @Test
    public void testDecodeRepeatedBoolArray() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedBool(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedBoolCollection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedBool(null, SerializationRuntime.<Boolean>getListFactory());
            }
        });
    }

    @Test
    public void testDecodeBytes() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeBytes();
            }
        });
    }

    @Test
    public void testDecodeRepeatedBytesCollection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedBytes(null, SerializationRuntime.<byte[]>getListFactory());
            }
        });
    }

    @Test
    public void testDecodeDouble() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeDouble();
            }
        });
    }

    @Test
    public void testDecodeRepeatedDoubleArray() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedDouble(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedDoubleCollection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedDouble(null, SerializationRuntime.<Double>getListFactory());
            }
        });
    }

    @Test
    public void testDecodeFloat() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeFloat();
            }
        });
    }

    @Test
    public void testDecodeRepeatedFloatArray() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedFloat(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedFloatCollection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedFloat(null, SerializationRuntime.<Float>getListFactory());
            }
        });
    }

    @Test
    public void testDecodeInt32() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeInt32();
            }
        });
    }

    @Test
    public void testDecodeRepeatedInt32Array() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedInt32(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedInt32Collection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedInt32(null, SerializationRuntime.<Integer>getListFactory());
            }
        });
    }

    @Test
    public void testDecodeSInt32() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeSInt32();
            }
        });
    }

    @Test
    public void testDecodeRepeatedSInt32Array() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedSInt32(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedSInt32Collection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedSInt32(null, SerializationRuntime.<Integer>getListFactory());
            }
        });
    }


    @Test
    public void testDecodeUInt32() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeUInt32();
            }
        });
    }

    @Test
    public void testDecodeRepeatedUInt32Array() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedUInt32(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedUInt32Collection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedUInt32(null, SerializationRuntime.<Integer>getListFactory());
            }
        });
    }

    @Test
    public void testDecodeFixed32() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeFixed32();
            }
        });
    }

    @Test
    public void testDecodeRepeatedFixed32Array() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedFixed32(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedFixed32Collection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedFixed32(
                        null,
                        SerializationRuntime.<Integer>getListFactory()
                );
            }
        });
    }

    @Test
    public void testDecodeInt64() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeInt64();
            }
        });
    }

    @Test
    public void testDecodeRepeatedInt64Array() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedInt64(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedInt64Collection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedInt64(null, SerializationRuntime.<Long>getListFactory());
            }
        });
    }

    @Test
    public void testDecodeSInt64() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeSInt64();
            }
        });
    }

    @Test
    public void testDecodeRepeatedSInt64Array() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedSInt64(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedSInt64Collection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedSInt64(null, SerializationRuntime.<Long>getListFactory());
            }
        });
    }

    @Test
    public void testDecodeUInt64() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeUInt64();
            }
        });
    }

    @Test
    public void testDecodeRepeatedUInt64Array() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedUInt64(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedUInt64Collection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedUInt64(null, SerializationRuntime.<Long>getListFactory());
            }
        });
    }

    @Test
    public void testDecodeFixed64() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeFixed64();
            }
        });
    }

    @Test
    public void testDecodeRepeatedFixed64Array() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedFixed64(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedFixed64Collection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedFixed64(null, SerializationRuntime.<Long>getListFactory());
            }
        });
    }

    @Test
    public void testDecodeString() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeString();
            }
        });
    }

    @Test
    public void testDecodeRepeatedStringArray() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedString(null);
            }
        });
    }

    @Test
    public void testDecodeRepeatedStringCollection() {
        assertThrowsEmptyDecoderException(new Runnable() {
            @Override
            public void run() {
                INSTANCE.decodeRepeatedString(null, SerializationRuntime.<String>getListFactory());
            }
        });
    }

    private static void assertThrowsEmptyDecoderException(@NonNull Runnable runnable) {
        RuntimeException exception = runCatching(runnable);
        assertThat(exception).isInstanceOf(IllegalStateException.class);
        assertThat(exception).hasMessageThat().isEqualTo("Decoder is empty");
    }

    @Nullable
    private static RuntimeException runCatching(@NonNull Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException exception) {
            return exception;
        }
        return null;
    }

    private enum TestEnum {
        TEST
    }

    @NonNull
    private static final EnumSerializerV1<TestEnum> ENUM_SERIALIZER =
            new EnumSerializerV1<TestEnum>() {
                @Override
                public int encode(@NonNull TestEnum value) {
                    return 0;
                }

                @NonNull
                @Override
                public TestEnum decode(int valueId) {
                    return TestEnum.TEST;
                }
            };

    @NonNull
    private static final SerializerV1<Object> SERIALIZER = new SerializerV1<Object>() {
        @Override
        public void encode(@NonNull EncoderV1 encoder, @NonNull Object message) {
        }

        @NonNull
        @Override
        public Object decode(@NonNull DecoderV1 decoder, @Nullable Object mergeFrom) {
            return new Object();
        }
    };
}
