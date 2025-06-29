package fermiumbooter.annotations;

import java.lang.annotation.*;

/**
 * Layer without codes from Fermium
*/
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MixinConfig {

	String name() default "";

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface SubInstance {}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface EarlyMixin {
		String name();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface LateMixin {
		String name();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface MixinToggle {
		String earlyMixin() default "";
		String lateMixin() default "";
		boolean defaultValue();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Repeatable(CompatHandlings.class)
	@interface CompatHandling {
		String modid();
		boolean desired() default true;
		boolean disableMixin() default true;
		String reason() default "Undefined";
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface CompatHandlings {
		CompatHandling[] value();
	}
}