package c98.core.launch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) public @interface ASMer {
	public String[] value() default "";
	public boolean subclasses() default false;
	public boolean log() default false;
}
