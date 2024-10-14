/*
 * Copyright 2024 The Android Open Source Project
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

package sun.misc;

// This code is not generated but the name must follow exactly the original names, which violate
// CheckStyle's rules (theUnsafe for instance).
// CHECKSTYLE:OFF Generated code
@SuppressWarnings({"JavaJniMissingFunction", "unused", "UnknownNullness"})
public final class Unsafe {
    public static final int INVALID_FIELD_OFFSET = -1;

    private static final sun.misc.Unsafe THE_ONE;
    private static final sun.misc.Unsafe theUnsafe;

    static {
        THE_ONE = null;
        theUnsafe = null;
    }

    private Unsafe() {
        throw new RuntimeException();
    }

    public static sun.misc.Unsafe getUnsafe() {
        throw new RuntimeException();
    }

    public long objectFieldOffset(java.lang.reflect.Field field) {
        throw new RuntimeException();
    }

    public int arrayBaseOffset(java.lang.Class<?> clazz) {
        throw new RuntimeException();
    }

    public int arrayIndexScale(java.lang.Class<?> clazz) {
        throw new RuntimeException();
    }

    private static native int getArrayBaseOffsetForComponentType(
            java.lang.Class<?> componentClass);

    private static native int getArrayIndexScaleForComponentType(
            java.lang.Class<?> componentClass);

    public native boolean compareAndSwapInt(
            java.lang.Object obj, long offset, int expectedValue, int newValue);

    public native boolean compareAndSwapLong(
            java.lang.Object obj, long offset, long expectedValue, long newValue);

    public native boolean compareAndSwapObject(java.lang.Object obj, long offset,
            java.lang.Object expectedValue, java.lang.Object newValue);

    public native int getIntVolatile(java.lang.Object obj, long offset);

    public native void putIntVolatile(java.lang.Object obj, long offset, int newValue);

    public native long getLongVolatile(java.lang.Object obj, long offset);

    public native void putLongVolatile(java.lang.Object obj, long offset, long newValue);

    public native java.lang.Object getObjectVolatile(java.lang.Object obj, long offset);

    public native void putObjectVolatile(
            java.lang.Object obj, long offset, java.lang.Object newValue);

    public native int getInt(java.lang.Object obj, long offset);

    public native void putInt(java.lang.Object obj, long offset, int newValue);

    public native void putOrderedInt(java.lang.Object obj, long offset, int newValue);

    public native long getLong(java.lang.Object obj, long offset);

    public native void putLong(java.lang.Object obj, long offset, long newValue);

    public native void putOrderedLong(java.lang.Object obj, long offset, long newValue);

    public native java.lang.Object getObject(java.lang.Object obj, long offset);

    public native void putObject(java.lang.Object obj, long offset, java.lang.Object newValue);

    public native void putOrderedObject(
            java.lang.Object obj, long offset, java.lang.Object newValue);

    public native boolean getBoolean(java.lang.Object obj, long offset);

    public native void putBoolean(java.lang.Object obj, long offset, boolean newValue);

    public native byte getByte(java.lang.Object obj, long offset);

    public native void putByte(java.lang.Object obj, long offset, byte newValue);

    public native char getChar(java.lang.Object obj, long offset);

    public native void putChar(java.lang.Object obj, long offset, char newValue);

    public native short getShort(java.lang.Object obj, long offset);

    public native void putShort(java.lang.Object obj, long offset, short newValue);

    public native float getFloat(java.lang.Object obj, long offset);

    public native void putFloat(java.lang.Object obj, long offset, float newValue);

    public native double getDouble(java.lang.Object obj, long offset);

    public native void putDouble(java.lang.Object obj, long offset, double newValue);

    public void park(boolean absolute, long time) {
        throw new RuntimeException();
    }

    public void unpark(java.lang.Object obj) {
        throw new RuntimeException();
    }

    public native java.lang.Object allocateInstance(java.lang.Class<?> c);

    public native int addressSize();

    public native int pageSize();

    public native long allocateMemory(long bytes);

    public native void freeMemory(long address);

    public native void setMemory(long address, long bytes, byte value);

    public native byte getByte(long address);

    public native void putByte(long address, byte x);

    public native short getShort(long address);

    public native void putShort(long address, short x);

    public native char getChar(long address);

    public native void putChar(long address, char x);

    public native int getInt(long address);

    public native void putInt(long address, int x);

    public native long getLong(long address);

    public native void putLong(long address, long x);

    public native float getFloat(long address);

    public native void putFloat(long address, float x);

    public native double getDouble(long address);

    public native void putDouble(long address, double x);

    public native void copyMemoryToPrimitiveArray(
            long srcAddr, java.lang.Object dst, long dstOffset, long bytes);

    public native void copyMemoryFromPrimitiveArray(
            java.lang.Object src, long srcOffset, long dstAddr, long bytes);

    public native void copyMemory(long srcAddr, long dstAddr, long bytes);

    public int getAndAddInt(java.lang.Object o, long offset, int delta) {
        throw new RuntimeException();
    }

    public long getAndAddLong(java.lang.Object o, long offset, long delta) {
        throw new RuntimeException();
    }

    public int getAndSetInt(java.lang.Object o, long offset, int newValue) {
        throw new RuntimeException();
    }

    public long getAndSetLong(java.lang.Object o, long offset, long newValue) {
        throw new RuntimeException();
    }

    public java.lang.Object getAndSetObject(
            java.lang.Object o, long offset, java.lang.Object newValue) {
        throw new RuntimeException();
    }

    public native void loadFence();

    public native void storeFence();

    public native void fullFence();
}
