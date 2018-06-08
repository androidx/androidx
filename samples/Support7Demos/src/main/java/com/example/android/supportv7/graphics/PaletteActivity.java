/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.supportv7.graphics;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.cursoradapter.widget.ResourceCursorAdapter;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.palette.graphics.Palette;

import com.example.android.supportv7.R;

/**
 * Activity which displays the images from the device's {@link MediaStore}, alongside the generated
 * {@link androidx.palette.graphics.Palette} results.
 *
 * Allows the customization of the number of colors used in the palette generation, to demonstrate
 * the difference in results for different types of images.
 */
public class PaletteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new PaletteMediaStoreListFragment())
                .commit();
    }

    /**
     * The {@link androidx.fragment.app.ListFragment} which does all of the hard work.
     */
    public static class PaletteMediaStoreListFragment extends ListFragment
            implements LoaderManager.LoaderCallbacks<Cursor> {

        /**
         * Projection used for querying the {@link android.provider.MediaStore}.
         */
        static final String[] PROJECTION = {
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATE_ADDED
        };

        private PhotosCursorAdapter mAdapter;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // Enable fast scroll to make it easier to navigate large number of images
            getListView().setFastScrollEnabled(true);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Create an Adapter and use a new Adapter
            mAdapter = new PhotosCursorAdapter(getActivity(), null);
            mAdapter.setNumColors(16);
            setListAdapter(mAdapter);

            // Start the loader manager to create our CursorLoader
            getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.sample_palette_actions, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_num_colors_8:
                    mAdapter.setNumColors(8);
                    item.setChecked(true);
                    return true;
                case R.id.menu_num_colors_12:
                    mAdapter.setNumColors(12);
                    item.setChecked(true);
                    return true;
                case R.id.menu_num_colors_16:
                    mAdapter.setNumColors(16);
                    item.setChecked(true);
                    return true;
                case R.id.menu_num_colors_24:
                    mAdapter.setNumColors(24);
                    item.setChecked(true);
                    return true;
                case R.id.menu_num_colors_32:
                    mAdapter.setNumColors(32);
                    item.setChecked(true);
                    return true;
            }

            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendEncodedPath(String.valueOf(id)).build();

            // Start the Detail Activity
            Intent intent = new Intent(getActivity(), PaletteDetailActivity.class);
            intent.setData(uri);
            startActivity(intent);
        }

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
            return new CursorLoader(
                    getActivity(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION,
                    null,
                    null,
                    MediaStore.Images.ImageColumns.DATE_ADDED + " DESC");
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
            mAdapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
            mAdapter.swapCursor(null);
        }

        private static class PhotosCursorAdapter extends ResourceCursorAdapter {

            private int mNumColors;

            public PhotosCursorAdapter(Context context, Cursor c) {
                super(context, R.layout.palette_list_item, c, false);
                mContext = context;
            }

            /**
             * Set the number of colors used for {@link Palette} generation.
             */
            void setNumColors(int numColors) {
                mNumColors = numColors;
                notifyDataSetChanged();
            }

            @Override
            public void bindView(final View view, Context context, Cursor cursor) {
                // Let's reset the view, clearing the ImageView and resetting the background colors
                // of the Palette UI
                ImageView imageView = (ImageView) view.findViewById(R.id.image);
                imageView.setImageDrawable(null);

                ViewCompat.setBackground(view.findViewById(R.id.text_vibrant), null);
                ViewCompat.setBackground(view.findViewById(R.id.text_muted), null);
                ViewCompat.setBackground(view.findViewById(R.id.text_light_vibrant), null);
                ViewCompat.setBackground(view.findViewById(R.id.text_light_muted), null);
                ViewCompat.setBackground(view.findViewById(R.id.text_dark_vibrant), null);
                ViewCompat.setBackground(view.findViewById(R.id.text_dark_muted), null);

                final long id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID));

                ImageLoader.loadMediaStoreThumbnail(imageView, id, new ImageLoader.Listener() {
                    @Override
                    public void onImageLoaded(Bitmap bitmap) {
                        new Palette.Builder(bitmap).maximumColorCount(mNumColors).generate(
                                new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(Palette palette) {
                                        setBackgroundColor(
                                                view.findViewById(R.id.text_vibrant),
                                                palette.getVibrantSwatch());
                                        setBackgroundColor(
                                                view.findViewById(R.id.text_muted),
                                                palette.getMutedSwatch());
                                        setBackgroundColor(
                                                view.findViewById(R.id.text_light_vibrant),
                                                palette.getLightVibrantSwatch());
                                        setBackgroundColor(
                                                view.findViewById(R.id.text_light_muted),
                                                palette.getLightMutedSwatch());
                                        setBackgroundColor(
                                                view.findViewById(R.id.text_dark_vibrant),
                                                palette.getDarkVibrantSwatch());
                                        setBackgroundColor(
                                                view.findViewById(R.id.text_dark_muted),
                                                palette.getDarkMutedSwatch());
                                    }
                                });
                    }
                });
            }
        }

        static void setBackgroundColor(View view, Palette.Swatch swatch) {
            if (view != null && swatch != null) {
                view.setBackgroundColor(swatch.getRgb());
            }
        }

    }

}