package fermiumbooter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.*;
import java.lang.reflect.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import fermiumbooter.internal.UpdateHelper;

import com.cleanroommc.configanytime.ConfigAnytime;
import fermiumbooter.internal.FBConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.GlobalProperties;
import fermiumbooter.annotations.MixinConfig;

/**
 * Layer without codes from Fermium
*/
@Deprecated
public abstract class FermiumRegistryAPI {

  @Deprecated static zone.rong.mixinbooter.Context activeContext = null;
  static Set<String> mods = new HashSet<>();

  @Deprecated private static final Logger LOGGER = FermiumPlugin.LOGGER;

  @Deprecated private static Multimap<String, BooleanSupplier> earlyMixins = HashMultimap.create();
  @Deprecated private static Multimap<String, BooleanSupplier> lateMixins = HashMultimap.create();
  @Deprecated private static Collection<String> rejectMixins;

  static {
    if (GlobalProperties.get(GlobalProperties.Keys.CLEANROOM_DISABLE_MIXIN_CONFIGS) == null) {
      GlobalProperties.put(GlobalProperties.Keys.CLEANROOM_DISABLE_MIXIN_CONFIGS, new HashSet<>());
    }
    rejectMixins = (Collection<String>) GlobalProperties.get(GlobalProperties.Keys.CLEANROOM_DISABLE_MIXIN_CONFIGS);
  }
  /**
   * Register multiple mixin config resources at once to be applied
   * @param late - whether to apply the mixin late or early
   * @param configurations - mixin config resource names
   */
  @Deprecated
  public static void enqueueMixin(boolean late, String... configurations) {
    for (String configuration : configurations) {
      enqueueMixin(late, configuration);
    }
  }

  /**
   * Register a mixin config resource to be applied
   * @param late - whether to apply the mixin late or early
   * @param configuration - mixin config resource name
   */
  @Deprecated
  public static void enqueueMixin(boolean late, String configuration) {
    enqueueMixin(late, configuration, () -> true);
  }

  /**
   * Add a mixin config resource to be applied, with a toggle to apply or not
   * Note: I do not think this specific method is necessary, but it's here just in case
   * @param late - whether to apply the mixin late or early
   * @param configuration - mixin config resource name
   * @param enabled - whether to apply the mixin or not
   */
  @Deprecated
  public static void enqueueMixin(boolean late, String configuration, boolean enabled) {
    if (enabled) enqueueMixin(late, configuration, () -> true);
    else enqueueMixin(late, configuration, () -> false);
  }

  /**
   * Add a mixin config resource to be applied, with a supplier to toggle application to be evaluated after all like-timed configs are registered
   * Note: If multiple suppliers are given for a single configuration, it is evaluated as OR
   * @param late - whether to apply the mixin late or early
   * @param configuration - mixin config resource name
   * @param supplier - supplier to determine whether to apply the mixin or not
   */
  @Deprecated
  public static void enqueueMixin(boolean late, String configuration, BooleanSupplier supplier) {
    checkState();
    if (configuration == null || configuration.trim().isEmpty()) {
      LOGGER.error("FermiumRegistryAPI supplied null or empty configuration name during mixin enqueue, ignoring.");
      return;
    }
    if (supplier == null) { // Do not evaluate supplier.get() itself for null now
      LOGGER.warn("FermiumRegistryAPI supplied null supplier for configuration \"" + configuration + "\" during mixin enqueue, ignoring.");
      return;
    }
    // Process rejects prior to application
    if (late && !FBConfig.Utils.forcedEarlyMixinConfigAdditionsSet.contains(configuration)) {
      LOGGER.debug("FermiumRegistryAPI supplied \"" + configuration + "\" for late mixin enqueue, adding.");
      lateMixins.put(configuration, supplier);
    } else {
      LOGGER.debug("FermiumRegistryAPI supplied \"" + configuration + "\" for early mixin enqueue, adding.");
      earlyMixins.put(configuration, supplier);
    }
  }

