package dagger.hilt.android;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
public @interface AndroidEntryPoint {
    Class<?> value() default Void.class;
}