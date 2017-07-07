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
package com.android.sample.githubbrowser.data;

import com.android.support.room.Decompose;
import com.android.support.room.Entity;
import com.android.support.room.PrimaryKey;
import com.android.support.room.RoomWarnings;

/**
 * Repository data object.
 */
@Entity
public class RepositoryData {
    @PrimaryKey public String id;
    public String name;
    public String full_name;
    @SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_DECOMPOSED_IS_DROPPED)
    @Decompose(prefix = "owner_") public PersonData owner;
    public String description;
    public String created_at;
    public int stargazers_count;
    public String language;
    public int forks_count;
    public int open_issues_count;
    @SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_DECOMPOSED_IS_DROPPED)
    @Decompose(prefix = "organization_") public PersonData organization;
    public int subscribers_count;

    public RepositoryData() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RepositoryData that = (RepositoryData) o;

        if (stargazers_count != that.stargazers_count) return false;
        if (forks_count != that.forks_count) return false;
        if (open_issues_count != that.open_issues_count) return false;
        if (subscribers_count != that.subscribers_count) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (full_name != null ? !full_name.equals(that.full_name) : that.full_name != null) {
            return false;
        }
        if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
        if (description != null ? !description.equals(that.description)
                : that.description != null) {
            return false;
        }
        if (created_at != null ? !created_at.equals(that.created_at) : that.created_at != null) {
            return false;
        }
        if (language != null ? !language.equals(that.language) : that.language != null) {
            return false;
        }
        return organization != null ? organization.equals(that.organization)
                : that.organization == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (full_name != null ? full_name.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
        result = 31 * result + stargazers_count;
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + forks_count;
        result = 31 * result + open_issues_count;
        result = 31 * result + (organization != null ? organization.hashCode() : 0);
        result = 31 * result + subscribers_count;
        return result;
    }
}