  /**
   * Add a mixin config resource to be applied, with a supplier to toggle application to be evaluated after all like-timed configs are registered
   * Note: If multiple suppliers are given for a single configuration, it is evaluated as OR
   * @param late - whether to apply the mixin late or early
   * @param configuration - mixin config resource name
   * @param supplier - supplier to determine whether to apply the mixin or not
   */
  @Deprecated
  public static void enqueueMixin(boolean late, String configuration, Supplier<Boolean> supplier) {
    enqueueMixin(late, configuration, () -> supplier.get().booleanValue());
  }

  /**
   * Designates a mixin config resource name to be ignored before application (Will only affect FermiumBooter applied mixins)
   * Note: Realistically you should not use this, but it is provided in the case of specific tweaker mod needs
   * @param configuration - mixin config resource name
   */
  @Deprecated
  public static void removeMixin(String configuration) {
    checkState();
    if (configuration == null || configuration.trim().isEmpty()) {
      LOGGER.debug("FermiumRegistryAPI supplied null or empty configuration name for mixin removal, ignoring.");
      return;
    }
    LOGGER.debug("FermiumRegistryAPI supplied \"" + configuration + "\" for mixin removal, adding.");
    rejectMixins.add(configuration);
  }
  
  // why static instead of dynamic
  public static boolean isModPresent(String modid) {
    return (activeContext != null && activeContext.isModPresent(modid)) || mods.contains(modid);
  }

  // crazy config handler, fermuim is too crazy.
  // removed at fermuim 1.3
  public static <T> void registerAnnotatedMixinConfig(Class<T> clazz, T instance) {
    UpdateHelper.onModRegisterV1_2Config(clazz.getName());
    ConfigAnytime.register(clazz); // wtf a instance here?
    searchForMixinConfig(clazz, instance);
  }

  private static void checkState() {
    if (earlyMixins == null)
      throw new IllegalStateException("Mixins should be registered before ModConstruction");
    if (lateMixins == null)
      throw new IllegalStateException("Mixins should be registered before ModConstruction");
    if (rejectMixins == null)
      throw new IllegalStateException("Mixins should be registered before ModConstruction");
  }

  // internal use methods should be package private.

  @Deprecated
  static Multimap<String, BooleanSupplier> getEarlyMixins() {
    return earlyMixins;
  }

  @Deprecated
  static Multimap<String, BooleanSupplier> getLateMixins() {
    return lateMixins;
  }

