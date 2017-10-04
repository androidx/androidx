package foo;

import static android.arch.lifecycle.Lifecycle.Event.ON_ANY;

import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;

public class TooManyArgs1 implements LifecycleObserver {
    @OnLifecycleEvent(ON_ANY)
    public void onAny(LifecycleOwner provider, Event event, int x) {
    }
}
