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

package androidx.camera.testing.impl.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.espresso.idling.CountingIdlingResource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A intermediate activity to launch another activity and wait for result.
 */
public class RequestResultTestActivity extends Activity {

    /** Used to pass the action intent of the target activity. */
    public static final String INTENT_EXTRA_INTENT_ACTION = "intent_action";
    /** Used to pass the extra bundle info to launch the target activity. */
    public static final String INTENT_EXTRA_BUNDLE = "intent_extra_bundle";
    /**
     * The result error code that is reported when the target activity finishes without returning
     * any result data, for example, when the target activity's process crashes or is killed.
     */
    public static final int RESULT_ERROR_NO_RESULT_DATA = -2;
    private static final String INTENT_EXTRA_RESULT_ERROR_MESSAGE = "result_error_message";
    private static final int REQUEST_CODE_DEFAULT = 0;
    private final CountingIdlingResource mRequestResultReady = new CountingIdlingResource(
            "RequestResultReady");

    private int mResultErrorCode = 0;
    private @Nullable String mResultErrorMessage = null;

    /**
     * Retrieves the CountingIdlingResource to know whether the result has been returned or not.
     */
    public @NonNull CountingIdlingResource getRequestResultReadyIdlingResource() {
        return mRequestResultReady;
    }

    /**
     * Retrieves the result error code.
     */
    public int getResultErrorCode() {
        return mResultErrorCode;
    }

    /**
     * Retrieves the result error message.
     */
    public @Nullable String getResultErrorMessage() {
        return mResultErrorMessage;
    }

    @SuppressWarnings("deprecation") // Bundle.get
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequestResultReady.increment();
        String intentAction = getIntent().getStringExtra(INTENT_EXTRA_INTENT_ACTION);
        Intent intent = new Intent(intentAction);
        Bundle bundle = getIntent().getBundleExtra(INTENT_EXTRA_BUNDLE);
        // Uses Bundle to bring the extra settings to the target activity. instanceof is used to
        // correctly cast the extra values. Needs to add new types and verify the results when new
        // types are needed here.
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value instanceof String) {
                    intent.putExtra(key, (String) value);
                } else if (value instanceof Boolean) {
                    intent.putExtra(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    intent.putExtra(key, (Integer) value);
                }
            }
        }
        startActivityForResult(intent, REQUEST_CODE_DEFAULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            // The target activity always returns a result Intent when it finishes normally. A null
            // Intent means that it didn't finish normally, e.g. its process crashed. In that case,
            // the result code is Activity.RESULT_CANCELED (0), which can't be distinguished from
            // the success result code used by the target activities (e.g. RESULT_SUCCESS (0) in
            // the extensions test app's ReleaseTestActivity). Reports a dedicated error code.
            mResultErrorCode = RESULT_ERROR_NO_RESULT_DATA;
            mResultErrorMessage = "No result data is returned from the target activity (result "
                    + "code: " + resultCode + "). The target activity might have crashed.";
        } else {
            mResultErrorCode = resultCode;
            mResultErrorMessage = data.getStringExtra(INTENT_EXTRA_RESULT_ERROR_MESSAGE);
        }
        mRequestResultReady.decrement();
    }
}
