package foo;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.OnLifecycleEvent;

public class DerivedFromJar extends test.library.LibraryBaseObserver {
    @OnLifecycleEvent(ON_START)
    public void doAnother() {
    }
}
