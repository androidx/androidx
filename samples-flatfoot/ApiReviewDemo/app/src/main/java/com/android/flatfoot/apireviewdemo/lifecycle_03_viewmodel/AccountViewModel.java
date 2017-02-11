package com.android.flatfoot.apireviewdemo.lifecycle_03_viewmodel;

import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

public class AccountViewModel extends ViewModel {

    LiveData<DataManagement.PersonDataWithStatus> personData;

    public AccountViewModel() {
        this.personData = DataManagement.getInstance().requestPersonData("jakewharton");
    }
}
