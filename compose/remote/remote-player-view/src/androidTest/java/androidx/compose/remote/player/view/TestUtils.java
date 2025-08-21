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

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Environment;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.creation.RemoteComposeContextAndroid;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;
import androidx.compose.remote.player.view.platform.RemoteComposeView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

public class TestUtils {

    private static final Platform sPlatform = new AndroidxPlatformServices();

    static String getMethodName() {
        return getMethodName(2);
    }

    static String getMethodName(int methodNum) {
        return new Throwable().getStackTrace()[methodNum].getMethodName();
    }

    static boolean diff(String a, String b) {
        String[] as = a.split("\n");
        String[] bs = b.split("\n");
        boolean ret = false;

        if (as.length != bs.length) {
            ret = true;
        }
        int max = Math.max(as.length, bs.length);
        String mark = new String(new char[50]).replace('0', '-');
        for (int i = 0; i < max; i++) {

            if (i >= as.length) {
                ret = true;
                continue;
            }
            if (i >= bs.length) {
                ret = true;
                continue;
            }
            if (!bs[i].trim().equals(as[i].trim())) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    static void dumpDifference(String expected, String result) {
        String[] e = expected.split("\n");
        String[] r = result.split("\n");
        int len = 0;
        for (int i = 0; i < r.length; i++) {
            len = Math.max(r[i].length(), len);
        }
        for (int i = 0; i < e.length; i++) {
            len = Math.max(e[i].length(), len);
        }

        System.out.println("------------------------------------------------");
        for (int i = 0; i < Math.max(e.length, r.length); i++) {
            String split = (e.length > i && r.length > i && e[i].equals(r[i])) ? "=" : "*";
            if (e.length > i) {
                String gap = new String(new char[len - e[i].length()]).replace('\0', ' ') + split;
                System.out.print(gap + e[i] + gap);
            }
            if (r.length > i) {
                System.out.print(r[i]);
            }

            System.out.println();
        }
        System.out.println("------------------------------------------------");
    }

    /**
     * Useful to test if you are saving the image typically you would look at them in a terminal :
     * adb pull /storage/emulated/0/Android/data/androidx.compose.remote.player.test/
     * files/Pictures/remoteBitmap.png
     * /storage/emulated/0/Android/data/androidx.compose.remote.player.test/
     * files/Pictures/localBitmap.png /tmp/
     *
     * <p>open /tmp/*.png
     *
     * @param appContext typically InstrumentationRegistry.getInstrumentation().getTargetContext();
     * @param bitmap
     * @param fileName
     */
    static void saveBitmap(Context appContext, Bitmap bitmap, String fileName) {
        File storageDir =
                appContext.getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES); // Using internal storage
        File imageFile = new File(storageDir, fileName);
        System.out.println("## adb pull " + imageFile.getAbsolutePath() + " /tmp/");

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            // Compress and write the bitmap to the file
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Verify the file was saved successfully
        assertTrue(imageFile.exists());
    }

    static String drawCommandList(Callback run) {
        //        int tw = 600;
        //        int th = 600;
        //        DebugPlayerContext debugContext = new DebugPlayerContext();
        //        debugContext.setHideString(false);
        //        RemoteComposeDocument doc = createDocument(debugContext, run);
        //        doc.paint(debugContext, Theme.UNSPECIFIED);

        //        return doc.toString();
        // Temporary remove YAML
        //        byte[] rawDoc = TestSerializeUtils.createDoc(run);
        //        return TestSerializeUtils.toYamlString(rawDoc);
        return "";
    }

    static String drawCommandList(Callback run, String command) {
        // Temporary remove YAML
        //        byte[] rawDoc = TestSerializeUtils.createDoc(run);
        //        return TestSerializeUtils.toYamlString(rawDoc, command);
        return "";
    }

    /**
     * Utility to emulate draw anchored text for testing purposes
     *
     * @param str text to draw
     * @param x anchor position
     * @param y anchor position
     * @param panX -1 = text is left of anchor, 0=center, 1=right
     * @param panY -1 = text is above anchor, 0=center, 1=below
     * @param canvas the canvas to draw on
     * @param paint the paint to use
     */
    public static void drawTextAnchored(
            String str, float x, float y, float panX, float panY, Canvas canvas, Paint paint) {
        Rect bounds = new Rect();
        paint.getTextBounds(str, 0, str.length(), bounds);

        float boxHeight = 0;
        float textHeight = (bounds.bottom - bounds.top);
        float yOff = (boxHeight - textHeight) * (1 - panY) / 2 - (bounds.top);
        float textWidth = (bounds.right - bounds.left);
        float boxWidth = 0;
        float xOff = (boxWidth - textWidth) * (1 + panX) / 2.f - (bounds.left);
        canvas.drawText(str, x + xOff, y + yOff, paint);
    }

    //    String drawCommandTest(DrawBitmapScaledTest.Callback run) {
    //        int tw = 600;
    //        int th = 600;
    //        DebugPlayerContext debugContext = new DebugPlayerContext();
    //        debugContext.setHideString(false);
    //
    //        RemoteComposeDocument doc = createDocument(debugContext, run);
    //        doc.paint(debugContext, Theme.UNSPECIFIED);
    //
    //        return debugContext.getTestResults();
    //    }

    /**
     * Save a document to a file
     *
     * @param name
     * @param doc
     * @param appContext
     */
    public static void saveDoc(String name, RemoteComposeDocument doc, Context appContext) {
        WireBuffer wb = doc.getDocument().getBuffer().getBuffer();
        int len = wb.getSize();
        byte[] buff = wb.getBuffer();

        File storageDir =
                appContext.getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES); // Using internal storage
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File imageFile = new File(storageDir, name);
        System.out.println("adb pull " + imageFile.getAbsolutePath() + " /tmp/");

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            // Compress and write the bitmap to the file
            fos.write(buff, 0, len);
            System.out.println("writing " + len + " bytes");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("done.");
    }

