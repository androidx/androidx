/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.view.inputmethod;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.R;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

@RequiresApi(30)
public class ImeSecondarySplitViewCompatTestActivity extends Activity {

    EditText mEditText;

    Button mHideImeButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ime_secondary_split_test_activity);
        mEditText = findViewById(R.id.edit_text_id);
        mHideImeButton = findViewById(R.id.hide_ime_id);
        mHideImeButton.setOnClickListener(view -> hideIme());
    }

    private void hideIme() {
        // Use deprecated WindowInsetsControllerCompat method to attempt to hide ime.
        WindowInsetsControllerCompat insetsController =
                ViewCompat.getWindowInsetsController(mEditText);
        if (insetsController != null) {
            insetsController.hide(WindowInsetsCompat.Type.ime());
        }
    }
}
