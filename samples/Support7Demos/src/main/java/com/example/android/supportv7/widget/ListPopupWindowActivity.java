/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.supportv7.widget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ListPopupWindow;

import com.example.android.supportv7.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ListPopupWindowActivity extends AppCompatActivity {
    private ViewGroup mContainer;

    private CheckBox mIsModal;

    private TextView mLog;

    private Button mButton;

    private ListPopupWindow mListPopupWindow;

    private BaseAdapter mListPopupAdapter;

    private SimpleDateFormat mDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_popup_window_activity);

        mDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

        mContainer = findViewById(R.id.container);
        mIsModal = (CheckBox) mContainer.findViewById(R.id.is_modal);
        mLog = (TextView) mContainer.findViewById(R.id.log);
        mButton = (Button) mContainer.findViewById(R.id.test_button);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListPopupWindow = new ListPopupWindow(mContainer.getContext());

                final String[] POPUP_CONTENT =
                        new String[] { "Alice", "Bob", "Charlie", "Deirdre", "El" };
                mListPopupAdapter = new BaseAdapter() {
                    class ViewHolder {
                        private TextView title;
                        private TextView shortcut;
                    }

                    @Override
                    public int getCount() {
                        return POPUP_CONTENT.length;
                    }

                    @Override
                    public Object getItem(int position) {
                        return POPUP_CONTENT[position];
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        if (convertView == null) {
                            convertView = LayoutInflater.from(parent.getContext()).inflate(
                                    R.layout.abc_popup_menu_item_layout, parent, false);
                            ViewHolder viewHolder = new ViewHolder();
                            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                            viewHolder.shortcut =
                                    (TextView) convertView.findViewById(R.id.shortcut);
                            convertView.setTag(viewHolder);
                        }

                        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
                        viewHolder.title.setText(POPUP_CONTENT[position]);
                        viewHolder.shortcut.setVisibility(View.GONE);
                        return convertView;
                    }
                };

                mListPopupWindow.setAdapter(mListPopupAdapter);
                mListPopupWindow.setAnchorView(mButton);

                // Register a listener to be notified when an item in our popup window has
                // been clicked.
                mListPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
                        addToLog("Item #"+ position + " clicked");
                        addToLog("Dismissing popup window");
                        mListPopupWindow.dismiss();
                    }
                });

                // Register a listener to be notified when our popup window is dismissed.
                mListPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        addToLog("Popup window dismissed");
                    }
                });

                // Set popup window modality based on the current checkbox state.
                mListPopupWindow.setModal(mIsModal.isChecked());

                // and show it
                mListPopupWindow.show();
            }
        });

        // Set up a click listener on the log text view. When the popup window is in modal
        // mode and is dismissed by tapping outside of its bounds *and* over the log text
        // view bounds, we should *not* get this click listener invoked.
        mLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToLog("Log view clicked");
            }
        });
    }

    private void addToLog(String toLog) {
        String toPrepend = mDateFormat.format(new Date()) + " " + toLog + "\n";
        mLog.setText(toPrepend + mLog.getText());
    }
}
