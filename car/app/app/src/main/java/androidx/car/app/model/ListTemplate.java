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

package androidx.car.app.model;

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;
import static androidx.car.app.model.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_FULL_LIST;

import static java.util.Objects.requireNonNull;

import androidx.annotation.OptIn;
import androidx.car.app.Screen;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.messaging.model.CarMessage;
import androidx.car.app.messaging.model.ConversationItem;
import androidx.car.app.model.constraints.ActionsConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;
import androidx.car.app.utils.CollectionUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A template representing a list of items.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in {@link Screen#onGetTemplate()}, this
 * template is considered a refresh of a previous one if:
 *
 * <ul>
 *   <li>The previous template is in a loading state (see {@link Builder#setLoading}}, or
 *   <li>The template title has not changed, and the {@link ItemList} structure between the
 *       templates have not changed. This means that if the previous template has multiple
 *       {@link ItemList} sections, the new template must have the same number of sections with
 *       the same headers. Further, the number of rows and the title (not counting spans) of
 *       each row must not have changed.
 *   <li>For rows that contain a {@link Toggle}, updates to the title are also allowed if the
 *       toggle state has changed between the previous and new templates.
 * </ul>
 */
@CarProtocol
@KeepFields
public final class ListTemplate implements Template {
    // These restrictions are imposed on ListTemplate to avoid exceeding the Android binder
    // transaction limit of 1MB.
    static final int MAX_ALLOWED_ITEMS = 100;
    static final int MAX_MESSAGES_PER_CONVERSATION = 10;

    private final boolean mIsLoading;
    /**
     * @deprecated use {@link Header.Builder#setTitle(CarText)}; mHeader replaces the need
     * for this field.
     */
    @Deprecated
    private final @Nullable CarText mTitle;
    /**
     * @deprecated use {@link Header.Builder#setStartHeaderAction(Action)}; mHeader replaces the
     * need for this field.
     */
    @Deprecated
    private final @Nullable Action mHeaderAction;
    private final @Nullable ItemList mSingleList;
    private final List<SectionedItemList> mSectionedLists;
    /**
     * @deprecated use {@link Header.Builder#addEndHeaderAction(Action)} for each action; mHeader
     * replaces the need for this field.
     */
    @Deprecated
    private final @Nullable ActionStrip mActionStrip;

    private final List<Action> mActions;

    /**
     * Represents a Header object to set the startHeaderAction, the title and the endHeaderActions
     *
     * @see ListTemplate.Builder#setHeader(Header)
     */
    @RequiresCarApi(7)
    private final @Nullable Header mHeader;


    /**
     * Returns the title of the template or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     *
     * @deprecated use {@link Header#getTitle()} instead.
     */
    @Deprecated
    public @Nullable CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the {@link Action} that is set to be displayed in the header of the template, or
     * {@code null} if not set.
     *
     * @see Builder#setHeaderAction(Action)
     *
     * @deprecated use {@link Header#getStartHeaderAction()} instead.
     */
    @Deprecated
    public @Nullable Action getHeaderAction() {
        return mHeaderAction;
    }

    /**
     * Returns the {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @see Builder#setActionStrip(ActionStrip)
     *
     * @deprecated use {@link Header#getEndHeaderActions()} instead.
     */
    @Deprecated
    public @Nullable ActionStrip getActionStrip() {
        return mActionStrip;
    }

    /**
     * Returns whether the template is loading.
     *
     * @see Builder#setLoading(boolean)
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Returns the {@link ItemList} instance containing the list of items to display or {@code
     * null} if one hasn't been set.
     *
     * @see Builder#setSingleList(ItemList)
     */
    public @Nullable ItemList getSingleList() {
        return mSingleList;
    }

    /**
     * Returns the list of {@link SectionedItemList} instances to be displayed in the template.
     *
     * @see Builder#addSectionedList(SectionedItemList)
     */
    public @NonNull List<SectionedItemList> getSectionedLists() {
        return CollectionUtils.emptyIfNull(mSectionedLists);
    }

    /**
     * Returns the list of additional actions.
     *
     * @see ListTemplate.Builder#addAction(Action)
     */
    @RequiresCarApi(6)
    public @NonNull List<Action> getActions() {
        return mActions;
    }

    /**
     * Returns the {@link Header} to display in this template.
     *
     * <p>This method was introduced in API 7, but is backwards compatible even if the client is
     * using API 6 or below. </p>
     *
     * @see ListTemplate.Builder#setHeader(Header)
     */
    public @Nullable Header getHeader() {
        if (mHeader != null) {
            return mHeader;
        }
        if (mTitle == null && mHeaderAction == null && mActionStrip == null) {
            return null;
        }
        Header.Builder headerBuilder = new Header.Builder();
        if (mTitle != null) {
            headerBuilder.setTitle(mTitle);
        }
        if (mHeaderAction != null) {
            headerBuilder.setStartHeaderAction(mHeaderAction);
        }
        if (mActionStrip != null) {
            for (Action action: mActionStrip.getActions()) {
                headerBuilder.addEndHeaderAction(action);
            }
        }
        return headerBuilder.build();
    }

    @Override
    public @NonNull String toString() {
        return "ListTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsLoading, mTitle, mHeaderAction, mSingleList, mSectionedLists,
                mActionStrip, mHeader);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ListTemplate)) {
            return false;
        }
        ListTemplate otherTemplate = (ListTemplate) other;

        return mIsLoading == otherTemplate.mIsLoading
                && Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mSingleList, otherTemplate.mSingleList)
                && Objects.equals(mSectionedLists, otherTemplate.mSectionedLists)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip)
                && Objects.equals(mActions, otherTemplate.mActions)
                && Objects.equals(mHeader, otherTemplate.mHeader);
    }

    ListTemplate(Builder builder) {
        mIsLoading = builder.mIsLoading;
        mTitle = builder.mTitle;
        mHeaderAction = builder.mHeaderAction;
        mSingleList = builder.mSingleList;
        mSectionedLists = CollectionUtils.unmodifiableCopy(builder.mSectionedLists);
        mActionStrip = builder.mActionStrip;
        mActions = CollectionUtils.unmodifiableCopy(builder.mActions);
        mHeader = builder.mHeader;
    }

    /** Constructs an empty instance, used by serialization code. */
    private ListTemplate() {
        mIsLoading = false;
        mTitle = null;
        mHeaderAction = null;
        mSingleList = null;
        mSectionedLists = Collections.emptyList();
        mActionStrip = null;
        mActions = Collections.emptyList();
        mHeader = null;
    }

    /**
     * Creates and returns a new {@link Builder} initialized with this {@link ListTemplate}'s data.
     */
    @ExperimentalCarApi
    public ListTemplate.@NonNull Builder toBuilder() {
        return new ListTemplate.Builder(this);
    }

    /** A builder of {@link ListTemplate}. */
    public static final class Builder {
        boolean mIsLoading;
        @Nullable ItemList mSingleList;
        final List<SectionedItemList> mSectionedLists;
        @Nullable CarText mTitle;
        @Nullable Action mHeaderAction;
        @Nullable ActionStrip mActionStrip;
        boolean mHasSelectableList;

        final List<Action> mActions;
        @Nullable Header mHeader;

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI will display a loading indicator where the list content
         * would be otherwise. The caller is expected to call {@link
         * androidx.car.app.Screen#invalidate()} and send the new template content
         * to the host once the data is ready.
         *
         * <p>If set to {@code false}, the UI will display the contents of the {@link ItemList}
         * instance(s) added via {@link #setSingleList} or {@link #addSectionedList}.
         */
        public @NonNull Builder setLoading(boolean isLoading) {
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
         * This template only supports either one of {@link Action#APP_ICON} and
         * {@link Action#BACK} as a header {@link Action}.
         *
         * @throws IllegalArgumentException if {@code headerAction} does not meet the template's
         *                                  requirements
         * @throws NullPointerException     if {@code headerAction} is {@code null}
         *
         * @deprecated Use {@link Header.Builder#setStartHeaderAction(Action)}
         */
        @Deprecated
        public @NonNull Builder setHeaderAction(@NonNull Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    Collections.singletonList(requireNonNull(headerAction)));
            mHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets the title of the template.
         *
         * <p>Unless set with this method, the template will not have a title.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is null
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         *
         * @deprecated Use {@link Header.Builder#setTitle(CarText)}
         */
        @Deprecated
        public @NonNull Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mTitle);
            return this;
        }

        /**
         * Sets a single {@link ItemList} to show in the template.
         *
         * <p>Note that this list cannot be mixed with others added via {@link #addSectionedList}
         * . If multiple lists were previously added, they will be cleared.
         *
         * @throws NullPointerException if {@code list} is null
         */
        public @NonNull Builder setSingleList(@NonNull ItemList list) {
            mSingleList = requireNonNull(list);
            mSectionedLists.clear();
            mHasSelectableList = false;
            return this;
        }

        /**
         * Adds an {@link SectionedItemList} to display in the template.
         *
         * <p>Use this method to add multiple lists to the template. Each
         * {@link SectionedItemList} will be grouped under its header. These lists cannot be
         * mixed with an {@link ItemList} added via {@link #setSingleList}. If a single list was
         * previously added, it will be cleared.
         *
         * <p>If the added {@link SectionedItemList} contains a
         * {@link ItemList.OnSelectedListener}, then it cannot be added alongside other
         * {@link SectionedItemList}(s).
         *
         * @throws NullPointerException     if {@code list} or {@code header} is {@code null}
         * @throws IllegalArgumentException if {@code list} is empty, if {@code list}'s {@link
         *                                  ItemList.OnItemVisibilityChangedListener} is set, if
         *                                  {@code header} is empty, or if a selectable list is
         *                                  added alongside other lists
         */
        public @NonNull Builder addSectionedList(@NonNull SectionedItemList list) {
            if (requireNonNull(list).getHeader().toString().length() == 0) {
                throw new IllegalArgumentException("Header cannot be empty");
            }

            ItemList itemList = list.getItemList();
            boolean isSelectableList = itemList.getOnSelectedDelegate() != null;
            if (mHasSelectableList || (isSelectableList && !mSectionedLists.isEmpty())) {
                throw new IllegalArgumentException(
                        "A selectable list cannot be added alongside any other lists");
            }
            mHasSelectableList = isSelectableList;

            if (itemList.getItems().isEmpty()) {
                throw new IllegalArgumentException("List cannot be empty");
            }

            if (itemList.getOnItemVisibilityChangedDelegate() != null) {
                throw new IllegalArgumentException(
                        "OnItemVisibilityChangedListener in the list is disallowed");
            }

            mSingleList = null;
            mSectionedLists.add(list);
            return this;
        }

        /**
         * Clears all of the {@link SectionedItemList}s added via
         * {@link #addSectionedList(SectionedItemList)}
         */
        @ExperimentalCarApi
        public @NonNull Builder clearSectionedLists() {
            mSectionedLists.clear();
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template or {@code null} to not display an {@link
         * ActionStrip}.
         *
         * <p>Unless set with this method, the template will not have an action strip.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its {@link ActionStrip}. Of the 2 allowed
         * {@link Action}s, one of them can contain a title as set via
         * {@link Action.Builder#setTitle}. Otherwise, only {@link Action}s with icons are allowed.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the requirements
         * @throws NullPointerException     if {@code actionStrip} is {@code null}
         *
         * @deprecated Use {@link Header.Builder#addEndHeaderAction(Action) for each action}
         */
        @Deprecated
        public @NonNull Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_SIMPLE.validateOrThrow(requireNonNull(actionStrip).getActions());
            mActionStrip = actionStrip;
            return this;
        }

        /**
         * Adds a template scoped action outside the rows.
         *
         * @throws IllegalArgumentException if {@code action} contains unsupported Action types,
         *                                  or does not contain a valid {@link CarIcon} and
         *                                  background {@link CarColor}, or if exceeds the
         *                                  maximum number of allowed actions for the template.
         * @see ActionsConstraints#ACTIONS_CONSTRAINTS_FAB
         */
        @RequiresCarApi(6)
        public @NonNull Builder addAction(@NonNull Action action) {
            List<Action> mActionsCopy = new ArrayList<>(mActions);
            mActionsCopy.add(requireNonNull(action));
            ActionsConstraints.ACTIONS_CONSTRAINTS_FAB.validateOrThrow(mActionsCopy);
            mActions.add(action);
            return this;
        }

        /**
         * Sets the {@link Header} for this template.
         *
         * <p>The end header actions will show up differently inside vs outside of a map template.
         * See {@link Header.Builder#addEndHeaderAction} for more details.</p>
         *
         * @throws NullPointerException if {@code header} is null
         */
        @RequiresCarApi(7)
        public @NonNull Builder setHeader(@NonNull Header header) {
            if (header.getStartHeaderAction() != null) {
                mHeaderAction = header.getStartHeaderAction();
            }
            if (header.getTitle() != null) {
                mTitle = header.getTitle();
            }
            if (!header.getEndHeaderActions().isEmpty()) {
                ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();
                for (Action action: header.getEndHeaderActions()) {
                    actionStripBuilder.addAction(action);
                }
                mActionStrip = actionStripBuilder.build();
            }
            mHeader = header;
            return this;
        }

        /**
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * The number of items in the {@link ItemList} should be smaller or equal than the limit
         * provided by
         * {@link androidx.car.app.constraints.ConstraintManager#CONTENT_LIMIT_TYPE_LIST}. The
         * host will ignore any items over that limit. Each {@link Row}s can add up to 2 lines of
         * texts via {@link Row.Builder#addText}.
         *
         * <p>If none of the header {@link Action}, the header title or the action strip have been
         * set on the template, the header is hidden.
         *
         * @throws IllegalStateException    if the template is in a loading state but there are
         *                                  lists added or vice versa
         * @throws IllegalArgumentException if the added {@link ItemList}(s) do not meet the
         *                                  template's requirements
         * @see androidx.car.app.constraints.ConstraintManager#getContentLimit(int)
         */
        public @NonNull ListTemplate build() {
            boolean hasList = mSingleList != null || !mSectionedLists.isEmpty();
            if (mIsLoading == hasList) {
                throw new IllegalStateException(
                        "Template is in a loading state but lists are added, or vice versa");
            }

            if (hasList) {
                if (!mSectionedLists.isEmpty()) {
                    ROW_LIST_CONSTRAINTS_FULL_LIST.validateOrThrow(mSectionedLists);
                } else if (mSingleList != null) {
                    ROW_LIST_CONSTRAINTS_FULL_LIST.validateOrThrow(mSingleList);
                }
            }

            if (!mSectionedLists.isEmpty()) {
                List<SectionedItemList> truncatedList = getTruncatedCopy(mSectionedLists);
                mSectionedLists.clear();
                mSectionedLists.addAll(truncatedList);
            } else if (mSingleList != null) {
                mSingleList = truncate(mSingleList, new TruncateCounter(MAX_ALLOWED_ITEMS));
            }

            return new ListTemplate(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
            mSectionedLists = new ArrayList<>();
            mActions = new ArrayList<>();
        }

        /** Creates a new {@link Builder}, populated from the input {@link ListTemplate} */
        @OptIn(markerClass = ExperimentalCarApi.class)
        Builder(@NonNull ListTemplate listTemplate) {
            mIsLoading = listTemplate.isLoading();
            mHeaderAction = listTemplate.getHeaderAction();
            mTitle = listTemplate.getTitle();
            mSingleList = listTemplate.getSingleList();

            // Must be mutable
            mSectionedLists = new ArrayList<>(listTemplate.getSectionedLists());

            mActionStrip = listTemplate.getActionStrip();
            mActions = new ArrayList<>(listTemplate.getActions());
            mHeader = listTemplate.getHeader();
        }
    }

    /** A wrapper around an int with helper methods to keep track of a truncation limit. */
    private static class TruncateCounter {
        private int mRemainingItems;

        TruncateCounter(int initialLimit) {
            mRemainingItems = initialLimit;
        }

        @CanIgnoreReturnValue
        public int decrement() {
            return --mRemainingItems;
        }

        @CanIgnoreReturnValue
        public int decrement(int minus) {
            mRemainingItems -= minus;
            return mRemainingItems;
        }

        public boolean canFit(int value) {
            return mRemainingItems >= value;
        }

        public int remainingItems() {
            return mRemainingItems;
        }
    }

    static List<SectionedItemList> getTruncatedCopy(List<SectionedItemList> originalList) {
        TruncateCounter itemLimit = new TruncateCounter(MAX_ALLOWED_ITEMS);
        List<SectionedItemList> result = new ArrayList<>();
        for (SectionedItemList original : originalList) {
            ItemList truncatedItemList = truncate(original.getItemList(), itemLimit);
            result.add(SectionedItemList.create(
                    truncatedItemList, original.getHeader().toCharSequence()));
            if (itemLimit.remainingItems() <= 0) {
                break;
            }
        }
        return result;
    }

    /** Truncates ListTemplates to not exceed the Android maximum binder transaction limit. */
    @OptIn(markerClass = ExperimentalCarApi.class)
    static ItemList truncate(ItemList itemList, TruncateCounter limit) {
        ItemList.Builder builder = new ItemList.Builder(itemList);
        builder.clearItems();
        for (Item item : itemList.getItems()) {
            // For Row and Grid items, no special truncation logic. Each item counts as 1.
            if (!(item instanceof ConversationItem)) {
                if (!limit.canFit(1)) {
                    break;
                }
                builder.addItem(item);
                limit.decrement();
                continue;
            }

            // For ConversationItem, truncate the messages to max 10.
            ConversationItem conversationItem = (ConversationItem) item;

            // Each message counts a 1, and each ConversationItem counts as 1, so a minimum of 2
            // spaces needs to remain.
            if (!limit.canFit(2)) {
                break;
            }

            // Rebuild the conversation item
            ConversationItem.Builder conversationBuilder =
                    new ConversationItem.Builder(conversationItem);
            int maxMessagesAllowed =
                    Math.min(limit.decrement(), MAX_MESSAGES_PER_CONVERSATION);
            int originalMessagesSize = conversationItem.getMessages().size();
            int messagesToAdd =
                    Math.min(originalMessagesSize, maxMessagesAllowed);
            // Messages are ordered oldest to the newest in a ConversationItem. Truncation should
            // remove the oldest messages from the beginning of the list.
            List<CarMessage> truncatedMessagesList =
                    conversationItem.getMessages().subList(
                            originalMessagesSize - messagesToAdd,
                            originalMessagesSize);
            conversationBuilder.setMessages(truncatedMessagesList);

            builder.addItem(conversationBuilder.build());

            limit.decrement(messagesToAdd);
        }

        return builder.build();
    }
}
