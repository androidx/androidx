package com.android.flatfoot.apireviewdemo.lifecycle_03_viewmodel;

import android.support.annotation.NonNull;

import com.android.flatfoot.apireviewdemo.common.entity.Person;
import com.android.flatfoot.apireviewdemo.common.github.GithubService;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class DataManagement {

    private static DataManagement sInstance = new DataManagement();

    interface Callback {
        void success(@NonNull Person person);

        void failure(String errorMsg);
    }

    public static DataManagement getInstance() {
        return sInstance;
    }

    private final GithubService mGithubService;

    public DataManagement() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mGithubService = retrofit.create(GithubService.class);
    }

    void requestPersonData(String user, final Callback callback) {
        mGithubService.getUser(user).enqueue(new retrofit2.Callback<Person>() {
            @Override
            public void onResponse(Call<Person> call, Response<Person> response) {
                if (response.isSuccessful()) {
                    callback.success(response.body());
                } else {
                    callback.failure("Failed with " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Person> call, Throwable t) {
                callback.failure("Failed with " + t.getMessage());
            }
        });
    }
}
