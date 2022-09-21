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

package androidx.wear.watchface.samples.minimal.style;

import static androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting;
import static androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting.ListOption;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.wear.watchface.editor.ListenableEditorSession;
import androidx.wear.watchface.style.MutableUserStyle;
import androidx.wear.watchface.style.UserStyleSetting;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/** Configuration activity for the watch face. */
public class ConfigActivity extends ComponentActivity {

    private static final String TAG = "ConfigActivity";

    private final Executor mMainExecutor = new Executor() {
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable runnable) {
            mHandler.post(runnable);
        }
    };

    private TextView mStyleValue;
    private final UserStyleSetting.Id mTimeStyleId = new UserStyleSetting.Id("TimeStyle");

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
                        mEditorSession = editorSession;
                        updateStyleValue();
                    }
                });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_activity_layout);

        mStyleValue = findViewById(R.id.style_value);

        findViewById(R.id.style_change).setOnClickListener((view) -> changeStyle());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStyleValue();
    }

    @Override
    protected void onDestroy() {
        finish();
        super.onDestroy();
    }

    private void changeStyle() {
        Log.d(TAG, "changeStyle");
        if (mEditorSession == null) {
            return;
        }

        MutableUserStyle userStyle = mEditorSession.getUserStyle().getValue().toMutableUserStyle();
        ListOption currentOption = (ListOption) userStyle.get(mTimeStyleId);
        ListUserStyleSetting listUserStyleSetting =
                (ListUserStyleSetting) mEditorSession.getUserStyleSchema()
                        .getRootUserStyleSettings()
                        .get(0);

        // Choose the first option in the list of options that isn't currentOption. We only expect
        // two options here, so this will flip flop between them.
        for (UserStyleSetting.Option option : listUserStyleSetting.getOptions()) {
            if (!option.getId().equals(currentOption.getId())) {
                userStyle.set(mTimeStyleId, option);
                break;
            }
        }
        mEditorSession.getUserStyle().setValue(userStyle.toUserStyle());
        updateStyleValue();
    }

    private void updateStyleValue() {
        if (mEditorSession == null) {
            return;
        }
        ListOption option =
                (ListOption) mEditorSession.getUserStyle().getValue().get(mTimeStyleId);
        mStyleValue.setText(option.getDisplayName());
    }

    private <T> void addCallback(ListenableFuture<T> future, FutureCallback<T> callback) {
        FutureCallback.addCallback(future, callback, mMainExecutor);
    }
}
