/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.retain.samples

import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.LocalRetainedValuesStoreProvider
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.retain.retainManagedRetainedValuesStore
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.compose.ui.graphics.painter.Painter

@Sampled
fun retainedEffectSample() {
    @Composable
    fun VideoPlayer(mediaUri: String) {
        val player = retain(mediaUri) { MediaPlayer(mediaUri) }

        // Initialize each player only once after we retain it.
        // If the uri (and therefore the player) change, we need to dispose the old player
        // and initialize the new one. Likewise, the player needs to be disposed of when
        // it stops being retained.
        RetainedEffect(player) {
            player.initialize()
            onRetire { player.close() }
        }

        // ...
    }
}

internal class MediaPlayer(val uri: String = "") {
    fun initialize() {}

    fun close() {}

    fun play() {}

    fun stop() {}
}

@Sampled
fun retainingCollapsingContentSample() {
    @Composable
    fun CollapsingMediaPlayer(visible: Boolean) {
        // Important: This retainedValuesStore is created outside of the if statement to ensure
        // that it lives as long as the CollapsingMediaPlayer composable itself.
        val retainedValuesStore = retainManagedRetainedValuesStore()

        // This content is only shown when `visible == true`
        if (visible) {
            LocalRetainedValuesStoreProvider(retainedValuesStore) {
                // Create a media player that will be retained when the CollapsingMediaPlayer is not
                // visible. This component can continue to play audio when the video is hidden.
                val mediaPlayer = retain { MediaPlayer() }
                RetainedEffect(mediaPlayer) {
                    mediaPlayer.play()
                    onRetire { mediaPlayer.stop() }
                }

                // Render the video component inside the RetainedContentHost.
            }
        }
    }
}

@Suppress("UnnecessaryLambdaCreation")
@Sampled
fun retainManagedRetainedValuesStoreSample() {
    @Composable
    fun RetainedAnimatedContent(active: Boolean, content: @Composable () -> Unit) {
        // Create a RetainedValuesStore. It will be added as a child to the current store and start
        // retaining exited values when the parent does. On Android, this store will implicitly
        // survive and forward retention events caused by configuration changes.
        val retainedValuesStore = retainManagedRetainedValuesStore()
        AnimatedContent(active) { targetState ->
            if (targetState) {
                // Install the RetainedValuesStore over the child content
                LocalRetainedValuesStoreProvider(retainedValuesStore) {
                    // Values retained here will be kept when this content is faded out,
                    // and restored when the content is added back to the composition.
                    content()
                }
            }
        }
    }
}

@Sampled
fun retainedValuesStoreRegistrySample() {
    // List item that retains a value
    @Composable
    fun Contact(contact: Contact, deleteContact: () -> Unit) {
        Row {
            // Retain this painter to cache the contact icon in memory
            val contactIcon = retain { ContactIconPainter() }
            Image(contactIcon, "Contact icon")
            Text(contact.name)
            // Optional delete action (likely nested in a DropdownMenu)
            IconButton(onClick = deleteContact) { Icon(Icons.Default.Delete, "Delete contact") }
        }
    }

    @Composable
    fun ContactsList(contacts: List<Contact>) {
        // Create the RetainedValuesStoreRegistry
        val retainedValuesStoreRegistry = retainRetainedValuesStoreRegistry()
        LazyColumn {
            items(contacts) { contact ->
                // Install it for an item in a list
                retainedValuesStoreRegistry.LocalRetainedValuesStoreProvider(contact.id) {
                    // This contact now gets its own retain store.
                    // If the store of ContactsList starts retaining exited values, this nested
                    // store will too. If this contact leaves and re-enters composition, it will
                    // keep its previously retained values.
                    Contact(
                        contact = contact,
                        deleteContact = {
                            // Call into the contacts provider to delete the contact
                            // ...
                            //
                            // Optional: Purge child stores if a contact gets deleted. This is a
                            // "best effort" when a contact is deleted and might not capture all
                            // deletions. If we miss a deletion, we'll continue retaining for that
                            // contact until the list is retired. If this is a large pool of
                            // objects, you may choose to be more aggressive about how you clear
                            // keys from the registry.
                            retainedValuesStoreRegistry.clearChild(contact.id)
                        },
                    )
                }
            }
        }
    }
}

private fun ContactIconPainter(): Painter = TODO()

private class Contact(val id: String, val name: String)
