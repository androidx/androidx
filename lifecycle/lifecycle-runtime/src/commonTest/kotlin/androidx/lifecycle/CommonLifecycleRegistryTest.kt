/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lifecycle

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.BeforeTest
import kotlin.test.Test

class CommonLifecycleRegistryTest {
    private lateinit var mLifecycleOwner: LifecycleOwner
    private lateinit var mRegistry: LifecycleRegistry

    @BeforeTest
    fun init() {
        mLifecycleOwner =
            object : LifecycleOwner {
                override val lifecycle
                    get() = mRegistry
            }
        mRegistry = LifecycleRegistry.createUnsafe(mLifecycleOwner)
    }

    @Test
    fun getCurrentState() {
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(mRegistry.currentState).isEqualTo(Lifecycle.State.RESUMED)
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(mRegistry.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun moveInitializedToDestroyed() {
        assertThrows<IllegalStateException> {
                mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
            .hasMessageThat()
            .contains(
                "State must be at least 'CREATED' to be moved to 'DESTROYED' in component " +
                    "$mLifecycleOwner"
            )
    }

    @Test
    fun moveDestroyedToAny() {
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThrows<IllegalStateException> {
                mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            }
            .hasMessageThat()
            .contains(
                "State is 'DESTROYED' and cannot be moved to `CREATED` in component " +
                    "$mLifecycleOwner"
            )
    }

    @Test
    fun setCurrentState() {
        mRegistry.currentState = Lifecycle.State.RESUMED
        assertThat(mRegistry.currentState).isEqualTo(Lifecycle.State.RESUMED)
        mRegistry.currentState = Lifecycle.State.DESTROYED
        assertThat(mRegistry.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun addRemove() {
        val observer = TestObserver()
        mRegistry.addObserver(observer)
        assertThat(mRegistry.observerCount).isEqualTo(1)
        mRegistry.removeObserver(observer)
        assertThat(mRegistry.observerCount).isEqualTo(0)
    }

    @Test
    fun addAndObserve() {
        val observer = TestObserver()
        mRegistry.addObserver(observer)
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        assertThat(observer.onCreateCallCount).isEqualTo(1)
        assertThat(observer.onStateChangedEvents).containsExactly(Lifecycle.Event.ON_CREATE)

        dispatchEvent(Lifecycle.Event.ON_CREATE)
        assertThat(observer.onCreateCallCount).isEqualTo(1)

        dispatchEvent(Lifecycle.Event.ON_START)
        assertThat(observer.onStopCallCount).isEqualTo(0)
        dispatchEvent(Lifecycle.Event.ON_STOP)
        assertThat(observer.onStopCallCount).isEqualTo(1)
    }

    @Test
    fun add2RemoveOne() {
        val observer1 = TestObserver()
        val observer2 = TestObserver()
        val observer3 = TestObserver()
        mRegistry.addObserver(observer1)
        mRegistry.addObserver(observer2)
        mRegistry.addObserver(observer3)
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        assertThat(observer1.onCreateCallCount).isEqualTo(1)
        assertThat(observer2.onCreateCallCount).isEqualTo(1)
        assertThat(observer3.onCreateCallCount).isEqualTo(1)

        mRegistry.removeObserver(observer2)
        dispatchEvent(Lifecycle.Event.ON_START)
        assertThat(observer1.onStartCallCount).isEqualTo(1)
        assertThat(observer2.onStartCallCount).isEqualTo(0)
        assertThat(observer3.onStartCallCount).isEqualTo(1)
    }

    @Test
    fun removeWhileTraversing() {
        val observer2 = TestObserver()
        val observer1 =
            object : TestObserver() {
                override fun onCreate(owner: LifecycleOwner) {
                    super.onCreate(owner)
                    mRegistry.removeObserver(observer2)
                }
            }
        mRegistry.addObserver(observer1)
        mRegistry.addObserver(observer2)
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        assertThat(observer1.onCreateCallCount).isEqualTo(1)
        assertThat(observer2.onCreateCallCount).isEqualTo(0)
    }

    @Test
    fun constructionOrder() {
        fullyInitializeRegistry()
        val observer = TestObserver()
        mRegistry.addObserver(observer)
        assertThat(observer.onStateChangedEvents)
            .containsExactly(
                Lifecycle.Event.ON_CREATE,
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME
            )
            .inOrder()
    }

    @Test
    fun constructionDestruction1() {
        fullyInitializeRegistry()
        val observer =
            object : TestObserver() {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    dispatchEvent(Lifecycle.Event.ON_PAUSE)
                }
            }
        mRegistry.addObserver(observer)
        assertThat(observer.onStateChangedEvents)
            .containsExactly(Lifecycle.Event.ON_CREATE, Lifecycle.Event.ON_START)
            .inOrder()
        assertThat(observer.onResumeCallCount).isEqualTo(0)
    }

    @Test
    fun constructionDestruction2() {
        fullyInitializeRegistry()
        val observer =
            object : TestObserver() {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    dispatchEvent(Lifecycle.Event.ON_PAUSE)
                    dispatchEvent(Lifecycle.Event.ON_STOP)
                    dispatchEvent(Lifecycle.Event.ON_DESTROY)
                }
            }
        mRegistry.addObserver(observer)
        assertThat(observer.onStateChangedEvents)
            .containsExactly(
                Lifecycle.Event.ON_CREATE,
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY,
            )
            .inOrder()
        assertThat(observer.onResumeCallCount).isEqualTo(0)
    }

    @Test
    fun twoObserversChangingState() {
        val observer1 =
            object : TestObserver() {
                override fun onCreate(owner: LifecycleOwner) {
                    super.onCreate(owner)
                    dispatchEvent(Lifecycle.Event.ON_START)
                }
            }
        val observer2 = TestObserver()
        mRegistry.addObserver(observer1)
        mRegistry.addObserver(observer2)
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        assertThat(observer1.onCreateCallCount).isEqualTo(1)
        assertThat(observer2.onCreateCallCount).isEqualTo(1)
        assertThat(observer1.onStartCallCount).isEqualTo(1)
        assertThat(observer2.onStartCallCount).isEqualTo(1)
    }

    @Test
    fun addDuringTraversing() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        val observer3 = TestObserver(::populateEvents)
        val observer1 =
            object : TestObserver(::populateEvents) {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    mRegistry.addObserver(observer3)
                }
            }
        val observer2 = TestObserver(::populateEvents)
        mRegistry.addObserver(observer1)
        mRegistry.addObserver(observer2)
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        dispatchEvent(Lifecycle.Event.ON_START)
        assertThat(events)
            .containsExactly(
                observer1 to Lifecycle.Event.ON_CREATE,
                observer2 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_START,
                observer3 to Lifecycle.Event.ON_CREATE,
                observer2 to Lifecycle.Event.ON_START,
                observer3 to Lifecycle.Event.ON_START,
            )
            .inOrder()
    }

    @Test
    fun addDuringAddition() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        val observer3 = TestObserver(::populateEvents)
        val observer2 =
            object : TestObserver(::populateEvents) {
                override fun onCreate(owner: LifecycleOwner) {
                    super.onCreate(owner)
                    mRegistry.addObserver(observer3)
                }
            }
        val observer1 =
            object : TestObserver(::populateEvents) {
                override fun onResume(owner: LifecycleOwner) {
                    super.onResume(owner)
                    mRegistry.addObserver(observer2)
                }
            }
        mRegistry.addObserver(observer1)
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        dispatchEvent(Lifecycle.Event.ON_START)
        dispatchEvent(Lifecycle.Event.ON_RESUME)
        assertThat(events)
            .containsExactly(
                observer1 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_START,
                observer1 to Lifecycle.Event.ON_RESUME,
                observer2 to Lifecycle.Event.ON_CREATE,
                observer2 to Lifecycle.Event.ON_START,
                observer2 to Lifecycle.Event.ON_RESUME,
                observer3 to Lifecycle.Event.ON_CREATE,
                observer3 to Lifecycle.Event.ON_START,
                observer3 to Lifecycle.Event.ON_RESUME,
            )
            .inOrder()
    }

