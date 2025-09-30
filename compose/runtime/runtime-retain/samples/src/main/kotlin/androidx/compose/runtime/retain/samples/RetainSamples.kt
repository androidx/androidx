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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.retain.LocalRetainScope
import androidx.compose.runtime.retain.RetainedContentHost
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.retain.retainControlledRetainScope
import androidx.compose.runtime.retain.retainRetainScopeHolder
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
fun retainedContentHostSample() {
    @Composable
    fun CollapsingMediaPlayer(visible: Boolean) {
        // This content is only shown when `visible == true`
        RetainedContentHost(active = visible) {
            // Create a media player that will be retained when the CollapsingMediaPlayer is no
            // longer visible. This component can continue to play audio when the video is hidden.
            val mediaPlayer = retain { MediaPlayer() }
            RetainedEffect(mediaPlayer) {
                mediaPlayer.play()
                onRetire { mediaPlayer.stop() }
            }

            // Render the video component inside the RetainedContentHost.
        }
    }
}

@Suppress("UnnecessaryLambdaCreation")
@Sampled
fun retainControlledRetainScopeSample() {
    @Composable
    fun AnimatedRetainedContentHost(active: Boolean, content: @Composable () -> Unit) {
        // Create a retain scope. It will be added as a child to the current scope and start
        // keeping exited values when the parent does. On Android, this scope will implicitly
        // survive and forward retention events caused by configuration changes.
        val retainScope = retainControlledRetainScope()
        AnimatedContent(active) { targetState ->
            if (targetState) {
                // Install the retain scope over the child content
                CompositionLocalProvider(LocalRetainScope provides retainScope) {
                    // Values retained here will be kept when this content is faded out,
                    // and restored when the content is added back to the composition.
                    content()
                }

                // Define the retention scenario that will issue commands to start and stop keeping
                // exited values. If you use this effect in your code, it must come AFTER the
                // content is composed to correctly capture values. This effect is not mandatory,
                // but is a convenient way to match the RetainScope's state to the visibility of its
                // content. You can manage the retain scope in any way suitable for your content.
                val composer = currentComposer
                DisposableEffect(retainScope) {
                    // Stop keeping exited values when we become active. Use the request count to
                    // only look at our state and to ignore any parent-influenced requests.
                    val cancellationHandle =
                        if (retainScope.keepExitedValuesRequestsFromSelf > 0) {
                            composer.scheduleFrameEndCallback {
                                retainScope.stopKeepingExitedValues()
                            }
                        } else {
                            null
                        }

                    onDispose {
                        // Start keeping exited values when we deactivate
                        cancellationHandle?.cancel()
                        retainScope.startKeepingExitedValues()
                    }
                }
            }
        }
    }
}

@Sampled
fun retainScopeHolderSample() {
    // List item that retains a value
    @Composable
    fun Contact(contact: Contact) {
        Row {
            // Retain this painter to cache the contact icon in memory
            val contactIcon = retain { ContactIconPainter() }
            Image(contactIcon, "Contact icon")
            Text(contact.name)
        }
    }

    @Composable
    fun ContactsList(contacts: List<Contact>) {
        // Create the RetainScopeHolder
        val retainScopeHolder = retainRetainScopeHolder()
        LazyColumn {
            items(contacts) { contact ->
                // Install it for an item in a list
                retainScopeHolder.RetainScopeProvider(contact.id) {
                    // This contact now gets its own retain scope.
                    // If the scope of ContactsList starts keeping exited values, this nested
                    // scope will too. If this contact leaves re-enters composition, it will keep
                    // its previously retained values.
                    Contact(contact)
                }
            }
        }

        // Optional: Purge child scopes if a contact gets deleted.
        DisposableEffect(contacts) {
            val contactIdsSet = contacts.map { it.id }
            retainScopeHolder.clearChildren { it !in contactIdsSet }
            onDispose {}
        }
    }
}

private fun ContactIconPainter(): Painter = TODO()

private class Contact(val id: String, val name: String)
