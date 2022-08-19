/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.test.uiautomator.testapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * {@link Activity} for testing {@link androidx.test.uiautomator.Until} functionality. Contains
 * a series of buttons and views that fulfill specific conditions.
 */
public class UntilTestActivity extends Activity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.until_test_activity);

        Button goneButton = (Button) findViewById(R.id.gone_button);
        TextView goneTarget = (TextView) findViewById(R.id.gone_target);
        goneButton.setOnClickListener(v -> goneTarget.setVisibility(View.GONE));

        Button findButton = (Button) findViewById(R.id.find_button);
        TextView findTarget = (TextView) findViewById(R.id.find_target);
        findButton.setOnClickListener(v -> findTarget.setVisibility(View.VISIBLE));

        Button checkedButton = (Button) findViewById(R.id.checked_button);
        CheckBox checkedTarget = (CheckBox) findViewById(R.id.checked_target);
        checkedButton.setOnClickListener(v -> checkedTarget.setChecked(true));

        Button clickableButton = (Button) findViewById(R.id.clickable_button);
        TextView clickableTarget = (TextView) findViewById(R.id.clickable_target);
        clickableButton.setOnClickListener(v -> clickableTarget.setClickable(true));

        Button enabledButton = (Button) findViewById(R.id.enabled_button);
        TextView enabledTarget = (TextView) findViewById(R.id.enabled_target);
        enabledButton.setOnClickListener(v -> enabledTarget.setEnabled(true));

        Button focusableButton = (Button) findViewById(R.id.focusable_button);
        TextView focusableTarget = (TextView) findViewById(R.id.focusable_target);
        focusableButton.setOnClickListener(v -> focusableTarget.setFocusable(true));

        Button focusedButton = (Button) findViewById(R.id.focused_button);
        TextView focusedTarget = (TextView) findViewById(R.id.focused_target);
        focusedButton.setOnClickListener(v -> focusedTarget.requestFocus());

        Button longClickableButton = (Button) findViewById(R.id.long_clickable_button);
        TextView longClickableTarget = (TextView) findViewById(R.id.long_clickable_target);
        longClickableButton.setOnClickListener(v -> longClickableTarget.setLongClickable(true));

        Button scrollableButton = (Button) findViewById(R.id.scrollable_button);
        HorizontalScrollView scrollableTarget = (HorizontalScrollView) findViewById(
                R.id.scrollable_target);
        scrollableButton.setOnClickListener(v -> {
            scrollableTarget.getLayoutParams().width = 1; // Force horizontal scrollbar.
            scrollableTarget.requestLayout();
        });

        Button selectedButton = (Button) findViewById(R.id.selected_button);
        TextView selectedTarget = (TextView) findViewById(R.id.selected_target);
        selectedButton.setOnClickListener(v -> selectedTarget.setSelected(true));

        Button descButton = (Button) findViewById(R.id.desc_button);
        TextView descTarget = (TextView) findViewById(R.id.desc_target);
        descButton.setOnClickListener(v -> descTarget.setContentDescription("updated_desc"));

        Button textButton = (Button) findViewById(R.id.text_button);
        TextView textTarget = (TextView) findViewById(R.id.text_target);
        textButton.setOnClickListener(v -> textTarget.setText("updated_text"));
    }
}
