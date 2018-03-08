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
package androidx.fragment.app.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * A simple Activity used to return a result.
 */
public class FragmentResultActivity extends Activity {
    public static final String EXTRA_RESULT_CODE = "result";
    public static final String EXTRA_RESULT_CONTENT = "result_content";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        int resultCode = getIntent().getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_OK);
        String result = getIntent().getStringExtra(EXTRA_RESULT_CONTENT);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_CONTENT, result);
        setResult(resultCode, intent);
        finish();
    }
}
