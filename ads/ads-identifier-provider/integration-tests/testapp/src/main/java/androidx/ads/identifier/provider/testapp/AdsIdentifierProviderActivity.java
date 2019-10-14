/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ads.identifier.provider.testapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.ads.identifier.provider.AdvertisingIdProviderInfo;
import androidx.ads.identifier.provider.AdvertisingIdProviderManager;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

/**
 * Simple activity as an Advertising ID Provider.
 */
public class AdsIdentifierProviderActivity extends Activity {

    private Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ads_identifier_provider);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new Adapter();
        recyclerView.setAdapter(mAdapter);
    }

    /** Lists all the Advertising ID Providers. */
    public void listProviders(View view) {
        List<AdvertisingIdProviderInfo> allAdIdProviders =
                AdvertisingIdProviderManager.getAdvertisingIdProviders(this);

        TextView textView = findViewById(R.id.main_text);
        textView.setText("There are " + allAdIdProviders.size() + " provider(s) on the device.\n");

        mAdapter.setAdvertisingIdProviderInfoList(allAdIdProviders);
    }

    static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private List<AdvertisingIdProviderInfo> mAdvertisingIdProviderInfoList;

        static class ViewHolder extends RecyclerView.ViewHolder {
            @NonNull
            TextView mTextView;
            @NonNull
            Button mButton;

            ViewHolder(@NonNull View view) {
                super(view);
                mTextView = view.findViewById(R.id.text);
                mButton = view.findViewById(R.id.button);
            }
        }

        Adapter() {
            mAdvertisingIdProviderInfoList = Collections.emptyList();
        }

        void setAdvertisingIdProviderInfoList(
                List<AdvertisingIdProviderInfo> advertisingIdProviderInfoList) {
            mAdvertisingIdProviderInfoList = advertisingIdProviderInfoList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.provider_info, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, int position) {
            AdvertisingIdProviderInfo providerInfo = mAdvertisingIdProviderInfoList.get(position);
            TextView textView = holder.mTextView;
            textView.setText("Package name: " + providerInfo.getPackageName() + "\n");

            textView.append("Settings UI intent: " + providerInfo.getSettingsIntent() + "\n");
            if (providerInfo.getSettingsIntent() != null) {
                holder.mButton.setClickable(true);
                holder.mButton.setOnClickListener(view -> {
                    view.getContext().startActivity(providerInfo.getSettingsIntent());
                });
            } else {
                holder.mButton.setClickable(false);
                textView.append(textView.getResources().getString(R.string.empty_settings_warning));
            }

            textView.append("Is highest priority: " + providerInfo.isHighestPriority() + "\n");
        }

        @Override
        public int getItemCount() {
            return mAdvertisingIdProviderInfoList.size();
        }
    }
}
