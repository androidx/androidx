/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.model;

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_TABS;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;

import androidx.car.app.Screen;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.TabsConstraints;
import androidx.car.app.utils.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A template representing a list of tabs and contents for the active tab.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in {@link Screen#onGetTemplate()}, this
 * template is considered a refresh of a previous one if:
 *
 * <ul>
 *   <li>The previous template is in a loading state (see {@link TabTemplate.Builder#setLoading}}
 *   , or
 *   <li>The {@link Tab}s structure between the templates has not changed and if the new
 *   template has the same active tab then the contents of that tab hasn't changed. This means that
 *   if the previous template has multiple {@link Tab}s, the new template must have the same
 *   number of tabs with the same title and icon.
 * </ul>
 */
@CarProtocol
@RequiresCarApi(6)
@KeepFields
public class TabTemplate implements Template {

    /** A listener for tab selection. */
    public interface TabCallback {
        /**
         * Notifies the selected {@code Tab} has changed.
         *
         * <p>The host invokes this callback as the user selects a tab.
         *
         * @param tabContentId the content ID of the selected tab.
         */
        default void onTabSelected(@NonNull String tabContentId) {
        }
    }

    private final boolean mIsLoading;
    private final @Nullable TabCallbackDelegate mTabCallbackDelegate;
    private final @Nullable Action mHeaderAction;
    private final @Nullable TabContents mTabContents;
    private final @Nullable List<Tab> mTabs;
    private final @Nullable String mActiveTabContentId;

    /**
     * Returns the {@link Action} that is set to be displayed in the header of the template, or
     * {@code null} if not set.
     *
     * @see TabTemplate.Builder#setHeaderAction(Action)
     */
    public @NonNull Action getHeaderAction() {
        return requireNonNull(mHeaderAction);
    }

    /**
     * Returns whether the template is loading.
     *
     * @see TabTemplate.Builder#setLoading(boolean)
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Returns the list of {@link Tab}s in the template.
     */
    public @NonNull List<Tab> getTabs() {
        return CollectionUtils.emptyIfNull(mTabs);
    }

    /**
     * Returns the {@link TabContents} for the currently active tab.
     */
    public @NonNull TabContents getTabContents() {
        return requireNonNull(mTabContents);
    }

    /**
     * Returns the {@link TabCallbackDelegate} for tab related callbacks.
     */
    public @NonNull TabCallbackDelegate getTabCallbackDelegate() {
        return requireNonNull(mTabCallbackDelegate);
    }

    /**
     * Returns the {@link Tab#getContentId()} for the active tab.
     */
    public @NonNull String getActiveTabContentId() {
        return requireNonNull(mActiveTabContentId);
    }

    @Override
    public @NonNull String toString() {
        return "TabTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsLoading, mHeaderAction, mTabs, mTabContents, mActiveTabContentId);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TabTemplate)) {
            return false;
        }
        TabTemplate otherTemplate = (TabTemplate) other;

        return mIsLoading == otherTemplate.mIsLoading
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mTabs, otherTemplate.mTabs)
                && Objects.equals(mTabContents, otherTemplate.mTabContents)
                && Objects.equals(mActiveTabContentId, otherTemplate.getActiveTabContentId());
    }

    TabTemplate(TabTemplate.Builder builder) {
        mIsLoading = builder.mIsLoading;
        mHeaderAction = builder.mHeaderAction;
        mTabs = CollectionUtils.unmodifiableCopy(builder.mTabs);
        mTabContents = builder.mTabContents;
        mTabCallbackDelegate = builder.mTabCallbackDelegate;
        mActiveTabContentId = builder.mActiveTabContentId;
    }

    /** Constructs an empty instance, used by serialization code. */
    private TabTemplate() {
        mIsLoading = false;
        mHeaderAction = null;
        mTabs = Collections.emptyList();
        mTabContents = null;
        mTabCallbackDelegate = null;
        mActiveTabContentId = null;
    }

    /** A builder of {@link TabTemplate}. */
    public static final class Builder {
        final @NonNull TabCallbackDelegate mTabCallbackDelegate;

        boolean mIsLoading;

        @Nullable Action mHeaderAction;

        final List<Tab> mTabs;
        @Nullable TabContents mTabContents;

        @Nullable String mActiveTabContentId;

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI will display a loading indicator where the content
         * would be otherwise. The caller is expected to call {@link
         * androidx.car.app.Screen#invalidate()} and send the new template content
         * to the host once the data is ready.
         *
         * <p>If set to {@code false}, the UI will display the contents of the template.
         */
        public TabTemplate.@NonNull Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed in the header of the template, or
         * {@code null} to not display an action.
         *
         * <p>Unless set with this method, the template will not have a header action.
         *
         * <h4>Requirements</h4>
         *
         * This template only supports {@link Action#APP_ICON} as a header {@link Action}.
         *
         * @throws IllegalArgumentException if {@code headerAction} does not meet the template's
         *                                  requirements
         * @throws NullPointerException     if {@code headerAction} is {@code null}
         */
        public TabTemplate.@NonNull Builder setHeaderAction(@NonNull Action headerAction) {
            ACTIONS_CONSTRAINTS_TABS.validateOrThrow(
                    Collections.singletonList(requireNonNull(headerAction)));
            mHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets the {@link TabContents} to show in the template. Note that only certain templates
         * may be used as content. See {@link TabContents.Builder#Builder(Template)} for more
         * details.
         *
         * @throws NullPointerException if {@code tabContents} is null
         */
        public TabTemplate.@NonNull Builder setTabContents(@NonNull TabContents tabContents) {
            mTabContents = requireNonNull(tabContents);
            return this;
        }

        /**
         * Stores the given {@code contentId} as the "active tab" to show on the screen. The given
         * ID must match a tab that was added by {@link #addTab(Tab)}.
         */
        public TabTemplate.@NonNull Builder setActiveTabContentId(@NonNull String contentId) {
            if (requireNonNull(contentId).isEmpty()) {
                throw new IllegalArgumentException("The content ID cannot be null or empty");
            }
            mActiveTabContentId = contentId;
            return this;
        }

        /**
         * Adds an {@link Tab} to display in the template.
         *
         * @throws NullPointerException if {@code tab} is {@code null}
         */
        public TabTemplate.@NonNull Builder addTab(@NonNull Tab tab) {
            requireNonNull(tab);
            mTabs.add(tab);
            return this;
        }

        /**
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * The number of {@link Tab}s provided in the template should be between 2 and 4, with only
         * one tab marked as active.
         *
         * <p>A header {@link Action} of type TYPE_APP_ICON is required.
         *
         * @throws IllegalStateException    if the template is in a loading state but there are
         *                                  tabs added or vice versa
         * @throws IllegalArgumentException if the added {@link Tab}(s) or header {@link Action}
         *                                  does not meet the template's requirements
         */
        public @NonNull TabTemplate build() {
            boolean hasTabs = mTabContents != null && !mTabs.isEmpty();
            if (mIsLoading && hasTabs) {
                throw new IllegalStateException(
                        "Template is in a loading state but tabs are added");
            }

            if (!mIsLoading && !hasTabs) {
                throw new IllegalStateException(
                        "Template is not in a loading state but does not contain tabs or tab "
                                + "contents");
            }

            if (hasTabs && mActiveTabContentId == null) {
                throw new IllegalStateException(
                        "Template requires setting content ID for the active tab when not in "
                                + "Loading state");
            }

            if (hasTabs && mActiveTabContentId != null) {
                TabsConstraints.DEFAULT.validateOrThrow(mTabs, mActiveTabContentId);
            }

            if (!mIsLoading && mHeaderAction == null) {
                throw new IllegalArgumentException(
                        "Template requires a Header Action of TYPE_APP_ICON when not in Loading "
                                + "state"
                );
            }

            return new TabTemplate(this);
        }

        /**
         * Creates a {@link TabTemplate.Builder} instance using the given {@link TabCallback}.
         *
         * <p>Note that the callback relates to UI events and will be executed on the main thread
         * using {@link Looper#getMainLooper()}.
         *
         * @param callback the callback to be invoked when the user selects a new tab in the header
         */
        @SuppressLint("ExecutorRegistration")
        public Builder(@NonNull TabCallback callback) {
            mTabCallbackDelegate = TabCallbackDelegateImpl.create(requireNonNull(callback));
            mTabs = new ArrayList<>();
        }

        /** Creates a new {@link Builder}, populated from the input {@link TabTemplate} */
        public Builder(@NonNull TabTemplate tabTemplate) {
            mIsLoading = tabTemplate.isLoading();
            mHeaderAction = tabTemplate.getHeaderAction();
            mTabs = new ArrayList<>(tabTemplate.getTabs());
            mTabContents = tabTemplate.getTabContents();
            mTabCallbackDelegate = tabTemplate.getTabCallbackDelegate();
            mActiveTabContentId = tabTemplate.getActiveTabContentId();
        }
    }
}
