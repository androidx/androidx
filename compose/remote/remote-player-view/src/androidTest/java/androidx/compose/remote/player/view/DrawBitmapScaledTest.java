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

import static androidx.compose.remote.player.view.TestSerializeUtils.loadFileFromRaw;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.Log;

import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.compose.remote.player.view.test.R;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DrawBitmapScaledTest {
    private final boolean mSaveImages = false;
    int mTw = 600;
    int mTh = 600;
    int mIw = 30;
    int mIh = 60;

    interface Callback extends TestUtils.Callback {}

    @Ignore("Temporarily disabled due to ongoing refactoring")
    @Test
    public void drawScaledBitmapParameterTest() {
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";
        Bitmap smallTestImage = TestUtils.createImage(30, 30, false);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(mTw, mTh);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setFilterBitmap(false).commit();
                    rdoc.drawScaledBitmap(
                            smallTestImage, 0, 0, mTw, mTh, 0, 0, mTw, mTh, 3, 0, "small image");
                    rdoc.drawScaledBitmap(
                            smallTestImage, 1, 2, 300, 400, 5, 6, 700, 800, 2, 1.2f, "small image");
                };
        String result = TestUtils.drawCommandList(cb, ".*DrawBitmapScaled.*");
        Log.v("MAIN", result);
        result = TestUtils.grep(result, ".*DrawBitmapScaled.*");
        String expected =
                " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType="
                        + " SCALE_FILL_HEIGHT  scaleFactor=   0.0  srcLeft=   0.0  srcTop=   0.0 "
                        + " srcRight=   600.0  srcBottom=   600.0  dstLeft=   0.0  dstTop=   0.0 "
                        + " dstRight=   600.0  dstBottom=   600.0 \n"
                        + " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType="
                        + " SCALE_FILL_WIDTH  scaleFactor=   1.2  srcLeft=   1.0  srcTop=   2.0 "
                        + " srcRight=   300.0  srcBottom=   400.0  dstLeft=   5.0  dstTop=   6.0 "
                        + " dstRight=   700.0  dstBottom=   800.0 \n";

        if (TestUtils.diff(expected, result)) {
            Log.v("EXPECT", expected);
            Log.v("RESULT", result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        rc_player.setDocument(doc);
        Bitmap remoteBitmap = TestUtils.docToBitmap(mTw, mTh, appContext, doc);
        assertNotNull(remoteBitmap);
    }

    private void scaledBitmapTestTool(
            int scaleType,
            String scaleTypeStr,
            Bitmap localBitmap,
            float scaleFactor,
            String expected) {
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";
        Bitmap smallTestImage = createSImage(mIw, mIh, false);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setFilterBitmap(false).setFilterBitmap(true).commit();
                    rdoc.drawScaledBitmap(
                            smallTestImage,
                            0,
                            0,
                            mIw,
                            mIh,
                            0,
                            0,
                            mTw,
                            mTh,
                            scaleType,
                            scaleFactor,
                            "small image");
                };
        String result = TestUtils.drawCommandList(cb, ".*DrawBitmapScaled.*");
        expected = cleanText(expected);
        result = cleanText(result);
        if (TestUtils.diff(expected, result)) {
            Log.v("EXPECT", expected);
            Log.v("RESULT", result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(mTw, mTh, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(mTw, mTh);
        TestUtils.captureGold("blend_" + scaleTypeStr, doc, appContext);

        if (!mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = TestUtils.getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            TestUtils.saveBitmap(appContext, remoteBitmap, remoteImageName);
            TestUtils.saveBitmap(appContext, localBitmap, localImageName);
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    private static String cleanText(String text) {

        text = text.replaceAll(" +", " ").trim();
        text = text.replaceAll("= ", "=");

        return text;
    }

    private void scaledBitmapIdTestTool(
            int scaleType,
            String scaleTypeStr,
            Bitmap localBitmap,
            float scaleFactor,
            String expected) {
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";
        Bitmap smallTestImage = createSImage(mIw, mIh, false);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setFilterBitmap(false).setFilterBitmap(true).commit();
                    int id = rdoc.addBitmap(smallTestImage);
                    rdoc.nameBitmapId(id, "Omicron");
                    rdoc.drawScaledBitmap(
                            id,
                            0,
                            0,
                            mIw,
                            mIh,
                            0,
                            0,
                            mTw,
                            mTh,
                            scaleType,
                            scaleFactor,
                            "small image");
                };
        String cmdList = TestUtils.drawCommandList(cb, ".*BitmapData.*");
        System.out.println(cmdList);
        assertEquals(
                " BitmapData  imageId= 43  imageWidth= 30  imageHeight= 60  imageType="
                        + " TYPE_PNG_8888  encoding= ENCODING_INLINE \n",
                cmdList);
        cmdList = TestUtils.drawCommandList(cb, ".*NamedVariable.*");

        assertEquals(
                " NamedVariable  varId= 43  varName= Omicron  varType= IMAGE_TYPE \n", cmdList);

        String result = TestUtils.drawCommandList(cb, ".*DrawBitmapScaled.*");

        if (TestUtils.diff(expected, result)) {
            Log.v("TEST", result);
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(mTw, mTh, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(mTw, mTh);
        TestUtils.captureGold("blend_" + scaleTypeStr, doc, appContext);

        if (!mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = TestUtils.getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            TestUtils.saveBitmap(appContext, remoteBitmap, remoteImageName);
            TestUtils.saveBitmap(appContext, localBitmap, localImageName);
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Ignore("Temporarily disabled due to ongoing refactoring")
    @Test
    public void testBitMapScaled0() {
        Bitmap smallTestImage = createSImage(mIw, mIh, false);
        Bitmap localBitmap = TestUtils.blank(mTw, mTh);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint();
        int ox = (mTw - mIw) / 2;
        int oy = (mTh - mIh) / 2;
        String expected =
                " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType= SCALE_NONE "
                        + " scaleFactor=   0.0  srcLeft=   0.0  srcTop=   0.0  srcRight=   30.0 "
                        + " srcBottom=   60.0  dstLeft=   0.0  dstTop=   0.0  dstRight=   600.0 "
                        + " dstBottom=   600.0 \n";
        canvas.drawBitmap(smallTestImage, null, new Rect(ox, oy, ox + mIw, oy + mIh), paint);
        scaledBitmapTestTool(0, "SCALE_NONE", localBitmap, 0, expected);
    }

    @Ignore("Temporarily disabled due to ongoing refactoring")
    @Test
    public void testBitMapScaled1() {
        Bitmap smallTestImage = createSImage(mIw, mIh, false);
        Bitmap localBitmap = TestUtils.blank(mTw, mTh);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint();
        int ox = (mTw - mIw) / 2;
        int oy = (mTh - mIh) / 2;
        String expected =
                " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType= SCALE_INSIDE "
                        + " scaleFactor=   0.0  srcLeft=   0.0  srcTop=   0.0  srcRight=   30.0 "
                        + " srcBottom=   60.0  dstLeft=   0.0  dstTop=   0.0  dstRight=   600.0 "
                        + " dstBottom=   600.0 \n";
        canvas.drawBitmap(smallTestImage, null, new Rect(ox, oy, ox + mIw, oy + mIh), paint);
        scaledBitmapTestTool(1, "SCALE_INSIDE", localBitmap, 0, expected);
        scaledBitmapIdTestTool(1, "SCALE_INSIDE", localBitmap, 0, expected);
    }

    @Ignore("Temporarily disabled due to ongoing refactoring")
    @Test
    public void testBitMapScaled2() {
        Bitmap smallTestImage = createSImage(mIw, mIh, false);
        Bitmap localBitmap = TestUtils.blank(mTw, mTh);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint();
        int sL = 0;
        int sT = 0;
        int sR = mIw;
        int sB = mIh;
        int dL = 0;
        int dT = 0;
        int dR = mTw;
        int dB = mTh;
        Scale s = new Scale(sL, sT, sR, sB, dL, dT, dR, dB, 2, 1);

        String expected =
                " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType="
                        + " SCALE_FILL_WIDTH  scaleFactor=   0.0  srcLeft=   0.0  srcTop=   0.0 "
                        + " srcRight=   30.0  srcBottom=   60.0  dstLeft=   0.0  dstTop=   0.0 "
                        + " dstRight=   600.0  dstBottom=   600.0 \n";
        canvas.drawBitmap(
                smallTestImage,
                new Rect(0, 0, mIw, mIh),
                new Rect(
                        (int) s.mFinalDstLeft,
                        (int) s.mFinalDstTop,
                        (int) s.mFinalDstRight,
                        (int) s.mFinalDstBottom),
                paint);
        scaledBitmapTestTool(2, "SCALE_FILL_WIDTH", localBitmap, 0, expected);
        scaledBitmapIdTestTool(2, "SCALE_FILL_WIDTH", localBitmap, 0, expected);
    }

    @Ignore("Temporarily disabled due to ongoing refactoring")
    @Test
    public void testBitMapScaled3() {
        Bitmap smallTestImage = createSImage(mIw, mIh, false);
        Bitmap localBitmap = TestUtils.blank(mTw, mTh);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint();
        int type = 3;
        int sL = 0;
        int sT = 0;
        int sR = mIw;
        int sB = mIh;
        int dL = 0;
        int dT = 0;
        int dR = mTw;
        int dB = mTh;
        Scale s = new Scale(sL, sT, sR, sB, dL, dT, dR, dB, type, 1);

        String expected =
                " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType="
                        + " SCALE_FILL_HEIGHT  scaleFactor=   0.0  srcLeft=   0.0  srcTop=   0.0 "
                        + " srcRight=   30.0  srcBottom=   60.0  dstLeft=   0.0  dstTop=   0.0 "
                        + " dstRight=   600.0  dstBottom=   600.0 \n";

        canvas.drawBitmap(
                smallTestImage,
                new Rect(0, 0, mIw, mIh),
                new Rect(
                        (int) s.mFinalDstLeft,
                        (int) s.mFinalDstTop,
                        (int) s.mFinalDstRight,
                        (int) s.mFinalDstBottom),
                paint);
        scaledBitmapTestTool(type, "SCALE_FILL_HEIGHT", localBitmap, 0, expected);
    }

    @Ignore("Temporarily disabled due to ongoing refactoring")
    @Test
    public void testBitMapScaled4() {
        Bitmap smallTestImage = createSImage(mIw, mIh, false);
        Bitmap localBitmap = TestUtils.blank(mTw, mTh);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint();
        int type = 4;
        int sL = 0;
        int sT = 0;
        int sR = mIw;
        int sB = mIh;
        int dL = 0;
        int dT = 0;
        int dR = mTw;
        int dB = mTh;
        Scale s = new Scale(sL, sT, sR, sB, dL, dT, dR, dB, type, 1);

        String expected =
                " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType= SCALE_FIT "
                        + " scaleFactor=   0.0  srcLeft=   0.0  srcTop=   0.0  srcRight=   30.0 "
                        + " srcBottom=   60.0  dstLeft=   0.0  dstTop=   0.0  dstRight=   600.0 "
                        + " dstBottom=   600.0 \n";

        canvas.drawBitmap(
                smallTestImage,
                new Rect(0, 0, mIw, mIh),
                new Rect(
                        (int) s.mFinalDstLeft,
                        (int) s.mFinalDstTop,
                        (int) s.mFinalDstRight,
                        (int) s.mFinalDstBottom),
                paint);
        scaledBitmapTestTool(type, "SCALE_FIT", localBitmap, 0, expected);
    }

    @Ignore("Temporarily disabled due to ongoing refactoring")
    @Test
    public void testBitMapScaled5() {
        Bitmap smallTestImage = createSImage(mIw, mIh, false);
        Bitmap localBitmap = TestUtils.blank(mTw, mTh);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint();
        int type = 5;
        int sL = 0;
        int sT = 0;
        int sR = mIw;
        int sB = mIh;
        int dL = 0;
        int dT = 0;
        int dR = mTw;
        int dB = mTh;
        Scale s = new Scale(sL, sT, sR, sB, dL, dT, dR, dB, type, 1);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_bitmap_scaled5);

        String expected2 =
                " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType= SCALE_CROP "
                        + " scaleFactor=   0.0  srcLeft=   0.0  srcTop=   0.0  srcRight=   30.0 "
                        + " srcBottom=   60.0  dstLeft=   0.0  dstTop=   0.0  dstRight=   600.0 "
                        + " dstBottom=   600.0 \n";

        canvas.drawBitmap(
                smallTestImage,
                new Rect(0, 0, mIw, mIh),
                new Rect(
                        (int) s.mFinalDstLeft,
                        (int) s.mFinalDstTop,
                        (int) s.mFinalDstRight,
                        (int) s.mFinalDstBottom),
                paint);
        scaledBitmapTestTool(type, "SCALE_CROP", localBitmap, 0, expected);
    }

    @Ignore("Temporarily disabled due to ongoing refactoring")
    @Test
    public void testBitMapScaled6() {
        Bitmap smallTestImage = createSImage(mIw, mIh, false);
        Bitmap localBitmap = TestUtils.blank(mTw, mTh);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint();
        int type = 6;
        int sL = 0;
        int sT = 0;
        int sR = mIw;
        int sB = mIh;
        int dL = 0;
        int dT = 0;
        int dR = mTw;
        int dB = mTh;
        Scale s = new Scale(sL, sT, sR, sB, dL, dT, dR, dB, type, 1);

        String expected =
                " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType="
                        + " SCALE_FILL_BOUNDS  scaleFactor=   0.0  srcLeft=   0.0  srcTop=   0.0 "
                        + " srcRight=   30.0  srcBottom=   60.0  dstLeft=   0.0  dstTop=   0.0 "
                        + " dstRight=   600.0  dstBottom=   600.0 \n";

        canvas.drawBitmap(
                smallTestImage,
                new Rect(0, 0, mIw, mIh),
                new Rect(
                        (int) s.mFinalDstLeft,
                        (int) s.mFinalDstTop,
                        (int) s.mFinalDstRight,
                        (int) s.mFinalDstBottom),
                paint);
        scaledBitmapTestTool(type, "SCALE_FILL_BOUNDS", localBitmap, 0, expected);
    }

    @Ignore("Temporarily disabled due to ongoing refactoring")
    @Test
    public void testBitMapScaled7() {
        Bitmap smallTestImage = createSImage(mIw, mIh, false);
        Bitmap localBitmap = TestUtils.blank(mTw, mTh);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint();
        int type = 7;
        int sL = 0;
        int sT = 0;
        int sR = mIw;
        int sB = mIh;
        int dL = 0;
        int dT = 0;
        int dR = mTw;
        int dB = mTh;
        Scale s = new Scale(sL, sT, sR, sB, dL, dT, dR, dB, type, 1);

        String expected =
                " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType="
                        + " SCALE_FIXED_SCALE  scaleFactor=   1.0  srcLeft=   0.0  srcTop=   0.0 "
                        + " srcRight=   30.0  srcBottom=   60.0  dstLeft=   0.0  dstTop=   0.0 "
                        + " dstRight=   600.0  dstBottom=   600.0 \n";

        canvas.drawBitmap(
                smallTestImage,
                new Rect(0, 0, mIw, mIh),
                new Rect(
                        (int) s.mFinalDstLeft,
                        (int) s.mFinalDstTop,
                        (int) s.mFinalDstRight,
                        (int) s.mFinalDstBottom),
                paint);
        scaledBitmapTestTool(type, "SCALE_FIXED_SCALE", localBitmap, 1, expected);
    }

    @Ignore("Temporarily disabled due to ongoing refactoring")
    @Test
    public void testBitMapScaled7a() {
        Bitmap smallTestImage = createSImage(mIw, mIh, false);
        Bitmap localBitmap = TestUtils.blank(mTw, mTh);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint();
        int type = 7;
        int sL = 0;
        int sT = 0;
        int sR = mIw;
        int sB = mIh;
        int dL = 0;
        int dT = 0;
        int dR = mTw;
        int dB = mTh;
        Scale s = new Scale(sL, sT, sR, sB, dL, dT, dR, dB, type, 2);
        String stype = "SCALE_FIXED_SCALE";
        String expected =
                " DrawBitmapScaled  imageId= 43  contentDescriptionId= 44  scaleType= "
                        + stype
                        + "  scaleFactor=   2.0  srcLeft=   0.0  srcTop=   0.0  srcRight= "
                        + "  30.0  srcBottom=   60.0  dstLeft=   0.0  dstTop=   0.0  dstRight=  "
                        + " 600.0  dstBottom=   600.0 \n";
        canvas.drawBitmap(
                smallTestImage,
                new Rect(0, 0, mIw, mIh),
                new Rect(
                        (int) s.mFinalDstLeft,
                        (int) s.mFinalDstTop,
                        (int) s.mFinalDstRight,
                        (int) s.mFinalDstBottom),
                paint);
        scaledBitmapTestTool(type, "SCALE_FIXED_SCALE", localBitmap, 2, expected);
    }

    /**
     * Creates a test image with a complex pattern
     *
     * @param tw width of image
     * @param th height of the image
     * @param darkTheme draw in a dark theme
     * @return Bitmap
     */
    static Bitmap createSImage(int tw, int th, boolean darkTheme) {
        Bitmap image = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();

        Canvas canvas = new Canvas(image);
        paint.setColor(Color.CYAN);
        canvas.drawRect(0, 0, tw, th, paint);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.BLACK);

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

    public static final int SCALE_NONE = 0;
    public static final int SCALE_INSIDE = 1;
    public static final int SCALE_FILL_WIDTH = 2;
    public static final int SCALE_FILL_HEIGHT = 3;
    public static final int SCALE_FIT = 4;
    public static final int SCALE_CROP = 5;
    public static final int SCALE_FILL_BOUNDS = 6;
    public static final int SCALE_FIXED_SCALE = 7;

    /** Operation to draw a given cached bitmap */
    public class Scale {
        private final boolean mDebug = false;
        float mSrcLeft;
        float mSrcTop;
        float mSrcRight;
        float mSrcBottom;
        float mDstLeft;
        float mDstTop;
        float mDstRight;
        float mDstBottom;
        int mContentDescId;
        float mScaleFactor;
        int mScaleType;

        float mFinalDstLeft;
        float mFinalDstTop;
        float mFinalDstRight;
        float mFinalDstBottom;

        public Scale() {}

        public Scale(
                float srcLeft,
                float srcTop,
                float srcRight,
                float srcBottom,
                float dstLeft,
                float dstTop,
                float dstRight,
                float dstBottom,
                int type,
                float scale) {

            mSrcLeft = srcLeft;
            mSrcTop = srcTop;
            mSrcRight = srcRight;
            mSrcBottom = srcBottom;
            mDstLeft = dstLeft;
            mDstTop = dstTop;
            mDstRight = dstRight;
            mDstBottom = dstBottom;
            mScaleType = type;
            mScaleFactor = scale;
            adjustDrawToType();
        }

        void setup(
                float srcLeft,
                float srcTop,
                float srcRight,
                float srcBottom,
                float dstLeft,
                float dstTop,
                float dstRight,
                float dstBottom,
                int type,
                float scale) {

            mSrcLeft = srcLeft;
            mSrcTop = srcTop;
            mSrcRight = srcRight;
            mSrcBottom = srcBottom;
            mDstLeft = dstLeft;
            mDstTop = dstTop;
            mDstRight = dstRight;
            mDstBottom = dstBottom;
            mScaleType = type;
            mScaleFactor = scale;
            adjustDrawToType();
        }

        String str(float v) {
            String s = "  " + (int) v;
            return s.substring(s.length() - 3);
        }

        void print(String str, float left, float top, float right, float bottom) {
            String s = str;
            s += str(left) + ", " + str(top) + ", " + str(right) + ", " + str(bottom) + ", ";
            s += " [" + str(right - left) + " x " + str(bottom - top) + "]";
            System.out.println(s);
        }

        /** This adjust destination on the DrawBitMapInt to support all contentScale types */
        private void adjustDrawToType() {
            int sw = (int) (mSrcRight - mSrcLeft);
            int sh = (int) (mSrcBottom - mSrcTop);
            float width = mDstRight - mDstLeft;
            float height = mDstBottom - mDstTop;
            int dw = (int) width;
            int dh = (int) height;
            int dLeft = 0;
            int dRight = dw;
            int dTop = 0;
            int dBottom = dh;
            if (mDebug) {
                print("test rc ", mSrcLeft, mSrcTop, mSrcRight, mSrcBottom);
                print("test dst ", mDstLeft, mDstTop, mDstRight, mDstBottom);
            }

            switch (mScaleType) {
                case SCALE_NONE:
                    dh = sh;
                    dw = sw;
                    dTop = ((int) height - dh) / 2;
                    dBottom = dh + dTop;
                    dLeft = ((int) width - dw) / 2;
                    dRight = dw + dLeft;
                    break;
                case SCALE_INSIDE:
                    if (dh > sh && dw > sw) {
                        dh = sh;
                        dw = sw;
                    } else if (sw * height > width * sh) { // width dominated
                        dh = (dw * sh) / sw;
                    } else {
                        dw = (dh * sw) / sh;
                    }
                    dTop = ((int) height - dh) / 2;
                    dBottom = dh + dTop;
                    dLeft = ((int) width - dw) / 2;
                    dRight = dw + dLeft;
                    break;
                case SCALE_FILL_WIDTH:
                    dh = (dw * sh) / sw;

                    dTop = ((int) height - dh) / 2;
                    dBottom = dh + dTop;
                    dLeft = ((int) width - dw) / 2;
                    dRight = dw + dLeft;
                    break;
                case SCALE_FILL_HEIGHT:
                    dw = (dh * sw) / sh;

                    dTop = ((int) height - dh) / 2;
                    dBottom = dh + dTop;
                    dLeft = ((int) width - dw) / 2;
                    dRight = dw + dLeft;
                    break;
                case SCALE_FIT:
                    if (sw * height > width * sh) { // width dominated
                        dh = (dw * sh) / sw;
                        dTop = ((int) height - dh) / 2;
                        dBottom = dh + dTop;
                    } else {
                        dw = (dh * sw) / sh;
                        dLeft = ((int) width - dw) / 2;
                        dRight = dw + dLeft;
                    }
                    break;
                case SCALE_CROP:
                    if (sw * height < width * sh) { // width dominated
                        dh = (dw * sh) / sw;
                        dTop = ((int) height - dh) / 2;
                        dBottom = dh + dTop;
                    } else {
                        dw = (dh * sw) / sh;
                        dLeft = ((int) width - dw) / 2;
                        dRight = dw + dLeft;
                    }
                    break;
                case SCALE_FILL_BOUNDS:
                    // do nothing
                    break;
                case SCALE_FIXED_SCALE:
                    dh = (int) (sh * mScaleFactor);
                    dw = (int) (sw * mScaleFactor);
                    dTop = ((int) height - dh) / 2;
                    dBottom = dh + dTop;
                    dLeft = ((int) width - dw) / 2;
                    dRight = dw + dLeft;
                    break;
            }

            mFinalDstRight = dRight + mDstLeft;
            mFinalDstLeft = dLeft + mDstLeft;
            mFinalDstBottom = dBottom + mDstTop;
            mFinalDstTop = dTop + mDstTop;

            if (mDebug) {
                print("test  out ", mFinalDstLeft, mFinalDstTop, mFinalDstRight, mFinalDstBottom);
            }
        }

        private String typeToString(int type) {
            String[] typeString = {
                "none",
                "inside",
                "fill_width",
                "fill_height",
                "fit",
                "crop",
                "fill_bounds",
                "fixed_scale"
            };
            return typeString[type];
        }

        //            (int) mSrcLeft, (int) mSrcTop,
        //                    (int) mSrcRight, (int) mSrcBottom,
        //                    (int) mFinalDstLeft, (int) mFinalDstTop,
        //                    (int) mFinalDstRight, (int) mFinalDstBottom, mContentDescId);

    }
}
