/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.webkit;

import androidx.annotation.AnyThread;
import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.GoogleAccountsEventBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;

/**
 * Event information for Google account changes.
 *
 * @see WebViewCompat#setGoogleAccountsCallback
 */
@AnyThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@WebViewCompat.ExperimentalGoogleAccountsCallback
public final class GoogleAccountsEvent {

    /** Any other action type. */
    public static final int ACTION_TYPE_OTHER = 0;

    /** The user attempted to sign out. */
    public static final int ACTION_TYPE_SIGN_OUT = 1;

    /** The user attempted to create an account. */
    public static final int ACTION_TYPE_CREATE_ACCOUNT = 2;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = {
            ACTION_TYPE_OTHER,
            ACTION_TYPE_SIGN_OUT,
            ACTION_TYPE_CREATE_ACCOUNT,
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface ActionType {
    }

    private final @NonNull GoogleAccountsEventBoundaryInterface mBoundaryInterface;

    /**
     * Factory method that returns the {@link GoogleAccountsEvent} associated with the given
     * {@link InvocationHandler}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static @NonNull GoogleAccountsEvent forInvocationHandler(
            @NonNull InvocationHandler invocationHandler) {
        GoogleAccountsEventBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        GoogleAccountsEventBoundaryInterface.class, invocationHandler);
        assert boundaryInterface != null;
        return new GoogleAccountsEvent(boundaryInterface);
    }

    private GoogleAccountsEvent(
            @NonNull GoogleAccountsEventBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Get the action type attempted by the user.
     *
     * @return One of {@link #ACTION_TYPE_OTHER}, {@link #ACTION_TYPE_SIGN_OUT}, or
     *     {@link #ACTION_TYPE_CREATE_ACCOUNT}.
     */
    public @ActionType int getActionType() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.GOOGLE_ACCOUNTS_CALLBACK;
        if (!feature.isSupportedByWebView()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
        switch (mBoundaryInterface.getActionType()) {
            case GoogleAccountsEventBoundaryInterface.ACTION_TYPE_SIGN_OUT:
                return ACTION_TYPE_SIGN_OUT;
            case GoogleAccountsEventBoundaryInterface.ACTION_TYPE_CREATE_ACCOUNT:
                return ACTION_TYPE_CREATE_ACCOUNT;
            case GoogleAccountsEventBoundaryInterface.ACTION_TYPE_OTHER:
            default:
                return ACTION_TYPE_OTHER;
        }
    }

    /**
     * The URL to redirect to after the account action.
     *
     * @return The continue URL.
     */
    public @NonNull String getContinueUrl() {
        return mBoundaryInterface.getContinueUrl();
    }

    /**
     * Whether the account change event was triggered from the outermost main frame.
     *
     * @return {@code true} if the event occurred in the outermost main frame, {@code false}
     *     otherwise (e.g. in an iframe).
     */
    public boolean isOutermostMainFrame() {
        return mBoundaryInterface.isOutermostMainFrame();
    }

    /**
     * Whether the account change event was triggered by a navigation request.
     *
     * @return {@code true} if the event was triggered by a navigation request, {@code false}
     *     otherwise (e.g. a resource request).
     */
    public boolean isNavigationRequest() {
        return mBoundaryInterface.isNavigationRequest();
    }
}
