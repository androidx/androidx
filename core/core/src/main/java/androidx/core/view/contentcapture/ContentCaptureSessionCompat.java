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

package androidx.core.view.contentcapture;

import static android.os.Build.VERSION.SDK_INT;

import android.view.View;
import android.view.ViewStructure;
import android.view.autofill.AutofillId;
import android.view.contentcapture.ContentCaptureSession;

import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewStructureCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Helper for accessing features in {@link ContentCaptureSession}.
 */
public class ContentCaptureSessionCompat {

    private static final String KEY_VIEW_TREE_APPEARING = "TREAT_AS_VIEW_TREE_APPEARING";
    private static final String KEY_VIEW_TREE_APPEARED = "TREAT_AS_VIEW_TREE_APPEARED";
    // Only guaranteed to be non-null on SDK_INT >= 29.
    private final Object mWrappedObj;
    private final View mView;

    /**
     * Provides a backward-compatible wrapper for {@link ContentCaptureSession}.
     * <p>
     * This method is not supported on devices running SDK < 29 since the platform
     * class will not be available.
     *
     * @param contentCaptureSession platform class to wrap
     * @param host view hosting the session.
     * @return wrapped class
     */
    @RequiresApi(29)
    public static @NonNull ContentCaptureSessionCompat toContentCaptureSessionCompat(
            @NonNull ContentCaptureSession contentCaptureSession, @NonNull View host) {
        return new ContentCaptureSessionCompat(contentCaptureSession, host);
    }

    /**
     * Provides the {@link ContentCaptureSession} represented by this object.
     * <p>
     * This method is not supported on devices running SDK < 29 since the platform
     * class will not be available.
     *
     * @return platform class object
     * @see ContentCaptureSessionCompat#toContentCaptureSessionCompat(ContentCaptureSession, View)
     */
    @RequiresApi(29)
    public @NonNull ContentCaptureSession toContentCaptureSession() {
        return (ContentCaptureSession) mWrappedObj;
    }

    /**
     * Creates a {@link ContentCaptureSessionCompat} instance.
     *
     * @param contentCaptureSession {@link ContentCaptureSession} for this host View.
     * @param host view hosting the session.
     */
    @RequiresApi(29)
    private ContentCaptureSessionCompat(@NonNull ContentCaptureSession contentCaptureSession,
            @NonNull View host) {
        this.mWrappedObj = contentCaptureSession;
        this.mView = host;
    }

    /**
     * Creates a new {@link AutofillId} for a virtual child, so it can be used to uniquely identify
     * the children in the session.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 29 and above, this method matches platform behavior.
     * <li>SDK 28 and below, this method returns null.
     * </ul>
     *
     * @param virtualChildId id of the virtual child, relative to the parent.
     *
     * @return {@link AutofillId} for the virtual child
     */
    public @Nullable AutofillId newAutofillId(long virtualChildId) {
        if (SDK_INT >= 29) {
            return Api29Impl.newAutofillId(
                    (ContentCaptureSession) mWrappedObj,
                    Objects.requireNonNull(ViewCompat.getAutofillId(mView)).toAutofillId(),
                    virtualChildId);
        }
        return null;
    }

    /**
     * Creates a {@link ViewStructure} for a "virtual" view, so it can be passed to
     * {@link #notifyViewsAppeared} by the view managing the virtual view hierarchy.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 29 and above, this method matches platform behavior.
     * <li>SDK 28 and below, this method returns null.
     * </ul>
     *
     * @param parentId id of the virtual view parent (it can be obtained by calling
     * {@link ViewStructure#getAutofillId()} on the parent).
     * @param virtualId id of the virtual child, relative to the parent.
     *
     * @return a new {@link ViewStructure} that can be used for Content Capture purposes.
     */
    public @Nullable ViewStructureCompat newVirtualViewStructure(
            @NonNull AutofillId parentId, long virtualId) {
        if (SDK_INT >= 29) {
            return ViewStructureCompat.toViewStructureCompat(
                    Api29Impl.newVirtualViewStructure(
                            (ContentCaptureSession) mWrappedObj, parentId, virtualId));
        }
        return null;
    }

