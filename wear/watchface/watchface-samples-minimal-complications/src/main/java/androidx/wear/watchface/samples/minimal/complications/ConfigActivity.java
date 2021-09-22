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
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.wear.watchface.complications.ComplicationDataSourceInfo;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.rendering.ComplicationDrawable;
import androidx.wear.watchface.editor.ListenableEditorSession;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/** Configuration activity for the watch face. */
public class ConfigActivity extends ComponentActivity {

    private static final String TAG = "ConfigActivity";

    private Executor mMainExecutor = new Executor() {
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable runnable) {
            mHandler.post(runnable);
        }
    };

    private TextView mComplicationProviderName;
    private ImageView mComplicationPreview;
    private ComplicationDrawable mComplicationPreviewDrawable;

    @Nullable
    private ListenableEditorSession mEditorSession;

    public ConfigActivity() {
        addCallback(
                ListenableEditorSession.listenableCreateOnWatchEditorSession(this),
                new BaseFutureCallback<ListenableEditorSession>(
                        this, TAG, "listenableCreateOnWatchEditingSession") {
                    @Override
                    public void onSuccess(ListenableEditorSession editorSession) {
                        super.onSuccess(editorSession);
                        setEditorSession(editorSession);
                    }
                });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_activity_layout);

        mComplicationProviderName = findViewById(R.id.complication_provider_name);
        mComplicationPreview = findViewById(R.id.complication_preview);
        mComplicationPreviewDrawable = new ComplicationDrawable(this);

        findViewById(R.id.complication_change).setOnClickListener((view) -> changeComplication());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mComplicationPreview.setOnClickListener((view) -> changeComplication());
        } else {
            mComplicationPreview.setVisibility(View.GONE);
        }
    }

    private void setEditorSession(ListenableEditorSession editorSession) {
        ConfigActivity.this.mEditorSession = editorSession;
        FlowLiveDataConversions.asLiveData(mEditorSession.getComplicationsDataSourceInfo()).observe(
                this,
                complicationDataSourceInfoMap -> {
                    if (!complicationDataSourceInfoMap.isEmpty()) {
                        ComplicationDataSourceInfo info =
                                complicationDataSourceInfoMap.get(WatchFaceService.COMPLICATION_ID);
                        if (info == null) {
                            mComplicationProviderName.setText(
                                    getString(R.string.complication_none));
                        } else {
                            mComplicationProviderName.setText(info.getName());
                        }
                    }
                }
        );

        FlowLiveDataConversions.asLiveData(mEditorSession.getComplicationsPreviewData()).observe(
                this,
                complicationsPreviewData -> {
                    if (complicationsPreviewData.isEmpty()) {
                        mComplicationPreview.setImageResource(R.drawable.preview_loading);
                    } else {
                        ComplicationData preview =
                                complicationsPreviewData.get(WatchFaceService.COMPLICATION_ID);
                        if (preview != null) {
                            mComplicationPreview.setImageDrawable(mComplicationPreviewDrawable);
                            mComplicationPreviewDrawable.setComplicationData(preview, true);
                        } else {
                            mComplicationPreview.setImageResource(R.drawable.preview_unavailable);
                        }
                    }
                }

        );
    }

    private void changeComplication() {
        Log.d(TAG, "changeComplication");
        if (mEditorSession == null) {
            return;
        }
        mEditorSession
                .listenableOpenComplicationDataSourceChooser(WatchFaceService.COMPLICATION_ID)
                .addListener(() -> { /* Empty on purpose. */ }, mMainExecutor);
    }

    @Override
    protected void onDestroy() {
        finish();
        super.onDestroy();
    }

    private <T> void addCallback(ListenableFuture<T> future, FutureCallback<T> callback) {
        FutureCallback.addCallback(future, callback, mMainExecutor);
    }
}