    /**
     * returns the document
     *
     * @param name
     * @param appContext
     * @return
     */
    public static RemoteComposeDocument getDoc(String name, Context appContext) {
        File storageDir =
                appContext.getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES); // Using internal storage
        File imageFile = new File(storageDir, name);
        System.out.println("getting " + imageFile.getAbsolutePath());

        try (FileInputStream fis = new FileInputStream(imageFile)) {
            // Compress and write the bitmap to the file
            RemoteComposeDocument doc = new RemoteComposeDocument(fis);
            return doc;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean gold = false;

    /**
     * Capture gold file
     *
     * @param name
     * @param doc
     * @param appContext
     */
    public static void captureGold(String name, RemoteComposeDocument doc, Context appContext) {
        if (!gold) {
            return;
        }
        name = toCamelCase(name);
        String fileName = name + ".rcd";
        System.out.println("saveing " + fileName);
        TestUtils.saveDoc(fileName, doc, appContext);
        RemoteComposeDocument fileDoc = TestUtils.getDoc(fileName, appContext);
        Bitmap fromFileBitmap = docToBitmap(600, 600, appContext, fileDoc);
        TestUtils.saveBitmap(appContext, fromFileBitmap, name + ".png");
    }

    /**
     * Convert to CamelCase
     *
     * @param input
     * @return
     */
    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty() || !input.contains("_")) {
            return input; // return the input if it's null or empty or has no "_"
        }

        StringBuilder camelCaseString = new StringBuilder();
        boolean nextCharToUpper = false;

        // Convert the first character to lowercase to ensure camelCase
        camelCaseString.append(Character.toLowerCase(input.charAt(0)));

