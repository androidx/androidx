/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.leanback;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import java.util.Random;

public class BrowseAnimationFragment extends
        androidx.leanback.app.BrowseFragment {
    private static final String TAG = "leanback.BrowseAnimationFragment";

    private static final int NUM_ROWS = 10;
    private ArrayObjectAdapter mRowsAdapter;
    private static Random sRand = new Random();

    static class Item {
        final String mText;
        final OnItemViewClickedListener mAction;

        Item(String text, OnItemViewClickedListener action) {
            mText = text;
            mAction = action;
        }

        @Override
        public String toString() {
            return mText;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setBadgeDrawable(ResourcesCompat.getDrawable(getActivity().getResources(),
                R.drawable.ic_title, getActivity().getTheme()));
        setTitle("Leanback Sample App");
        setHeadersState(HEADERS_ENABLED);

        setOnSearchClickedListener(new View.OnClickListener() {
                @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setupRows();
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private void setupRows() {
        ListRowPresenter lrp = new ListRowPresenter();
        mRowsAdapter = new ArrayObjectAdapter(lrp);
        for (int i = 0; i < NUM_ROWS; ++i) {
            mRowsAdapter.add(
                    createRandomRow(new HeaderItem(i, "Row " + i)));
        }
        setAdapter(mRowsAdapter);
    }

    Item createRandomItem() {
        switch (sRand.nextInt(15)) {
        default:
        case 0:
            return new Item("Remove Item before", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    ArrayObjectAdapter adapter = ((ArrayObjectAdapter) ((ListRow) row)
                            .getAdapter());
                    int index = adapter.indexOf(item);
                    if (index >= 0) {
                        if (index > 0)
                            index--;
                        adapter.removeItems(index, 1);
                    }
                }
            });
        case 1:
            return new Item("Remove Item after", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    ArrayObjectAdapter adapter = ((ArrayObjectAdapter) ((ListRow) row)
                            .getAdapter());
                    int index = adapter.indexOf(item);
                    if (index >= 0) {
                        if (index < adapter.size() - 1)
                            index++;
                        adapter.removeItems(index, 1);
                    }
                }
            });
        case 2:
            return new Item("Remove Item", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    ArrayObjectAdapter adapter = ((ArrayObjectAdapter) ((ListRow) row)
                            .getAdapter());
                    int index = adapter.indexOf(item);
                    if (index >= 0) {
                        adapter.removeItems(index, 1);
                    }
                }
            });
        case 3:
            return new Item("Remove all Items", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    ArrayObjectAdapter adapter = ((ArrayObjectAdapter) ((ListRow) row)
                            .getAdapter());
                    adapter.clear();
                }
            });
        case 4:
            return new Item("add item before", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    ArrayObjectAdapter adapter = ((ArrayObjectAdapter) ((ListRow) row)
                            .getAdapter());
                    int index = adapter.indexOf(item);
                    if (index >= 0) {
                        adapter.add(index, createRandomItem());
                    }
                }
            });
        case 5:
            return new Item("add item after", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    ArrayObjectAdapter adapter = ((ArrayObjectAdapter) ((ListRow) row)
                            .getAdapter());
                    int index = adapter.indexOf(item);
                    if (index >= 0) {
                        adapter.add(index + 1, createRandomItem());
                    }
                }
            });
        case 6:
            return new Item("add random items before",
                    new OnItemViewClickedListener() {
                            @Override
                        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                RowPresenter.ViewHolder rowViewHolder, Row row) {
                            ArrayObjectAdapter adapter = ((ArrayObjectAdapter) ((ListRow) row)
                                    .getAdapter());
                            int index = adapter.indexOf(item);
                            if (index >= 0) {
                                int count = sRand.nextInt(4) + 1;
                                for (int i = 0; i < count; i++) {
                                    adapter.add(index + i, createRandomItem());
                                }
                            }
                        }
                    });
        case 7:
            return new Item("add random items after",
                    new OnItemViewClickedListener() {
                            @Override
                        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                RowPresenter.ViewHolder rowViewHolder, Row row) {
                            ArrayObjectAdapter adapter = ((ArrayObjectAdapter) ((ListRow) row)
                                    .getAdapter());
                            int index = adapter.indexOf(item);
                            if (index >= 0) {
                                int count = sRand.nextInt(4) + 1;
                                for (int i = 0; i < count; i++) {
                                    adapter.add(index + 1 + i,
                                            createRandomItem());
                                }
                            }
                        }
                    });
        case 8:
            return new Item("add row before", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int index = mRowsAdapter.indexOf(row);
                    if (index >= 0) {
                        int headerId = sRand.nextInt();
                        mRowsAdapter.add(index, createRandomRow(new HeaderItem(
                                headerId, "Row " + headerId)));
                    }
                }
            });
        case 9:
            return new Item("add row after", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int index = mRowsAdapter.indexOf(row);
                    if (index >= 0) {
                        int headerId = sRand.nextInt();
                        mRowsAdapter.add(
                                index + 1, createRandomRow(new HeaderItem(
                                        headerId, "Row " + headerId)));
                    }
                }
            });
        case 10:
            return new Item("delete row", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    mRowsAdapter.remove(row);
                }
            });
        case 11:
            return new Item("delete row before", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int index = mRowsAdapter.indexOf(row);
                    if (index > 0) {
                        mRowsAdapter.removeItems(index - 1, 1);
                    }
                }
            });
        case 12:
            return new Item("delete row after", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int index = mRowsAdapter.indexOf(row);
                    if (index < mRowsAdapter.size() - 1) {
                        mRowsAdapter.removeItems(index + 1, 1);
                    }
                }
            });
        case 13:
            return new Item("Replace Item before", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    ArrayObjectAdapter adapter = ((ArrayObjectAdapter) ((ListRow) row)
                            .getAdapter());
                    int index = adapter.indexOf(item);
                    if (index >= 0) {
                        if (index > 0)
                            index--;
                        adapter.replace(index, createRandomItem());
                    }
                }
            });
        case 14:
            return new Item("Remove all then re-add", new OnItemViewClickedListener() {
                    @Override
                public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                        RowPresenter.ViewHolder rowViewHolder, Row row) {
                    final ArrayObjectAdapter adapter = ((ArrayObjectAdapter) ((ListRow) row)
                            .getAdapter());
                   adapter.clear();
                   new Handler().postDelayed(new Runnable() {
                       @Override
                       public void run() {
                           adapter.add(0, createRandomItem());
                       }
                   }, 1000);
                }
            });
        }
    }

    ListRow createRandomRow(HeaderItem header) {
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(
                new StringPresenter());
        for (int i = 0; i < 8; i++) {
            listRowAdapter.add(createRandomItem());
        }
        return new ListRow(header, listRowAdapter);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            ((Item) item).mAction.onItemClicked(itemViewHolder, item, rowViewHolder, row);
        }
    }
}
