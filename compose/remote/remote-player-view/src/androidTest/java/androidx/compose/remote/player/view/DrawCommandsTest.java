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
package androidx.compose.remote.player.view;

import static androidx.compose.remote.player.view.TestUtils.diff;
import static androidx.compose.remote.player.view.TestUtils.dumpDifference;

import static org.junit.Assert.assertEquals;

import android.graphics.Path;
import android.util.Log;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.creation.RemoteComposeContextAndroid;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;
import androidx.compose.remote.serialization.yaml.YAMLSerializer;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class DrawCommandsTest {

    private final Platform mPlatform = new AndroidxPlatformServices();

    // ########################### TEST UTILS ######################################

    interface Callback {
        void run(RemoteComposeContextAndroid foo);
    }

    private RemoteComposeDocument createDocument(RemoteContext context, final Callback cb) {

        RemoteComposeContextAndroid doc =
                new RemoteComposeContextAndroid(
                        600,
                        600,
                        "Demo",
                        mPlatform,
                        doc1 -> {
                            if (cb != null) {
                                cb.run(doc1);
                            }

                            return null;
                        });

        byte[] buffer = doc.buffer();
        int bufferSize = doc.bufferSize();
        System.out.println("size of doc " + bufferSize / 1024 + "KB");
        RemoteComposeDocument recreatedDocument =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));
        recreatedDocument.initializeContext(context);
        return recreatedDocument;
    }

    String drawCommandTest(Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        RemoteComposeDocument doc = createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        return debugContext.getTestResults();
    }

    String drawCommandList(Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        RemoteComposeDocument doc = createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        return doc.toString();
    }

    String drawCommandListSerialize(Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        RemoteComposeDocument doc = createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);
        YAMLSerializer serializer = new YAMLSerializer();
        //        JSONSerializer serializer = new JSONSerializer();
        doc.serialize(serializer.serializeMap());
        return serializer.toString();
    }

    // ########################### END TEST UTILS ######################################

    @Test
    public void testDrawArc() {
        String result =
                drawCommandList(
                        rdoc -> {
                            rdoc.drawArc(10, 20, 30, 40, 1, 360);
                        });
        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "DrawArc 10.0 20.0 30.0 40.0\n"
                        + "}";
        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }

        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testDrawCircle() {
        String result =
                drawCommandList(
                        rdoc -> {
                            rdoc.drawCircle(100.0f, 200.0f, 50.0f);
                        });
        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "DrawCircle 100.0 200.0 50.0\n"
                        + "}";

        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }
        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testDrawLine() {
        String result =
                drawCommandList(
                        rdoc -> {
                            rdoc.drawLine(10, 20, 30, 40.0f);
                        });

        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "DrawLine 10.0 20.0 30.0 40.0\n"
                        + "}";
        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }

        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testDrawOval() {
        String result =
                drawCommandList(
                        rdoc -> {
                            rdoc.drawOval(10, 20, 30, 40.0f);
                        });
        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "DrawOval 10.0 20.0 30.0 40.0\n"
                        + "}";

        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }
        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testDrawPath() {
        String result =
                drawCommandList(
                        rdoc -> {
                            Path p = new Path();
                            p.reset();
                            p.quadTo(1, 2, 3, 4);
                            p.moveTo(2, 3);
                            p.close();
                            rdoc.drawPath(p);
                        });
        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PathData[43] = \"M 0.0 0.0 Q 0.0 0.0 1.0 2.0 3.0 4.0 M 2.0 3.0 Z\"\n"
                        + "DrawPath [43], 0.0, 1.0\n"
                        + "}";

        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }
        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testDrawRect() {
        String result =
                drawCommandList(
                        rdoc -> {
                            rdoc.drawRect(10, 20, 30, 40.0f);
                        });
        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "DrawRect 10.0 20.0 30.0 40.0\n"
                        + "}";

        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }
        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testDrawRoundRect() {
        String result =
                drawCommandList(
                        rdoc -> {
                            rdoc.drawRoundRect(10, 20, 30, 40.0f, 5, 6);
                        });
        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "DrawRoundRect 10.0 20.0 30.0 40.0\n"
                        + "}";

        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }
        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testDrawTextOnPath() {
        String result =
                drawCommandList(
                        rdoc -> {
                            Path p = new Path();
                            p.moveTo(1, 2);
                            p.lineTo(300, 400);
                            rdoc.drawTextOnPath("hello", p, 1, 2.0f);
                        });
        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "TextData[43] = \"hello\"\n"
                        + "PathData[44] = \"M 1.0 2.0 L 1.0 2.0 300.0 400.0\"\n"
                        + "DrawTextOnPath [43] [44] 1.0, 2.0\n"
                        + "}";

        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }
        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testDrawTextRun() {
        String result =
                drawCommandList(
                        rdoc -> {
                            rdoc.drawTextRun("world", 20, 30, 1, 5, 12.0f, 34.0f, false);
                        });
        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "TextData[43] = \"world\"\n"
                        + "DrawTextRun [43] 20, 30, 12.0, 34.0\n"
                        + "}";

        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }
        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testDrawTweenPath() {
        String result =
                drawCommandListSerialize(
                        rdoc -> {
                            Path p1 = new Path();
                            p1.moveTo(1, 2);
                            p1.lineTo(300, 400);
                            Path p2 = new Path();
                            p2.moveTo(5, 6);
                            p2.lineTo(700, 800);
                            rdoc.drawTweenPath(p1, p2, 0.5f, 0, 1);
                        });
        String expectedResult =
                "type: CoreDocument\n"
                        + "width: 0\n"
                        + "height: 0\n"
                        + "operations:\n"
                        + "- {type: TextData, textId: 42, text: Demo}\n"
                        + "- {type: RootContentDescription, contentDescriptionId: 42}\n"
                        + "- type: PathData\n"
                        + "  id: 43\n"
                        + "  path: [M, 1.0, 2.0, L, 1.0, 2.0, 300.0, 400.0]\n"
                        + "- type: PathData\n"
                        + "  id: 44\n"
                        + "  path: [M, 5.0, 6.0, L, 5.0, 6.0, 700.0, 800.0]\n"
                        + "- type: DrawTweenPath\n"
                        + "  path1Id: 43\n"
                        + "  path2Id: 44\n"
                        + "  tween: {type: Value, value: 0.5}\n"
                        + "  start: {type: Value, value: 0.0}\n"
                        + "  stop: {type: Value, value: 1.0}\n";

        if (TestUtils.diff(result, expectedResult)) {
            Log.v("TEST", result);
            TestUtils.dumpDifference(result, expectedResult);
        }
        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testPathData() {
        String result =
                drawCommandList(
                        rdoc -> {
                            Path p = new Path();
                            p.reset();
                            p.cubicTo(1, 2, 3, 4, 5, 6);
                            p.close();
                            int v = rdoc.addPathData(p);
                            rdoc.drawPath(v);
                        });
        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PathData[43] = \"M 0.0 0.0 C 0.0 0.0 1.0 2.0 3.0 4.0 5.0 6.0 Z\"\n"
                        + "DrawPath [43], 0.0, 1.0\n"
                        + "}";
        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }

        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }

    @Test
    public void testPaintData() {
        String result =
                drawCommandList(
                        rdoc -> {
                            Path p = new Path();
                            p.reset();
                            p.cubicTo(1, 2, 3, 4, 5, 6);
                            p.close();
                            int v = rdoc.addPathData(p);
                            rdoc.getPainter().setAntiAlias(true).commit();

                            rdoc.drawPath(v);
                        });

        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PathData[43] = \"M 0.0 0.0 C 0.0 0.0 1.0 2.0 3.0 4.0 5.0 6.0 Z\"\n"
                        + "PaintData \"\n"
                        + "    AntiAlias(1),\n"
                        + "\"\n"
                        + "DrawPath [43], 0.0, 1.0\n"
                        + "}";

        if (diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }
        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);
    }
}
