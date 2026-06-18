/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.operations.layout.modifiers;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.paint.PaintChanges;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

@RunWith(JUnit4.class)
public class BorderModifierOperationTest {
    private PaintContext mPaintContext;
    private RemoteContext mRemoteContext;

    @Before
    public void setUp() {
        mPaintContext = mock(PaintContext.class);
        mRemoteContext = mock(RemoteContext.class);
        when(mPaintContext.getContext()).thenReturn(mRemoteContext);
        when(mPaintContext.getDensityBehavior()).thenReturn(CoreDocument.DENSITY_BEHAVIOR_PIXELS);

        // Default to new behavior
        when(mRemoteContext.supportsVersion(1, 1, 0)).thenReturn(true);
        CoreDocument doc = mock(CoreDocument.class);
        when(mRemoteContext.getDocument()).thenReturn(doc);
    }

    @Test
    public void testRectangleBorder() {
        BorderModifierOperation op = new BorderModifierOperation(
                0, 0, 1, 0,
                10f, // borderWidth
                0f,  // roundedCorner
                1f, 0f, 0f, 1f, // color red
                ShapeType.RECTANGLE
        );
        op.layout(mRemoteContext, null, 100f, 100f);
        op.paint(mPaintContext);

        // Verify drawRect is called with insets (halfStroke = 5f)
        verify(mPaintContext).drawRect(5f, 5f, 95f, 95f);
        verify(mPaintContext, never()).drawRoundRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                anyFloat(), anyFloat());

