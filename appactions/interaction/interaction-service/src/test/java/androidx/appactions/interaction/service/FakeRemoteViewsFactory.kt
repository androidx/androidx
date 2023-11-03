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

package androidx.appactions.interaction.service

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.appactions.interaction.service.test.R

/** Fake implementation of a RemoteViewsFactory. */
internal class FakeRemoteViewsFactory : RemoteViewsFactory {

    private val collectionCount = 3
    private val viewTypeCount = 1
    private val hasStableIds = true
    private val packageName = "androidx.appactions.interaction.service"

    override fun onCreate() {
        // do nothing
    }

    override fun onDataSetChanged() {
        // do nothing
    }

    override fun onDestroy() {
        // do nothing
    }

    override fun getCount(): Int {
        return collectionCount
    }

    override fun getViewAt(i: Int): RemoteViews {
        // here we use the position as the view Id, which is the id given to the root of the
        // RemoteViews. This is just so that we can use getViewId in test to verify that RemoteViews
        // received is correct.
        return if (VERSION.SDK_INT >= VERSION_CODES.S) {
            RemoteViews(packageName, R.layout.remote_view, i)
        } else RemoteViews(packageName, R.layout.remote_view)
    }

    override fun getLoadingView(): RemoteViews {
        return if (VERSION.SDK_INT >= VERSION_CODES.S) {
            RemoteViews(packageName, R.layout.loading_view)
        } else RemoteViews(packageName, R.layout.loading_view)
    }

    override fun getViewTypeCount(): Int {
        return viewTypeCount
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun hasStableIds(): Boolean {
        return hasStableIds
    }
}
