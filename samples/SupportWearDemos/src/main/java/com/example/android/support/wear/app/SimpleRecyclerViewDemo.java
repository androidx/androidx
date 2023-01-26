package com.example.android.support.wear.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.support.wear.R;

/**
 * Main activity for the RecyclerView demo.
 */
public class SimpleRecyclerViewDemo extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rv_demo);
        RecyclerView rv = findViewById(R.id.rv_container);

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new SimpleRecyclerViewDemo.DemoAdapter());
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mView;
        ViewHolder(TextView itemView) {
            super(itemView);
            mView = itemView;
        }
    }

    private static class DemoAdapter extends RecyclerView.Adapter<ViewHolder> {
        private static final int ITEM_COUNT = 100;
        private static final int ELEMENT_HEIGHT_DP = 50;
        private static final int ELEMENT_TEXT_SIZE = 14;

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView view = new TextView(parent.getContext());
            view.setHeight(ELEMENT_HEIGHT_DP);
            view.setTextSize(ELEMENT_TEXT_SIZE);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mView.setText("Holder at position " + position);
            holder.mView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return ITEM_COUNT;
        }
    }
}