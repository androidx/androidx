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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.annotation.Retention;

import androidx.car.R;

/**
 * Adapter for {@link PagedListView} to display {@link ListItem}.
 *
 * Implements {@link PagedListView.ItemCap} - defaults to unlimited item count.
 */
public class ListItemAdapter extends
        RecyclerView.Adapter<ListItemAdapter.ViewHolder> implements PagedListView.ItemCap {
    @Retention(SOURCE)
    @IntDef({CAR_PAGED_LIST_ITEM, CAR_PAGED_LIST_CARD})
    public @interface PagedListItemType {}
    public static final int CAR_PAGED_LIST_ITEM = 0;
    public static final int CAR_PAGED_LIST_CARD = 1;

    private final Context mContext;
    private final ListItemProvider mItemProvider;

    private int mMaxItems = PagedListView.ItemCap.UNLIMITED;

    public ListItemAdapter(Context context, ListItemProvider itemProvider) {
        mContext = context;
        mItemProvider = itemProvider;
    }

    @Override
    public int getItemViewType(int position) {
        return mItemProvider.get(position).getViewType();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, @PagedListItemType int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        int layoutId;
        switch (viewType) {
            case CAR_PAGED_LIST_ITEM:
                layoutId = R.layout.car_paged_list_item;
                break;
            case CAR_PAGED_LIST_CARD:
                layoutId = R.layout.car_paged_list_card;
                break;
            default:
                throw new IllegalArgumentException("Unrecognizable view type: " + viewType);
        }
        View itemView = inflater.inflate(layoutId, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ListItem item = mItemProvider.get(position);
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
