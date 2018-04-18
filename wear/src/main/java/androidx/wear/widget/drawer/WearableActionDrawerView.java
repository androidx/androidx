/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.wear.widget.drawer;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.R;
import androidx.wear.internal.widget.ResourcesUtil;
import androidx.wear.widget.drawer.WearableActionDrawerMenu.WearableActionDrawerMenuItem;

import java.util.Objects;

/**
 * Ease of use class for creating a Wearable action drawer. This can be used with {@link
 * WearableDrawerLayout} to create a drawer for users to easily pull up contextual actions. These
 * contextual actions may be specified by using a {@link Menu}, which may be populated by either:
 *
 * <ul> <li>Specifying the {@code app:actionMenu} attribute in the XML layout file. Example:
 * <pre>
 * &lt;androidx.wear.widget.drawer.WearableActionDrawerView
 *     xmlns:app="http://schemas.android.com/apk/res-auto"
 *     android:layout_width=”match_parent”
 *     android:layout_height=”match_parent”
 *     app:actionMenu="@menu/action_drawer" /&gt;</pre>
 *
 * <li>Getting the menu with {@link #getMenu}, and then inflating it with {@link
 * MenuInflater#inflate}. Example:
 * <pre>
 * Menu menu = actionDrawer.getMenu();
 * getMenuInflater().inflate(R.menu.action_drawer, menu);</pre>
 *
 * </ul>
 *
 * <p><b>The full {@link Menu} and {@link MenuItem} APIs are not implemented.</b> The following
 * methods are guaranteed to work:
 *
 * <p>For {@link Menu}, the add methods, {@link Menu#clear}, {@link Menu#removeItem}, {@link
 * Menu#findItem}, {@link Menu#size}, and {@link Menu#getItem} are implemented.
 *
 * <p>For {@link MenuItem}, setting and getting the title and icon, {@link MenuItem#getItemId}, and
 * {@link MenuItem#setOnMenuItemClickListener} are implemented.
 */
public class WearableActionDrawerView extends WearableDrawerView {

    private static final String TAG = "WearableActionDrawer";

    private final RecyclerView mActionList;
    private final int mTopPadding;
    private final int mBottomPadding;
    private final int mLeftPadding;
    private final int mRightPadding;
    private final int mFirstItemTopPadding;
    private final int mLastItemBottomPadding;
    private final int mIconRightMargin;
    private final boolean mShowOverflowInPeek;
    @Nullable private final ImageView mPeekActionIcon;
    @Nullable private final ImageView mPeekExpandIcon;
    private final RecyclerView.Adapter<RecyclerView.ViewHolder> mActionListAdapter;
    private OnMenuItemClickListener mOnMenuItemClickListener;
    private Menu mMenu;
    @Nullable private CharSequence mTitle;

    public WearableActionDrawerView(Context context) {
        this(context, null);
    }

    public WearableActionDrawerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableActionDrawerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WearableActionDrawerView(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setLockedWhenClosed(true);

        boolean showOverflowInPeek = false;
        int menuRes = 0;
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(
                    attrs, R.styleable.WearableActionDrawerView, defStyleAttr, 0 /* defStyleRes */);

            try {
                mTitle = typedArray.getString(R.styleable.WearableActionDrawerView_drawerTitle);
                showOverflowInPeek = typedArray.getBoolean(
                        R.styleable.WearableActionDrawerView_showOverflowInPeek, false);
                menuRes = typedArray
                        .getResourceId(R.styleable.WearableActionDrawerView_actionMenu, 0);
            } finally {
                typedArray.recycle();
            }
        }

        AccessibilityManager accessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        mShowOverflowInPeek = showOverflowInPeek || accessibilityManager.isEnabled();

