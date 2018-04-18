/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.car.widget;

import android.app.Activity;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.StyleRes;
import androidx.car.R;
import androidx.car.utils.CarUxRestrictionsHelper;
import androidx.car.utils.ListItemBackgroundResolver;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Function;

/**
 * Adapter for {@link PagedListView} to display {@link ListItem}.
 *
 * <ul>
 *     <li> Implements {@link PagedListView.ItemCap} - defaults to unlimited item count.
 *     <li> Implements {@link PagedListView.DividerVisibilityManager} - to control dividers after
 *     individual {@link ListItem}.
 * </ul>
 *
 * <p>To enable support for {@link CarUxRestrictions}, call {@link #start()} in your
 * {@code Activity}'s {@link android.app.Activity#onCreate(Bundle)}, and {@link #stop()} in
 * {@link Activity#onStop()}.
 */
public class ListItemAdapter extends
        RecyclerView.Adapter<ListItem.ViewHolder> implements PagedListView.ItemCap,
        PagedListView.DividerVisibilityManager {

    /**
     * Constant class for background style of items.
     */
    public static final class BackgroundStyle {
        private BackgroundStyle() {}

        /**
         * Sets the background color of each item.
         * Background can be configured by {@link R.styleable#ListItem_listItemBackgroundColor}.
         */
        public static final int SOLID = 0;
        /**
         * Sets the background color of each item to none (transparent).
         */
        public static final int NONE = 1;
        /**
         * Sets each item in {@link CardView} with a rounded corner background and shadow.
         */
        public static final int CARD = 2;
        /**
         * Sets background of each item so the combined list looks like one elongated card, namely
         * top and bottom item will have rounded corner at only top/bottom side respectively. If
         * only one item exists, it will have both top and bottom rounded corner.
         */
        public static final int PANEL = 3;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        BackgroundStyle.SOLID,
        BackgroundStyle.NONE,
        BackgroundStyle.CARD,
        BackgroundStyle.PANEL
    })
    private @interface ListBackgroundStyle {}

    static final int LIST_ITEM_TYPE_TEXT = 1;
    static final int LIST_ITEM_TYPE_SEEKBAR = 2;
    static final int LIST_ITEM_TYPE_SUBHEADER = 3;

    private final SparseIntArray mViewHolderLayoutResIds = new SparseIntArray();
    private final SparseArray<Function<View, ListItem.ViewHolder>> mViewHolderCreator =
            new SparseArray<>();

    @ListBackgroundStyle private int mBackgroundStyle;

    @ColorInt private int mListItemBackgroundColor;
    @StyleRes private int mListItemTitleTextAppearance;
    @StyleRes private int mListItemBodyTextAppearance;

    private final CarUxRestrictionsHelper mUxRestrictionsHelper;
    private CarUxRestrictions mCurrentUxRestrictions;

    private Context mContext;
    private final ListItemProvider mItemProvider;

    private int mMaxItems = PagedListView.ItemCap.UNLIMITED;

    /**
     * Defaults {@link BackgroundStyle} to {@link BackgroundStyle#SOLID}.
     */
    public ListItemAdapter(Context context, ListItemProvider itemProvider) {
        this(context, itemProvider, BackgroundStyle.SOLID);
    }

    public ListItemAdapter(Context context, ListItemProvider itemProvider,
            @ListBackgroundStyle int backgroundStyle) {
        mContext = context;
        mItemProvider = itemProvider;
        mBackgroundStyle = backgroundStyle;

        registerListItemViewType(LIST_ITEM_TYPE_TEXT,
                R.layout.car_list_item_text_content, TextListItem::createViewHolder);
        registerListItemViewType(LIST_ITEM_TYPE_SEEKBAR,
                R.layout.car_list_item_seekbar_content, SeekbarListItem::createViewHolder);
        registerListItemViewType(LIST_ITEM_TYPE_SUBHEADER,
                R.layout.car_list_item_subheader_content, SubheaderListItem::createViewHolder);

        mUxRestrictionsHelper =
                new CarUxRestrictionsHelper(context, carUxRestrictions -> {
                    mCurrentUxRestrictions = carUxRestrictions;
                    notifyDataSetChanged();
                });
    }

    /**
     * Enables support for {@link CarUxRestrictions}.
     *
     * <p>This method can be called from {@code Activity}'s {@link Activity#onStart()}, or at the
     * time of construction.
     *
     * <p>This method must be accompanied with a matching {@link #stop()} to avoid leak.
     */
    public void start() {
        mUxRestrictionsHelper.start();
    }

    /**
     * Disables support for {@link CarUxRestrictions}, and frees up resources.
     *
     * <p>This method should be called from {@code Activity}'s {@link Activity#onStop()}, or at the
     * time of this adapter being discarded.
     */
    public void stop() {
        mUxRestrictionsHelper.stop();
    }

    /**
     * Registers a function that returns {@link RecyclerView.ViewHolder}
     * for its matching view type returned by {@link ListItem#getViewType()}.
     *
     * <p>The function will receive a view as {@link RecyclerView.ViewHolder#itemView}. This view
     * uses background defined by {@link BackgroundStyle}.
     *
     * <p>Subclasses of {@link ListItem} in package androidx.car.widget are already registered.
     *
     * @param viewType use negative value for custom view type.
     * @param function function to create ViewHolder for {@code viewType}.
     */
    public void registerListItemViewType(int viewType, @LayoutRes int layoutResId,
            Function<View, ListItem.ViewHolder> function) {
        if (mViewHolderLayoutResIds.get(viewType) != 0
                || mViewHolderCreator.get(viewType) != null) {
            throw new IllegalArgumentException("View type is already registered.");
        }
        mViewHolderCreator.put(viewType, function);
        mViewHolderLayoutResIds.put(viewType, layoutResId);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        // When attached to the RecyclerView, update the Context so that this ListItemAdapter can
        // retrieve theme information off that view.
        mContext = recyclerView.getContext();

        TypedArray a = mContext.getTheme().obtainStyledAttributes(R.styleable.ListItem);

        mListItemBackgroundColor = a.getColor(R.styleable.ListItem_listItemBackgroundColor,
                mContext.getColor(R.color.car_card));
        mListItemTitleTextAppearance = a.getResourceId(
                R.styleable.ListItem_listItemTitleTextAppearance,
                R.style.TextAppearance_Car_Body1);
        mListItemBodyTextAppearance = a.getResourceId(
                R.styleable.ListItem_listItemBodyTextAppearance,
                R.style.TextAppearance_Car_Body2);
        a.recycle();
    }

    @Override
    public ListItem.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mViewHolderLayoutResIds.get(viewType) == 0
                || mViewHolderCreator.get(viewType) == null) {
            throw new IllegalArgumentException("Unregistered view type.");
        }

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View itemView = inflater.inflate(mViewHolderLayoutResIds.get(viewType), parent, false);

        ViewGroup container = createListItemContainer();
        container.addView(itemView);
        return mViewHolderCreator.get(viewType).apply(container);
    }

    /**
     * Creates a view with background set by {@link BackgroundStyle}.
     */
    private ViewGroup createListItemContainer() {
        ViewGroup container;
        if (mBackgroundStyle == BackgroundStyle.CARD) {
            CardView card = new CardView(mContext);
            RecyclerView.LayoutParams cardLayoutParams = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLayoutParams.bottomMargin = mContext.getResources().getDimensionPixelSize(
                    R.dimen.car_padding_1);
            card.setLayoutParams(cardLayoutParams);
            card.setRadius(mContext.getResources().getDimensionPixelSize(R.dimen.car_radius_1));
            card.setCardBackgroundColor(mListItemBackgroundColor);

            container = card;
        } else {
            FrameLayout frameLayout = new FrameLayout(mContext);
            frameLayout.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            // Skip setting background color for NONE.
            if (mBackgroundStyle != BackgroundStyle.NONE) {
                frameLayout.setBackgroundColor(mListItemBackgroundColor);
            }

            container = frameLayout;
        }
        return container;
    }

    @Override
    public int getItemViewType(int position) {
        return mItemProvider.get(position).getViewType();
    }

    @Override
    public void onBindViewHolder(ListItem.ViewHolder holder, int position) {
        if (mBackgroundStyle == BackgroundStyle.PANEL) {
            ListItemBackgroundResolver.setBackground(
                    holder.itemView, position, mItemProvider.size());
        }

        // Car may not be initialized thus current UXR will not be available.
        if (mCurrentUxRestrictions != null) {
            holder.complyWithUxRestrictions(mCurrentUxRestrictions);
        }

        ListItem item = mItemProvider.get(position);
        item.setTitleTextAppearance(mListItemTitleTextAppearance);
        item.setBodyTextAppearance(mListItemBodyTextAppearance);

        item.bind(holder);
    }

    @Override
    public int getItemCount() {
        return mMaxItems == PagedListView.ItemCap.UNLIMITED
                ? mItemProvider.size()
                : Math.min(mItemProvider.size(), mMaxItems);
    }

    @Override
    public void setMaxItems(int maxItems) {
        mMaxItems = maxItems;
    }

    @Override
    public boolean shouldHideDivider(int position) {
        // By default we should show the divider i.e. return false.

        // Check if position is within range, and then check the item flag.
        return position >= 0 && position < getItemCount()
                && mItemProvider.get(position).shouldHideDivider();
    }
}
