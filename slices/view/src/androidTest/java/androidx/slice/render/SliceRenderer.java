/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice.render;

import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.SliceUtils;
import androidx.slice.view.test.R;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.widget.SliceView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SliceRenderer {

    private static final String TAG = "SliceRenderer";
    public static final String SCREENSHOT_DIR = "slice-screenshots";

    private static final int MAX_CONCURRENT = 5;

    private static File sScreenshotDirectory;

    private final Object mRenderLock = new Object();

    private final Activity mContext;
    private final View mLayout;
    private final SliceView mSV1;
    private final SliceView mSV2;
    private final SliceView mSV3;
    private final ViewGroup mParent;
    private final Handler mHandler;
    private final SliceCreator mSliceCreator;
    private CountDownLatch mDoneLatch;

    public SliceRenderer(Activity context) {
        mContext = context;
        mParent = new ViewGroup(mContext) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1000,
                        mContext.getResources().getDisplayMetrics());
                int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 330,
                        mContext.getResources().getDisplayMetrics());
                mLayout.measure(makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                mLayout.layout(0, 0, width, height);
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                return false;
            }
        };
        mLayout = LayoutInflater.from(context).inflate(R.layout.render_layout, null);
        mSV1 = mLayout.findViewById(R.id.sv1);
        mSV1.setMode(SliceView.MODE_SHORTCUT);
        mSV2 = mLayout.findViewById(R.id.sv2);
        mSV2.setMode(SliceView.MODE_SMALL);
        mSV3 = mLayout.findViewById(R.id.sv3);
        mSV3.setMode(SliceView.MODE_LARGE);
        disableAnims(mLayout);
        mHandler = new Handler();
        ((ViewGroup) mContext.getWindow().getDecorView()).addView(mParent);
        mParent.addView(mLayout);
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mSliceCreator = new SliceCreator(mContext);
    }

    private void disableAnims(View view) {
        if (view instanceof RecyclerView) {
            ((RecyclerView) view).setItemAnimator(null);
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                disableAnims(viewGroup.getChildAt(i));
            }
        }
    }


    private File getScreenshotDirectory() {
        if (sScreenshotDirectory == null) {
            File storage = mContext.getFilesDir();
            sScreenshotDirectory = new File(storage, SCREENSHOT_DIR);
            if (!sScreenshotDirectory.exists()) {
                if (!sScreenshotDirectory.mkdirs()) {
                    throw new RuntimeException(
                            "Failed to create a screenshot directory.");
                }
            }
        }
        return sScreenshotDirectory;
    }


    private void doRender() {
        final File output = getScreenshotDirectory();
        if (!output.exists()) {
            output.mkdir();
        }
        mDoneLatch = new CountDownLatch(SliceCreator.URI_PATHS.length * 2 + 2);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (final String slice : SliceCreator.URI_PATHS) {
            final Slice s = mSliceCreator.onBindSlice(SliceCreator.getUri(slice, mContext));

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    doRender(slice, s, new File(output, String.format("%s.png", slice)),
                            true /* scrollable */);
                }
            });
            final Slice serialized = serAndUnSer(s);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    doRender(slice + "-ser", serialized, new File(output, String.format(
                            "%s-serialized.png", slice)), true /* scrollable */);
                }
            });
            if (slice.equals("wifi") || slice.equals("wifi2")) {
                // Test scrolling
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        doRender(slice + "-ns", s, new File(output, String.format(
                                "%s-no-scroll.png", slice)), false /* scrollable */);
                    }
                });
            }
        }
        try {
            mDoneLatch.await();
        } catch (InterruptedException e) {
        }
        Log.d(TAG, "Wrote render to " + output.getAbsolutePath());
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ViewGroup) mParent.getParent()).removeView(mParent);
            }
        });
    }

    private Slice serAndUnSer(Slice s) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            SliceUtils.serializeSlice(s, mContext, outputStream, "UTF-8",
                    new SliceUtils.SerializeOptions()
                            .setImageMode(SliceUtils.SerializeOptions.MODE_CONVERT)
                            .setActionMode(SliceUtils.SerializeOptions.MODE_CONVERT));

            byte[] bytes = outputStream.toByteArray();
            Log.d(TAG, "Serialized: " + new String(bytes));
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            return SliceUtils.parseSlice(mContext, inputStream, "UTF-8",
                    new SliceUtils.SliceActionListener() {
                        @Override
                        public void onSliceAction(Uri actionUri) { }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doRender(final String slice, final Slice s, final File file,
            final boolean scrollable) {
        Log.d(TAG, "Rendering " + slice + " to " + file.getAbsolutePath());

        try {
            final CountDownLatch l = new CountDownLatch(1);
            final Bitmap[] b = new Bitmap[1];
            synchronized (mRenderLock) {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSV1.setSlice(s);
                        mSV2.setSlice(s);
                        mSV3.setSlice(s);
                        mSV3.setScrollable(scrollable);
                        mSV1.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                            @Override
                            public void onLayoutChange(View v, int left, int top, int right,
                                    int bottom,
                                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                                mSV1.removeOnLayoutChangeListener(this);
                                mSV1.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "Drawing " + slice);
                                        b[0] = Bitmap.createBitmap(mLayout.getMeasuredWidth(),
                                                mLayout.getMeasuredHeight(),
                                                Bitmap.Config.ARGB_8888);

                                        mLayout.draw(new Canvas(b[0]));
                                        l.countDown();
                                    }
                                }, 10);
                            }
                        });
                    }
                });
                l.await();
            }
            doCompress(slice, b[0], new FileOutputStream(file));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doCompress(final String slice, final Bitmap b, final FileOutputStream s) {
        Log.d(TAG, "Compressing " + slice);
        if (!b.compress(Bitmap.CompressFormat.PNG, 100, s)) {
            throw new RuntimeException("Unable to compress");
        }

        b.recycle();
        mDoneLatch.countDown();
        Log.d(TAG, "Done " + slice);
    }

    public void renderAll(final Runnable runnable) {
        final ProgressDialog dialog = ProgressDialog.show(mContext, null, "Rendering...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                doRender();
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        runnable.run();
                    }
                });
            }
        }).start();
    }
}
