package com.android.flatfoot.apireviewdemo.lifecycle_03_viewmodel;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.android.flatfoot.apireviewdemo.R;
import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

public class OneAccountActivity extends LifecycleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_model1);

        AccountViewModel viewModel = ViewModelStore.get(this, "oneaccount", AccountViewModel.class);
        viewModel.personData.observe(this, new Observer<DataManagement.PersonDataWithStatus>() {
            @Override
            public void onChanged(@Nullable DataManagement.PersonDataWithStatus data) {
                if (data.person != null) {
                    TextView emailView = (TextView) findViewById(R.id.name);
                    emailView.setText(data.person.getName());
                    TextView nameView = (TextView) findViewById(R.id.company);
                    nameView.setText(data.person.getCompany());
                }

                findViewById(R.id.loading_spinner).setVisibility(data.loading ?
                        View.VISIBLE : View.GONE);
            }
        });
    }
}
