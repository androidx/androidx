/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.debugview.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.debugview.R;
import androidx.fragment.app.Fragment;

/**
 * A fragment for displaying page navigation shortcuts of the debug view.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, /*attachToRoot=*/false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button documentListButton = getView().findViewById(R.id.view_documents_button);
        documentListButton.setOnClickListener(
                unusedView -> {
                    DocumentListFragment documentListFragment = new DocumentListFragment();
                    navigateToFragment(documentListFragment);
                });

        Button schemaTypeListButton = getView().findViewById(R.id.view_schema_types_button);
        schemaTypeListButton.setOnClickListener(
                unusedView -> {
                    SchemaTypeListFragment schemaTypeListFragment = new SchemaTypeListFragment();
                    navigateToFragment(schemaTypeListFragment);
                });
    }

    private void navigateToFragment(Fragment fragment) {
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(/*name=*/null)
                .commit();
    }
}
