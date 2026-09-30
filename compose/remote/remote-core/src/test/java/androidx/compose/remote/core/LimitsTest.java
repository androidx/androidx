/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.operations.BitmapData;

import org.junit.Test;

import java.util.ArrayList;

public class LimitsTest {

    @Test
    public void testMaxOpCount() {
        int original = Limits.MAX_OP_COUNT;
        try {
            Limits.MAX_OP_COUNT = 100;
            assertEquals(100, Limits.MAX_OP_COUNT);
        } finally {
            Limits.MAX_OP_COUNT = original;
        }
    }

    @Test
    public void testMaxImageDimension() {
        int original = Limits.MAX_IMAGE_DIMENSION;
        try {
            Limits.MAX_IMAGE_DIMENSION = 100;
            assertEquals(100, Limits.MAX_IMAGE_DIMENSION);

            // Test that BitmapData.read respects the limit
            WireBuffer buffer = new WireBuffer();
            buffer.start(Operations.DATA_BITMAP);
            buffer.writeInt(1); // imageId
            buffer.writeInt(101); // width > limit
            buffer.writeInt(50); // height
            buffer.writeBuffer(new byte[10]);
            buffer.setIndex(0);
            buffer.readByte(); // consume OP_CODE

            assertThrows(
                    RuntimeException.class,
                    () -> {
                        BitmapData.read(buffer, new ArrayList<>());
                    });

            // Test within limit
            WireBuffer buffer2 = new WireBuffer();
            buffer2.start(Operations.DATA_BITMAP);
            buffer2.writeInt(2); // imageId
            buffer2.writeInt(100); // width == limit
            buffer2.writeInt(50); // height
            buffer2.writeBuffer(new byte[10]);
            buffer2.setIndex(0);
            buffer2.readByte(); // consume OP_CODE

            BitmapData.read(buffer2, new ArrayList<>()); // Should not throw
        } finally {
            Limits.MAX_IMAGE_DIMENSION = original;
        }
    }

    @Test
    public void testImageUrlsDisabledByDefault_fails() {
        boolean originalEnableImageUrls = Limits.ENABLE_IMAGE_URLS;
        try {
            Limits.ENABLE_IMAGE_URLS = false;

            WireBuffer buffer = new WireBuffer();
            byte[] urlBytes = "http://example.com/test.png".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            BitmapData.apply(
                    buffer,
                    1, // imageId
                    (short) BitmapData.TYPE_PNG,
                    (short) 10, // width
                    (short) BitmapData.ENCODING_URL,
                    (short) 10, // height
                    urlBytes
            );
            buffer.setIndex(0);
            buffer.readByte(); // consume OP_CODE

            RuntimeException e = assertThrows(
                    RuntimeException.class,
                    () -> {
                        BitmapData.read(buffer, new ArrayList<>());
                    });
            assertTrue(e.getMessage().contains("URL image not supported"));
        } finally {
            Limits.ENABLE_IMAGE_URLS = originalEnableImageUrls;
        }
    }

    @Test
    public void testEnsureOffscreenBitmapRespectsLimits() {
        int origMaxDim = Limits.MAX_IMAGE_DIMENSION;
        int origMaxMem = Limits.MAX_BITMAP_MEMORY;
        try {
            Limits.MAX_IMAGE_DIMENSION = 1000;
            Limits.MAX_BITMAP_MEMORY = 500 * 500 * 4;

            MacroTest.MockRemoteContext context = new MacroTest.MockRemoteContext();
            BitmapData bd =
                    new BitmapData(
                            1,
                            BitmapData.TYPE_RAW8888,
                            (short) 1,
                            BitmapData.ENCODING_COMPONENT_OFFSCREEN_BUFFER,
                            (short) 1,
                            new byte[0]);

            // Width exceeds MAX_IMAGE_DIMENSION
            context.mLastComponent =
                    new androidx.compose.remote.core.operations.layout.Component(
                            null, 10, -1, 0f, 0f, 1001f, 100f);
            assertThrows(RuntimeException.class, () -> bd.ensureOffscreenBitmap(context));

            // Height exceeds MAX_IMAGE_DIMENSION
            context.mLastComponent =
                    new androidx.compose.remote.core.operations.layout.Component(
                            null, 10, -1, 0f, 0f, 100f, 1001f);
            assertThrows(RuntimeException.class, () -> bd.ensureOffscreenBitmap(context));

            // Total bitmap memory (reqW * reqH * 4) exceeds MAX_BITMAP_MEMORY
            context.mLastComponent =
                    new androidx.compose.remote.core.operations.layout.Component(
                            null, 10, -1, 0f, 0f, 600f, 600f);
            assertThrows(RuntimeException.class, () -> bd.ensureOffscreenBitmap(context));

            // Within both MAX_IMAGE_DIMENSION and MAX_BITMAP_MEMORY
            context.mLastComponent =
                    new androidx.compose.remote.core.operations.layout.Component(
                            null, 10, -1, 0f, 0f, 400f, 400f);
            bd.ensureOffscreenBitmap(context);
            assertEquals(400, bd.getWidth());
            assertEquals(400, bd.getHeight());
        } finally {
            Limits.MAX_IMAGE_DIMENSION = origMaxDim;
            Limits.MAX_BITMAP_MEMORY = origMaxMem;
        }
    }

