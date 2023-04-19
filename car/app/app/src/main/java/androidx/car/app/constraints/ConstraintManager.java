/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.constraints;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.HostException;
import androidx.car.app.R;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.managers.Manager;
import androidx.car.app.utils.LogTags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Manages various constraints on the library as enforced by the host.
 *
 * <p>Depending on the host the app is connected to, there could be different various limits that
 * apply, such as the number of items that could be in a list in different templates. An app can
 * use this manager to query for these limits at runtime and react accordingly.
 */
@RequiresCarApi(2)
public class ConstraintManager implements Manager {
    /**
     * Represents the types of lists that apps can create.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @IntDef({CONTENT_LIMIT_TYPE_LIST, CONTENT_LIMIT_TYPE_GRID, CONTENT_LIMIT_TYPE_PLACE_LIST,
            CONTENT_LIMIT_TYPE_ROUTE_LIST, CONTENT_LIMIT_TYPE_PANE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentLimitType {
    }

    /**
     * Represents the limit for generic, uniform list contents.
     *
     * <p>The limit of this type should be no smaller than the values defined in
     * {@link androidx.car.app.R.integer#content_limit_list}, but the host may support a larger
     * limit.
     */
    public static final int CONTENT_LIMIT_TYPE_LIST = 0;

    /**
     * Represents the limit for contents to be shown in a grid format.
     *
     * <p>The limit of this type should be no smaller than the values defined in
     * {@link androidx.car.app.R.integer#content_limit_grid}, but the host may support a larger
     * limit.
     */
    public static final int CONTENT_LIMIT_TYPE_GRID = 1;

    /**
     * Represents the limit for list contents that are associated with points of interest.
     *
     * <p>The limit of this type should be no smaller than the values defined in
     * {@link androidx.car.app.R.integer#content_limit_place_list}, but the host may support a
     * larger limit.
     */
    public static final int CONTENT_LIMIT_TYPE_PLACE_LIST = 2;

    /**
     * Represents the limit for list contents that are associated with navigation routes.
     *
     * <p>The limit of this type should be no smaller than the values defined in
     * {@link androidx.car.app.R.integer#content_limit_route_list}, but the host may support a
     * larger limit.
     */
    public static final int CONTENT_LIMIT_TYPE_ROUTE_LIST = 3;

    /**
     * Represents the limit for contents to be shown in a pane format.
     *
     * <p>The limit of this type should be no smaller than the values defined in
     * {@link androidx.car.app.R.integer#content_limit_pane}, but the host may support a larger
     * limit.
     */
    public static final int CONTENT_LIMIT_TYPE_PANE = 4;

    @NonNull
    private final CarContext mCarContext;
    @NonNull
    private final HostDispatcher mHostDispatcher;

    /**
     * Requests for the limit associated with the {@code contentLimitType}.
     *
     * @throws HostException if the remote call fails
     */
    public int getContentLimit(@ContentLimitType int contentLimitType) {
        Integer limit = null;
        try {
            // TODO(b/185805900): consider caching these values if performance is a concern.
            limit = mHostDispatcher.dispatchForResult(
                    CarContext.CONSTRAINT_SERVICE,
                    "getContentLimit", (IConstraintHost host) -> {
                        return host.getContentLimit(contentLimitType);
                    }
            );
        } catch (RemoteException e) {
            // The host is dead, don't crash the app, just log.
            Log.w(LogTags.TAG, "Failed to retrieve list limit from the host, using defaults", e);
        }

        if (limit != null) {
            return limit;
        }

        // Returns default values as documented if host call failed.
        return mCarContext.getResources().getInteger(getResourceIdForContentType(contentLimitType));
    }


    /**
     * Determines if the hosts supports App Driven Refresh.
     * This enables applications to refresh lists content without being counted towards a step.
     *
     * If this function returns false the app should return a template that is of the same
     * type and contains the same main content as the previous template, the new template will
     * not be counted against the quota.
     *
     */
    @RequiresCarApi(6)
    public boolean isAppDrivenRefreshEnabled() {
        Boolean result;
        try {
            // TODO(b/185805900): consider caching these values if performance is a concern.
            result = mHostDispatcher.dispatchForResult(
                    CarContext.CONSTRAINT_SERVICE,
                    "isAppDrivenRefreshEnabled", IConstraintHost::isAppDrivenRefreshEnabled
            );
            return Boolean.TRUE.equals(result);
        } catch (RemoteException e) {
            // The host is dead, don't crash the app, just log.
            Log.w(LogTags.TAG,
                    "Failed to retrieve if the host supports appDriven Refresh, using defaults", e);
        }
        // Returns default values as documented if host call failed.
        return false;
    }

    @IntegerRes
    private int getResourceIdForContentType(@ContentLimitType int contentType) {
        switch (contentType) {
            case CONTENT_LIMIT_TYPE_GRID:
                return R.integer.content_limit_grid;
            case CONTENT_LIMIT_TYPE_PANE:
                return R.integer.content_limit_pane;
            case CONTENT_LIMIT_TYPE_PLACE_LIST:
                return R.integer.content_limit_place_list;
            case CONTENT_LIMIT_TYPE_ROUTE_LIST:
                return R.integer.content_limit_route_list;
            case CONTENT_LIMIT_TYPE_LIST:
            default:
                return R.integer.content_limit_list;
        }
    }

    /**
     * Creates an instance of {@link ConstraintManager}.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(LIBRARY)
    public static ConstraintManager create(@NonNull CarContext context,
            @NonNull HostDispatcher hostDispatcher) {
        return new ConstraintManager(requireNonNull(context), requireNonNull(hostDispatcher));
    }

    private ConstraintManager(CarContext context, HostDispatcher hostDispatcher) {
        mCarContext = context;
        mHostDispatcher = hostDispatcher;
    }
}