        // Verify paint properties (stroke style)
        ArgumentCaptor<PaintBundle> paintCaptor = ArgumentCaptor.forClass(PaintBundle.class);
        verify(mPaintContext).replacePaint(paintCaptor.capture());
        PaintBundle paint = paintCaptor.getValue();
        PaintChanges paintChanges = mock(PaintChanges.class);
        paint.applyPaintChange(mPaintContext, paintChanges);
        verify(paintChanges).setStyle(PaintBundle.STYLE_STROKE);
        verify(paintChanges).setStrokeWidth(10f);
    }

    @Test
    public void testRectangleBorderFilled() {
        BorderModifierOperation op = new BorderModifierOperation(
                0, 0, 1, 0,
                60f, // borderWidth >= width/2
                0f,  // roundedCorner
                1f, 0f, 0f, 1f, // color red
                ShapeType.RECTANGLE
        );
        op.layout(mRemoteContext, null, 100f, 100f);
        op.paint(mPaintContext);

        // Verify drawRect is called without insets (filled)
        verify(mPaintContext).drawRect(0f, 0f, 100f, 100f);
        verify(mPaintContext, never()).drawRoundRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                anyFloat(), anyFloat());

        // Verify paint properties (fill style)
        ArgumentCaptor<PaintBundle> paintCaptor = ArgumentCaptor.forClass(PaintBundle.class);
        verify(mPaintContext).replacePaint(paintCaptor.capture());
        PaintBundle paint = paintCaptor.getValue();
        PaintChanges paintChanges = mock(PaintChanges.class);
        paint.applyPaintChange(mPaintContext, paintChanges);
        verify(paintChanges).setStyle(PaintBundle.STYLE_FILL);
    }

    @Test
    public void testRoundRectBorder() {
        BorderModifierOperation op = new BorderModifierOperation(
                0, 0, 1, 0,
                10f, // borderWidth
                20f, // roundedCorner
                1f, 0f, 0f, 1f, // color red
                ShapeType.ROUNDED_RECTANGLE
        );
        op.layout(mRemoteContext, null, 100f, 100f);
        op.paint(mPaintContext);

        // Verify drawRoundRect is called with insets (halfStroke = 5f, cornerRadius shunk by 5f)
        verify(mPaintContext).drawRoundRect(5f, 5f, 95f, 95f, 15f, 15f);
        verify(mPaintContext, never()).drawRect(anyFloat(), anyFloat(), anyFloat(), anyFloat());

        // Verify paint properties (stroke style)
        ArgumentCaptor<PaintBundle> paintCaptor = ArgumentCaptor.forClass(PaintBundle.class);
        verify(mPaintContext).replacePaint(paintCaptor.capture());
        PaintBundle paint = paintCaptor.getValue();
        PaintChanges paintChanges = mock(PaintChanges.class);
        paint.applyPaintChange(mPaintContext, paintChanges);
        verify(paintChanges).setStyle(PaintBundle.STYLE_STROKE);
        verify(paintChanges).setStrokeWidth(10f);
    }

    @Test
    public void testRoundRectBorderFilled() {
        BorderModifierOperation op = new BorderModifierOperation(
                0, 0, 1, 0,
                60f, // borderWidth >= width/2
                20f, // roundedCorner
                1f, 0f, 0f, 1f, // color red
                ShapeType.ROUNDED_RECTANGLE
        );
        op.layout(mRemoteContext, null, 100f, 100f);
        op.paint(mPaintContext);

        // Verify drawRoundRect is called without insets (filled)
        verify(mPaintContext).drawRoundRect(0f, 0f, 100f, 100f, 20f, 20f);
        verify(mPaintContext, never()).drawRect(anyFloat(), anyFloat(), anyFloat(), anyFloat());

        // Verify paint properties (fill style)
        ArgumentCaptor<PaintBundle> paintCaptor = ArgumentCaptor.forClass(PaintBundle.class);
        verify(mPaintContext).replacePaint(paintCaptor.capture());
        PaintBundle paint = paintCaptor.getValue();
        PaintChanges paintChanges = mock(PaintChanges.class);
        paint.applyPaintChange(mPaintContext, paintChanges);
        verify(paintChanges).setStyle(PaintBundle.STYLE_FILL);
    }

    @Test
    public void testCircleBorder() {
        BorderModifierOperation op = new BorderModifierOperation(
                0, 0, 1, 0,
                10f, // borderWidth
                0f,  // roundedCorner ignored for CIRCLE
                1f, 0f, 0f, 1f, // color red
                ShapeType.CIRCLE
        );
        op.layout(mRemoteContext, null, 100f, 80f);
        op.paint(mPaintContext);

        // Circle radius size = min(width, height) / 2f = 40f
        // Inset halfStroke = 5f
        // Shrunk corner radius rx = 40f - 5f = 35f
        verify(mPaintContext).drawRoundRect(5f, 5f, 95f, 75f, 35f, 35f);
        verify(mPaintContext, never()).drawRect(anyFloat(), anyFloat(), anyFloat(), anyFloat());

        // Verify paint properties (stroke style)
        ArgumentCaptor<PaintBundle> paintCaptor = ArgumentCaptor.forClass(PaintBundle.class);
        verify(mPaintContext).replacePaint(paintCaptor.capture());
        PaintBundle paint = paintCaptor.getValue();
        PaintChanges paintChanges = mock(PaintChanges.class);
        paint.applyPaintChange(mPaintContext, paintChanges);
        verify(paintChanges).setStyle(PaintBundle.STYLE_STROKE);
        verify(paintChanges).setStrokeWidth(10f);
    }



    @Test
    public void testRectangleBorderLegacy() {
        BorderModifierOperation op = new BorderModifierOperation(
                0, 0, 0, 0, // reserve1 = 0 (old behavior)
                10f, // borderWidth
                0f,  // roundedCorner
                1f, 0f, 0f, 1f, // color red
                ShapeType.RECTANGLE
        );
        op.layout(mRemoteContext, null, 100f, 100f);
        op.paint(mPaintContext);

        // Verify drawRect is called with 0f coordinates (no insets)
        verify(mPaintContext).drawRect(0f, 0f, 100f, 100f);
        verify(mPaintContext, never()).drawRoundRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                anyFloat(), anyFloat());
    }

    @Test
    public void testRoundRectBorderLegacy() {
        BorderModifierOperation op = new BorderModifierOperation(
                0, 0, 0, 0, // reserve1 = 0 (old behavior)
                10f, // borderWidth
                20f, // roundedCorner
                1f, 0f, 0f, 1f, // color red
                ShapeType.ROUNDED_RECTANGLE
        );
        op.layout(mRemoteContext, null, 100f, 100f);
        op.paint(mPaintContext);

        // Verify drawRoundRect is called with 0f coordinates (no insets, cornerRadius not shrunk)
        verify(mPaintContext).drawRoundRect(0f, 0f, 100f, 100f, 20f, 20f);
        verify(mPaintContext, never()).drawRect(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }
}
