/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.integration.view.demos.examples.old;

import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.ConditionalOperations;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demos for flow control mechanisms (loops and conditionals) in RemoteCompose.
 */
@SuppressLint("RestrictedApiAndroidX")
public class FlowControlChecks {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private FlowControlChecks() {
    }

    /**
     * Demo showing basic conditional drawing.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter testConditional() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 6, 0,
                sPlatform);
        rc.root(() -> {
            rc.startBox(new RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START);
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float cx = rc.floatExpression(w, 2, DIV);
            float cy = rc.floatExpression(h, 2, DIV);
            rc.drawRoundRect(0, 0, w, h, 100, 100);
            rc.getPainter().setColor(Color.YELLOW).setTextSize(100f).commit();
            rc.drawTextAnchored("expression in eval loop", cx, cy, 0.0f, 0.0f, 0);
            float tick = rc.floatExpression(
                    rc.exp(Rc.Time.TIME_IN_SEC, 3, MOD, 1, SUB, cx, cy, MIN, MUL, 10, ADD),
                    rc.anim(0.5f, Rc.Animate.CUBIC_STANDARD | (1 << 12)));
            float odd = rc.floatExpression(rc.exp(Rc.Time.TIME_IN_SEC, 3, MOD, 1, SUB));
            rc.floatExpression(Rc.Time.CONTINUOUS_SEC);
            rc.drawRect(0, cy, tick, h);
            rc.addDebugMessage(" color ", odd);

            rc.conditionalOperations(Rc.Condition.GT, odd, 0f);
            rc.getPainter().setColor(Color.RED).commit();
            rc.drawCircle(cx, cy, tick);
            rc.endConditionalOperations();

            rc.conditionalOperations(Rc.Condition.LT, odd, 0f);
            rc.getPainter().setColor(Color.GREEN).commit();

            rc.drawCircle(cx, cy, tick);
            rc.endConditionalOperations();
            rc.endCanvas();
            rc.endBox();
        });
        return rc;
    }

    /**
     * Demo showing nested loops and various static equality checks.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter flowControlChecks1() {
        RemoteComposeWriter rc = new RemoteComposeWriter(500, 500, "sd", 6, 0, sPlatform);
        rc.root(() -> {
            rc.startBox(new RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START);
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            rc.drawRoundRect(0, 0, w, h, 64, 64);
            rc.addDebugMessage(">>> simple loop:");
            float i = rc.createFloatId();
            rc.loop(Utils.idFromNan(i), 0f, 1f, 10f, () -> {
                rc.addDebugMessage("  >>> count", i);
            });
            rc.addDebugMessage(">>> nested loop:");
            float k = rc.createFloatId();

            rc.loop(Utils.idFromNan(i), 0f, 1f, 3f, () -> {
                rc.addDebugMessage("  >>> count", i);
                rc.loop(Utils.idFromNan(k), 0f, 1f, 3f, () -> {
                    rc.addDebugMessage("        >>> count2",
                            rc.floatExpression(k, i, 100, MUL, ADD));
                });
            });
            rc.conditionalOperations(ConditionalOperations.TYPE_EQ, 3, 3);
            rc.addDebugMessage(" >>> static Equality check");
            rc.endConditionalOperations();

            rc.loop(Utils.idFromNan(i), 0f, 1f, 3f, () -> {
                rc.addDebugMessage("  >>> count", i);
                rc.loop(Utils.idFromNan(k), 0f, 1f, 3f, () -> {
                    rc.addDebugMessage("        >>> count2",
                            rc.floatExpression(k, i, 100, MUL, ADD));
                    rc.conditionalOperations(ConditionalOperations.TYPE_EQ, 3, 3);
                    rc.addDebugMessage("        >>> static Equality check");
                    rc.endConditionalOperations();
                    rc.conditionalOperations(ConditionalOperations.TYPE_EQ, 2, 3);
                    rc.addDebugMessage(
                            "        >>> static Equality FAIL should not" + " see this");
                    rc.endConditionalOperations();
                });
            });
            float ans = rc.floatExpression(10, 10, MUL, 10, ADD);
            rc.conditionalOperations(ConditionalOperations.TYPE_EQ, ans, 110);
            rc.addDebugMessage(" >>> 10*10+10 = 110 ", ans);
            rc.endConditionalOperations();

            rc.loop(Utils.idFromNan(i), 0f, 1f, 3f, () -> {
                rc.addDebugMessage("  >>> count", i);
                rc.loop(Utils.idFromNan(k), 0f, 1f, 3f, () -> {
                    rc.addDebugMessage("        >>> count2",
                            rc.floatExpression(k, i, 100, MUL, ADD));
                    rc.conditionalOperations(ConditionalOperations.TYPE_EQ, k, i);
                    rc.addDebugMessage("        >>> static Equality check k = i", k);
                    rc.endConditionalOperations();
                    rc.conditionalOperations(ConditionalOperations.TYPE_NEQ, k, i);
                    rc.addDebugMessage("        >>> static inequality check k != i", k);
                    rc.endConditionalOperations();
                });
            });
            rc.loop(Utils.idFromNan(i), 0f, 1f, 60f, () -> {
                float tmp = rc.floatExpression(i, 10, MUL);
                rc.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f,
                        rc.floatExpression(i, 15, MOD));
                rc.addDebugMessage(">>>>> i,15,MOD ", tmp);
                rc.endConditionalOperations();
            });
            rc.endCanvas();
            rc.endBox();
        });
        return rc;
    }

    /**
     * Demo showing basic loops and debug messages.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter flowControlChecks2() {
        RemoteComposeWriter rc = new RemoteComposeWriter(500, 500, "sd", 6, 0, sPlatform);
        rc.root(() -> {
            rc.startBox(new RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START);
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();

            rc.drawRoundRect(0, 0, w, h, 64, 64);
            rc.addDebugMessage(">>> simple loop:");
            float i = rc.createFloatId();
            rc.loop(Utils.idFromNan(i), 0f, 1f, 60f, () -> {
                float tmp = rc.floatExpression(i, 10, MUL);
                rc.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f,
                        rc.floatExpression(i, 15, MOD));
                rc.addDebugMessage(">>>>> i,15,MOD ", tmp);
                rc.endConditionalOperations();
            });
            rc.endCanvas();
            rc.endBox();
        });
        return rc;
    }
}
