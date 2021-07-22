/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.samples.minimal.complications;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.wear.complications.ComplicationDataSourceInfo;
import androidx.wear.complications.data.ComplicationData;
import androidx.wear.watchface.complications.rendering.ComplicationDrawable;
import androidx.wear.watchface.editor.ListenableEditorSession;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.Executor;

/** Configuration activity for the watch face. */
public class ConfigActivity extends ComponentActivity {

    private static final String TAG = "ConfigActivity";

    private Executor mMainExecutor;

    private TextView mComplicationProviderName;
    private ImageView mComplicationPreview;
    private ComplicationDrawable mComplicationPreviewDrawable;

    @Nullable private ListenableEditorSession mEditorSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_activity_layout);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mMainExecutor = MainExecutorApi28.getMainExecutor(this);
        } else {
            mMainExecutor = MainExecutorApi25.getMainExecutor();
        }

        mComplicationProviderName = findViewById(R.id.complication_provider_name);
        mComplicationPreview = findViewById(R.id.complication_preview);
        mComplicationPreviewDrawable = new ComplicationDrawable(this);

        findViewById(R.id.complication_change).setOnClickListener((view) -> changeComplication());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mComplicationPreview.setOnClickListener((view) -> changeComplication());
        } else {
            mComplicationPreview.setVisibility(View.GONE);
        }

        addCallback(
                ListenableEditorSession.listenableCreateOnWatchEditingSession(this, getIntent()),
                new BaseFutureCallback<ListenableEditorSession>(
                        this, TAG, "listenableCreateOnWatchEditingSession") {
                    @Override
                    public void onSuccess(ListenableEditorSession editorSession) {
                        super.onSuccess(editorSession);
                        ConfigActivity.this.mEditorSession = editorSession;
                        updateComplicationSlotStatus();
                    }
                });
    }

    private void changeComplication() {
        Log.d(TAG, "changeComplication");
        if (mEditorSession == null) {
            return;
        }
        mEditorSession
                .listenableOpenComplicationDataSourceChooser(WatchFaceService.COMPLICATION_ID)
                .addListener(this::updateComplicationSlotStatus, mMainExecutor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateComplicationSlotStatus();
    }

    @Override
    protected void onDestroy() {
        if (mEditorSession != null) {
            mEditorSession.setCommitChangesOnClose(true);
            mEditorSession.close();
        }
        finish();
        super.onDestroy();
    }

    private void updateComplicationSlotStatus() {
        if (mEditorSession == null) {
            return;
        }
        updateComplicationDataSourceName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            updateComplicationSlotPreview();
        }
    }

    private void updateComplicationDataSourceName() {
        if (mEditorSession == null) {
            return;
        }
        addCallback(
                mEditorSession.getListenableComplicationsProviderInfo(),
                new BaseFutureCallback<Map<Integer, ComplicationDataSourceInfo>>(
                        this, TAG, "getListenableComplicationsProviderInfo") {
                    @Override
                    public void onSuccess(Map<Integer, ComplicationDataSourceInfo> value) {
                        super.onSuccess(value);
                        ComplicationDataSourceInfo info =
                                value.get(WatchFaceService.COMPLICATION_ID);
                        if (info == null) {
                            mComplicationProviderName.setText(
                                    getString(R.string.complication_none));
                        } else {
                            mComplicationProviderName.setText(info.getName());
                        }
                    }
                });
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private void updateComplicationSlotPreview() {
        if (mEditorSession == null) {
            return;
        }
        addCallback(
                mEditorSession.getListenableComplicationPreviewData(),
                new BaseFutureCallback<Map<Integer, ComplicationData>>(
                        this, TAG, "getListenableComplicationPreviewData") {
                    @Override
                    public void onSuccess(Map<Integer, ComplicationData> value) {
                        super.onSuccess(value);
                        ComplicationData preview = value.get(WatchFaceService.COMPLICATION_ID);
                        if (preview != null) {
                            mComplicationPreview.setImageDrawable(mComplicationPreviewDrawable);
                            mComplicationPreviewDrawable.setComplicationData(preview, true);
                        } else {
                            mComplicationPreview.setImageResource(R.drawable.preview_unavailable);
                        }
                    }

                    @Override
                    public void onPending() {
                        super.onPending();
                        mComplicationPreview.setImageResource(R.drawable.preview_loading);
                    }
                });
    }

    private <T> void addCallback(ListenableFuture<T> future, FutureCallback<T> callback) {
        FutureCallback.addCallback(future, callback, mMainExecutor);
    }

    private static final class MainExecutorApi25 {
        public static Handler sMainThreadHandler = new Handler(Looper.getMainLooper());

        public static Executor getMainExecutor() {
            return sMainThreadHandler::post;
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private static final class MainExecutorApi28 {
        public static Executor getMainExecutor(Context context) {
            return context.getMainExecutor();
        }
    }
}
