/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.browser.browseractions;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.browser.R;
import androidx.core.widget.TextViewCompat;

import java.util.List;

/**
 * The class to show fallback menu for Browser Actions if no provider is available.
 */
class BrowserActionsFallbackMenuUi implements AdapterView.OnItemClickListener {
    /** @hide */
    @VisibleForTesting
    @RestrictTo(LIBRARY_GROUP)
    interface BrowserActionsFallMenuUiListener {
        void onMenuShown(View view);
    }

    private static final String TAG = "BrowserActionskMenuUi";

    private final Context mContext;
    private final Uri mUri;
    private final List<BrowserActionItem> mMenuItems;

    private BrowserActionsFallMenuUiListener mMenuUiListener;

    private BrowserActionsFallbackMenuDialog mBrowserActionsDialog;

    /**
     * @param context The {@link Context} used to show the fallback menu.
     * @param uri The uri which users click to trigger the menu.
     * @param menuItems The custom menu items shown in the menu.
     */
    BrowserActionsFallbackMenuUi(
            Context context, Uri uri, List<BrowserActionItem> menuItems) {
        mContext = context;
        mUri = uri;
        mMenuItems = menuItems;
    }

    /** @hide */
    @VisibleForTesting
    @RestrictTo(LIBRARY_GROUP)
    void setMenuUiListener(BrowserActionsFallMenuUiListener menuUiListener) {
        mMenuUiListener = menuUiListener;
    }

    /**
     * Shows the fallback menu.
     */
    public void displayMenu() {
        final View view = LayoutInflater.from(mContext).inflate(
                R.layout.browser_actions_context_menu_page, null);
        mBrowserActionsDialog = new BrowserActionsFallbackMenuDialog(mContext, initMenuView(view));
        mBrowserActionsDialog.setContentView(view);
        if (mMenuUiListener != null) {
            mBrowserActionsDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    mMenuUiListener.onMenuShown(view);
                }
            });
        }
        mBrowserActionsDialog.show();
    }

    private BrowserActionsFallbackMenuView initMenuView(View view) {
        BrowserActionsFallbackMenuView menuView =
                (BrowserActionsFallbackMenuView) view.findViewById(R.id.browser_actions_menu_view);

        final TextView urlTextView = (TextView) view.findViewById(R.id.browser_actions_header_text);
        urlTextView.setText(mUri.toString());
        urlTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextViewCompat.getMaxLines(urlTextView) == Integer.MAX_VALUE) {
                    urlTextView.setMaxLines(1);
                    urlTextView.setEllipsize(TextUtils.TruncateAt.END);
                } else {
                    urlTextView.setMaxLines(Integer.MAX_VALUE);
                    urlTextView.setEllipsize(null);
                }
            }
        });

        ListView menuListView = (ListView) view.findViewById(R.id.browser_actions_menu_items);
        BrowserActionsFallbackMenuAdapter adapter =
                new BrowserActionsFallbackMenuAdapter(mMenuItems, mContext);
        menuListView.setAdapter(adapter);
        menuListView.setOnItemClickListener(this);

        return menuView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PendingIntent action = mMenuItems.get(position).getAction();
        try {
            action.send();
            mBrowserActionsDialog.dismiss();
        } catch (CanceledException e) {
            Log.e(TAG, "Failed to send custom item action", e);
        }
    }
}
