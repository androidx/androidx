/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.core.operations.loom;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.SystemInfo;
import androidx.compose.remote.core.WireBuffer;

import org.jspecify.annotations.NonNull;

import java.util.Set;

/** A specialized WireBuffer that handles ID remapping during patterns expansion. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LoomWireBuffer extends WireBuffer {
    private final WireBuffer mWrapped;
    private final RemapContext mContext;

    public LoomWireBuffer(@NonNull WireBuffer wrapped, @NonNull RemapContext context) {
        mWrapped = wrapped;
        mContext = context;
    }

    @Override
    public int declareId() {
        return mContext.declareId(mWrapped.readInt());
    }

    @Override
    public int readId() {
        return mContext.resolveId(mWrapped.readInt());
    }

    @Override
    public float readNanId() {
        return mContext.resolveNanId(mWrapped.readFloat());
    }

    @Override
    public long readLongNanId() {
        return mContext.resolveLongNanId(mWrapped.readLong());
    }

    @NonNull
    public RemapContext getRemapContext() {
        return mContext;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Delegation to wrapped buffer
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public @NonNull SystemInfo getSystemInfo() {
        return mWrapped.getSystemInfo();
    }

    @Override
    public void setSystemInfo(@NonNull SystemInfo systemInfo) {
        mWrapped.setSystemInfo(systemInfo);
    }

    @Override
    public byte @NonNull [] getBuffer() {
        return mWrapped.getBuffer();
    }

    @Override
    public int getMax_size() {
        return mWrapped.getMax_size();
    }

    @Override
    public int getIndex() {
        return mWrapped.getIndex();
    }

    @Override
    public int getSize() {
        return mWrapped.getSize();
    }

    @Override
    public void setIndex(int index) {
        mWrapped.setIndex(index);
    }

    @Override
    public void start(int type) {
        mWrapped.start(type);
    }

    @Override
    public void startWithSize(int type) {
        mWrapped.startWithSize(type);
    }

    @Override
    public void endWithSize() {
        mWrapped.endWithSize();
    }

    @Override
    public void reset(int expectedSize) {
        mWrapped.reset(expectedSize);
    }

    @Override
    public int size() {
        return mWrapped.size();
    }

    @Override
    public boolean available() {
        return mWrapped.available();
    }

    @Override
    public int readOperationType() {
        return mWrapped.readOperationType();
    }

    @Override
    public boolean readBoolean() {
        return mWrapped.readBoolean();
    }

    @Override
    public int readByte() {
        return mWrapped.readByte();
    }

    @Override
    public int readShort() {
        return mWrapped.readShort();
    }

    @Override
    public int peekInt() {
        return mWrapped.peekInt();
    }

    @Override
    public int readInt() {
        return mWrapped.readInt();
    }

    @Override
    public long readLong() {
        return mWrapped.readLong();
    }

    @Override
    public float readFloat() {
        return mWrapped.readFloat();
    }

    @Override
    public double readDouble() {
        return mWrapped.readDouble();
    }

    @Override
    public byte @NonNull [] readBuffer() {
        return mWrapped.readBuffer();
    }

    @Override
    public byte @NonNull [] readBuffer(int maxSize) {
        return mWrapped.readBuffer(maxSize);
    }

    @Override
    public @NonNull String readUTF8() {
        return mWrapped.readUTF8();
    }

    @Override
    public @NonNull String readUTF8(int maxSize) {
        return mWrapped.readUTF8(maxSize);
    }

    @Override
    public void writeBoolean(boolean value) {
        mWrapped.writeBoolean(value);
    }

    @Override
    public void writeByte(int value) {
        mWrapped.writeByte(value);
    }

    @Override
    public void writeShort(int value) {
        mWrapped.writeShort(value);
    }

    @Override
    public void writeInt(int value) {
        mWrapped.writeInt(value);
    }

    @Override
    public void overwriteInt(int position, int value) {
        mWrapped.overwriteInt(position, value);
    }

    @Override
    public void writeLong(long value) {
        mWrapped.writeLong(value);
    }

    @Override
    public void writeFloat(float value) {
        mWrapped.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) {
        mWrapped.writeDouble(value);
    }

    @Override
    public void writeBuffer(byte @NonNull [] b) {
        mWrapped.writeBuffer(b);
    }

    @Override
    public void write(byte @NonNull [] b) {
        mWrapped.write(b);
    }

    @Override
    public void writeUTF8(@NonNull String content) {
        mWrapped.writeUTF8(content);
    }

    @Override
    public byte @NonNull [] cloneBytes() {
        return mWrapped.cloneBytes();
    }

    @Override
    public void setVersion(int documentApiLevel, int profiles) {
        mWrapped.setVersion(documentApiLevel, profiles);
    }

    @Override
    public void setValidOperations(@NonNull Set<Integer> supportedOperations) {
        mWrapped.setValidOperations(supportedOperations);
    }

    @Override
    public void moveBlock(int beyond, int insertLocation) {
        mWrapped.moveBlock(beyond, insertLocation);
    }
}
