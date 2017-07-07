/*
 * Copyright (C) 2017 The Android Open Source Project
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

/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.sample.githubbrowser.network;

import com.android.sample.githubbrowser.data.ContributorData;
import com.android.sample.githubbrowser.data.PersonData;
import com.android.sample.githubbrowser.data.RepositoryData;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit-powered service to connect to Github backend.
 */
public interface GithubService {
    /**
     * Lists the repositories for the specified user.
     */
    @GET("/users/{user}/repos")
    Call<List<RepositoryData>> listRepositories(@Path("user") String user, @Query("page") int page);

    /**
     * Gets the information about the specified repository.
     */
    @GET("/repos/{user}/{name}")
    Call<RepositoryData> getRepository(@Path("user") String user, @Path("name") String name);

    /**
     * Lists the contributors for the specified project owned by the specified user.
     */
    @GET("/repos/{user}/{project}/contributors")
    Call<List<ContributorData>> getContributors(@Path("user") String owner,
            @Path("project") String project, @Query("page") int page);

    /**
     * Gets the information about the specified user.
     */
    @GET("/users/{user}")
    Call<PersonData> getUser(@Path("user") String user);
}
