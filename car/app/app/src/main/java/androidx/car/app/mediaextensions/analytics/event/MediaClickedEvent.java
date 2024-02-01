/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.car.app.mediaextensions.analytics.event;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_MEDIA_ID;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_VIEW_COMPONENT;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEventsUtil.viewComponentToString;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/** Describes a media clicked event. */
public class MediaClickedEvent extends AnalyticsEvent {

    private final String mMediaId;
    @ViewComponent
    private final int mViewComponent;

    @RestrictTo(LIBRARY)
    public MediaClickedEvent(@NonNull Bundle eventBundle) {
        super(eventBundle, EVENT_TYPE_MEDIA_CLICKED_EVENT);
        mMediaId = eventBundle.getString(ANALYTICS_EVENT_DATA_KEY_MEDIA_ID);
        mViewComponent = eventBundle.getInt(ANALYTICS_EVENT_DATA_KEY_VIEW_COMPONENT);
    }

    @Nullable
    public String getMediaId() {
        return mMediaId;
    }

    @ViewComponent
    public int getViewComponent() {
        return mViewComponent;
    }

    @Override
    @NonNull
    public String toString() {
        final StringBuilder sb = new StringBuilder("MediaClickedEvent{");
        sb.append("mMediaId='").append(mMediaId).append('\'');
        sb.append(", mViewComponent=").append(viewComponentToString(mViewComponent));
        sb.append('}');
        return sb.toString();
    }
}
