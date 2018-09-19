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

package androidx.ui.gestures.pointer_router

import org.hamcrest.CoreMatchers.`is`
import androidx.ui.engine.geometry.Offset
import androidx.ui.flutter_test.TestPointer
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.assertions.FlutterErrorDetails
import androidx.ui.gestures.events.PointerEvent
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PointerRouterTest {

    @Test
    fun `Should route pointers`() {
        var callbackRan = false
        val callback = { _: PointerEvent ->
            callbackRan = true
        }

        val pointer2 = TestPointer(2)
        val pointer3 = TestPointer(3)

        val router = PointerRouter()
        router.addRoute(3, callback)
        router.route(pointer2.down(Offset.zero))
        assertThat(callbackRan, `is`(false))
        router.route(pointer3.down(Offset.zero))
        assertThat(callbackRan, `is`(true))
        callbackRan = false
        router.removeRoute(3, callback)
        router.route(pointer3.up())
        assertThat(callbackRan, `is`(false))
    }

    @Test
    fun `Supports re-entrant cancellation`() {
        var callbackRan = false
        val callback = { _: PointerEvent ->
            callbackRan = true
        }
        val router = PointerRouter()
        router.addRoute(2) { _: PointerEvent ->
            router.removeRoute(2, callback)
        }
        router.addRoute(2, callback)
        val pointer2 = TestPointer(2)
        router.route(pointer2.down(Offset.zero))
        assertThat(callbackRan, `is`(false))
    }

    @Test
    fun `Supports global callbacks`() {
        var secondCallbackRan = false
        val secondCallback = { _: PointerEvent ->
            secondCallbackRan = true
        }

        var firstCallbackRan = false
        val router = PointerRouter()
        router.addGlobalRoute { _: PointerEvent ->
            firstCallbackRan = true
            router.addGlobalRoute(secondCallback)
        }

        val pointer2 = TestPointer(2)
        router.route(pointer2.down(Offset.zero))
        assertThat(firstCallbackRan, `is`(true))
        assertThat(secondCallbackRan, `is`(false))
    }

    @Test
    fun `Supports re-entrant global cancellation`() {
        var callbackRan = false
        val callback = { _: PointerEvent ->
            callbackRan = true
        }
        val router = PointerRouter()
        router.addGlobalRoute { _: PointerEvent ->
            router.removeGlobalRoute(callback)
        }
        router.addGlobalRoute(callback)
        val pointer2 = TestPointer(2)
        router.route(pointer2.down(Offset.zero))
        assertThat(callbackRan, `is`(false))
    }

    @Test
    fun `Per-pointer callbacks cannot re-entrantly add global routes`() {
        var callbackRan = false
        val callback = { _: PointerEvent ->
            callbackRan = true
        }
        val router = PointerRouter()
        var perPointerCallbackRan = false
        router.addRoute(2) { _: PointerEvent ->
            perPointerCallbackRan = true
            router.addGlobalRoute(callback)
        }
        val pointer2 = TestPointer(2)
        router.route(pointer2.down(Offset.zero))
        assertThat(perPointerCallbackRan, `is`(true))
        assertThat(callbackRan, `is`(false))
    }

    @Test
    fun `Per-pointer callbacks happen before global callbacks`() {
        val log = mutableListOf<String>()
        val router = PointerRouter()
        router.addGlobalRoute { _: PointerEvent ->
            log.add("global 1")
        }
        router.addRoute(2) { _: PointerEvent ->
            log.add("per-pointer 1")
        }
        router.addGlobalRoute { _: PointerEvent ->
            log.add("global 2")
        }
        router.addRoute(2) { _: PointerEvent ->
            log.add("per-pointer 2")
        }
        val pointer2 = TestPointer(2)
        router.route(pointer2.down(Offset.zero))
        assertThat(
            log, `is`(
                equalTo(
                    listOf(
                        "per-pointer 1",
                        "per-pointer 2",
                        "global 1",
                        "global 2"
                    )
                )
            )
        )
    }

    @Test
    fun `Exceptions do not stop pointer routing`() {
        val log = mutableListOf<String>()
        val router = PointerRouter()
        router.addRoute(2) { _: PointerEvent ->
            log.add("per-pointer 1")
        }
        router.addRoute(2) { _: PointerEvent ->
            log.add("per-pointer 2")
            throw Exception("Having a bad day!")
        }
        router.addRoute(2) { _: PointerEvent ->
            log.add("per-pointer 3")
        }

        val previousErrorHandler = FlutterError.onError
        FlutterError.onError = { _: FlutterErrorDetails ->
            log.add("error report")
        }

        val pointer2 = TestPointer(2)
        router.route(pointer2.down(Offset.zero))
        assertThat(
            log, `is`(
                equalTo(
                    listOf(
                        "per-pointer 1",
                        "per-pointer 2",
                        "error report",
                        "per-pointer 3"
                    )
                )
            )
        )

        FlutterError.onError = previousErrorHandler
    }
}