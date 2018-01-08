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

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.car.R;
import androidx.car.utils.ListItemBackgroundResolver;

/**
 * Adapter for {@link PagedListView} to display {@link ListItem}.
 *
 * <ul>
 *     <li> Implements {@link PagedListView.ItemCap} - defaults to unlimited item count.
 *     <li> Implements {@link PagedListView.DividerVisibilityManager} - to control dividers after
 *     individual {@link ListItem}.
 * </ul>
 *
 */
public class ListItemAdapter extends
        RecyclerView.Adapter<ListItemAdapter.ViewHolder> implements PagedListView.ItemCap,
        PagedListView.DividerVisibilityManager {

    /**
     * Constant class for background style of items.
     */
    public static final class BackgroundStyle {
        private BackgroundStyle() {}

        /**
         * Sets the background color of each item.
         */
        public static final int NONE = 0;
        /**
         * Sets each item in {@link CardView} with a rounded corner background and shadow.
         */
        public static final int CARD = 1;
        /**
         * Sets background of each item so the combined list looks like one elongated card, namely
         * top and bottom item will have rounded corner at only top/bottom side respectively. If
         * only one item exists, it will have both top and bottom rounded corner.
         */
        public static final int PANEL = 2;
    }

    private int mBackgroundStyle;

    private final Context mContext;
    private final ListItemProvider mItemProvider;

    private int mMaxItems = PagedListView.ItemCap.UNLIMITED;

    public ListItemAdapter(Context context, ListItemProvider itemProvider) {
        this(context, itemProvider, BackgroundStyle.NONE);
    }

    public ListItemAdapter(Context context, ListItemProvider itemProvider,
            int backgroundStyle) {
        mContext = context;
        mItemProvider = itemProvider;
        mBackgroundStyle = backgroundStyle;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View itemView = inflater.inflate(R.layout.car_paged_list_item_content, parent, false);

        ViewGroup container = createListItemContainer();
        container.addView(itemView);
        return new ViewHolder(container);
    }

    private ViewGroup createListItemContainer() {
        ViewGroup container;
        switch (mBackgroundStyle) {
            case BackgroundStyle.NONE:
            case BackgroundStyle.PANEL:
                FrameLayout frameLayout = new FrameLayout(mContext);
                frameLayout.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                frameLayout.setBackgroundColor(mContext.getColor(R.color.car_card));

                container = frameLayout;
                break;
            case BackgroundStyle.CARD:
                CardView card = new CardView(mContext);
                RecyclerView.LayoutParams cardLayoutParams = new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cardLayoutParams.bottomMargin = mContext.getResources().getDimensionPixelSize(
                        R.dimen.car_padding_3);
                card.setLayoutParams(cardLayoutParams);
                card.setRadius(mContext.getResources().getDimensionPixelSize(R.dimen.car_radius_1));
                card.setCardBackgroundColor(mContext.getColor(R.color.car_card));

                container = card;
                break;
            default:
                throw new IllegalArgumentException("Unknown background style. "
                        + "Expected constants in class ListItemAdapter.BackgroundStyle.");
        }
        return container;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ListItem item = mItemProvider.get(position);
        item.bind(holder);

        if (mBackgroundStyle == BackgroundStyle.PANEL) {
            ListItemBackgroundResolver.setBackground(
                    holder.itemView, position, mItemProvider.size());
        }
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

    /**
     * Holds views of an item in PagedListView.
     *
     * <p>This ViewHolder maps to views in layout car_paged_list_item_content.xml.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private RelativeLayout mContainerLayout;

        private ImageView mPrimaryIcon;

        private TextView mTitle;
        private TextView mBody;

        private View mSupplementalIconDivider;
        private ImageView mSupplementalIcon;

        private Button mAction1;
        private View mAction1Divider;

        private Button mAction2;
        private View mAction2Divider;

        public ViewHolder(View itemView) {
            super(itemView);

            mContainerLayout = itemView.findViewById(R.id.container);

            mPrimaryIcon = itemView.findViewById(R.id.primary_icon);

            mTitle = itemView.findViewById(R.id.title);
            mBody = itemView.findViewById(R.id.body);

            mSupplementalIcon = itemView.findViewById(R.id.supplemental_icon);
            mSupplementalIconDivider = itemView.findViewById(R.id.supplemental_icon_divider);

            mAction1 = itemView.findViewById(R.id.action1);
            mAction1Divider = itemView.findViewById(R.id.action1_divider);
            mAction2 = itemView.findViewById(R.id.action2);
            mAction2Divider = itemView.findViewById(R.id.action2_divider);
        }

        public RelativeLayout getContainerLayout() {
            return mContainerLayout;
        }

        public ImageView getPrimaryIcon() {
            return mPrimaryIcon;
        }

        public TextView getTitle() {
            return mTitle;
        }

        public TextView getBody() {
            return mBody;
        }

        public ImageView getSupplementalIcon() {
            return mSupplementalIcon;
        }

        public View getSupplementalIconDivider() {
            return mSupplementalIconDivider;
        }

        public Button getAction1() {
            return mAction1;
        }

        public View getAction1Divider() {
            return mAction1Divider;
        }

        public Button getAction2() {
            return mAction2;
        }

        public View getAction2Divider() {
            return mAction2Divider;
        }
    }
}
