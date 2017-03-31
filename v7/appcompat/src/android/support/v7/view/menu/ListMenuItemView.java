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

package android.support.v7.view.menu;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.RestrictTo;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.support.v7.widget.TintTypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

/**
 * The item view for each item in the ListView-based MenuViews.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ListMenuItemView extends LinearLayout implements MenuView.ItemView {
    private static final String TAG = "ListMenuItemView";
    private MenuItemImpl mItemData;

    private ImageView mIconView;
    private RadioButton mRadioButton;
    private TextView mTitleView;
    private CheckBox mCheckBox;
    private TextView mShortcutView;
    private ImageView mSubMenuArrowView;

    private Drawable mBackground;
    private int mTextAppearance;
    private Context mTextAppearanceContext;
    private boolean mPreserveIconSpacing;
    private Drawable mSubMenuArrow;

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

        a.recycle();
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

            final int newVisibility = checkable ? VISIBLE : GONE;
            if (compoundButton.getVisibility() != newVisibility) {
                compoundButton.setVisibility(newVisibility);
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
        addView(mIconView, 0);
    }

    private void insertRadioButton() {
        LayoutInflater inflater = getInflater();
        mRadioButton =
                (RadioButton) inflater.inflate(R.layout.abc_list_menu_item_radio,
                        this, false);
        addView(mRadioButton);
    }

    private void insertCheckBox() {
        LayoutInflater inflater = getInflater();
        mCheckBox =
                (CheckBox) inflater.inflate(R.layout.abc_list_menu_item_checkbox,
                        this, false);
        addView(mCheckBox);
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
}

