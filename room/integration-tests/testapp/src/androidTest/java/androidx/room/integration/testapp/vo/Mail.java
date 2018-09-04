/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.integration.testapp.vo;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.PrimaryKey;

@Entity
@Fts4
public class Mail {

    @PrimaryKey
    @ColumnInfo(name = "rowid")
    public long rowId;

    public String subject;
    public String body;
    public long datetime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mail mail = (Mail) o;

        if (rowId != mail.rowId) return false;
        if (datetime != mail.datetime) return false;
        if (subject != null ? !subject.equals(mail.subject) : mail.subject != null) return false;
        return body != null ? body.equals(mail.body) : mail.body == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (rowId ^ (rowId >>> 32));
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (int) (datetime ^ (datetime >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Mail{"
                + "rowId=" + rowId
                + ", subject='" + subject + '\''
                + ", body='" + body + '\''
                + ", datetime=" + datetime
                + '}';
    }
}
