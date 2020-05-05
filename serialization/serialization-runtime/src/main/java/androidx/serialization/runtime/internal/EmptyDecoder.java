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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * This decoder mimics an empty buffer for constructing default instances of message classes.
 * <p>
 * As it has no internal buffer, it is also stateless. A static instance is available as
 * {@link #INSTANCE}.
 */
final class EmptyDecoder implements DecoderV1 {
    @NonNull
    public static final EmptyDecoder INSTANCE = new EmptyDecoder();

    @NonNull
    private static final String DECODE_FIELD_EXCEPTION_MESSAGE = "Decoder is empty";

    private EmptyDecoder() {
    }

    @Override
    public boolean hasNextField() {
        return false;
    }

    @Override
    public int nextFieldId() {
        throw new NoSuchElementException();
    }

    @NonNull
    @Override
    public <T> T decodeMessage(@NonNull SerializerV1<T> serializer, @Nullable T mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <T, C extends Collection<T>> C decodeRepeatedMessage(
            @NonNull SerializerV1<T> serializer,
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <T extends Enum<T>> T decodeEnum(@NonNull EnumSerializerV1<T> serializer) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <T extends Enum<T>, C extends Collection<T>> C decodeRepeatedEnum(
            @NonNull EnumSerializerV1<T> serializer,
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public boolean decodeBool() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public boolean[] decodeRepeatedBool(@Nullable boolean[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Boolean>> C decodeRepeatedBool(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public byte[] decodeBytes() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<byte[]>> C decodeRepeatedBytes(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public double decodeDouble() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public double[] decodeRepeatedDouble(@Nullable double[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Double>> C decodeRepeatedDouble(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public float decodeFloat() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public float[] decodeRepeatedFloat(@Nullable float[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Float>> C decodeRepeatedFloat(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public int decodeInt32() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public int[] decodeRepeatedInt32(@Nullable int[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Integer>> C decodeRepeatedInt32(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public int decodeSInt32() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public int[] decodeRepeatedSInt32(@Nullable int[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Integer>> C decodeRepeatedSInt32(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public int decodeUInt32() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public int[] decodeRepeatedUInt32(@Nullable int[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Integer>> C decodeRepeatedUInt32(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public int decodeFixed32() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public int[] decodeRepeatedFixed32(@Nullable int[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Integer>> C decodeRepeatedFixed32(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public long decodeInt64() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public long[] decodeRepeatedInt64(@Nullable long[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Long>> C decodeRepeatedInt64(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public long decodeSInt64() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public long[] decodeRepeatedSInt64(@Nullable long[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Long>> C decodeRepeatedSInt64(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public long decodeUInt64() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public long[] decodeRepeatedUInt64(@Nullable long[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Long>> C decodeRepeatedUInt64(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @Override
    public long decodeFixed64() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public long[] decodeRepeatedFixed64(@Nullable long[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<Long>> C decodeRepeatedFixed64(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public String decodeString() {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public String[] decodeRepeatedString(@Nullable String[] mergeFrom) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }

    @NonNull
    @Override
    public <C extends Collection<String>> C decodeRepeatedString(
            @Nullable C mergeFrom,
            @NonNull CollectionFactory<C> factory
    ) {
        throw new IllegalStateException(DECODE_FIELD_EXCEPTION_MESSAGE);
    }
}
