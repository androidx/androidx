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

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.wear.watchface.editor.ListenableEditorSession;
import androidx.wear.watchface.style.UserStyle;
import androidx.wear.watchface.style.UserStyleSetting;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/** Configuration activity for the watch face. */
public class ConfigActivity extends ComponentActivity {

    private static final String TAG = "ConfigActivity";

    private static final EnumMap<TimeStyle.Value, TimeStyle.Value> NEXT_VALUE_MAP =
            createNextValueMap();

    private Executor mMainExecutor;
    private TimeStyle mTimeStyle;

    private TextView mStyleValue;

    @Nullable
    private ListenableEditorSession mEditorSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_activity_layout);
        mMainExecutor = ContextCompat.getMainExecutor(getApplicationContext());
        mTimeStyle = new TimeStyle(this);

        mStyleValue = findViewById(R.id.style_value);

        findViewById(R.id.style_change).setOnClickListener((view) -> changeStyle());

        addCallback(
                ListenableEditorSession.listenableCreateOnWatchEditorSession(this, getIntent()),
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
    protected void onResume() {
        super.onResume();
        updateStyleValue();
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

    private void changeStyle() {
        Log.d(TAG, "changeStyle");
        if (mEditorSession == null) {
            return;
        }

        UserStyle userStyle = copyOfUserStyle(mEditorSession.getUserStyle());
        TimeStyle.Value value = mTimeStyle.get(userStyle);
        TimeStyle.Value newValue = NEXT_VALUE_MAP.get(value);
        mTimeStyle.set(userStyle, newValue);
        mEditorSession.setUserStyle(userStyle);
        updateStyleValue();
    }

    private void updateStyleValue() {
        if (mEditorSession == null) {
            return;
        }
        TimeStyle.Value value = mTimeStyle.get(mEditorSession.getUserStyle());
        mStyleValue.setText(mTimeStyle.getDisplayName(value));
    }

    private <T> void addCallback(ListenableFuture<T> future, FutureCallback<T> callback) {
        FutureCallback.addCallback(future, callback, mMainExecutor);
    }

    private static UserStyle copyOfUserStyle(UserStyle userStyle) {
        Map<UserStyleSetting, UserStyleSetting.Option> styleMap = new HashMap<>();
        styleMap.putAll(userStyle.getSelectedOptions());
        return new UserStyle(styleMap);
    }

    private static EnumMap<TimeStyle.Value, TimeStyle.Value> createNextValueMap() {
        EnumMap<TimeStyle.Value, TimeStyle.Value> map = new EnumMap<>(TimeStyle.Value.class);
        map.put(TimeStyle.Value.MINIMAL, TimeStyle.Value.SECONDS);
        map.put(TimeStyle.Value.SECONDS, TimeStyle.Value.MINIMAL);
        return map;
    }
}
