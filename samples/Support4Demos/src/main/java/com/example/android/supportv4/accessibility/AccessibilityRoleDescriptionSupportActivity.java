/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.supportv4.accessibility;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.example.android.supportv4.R;

/**
 * This class demonstrates how to use the support library to set custom
 * role descriptions on your views. This functionality is supported in the
 * support-v4 library on devices running KitKat (API 19) or later.
 */
public class AccessibilityRoleDescriptionSupportActivity extends Activity {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accessibility_roledescription);

        TextView firstTextView = findViewById(R.id.text_view_1);
        String roleDescriptionTextView = getString(R.string.accessibility_roledescription_role);
        ViewCompat.setAccessibilityDelegate(firstTextView,
            new RoleDescriptionAccessibilityDelegate(roleDescriptionTextView));

        TextView heading1 = findViewById(R.id.text_heading_1);
        String roleDescriptionHeading1 = getString(R.string.accessibility_roledescription_h1_role);
        ViewCompat.setAccessibilityDelegate(heading1,
            new RoleDescriptionAccessibilityDelegate(roleDescriptionHeading1));

        TextView heading2 = findViewById(R.id.text_heading_2);
        String roleDescriptionHeading2 = getString(R.string.accessibility_roledescription_h2_role);
        ViewCompat.setAccessibilityDelegate(heading2,
            new RoleDescriptionAccessibilityDelegate(roleDescriptionHeading2));

        TextView heading3 = findViewById(R.id.text_heading_3);
        String roleDescriptionHeading3 = getString(R.string.accessibility_roledescription_h3_role);
        ViewCompat.setAccessibilityDelegate(heading3,
            new RoleDescriptionAccessibilityDelegate(roleDescriptionHeading3));

        // This is an example of an <strong>incorrect</strong> use of the role description.
        // You should not set the role description for standard widgets in your own code.
        Button button = findViewById(R.id.button);
        String roleDescriptionButton =
            getString(R.string.accessibility_roledescription_button_role);
        ViewCompat.setAccessibilityDelegate(button,
            new RoleDescriptionAccessibilityDelegate(roleDescriptionButton));
    }

    /**
     * This class subclasses AccessibilityDelegateCompat to modify a view's role description.
     * You can either override View.onPopulateAccessibilityEvent (API 14+)  or use an accessibility
     * delegate to set the role description. Using an accessibility delegate provides pre-ICS
     * compatibility, and helps to organize your accessibility-related code.
     */
    private static class RoleDescriptionAccessibilityDelegate extends AccessibilityDelegateCompat {
        private final String mRoleDescription;

        public RoleDescriptionAccessibilityDelegate(String roleDescription) {
          mRoleDescription = roleDescription;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host,
                AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            // This call will succeed on all platforms, but it will only set the role description
            // on devices running KitKat (API 19) or later. On older platforms the method call
            // will succeed but do nothing.
            info.setRoleDescription(mRoleDescription);
        }
    }

}
