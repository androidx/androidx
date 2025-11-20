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

package com.example.androidx.mediarouting.activities.systemrouting;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.androidx.mediarouting.R;
import com.example.androidx.mediarouting.activities.systemrouting.source.AndroidXMediaRouterSystemRoutesSource;
import com.example.androidx.mediarouting.activities.systemrouting.source.AudioManagerSystemRoutesSource;
import com.example.androidx.mediarouting.activities.systemrouting.source.BluetoothManagerSystemRoutesSource;
import com.example.androidx.mediarouting.activities.systemrouting.source.MediaRouter2SystemRoutesSource;
import com.example.androidx.mediarouting.activities.systemrouting.source.MediaRouterSystemRoutesSource;
import com.example.androidx.mediarouting.activities.systemrouting.source.SystemRoutesSource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows available system routes gathered from different sources.
 */
public final class SystemRoutingActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_BLUETOOTH_CONNECT = 4199;

    private final @NonNull SystemRoutesAdapter mSystemRoutesAdapter =
            new SystemRoutesAdapter(this::onRouteItemClicked);

    private final @NonNull Map<String, SystemRoutesSource> mSystemRoutesSources = new HashMap<>();
    private @NonNull SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * Creates and launches an intent to start current activity.
     */
    public static void launch(@NonNull Context context) {
        Intent intent = new Intent(context, SystemRoutingActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_routing);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        mSwipeRefreshLayout = findViewById(R.id.pull_to_refresh_layout);

        recyclerView.setAdapter(mSystemRoutesAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mSwipeRefreshLayout.setOnRefreshListener(this::refreshSystemRoutesList);

        if (hasBluetoothPermission()) {
            initializeSystemRoutesSources();
            refreshSystemRoutesList();
        } else {
            requestBluetoothPermission();
        }
    }

    @Override
    protected void onDestroy() {
        for (SystemRoutesSource source : mSystemRoutesSources.values()) {
            source.stop();
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String @NonNull [] permissions,
            int @NonNull [] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_BLUETOOTH_CONNECT
                && grantResults.length > 0) {
            if (grantResults[0] != PERMISSION_GRANTED) {
                Toast.makeText(
                                this,
                                getString(R.string.system_routing_activity_bluetooth_denied),
                                Toast.LENGTH_LONG)
                        .show();
            }
            initializeSystemRoutesSources();
            refreshSystemRoutesList();
        }
    }

    private void refreshSystemRoutesList() {
        List<SystemRoutesAdapterItem> systemRoutesSourceItems = new ArrayList<>();
        for (SystemRoutesSource source : mSystemRoutesSources.values()) {
            systemRoutesSourceItems.add(source.getSourceItem());
            systemRoutesSourceItems.addAll(source.fetchSourceRouteItems());
        }
        mSystemRoutesAdapter.setItems(systemRoutesSourceItems);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void onRouteItemClicked(SystemRouteItem item) {
        SystemRoutesSource systemRoutesSource = mSystemRoutesSources.get(item.mSourceId);
        if (systemRoutesSource == null) {
            throw new IllegalStateException("Couldn't find source with id: " + item.mSourceId);
        }
        if (!systemRoutesSource.select(item)) {
            Toast.makeText(
                            /* context= */ this,
                            "Something went wrong with route selection",
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    private boolean hasBluetoothPermission() {
        return ContextCompat.checkSelfPermission(/* context= */ this, BLUETOOTH_CONNECT)
                == PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(/* context= */ this, BLUETOOTH_SCAN)
                == PERMISSION_GRANTED;
    }

    private void requestBluetoothPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{BLUETOOTH_CONNECT, BLUETOOTH_SCAN}, REQUEST_CODE_BLUETOOTH_CONNECT);
    }

    private void initializeSystemRoutesSources() {
        ArrayList<SystemRoutesSource> sources = new ArrayList<>();
        sources.add(MediaRouterSystemRoutesSource.create(/* context= */ this));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            sources.add(MediaRouter2SystemRoutesSource.create(/* context= */ this));
        }

        sources.add(AndroidXMediaRouterSystemRoutesSource.create(/* context= */ this));

        if (hasBluetoothPermission()) {
            sources.add(BluetoothManagerSystemRoutesSource.create(/* context= */ this));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sources.add(AudioManagerSystemRoutesSource.create(/* context= */ this));
        }

        mSystemRoutesSources.clear();
        for (SystemRoutesSource source : sources) {
            source.setOnRoutesChangedListener(this::refreshSystemRoutesList);
            mSystemRoutesSources.put(source.getSourceId(), source);
            source.start();
        }
    }
}
