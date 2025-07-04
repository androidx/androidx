/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.demo.layout;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.window.WindowSdkExtensions;
import androidx.window.demo.common.EdgeToEdgeActivity;
import androidx.window.demo.databinding.ActivityLayoutWindowInfoJavaApiDemoLayoutBinding;
import androidx.window.java.layout.WindowInfoTrackerCallbackAdapter;
import androidx.window.layout.WindowInfoTracker;
import androidx.window.layout.WindowLayoutInfo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Activity to show how to get WindowLayoutInfo with the Java-supported API. */
public class WindowInfoJavaApiDemoActivity extends EdgeToEdgeActivity {
    // Logcat tag length must be <= 23 characters.
    private static final String TAG = "WinInfoJavaApiDemo";

    private @NonNull WindowInfoTrackerCallbackAdapter mWindowInfoTrackerCallbackAdapter;
    private @NonNull TextView mWindowLayoutInfoGetterValue;
    private @NonNull TextView mWindowLayoutInfoFlowValue;

    /** The Window Manager extension version available on this device. */
    private final int mExtensionVersion = WindowSdkExtensions.getInstance().getExtensionVersion();

    /** WindowLayoutInfoListener callback. */
    private final Consumer<WindowLayoutInfo> mWindowLayoutInfoChangedConsumer =
            this::onWindowLayoutInfoUpdated;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityLayoutWindowInfoJavaApiDemoLayoutBinding viewBinding =
                ActivityLayoutWindowInfoJavaApiDemoLayoutBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        mWindowLayoutInfoGetterValue = viewBinding.windowLayoutInfoGetterValue;
        mWindowLayoutInfoFlowValue = viewBinding.windowLayoutInfoFlowValue;

        final WindowInfoTracker tracker = WindowInfoTracker.getOrCreate(this);
        if (mExtensionVersion >= 9) {
            mWindowLayoutInfoGetterValue.setText(
                    tracker.getCurrentWindowLayoutInfo(this).toString());
        } else {
            mWindowLayoutInfoGetterValue.setText(
                    "WindowLayoutInfo getter is not available on this device. "
                            + "The WM extension version must be at least 9, "
                            + "but this device has version " + mExtensionVersion + ".");
        }

        mWindowInfoTrackerCallbackAdapter = new WindowInfoTrackerCallbackAdapter(tracker);
        mWindowInfoTrackerCallbackAdapter.addWindowLayoutInfoListener(
                this, Runnable::run, this::onWindowLayoutInfoUpdated);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWindowInfoTrackerCallbackAdapter != null) {
            mWindowInfoTrackerCallbackAdapter.addWindowLayoutInfoListener(
                    this, Runnable::run, mWindowLayoutInfoChangedConsumer);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWindowInfoTrackerCallbackAdapter != null) {
            mWindowInfoTrackerCallbackAdapter.removeWindowLayoutInfoListener(
                    mWindowLayoutInfoChangedConsumer);
        }
    }

    private void onWindowLayoutInfoUpdated(@NonNull WindowLayoutInfo windowLayoutInfo) {
        Log.d(TAG, "onWindowLayoutInfoUpdated: " + windowLayoutInfo);
        ContextCompat.getMainExecutor(this).execute(
                () -> mWindowLayoutInfoFlowValue.setText(windowLayoutInfo.toString()));
    }
}
