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

import android.util.Log;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.creation.RemoteComposeContext;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class LoopOperationTest {

    private final Platform mPlatform = new AndroidxPlatformServices();

    interface Callback {
        void run(RemoteComposeContext foo);
    }

    private RemoteComposeDocument createDocument(RemoteContext context, final Callback cb) {

        RemoteComposeContext doc =
                new RemoteComposeContext(
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
        debugContext.setHideString(true);

        RemoteComposeDocument doc = createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        return debugContext.getTestResults();
    }

    String drawCommandList(Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setHideString(false);

        RemoteComposeDocument doc = createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        return doc.toString();
    }

    @Test
    public void testLoop() {

        Callback cb =
                rdoc -> {
                    rdoc.root(
                            () -> {
                                rdoc.canvas(
                                        new RecordingModifier().size(100),
                                        () -> {
                                            rdoc.loop(
                                                    0,
                                                    0,
                                                    1,
                                                    3,
                                                    () -> {
                                                        rdoc.drawCircle(10, 20, 30);
                                                        rdoc.drawLine(0f, 0f, 100f, 100f);
                                                    });
                                        });
                            });
                };

        String result = drawCommandTest(cb);
        Log.v("TEST", result);
        String expected =
                "header(1, 1, 0) 600 x 600, 0\n"
                        + "loadText(42)\n"
                        + "setTheme(-1)\n"
                        + "matrixSave()\n"
                        + "clipRect(0.0, 0.0, 0.0, 0.0)\n"
                        + "matrixSave()\n"
                        + "translate (0.0, 0.0)\n"
                        + "translate (-0.0, -0.0)\n"
                        + "translate (0.0, 0.0)\n"
                        + "matrixSave()\n"
                        + "translate (0.0, 0.0)\n"
                        + "drawCircle(10.0, 20.0, 30.0)\n"
                        + "drawLine(0.0, 0.0, 100.0, 100.0)\n"
                        + "drawCircle(10.0, 20.0, 30.0)\n"
                        + "drawLine(0.0, 0.0, 100.0, 100.0)\n"
                        + "drawCircle(10.0, 20.0, 30.0)\n"
                        + "drawLine(0.0, 0.0, 100.0, 100.0)\n"
                        + "matrixRestore()\n"
                        + "translate (-0.0, -0.0)\n"
                        + "matrixRestore()\n"
                        + "matrixRestore()\n";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);
    }

    @Test
    public void testLoopFrom() {

        Callback cb =
                rdoc -> {
                    rdoc.root(
                            () -> {
                                rdoc.canvas(
                                        new RecordingModifier().size(100),
                                        () -> {
                                            rdoc.loop(
                                                    0,
                                                    1,
                                                    1,
                                                    3,
                                                    () -> {
                                                        rdoc.drawCircle(10, 20, 30);
                                                        rdoc.drawLine(0f, 0f, 100f, 100f);
                                                    });
                                        });
                            });
                };

        String result = drawCommandTest(cb);
        Log.v("TEST", result);
        String expected =
                "header(1, 1, 0) 600 x 600, 0\n"
                        + "loadText(42)\n"
                        + "setTheme(-1)\n"
                        + "matrixSave()\n"
                        + "clipRect(0.0, 0.0, 0.0, 0.0)\n"
                        + "matrixSave()\n"
                        + "translate (0.0, 0.0)\n"
                        + "translate (-0.0, -0.0)\n"
                        + "translate (0.0, 0.0)\n"
                        + "matrixSave()\n"
                        + "translate (0.0, 0.0)\n"
                        + "drawCircle(10.0, 20.0, 30.0)\n"
                        + "drawLine(0.0, 0.0, 100.0, 100.0)\n"
                        + "drawCircle(10.0, 20.0, 30.0)\n"
                        + "drawLine(0.0, 0.0, 100.0, 100.0)\n"
                        + "matrixRestore()\n"
                        + "translate (-0.0, -0.0)\n"
                        + "matrixRestore()\n"
                        + "matrixRestore()\n";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);
    }

    @Test
    public void testLoopStep() {

        Callback cb =
                rdoc -> {
                    rdoc.root(
                            () -> {
                                rdoc.canvas(
                                        new RecordingModifier().size(100),
                                        () -> {
                                            rdoc.loop(
                                                    0,
                                                    1,
                                                    2,
                                                    3,
                                                    () -> {
                                                        rdoc.drawCircle(10, 20, 30);
                                                        rdoc.drawLine(0f, 0f, 100f, 100f);
                                                    });
                                        });
                            });
                };

        String result = drawCommandTest(cb);
        Log.v("TEST", result);
        String expected =
                "header(1, 1, 0) 600 x 600, 0\n"
                        + "loadText(42)\n"
                        + "setTheme(-1)\n"
                        + "matrixSave()\n"
                        + "clipRect(0.0, 0.0, 0.0, 0.0)\n"
                        + "matrixSave()\n"
                        + "translate (0.0, 0.0)\n"
                        + "translate (-0.0, -0.0)\n"
                        + "translate (0.0, 0.0)\n"
                        + "matrixSave()\n"
                        + "translate (0.0, 0.0)\n"
                        + "drawCircle(10.0, 20.0, 30.0)\n"
                        + "drawLine(0.0, 0.0, 100.0, 100.0)\n"
                        + "matrixRestore()\n"
                        + "translate (-0.0, -0.0)\n"
                        + "matrixRestore()\n"
                        + "matrixRestore()\n";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);
    }

    @Test
    public void testLoopIndex() {

        Callback cb =
                rdoc -> {
                    rdoc.root(
                            () -> {
                                rdoc.canvas(
                                        new RecordingModifier().size(100),
                                        () -> {
                                            float index = rdoc.addFloatConstant(0f);
                                            rdoc.loop(
                                                    Utils.idFromNan(index),
                                                    10,
                                                    10,
                                                    100,
                                                    () -> {
                                                        float x =
                                                                rdoc.floatExpression(
                                                                        index,
                                                                        2,
                                                                        AnimatedFloatExpression
                                                                                .MUL);
                                                        float y =
                                                                rdoc.floatExpression(
                                                                        index,
                                                                        3,
                                                                        AnimatedFloatExpression
                                                                                .MUL);
                                                        rdoc.drawCircle(x, y, index);
                                                    });
                                        });
                            });
                };

        String result = drawCommandTest(cb);
        Log.v("TEST", result);
        String expected =
                "header(1, 1, 0) 600 x 600, 0\n"
                        + "loadText(42)\n"
                        + "setTheme(-1)\n"
                        + "matrixSave()\n"
                        + "clipRect(0.0, 0.0, 0.0, 0.0)\n"
                        + "matrixSave()\n"
                        + "translate (0.0, 0.0)\n"
                        + "translate (-0.0, -0.0)\n"
                        + "translate (0.0, 0.0)\n"
                        + "matrixSave()\n"
                        + "translate (0.0, 0.0)\n"
                        + "drawCircle(20.0, 30.0, 10.0)\n"
                        + "drawCircle(40.0, 60.0, 20.0)\n"
                        + "drawCircle(60.0, 90.0, 30.0)\n"
                        + "drawCircle(80.0, 120.0, 40.0)\n"
                        + "drawCircle(100.0, 150.0, 50.0)\n"
                        + "drawCircle(120.0, 180.0, 60.0)\n"
                        + "drawCircle(140.0, 210.0, 70.0)\n"
                        + "drawCircle(160.0, 240.0, 80.0)\n"
                        + "drawCircle(180.0, 270.0, 90.0)\n"
                        + "matrixRestore()\n"
                        + "translate (-0.0, -0.0)\n"
                        + "matrixRestore()\n"
                        + "matrixRestore()\n";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);
    }
}
