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

package com.example.androidx.mediarouting.activities;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouterParams;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidx.mediarouting.R;
import com.example.androidx.mediarouting.RoutesManager;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRoutingActivity;
import com.example.androidx.mediarouting.services.SampleDynamicGroupMediaRouteProviderService;
import com.example.androidx.mediarouting.services.SampleMediaRouteProviderService;
import com.example.androidx.mediarouting.ui.RoutesAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Allows the user to control dialog types, enabling or disabling Dynamic Groups, enabling or
 * disabling transfer to local and customize the routes exposed by {@link
 * SampleDynamicGroupMediaRouteProviderService}.
 */
public final class SettingsActivity extends AppCompatActivity {
    private final ProviderServiceConnection mConnection = new ProviderServiceConnection();
    private PackageManager mPackageManager;
    private MediaRouter mMediaRouter;
    private RoutesManager mRoutesManager;
    private RoutesAdapter mRoutesAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mPackageManager = getPackageManager();
        mMediaRouter = MediaRouter.getInstance(this);
        mRoutesManager = RoutesManager.getInstance(getApplicationContext());

        setUpViews();

        RoutesAdapter.RouteItemListener routeItemListener = new RoutesAdapter.RouteItemListener() {
            @Override
            public void onRouteEditClick(@NonNull String routeId) {
                AddEditRouteActivity.launchActivity(
                        /* context= */ SettingsActivity.this, /* routeId */ routeId);
            }

            @Override
            public void onRouteDeleteClick(@NonNull String routeId) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.delete_route_alert_dialog_title)
                        .setMessage(R.string.delete_route_alert_dialog_message)
                        .setPositiveButton(android.R.string.cancel, null)
                        .setNegativeButton(
                                android.R.string.ok,
                                (dialogInterface, i) -> {
                                    mRoutesManager.deleteRouteWithId(routeId);
                                    SampleDynamicGroupMediaRouteProviderService providerService =
                                            mConnection.mService;
                                    if (providerService != null) {
                                        providerService.reloadRoutes();
                                    }
                                    mRoutesAdapter.updateRoutes(
                                            mRoutesManager.getRouteItems());
                                })
                        .show();
            }
        };

        Button goToRouteListingPreferenceButton =
                findViewById(R.id.go_to_route_listing_preference_button);
        goToRouteListingPreferenceButton.setOnClickListener(
                unusedView -> {
                    startActivity(new Intent(this, RouteListingPreferenceActivity.class));
                });

        RecyclerView routeList = findViewById(R.id.routes_recycler_view);
        routeList.setLayoutManager(new LinearLayoutManager(/* context= */ this));
        mRoutesAdapter = new RoutesAdapter(mRoutesManager.getRouteItems(), routeItemListener);
        routeList.setAdapter(mRoutesAdapter);
        routeList.setHasFixedSize(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindToDynamicProviderService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRoutesAdapter.updateRoutes(mRoutesManager.getRouteItems());
    }

    @Override
    protected void onStop() {
        try {
            unbindService(mConnection);
        } catch (RuntimeException e) {
            // This happens when the provider is disabled, but there's no way of preventing this
            // completely so we just ignore the exception.
        }
        super.onStop();
    }

    private void setUpViews() {
        setUpDynamicGroupsEnabledSwitch();
        setUpTransferToLocalSwitch();
        setUpSimpleProviderEnabledSwitch();
        setUpDynamicProviderEnabledSwitch();
        setUpDialogTypeDropDownList();
        setUpNewRouteButton();
        setupSystemRoutesButton();
    }

    private void setUpDynamicGroupsEnabledSwitch() {
        Switch dynamicRoutingEnabled = findViewById(R.id.dynamic_routing_switch);
        dynamicRoutingEnabled.setChecked(mRoutesManager.isDynamicRoutingEnabled());
        dynamicRoutingEnabled.setOnCheckedChangeListener(
                (compoundButton, enabled) -> {
                    mRoutesManager.setDynamicRoutingEnabled(enabled);
                    SampleDynamicGroupMediaRouteProviderService providerService =
                            mConnection.mService;
                    if (providerService != null) {
                        providerService.reloadDynamicRoutesEnabled();
                    }
                });
    }

    private void setUpTransferToLocalSwitch() {
        Switch showThisPhoneSwitch = findViewById(R.id.show_this_phone_switch);
        showThisPhoneSwitch.setChecked(mMediaRouter.getRouterParams().isTransferToLocalEnabled());
        showThisPhoneSwitch.setOnCheckedChangeListener(
                (compoundButton, enabled) -> {
                    MediaRouterParams.Builder builder =
                            new MediaRouterParams.Builder(mMediaRouter.getRouterParams());
                    builder.setTransferToLocalEnabled(enabled);
                    mMediaRouter.setRouterParams(builder.build());
                });
    }

    private void setUpSimpleProviderEnabledSwitch() {
        Switch simpleProviderEnabledSwitch = findViewById(R.id.enable_simple_provider_switch);
        ComponentName simpleProviderComponentName =
                new ComponentName(/* context= */ this, SampleMediaRouteProviderService.class);
        simpleProviderEnabledSwitch.setChecked(
                mPackageManager.getComponentEnabledSetting(simpleProviderComponentName)
                        != PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        simpleProviderEnabledSwitch.setOnCheckedChangeListener(
                (compoundButton, enabled) -> {
                    mPackageManager
                            .setComponentEnabledSetting(
                                    simpleProviderComponentName,
                                    enabled
                                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    /* flags= */ PackageManager.DONT_KILL_APP);
                });
    }

    private void setUpDynamicProviderEnabledSwitch() {
        Switch dynamicProviderEnabledSwitch = findViewById(R.id.enable_dynamic_provider_switch);
        ComponentName dynamicProviderComponentName =
                new ComponentName(
                        /* context= */ this, SampleDynamicGroupMediaRouteProviderService.class);
        dynamicProviderEnabledSwitch.setChecked(
                mPackageManager.getComponentEnabledSetting(dynamicProviderComponentName)
                        != PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        dynamicProviderEnabledSwitch.setOnCheckedChangeListener(
                (compoundButton, enabled) -> {
                    mPackageManager
                            .setComponentEnabledSetting(
                                    dynamicProviderComponentName,
                                    enabled
                                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    /* flags= */ PackageManager.DONT_KILL_APP);
                    if (enabled) {
                        bindToDynamicProviderService();
                    }
                });
    }

    private void setUpDialogTypeDropDownList() {
        Spinner spinner = findViewById(R.id.dialog_spinner);
        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> adapterView, View view, int i, long l) {
                        mRoutesManager.setDialogType(RoutesManager.DialogType.values()[i]);
                        mRoutesManager.reloadDialogType();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                });

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                        this, R.array.dialog_types_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        if (mMediaRouter.getRouterParams().isOutputSwitcherEnabled()) {
            spinner.setSelection(2);
        } else if (mMediaRouter.getRouterParams().getDialogType()
                == MediaRouterParams.DIALOG_TYPE_DYNAMIC_GROUP) {
            spinner.setSelection(1);
        }
    }

    private void setUpNewRouteButton() {
        FloatingActionButton newRouteButton = findViewById(R.id.new_route_button);
        newRouteButton.setOnClickListener(
                view -> {
                    AddEditRouteActivity.launchActivity(
                            /* context= */ SettingsActivity.this, /* routeId= */ null);
                });
    }

    private void setupSystemRoutesButton() {
        AppCompatButton showSystemRoutesButton = findViewById(R.id.open_system_routes);
        showSystemRoutesButton.setOnClickListener(v -> SystemRoutingActivity.launch(this));
    }

    private void bindToDynamicProviderService() {
        Intent intent = new Intent(this, SampleDynamicGroupMediaRouteProviderService.class);
        intent.setAction(SampleDynamicGroupMediaRouteProviderService.ACTION_BIND_LOCAL);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private static class ProviderServiceConnection implements ServiceConnection {

        @Nullable private SampleDynamicGroupMediaRouteProviderService mService;

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SampleDynamicGroupMediaRouteProviderService.LocalBinder binder =
                    (SampleDynamicGroupMediaRouteProviderService.LocalBinder) service;
            mService = binder.getService();
            mService.reloadRoutes();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    }
}