        if (!mShowOverflowInPeek) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View peekView = layoutInflater.inflate(R.layout.ws_action_drawer_peek_view,
                    getPeekContainer(), false /* attachToRoot */);
            setPeekContent(peekView);
            mPeekActionIcon = peekView.findViewById(R.id.ws_action_drawer_peek_action_icon);
            mPeekExpandIcon = peekView.findViewById(R.id.ws_action_drawer_expand_icon);
        } else {
            mPeekActionIcon = null;
            mPeekExpandIcon = null;
            getPeekContainer().setContentDescription(
                    context.getString(R.string.ws_action_drawer_content_description));
        }

        if (menuRes != 0) {
            // This must occur after initializing mPeekActionIcon, otherwise updatePeekIcons will
            // exit early.
            MenuInflater inflater = new MenuInflater(context);
            inflater.inflate(menuRes, getMenu());
        }

        int screenWidthPx = ResourcesUtil.getScreenWidthPx(context);
        int screenHeightPx = ResourcesUtil.getScreenHeightPx(context);

        Resources res = getResources();
        mTopPadding = res.getDimensionPixelOffset(R.dimen.ws_action_drawer_item_top_padding);
        mBottomPadding = res.getDimensionPixelOffset(R.dimen.ws_action_drawer_item_bottom_padding);
        mLeftPadding =
                ResourcesUtil.getFractionOfScreenPx(
                        context, screenWidthPx, R.fraction.ws_action_drawer_item_left_padding);
        mRightPadding =
                ResourcesUtil.getFractionOfScreenPx(
                        context, screenWidthPx, R.fraction.ws_action_drawer_item_right_padding);

        mFirstItemTopPadding =
                ResourcesUtil.getFractionOfScreenPx(
                        context, screenHeightPx,
                        R.fraction.ws_action_drawer_item_first_item_top_padding);
        mLastItemBottomPadding =
                ResourcesUtil.getFractionOfScreenPx(
                        context, screenHeightPx,
                        R.fraction.ws_action_drawer_item_last_item_bottom_padding);

        mIconRightMargin = res
                .getDimensionPixelOffset(R.dimen.ws_action_drawer_item_icon_right_margin);

        mActionList = new RecyclerView(context);
        mActionList.setLayoutManager(new LinearLayoutManager(context));
        mActionListAdapter = new ActionListAdapter(getMenu());
        mActionList.setAdapter(mActionListAdapter);
        setDrawerContent(mActionList);
    }

    @Override
    public void onDrawerOpened() {
        if (mActionListAdapter.getItemCount() > 0) {
            RecyclerView.ViewHolder holder = mActionList.findViewHolderForAdapterPosition(0);
            if (holder != null && holder.itemView != null) {
                holder.itemView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        }
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        // Prevent the window from being swiped closed while it is open by saying that it can scroll
        // horizontally.
        return isOpened();
    }

    @Override
    public void onPeekContainerClicked(View v) {
        if (mShowOverflowInPeek) {
            super.onPeekContainerClicked(v);
        } else {
            onMenuItemClicked(0);
        }
    }

    @Override
  /* package */ int preferGravity() {
        return Gravity.BOTTOM;
    }

    /**
     * Set a {@link OnMenuItemClickListener} for this action drawer.
     */
    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        mOnMenuItemClickListener = listener;
    }

    /**
     * Sets the title for this action drawer. If {@code title} is {@code null}, then the title will
     * be removed.
     */
    public void setTitle(@Nullable CharSequence title) {
        if (Objects.equals(title, mTitle)) {
            return;
        }

        CharSequence oldTitle = mTitle;
        mTitle = title;
        if (oldTitle == null) {
            mActionListAdapter.notifyItemInserted(0);
        } else if (title == null) {
            mActionListAdapter.notifyItemRemoved(0);
        } else {
            mActionListAdapter.notifyItemChanged(0);
        }
    }

    private boolean hasTitle() {
        return mTitle != null;
    }

    private void onMenuItemClicked(int position) {
        if (position >= 0 && position < getMenu().size()) { // Sanity check.
            WearableActionDrawerMenuItem menuItem =
                    (WearableActionDrawerMenuItem) getMenu().getItem(position);
            if (menuItem.invoke()) {
                return;
            }

            if (mOnMenuItemClickListener != null) {
                mOnMenuItemClickListener.onMenuItemClick(menuItem);
            }
        }
    }

    private void updatePeekIcons() {
        if (mPeekActionIcon == null || mPeekExpandIcon == null) {
            return;
        }

        Menu menu = getMenu();
        int numberOfActions = menu.size();

        // Only show drawer content (and allow it to be opened) when there's more than one action.
        if (numberOfActions > 1) {
            setDrawerContent(mActionList);
            mPeekExpandIcon.setVisibility(VISIBLE);
        } else {
            setDrawerContent(null);
            mPeekExpandIcon.setVisibility(GONE);
        }

        if (numberOfActions >= 1) {
            Drawable firstActionDrawable = menu.getItem(0).getIcon();
            // Because the ImageView will tint the Drawable white, attempt to get a mutable copy of
            // it. If a copy isn't made, the icon will be white in the expanded state, rendering it
            // invisible.
            if (firstActionDrawable != null) {
                firstActionDrawable = firstActionDrawable.getConstantState().newDrawable().mutate();
                firstActionDrawable.clearColorFilter();
            }

            mPeekActionIcon.setImageDrawable(firstActionDrawable);
            mPeekActionIcon.setContentDescription(menu.getItem(0).getTitle());
        }
    }

    /**
     * Returns the Menu object that this WearableActionDrawer represents.
     *
     * <p>Applications should use this method to obtain the WearableActionDrawers's Menu object and
     * inflate or add content to it as necessary.
     *
     * @return the Menu presented by this view
     */
    public Menu getMenu() {
        if (mMenu == null) {
            mMenu = new WearableActionDrawerMenu(
                    getContext(),
                    new WearableActionDrawerMenu.WearableActionDrawerMenuListener() {
                        @Override
                        public void menuItemChanged(int position) {
                            if (mActionListAdapter != null) {
                                int listPosition = hasTitle() ? position + 1 : position;
                                mActionListAdapter.notifyItemChanged(listPosition);
                            }
                            if (position == 0) {
                                updatePeekIcons();
                            }
                        }

                        @Override
                        public void menuItemAdded(int position) {
                            if (mActionListAdapter != null) {
                                int listPosition = hasTitle() ? position + 1 : position;
                                mActionListAdapter.notifyItemInserted(listPosition);
                            }
                            // Handle transitioning from 0->1 items (set peek icon) and
                            // 1->2 (switch to ellipsis.)
                            if (position <= 1) {
                                updatePeekIcons();
                            }
                        }

                        @Override
                        public void menuItemRemoved(int position) {
                            if (mActionListAdapter != null) {
                                int listPosition = hasTitle() ? position + 1 : position;
                                mActionListAdapter.notifyItemRemoved(listPosition);
                            }
                            // Handle transitioning from 2->1 items (remove ellipsis), and
                            // also the removal of item 1, which could cause the peek icon
                            // to change.
                            if (position <= 1) {
                                updatePeekIcons();
                            }
                        }

                        @Override
                        public void menuChanged() {
                            if (mActionListAdapter != null) {
                                mActionListAdapter.notifyDataSetChanged();
                            }
                            updatePeekIcons();
                        }
                    });
        }

        return mMenu;
    }

    private static final class TitleViewHolder extends RecyclerView.ViewHolder {

        public final View view;
        public final TextView textView;

        TitleViewHolder(View view) {
            super(view);
            this.view = view;
            textView = (TextView) view.findViewById(R.id.ws_action_drawer_title);
        }
    }

    private final class ActionListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int TYPE_ACTION = 0;
        public static final int TYPE_TITLE = 1;
        private final Menu mActionMenu;
        private final View.OnClickListener mItemClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int childPos =
                                mActionList.getChildAdapterPosition(v) - (hasTitle() ? 1 : 0);
                        if (childPos == RecyclerView.NO_POSITION) {
                            Log.w(TAG, "invalid child position");
                            return;
                        }
                        onMenuItemClicked(childPos);
                    }
                };

        ActionListAdapter(Menu menu) {
            mActionMenu = getMenu();
        }

        @Override
        public int getItemCount() {
            return mActionMenu.size() + (hasTitle() ? 1 : 0);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            int titleAwarePosition = hasTitle() ? position - 1 : position;
            if (viewHolder instanceof ActionItemViewHolder) {
                ActionItemViewHolder holder = (ActionItemViewHolder) viewHolder;
                holder.view.setPadding(
                        mLeftPadding,
                        position == 0 ? mFirstItemTopPadding : mTopPadding,
                        mRightPadding,
                        position == getItemCount() - 1 ? mLastItemBottomPadding : mBottomPadding);

                Drawable icon = mActionMenu.getItem(titleAwarePosition).getIcon();
                if (icon != null) {
                    icon = icon.getConstantState().newDrawable().mutate();
                }
                CharSequence title = mActionMenu.getItem(titleAwarePosition).getTitle();
                holder.textView.setText(title);
                holder.textView.setContentDescription(title);
                holder.iconView.setImageDrawable(icon);
            } else if (viewHolder instanceof TitleViewHolder) {
                TitleViewHolder holder = (TitleViewHolder) viewHolder;
                holder.textView.setPadding(0, mFirstItemTopPadding, 0, mBottomPadding);
                holder.textView.setText(mTitle);
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case TYPE_TITLE:
                    View titleView =
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.ws_action_drawer_title_view, parent, false);
                    return new TitleViewHolder(titleView);

                case TYPE_ACTION:
                default:
                    View actionView =
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.ws_action_drawer_item_view, parent, false);
                    actionView.setOnClickListener(mItemClickListener);
                    return new ActionItemViewHolder(actionView);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return hasTitle() && position == 0 ? TYPE_TITLE : TYPE_ACTION;
        }
    }

    private final class ActionItemViewHolder extends RecyclerView.ViewHolder {

        public final View view;
        public final ImageView iconView;
        public final TextView textView;

        ActionItemViewHolder(View view) {
            super(view);
            this.view = view;
            iconView = (ImageView) view.findViewById(R.id.ws_action_drawer_item_icon);
            ((LinearLayout.LayoutParams) iconView.getLayoutParams()).setMarginEnd(mIconRightMargin);
            textView = (TextView) view.findViewById(R.id.ws_action_drawer_item_text);
        }
    }
}
