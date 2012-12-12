/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v13.app;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import java.util.ArrayList;
import java.util.HashMap;

class RemoteViewsListFactory implements RemoteViewsFactory {

    ArrayList<RemoteViews> mRemoteViewsList;
    int mViewTypeCount;

    private static final HashMap<Intent.FilterComparison, RemoteViewsListFactory> sFactories =
            new HashMap<Intent.FilterComparison, RemoteViewsListFactory>();

    public RemoteViewsListFactory(ArrayList<RemoteViews> list) {
        setViewsList(list);

    }

    public void setViewsList(ArrayList<RemoteViews> list) {
        mRemoteViewsList = list;
        init();
    }

    private void init() {
        if (mRemoteViewsList == null) return;

        ArrayList<Integer> viewTypes = new ArrayList<Integer>();
        for (RemoteViews rv : mRemoteViewsList) {
            if (!viewTypes.contains(rv.getLayoutId())) {
                viewTypes.add(rv.getLayoutId());
            }
        }
        mViewTypeCount = viewTypes.size();
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        init();
    }

    @Override
    public void onDestroy() {
        Intent.FilterComparison key = null;
        for (Intent.FilterComparison k : sFactories.keySet()) {
            if (sFactories.get(k) == this) {
                key = k;
            }
        }
        if (key != null) {
            sFactories.remove(key);
        }
    }

    @Override
    public int getCount() {
        if (mRemoteViewsList != null) {
            return mRemoteViewsList.size();
        } else {
            return 0;
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < getCount()) {
            return mRemoteViewsList.get(position);
        } else {
            return null;
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return mViewTypeCount;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public static RemoteViewsFactory getFactory(Intent intent) {
        Intent.FilterComparison fc = new Intent.FilterComparison(intent);
        return sFactories.get(fc);
    }

    public static void addOrUpdateListFactory(Context ctx, Intent intent, int widgetId, int viewId,
            ArrayList<RemoteViews> list) {

        Intent.FilterComparison fc = new Intent.FilterComparison(intent);
        if (sFactories.containsKey(fc)) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            RemoteViewsListFactory rvlf = sFactories.get(fc);
            rvlf.setViewsList(list);
            mgr.notifyAppWidgetViewDataChanged(widgetId, viewId);
        } else {
            RemoteViewsListFactory rvlf = new RemoteViewsListFactory(list);
            sFactories.put(fc, rvlf);
        }
    }
}
