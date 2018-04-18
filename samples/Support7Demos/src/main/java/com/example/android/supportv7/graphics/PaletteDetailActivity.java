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

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import com.example.android.supportv7.R;

import java.util.List;

/**
 * Activity which displays the more details about a generated {@link Palette} for a specific
 * {@link android.provider.MediaStore} image.
 *
 * Displays the full generated palette of colors in a grid, which allows clicking on an palette item
 * to display more information in a {@link Toast}.
 *
 * Also allows the customization of the number of colors used in the palette generation for
 * demonstration purposes.
 */
public class PaletteDetailActivity extends AppCompatActivity {

    private ImageView mImageView;
    private GridView mGridView;
    private SwatchesPalette mSwatchesPalette;

    private Uri mImageUri;

    private Toast mCurrentToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palette_activity_detail);

        mImageUri = getIntent().getData();

        mImageView = findViewById(R.id.image);
        mGridView = findViewById(R.id.palette);
        mSwatchesPalette = new SwatchesPalette();
        mGridView.setAdapter(mSwatchesPalette);

        // Set an OnItemClickListener to display a information Toast when a Palette item is clicked
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                // Cancel the current Toast if there is already one being displayed
                if (mCurrentToast != null) {
                    mCurrentToast.cancel();
                }

                final Palette.Swatch item = (Palette.Swatch) adapterView.getItemAtPosition(pos);
                mCurrentToast = Toast.makeText(PaletteDetailActivity.this,
                        item.toString(), Toast.LENGTH_LONG);
                mCurrentToast.show();
            }
        });

        // Load the image with a default number of colors
        loadImage(16);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample_palette_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_num_colors_8:
                loadImage(8);
                item.setChecked(true);
                return true;
            case R.id.menu_num_colors_12:
                loadImage(12);
                item.setChecked(true);
                return true;
            case R.id.menu_num_colors_16:
                loadImage(16);
                item.setChecked(true);
                return true;
            case R.id.menu_num_colors_24:
                loadImage(24);
                item.setChecked(true);
                return true;
            case R.id.menu_num_colors_32:
                loadImage(32);
                item.setChecked(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadImage(final int numColors) {
        final int id = Integer.parseInt(mImageUri.getLastPathSegment());

        ImageLoader.loadMediaStoreThumbnail(mImageView, id, new ImageLoader.Listener() {
            @Override
            public void onImageLoaded(Bitmap bitmap) {
                new Palette.Builder(bitmap).maximumColorCount(numColors).generate(
                        new Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                                populatePalette(palette);
                            }
                        });
            }
        });
    }

    private class SwatchesPalette extends BaseAdapter {

        private List<Palette.Swatch> mSwatches;

        @Override
        public int getCount() {
            return mSwatches != null ? mSwatches.size() : 0;
        }

        @Override
        public Palette.Swatch getItem(int position) {
            return mSwatches.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        void setSwatches(List<Palette.Swatch> palette) {
            mSwatches = palette;
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.palette_grid_item, parent, false);
            }
            setBackgroundColor(view, getItem(position));
            return view;
        }
    }

    private void populatePalette(Palette palette) {
        mSwatchesPalette.setSwatches(palette.getSwatches());

        setBackgroundColor(findViewById(R.id.text_vibrant), palette.getVibrantSwatch());
        setBackgroundColor(findViewById(R.id.text_muted), palette.getMutedSwatch());
        setBackgroundColor(findViewById(R.id.text_light_vibrant), palette.getLightVibrantSwatch());
        setBackgroundColor(findViewById(R.id.text_light_muted), palette.getLightMutedSwatch());
        setBackgroundColor(findViewById(R.id.text_dark_vibrant), palette.getDarkVibrantSwatch());
        setBackgroundColor(findViewById(R.id.text_dark_muted), palette.getDarkMutedSwatch());
    }

    private void setBackgroundColor(View view, Palette.Swatch swatch) {
        if (view != null && swatch != null) {
            view.setBackgroundColor(swatch.getRgb());
        }
    }

}