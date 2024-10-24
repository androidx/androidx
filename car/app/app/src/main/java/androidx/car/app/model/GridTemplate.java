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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;

import static java.util.Objects.requireNonNull;

import androidx.annotation.IntDef;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.ActionsConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;
import androidx.car.app.utils.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A template representing a grid of items.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in
 * {@link androidx.car.app.Screen#onGetTemplate()}, this template is considered a refresh of a
 * previous one if:
 *
 * <ul>
 *   <li>The previous template is in a loading state (see {@link Builder#setLoading}, or
 *   <li>The template title has not changed, and the number of grid items and the title of each
 *       grid item have not changed.
 * </ul>
 */
@CarProtocol
@KeepFields
public final class GridTemplate implements Template {
    /**
     * The size of each grid item contained within this GridTemplate.
     *
     * <p>The host decides how to map these size buckets to dimensions. The grid item image size
     * and grid item width will vary by bucket, and the number of items per row
     * will be adjusted according to bucket and screen size.
     */
    @ExperimentalCarApi
    @IntDef(
            value = {
                    ITEM_SIZE_SMALL,
                    ITEM_SIZE_MEDIUM,
                    ITEM_SIZE_LARGE
            })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface ItemSize {
    }

    /**
     * Represents a small size for all grid items within a template. This is the default size.
     *
     * @see GridTemplate.Builder#setItemSize(int)
     */
    @ExperimentalCarApi
    public static final int ITEM_SIZE_SMALL = (1 << 0);

    /**
     * Represents a medium size for all grid items within a template.
     *
     * @see GridTemplate.Builder#setItemSize(int)
     */
    @ExperimentalCarApi
    public static final int ITEM_SIZE_MEDIUM = (1 << 1);

    /**
     * Represents a large size for all grid items within a template.
     *
     * @see GridTemplate.Builder#setItemSize(int)
     */
    @ExperimentalCarApi
    public static final int ITEM_SIZE_LARGE = (1 << 2);

    /**
     * The shape of each grid item image contained within this GridTemplate.
     *
     * <p>Grid item images will be cropped by the host to match the shape type.
     */
    @ExperimentalCarApi
    @IntDef(
            value = {
                    ITEM_IMAGE_SHAPE_UNSET,
                    ITEM_IMAGE_SHAPE_CIRCLE,
            })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface ItemImageShape {
    }

    /**
     * Represents a preference to keep the images as-is without modifying their shape.
     *
     * <p>This is the default setting.
     *
     * @see GridTemplate.Builder#setItemImageShape(int)
     */
    @ExperimentalCarApi
    public static final int ITEM_IMAGE_SHAPE_UNSET = (1 << 0);

    /**
     * Represents a preference to crop all grid item images into the shape of a circle.
     *
     * @see GridTemplate.Builder#setItemImageShape(int)
     */
    @ExperimentalCarApi
    public static final int ITEM_IMAGE_SHAPE_CIRCLE = (1 << 1);

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

    /**
     * @deprecated use {@link Header.Builder#addEndHeaderAction(Action)} for each action; mHeader
     * replaces the need for this field.
     */
    @Deprecated
    private final @Nullable ActionStrip mActionStrip;
    private final List<Action> mActions;
    @ItemSize
    private final int mItemSize;
    @ItemImageShape
    private final int mItemImageShape;

    /**
     * Represents a Header object to set the startHeaderAction, the title and the endHeaderActions
     *
     * @see GridTemplate.Builder#setHeader(Header)
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
     * Returns the {@link ItemList} instance that contains the grid items to display or {@code
     * null} if not set.
     *
     * @see Builder#setSingleList(ItemList)
     */
    public @Nullable ItemList getSingleList() {
        return mSingleList;
    }

    /**
     * Returns the list of additional actions.
     *
     * @see GridTemplate.Builder#addAction(Action)
     */
    @ExperimentalCarApi
    @RequiresCarApi(7)
    public @NonNull List<Action> getActions() {
        return mActions;
    }

    /**
     * Returns the grid item size, which applies to all grid items in the template.
     *
     * @see GridTemplate.Builder#setItemSize(int)
     */
    @ExperimentalCarApi
    @ItemSize
    public int getItemSize() {
        return mItemSize;
    }

    /**
     * Returns the item image shape.
     *
     * <p>All item images in the grid are cropped into the specified shape.
     *
     * @see GridTemplate.Builder#setItemImageShape(int)
     */
    @ExperimentalCarApi
    @ItemImageShape
    public int getItemImageShape() {
        return mItemImageShape;
    }

    /**
     * Returns the {@link Header} to display in this template.
     *
     * <p>This method was introduced in API 7, but is backwards compatible even if the client is
     * using API 6 or below. </p>
     *
     * @see GridTemplate.Builder#setHeader(Header)
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
        return "GridTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsLoading, mTitle, mHeaderAction, mSingleList, mActionStrip,
                mItemSize, mItemImageShape, mHeader);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GridTemplate)) {
            return false;
        }
        GridTemplate otherTemplate = (GridTemplate) other;

        return mIsLoading == otherTemplate.mIsLoading
                && Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mSingleList, otherTemplate.mSingleList)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip)
                && Objects.equals(mActions, otherTemplate.mActions)
                && mItemSize == otherTemplate.mItemSize
                && mItemImageShape == otherTemplate.mItemImageShape
                && Objects.equals(mHeader, otherTemplate.mHeader);
    }

    GridTemplate(Builder builder) {
        mIsLoading = builder.mIsLoading;
        mTitle = builder.mTitle;
        mHeaderAction = builder.mHeaderAction;
        mSingleList = builder.mSingleList;
        mActionStrip = builder.mActionStrip;
        mActions = CollectionUtils.unmodifiableCopy(builder.mActions);
        mItemSize = builder.mItemSize;
        mItemImageShape = builder.mItemImageShape;
        mHeader = builder.mHeader;
    }

    /** Constructs an empty instance, used by serialization code. */
    @OptIn(markerClass = ExperimentalCarApi.class)
    private GridTemplate() {
        mIsLoading = false;
        mTitle = null;
        mHeaderAction = null;
        mSingleList = null;
        mActionStrip = null;
        mActions = Collections.emptyList();
        mItemSize = ITEM_SIZE_SMALL;
        mItemImageShape = ITEM_IMAGE_SHAPE_UNSET;
        mHeader = null;
    }

    /** A builder of {@link GridTemplate}. */
    @OptIn(markerClass = ExperimentalCarApi.class)
    public static final class Builder {
        boolean mIsLoading;
        @Nullable ItemList mSingleList;
        @Nullable CarText mTitle;
        @Nullable Action mHeaderAction;
        @Nullable ActionStrip mActionStrip;
        final List<Action> mActions = new ArrayList<>();
        @ItemSize
        int mItemSize = ITEM_SIZE_SMALL;
        @ItemImageShape int mItemImageShape = ITEM_IMAGE_SHAPE_UNSET;
        @Nullable Header mHeader;

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI shows a loading indicator where the grid content
         * would be otherwise. The caller is expected to call
         * {@link androidx.car.app.Screen#invalidate()} and send the new template content to the
         * host once the data is ready. If set to {@code false}, the UI shows the
         * {@link ItemList} contents added via {@link #setSingleList}.
         */
        public @NonNull Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed in the header of the template.
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
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(Collections.singletonList(headerAction));
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
         * @throws NullPointerException if {@code list} is null
         */
        public @NonNull Builder setSingleList(@NonNull ItemList list) {
            mSingleList = requireNonNull(list);
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template.
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
         * Adds a template scoped action outside of the grid items. This action will be displayed
         * as a floating action button.
         *
         * @throws IllegalArgumentException if {@code action} contains unsupported Action types,
         *                                  or does not contain a valid {@link CarIcon} and
         *                                  background {@link CarColor}, or if exceeds the
         *                                  maximum number of allowed actions for the template.
         * @see ActionsConstraints#ACTIONS_CONSTRAINTS_FAB
         */
        @ExperimentalCarApi
        @RequiresCarApi(7)
        public @NonNull Builder addAction(@NonNull Action action) {
            List<Action> mActionsCopy = new ArrayList<>(mActions);
            mActionsCopy.add(requireNonNull(action));
            ActionsConstraints.ACTIONS_CONSTRAINTS_FAB.validateOrThrow(mActionsCopy);
            mActions.add(action);
            return this;
        }

        /**
         * Sets a relative size of all grid items in the template.
         *
         * <p>This setting will affect the grid item image size and minimum width of each item.
         * It can also impact the number of items displayed per row depending on screen size.
         * These values may change in the future.
         *
         * <p>This setting takes precedence over the {@link GridItem#IMAGE_TYPE_LARGE} setting
         * for determining the grid item image size.
         *
         * <p>If this is not called, the default value is {@link #ITEM_SIZE_SMALL}
         */
        @ExperimentalCarApi
        public @NonNull Builder setItemSize(@ItemSize int gridItemSize) {
            mItemSize = gridItemSize;
            return this;
        }

        /**
         * Sets the item image shape for this template.
         *
         * <p>Grid item images will all be cropped to the specified shape. If set to
         * ITEM_IMAGE_SHAPE_UNSET, the images will be rendered as-is without changing the shape.
         *
         * <p>If not set, default to ITEM_IMAGE_SHAPE_UNSET.
         */
        @ExperimentalCarApi
        public @NonNull Builder setItemImageShape(@ItemImageShape int itemImageShape) {
            mItemImageShape = itemImageShape;
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
         * {@link androidx.car.app.constraints.ConstraintManager#CONTENT_LIMIT_TYPE_GRID}. The
         * host will ignore any items over that limit.
         *
         * <p>If none of the header {@link Action}, the header title or the action strip have been
         * set on the template, the header is hidden.
         *
         * @throws IllegalStateException    if the template is in a loading state but there are
         *                                  lists added, or vice versa.
         * @throws IllegalArgumentException if the added {@link ItemList} does not meet the
         *                                  template's requirements.
         * @see androidx.car.app.constraints.ConstraintManager#getContentLimit(int)
         */
        public @NonNull GridTemplate build() {
            boolean hasList = mSingleList != null;
            if (mIsLoading == hasList) {
                throw new IllegalStateException(
                        "Template is in a loading state but lists are added, or vice versa");
            }

            if (mSingleList != null) {
                for (Item gridItemObject : mSingleList.getItems()) {
                    if (!(gridItemObject instanceof GridItem)) {
                        throw new IllegalArgumentException(
                                "All the items in grid template's item list must be grid items");
                    }
                }
            }

            return new GridTemplate(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