    @Test
    public void testAddMesh2DUvAndColorsLimits() {
        // 1. uvLen exceeds MAX_MESH_2D_VERTICES * 2
        WireBuffer uvBuffer = new WireBuffer();
        uvBuffer.writeInt(1); // meshId
        uvBuffer.writeInt(androidx.compose.remote.core.operations.AddMesh2D.TYPE_VALUES);
        uvBuffer.writeInt(0); // layout
        uvBuffer.writeInt(2); // uCount
        uvBuffer.writeInt(2); // vCount
        uvBuffer.writeInt(0); // flags
        uvBuffer.writeInt(0); // aux
        uvBuffer.writeInt(0); // indexCount
        uvBuffer.writeInt(0); // vertsLen
        uvBuffer.writeInt((Limits.MAX_MESH_2D_VERTICES + 1) * 2); // uvLen > limit
        uvBuffer.writeInt(0); // colorsLen
        uvBuffer.setIndex(0);
        assertThrows(
                RuntimeException.class,
                () ->
                        androidx.compose.remote.core.operations.AddMesh2D.read(
                                uvBuffer, new ArrayList<>()));

        // 2. colorsLen exceeds MAX_MESH_2D_VERTICES
        WireBuffer colorsBuffer = new WireBuffer();
        colorsBuffer.writeInt(1); // meshId
        colorsBuffer.writeInt(androidx.compose.remote.core.operations.AddMesh2D.TYPE_VALUES);
        colorsBuffer.writeInt(0); // layout
        colorsBuffer.writeInt(2); // uCount
        colorsBuffer.writeInt(2); // vCount
        colorsBuffer.writeInt(0); // flags
        colorsBuffer.writeInt(0); // aux
        colorsBuffer.writeInt(0); // indexCount
        colorsBuffer.writeInt(0); // vertsLen
        colorsBuffer.writeInt(0); // uvLen
        colorsBuffer.writeInt(Limits.MAX_MESH_2D_VERTICES + 1); // colorsLen > limit
        colorsBuffer.setIndex(0);
        assertThrows(
                RuntimeException.class,
                () ->
                        androidx.compose.remote.core.operations.AddMesh2D.read(
                                colorsBuffer, new ArrayList<>()));
    }

    @Test
    public void testEventActionOperationDataLenLimit() {
        WireBuffer buffer = new WireBuffer();
        buffer.writeInt(0); // version
        buffer.writeInt(1); // type
        buffer.writeInt(0); // filter
        buffer.writeShort(1); // commonFlags: FLAG_UNCONDITIONAL (hasData = true)
        buffer.writeShort(0); // flags
        buffer.writeInt(Limits.MAX_FUNCTION_ARGUMENTS + 1); // dataLen > limit
        buffer.setIndex(0);

        assertThrows(
                RuntimeException.class,
                () ->
                        androidx.compose.remote.core.operations.EventActionOperation.read(
                                buffer, new ArrayList<>()));
    }

    @Test
    public void testFloatFunctionDefineGlobalDepthLimit() {
        MacroTest.MockRemoteContext context = new MacroTest.MockRemoteContext();
        int chainLength = 20;
        androidx.compose.remote.core.operations.FloatFunctionDefine[] functions =
                new androidx.compose.remote.core.operations.FloatFunctionDefine[chainLength];
        for (int i = 0; i < chainLength; i++) {
            functions[i] =
                    new androidx.compose.remote.core.operations.FloatFunctionDefine(
                            100 + i, new int[0]);
        }
        for (int i = 0; i < chainLength - 1; i++) {
            final androidx.compose.remote.core.operations.FloatFunctionDefine nextFn =
                    functions[i + 1];
            functions[i]
                    .getList()
                    .add(
                            new Operation() {
                                @Override
                                public void write(WireBuffer buffer) {}

                                @Override
                                public void apply(RemoteContext context) {
                                    nextFn.execute(context);
                                }

                                @Override
                                public String deepToString(String indent) {
                                    return "";
                                }
                            });
        }

        RuntimeException e =
                assertThrows(RuntimeException.class, () -> functions[0].execute(context));
        assertTrue(e.getMessage().contains("Recursion not allowed"));
    }
}