    /**
     * Notifies the Content Capture Service that a list of nodes has appeared in the view structure.
     *
     * <p>Typically called manually by views that handle their own virtual view hierarchy.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 34 and above, this method matches platform behavior.
     * <li>SDK 29 through 33, this method is a best-effort to match platform behavior, by
     * wrapping the virtual children with a pair of special view appeared events.
     * <li>SDK 28 and below, this method does nothing.
     *
     * @param appearedNodes nodes that have appeared. Each element represents a view node that has
     * been added to the view structure. The order of the elements is important, which should be
     * preserved as the attached order of when the node is attached to the virtual view hierarchy.
     */
    public void notifyViewsAppeared(@NonNull List<ViewStructure> appearedNodes) {
        if (SDK_INT >= 34) {
            Api34Impl.notifyViewsAppeared((ContentCaptureSession) mWrappedObj, appearedNodes);
        } else if (SDK_INT >= 29) {
            ViewStructure treeAppearing = Api29Impl.newViewStructure(
                    (ContentCaptureSession) mWrappedObj, mView);
            treeAppearing.getExtras().putBoolean(KEY_VIEW_TREE_APPEARING, true);
            Api29Impl.notifyViewAppeared((ContentCaptureSession) mWrappedObj, treeAppearing);

            for (int i = 0; i < appearedNodes.size(); i++) {
                Api29Impl.notifyViewAppeared(
                        (ContentCaptureSession) mWrappedObj, appearedNodes.get(i));
            }

            ViewStructure treeAppeared = Api29Impl.newViewStructure(
                    (ContentCaptureSession) mWrappedObj, mView);
            treeAppeared.getExtras().putBoolean(KEY_VIEW_TREE_APPEARED, true);
            Api29Impl.notifyViewAppeared((ContentCaptureSession) mWrappedObj, treeAppeared);
        }
    }

    /**
     * Notifies the Content Capture Service that many nodes has been removed from a virtual view
     * structure.
     *
     * <p>Should only be called by views that handle their own virtual view hierarchy.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 34 and above, this method matches platform behavior.
     * <li>SDK 29 through 33, this method is a best-effort to match platform behavior, by
     * wrapping the virtual children with a pair of special view appeared events.
     * <li>SDK 28 and below, this method does nothing.
     * </ul>
     *
     * @param virtualIds ids of the virtual children.
     */
    public void notifyViewsDisappeared(long @NonNull [] virtualIds) {
        if (SDK_INT >= 34) {
            Api29Impl.notifyViewsDisappeared(
                    (ContentCaptureSession) mWrappedObj,
                    Objects.requireNonNull(ViewCompat.getAutofillId(mView)).toAutofillId(),
                    virtualIds);
        } else if (SDK_INT >= 29) {
            ViewStructure treeAppearing = Api29Impl.newViewStructure(
                    (ContentCaptureSession) mWrappedObj, mView);
            treeAppearing.getExtras().putBoolean(KEY_VIEW_TREE_APPEARING, true);
            Api29Impl.notifyViewAppeared((ContentCaptureSession) mWrappedObj, treeAppearing);

            Api29Impl.notifyViewsDisappeared(
                    (ContentCaptureSession) mWrappedObj,
                    Objects.requireNonNull(ViewCompat.getAutofillId(mView)).toAutofillId(),
                    virtualIds);

            ViewStructure treeAppeared = Api29Impl.newViewStructure(
                    (ContentCaptureSession) mWrappedObj, mView);
            treeAppeared.getExtras().putBoolean(KEY_VIEW_TREE_APPEARED, true);
            Api29Impl.notifyViewAppeared((ContentCaptureSession) mWrappedObj, treeAppeared);
        }
    }

    /**
     * Notifies the Intelligence Service that the value of a text node has been changed.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 29 and above, this method matches platform behavior.
     * <li>SDK 28 and below, this method does nothing.
     * </ul>
     *
     * @param id of the node.
     * @param text new text.
     */
    public void notifyViewTextChanged(@NonNull AutofillId id, @Nullable CharSequence text) {
        if (SDK_INT >= 29) {
            Api29Impl.notifyViewTextChanged((ContentCaptureSession) mWrappedObj, id, text);
        }
    }

    @RequiresApi(34)
    private static class Api34Impl {
        private Api34Impl() {
            // This class is not instantiable.
        }

        static void notifyViewsAppeared(
                ContentCaptureSession contentCaptureSession, List<ViewStructure> appearedNodes) {
            contentCaptureSession.notifyViewsAppeared(appearedNodes);
        }
    }
    @RequiresApi(29)
    private static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        static void notifyViewsDisappeared(
                ContentCaptureSession contentCaptureSession, AutofillId hostId, long[] virtualIds) {
            contentCaptureSession.notifyViewsDisappeared(hostId, virtualIds);
        }

        static void notifyViewAppeared(
                ContentCaptureSession contentCaptureSession, ViewStructure node) {
            contentCaptureSession.notifyViewAppeared(node);
        }
        static ViewStructure newViewStructure(
                ContentCaptureSession contentCaptureSession, View view) {
            return contentCaptureSession.newViewStructure(view);
        }

        static ViewStructure newVirtualViewStructure(ContentCaptureSession contentCaptureSession,
                AutofillId parentId, long virtualId) {
            return contentCaptureSession.newVirtualViewStructure(parentId, virtualId);
        }


        static AutofillId newAutofillId(ContentCaptureSession contentCaptureSession,
                AutofillId hostId, long virtualChildId) {
            return contentCaptureSession.newAutofillId(hostId, virtualChildId);
        }

        public static void notifyViewTextChanged(ContentCaptureSession contentCaptureSession,
                AutofillId id, CharSequence charSequence) {
            contentCaptureSession.notifyViewTextChanged(id, charSequence);

        }
    }
}
