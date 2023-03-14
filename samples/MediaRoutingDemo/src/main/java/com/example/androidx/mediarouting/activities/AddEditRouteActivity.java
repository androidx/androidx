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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.androidx.mediarouting.R;
import com.example.androidx.mediarouting.RoutesManager;
import com.example.androidx.mediarouting.data.RouteItem;
import com.example.androidx.mediarouting.services.SampleDynamicGroupMediaRouteProviderService;

/** Allows the user to add and edit routes. */
public class AddEditRouteActivity extends AppCompatActivity {
    private static final String EXTRA_ROUTE_ID_KEY = "routeId";

    private SampleDynamicGroupMediaRouteProviderService mService;
    private ServiceConnection mConnection;
    private RoutesManager mRoutesManager;
    private RouteItem mRouteItem;
    private Switch mCanDisconnectSwitch;

    /** Launches the activity. */
    public static void launchActivity(@NonNull Context context, @Nullable String routeId) {
        Intent intent = new Intent(context, AddEditRouteActivity.class);
        intent.putExtra(EXTRA_ROUTE_ID_KEY, routeId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_route);

        mRoutesManager = RoutesManager.getInstance(getApplicationContext());

        String routeId = getIntent().getStringExtra(EXTRA_ROUTE_ID_KEY);
        mRouteItem = mRoutesManager.getRouteWithId(routeId);
        mConnection = new ProviderServiceConnection();

        if (mRouteItem == null) {
            mRouteItem = new RouteItem();
        } else {
            mRouteItem = RouteItem.copyOf(mRouteItem);
        }

        mCanDisconnectSwitch = findViewById(R.id.cam_disconnect_switch);

        setUpViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, SampleDynamicGroupMediaRouteProviderService.class);
        intent.setAction(SampleDynamicGroupMediaRouteProviderService.ACTION_BIND_LOCAL);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    private void setUpViews() {
        setUpEditText(
                findViewById(R.id.name_edit_text),
                mRouteItem.getName(),
                newName -> mRouteItem.setName(newName));

        setUpEditText(
                findViewById(R.id.description_edit_text),
                mRouteItem.getDescription(),
                newDescription -> mRouteItem.setDescription(newDescription));

        setUpEnumBasedSpinner(
                /* context= */ this,
                findViewById(R.id.control_filters_spinner),
                mRouteItem.getControlFilter(),
                newControlFilter ->
                        mRouteItem.setControlFilter((RouteItem.ControlFilter) newControlFilter));

        setUpEnumBasedSpinner(
                /* context= */ this,
                findViewById(R.id.playback_stream_spinner),
                mRouteItem.getPlaybackStream(),
                newPlaybackStream ->
                        mRouteItem.setPlaybackStream((RouteItem.PlaybackStream) newPlaybackStream));

        setUpEnumBasedSpinner(
                /* context= */ this,
                findViewById(R.id.playback_type_spinner),
                mRouteItem.getPlaybackType(),
                newPlaybackType ->
                        mRouteItem.setPlaybackType((RouteItem.PlaybackType) newPlaybackType));

        setUpEnumBasedSpinner(
                /* context= */ this,
                findViewById(R.id.device_type_spinner),
                mRouteItem.getDeviceType(),
                newDeviceType -> mRouteItem.setDeviceType((RouteItem.DeviceType) newDeviceType));

        setUpEnumBasedSpinner(
                /* context= */ this,
                findViewById(R.id.volume_handling_spinner),
                mRouteItem.getVolumeHandling(),
                mewVolumeHandling ->
                        mRouteItem.setVolumeHandling((RouteItem.VolumeHandling) mewVolumeHandling));

        setUpEditText(
                findViewById(R.id.volume_edit_text),
                String.valueOf(mRouteItem.getVolume()),
                mewVolume -> mRouteItem.setVolume(Integer.parseInt(mewVolume)));

        setUpEditText(
                findViewById(R.id.volume_max_edit_text),
                String.valueOf(mRouteItem.getVolumeMax()),
                mewVolumeMax -> mRouteItem.setVolumeMax(Integer.parseInt(mewVolumeMax)));

        setUpCanDisconnectSwitch();

        setUpSaveButton();
    }

    private void setUpCanDisconnectSwitch() {
        mCanDisconnectSwitch.setChecked(mRouteItem.isCanDisconnect());
        mCanDisconnectSwitch.setOnCheckedChangeListener(
                (compoundButton, b) -> {
                    mRouteItem.setCanDisconnect(b);
                });
    }

    private void setUpSaveButton() {
        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(
                view -> {
                    mRoutesManager.addRoute(mRouteItem);
                    mService.reloadRoutes();
                    finish();
                });
    }

    private static void setUpEditText(
            EditText editText,
            String currentValue,
            RoutePropertySetter<String> routePropertySetter) {
        editText.setText(currentValue);
        editText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        routePropertySetter.accept(charSequence.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {}
                });
    }

    private static void setUpEnumBasedSpinner(
            Context context,
            Spinner spinner,
            Enum<?> anEnum,
            RoutePropertySetter<Enum<?>> routePropertySetter) {
        Enum<?>[] enumValues = anEnum.getDeclaringClass().getEnumConstants();
        ArrayAdapter<Enum<?>> adapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, enumValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(anEnum.ordinal());

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> adapterView, View view, int i, long l) {
                        routePropertySetter.accept(
                                anEnum.getDeclaringClass().getEnumConstants()[i]);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                });
    }

    private interface RoutePropertySetter<T> {
        void accept(T value);
    }

    private class ProviderServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SampleDynamicGroupMediaRouteProviderService.LocalBinder binder =
                    (SampleDynamicGroupMediaRouteProviderService.LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    }
}
