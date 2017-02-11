package com.android.flatfoot.apireviewdemo.common.github;

import com.android.flatfoot.apireviewdemo.common.entity.Person;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit-powered service to connect to Github backend.
 */
public interface GithubService {
    /**
     * Gets the information about the specified user.
     */
    @GET("/users/{user}")
    Call<Person> getUser(@Path("user") String user);
}
