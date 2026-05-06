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
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;
import androidx.compose.remote.integration.view.demos.R;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

/**
 * Demos for using variable fonts.
 */
@SuppressLint("RestrictedApiAndroidX")
public class VariableFontDemo {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private VariableFontDemo() {
    }

    static @NonNull RemoteComposeWriterAndroid makeRc() {
        return new RemoteComposeWriterAndroid(
                        300,
                        300,
                        "Clock",
                        CoreDocument.DOCUMENT_API_LEVEL,
                        RcProfiles.PROFILE_ANDROIDX,
                sPlatform);
    }

    /**
     * Demo using system typeface as a font with multiple axis animations.
     * @param activity the activity context.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static @NonNull RemoteComposeWriter demo4(@NonNull Activity activity) {
        RemoteComposeWriterAndroid rc = makeRc();
        rc.floatExpression(Rc.Time.CONTINUOUS_SEC);
        float w = rc.floatExpression(Rc.System.WINDOW_WIDTH);
        float w2 = rc.floatExpression(Rc.System.WINDOW_WIDTH, 2, DIV);
        float h = rc.floatExpression(Rc.System.WINDOW_HEIGHT);
        float cx = rc.floatExpression(w, 0.5f, MUL);
        rc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit();
        float right1 = rc.floatExpression(w, 20, SUB);
        float bottom1 = rc.floatExpression(h, 2, DIV, 10, SUB);
        rc.drawRoundRect(20, 20, right1, bottom1, 32, 32);
        float top = rc.floatExpression(bottom1, 20, ADD);

        float bottom2 = rc.floatExpression(h, 20, SUB);
        rc.drawRoundRect(20, top, right1, bottom2, 32, 32);

        float cy1 = rc.floatExpression(20, bottom1, ADD, 2, DIV);
        float cy2 = rc.floatExpression(top, bottom2, ADD, 2, DIV);

        float fontSize = rc.floatExpression(w2, 2, DIV);
        float weight = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 1, MOD, 800, MUL, 100f, ADD);
        float m1 = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 2, MOD, 1, SUB);
        float m2 = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 4, DIV, 2, MOD, 1, SUB);
        float t1 = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 1, MOD);
        float t2 = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 2, DIV, 1, MOD);

        int fontDataId = rc.addFont(getFontBytesFromResource(activity, 0));
        rc.getPainter()
                .setTextSize(fontSize)
                .setTypeface(fontDataId)
                .setAxis(
                        new String[]{"wght", "T1  ", "T2  ", "M1  ", "M2  "},
                        new float[]{weight, t1, t2, m1, m2})
                .setColor(Color.RED)
                .setStyle(Paint.Style.FILL)
                .commit();

        rc.drawTextAnchored("\uD83C\uDF1D➟", cx, cy1, 0, 0, 0);

        rc.drawTextAnchored("\uD83D\uDC22\uD83D\uDD12", cx, cy2, 0, 0, 0);

        return rc;
    }

    /**
     * Demo showing variable font axis animations (weight, width, CASL).
     * @param activity the activity context.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static @NonNull RemoteComposeWriter demo3(@NonNull Activity activity) {
        RemoteComposeWriterAndroid rc = makeRc();
        rc.floatExpression(Rc.Time.CONTINUOUS_SEC);
        float w = rc.floatExpression(Rc.System.WINDOW_WIDTH);
        float w2 = rc.floatExpression(Rc.System.WINDOW_WIDTH, 2, DIV);
        float h = rc.floatExpression(Rc.System.WINDOW_HEIGHT);
        float cx1 = rc.floatExpression(w2, 0.5f, MUL);
        float cx2 = rc.floatExpression(w2, 1.5f, MUL);
        float cy = rc.floatExpression(h, 0.5f, MUL);
        rc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit();
        float right1 = rc.floatExpression(w2, 10, SUB);
        float bottom1 = rc.floatExpression(h, 20, SUB);
        rc.drawRoundRect(20, 20, right1, bottom1, 32, 32);
        float left2 = rc.floatExpression(w2, 10, ADD);
        float right2 = rc.floatExpression(w, 20, SUB);
        rc.drawRoundRect(left2, 20, right2, bottom1, 32, 32);
        float fontSize = rc.floatExpression(w2, 2, DIV);
        float weight =
                rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 8, DIV, 1, MOD, 800, MUL, 100f, ADD);
        float width =
                rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 4, DIV, 1, MOD, 50f, MUL, 75f, ADD);
        float t1 = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 1, MOD);

        int fontDataId = rc.addFont(getFontBytesFromResource(activity, 0));
        rc.getPainter()
                .setTextSize(fontSize)
                .setTypeface(fontDataId)
                .setAxis(new String[]{"wght", "wdth", "CASL"}, new float[]{weight, width, t1})
                .setColor(Color.RED)
                .setStyle(Paint.Style.FILL)
                .commit();
        DecimalFormat df = new DecimalFormat("00");
        float count = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 100, MOD);
        int txtId = rc.createTextFromFloat(count, 2, 0, 0);
        rc.drawTextAnchored(txtId, cx1, cy, 0, 0, 0);

        rc.drawTextAnchored(df.format(23f), cx2, cy, 0, 0, 0);

        return rc;
    }

    /**
     * Loads a font from a Resource id.
     *
     * @param context        app context.
     * @param fontResourceId font resource id.
     * @return byte array of the font.
     */
    static byte @Nullable [] getFontBytesFromResource(@NonNull Context context,
            int fontResourceId) {
        InputStream inputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            // Get the Resources object from the context
            Resources resources = context.getResources();

            // Open an InputStream to the raw font resource
            inputStream = resources.openRawResource(fontResourceId);

            // Create a ByteArrayOutputStream to read the bytes into memory
            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; // A common buffer size
            int bytesRead;

            // Read bytes from the InputStream and write them to the ByteArrayOutputStream
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            // Return the byte array
            return byteArrayOutputStream.toByteArray();

        } catch (Resources.NotFoundException e) {
            System.err.println("Font resource not found: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error reading font resource: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure streams are closed to prevent resource leaks
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing InputStream: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing ByteArrayOutputStream: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null; // Return null if any error occurred
    }

    /**
     * Demo showing variable font loaded from a raw resource.
     * @param activity the activity context.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static @NonNull RemoteComposeWriter demo2(@NonNull Activity activity) {
        RemoteComposeWriterAndroid rc = makeRc();
        rc.floatExpression(Rc.Time.CONTINUOUS_SEC);
        float w = rc.floatExpression(Rc.System.WINDOW_WIDTH);
        float w2 = rc.floatExpression(Rc.System.WINDOW_WIDTH, 2, DIV);
        float h = rc.floatExpression(Rc.System.WINDOW_HEIGHT);
        float cx1 = rc.floatExpression(w2, 0.5f, MUL);
        float cx2 = rc.floatExpression(w2, 1.5f, MUL);
        float cy = rc.floatExpression(h, 0.5f, MUL);
        rc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit();
        float right1 = rc.floatExpression(w2, 10, SUB);
        float bottom1 = rc.floatExpression(h, 20, SUB);
        rc.drawRoundRect(20, 20, right1, bottom1, 32, 32);
        float left2 = rc.floatExpression(w2, 10, ADD);
        float right2 = rc.floatExpression(w, 20, SUB);
        rc.drawRoundRect(left2, 20, right2, bottom1, 32, 32);
        float fontSize = rc.floatExpression(w2, 2, DIV);
        float weight = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 1, MOD, 800, MUL, 100f, ADD);
        float width = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 1, MOD, 50f, MUL, 75f, ADD);
        byte[] data;
        try {
            InputStream inputStream =
                    activity.getResources().openRawResource(
                            R.raw.fancy_clock2);
            data = inputStream.readAllBytes();
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Utils.log("red the data size: " + data.length);
        int fontDataId = rc.addFont(data);
        Utils.log("red the data size: " + data.length);

        rc.getPainter()
                .setTextSize(fontSize)
                .setTypeface(fontDataId)
                .setAxis(new String[]{"wght", "wdth"}, new float[]{weight, width})
                .setColor(Color.RED)
                .setStyle(Paint.Style.FILL)
                .commit();
        DecimalFormat df = new DecimalFormat("00");

        rc.drawTextAnchored(df.format(12f), cx1, cy, 0, 0, 0);

        rc.drawTextAnchored("AS", cx2, cy, 0, 0, 0);

        return rc;
    }

    /**
     * Demo showing variable font specified by name.
     * @param activity the activity context.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter demo1(@NonNull Activity activity) {
        RemoteComposeWriterAndroid rc = makeRc();
        rc.floatExpression(Rc.Time.CONTINUOUS_SEC);
        float w = rc.floatExpression(Rc.System.WINDOW_WIDTH);
        float w2 = rc.floatExpression(Rc.System.WINDOW_WIDTH, 2, DIV);
        float h = rc.floatExpression(Rc.System.WINDOW_HEIGHT);
        float cx1 = rc.floatExpression(w2, 0.5f, MUL);
        float cx2 = rc.floatExpression(w2, 1.5f, MUL);
        float cy = rc.floatExpression(h, 0.5f, MUL);
        rc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit();
        float right1 = rc.floatExpression(w2, 10, SUB);
        float bottom1 = rc.floatExpression(h, 20, SUB);
        rc.drawRoundRect(20, 20, right1, bottom1, 32, 32);
        float left2 = rc.floatExpression(w2, 10, ADD);
        float right2 = rc.floatExpression(w, 20, SUB);
        rc.drawRoundRect(left2, 20, right2, bottom1, 32, 32);
        float fontSize = rc.floatExpression(w2, 2, DIV);
        float weight = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 1, MOD, 800, MUL, 100f, ADD);
        float width = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 1, MOD, 50f, MUL, 75f, ADD);

        rc.getPainter()
                .setTextSize(fontSize)
                .setTypeface("RobotoFlex-Regular")
                .setAxis(new String[]{"wght", "wdth"}, new float[]{weight, width})
                .setColor(Color.RED)
                .setStyle(Paint.Style.FILL)
                .commit();
        DecimalFormat df = new DecimalFormat("00");

        rc.drawTextAnchored(df.format(12f), cx1, cy, 0, 0, 0);

        rc.drawTextAnchored("AS", cx2, cy, 0, 0, 0);

        return rc;
    }

}
