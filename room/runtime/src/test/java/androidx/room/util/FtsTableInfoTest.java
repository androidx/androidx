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

package androidx.room.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.internal.util.collections.Sets;

import java.util.Set;

@RunWith(JUnit4.class)
public class FtsTableInfoTest {

    @Test
    public void test_parseOptions() {
        assertOptions("CREATE VIRTUAL TABLE Book USING FTS4()");

        assertOptions("CREATE VIRTUAL TABLE Book USING FTS4(  )");

        assertOptions("CREATE VIRTUAL TABLE Book USING fts4(author)");

        assertOptions("CREATE VIRTUAL TABLE Book USING FTS4(author, matchinfo=fts3)",
                "matchinfo=fts3");

        assertOptions("CREATE VIRTUAL TABLE \"Book\" USING FTS4(\"author\", "
                        + "matchinfo=fts3)",
                "matchinfo=fts3");

        assertOptions("CREATE VIRTUAL TABLE `Fun'Names` USING FTS4(matchinfo=fts3)",
                "matchinfo=fts3");

        assertOptions("CREATE VIRTUAL TABLE `Fun'With'Names` USING FTS4(\"odd'column'\", "
                        + "`odd'column'again`, [select], 'left[col]is`weird', matchinfo=fts3)",
                "matchinfo=fts3");

        assertOptions("CREATE VIRTUAL TABLE 'Book' USING FTS4('content', 'pages', "
                        + "'isbn', notindexed=pages, notindexed=isbn)",
                "notindexed=pages", "notindexed=isbn");

        assertOptions("CREATE VIRTUAL TABLE `Book` USING FTS4(tokenize=porter, "
                        + "`content`, `pages`, notindexed=pages)",
                "tokenize=porter", "notindexed=pages");

        assertOptions("CREATE VIRTUAL TABLE `Book` USING FTS4(tokenize=porter, "
                        + "`content`, `pages`, notindexed=pages)",
                "tokenize=porter", "notindexed=pages");

        assertOptions("CREATE VIRTUAL TABLE `Book` USING FTS4(tokenize=unicode61 "
                        + "\"tokenchars=,\")",
                "tokenize=unicode61 \"tokenchars=,\"");

        assertOptions("CREATE VIRTUAL TABLE `Book` USING FTS4(tokenize=unicode61 "
                        + "`tokenchars=,`)",
                "tokenize=unicode61 `tokenchars=,`");

        assertOptions("CREATE VIRTUAL TABLE `Book` USING FTS4(tokenize=unicode61 "
                        + "\"tokenchars=.=\" \"separators=X\", `pages`, notindexed=pages)",
                "tokenize=unicode61 \"tokenchars=.=\" \"separators=X\"",
                "notindexed=pages");

        assertOptions("CREATE VIRTUAL TABLE `Book` USING FTS4(tokenize=porter, "
                        + "`author`, languageid=`lid`, matchinfo=fts3, notindexed=`pages`, "
                        + "order=desc, prefix=`2,4`)",
                "tokenize=porter", "languageid=`lid`", "matchinfo=fts3",
                "notindexed=`pages`", "order=desc", "prefix=`2,4`");
    }

    private void assertOptions(String createSql, String... options) {
        Set<String> actualOptions = FtsTableInfo.parseOptions(createSql);
        Set<String> expectedOptions = Sets.newSet(options);
        assertThat(actualOptions, is(expectedOptions));
    }
}