    @Test
    fun subscribeToDead() {
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        val observer1 = TestObserver()
        mRegistry.addObserver(observer1)
        assertThat(observer1.onCreateCallCount).isEqualTo(1)
        dispatchEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(observer1.onDestroyCallCount).isEqualTo(1)
        val observer2 = TestObserver()
        mRegistry.addObserver(observer2)
        assertThat(observer2.onCreateCallCount).isEqualTo(0)
    }

    @Test
    fun downEvents() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        fullyInitializeRegistry()
        val observer1 = TestObserver(::populateEvents)
        val observer2 = TestObserver(::populateEvents)
        mRegistry.addObserver(observer1)
        mRegistry.addObserver(observer2)
        dispatchEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(events)
            .containsAtLeast(
                observer2 to Lifecycle.Event.ON_PAUSE,
                observer1 to Lifecycle.Event.ON_PAUSE,
            )
            .inOrder()
        dispatchEvent(Lifecycle.Event.ON_STOP)
        assertThat(events)
            .containsAtLeast(
                observer2 to Lifecycle.Event.ON_STOP,
                observer1 to Lifecycle.Event.ON_STOP,
            )
            .inOrder()
        dispatchEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(events)
            .containsAtLeast(
                observer2 to Lifecycle.Event.ON_DESTROY,
                observer1 to Lifecycle.Event.ON_DESTROY,
            )
            .inOrder()
    }

    @Test
    fun downEventsAddition() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        dispatchEvent(Lifecycle.Event.ON_START)
        val observer1 = TestObserver(::populateEvents)
        val observer3 = TestObserver(::populateEvents)
        val observer2 =
            object : TestObserver(::populateEvents) {
                override fun onStop(owner: LifecycleOwner) {
                    super.onStop(owner)
                    mRegistry.addObserver(observer3)
                }
            }
        mRegistry.addObserver(observer1)
        mRegistry.addObserver(observer2)
        dispatchEvent(Lifecycle.Event.ON_STOP)
        assertThat(events)
            .containsAtLeast(
                observer2 to Lifecycle.Event.ON_STOP,
                observer3 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_STOP,
            )
            .inOrder()
        dispatchEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(events)
            .containsAtLeast(
                observer3 to Lifecycle.Event.ON_DESTROY,
                observer2 to Lifecycle.Event.ON_DESTROY,
                observer1 to Lifecycle.Event.ON_DESTROY,
            )
            .inOrder()
    }

    @Test
    fun downEventsRemoveAll() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        fullyInitializeRegistry()
        val observer1 = TestObserver(::populateEvents)
        val observer3 = TestObserver(::populateEvents)
        val observer2 =
            object : TestObserver(::populateEvents) {
                override fun onStop(owner: LifecycleOwner) {
                    super.onStop(owner)
                    mRegistry.removeObserver(observer3)
                    mRegistry.removeObserver(this)
                    mRegistry.removeObserver(observer1)
                    assertThat(mRegistry.observerCount).isEqualTo(0)
                }
            }
        mRegistry.addObserver(observer1)
        mRegistry.addObserver(observer2)
        mRegistry.addObserver(observer3)
        dispatchEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(events)
            .containsAtLeast(
                observer3 to Lifecycle.Event.ON_PAUSE,
                observer2 to Lifecycle.Event.ON_PAUSE,
                observer1 to Lifecycle.Event.ON_PAUSE,
            )
            .inOrder()
        assertThat(observer3.onPauseCallCount).isEqualTo(1)
        assertThat(observer2.onPauseCallCount).isEqualTo(1)
        assertThat(observer1.onPauseCallCount).isEqualTo(1)
        dispatchEvent(Lifecycle.Event.ON_STOP)
        assertThat(events)
            .containsAtLeast(
                observer3 to Lifecycle.Event.ON_STOP,
                observer2 to Lifecycle.Event.ON_STOP,
            )
            .inOrder()
        assertThat(observer1.onStopCallCount).isEqualTo(0)
        dispatchEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(observer3.onPauseCallCount).isEqualTo(1)
        assertThat(observer2.onPauseCallCount).isEqualTo(1)
        assertThat(observer1.onPauseCallCount).isEqualTo(1)
    }

    @Test
    fun deadParentInAddition() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        fullyInitializeRegistry()
        val observer2 = TestObserver(::populateEvents)
        val observer3 = TestObserver(::populateEvents)
        val observer1 =
            object : TestObserver(::populateEvents) {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    mRegistry.removeObserver(this)
                    assertThat(mRegistry.observerCount).isEqualTo(0)
                    mRegistry.addObserver(observer2)
                    mRegistry.addObserver(observer3)
                }
            }
        mRegistry.addObserver(observer1)
        assertThat(events)
            .containsExactly(
                observer1 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_START,
                observer2 to Lifecycle.Event.ON_CREATE,
                observer3 to Lifecycle.Event.ON_CREATE,
                observer2 to Lifecycle.Event.ON_START,
                observer2 to Lifecycle.Event.ON_RESUME,
                observer3 to Lifecycle.Event.ON_START,
                observer3 to Lifecycle.Event.ON_RESUME,
            )
            .inOrder()
    }

    @Test
    fun deadParentWhileTraversing() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        val observer2 = TestObserver(::populateEvents)
        val observer3 = TestObserver(::populateEvents)
        val observer1 =
            object : TestObserver(::populateEvents) {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    mRegistry.removeObserver(this)
                    assertThat(mRegistry.observerCount).isEqualTo(0)
                    mRegistry.addObserver(observer2)
                    mRegistry.addObserver(observer3)
                }
            }
        mRegistry.addObserver(observer1)
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        dispatchEvent(Lifecycle.Event.ON_START)
        assertThat(events)
            .containsExactly(
                observer1 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_START,
                observer2 to Lifecycle.Event.ON_CREATE,
                observer3 to Lifecycle.Event.ON_CREATE,
                observer2 to Lifecycle.Event.ON_START,
                observer3 to Lifecycle.Event.ON_START,
            )
            .inOrder()
    }

    @Test
    fun removeCascade() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        val observer3 = TestObserver(::populateEvents)
        val observer4 = TestObserver(::populateEvents)
        val observer2 =
            object : TestObserver(::populateEvents) {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    mRegistry.removeObserver(this)
                }
            }
        val observer1 =
            object : TestObserver(::populateEvents) {
                override fun onResume(owner: LifecycleOwner) {
                    super.onResume(owner)
                    mRegistry.removeObserver(this)
                    mRegistry.addObserver(observer2)
                    mRegistry.addObserver(observer3)
                    mRegistry.addObserver(observer4)
                }
            }
        fullyInitializeRegistry()
        mRegistry.addObserver(observer1)
        assertThat(events)
            .containsExactly(
                observer1 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_START,
                observer1 to Lifecycle.Event.ON_RESUME,
                observer2 to Lifecycle.Event.ON_CREATE,
                observer2 to Lifecycle.Event.ON_START,
                observer3 to Lifecycle.Event.ON_CREATE,
                observer3 to Lifecycle.Event.ON_START,
                observer4 to Lifecycle.Event.ON_CREATE,
                observer4 to Lifecycle.Event.ON_START,
                observer3 to Lifecycle.Event.ON_RESUME,
                observer4 to Lifecycle.Event.ON_RESUME,
            )
            .inOrder()
    }

    @Test
    fun changeStateDuringDescending() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        val observer2 = TestObserver(::populateEvents)
        val observer1 =
            object : TestObserver(::populateEvents) {
                override fun onPause(owner: LifecycleOwner) {
                    super.onPause(owner)
                    mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                    mRegistry.addObserver(observer2)
                }
            }
        fullyInitializeRegistry()
        mRegistry.addObserver(observer1)
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(events)
            .containsAtLeast(
                observer1 to Lifecycle.Event.ON_PAUSE,
                observer2 to Lifecycle.Event.ON_CREATE,
                observer2 to Lifecycle.Event.ON_START,
                observer1 to Lifecycle.Event.ON_RESUME,
                observer2 to Lifecycle.Event.ON_RESUME,
            )
            .inOrder()
    }

    @Test
    fun siblingLimitationCheck() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        fullyInitializeRegistry()
        val observer2 = TestObserver(::populateEvents)
        val observer3 = TestObserver(::populateEvents)
        val observer1 =
            object : TestObserver(::populateEvents) {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    mRegistry.addObserver(observer2)
                }

                override fun onResume(owner: LifecycleOwner) {
                    super.onResume(owner)
                    mRegistry.addObserver(observer3)
                }
            }
        mRegistry.addObserver(observer1)
        assertThat(events)
            .containsExactly(
                observer1 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_START,
                observer2 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_RESUME,
                observer3 to Lifecycle.Event.ON_CREATE,
                observer2 to Lifecycle.Event.ON_START,
                observer2 to Lifecycle.Event.ON_RESUME,
                observer3 to Lifecycle.Event.ON_START,
                observer3 to Lifecycle.Event.ON_RESUME,
            )
            .inOrder()
    }

    @Test
    fun siblingRemovalLimitationCheck1() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        fullyInitializeRegistry()
        val observer2 = TestObserver(::populateEvents)
        val observer3 = TestObserver(::populateEvents)
        val observer4 = TestObserver(::populateEvents)
        val observer1 =
            object : TestObserver(::populateEvents) {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    mRegistry.addObserver(observer2)
                }

                override fun onResume(owner: LifecycleOwner) {
                    super.onResume(owner)
                    mRegistry.removeObserver(observer2)
                    mRegistry.addObserver(observer3)
                    mRegistry.addObserver(observer4)
                }
            }
        mRegistry.addObserver(observer1)
        assertThat(events)
            .containsExactly(
                observer1 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_START,
                observer2 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_RESUME,
                observer3 to Lifecycle.Event.ON_CREATE,
                observer3 to Lifecycle.Event.ON_START,
                observer4 to Lifecycle.Event.ON_CREATE,
                observer4 to Lifecycle.Event.ON_START,
                observer3 to Lifecycle.Event.ON_RESUME,
                observer4 to Lifecycle.Event.ON_RESUME,
            )
            .inOrder()
    }

    @Test
    fun siblingRemovalLimitationCheck2() {
        val events = mutableListOf<Pair<LifecycleObserver, Lifecycle.Event>>()
        fun populateEvents(observer: LifecycleObserver, event: Lifecycle.Event) {
            events.add(observer to event)
        }
        fullyInitializeRegistry()
        val observer2 = TestObserver(::populateEvents)
        val observer3 =
            object : TestObserver(::populateEvents) {
                override fun onCreate(owner: LifecycleOwner) {
                    super.onCreate(owner)
                    mRegistry.removeObserver(observer2)
                }
            }
        val observer4 = TestObserver(::populateEvents)
        val observer1 =
            object : TestObserver(::populateEvents) {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    mRegistry.addObserver(observer2)
                }

                override fun onResume(owner: LifecycleOwner) {
                    super.onResume(owner)
                    mRegistry.addObserver(observer3)
                    mRegistry.addObserver(observer4)
                }
            }
        mRegistry.addObserver(observer1)
        assertThat(events)
            .containsExactly(
                observer1 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_START,
                observer2 to Lifecycle.Event.ON_CREATE,
                observer1 to Lifecycle.Event.ON_RESUME,
                observer3 to Lifecycle.Event.ON_CREATE,
                observer3 to Lifecycle.Event.ON_START,
                observer4 to Lifecycle.Event.ON_CREATE,
                observer4 to Lifecycle.Event.ON_START,
                observer3 to Lifecycle.Event.ON_RESUME,
                observer4 to Lifecycle.Event.ON_RESUME,
            )
            .inOrder()
    }

    @Test
    fun sameObserverReAddition() {
        val observer = TestObserver()
        mRegistry.addObserver(observer)
        mRegistry.removeObserver(observer)
        mRegistry.addObserver(observer)
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        assertThat(observer.onCreateCallCount).isEqualTo(1)
    }

    private fun dispatchEvent(event: Lifecycle.Event) {
        mRegistry.handleLifecycleEvent(event)
    }

    private fun fullyInitializeRegistry() {
        dispatchEvent(Lifecycle.Event.ON_CREATE)
        dispatchEvent(Lifecycle.Event.ON_START)
        dispatchEvent(Lifecycle.Event.ON_RESUME)
    }
}
