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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Service;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.accessibilityservice.AccessibilityServiceInfoCompat;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.support.v4.view.accessibility.AccessibilityManagerCompat.AccessibilityStateChangeListenerCompat;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.supportv4.R;

import java.util.List;

/**
 * <p>
 * This class demonstrates how to use the support library to register
 * an AccessibilityManager.AccessibilityStateChangeListener introduced
 * in ICS to watch changes to the global accessibility state on the
 * device in a backwards compatible manner.
 * </p>
 * <p>
 * This class also demonstrates how to use the support library to query
 * information about enabled accessibility services via APIs introduced
 * in ICS in a backwards compatible manner.
 * </p>
 */
public class AccessibilityManagerSupportActivity extends Activity {

    /** Handle to the accessibility manager service. */
    private AccessibilityManager mAccessibilityManager;

    /** Handle to the View showing accessibility services summary */
    private TextView mAccessibilityStateView;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accessibility_manager);
        mAccessibilityManager = (AccessibilityManager) getSystemService(
                Service.ACCESSIBILITY_SERVICE);
        mAccessibilityStateView = (TextView) findViewById(R.id.accessibility_state);
        registerAccessibilityStateChangeListener();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        updateAccessibilityStateView();
    }

    /**
     * Registers an AccessibilityStateChangeListener that show a Toast
     * when the global accessibility state on the device changes.
     */
    private void registerAccessibilityStateChangeListener() {
        // The AccessibilityStateChange listener APIs were added in ICS. Therefore to be
        // backwards compatible we use the APIs in the support library. Note that if the
        // platform API version is lower and the called API is not available no listener
        // is added and you will not receive a call of onAccessibilityStateChanged.
        AccessibilityManagerCompat.addAccessibilityStateChangeListener(mAccessibilityManager,
                new AccessibilityStateChangeListenerCompat() {
            @Override
            public void onAccessibilityStateChanged(boolean enabled) {
                Toast.makeText(AccessibilityManagerSupportActivity.this,
                        getString(R.string.accessibility_manager_accessibility_state, enabled),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the content of a TextView with description of the enabled
     * accessibility services.
     */
    private void updateAccessibilityStateView() {
        // The API for getting the enabled accessibility services based on feedback
        // type was added in ICS. Therefore to be backwards compatible we use the
        // APIs in the support library. Note that if the platform API version is lower
        // and the called API is not available an empty list of services is returned.
        List<AccessibilityServiceInfo> enabledServices =
            AccessibilityManagerCompat.getEnabledAccessibilityServiceList(mAccessibilityManager,
                    AccessibilityServiceInfo.FEEDBACK_SPOKEN);
        if (!enabledServices.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            final int enabledServiceCount = enabledServices.size();
            for (int i = 0; i < enabledServiceCount; i++) {
                AccessibilityServiceInfo service = enabledServices.get(i);
                // Some new APIs were added in ICS for getting more information about
                // an accessibility service. Again accessed them via the support library.
                ResolveInfo resolveInfo = AccessibilityServiceInfoCompat.getResolveInfo(service);
                String serviceDescription = getString(
                        R.string.accessibility_manager_enabled_service,
                        resolveInfo.loadLabel(getPackageManager()),
                        AccessibilityServiceInfoCompat.feedbackTypeToString(service.feedbackType),
                        AccessibilityServiceInfoCompat.getDescription(service),
                        AccessibilityServiceInfoCompat.getSettingsActivityName(service));
                builder.append(serviceDescription);
            }
            mAccessibilityStateView.setText(builder);
        } else {
            // Either no services or the platform API version is not high enough.
            mAccessibilityStateView.setText(getString(
                    R.string.accessibility_manager_no_enabled_services));
        }
    }
}
