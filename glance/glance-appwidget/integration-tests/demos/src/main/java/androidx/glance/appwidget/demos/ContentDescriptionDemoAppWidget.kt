/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.appwidget.demos

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * This demo widget shows difference in behavior with screen readers when content description is set
 * vs not set on a parent composable. Uses a lazy list to demonstrate navigation between items as
 * well.
 */
class ContentDescriptionDemoAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content()
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content()
    }

    @Composable
    private fun Content() {
        var setTopLevelContentDescriptionPerItem by remember { mutableStateOf(false) }

        GlanceTheme {
            Column(
                modifier =
                    GlanceModifier.fillMaxSize()
                        .padding(16.dp)
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.surface)
            ) {
                CheckBox(
                    text = "Set top-level content description per-item",
                    checked = setTopLevelContentDescriptionPerItem,
                    onCheckedChange = {
                        setTopLevelContentDescriptionPerItem = !setTopLevelContentDescriptionPerItem
                    }
                )
                VerticalSpacer()
                BooksList(
                    setTopLevelContentDescriptionPerItem = setTopLevelContentDescriptionPerItem
                )
            }
        }
    }

    @Composable
    fun BooksList(setTopLevelContentDescriptionPerItem: Boolean) {
        // You should fetch these from your repository and pass them down from provideGlance,
        // however, for simplicity of the demo, we define these here.
        val books = DEMO_BOOKS
        val booksInWishList = remember { mutableStateListOf<String>() }

        LazyColumn {
            itemsIndexed(books) { index, book ->
                key(index) {
                    BookItem(
                        book = book,
                        isInWishList = booksInWishList.contains(book.id),
                        toggleWishList = {
                            if (booksInWishList.contains(book.id)) {
                                booksInWishList.remove(book.id)
                            } else {
                                booksInWishList.add(book.id)
                            }
                        },
                        isLastItem = index == books.size - 1,
                        setTopLevelContentDescription = setTopLevelContentDescriptionPerItem
                    )
                }
            }
        }
    }

    @Composable
    fun BookItem(
        book: Book,
        isInWishList: Boolean,
        toggleWishList: () -> Unit,
        isLastItem: Boolean,
        setTopLevelContentDescription: Boolean
    ) {
        var modifier =
            GlanceModifier.padding(8.dp)
                .background(GlanceTheme.colors.background)
                .clickable(actionStartActivity<ListClickDestinationActivity>())

        // For this UI, in Glance, the top-level Column and the CircleIconButton will be the two
        // navigable items seen when using accessibility services like talkback. Individual text
        // items in BookDetails will not be focusable (as they are not clickable). Since each
        // BookItem is part of a list, irrespective of whether the top-level container is clickable
        // or not, it will be navigable. But, if it is not part of a List/Grid, it will be navigable
        // only if it is clickable.
        //
        // 1. When setTopLevelContentDescription == false, on focusing the top-level Column, all the
        // non-clickable content in BookDetails will be read out. If any of the texts has content
        // description set, it will be read instead of the visible text. Then, on navigating to the
        // CircleIconButton, its content description will be read out.
        //
        // 2. When setTopLevelContentDescription == true, on focusing the top-level Column, only the
        // contentDescription set on it will be read. So, if you set contentDescription on outer
        // container like this, you should make sure the value you set is representative of all
        // the text inside; as merge semantics is not supported in Glance. Similar to #1, on
        // navigating to the CircleIconButton, its content description will be read out.
        if (setTopLevelContentDescription) {
            modifier =
                modifier.semantics {
                    contentDescription =
                        "This is an explicit content description set on the container of ${book.title}"
                }
        }

        Column(modifier = modifier) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                BookDetails(book = book, modifier = GlanceModifier.padding(5.dp).defaultWeight())
                HorizontalSpacer()
                CircleIconButton(
                    imageProvider =
                        if (isInWishList) {
                            ImageProvider(R.drawable.baseline_check_24)
                        } else {
                            ImageProvider(R.drawable.baseline_add_24)
                        },
                    backgroundColor = GlanceTheme.colors.secondary,
                    contentColor = GlanceTheme.colors.onSecondary,
                    // On navigating to this icon button, this content description will be read out.
                    contentDescription =
                        if (isInWishList) {
                            "Remove from wish list"
                        } else {
                            "Add to wish list"
                        },
                    onClick = toggleWishList,
                )
            }
            if (!isLastItem) {
                ListSeparator()
            }
        }
    }

    @Composable
    fun BookDetails(modifier: GlanceModifier, book: Book) {
        Column(modifier) {
            Text(
                // If top-level container doesn't have an explicit content description set, this
                // text will be included in content read out by the screen reader.
                text = book.title,
                style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold)
            )
            VerticalSpacer()
            Row {
                Text(
                    // If top-level container doesn't have a explicit content description set, this
                    // text will be included in content read out by the screen reader.
                    text = book.author,
                    style = TextStyle(color = GlanceTheme.colors.secondary)
                )
                HorizontalSpacer()
                Text(
                    // This won't be read as the text composable has a contentDescription is set.
                    text = book.genre,
                    style = TextStyle(color = GlanceTheme.colors.onTertiary),
                    modifier =
                        GlanceModifier.padding(5.dp)
                            .cornerRadius(16.dp)
                            .background(GlanceTheme.colors.tertiary)
                            // If top-level container doesn't have a explicit content description
                            // set,
                            // this contentDescription will be included in content read out by the
                            // screen reader.
                            .semantics { contentDescription = "The genre is ${book.genre}" }
                )
            }
        }
    }

    @Composable
    fun ListSeparator() {
        Spacer(
            modifier =
                GlanceModifier.height(1.dp)
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.onBackground)
        )
    }

    @Composable
    fun VerticalSpacer() {
        Spacer(modifier = GlanceModifier.height(5.dp))
    }

    @Composable
    fun HorizontalSpacer() {
        Spacer(modifier = GlanceModifier.width(5.dp))
    }

    companion object {
        data class Book(val id: String, val title: String, val author: String, val genre: String)

        val DEMO_BOOKS =
            listOf(
                Book("1", "Book 1", "John Doe", "Thriller"),
                Book("2", "Book 2", "Jane Doe", "Adventure")
            )
    }
}

class ContentDescriptionAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ContentDescriptionDemoAppWidget()
}
