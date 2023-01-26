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

package androidx.wear.watchface.samples.minimal.instances;

import static androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting;
import static androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting.ListOption;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.wear.watchface.editor.ListenableEditorSession;
import androidx.wear.watchface.style.MutableUserStyle;
import androidx.wear.watchface.style.UserStyleSetting;
import androidx.wear.widget.CurvedTextView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/** Configuration activity for the watch face. */
public class ConfigActivity extends ComponentActivity {

    private static final String TAG = "ConfigActivity";

    private CurvedTextView mInstanceId;
    private TextView mStyleValue;
    private final UserStyleSetting.Id mTimeStyleId = new UserStyleSetting.Id("TimeStyle");

    @Nullable
    private ListenableEditorSession mEditorSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_activity_layout);
        listenForEditorSession();

        mInstanceId = findViewById(R.id.instance_id);
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

    private void listenForEditorSession() {
        ListenableFuture<ListenableEditorSession> editorSessionFuture =
                ListenableEditorSession.listenableCreateOnWatchEditorSession(this);
        editorSessionFuture.addListener(() -> {
            ListenableEditorSession editorSession;
            try {
                editorSession = editorSessionFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                return;
            }
            if (editorSession == null) {
                return;
            }
            mEditorSession = editorSession;
            updateInstanceId();
            updateStyleValue();
        }, ContextCompat.getMainExecutor(this));
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

    private void updateInstanceId() {
        if (mEditorSession == null) {
            return;
        }
        mInstanceId.setText(Objects.toString(mEditorSession.getWatchFaceId()));
    }

    private void updateStyleValue() {
        if (mEditorSession == null) {
            return;
        }
        ListOption option =
                (ListOption) mEditorSession.getUserStyle().getValue().get(mTimeStyleId);
        mStyleValue.setText(option.getDisplayName());
    }
}
