package foo;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

public class Bar implements LifecycleObserver {
    @OnLifecycleEvent(ON_START)
    public void doOnStart() {
    }

    @OnLifecycleEvent(ON_STOP)
    public void doOnStop1Arg(LifecycleOwner provider) {
    }

    @OnLifecycleEvent(ON_STOP)
    public void doOnStop2Args(LifecycleOwner provider) {
    }

    public static class Inner1 implements LifecycleObserver {
        @OnLifecycleEvent(ON_START)
        public void doOnStart() {
        }

        public static class Inner2 implements LifecycleObserver {
            @OnLifecycleEvent(ON_START)
            public void doOnStart() {
            }

            public static class Inner3 implements LifecycleObserver {
                @OnLifecycleEvent(ON_START)
                public void doOnStart() {
                }
            }
        }
    }
}
