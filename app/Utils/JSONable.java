package Utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JSONable {
	boolean mandatory() default false;
	boolean defaultField() default true;
	boolean excluded() default false;
	String name() default "";
}
