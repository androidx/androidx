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

package androidx.app.slice.render;

import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceProvider;
import androidx.app.slice.view.test.R;
import androidx.app.slice.widget.SliceLiveData;
import androidx.app.slice.widget.SliceView;

public class SliceRenderer {

    private static final String TAG = "SliceRenderer";
    public static final String SCREENSHOT_DIR = "slice-screenshots";
    private static File sScreenshotDirectory;

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
                int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 900,
                        mContext.getResources().getDisplayMetrics());
                int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200,
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
            File storage = mContext.getDataDir();
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
        File output = getScreenshotDirectory();
        if (!output.exists()) {
            output.mkdir();
        }
        mDoneLatch = new CountDownLatch(SliceCreator.URI_PATHS.length);
        for (String slice : SliceCreator.URI_PATHS) {
            doRender(slice, new File(output, String.format("%s.png", slice)));
        }
        Log.d(TAG, "Wrote render to " + output.getAbsolutePath());
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ViewGroup) mParent.getParent()).removeView(mParent);
            }
        });
        try {
            mDoneLatch.await();
        } catch (InterruptedException e) {
        }
    }

    private void doRender(final String slice, final File file) {
        Log.d(TAG, "Rendering " + slice + " to " + file.getAbsolutePath());

        final Slice s = mSliceCreator.onBindSlice(SliceCreator.getUri(slice, mContext));

        final CountDownLatch l = new CountDownLatch(1);
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSV1.setSlice(s);
                mSV2.setSlice(s);
                mSV3.setSlice(s);
                mSV1.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                            int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        mSV1.removeOnLayoutChangeListener(this);
                        mSV1.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "Drawing " + slice);
                                Bitmap b = Bitmap.createBitmap(mLayout.getMeasuredWidth(),
                                        mLayout.getMeasuredHeight(),
                                        Bitmap.Config.ARGB_8888);

                                mLayout.draw(new Canvas(b));
                                try {
                                    doCompress(slice, b, new FileOutputStream(file));
                                } catch (FileNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                                l.countDown();
                            }
                        }, 10);
                    }
                });
            }
        });
        try {
            l.await();
        } catch (InterruptedException e) {
        }
    }

    private void doCompress(final String slice, final Bitmap b, final FileOutputStream s) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Compressing " + slice);
                if (!b.compress(Bitmap.CompressFormat.PNG, 100, s)) {
                    throw new RuntimeException("Unable to compress");
                }

                b.recycle();
                Log.d(TAG, "Done " + slice);
                mDoneLatch.countDown();
            }
        });
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
