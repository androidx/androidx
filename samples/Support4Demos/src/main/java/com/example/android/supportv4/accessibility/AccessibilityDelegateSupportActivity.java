/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.example.android.supportv4.R;

/**
 * This class demonstrates how to use the support library to register
 * a View.AccessibilityDelegate that customizes the accessibility
 * behavior of a View. Aiming to maximize simplicity this example
 * tweaks the text reported to accessibility services but using
 * these APIs a client can inject any accessibility functionality into
 * a View.
 */
public class AccessibilityDelegateSupportActivity extends Activity {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accessibility_delegate);
    }

    /**
     * This class represents a View that is customized via an AccessibilityDelegate
     * as opposed to inheritance. An accessibility delegate can be used for adding
     * accessibility to custom Views, i.e. ones that extend classes from android.view,
     * in a backwards compatible fashion. Note that overriding a method whose return
     * type or arguments are not part of a target platform APIs makes your application
     * not backwards compatible with that platform version.
     */
    public static class AccessibilityDelegateSupportView extends View {

        public AccessibilityDelegateSupportView(Context context, AttributeSet attrs) {
            super(context, attrs);
            installAccessibilityDelegate();
        }

        private void installAccessibilityDelegate() {
            // The accessibility delegate enables customizing accessibility behavior
            // via composition as opposed as inheritance. The main benefit is that
            // one can write a backwards compatible application by setting the delegate
            // only if the API level is high enough i.e. the delegate is part of the APIs.
            // The easiest way to achieve that is by using the support library which
            // takes the burden of checking API version and knowing which API version
            // introduced the delegate off the developer.
            ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
                @Override
                public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                    super.onPopulateAccessibilityEvent(host, event);
                    // Note that View.onPopulateAccessibilityEvent was introduced in
                    // ICS and we would like to tweak a bit the text that is reported to
                    // accessibility services via the AccessibilityEvent.
                    event.getText().add(getContext().getString(
                            R.string.accessibility_delegate_custom_text_added));
                }

                @Override
                public void onInitializeAccessibilityNodeInfo(View host,
                        AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    // Note that View.onInitializeAccessibilityNodeInfo was introduced in
                    // ICS and we would like to tweak a bit the text that is reported to
                    // accessibility services via the AccessibilityNodeInfo.
                    info.setText(getContext().getString(
                            R.string.accessibility_delegate_custom_text_added));
                }
            });
        }
    }
}
