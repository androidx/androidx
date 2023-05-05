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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.androidx.mediarouting.R;
import com.example.androidx.mediarouting.activities.systemrouting.source.ComposedSystemRoutesSource;
import com.example.androidx.mediarouting.activities.systemrouting.source.SystemRoutesSource;

import java.util.List;

/**
 * Shows available system routes gathered from different sources.
 */
public final class SystemRoutingActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_BLUETOOTH_CONNECT = 4199;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private SystemRoutesAdapter mSystemRoutesAdapter = new SystemRoutesAdapter();

    private SystemRoutesSource mSystemRoutesSource;

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

        mSystemRoutesSource = ComposedSystemRoutesSource.create(this);

        mRecyclerView = findViewById(R.id.recycler_view);
        mSwipeRefreshLayout = findViewById(R.id.pull_to_refresh_layout);

        mRecyclerView.setAdapter(mSystemRoutesAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            refreshSystemRoutesList();
            mSwipeRefreshLayout.setRefreshing(false);
        });

        refreshSystemRoutesList();
        requestBluetoothPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_BLUETOOTH_CONNECT
                && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onBluetoothPermissionGranted();
            } else {
                onBluetoothPermissionDenied();
            }
        }
    }

    private void refreshSystemRoutesList() {
        List<SystemRouteItem> systemRoutes = mSystemRoutesSource.fetchRoutes();
        mSystemRoutesAdapter.setItems(systemRoutes);
    }

    private void requestBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                REQUEST_CODE_BLUETOOTH_CONNECT);
    }

    private void onBluetoothPermissionGranted() {
        mSystemRoutesSource = ComposedSystemRoutesSource.create(this);
        refreshSystemRoutesList();
    }

    private void onBluetoothPermissionDenied() {
        Toast.makeText(this, getString(R.string.system_routing_activity_bluetooth_denied),
                Toast.LENGTH_LONG).show();
    }
}