  static void searchForMixinConfig(final Class<?> clazz, final Object instance) {
    try {
    for (Field f : clazz.getDeclaredFields()) {
      if(f.isAnnotationPresent(MixinConfig.SubInstance.class)) {
				searchForMixinConfig(f.getType(), f.get(instance));
			} else if (f.isAnnotationPresent(MixinConfig.EarlyMixin.class)) {
        final MixinConfig.EarlyMixin earlyMixin = f.getAnnotation(MixinConfig.EarlyMixin.class);
        final Field field = f;
        enqueueMixin(false, earlyMixin.name(), ()->
        {
          if(FBConfig.overrideMixinCompatibilityChecks) {
            boolean disableMixin = false;
            for (MixinConfig.CompatHandling compat : field.getAnnotationsByType(MixinConfig.CompatHandling.class)) {
              if (compat.desired() != isModPresent(compat.modid())) {
                LOGGER.error(
                  "FermiumBooterDepoliticization annotated mixin config {} from {} {} {} {}: {}.",
                  field.getName(), earlyMixin.name(),
                  compat.disableMixin() ? "disabled as incompatible" : "may have issues",
                  compat.desired() ? "without" : "with", 
                  compat.modid(), compat.reason());
                if (compat.disableMixin()) {
                  disableMixin = true;
                }
              }
            }
            if (disableMixin) {
              return false;
            }
          }
          try {
            return field.getBoolean(instance);
          } catch (Throwable t) {
            LOGGER.error("FermiumBooterDepoliticization failed to parse provided config class " + clazz.getName(), t);
            return false;
          }
        });
      } else if (f.isAnnotationPresent(MixinConfig.LateMixin.class)) {
        final MixinConfig.LateMixin earlyMixin = f.getAnnotation(MixinConfig.LateMixin.class);
        final Field field = f;
        enqueueMixin(true, earlyMixin.name(), ()->
        {
          if(FBConfig.overrideMixinCompatibilityChecks) {
            boolean disableMixin = false;
            for (MixinConfig.CompatHandling compat : field.getAnnotationsByType(MixinConfig.CompatHandling.class)) {
              if (compat.desired() != isModPresent(compat.modid())) {
                LOGGER.error(
                  "FermiumBooterDepoliticization annotated mixin config {} from {} {} {} {}: {}.",
                  field.getName(), earlyMixin.name(),
                  compat.disableMixin() ? "disabled as incompatible" : "may have issues",
                  compat.desired() ? "without" : "with", 
                  compat.modid(), compat.reason());
                if (compat.disableMixin()) {
                  disableMixin = true;
                }
              }
            }
            if (disableMixin) {
              return false;
            }
          }
          try {
            return field.getBoolean(instance);
          } catch (Throwable t) {
            LOGGER.error("FermiumBooterDepoliticization failed to parse provided config class " + clazz.getName(), t);
            return false;
          }
        });
      } else if (f.isAnnotationPresent(MixinConfig.MixinToggle.class)) {
        final MixinConfig.MixinToggle mixinToggle = f.getAnnotation(MixinConfig.MixinToggle.class);
        final Field field = f;
        if (StringUtils.isNotEmpty(mixinToggle.earlyMixin())) {
          enqueueMixin(false, mixinToggle.earlyMixin(), ()->
          {
            if(FBConfig.overrideMixinCompatibilityChecks) {
              for (MixinConfig.CompatHandling compat : field.getAnnotationsByType(MixinConfig.CompatHandling.class)) {
                if (compat.desired() != isModPresent(compat.modid())) {
                  LOGGER.error(
                          "FermiumBooterDepoliticization annotated mixin config {} from {} {} {} {}: {}.",
                          field.getName(), mixinToggle.earlyMixin(),
                          compat.disableMixin() ? "disabled as incompatible" : "may have issues",
                          compat.desired() ? "without" : "with",
                          compat.modid(), compat.reason());
                  if (compat.disableMixin()) {
                    return false;
                  }
                }
              }
            }
            try {
              return field.getBoolean(instance);
            } catch (Throwable t) {
              LOGGER.error("FermiumBooterDepoliticization failed to parse provided config class " + clazz.getName(), t);
              return mixinToggle.defaultValue();
            }
          });
        }
        if (StringUtils.isNotEmpty(mixinToggle.lateMixin())) {
          enqueueMixin(true, mixinToggle.lateMixin(), ()->
          {
            if(FBConfig.overrideMixinCompatibilityChecks) {
              for (MixinConfig.CompatHandling compat : field.getAnnotationsByType(MixinConfig.CompatHandling.class)) {
                if (compat.desired() != isModPresent(compat.modid())) {
                  LOGGER.error(
                          "FermiumBooterDepoliticization annotated mixin config {} from {} {} {} {}: {}.",
                          field.getName(), mixinToggle.lateMixin(),
                          compat.disableMixin() ? "disabled as incompatible" : "may have issues",
                          compat.desired() ? "without" : "with",
                          compat.modid(), compat.reason());
                  if (compat.disableMixin()) {
                    return false;
                  }
                }
              }
            }
            try {
              return field.getBoolean(instance);
            } catch (Throwable t) {
              LOGGER.error("FermiumBooterDepoliticization failed to parse provided config class " + clazz.getName(), t);
              return mixinToggle.defaultValue();
            }
          });
        }
      }
    }
    } catch (Throwable t) {
      LOGGER.error("FermiumBooterDepoliticization failed to parse provided config class " + clazz.getName(), t);
    }
  }

  @Deprecated
  static void clear() {
    // :)
    earlyMixins = null;
    lateMixins = null;
    rejectMixins = null;
    activeContext = null;
    mods = null;
  }
  
}