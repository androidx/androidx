package com.android.flatfoot.apireviewdemo.full_sample_xxx;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.android.flatfoot.apireviewdemo.R;
import com.android.flatfoot.apireviewdemo.common.entity.Person;
import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import java.util.Random;

public class SwitchAccountsActivity extends LifecycleActivity {

    private final String[] USERS = new String[] {"yigit", "JakeWharton"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final AccountViewModel viewModel = ViewModelStore.get(this, "switchaccounts",
                AccountViewModel.class);
        viewModel.setUser(USERS[0]);
        setContentView(R.layout.switch_accounts);

        findViewById(R.id.switch_user).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.setUser(USERS[new Random().nextInt(USERS.length)]);
            }
        });

        viewModel.personData.observe(this, new Observer<Person>() {
            @Override
            public void onChanged(@Nullable Person data) {
                if (data != null) {
                    TextView emailView = (TextView) findViewById(R.id.name);
                    emailView.setText(data.getName());
                    TextView nameView = (TextView) findViewById(R.id.company);
                    nameView.setText(data.getCompany());
                }
            }
        });

        viewModel.statusData.observe(this, new Observer<AccountViewModel.Status>() {
            @Override
            public void onChanged(@Nullable AccountViewModel.Status status) {
                findViewById(R.id.loading_spinner).setVisibility(
                        status.updating ? View.VISIBLE : View.GONE);
            }
        });

        findViewById(R.id.force_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewModel.forceRefresh();
            }
        });
    }
}
