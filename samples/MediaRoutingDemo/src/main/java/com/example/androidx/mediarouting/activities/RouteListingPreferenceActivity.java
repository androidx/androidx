/*
 * Copyright 2023 The Android Open Source Project
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

package com.example.androidx.mediarouting.activities;

import static androidx.mediarouter.media.RouteListingPreference.Item.FLAG_ONGOING_SESSION;
import static androidx.mediarouter.media.RouteListingPreference.Item.FLAG_ONGOING_SESSION_MANAGED;
import static androidx.mediarouter.media.RouteListingPreference.Item.FLAG_SUGGESTED;
import static androidx.mediarouter.media.RouteListingPreference.Item.SUBTEXT_CUSTOM;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.RouteListingPreference;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidx.mediarouting.R;
import com.example.androidx.mediarouting.RoutesManager;
import com.example.androidx.mediarouting.RoutesManager.RouteListingPreferenceItemHolder;
import com.example.androidx.mediarouting.ui.UiUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Allows the user to manage the route listing preference of this app. */
public class RouteListingPreferenceActivity extends AppCompatActivity {

    private RoutesManager mRoutesManager;
    private RecyclerView mRouteListingPreferenceRecyclerView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < 34) {
            Toast.makeText(
                            /* context= */ this,
                            "Route Listing Preference requires Android U+",
                            Toast.LENGTH_LONG)
                    .show();
            finish();
            return;
        }

        setContentView(R.layout.activity_route_listing_preference);

        mRoutesManager = RoutesManager.getInstance(/* context= */ this);

        Switch preferSystemOrderingSwitch = findViewById(R.id.prefer_system_ordering_switch);
        preferSystemOrderingSwitch.setChecked(
                mRoutesManager.getRouteListingSystemOrderingPreferred());
        preferSystemOrderingSwitch.setOnCheckedChangeListener(
                (unusedButton, isChecked) -> {
                    mRoutesManager.setRouteListingSystemOrderingPreferred(isChecked);
                });
        preferSystemOrderingSwitch.setEnabled(mRoutesManager.isRouteListingPreferenceEnabled());

        Switch enableRouteListingPreferenceSwitch =
                findViewById(R.id.enable_route_listing_preference_switch);
        enableRouteListingPreferenceSwitch.setChecked(
                mRoutesManager.isRouteListingPreferenceEnabled());
        enableRouteListingPreferenceSwitch.setOnCheckedChangeListener(
                (unusedButton, isChecked) -> {
                    mRoutesManager.setRouteListingPreferenceEnabled(isChecked);
                    preferSystemOrderingSwitch.setEnabled(isChecked);
                });

        mRouteListingPreferenceRecyclerView =
                findViewById(R.id.route_listing_preference_recycler_view);
        new ItemTouchHelper(new RecyclerViewCallback())
                .attachToRecyclerView(mRouteListingPreferenceRecyclerView);
        mRouteListingPreferenceRecyclerView.setLayoutManager(
                new LinearLayoutManager(/* context= */ this));
        mRouteListingPreferenceRecyclerView.setHasFixedSize(true);
        mRouteListingPreferenceRecyclerView.setAdapter(
                new RouteListingPreferenceRecyclerViewAdapter());

        FloatingActionButton newRouteButton =
                findViewById(R.id.new_route_listing_preference_item_button);
        newRouteButton.setOnClickListener(
                view ->
                        setUpRouteListingPreferenceItemEditionDialog(
                                mRoutesManager.getRouteListingPreferenceItems().size()));
    }

    private void setUpRouteListingPreferenceItemEditionDialog(int itemPositionInList) {
        List<RouteListingPreferenceItemHolder> routeListingPreference =
                mRoutesManager.getRouteListingPreferenceItems();
        List<MediaRouter.RouteInfo> routesWithNoAssociatedListingPreferenceItem =
                getRoutesWithNoAssociatedListingPreferenceItem();
        if (itemPositionInList == routeListingPreference.size()
                && routesWithNoAssociatedListingPreferenceItem.isEmpty()) {
            Toast.makeText(/* context= */ this, "No (more) routes available", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        View dialogView =
                getLayoutInflater()
                        .inflate(R.layout.route_listing_preference_item_dialog, /* root= */ null);

        Spinner routeSpinner = dialogView.findViewById(R.id.rlp_item_dialog_route_name_spinner);
        List<RouteListingPreferenceItemHolder> spinnerEntries = new ArrayList<>();

        Spinner selectionBehaviorSpinner =
                dialogView.findViewById(R.id.rlp_item_dialog_selection_behavior_spinner);
        UiUtils.setUpEnumBasedSpinner(
                /* context= */ this,
                selectionBehaviorSpinner,
                RouteListingPreferenceItemSelectionBehavior.SELECTION_BEHAVIOR_TRANSFER,
                (unused) -> {});

        CheckBox ongoingSessionCheckBox =
                dialogView.findViewById(R.id.rlp_item_dialog_ongoing_session_checkbox);
        CheckBox sessionManagedCheckBox =
                dialogView.findViewById(R.id.rlp_item_dialog_session_managed_checkbox);
        CheckBox suggestedRouteCheckBox =
                dialogView.findViewById(R.id.rlp_item_dialog_suggested_checkbox);

        Spinner subtextSpinner = dialogView.findViewById(R.id.rlp_item_dialog_subtext_spinner);
        UiUtils.setUpEnumBasedSpinner(
                /* context= */ this,
                subtextSpinner,
                RouteListingPreferenceItemSubtext.SUBTEXT_NONE,
                (unused) -> {});

        if (itemPositionInList < routeListingPreference.size()) {
            RouteListingPreferenceItemHolder itemHolder =
                    routeListingPreference.get(itemPositionInList);
            spinnerEntries.add(itemHolder);
            int selectionBehaviorOrdinalIndex =
                    RouteListingPreferenceItemSelectionBehavior.fromConstant(
                                    itemHolder.mItem.getSelectionBehavior())
                            .ordinal();
            selectionBehaviorSpinner.setSelection(selectionBehaviorOrdinalIndex);
            ongoingSessionCheckBox.setChecked(itemHolder.hasFlag(FLAG_ONGOING_SESSION));
            sessionManagedCheckBox.setChecked(itemHolder.hasFlag(FLAG_ONGOING_SESSION_MANAGED));
            suggestedRouteCheckBox.setChecked(itemHolder.hasFlag(FLAG_SUGGESTED));
            int subtextOrdinalIndex =
                    RouteListingPreferenceItemSubtext.fromConstant(itemHolder.mItem.getSubText())
                            .ordinal();
            subtextSpinner.setSelection(subtextOrdinalIndex);
        }
        for (MediaRouter.RouteInfo routeInfo : routesWithNoAssociatedListingPreferenceItem) {
            spinnerEntries.add(
                    new RouteListingPreferenceItemHolder(
                            new RouteListingPreference.Item.Builder(routeInfo.getId()).build(),
                            routeInfo.getName()));
        }
        routeSpinner.setAdapter(
                new ArrayAdapter<>(
                        /* context= */ this, android.R.layout.simple_spinner_item, spinnerEntries));

        AlertDialog editRlpItemDialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setPositiveButton(
                                "Accept",
                                (unusedDialog, unusedWhich) -> {
                                    RouteListingPreferenceItemHolder item =
                                            (RouteListingPreferenceItemHolder)
                                                    routeSpinner.getSelectedItem();
                                    RouteListingPreferenceItemSelectionBehavior selectionBehavior =
                                            (RouteListingPreferenceItemSelectionBehavior)
                                                    selectionBehaviorSpinner.getSelectedItem();
                                    int flags = 0;
                                    flags |=
                                            ongoingSessionCheckBox.isChecked()
                                                    ? FLAG_ONGOING_SESSION
                                                    : 0;
                                    flags |=
                                            sessionManagedCheckBox.isChecked()
                                                    ? FLAG_ONGOING_SESSION_MANAGED
                                                    : 0;
                                    flags |=
                                            suggestedRouteCheckBox.isChecked() ? FLAG_SUGGESTED : 0;
                                    RouteListingPreferenceItemSubtext subtext =
                                            (RouteListingPreferenceItemSubtext)
                                                    subtextSpinner.getSelectedItem();
                                    onEditRlpItemDialogAccepted(
                                            item.mItem.getRouteId(),
                                            item.mRouteName,
                                            selectionBehavior.mConstant,
                                            flags,
                                            subtext.mConstant,
                                            itemPositionInList);
                                })
                        .setNegativeButton("Dismiss", (unusedDialog, unusedWhich) -> {})
                        .create();

        editRlpItemDialog.show();
    }

    private void onEditRlpItemDialogAccepted(
            String routeId,
            String routeName,
            int selectionBehavior,
            int flags,
            int subtext,
            int itemPositionInList) {
        ArrayList<RouteListingPreferenceItemHolder> newRouteListingPreference =
                new ArrayList<>(mRoutesManager.getRouteListingPreferenceItems());
        RecyclerView.Adapter<?> adapter = mRouteListingPreferenceRecyclerView.getAdapter();
        RouteListingPreference.Item.Builder newItemBuilder =
                new RouteListingPreference.Item.Builder(routeId)
                        .setFlags(flags)
                        .setSelectionBehavior(selectionBehavior)
                        .setSubText(subtext);
        if (subtext == SUBTEXT_CUSTOM) {
            newItemBuilder.setCustomSubtextMessage("A custom subtext");
        }
        RouteListingPreference.Item newItem = newItemBuilder.build();
        RouteListingPreferenceItemHolder newItemAndNamePair =
                new RouteListingPreferenceItemHolder(newItem, routeName);
        if (itemPositionInList < newRouteListingPreference.size()) {
            newRouteListingPreference.set(itemPositionInList, newItemAndNamePair);
            adapter.notifyItemChanged(itemPositionInList);
        } else {
            newRouteListingPreference.add(newItemAndNamePair);
            adapter.notifyItemInserted(itemPositionInList);
        }
        mRoutesManager.setRouteListingPreferenceItems(newRouteListingPreference);
    }

    @NonNull
    private ImmutableList<MediaRouter.RouteInfo> getRoutesWithNoAssociatedListingPreferenceItem() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return ImmutableList.of();
        }
        Set<String> routesWithAssociatedRouteListingPreferenceItem = new HashSet<>();
        for (RouteListingPreferenceItemHolder element :
                mRoutesManager.getRouteListingPreferenceItems()) {
            String routeId = element.mItem.getRouteId();
            routesWithAssociatedRouteListingPreferenceItem.add(routeId);
        }

        ImmutableList.Builder<MediaRouter.RouteInfo> resultBuilder = ImmutableList.builder();
        for (MediaRouter.RouteInfo route : MediaRouter.getInstance(this).getRoutes()) {
            if (!routesWithAssociatedRouteListingPreferenceItem.contains(route.getId())) {
                resultBuilder.add(route);
            }
        }
        return resultBuilder.build();
    }

    private class RecyclerViewCallback extends ItemTouchHelper.SimpleCallback {

        private static final int INDEX_UNSET = -1;

        private int mDraggingFromPosition;
        private int mDraggingToPosition;

        private RecyclerViewCallback() {
            super(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                    ItemTouchHelper.START | ItemTouchHelper.END);
            mDraggingFromPosition = INDEX_UNSET;
            mDraggingToPosition = INDEX_UNSET;
        }

        @Override
        public boolean onMove(
                @NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder origin,
                @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = origin.getBindingAdapterPosition();
            int toPosition = target.getBindingAdapterPosition();
            if (mDraggingFromPosition == INDEX_UNSET) {
                // A drag has started, but we wait for the clearView() call to update the route
                // listing preference.
                mDraggingFromPosition = fromPosition;
            }
            mDraggingToPosition = toPosition;
            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            ArrayList<RouteListingPreferenceItemHolder> newRouteListingPreference =
                    new ArrayList<>(mRoutesManager.getRouteListingPreferenceItems());
            int itemPosition = viewHolder.getBindingAdapterPosition();
            newRouteListingPreference.remove(itemPosition);
            mRoutesManager.setRouteListingPreferenceItems(newRouteListingPreference);
            viewHolder.getBindingAdapter().notifyItemRemoved(itemPosition);
        }

        @Override
        public void clearView(
                @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (mDraggingFromPosition != INDEX_UNSET) {
                ArrayList<RouteListingPreferenceItemHolder> newRouteListingPreference =
                        new ArrayList<>(mRoutesManager.getRouteListingPreferenceItems());
                newRouteListingPreference.add(
                        mDraggingToPosition,
                        newRouteListingPreference.remove(mDraggingFromPosition));
                mRoutesManager.setRouteListingPreferenceItems(newRouteListingPreference);
            }
            mDraggingFromPosition = INDEX_UNSET;
            mDraggingToPosition = INDEX_UNSET;
        }
    }

    private class RouteListingPreferenceRecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewItemViewHolder> {
        @NonNull
        @Override
        public RecyclerViewItemViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            TextView textView =
                    (TextView)
                            LayoutInflater.from(parent.getContext())
                                    .inflate(
                                            android.R.layout.simple_list_item_1,
                                            parent,
                                            /* attachToRoot= */ false);
            return new RecyclerViewItemViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerViewItemViewHolder holder, int position) {
            holder.mTextView.setText(
                    mRoutesManager.getRouteListingPreferenceItems().get(position).mRouteName);
        }

        @Override
        public int getItemCount() {
            return mRoutesManager.getRouteListingPreferenceItems().size();
        }
    }

    private class RecyclerViewItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public final TextView mTextView;

        private RecyclerViewItemViewHolder(TextView textView) {
            super(textView);
            mTextView = textView;
            textView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            setUpRouteListingPreferenceItemEditionDialog(getBindingAdapterPosition());
        }
    }

    private enum RouteListingPreferenceItemSelectionBehavior {
        SELECTION_BEHAVIOR_NONE(RouteListingPreference.Item.SELECTION_BEHAVIOR_NONE, "None"),
        SELECTION_BEHAVIOR_TRANSFER(
                RouteListingPreference.Item.SELECTION_BEHAVIOR_TRANSFER, "Transfer"),
        SELECTION_BEHAVIOR_GO_TO_APP(
                RouteListingPreference.Item.SELECTION_BEHAVIOR_GO_TO_APP, "Go to app");

        public final int mConstant;
        public final String mHumanReadableString;

        RouteListingPreferenceItemSelectionBehavior(
                int constant, @NonNull String humanReadableString) {
            mConstant = constant;
            mHumanReadableString = humanReadableString;
        }

        @NonNull
        @Override
        public String toString() {
            return mHumanReadableString;
        }

        public static RouteListingPreferenceItemSelectionBehavior fromConstant(int constant) {
            switch (constant) {
                case RouteListingPreference.Item.SELECTION_BEHAVIOR_NONE:
                    return SELECTION_BEHAVIOR_NONE;
                case RouteListingPreference.Item.SELECTION_BEHAVIOR_TRANSFER:
                    return SELECTION_BEHAVIOR_TRANSFER;
                case RouteListingPreference.Item.SELECTION_BEHAVIOR_GO_TO_APP:
                    return SELECTION_BEHAVIOR_GO_TO_APP;
                default:
                    throw new IllegalArgumentException("Illegal selection behavior: " + constant);
            }
        }
    }

    private enum RouteListingPreferenceItemSubtext {
        SUBTEXT_NONE(RouteListingPreference.Item.SUBTEXT_NONE, "None"),
        SUBTEXT_ERROR_UNKNOWN(RouteListingPreference.Item.SUBTEXT_ERROR_UNKNOWN, "Unknown error"),
        SUBTEXT_SUBSCRIPTION_REQUIRED(
                RouteListingPreference.Item.SUBTEXT_SUBSCRIPTION_REQUIRED, "Subscription required"),
        SUBTEXT_DOWNLOADED_CONTENT_ROUTING_DISALLOWED(
                RouteListingPreference.Item.SUBTEXT_DOWNLOADED_CONTENT_ROUTING_DISALLOWED,
                "Downloaded content disallowed"),
        SUBTEXT_AD_ROUTING_DISALLOWED(
                RouteListingPreference.Item.SUBTEXT_AD_ROUTING_DISALLOWED, "Ad in progress"),
        SUBTEXT_DEVICE_LOW_POWER(
                RouteListingPreference.Item.SUBTEXT_DEVICE_LOW_POWER, "Device in low power mode"),
        SUBTEXT_UNAUTHORIZED(RouteListingPreference.Item.SUBTEXT_UNAUTHORIZED, "Unauthorized"),
        SUBTEXT_TRACK_UNSUPPORTED(
                RouteListingPreference.Item.SUBTEXT_TRACK_UNSUPPORTED, "Track unsupported"),
        SUBTEXT_CUSTOM(
                RouteListingPreference.Item.SUBTEXT_CUSTOM, "Custom text (placeholder value)");

        public final int mConstant;
        @NonNull public final String mHumanReadableString;

        RouteListingPreferenceItemSubtext(int constant, @NonNull String humanReadableString) {
            mConstant = constant;
            mHumanReadableString = humanReadableString;
        }

        @NonNull
        @Override
        public String toString() {
            return mHumanReadableString;
        }

        public static RouteListingPreferenceItemSubtext fromConstant(int constant) {
            switch (constant) {
                case RouteListingPreference.Item.SUBTEXT_NONE:
                    return SUBTEXT_NONE;
                case RouteListingPreference.Item.SUBTEXT_ERROR_UNKNOWN:
                    return SUBTEXT_ERROR_UNKNOWN;
                case RouteListingPreference.Item.SUBTEXT_SUBSCRIPTION_REQUIRED:
                    return SUBTEXT_SUBSCRIPTION_REQUIRED;
                case RouteListingPreference.Item.SUBTEXT_DOWNLOADED_CONTENT_ROUTING_DISALLOWED:
                    return SUBTEXT_DOWNLOADED_CONTENT_ROUTING_DISALLOWED;
                case RouteListingPreference.Item.SUBTEXT_AD_ROUTING_DISALLOWED:
                    return SUBTEXT_AD_ROUTING_DISALLOWED;
                case RouteListingPreference.Item.SUBTEXT_DEVICE_LOW_POWER:
                    return SUBTEXT_DEVICE_LOW_POWER;
                case RouteListingPreference.Item.SUBTEXT_UNAUTHORIZED:
                    return SUBTEXT_UNAUTHORIZED;
                case RouteListingPreference.Item.SUBTEXT_TRACK_UNSUPPORTED:
                    return SUBTEXT_TRACK_UNSUPPORTED;
                case RouteListingPreference.Item.SUBTEXT_CUSTOM:
                    return SUBTEXT_CUSTOM;
                default:
                    throw new IllegalArgumentException("Illegal subtext constant: " + constant);
            }
        }
    }
}
