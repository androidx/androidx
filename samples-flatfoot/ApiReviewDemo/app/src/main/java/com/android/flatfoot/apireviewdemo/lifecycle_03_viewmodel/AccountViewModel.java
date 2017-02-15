package com.android.flatfoot.apireviewdemo.lifecycle_03_viewmodel;

import android.support.annotation.NonNull;

import com.android.flatfoot.apireviewdemo.common.entity.Person;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

public class AccountViewModel extends ViewModel {

    static class PersonDataWithStatus {
        final Person person;
        final String errorMsg;
        final boolean loading;

        PersonDataWithStatus(Person person, String errorMsg, boolean loading) {
            this.person = person;
            this.errorMsg = errorMsg;
            this.loading = loading;
        }
    }

    LiveData<PersonDataWithStatus> personData = new LiveData<>();

    public AccountViewModel() {
        personData.setValue(new PersonDataWithStatus(null, null, true));
        DataManagement.getInstance().requestPersonData("jakewharton",
                new DataManagement.Callback() {
                    @Override
                    public void success(@NonNull Person person) {
                        personData.setValue(new PersonDataWithStatus(person, null, false));
                    }

                    @Override
                    public void failure(String errorMsg) {
                        personData.setValue(new PersonDataWithStatus(null, errorMsg, false));
                    }
                });
    }
}
