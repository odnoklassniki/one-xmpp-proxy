package one.ejb;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This can be applied to a package (in package-info.java), class or method to enforce nullability
 * for all enclosed elements. The annotation alone enforces non nullability of enclosed method
 * return value and parameter values.
 * <p>
 * If applied with a an argument i.e. NonNullBeDefault (false), it will cancel out any global
 * default for the particular element.
 * 
 * @author sergey.vladimirov
 */
@NotNullByDefault(false)
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD })
public @interface NotNullByDefault {
    boolean value() default true;
}
