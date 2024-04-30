/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License = 0 Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing = 0 software
 * distributed under the License is distributed on an "AS IS" BASIS = 0
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND = 0 either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.protolayout.renderer.common;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Shared constants. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Constants {

    private Constants() {}

    /** The reason why an update was requested. */
    @IntDef({
        UPDATE_REQUEST_REASON_UNKNOWN,
        UPDATE_REQUEST_REASON_SYSUI_CAROUSEL,
        UPDATE_REQUEST_REASON_FRESHNESS,
        UPDATE_REQUEST_REASON_USER_INTERACTION,
        UPDATE_REQUEST_REASON_UPDATE_REQUESTER,
        UPDATE_REQUEST_REASON_CACHE_INVALIDATION,
        UPDATE_REQUEST_REASON_RETRY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UpdateRequestReason {}

    /** Unknown reason. */
    public static final int UPDATE_REQUEST_REASON_UNKNOWN = 0;

    /** Update triggered by SysUI Carousel. */
    public static final int UPDATE_REQUEST_REASON_SYSUI_CAROUSEL = 1;

    /** Update triggered by freshness. */
    public static final int UPDATE_REQUEST_REASON_FRESHNESS = 2;

    /** Update triggered by user interaction (e.g. clicking on the tile). */
    public static final int UPDATE_REQUEST_REASON_USER_INTERACTION = 3;

    /** Update triggered using update requester. */
    public static final int UPDATE_REQUEST_REASON_UPDATE_REQUESTER = 4;

    /** Update triggered due to clearing the cache. */
    public static final int UPDATE_REQUEST_REASON_CACHE_INVALIDATION = 5;

    /** Update triggered by retry policy. */
    public static final int UPDATE_REQUEST_REASON_RETRY = 6;
}
