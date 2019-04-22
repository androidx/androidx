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

package androidx.mediarouter.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.RestrictTo;
import androidx.fragment.app.DialogFragment;
import androidx.mediarouter.media.MediaRouteSelector;

/**
 * Media route controller dialog fragment.
 * <p>
 * Creates a {@link MediaRouteControllerDialog}.  The application may subclass
 * this dialog fragment to customize the media route controller dialog.
 * </p>
 */
public class MediaRouteControllerDialogFragment extends DialogFragment {
    private static final String ARGUMENT_SELECTOR = "selector";
    private boolean mUseDynamicGroup = false;
    private Dialog mDialog;
    private MediaRouteSelector mSelector;

    /**
     * Creates a media route controller dialog fragment.
     * <p>
     * All subclasses of this class must also possess a default constructor.
     * </p>
     */
    public MediaRouteControllerDialogFragment() {
        setCancelable(true);
    }

    /**
     * Gets the media route selector for filtering the routes that the user can select.
     *
     * @return The selector, never null.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public MediaRouteSelector getRouteSelector() {
        ensureRouteSelector();
        return mSelector;
    }

    private void ensureRouteSelector() {
        if (mSelector == null) {
            Bundle args = getArguments();
            if (args != null) {
                mSelector = MediaRouteSelector.fromBundle(args.getBundle(ARGUMENT_SELECTOR));
            }
            if (mSelector == null) {
                mSelector = MediaRouteSelector.EMPTY;
            }
        }
    }

    /**
     * Sets whether it creates dialog for dynamic group or not.
     * This method must be called before a dialog is created,
     * otherwise, this will throw {@link IllegalStateException}
     *
     * @param useDynamicGroup true if this should create the dialog for dynamic group
     */
    void setUseDynamicGroup(boolean useDynamicGroup) {
        if (mDialog != null) {
            throw new IllegalStateException("This must be called before creating dialog");
        }
        mUseDynamicGroup = useDynamicGroup;
    }

    /**
     * Sets the media route selector for filtering the routes that the user can select.
     * This method must be called before the fragment is added.
     *
     * @param selector The selector to set.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setRouteSelector(MediaRouteSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }

        ensureRouteSelector();
        if (!mSelector.equals(selector)) {
            mSelector = selector;

            Bundle args = getArguments();
            if (args == null) {
                args = new Bundle();
            }
            args.putBundle(ARGUMENT_SELECTOR, selector.asBundle());
            setArguments(args);

            if (mDialog != null) {
                if (mUseDynamicGroup) {
                    ((MediaRouteDynamicControllerDialog) mDialog).setRouteSelector(selector);
                }
            }
        }
    }

    /**
     * Called when the cast dialog is being created.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public MediaRouteDynamicControllerDialog onCreateDynamicControllerDialog(Context context) {
        return new MediaRouteDynamicControllerDialog(context);
    }

    /**
     * Called when the controller dialog is being created.
     * <p>
     * Subclasses may override this method to customize the dialog.
     * </p>
     */
    public MediaRouteControllerDialog onCreateControllerDialog(
            Context context, Bundle savedInstanceState) {
        return new MediaRouteControllerDialog(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mUseDynamicGroup) {
            mDialog = onCreateDynamicControllerDialog(getContext());
            ((MediaRouteDynamicControllerDialog) mDialog).setRouteSelector(mSelector);
        } else {
            mDialog = onCreateControllerDialog(getContext(), savedInstanceState);
        }
        return mDialog;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mDialog != null && !mUseDynamicGroup) {
            ((MediaRouteControllerDialog) mDialog).clearGroupListAnimation(false);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDialog != null) {
            if (mUseDynamicGroup) {
                ((MediaRouteDynamicControllerDialog) mDialog).updateLayout();
            } else {
                ((MediaRouteControllerDialog) mDialog).updateLayout();
            }
        }
    }
}
