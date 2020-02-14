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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.RestrictTo;
import androidx.fragment.app.DialogFragment;
import androidx.mediarouter.media.MediaRouteSelector;

/**
 * Media route chooser dialog fragment.
 * <p>
 * Creates a {@link MediaRouteChooserDialog}.  The application may subclass
 * this dialog fragment to customize the media route chooser dialog.
 * </p>
 */
public class MediaRouteChooserDialogFragment extends DialogFragment {
    private static final String ARGUMENT_SELECTOR = "selector";
    private boolean mUseDynamicGroup = false;
    private Dialog mDialog;
    private MediaRouteSelector mSelector;

    /**
     * Creates a media route chooser dialog fragment.
     * <p>
     * All subclasses of this class must also possess a default constructor.
     * </p>
     */
    public MediaRouteChooserDialogFragment() {
        setCancelable(true);
    }

    /**
     * Gets the media route selector for filtering the routes that the user can select.
     *
     * @return The selector, never null.
     */
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
     */
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
                    ((MediaRouteDynamicChooserDialog) mDialog).setRouteSelector(selector);
                } else {
                    ((MediaRouteChooserDialog) mDialog).setRouteSelector(selector);
                }
            }
        }
    }

    /**
     * Called when the device picker dialog is being created.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public MediaRouteDynamicChooserDialog onCreateDynamicChooserDialog(Context context) {
        return new MediaRouteDynamicChooserDialog(context);
    }

    /**
     * Called when the chooser dialog is being created.
     * <p>
     * Subclasses may override this method to customize the dialog.
     * </p>
     */
    public MediaRouteChooserDialog onCreateChooserDialog(
            Context context, Bundle savedInstanceState) {
        return new MediaRouteChooserDialog(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mUseDynamicGroup) {
            mDialog = onCreateDynamicChooserDialog(getContext());
            ((MediaRouteDynamicChooserDialog) mDialog).setRouteSelector(getRouteSelector());
        } else {
            mDialog = onCreateChooserDialog(getContext(), savedInstanceState);
            ((MediaRouteChooserDialog) mDialog).setRouteSelector(getRouteSelector());
        }
        return mDialog;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDialog == null) {
            return;
        }
        if (mUseDynamicGroup) {
            ((MediaRouteDynamicChooserDialog) mDialog).updateLayout();
        } else {
            ((MediaRouteChooserDialog) mDialog).updateLayout();
        }
    }
}
