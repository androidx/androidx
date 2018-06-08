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

package com.example.androidx.media;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

/**
 * Start activity for the VideoViewTest application.  This class manages the UI
 * which allows a user to select a video to play back.
 */
public class VideoSelector extends Activity {
    private ListView      mSelectList;
    private VideoItemList mSelectItems;
    private EditText      mUrlText;
    private CheckBox      mTextureViewCheckbox;
    private CheckBox      mLoopingCheckbox;
    private CheckBox      mAdvertisementCheckBox;

    private static final String TEST_VID_STASH = "/sdcard";
    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 100;

    private Intent createLaunchIntent(Context ctx, String url) {
        Intent ret_val = new Intent(ctx, VideoViewTest.class);
        ret_val.setData(Uri.parse(url));
        ret_val.putExtra(
                VideoViewTest.LOOPING_EXTRA_NAME, mLoopingCheckbox.isChecked());
        ret_val.putExtra(
                VideoViewTest.USE_TEXTURE_VIEW_EXTRA_NAME, mTextureViewCheckbox.isChecked());
        ret_val.putExtra(
                VideoViewTest.MEDIA_TYPE_ADVERTISEMENT, mAdvertisementCheckBox.isChecked());
        return ret_val;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_selector);

        mSelectList  = (ListView) findViewById(R.id.select_list);
        final Button playButton = (Button) findViewById(R.id.play_button);
        mUrlText = (EditText) findViewById(R.id.video_selection_input);
        mSelectItems = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                setUpInitialItemList();
            } else {
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                        EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
            }
        } else {
            setUpInitialItemList();
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent launch = createLaunchIntent(
                        VideoSelector.this,
                        mUrlText.getText().toString());
                startActivity(launch);
            }
        });
        mLoopingCheckbox = findViewById(R.id.looping_checkbox);
        mLoopingCheckbox.setChecked(false);
        mTextureViewCheckbox = findViewById(R.id.use_textureview_checkbox);
        mTextureViewCheckbox.setChecked(false);
        mAdvertisementCheckBox = findViewById(R.id.media_type_advertisement);
        mAdvertisementCheckBox.setChecked(false);
    }

    @Override
    public void onBackPressed() {
        if ((null != mSelectItems) && (null != mSelectHandler) && !mSelectItems.getIsRoot()) {
            mSelectHandler.onItemClick(null, null, 0, 0);
        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                setUpInitialItemList();
            }
        }
    }

    private void setUpInitialItemList() {
        mSelectItems = createVil(TEST_VID_STASH);
        mSelectList.setAdapter(mSelectItems);
        mSelectList.setOnItemClickListener(mSelectHandler);
    }

    /**
     * VideoItem is a class used to represent a selectable item on the listbox
     * used to select videos to playback.
     */
    private static class VideoItem {
        private final String mToStringName;
        private final String mName;
        private final String mUrl;
        private final boolean mIsDir;

        VideoItem(String name, String url, boolean isDir) {
            mName = name;
            mUrl  = url;
            mIsDir = isDir;

            if (isDir) {
                mToStringName = String.format("[dir] %s", name);
            } else {
                int ndx = url.indexOf(':');
                if (ndx > 0) {
                    mToStringName = String.format("[%s] %s", url.substring(0, ndx), name);
                } else {
                    mToStringName = name;
                }
            }
        }

        public static VideoItem createFromLinkFile(File f) {
            VideoItem retVal = null;

            try {
                BufferedReader rd = new BufferedReader(new FileReader(f));
                String name = rd.readLine();
                String url  = rd.readLine();
                if ((null != name) && (null != url)) {
                    retVal = new VideoItem(name, url, false);
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }

            return retVal;
        }

        @Override
        public String toString() {
            return mToStringName;
        }

        public String getName() {
            return mName;
        }

        public String getUrl() {
            return mUrl;
        }

        public boolean getIsDir() {
            return mIsDir;
        }
    }

    private OnItemClickListener mSelectHandler = new OnItemClickListener() {
        public void onItemClick(AdapterView parent,
                                View v,
                                int position,
                                long id) {
            if ((position >= 0) && (position < mSelectItems.getCount())) {
                VideoItem item = mSelectItems.getItem(position);
                if (item.getIsDir()) {
                    VideoItemList new_list = createVil(item.getUrl());
                    if (null != new_list) {
                        mSelectItems = new_list;
                        mSelectList.setAdapter(mSelectItems);
                    }
                } else {
                    Intent launch = createLaunchIntent(
                            VideoSelector.this,
                            item.getUrl());
                    startActivity(launch);
                }
            }
        }
    };

    /**
     * VideoItemList is an array adapter of video items used by the android
     * framework to populate the list of videos to select.
     */
    private class VideoItemList extends ArrayAdapter<VideoItem> {
        private final String mPath;
        private final boolean mIsRoot;

        private VideoItemList(String path, boolean isRoot) {
            super(VideoSelector.this,
                  R.layout.video_list_item,
                  R.id.video_list_item);
            mPath = path;
            mIsRoot = isRoot;
        }

        public String getPath() {
            return mPath;
        }

        public boolean getIsRoot() {
            return mIsRoot;
        }
    };

    private VideoItemList createVil(String p) {
        boolean is_root = TEST_VID_STASH.equals(p);

        File dir = new File(p);
        if (!dir.isDirectory() || !dir.canRead()) {
            return null;
        }

        VideoItemList retVal = new VideoItemList(p, is_root);

        // If this is not the root directory, go ahead and add the back link to
        // our parent.
        if (!is_root) {
            retVal.add(new VideoItem("..", dir.getParentFile().getAbsolutePath(), true));
        }

        // Make a sorted list of directories and files contained in this
        // directory.
        TreeMap<String, VideoItem> dirs  = new TreeMap<String, VideoItem>();
        TreeMap<String, VideoItem> files = new TreeMap<String, VideoItem>();

        File search_dir = new File(p);
        File[] flist = search_dir.listFiles();
        if (null == flist) {
            return retVal;
        }

        for (File f : flist) {
            if (f.canRead()) {
                if (f.isFile()) {
                    String fname = f.getName();
                    VideoItem newItem = null;

                    if (fname.endsWith(".url")) {
                        newItem = VideoItem.createFromLinkFile(f);
                    } else {
                        String url = "file://" + f.getAbsolutePath();
                        newItem = new VideoItem(fname, url, false);
                    }

                    if (null != newItem) {
                        files.put(newItem.getName(), newItem);
                    }
                } else if (f.isDirectory()) {
                    VideoItem newItem = new VideoItem(f.getName(), f.getAbsolutePath(), true);
                    dirs.put(newItem.getName(), newItem);
                }
            }
        }

        // now add the the sorted directories to the result set.
        for (VideoItem vi : dirs.values()) {
            retVal.add(vi);
        }

        // finally add the the sorted files to the result set.
        for (VideoItem vi : files.values()) {
            retVal.add(vi);
        }

        return retVal;
    }
}
