package foo;

import static com.android.support.lifecycle.Lifecycle.ON_START;
import static com.android.support.lifecycle.Lifecycle.ON_STOP;

import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.OnLifecycleEvent;

public class Bar {
    @OnLifecycleEvent(ON_START)
    public void doOnStart() {
    }

    @OnLifecycleEvent(ON_STOP)
    public void doOnStop1Arg(LifecycleProvider provider) {
    }

    @OnLifecycleEvent(ON_STOP)
    public void doOnStop2Args(LifecycleProvider provider, int lastEvent) {
    }

    public static class Inner1 {
        @OnLifecycleEvent(ON_START)
        public void doOnStart() {
        }

        public static class Inner2 {
            @OnLifecycleEvent(ON_START)
            public void doOnStart() {
            }

            public static class Inner3 {
                @OnLifecycleEvent(ON_START)
                public void doOnStart() {
                }
            }
        }
    }
}