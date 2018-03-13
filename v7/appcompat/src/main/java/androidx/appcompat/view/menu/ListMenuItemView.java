/*
 * Copyright (C) 2006 The Android Open Source Project
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

package androidx.appcompat.view.menu;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.view.ViewCompat;

/**
 * The item view for each item in the ListView-based MenuViews.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ListMenuItemView extends LinearLayout
        implements MenuView.ItemView, AbsListView.SelectionBoundsAdjuster {
    private static final String TAG = "ListMenuItemView";
    private MenuItemImpl mItemData;

    private ImageView mIconView;
    private RadioButton mRadioButton;
    private TextView mTitleView;
    private CheckBox mCheckBox;
    private TextView mShortcutView;
    private ImageView mSubMenuArrowView;
    private ImageView mGroupDivider;
    private LinearLayout mContent;

    private Drawable mBackground;
    private int mTextAppearance;
    private Context mTextAppearanceContext;
    private boolean mPreserveIconSpacing;
    private Drawable mSubMenuArrow;
    private boolean mHasListDivider;

    private int mMenuType;

    private LayoutInflater mInflater;

    private boolean mForceShowIcon;

    public ListMenuItemView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.listMenuViewStyle);
    }

    public ListMenuItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);

        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(),
                attrs, R.styleable.MenuView, defStyleAttr, 0);

        mBackground = a.getDrawable(R.styleable.MenuView_android_itemBackground);
        mTextAppearance = a.getResourceId(R.styleable.
                MenuView_android_itemTextAppearance, -1);
        mPreserveIconSpacing = a.getBoolean(
                R.styleable.MenuView_preserveIconSpacing, false);
        mTextAppearanceContext = context;
        mSubMenuArrow = a.getDrawable(R.styleable.MenuView_subMenuArrow);

        final TypedArray b = context.getTheme()
                .obtainStyledAttributes(null, new int[] { android.R.attr.divider },
                        R.attr.dropDownListViewStyle, 0);
        mHasListDivider = b.hasValue(0);

        a.recycle();
        b.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ViewCompat.setBackground(this, mBackground);

        mTitleView = findViewById(R.id.title);
        if (mTextAppearance != -1) {
            mTitleView.setTextAppearance(mTextAppearanceContext,
                    mTextAppearance);
        }

        mShortcutView = findViewById(R.id.shortcut);
        mSubMenuArrowView = findViewById(R.id.submenuarrow);
        if (mSubMenuArrowView != null) {
            mSubMenuArrowView.setImageDrawable(mSubMenuArrow);
        }
        mGroupDivider = findViewById(R.id.group_divider);

        mContent = findViewById(R.id.content);
    }

    @Override
    public void initialize(MenuItemImpl itemData, int menuType) {
        mItemData = itemData;
        mMenuType = menuType;

        setVisibility(itemData.isVisible() ? View.VISIBLE : View.GONE);

        setTitle(itemData.getTitleForItemView(this));
        setCheckable(itemData.isCheckable());
        setShortcut(itemData.shouldShowShortcut(), itemData.getShortcut());
        setIcon(itemData.getIcon());
        setEnabled(itemData.isEnabled());
        setSubMenuArrowVisible(itemData.hasSubMenu());
        setContentDescription(itemData.getContentDescription());
    }

    private void addContentView(View v) {
        addContentView(v, -1);
    }

    private void addContentView(View v, int index) {
        if (mContent != null) {
            mContent.addView(v, index);
        } else {
            addView(v, index);
        }
    }

    public void setForceShowIcon(boolean forceShow) {
        mPreserveIconSpacing = mForceShowIcon = forceShow;
    }

    @Override
    public void setTitle(CharSequence title) {
        if (title != null) {
            mTitleView.setText(title);

            if (mTitleView.getVisibility() != VISIBLE) mTitleView.setVisibility(VISIBLE);
        } else {
            if (mTitleView.getVisibility() != GONE) mTitleView.setVisibility(GONE);
        }
    }

    @Override
    public MenuItemImpl getItemData() {
        return mItemData;
    }

    @Override
    public void setCheckable(boolean checkable) {
        if (!checkable && mRadioButton == null && mCheckBox == null) {
            return;
        }

        // Depending on whether its exclusive check or not, the checkbox or
        // radio button will be the one in use (and the other will be otherCompoundButton)
        final CompoundButton compoundButton;
        final CompoundButton otherCompoundButton;

        if (mItemData.isExclusiveCheckable()) {
            if (mRadioButton == null) {
                insertRadioButton();
            }
            compoundButton = mRadioButton;
            otherCompoundButton = mCheckBox;
        } else {
            if (mCheckBox == null) {
                insertCheckBox();
            }
            compoundButton = mCheckBox;
            otherCompoundButton = mRadioButton;
        }

        if (checkable) {
            compoundButton.setChecked(mItemData.isChecked());

            if (compoundButton.getVisibility() != VISIBLE) {
                compoundButton.setVisibility(VISIBLE);
            }

            // Make sure the other compound button isn't visible
            if (otherCompoundButton != null && otherCompoundButton.getVisibility() != GONE) {
                otherCompoundButton.setVisibility(GONE);
            }
        } else {
            if (mCheckBox != null) {
                mCheckBox.setVisibility(GONE);
            }
            if (mRadioButton != null) {
                mRadioButton.setVisibility(GONE);
            }
        }
    }

    @Override
    public void setChecked(boolean checked) {
        CompoundButton compoundButton;

        if (mItemData.isExclusiveCheckable()) {
            if (mRadioButton == null) {
                insertRadioButton();
            }
            compoundButton = mRadioButton;
        } else {
            if (mCheckBox == null) {
                insertCheckBox();
            }
            compoundButton = mCheckBox;
        }

        compoundButton.setChecked(checked);
    }

    private void setSubMenuArrowVisible(boolean hasSubmenu) {
        if (mSubMenuArrowView != null) {
            mSubMenuArrowView.setVisibility(hasSubmenu ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setShortcut(boolean showShortcut, char shortcutKey) {
        final int newVisibility = (showShortcut && mItemData.shouldShowShortcut())
                ? VISIBLE : GONE;

        if (newVisibility == VISIBLE) {
            mShortcutView.setText(mItemData.getShortcutLabel());
        }

        if (mShortcutView.getVisibility() != newVisibility) {
            mShortcutView.setVisibility(newVisibility);
        }
    }

    @Override
    public void setIcon(Drawable icon) {
        final boolean showIcon = mItemData.shouldShowIcon() || mForceShowIcon;
        if (!showIcon && !mPreserveIconSpacing) {
            return;
        }

        if (mIconView == null && icon == null && !mPreserveIconSpacing) {
            return;
        }

        if (mIconView == null) {
            insertIconView();
        }

        if (icon != null || mPreserveIconSpacing) {
            mIconView.setImageDrawable(showIcon ? icon : null);

            if (mIconView.getVisibility() != VISIBLE) {
                mIconView.setVisibility(VISIBLE);
            }
        } else {
            mIconView.setVisibility(GONE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIconView != null && mPreserveIconSpacing) {
            // Enforce minimum icon spacing
            ViewGroup.LayoutParams lp = getLayoutParams();
            LayoutParams iconLp = (LayoutParams) mIconView.getLayoutParams();
            if (lp.height > 0 && iconLp.width <= 0) {
                iconLp.width = lp.height;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void insertIconView() {
        LayoutInflater inflater = getInflater();
        mIconView = (ImageView) inflater.inflate(R.layout.abc_list_menu_item_icon,
                this, false);
        addContentView(mIconView, 0);
    }

    private void insertRadioButton() {
        LayoutInflater inflater = getInflater();
        mRadioButton =
                (RadioButton) inflater.inflate(R.layout.abc_list_menu_item_radio,
                        this, false);
        addContentView(mRadioButton);
    }

    private void insertCheckBox() {
        LayoutInflater inflater = getInflater();
        mCheckBox =
                (CheckBox) inflater.inflate(R.layout.abc_list_menu_item_checkbox,
                        this, false);
        addContentView(mCheckBox);
    }

    @Override
    public boolean prefersCondensedTitle() {
        return false;
    }

    @Override
    public boolean showsIcon() {
        return mForceShowIcon;
    }

    private LayoutInflater getInflater() {
        if (mInflater == null) {
            mInflater = LayoutInflater.from(getContext());
        }
        return mInflater;
    }

    /**
     * Enable or disable group dividers for this view.
     */
    public void setGroupDividerEnabled(boolean groupDividerEnabled) {
        // If mHasListDivider is true, disabling the groupDivider.
        // Otherwise, checking enbling it according to groupDividerEnabled flag.
        if (mGroupDivider != null) {
            mGroupDivider.setVisibility(!mHasListDivider
                    && groupDividerEnabled ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void adjustListItemSelectionBounds(Rect rect) {
        if (mGroupDivider != null && mGroupDivider.getVisibility() == View.VISIBLE) {
            // groupDivider is a part of ListMenuItemView.
            // If ListMenuItem with divider enabled is hovered/clicked, divider also gets selected.
            // Clipping the selector bounds from the top divider portion when divider is enabled,
            // so that divider does not get selected on hover or click.
            final LayoutParams lp = (LayoutParams) mGroupDivider.getLayoutParams();
            rect.top += mGroupDivider.getHeight() + lp.topMargin + lp.bottomMargin;
        }
    }
}

