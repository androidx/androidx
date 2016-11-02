package foo;
import com.android.support.lifecycle.OnState;
import static com.android.support.lifecycle.Lifecycle.STARTED;
import static com.android.support.lifecycle.Lifecycle.STOPPED;
import com.android.support.lifecycle.LifecycleProvider;

public class Bar {
    @OnState(STARTED)
    public void doOnStart(){}
    @OnState(STOPPED)
    public void doOnStop1Arg(LifecycleProvider provider){}
    @OnState(STOPPED)
    public void doOnStop2Args(LifecycleProvider provider, int prevState){}
    public static class Inner1 {
        @OnState(STARTED)
        public void doOnStart(){}
        public static class Inner2 {
            @OnState(STARTED)
            public void doOnStart(){}
            public static class Inner3 {
                @OnState(STARTED)
                public void doOnStart(){}
            }
        }
    }
}