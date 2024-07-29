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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.espresso.idling.CountingIdlingResource;

/**
 * A intermediate activity to launch another activity and wait for result.
 */
public class RequestResultTestActivity extends Activity {

    /** Used to pass the action intent of the target activity. */
    public static final String INTENT_EXTRA_INTENT_ACTION = "intent_action";
    /** Used to pass the extra bundle info to launch the target activity. */
    public static final String INTENT_EXTRA_BUNDLE = "intent_extra_bundle";
    private static final String INTENT_EXTRA_RESULT_ERROR_MESSAGE = "result_error_message";
    private static final int REQUEST_CODE_DEFAULT = 0;
    private final CountingIdlingResource mRequestResultReady = new CountingIdlingResource(
            "RequestResultReady");

    private int mResultErrorCode = 0;
    @Nullable
    private String mResultErrorMessage = null;

    /**
     * Retrieves the CountingIdlingResource to know whether the result has been returned or not.
     */
    @NonNull
    public CountingIdlingResource getRequestResultReadyIdlingResource() {
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
    @Nullable
    public String getResultErrorMessage() {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mResultErrorCode = resultCode;
        mResultErrorMessage = data.getStringExtra(INTENT_EXTRA_RESULT_ERROR_MESSAGE);
        mRequestResultReady.decrement();
    }
}
