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
import android.net.Uri;
import android.widget.RemoteViews;

import java.util.ArrayList;

/**
 * This is a compatibility version of RemoteViews which simplifies the creation of basic collection
 * widgets (ie. widgets containing {@link android.widget.ListView}, {@link android.widget.GridView},
 * {@link android.widget.StackView} or {@link android.widget.AdapterViewAnimator}). This class
 * provides a compatibility version of the method RemoteViews#setRemoteAdapter(int, ArrayList, int),
 * introduced in API level 18. For platform versions supporting the new API, this class will act as
 * a simple pass-through, and for prior versions will emulate the new API's behavior.
 *
 * Be sure to include declare {@link RemoteViewsServiceCompat} in your manifest, as specified in
 * the doc for
 * {@link RemoteViewsCompat#setRemoteAdapter(RemoteViews, Context, int, int, ArrayList, int)}.
 *
 */
public class RemoteViewsCompat {

    static final String EXTRA_VIEW_ID = "android.support.v13.app.EXTRA_VIEW_ID";

    interface RemoteViewsCompatImpl {
        void setRemoteAdapter(RemoteViews rv, Context ctx, int viewId, int widgetId,
                ArrayList<RemoteViews> list, int viewTypeCount);
    }

    static final RemoteViewsCompatImpl IMPL;
    static {
        // TODO: Replace 17 with 18 when the API level gets bumped
        if (android.os.Build.VERSION.SDK_INT < 17) {
            IMPL = new BaseRemoteViewsCompatImpl();
        } else {
            IMPL = new KRemoteViewsCompatImpl();
        }
    }

    /**
     * Compatibility version of RemoteViews#setRemoteAdapter(int, ArrayList, int).
     *
     * When using this method, it is required that include {@link RemoteViewsServiceCompat}
     * in your manifest:
     *
     * <code>
     *      <service android:name="android.support.v13.app.RemoteViewsServiceCompat"
     *          android:permission="android.permission.BIND_REMOTEVIEWS"
     *          android:exported="false" />
     * </code>
     *
     * @param rv The RemoteViews object.
     * @param ctx Context.
     * @param viewId The id of the AdapterView to which this list of RemoteViews will be applied.
     * @param widgetId The app widget id.
     * @param list The list of RemoteViews which will become the children of the AdapterView.
     * @param viewTypeCount The maximum number of unique layout id's used to construct the list of
     *      RemoteViews. This count cannot change during the life-cycle of a given widget, so this
     *      parameter should account for the maximum possible number of types that may appear in the
     *      See {@link android.widget.Adapter#getViewTypeCount()}.
     */
    public static void setRemoteAdapter(RemoteViews rv, Context ctx, int viewId, int widgetId,
            ArrayList<RemoteViews> list, int viewTypeCount) {
        IMPL.setRemoteAdapter(rv, ctx, viewId, widgetId, list, viewTypeCount);
    }

    static class BaseRemoteViewsCompatImpl implements RemoteViewsCompatImpl {
        @Override
        public void setRemoteAdapter(RemoteViews rv, Context ctx, int viewId, int widgetId,
                ArrayList<RemoteViews> list, int viewTypeCount) {

            // We embed the widget id and view id into the intent so that it can be used
            // as a unique key for the list of RemoteViews.
            final Intent intent = new Intent(ctx, RemoteViewsServiceCompat.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            intent.putExtra(EXTRA_VIEW_ID, viewId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViewsListFactory.addOrUpdateListFactory(ctx, intent, widgetId, viewId, list,
                    viewTypeCount);
            rv.setRemoteAdapter(widgetId, viewId, intent);
        }
    }

    static class KRemoteViewsCompatImpl implements RemoteViewsCompatImpl {
        @Override
        public void setRemoteAdapter(RemoteViews rv, Context ctx, int viewId, int widgetId,
                ArrayList<RemoteViews> list, int viewTypeCount) {
            RemoteViewsCompatK.setRemoteAdapter(rv, viewId, widgetId, list, viewTypeCount);
        }
    }
}