        // Start from the second character
        for (int i = 1; i < input.length(); i++) {
            char currentChar = input.charAt(i);

            if (currentChar == '_') {
                nextCharToUpper = true; // Mark the next character to be converted to uppercase
            } else {
                if (nextCharToUpper) {
                    camelCaseString.append(Character.toUpperCase(currentChar));
                    nextCharToUpper = false; // Reset the flag
                } else {
                    camelCaseString.append(Character.toLowerCase(currentChar));
                }
            }
        }

        return camelCaseString.toString();
    }

    /** callback interface */
    public interface Callback {
        /**
         * functor
         *
         * @param foo context
         */
        void run(RemoteComposeContextAndroid foo);
    }

    static RemoteComposeDocument createDocument(RemoteContext context, final Callback cb) {
        RemoteComposeContextAndroid doc =
                new RemoteComposeContextAndroid(
                        600,
                        600,
                        "Demo",
                        sPlatform,
                        doc1 -> {
                            if (cb != null) {
                                cb.run(doc1);
                            }

                            return null;
                        });

        byte[] buffer = doc.buffer();
        int bufferSize = doc.bufferSize();
        System.out.println("size of doc " + memSize(bufferSize));
        RemoteComposeDocument recreatedDocument =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));
        return recreatedDocument;
    }

    private static String memSize(int size) {
        DecimalFormat df = new DecimalFormat("#.0##");
        if (size > 1024 * 1024) {
            return df.format(size / (1024 * 1024f)) + "MB";
        }
        if (size > 1024) {
            return df.format(size / (1024f)) + "KB";
        }
        return size + " Bytes";
    }

    /**
     * Creates a test image with a complex pattern
     *
     * @param tw width of image
     * @param th height of the image
     * @param darkTheme draw in a dark theme
     * @return Bitmap
     */
    static Bitmap createImage(int tw, int th, boolean darkTheme) {
        Bitmap image = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();

        Canvas canvas = new Canvas(image);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3f);
        if (darkTheme) {
            paint.setColor(Color.BLACK);
        } else {
            paint.setColor(Color.WHITE);
        }
        canvas.drawRect(tw / 3, tw / 3, tw * 2 / 3, th * 2 / 3, paint);
        paint.setColor(Color.RED);
        canvas.drawLine(0f, 0f, tw, th, paint);
        canvas.drawLine(0f, th, tw, 0f, paint);
        paint.setColor(Color.BLUE);
        canvas.drawLine(300f, 0f, 300f, 600f, paint);
        canvas.drawLine(0f, 300f, 600f, 300f, paint);
        String text = "R";
        Rect bounds = new Rect();
        paint.setAntiAlias(true);

        paint.setShader(
                new RadialGradient(
                        tw / 2f, th / 2f, tw / 2f, Color.WHITE, Color.BLUE, Shader.TileMode.CLAMP));

        paint.setTextSize(Math.min(tw, th));
        paint.getTextBounds(text, 0, text.length(), bounds);
        paint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText(text, (tw) / 2f, (th - (paint.descent() + paint.ascent())) / 2f, paint);
        return image;
    }

    /**
     * Creates a test alpha 8 image with a complex pattern
     *
     * @param tw width of image
     * @param th height of the image
     * @return Bitmap
     */
    static Bitmap createAlpha8Image(int tw, int th) {
        Bitmap image = Bitmap.createBitmap(tw, th, Bitmap.Config.ALPHA_8);
        Paint paint = new Paint();

        Canvas canvas = new Canvas(image);
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.WHITE);
        canvas.drawRect(tw / 3, tw / 3, tw * 2 / 3, th * 2 / 3, paint);
        paint.setColor(Color.DKGRAY);
        canvas.drawLine(0f, 0f, tw, th, paint);
        canvas.drawLine(0f, th, tw, 0f, paint);
        paint.setColor(Color.GRAY);
        canvas.drawLine(300f, 0f, 300f, 600f, paint);
        canvas.drawLine(0f, 300f, 600f, 300f, paint);
        String text = "R";
        Rect bounds = new Rect();
        paint.setAntiAlias(true);

        paint.setShader(
                new RadialGradient(
                        tw / 2f,
                        th / 2f,
                        tw / 2f,
                        Color.WHITE,
                        Color.BLACK,
                        Shader.TileMode.CLAMP));

        paint.setTextSize(Math.min(tw, th));
        paint.getTextBounds(text, 0, text.length(), bounds);
        paint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText(text, (tw) / 2f, (th - (paint.descent() + paint.ascent())) / 2f, paint);
        return image;
    }

    static float compareImages(Bitmap bitmap1, Bitmap bitmap2) {
        if (bitmap1.getWidth() != bitmap2.getWidth()
                || bitmap1.getHeight() != bitmap2.getHeight()) {
            return 0;
        }
        float sqr_sum = 0;
        int count = 0;
        for (int y = 0; y < bitmap1.getHeight(); y++) {
            for (int x = 0; x < bitmap1.getWidth(); x++) {
                int pix1 = bitmap1.getPixel(x, y);
                float r1 = (pix1 & 0xFF0000) >> 16;
                float g1 = (pix1 & 0xFF00) >> 8;
                float b1 = (pix1 & 0xFF);
                int pix2 = bitmap2.getPixel(x, y);
                float r2 = (pix2 & 0xFF0000) >> 16;
                float g2 = (pix2 & 0xFF00) >> 8;
                float b2 = (pix2 & 0xFF);
                sqr_sum += (r1 - r2) * (r1 - r2);
                sqr_sum += (g1 - g2) * (g1 - g2);
                sqr_sum += (b1 - b2) * (b1 - b2);
                count += 3;
            }
        }
        return (float) Math.sqrt(sqr_sum / count);
    }

    static Bitmap blank(int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFFAABBCC);
        return bitmap;
    }

    static Bitmap docToBitmap(int w, int h, Context appContext, RemoteComposeDocument doc) {
        Bitmap bitmap = blank(w, h);
        RemoteComposeView remoteCanvas = new RemoteComposeView(appContext);
        remoteCanvas.layout(0, 0, w, h);
        remoteCanvas.setDocument(doc);
        remoteCanvas.draw(new Canvas(bitmap));
        return bitmap;
    }

    interface ModifyCanvas {
        void modify(RemoteComposeView canvas);
    }

    static Bitmap docToBitmap(
            int w,
            int h,
            Context appContext,
            RemoteComposeDocument doc,
            ModifyCanvas modifyCanvas) {
        Bitmap bitmap = blank(w, h);
        RemoteComposeView remoteCanvas = new RemoteComposeView(appContext);
        remoteCanvas.setDocument(doc);
        if (modifyCanvas != null) {
            modifyCanvas.modify(remoteCanvas);
        }
        remoteCanvas.draw(new Canvas(bitmap));
        return bitmap;
    }

    /**
     * Save both remote and local images
     *
     * @param name
     * @param remote
     * @param local
     * @param appContext
     */
    public static void saveBoth(String name, Bitmap remote, Bitmap local, Context appContext) {
        String methodName = getMethodName();
        String remoteImageName = name + "Remote.png";
        String localImageName = name + "Local.png";

        saveBitmap(appContext, remote, remoteImageName);
        saveBitmap(appContext, local, localImageName);
    }

    /**
     * Remove time
     *
     * @param text
     * @return
     */
    public static String removeTime(String text) {
        StringBuilder builder = new StringBuilder();
        for (String line : text.split("\n")) {
            if (!line.matches(".*loadFloat\\[1*[0-9]\\].*")) {
                builder.append(line);
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    /**
     * Show only lines that have that line
     *
     * @param input
     * @param match
     * @return
     */
    public static String grep(String input, String match) {
        StringBuilder str = new StringBuilder();
        String[] lines = input.split("\n");
        for (String line : lines) {
            if (line.matches(match)) {
                str.append(line);
                str.append("\n");
            }
        }
        return str.toString();
    }
}
