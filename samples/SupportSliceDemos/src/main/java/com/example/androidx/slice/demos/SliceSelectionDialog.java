/*
 * Copyright 2018 The Android Open Source Project
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

package com.example.androidx.slice.demos;

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.app.ProgressDialog;
import android.app.slice.SliceProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceViewManager;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.ListContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Helper for selecting slices from apps.
 */
public class SliceSelectionDialog {

    private static final String TAG = "SliceSelectionDialog";

    private SliceSelectionDialog() {
    }

    /**
     * Show the selection dialog
     */
    public static void create(Context context, Consumer<Uri> selectedCallback) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> apps = pm.getInstalledPackages(PackageManager.GET_PROVIDERS);
        List<ProviderInfo> sliceProviders = new ArrayList<>();
        IconCache cache = new IconCache();
        HashMap<String, String> labels = new HashMap<>();
        ProviderTypeCache typeCache = new ProviderTypeCache(context);

        ProgressDialog dialog = ProgressDialog.show(context, null, "Loading...");

        new Thread(() -> {
            for (PackageInfo app : apps) {
                if (app.providers == null || app.providers.length == 0) {
                    continue;
                }
                labels.put(app.packageName, app.applicationInfo.loadLabel(pm).toString());
                for (ProviderInfo provider : app.providers) {
                    if (SliceProvider.SLICE_TYPE.equals(typeCache.getType(provider))) {
                        sliceProviders.add(provider);
                    }
                }
            }
            Collections.sort(sliceProviders,
                    (o1, o2) -> labels.get(o1.packageName).compareTo(labels.get(o2.packageName)));
            new Handler(Looper.getMainLooper()).post(() -> {
                dialog.dismiss();
                ListAdapter adapter = new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return sliceProviders.size();
                    }

                    @Override
                    public Object getItem(int position) {
                        return sliceProviders.get(position);
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        if (convertView == null) {
                            convertView = LayoutInflater.from(context).inflate(R.layout.app_item,
                                    parent,
                                    false);
                        }
                        ImageView iv = convertView.findViewById(R.id.icon);
                        TextView title = convertView.findViewById(R.id.title);
                        convertView.findViewById(R.id.summary).setVisibility(View.GONE);
                        ProviderInfo provider = sliceProviders.get(position);
                        cache.loadIcon(iv, provider, pm);
                        title.setText(labels.get(provider.packageName));
                        return convertView;
                    }
                };
                DialogInterface.OnClickListener clickListener = (dialog1, which) -> {
                    ProviderInfo appInfo = sliceProviders.get(which);
                    showSliceList(context, selectedCallback, appInfo,
                            labels.get(appInfo.packageName));
                };
                new AlertDialog.Builder(context)
                        .setTitle("Select app")
                        .setAdapter(adapter, clickListener)
                        .show();
            });
        }).start();
    }

    private static void showSliceList(Context context, Consumer<Uri> selectedCallback,
            ProviderInfo provider, String label) {
        ProgressDialog dialog = ProgressDialog.show(context, null, "Loading...");

        new Thread(() -> {
            String authority = provider.authority.split(";", -1)[0];
            HashMap<String, String> labels = new HashMap<>();
            SliceViewManager sliceViewManager = SliceViewManager.getInstance(context);
            List<Uri> slices = new ArrayList<>(sliceViewManager.getSliceDescendants(
                    new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                            .authority(authority)
                            .build()));

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setPackage(provider.packageName);
            Uri uri = sliceViewManager.mapIntentToUri(mainIntent);
            List<ResolveInfo> info = context.getPackageManager().queryIntentActivities(mainIntent,
                    0);
            if (info.size() == 1) {
                // If we resolve to an activity, set the intent to explicitly
                // point at that activity.
                mainIntent = new Intent()
                        .setComponent(new ComponentName(provider.packageName,
                                    info.get(0).activityInfo.name));
            }
            if (uri != null) {
                slices.add(uri);
            }
            for (Uri slice : slices) {
                labels.put(slice.toString(), loadLabel(context, sliceViewManager, slice));
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                dialog.dismiss();
                ListAdapter adapter = new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return slices.size();
                    }

                    @Override
                    public Object getItem(int position) {
                        return slices.get(position);
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        if (convertView == null) {
                            convertView = LayoutInflater.from(context).inflate(R.layout.app_item,
                                    parent,
                                    false);
                        }
                        TextView title = convertView.findViewById(R.id.title);
                        TextView summary = convertView.findViewById(R.id.summary);
                        convertView.findViewById(R.id.icon_frame).setVisibility(View.GONE);
                        Uri uri = slices.get(position);
                        title.setText(labels.get(uri.toString()));
                        summary.setText(uri.toString());
                        return convertView;
                    }
                };
                DialogInterface.OnClickListener clickListener = (dialog1, which) -> {
                    Uri slice = slices.get(which);
                    selectedCallback.accept(slice);
                };
                new AlertDialog.Builder(context)
                    .setTitle(label)
                    .setAdapter(adapter, clickListener)
                    .show();
            });
        }).start();
    }

    private static String loadLabel(Context context, SliceViewManager sliceViewManager, Uri slice) {
        Slice content = bindSliceSynchronous(context, sliceViewManager, slice);
        return String.valueOf(findTitle(context, content, SliceMetadata.from(context, content)));
    }

    protected static CharSequence findTitle(Context context, Slice loadedSlice,
            SliceMetadata metaData) {
        ListContent content = new ListContent(loadedSlice);
        SliceItem headerItem = content.getHeader() != null
                ? content.getHeader().getSliceItem() : null;
        if (headerItem == null) return null;
        // Look for a title, then large text, then any text at all.
        SliceItem title = SliceQuery.find(headerItem, FORMAT_TEXT, HINT_TITLE, null);
        if (title != null) {
            return title.getText();
        }
        title = SliceQuery.find(headerItem, FORMAT_TEXT, HINT_LARGE, null);
        if (title != null) {
            return title.getText();
        }
        title = SliceQuery.find(headerItem, FORMAT_TEXT);
        if (title != null) {
            return title.getText();
        }
        return null;
    }

    protected static Slice bindSliceSynchronous(Context context, SliceViewManager manager,
            Uri slice) {
        final Slice[] returnSlice = new Slice[1];
        CountDownLatch latch = new CountDownLatch(1);
        SliceViewManager.SliceCallback callback = new SliceViewManager.SliceCallback() {
            @Override
            public void onSliceUpdated(Slice s) {
                try {
                    SliceMetadata m = SliceMetadata.from(context, s);
                    if (m.getLoadingState() == SliceMetadata.LOADED_ALL) {
                        returnSlice[0] = s;
                        latch.countDown();
                        manager.unregisterSliceCallback(slice, this);
                    }
                } catch (Exception e) {
                    Log.w(TAG, slice + " cannot be indexed", e);
                    returnSlice[0] = s;
                    latch.countDown();
                }
            }
        };
        // Register a callback until we get a loaded slice.
        manager.registerSliceCallback(slice, callback);
        // Trigger the first bind in case no loading is needed.
        callback.onSliceUpdated(manager.bindSlice(slice));
        try {
            latch.await(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        return returnSlice[0];
    }

    private static class IconCache {
        private final HashMap<String, Drawable> mIcons = new HashMap<>();

        private void loadIcon(ImageView iv, ProviderInfo provider, PackageManager pm) {
            if (mIcons.containsKey(provider.packageName)) {
                iv.setImageDrawable(mIcons.get(provider.packageName));
            }
            AsyncTask.execute(() -> {
                mIcons.put(provider.packageName, provider.loadIcon(pm));
                iv.post(() -> iv.setImageDrawable(mIcons.get(provider.packageName)));
            });
        }
    }

    private static class ProviderTypeCache {
        private static final String PROVIDER_LOOKUP = "provider_lookup";

        private final SharedPreferences mSharedPreferences;
        private final Context mContext;

        ProviderTypeCache(Context context) {
            mContext = context;
            mSharedPreferences = context.getSharedPreferences(PROVIDER_LOOKUP, 0);
        }

        private String getType(ProviderInfo provider) {
            if (provider == null || provider.authority == null) {
                return "";
            }
            String authority = provider.authority.split(";", -1)[0];
            if (mSharedPreferences.contains(authority)) {
                return mSharedPreferences.getString(authority, null);
            }
            String type = String.valueOf(mContext.getContentResolver().getType(new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(authority)
                        .build()));
            mSharedPreferences.edit()
                .putString(authority, type)
                .commit();
            return type;
        }
    }
}
