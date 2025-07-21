package fermiumbooter.internal;

import net.minecraftforge.common.config.Config;
import java.util.*;

@Config(modid = "fermiumbooter")
public class FBConfig {
	
	@Config.Name("Override Mixin Config Compatibility Checks")
	public static boolean overrideMixinCompatibilityChecks = false;

	@Config.Name("Forced Early Mixin Config Additions")
	public static String[] forcedEarlyMixinConfigAdditions = {};
	
	@Config.Name("Forced Early Mixin Config Removals")
	public static String[] forcedEarlyMixinConfigRemovals = {};

	public static class Utils {
		public static final Set<String> forcedEarlyMixinConfigAdditionsSet = new HashSet<>(Arrays.asList(FBConfig.forcedEarlyMixinConfigAdditions));
	}
}