package com.android.flatfoot.apireviewdemo.lifecycle_03_viewmodel;

import com.android.flatfoot.apireviewdemo.common.entity.Person;
import com.android.flatfoot.apireviewdemo.common.github.GithubService;
import com.android.support.lifecycle.LiveData;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class DataManagement {

    private static DataManagement sInstance = new DataManagement();

    public static DataManagement getInstance() {
        return sInstance;
    }

    static class PersonDataWithStatus {
        final Person person;
        final int status;
        final boolean loading;

        PersonDataWithStatus(Person person, int status, boolean loading) {
            this.person = person;
            this.status = status;
            this.loading = loading;
        }
    }

    private final GithubService mGithubService;

    public DataManagement() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mGithubService = retrofit.create(GithubService.class);
    }

    LiveData<PersonDataWithStatus> requestPersonData(String user) {
        final LiveData<PersonDataWithStatus> data = new LiveData<>();
        data.setValue(new PersonDataWithStatus(null, -1, true));
        mGithubService.getUser(user).enqueue(new Callback<Person>() {
            @Override
            public void onResponse(Call<Person> call, Response<Person> response) {
                if (response.isSuccessful()) {
                    data.setValue(
                            new PersonDataWithStatus(response.body(), response.code(), false));
                } else {
                    data.setValue(new PersonDataWithStatus(null, response.code(), false));
                }
            }

            @Override
            public void onFailure(Call<Person> call, Throwable t) {
                data.setValue(new PersonDataWithStatus(null, -1, false));
            }
        });
        return data;
    }
}
