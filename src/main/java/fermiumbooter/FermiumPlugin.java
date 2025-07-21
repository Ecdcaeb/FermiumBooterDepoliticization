package fermiumbooter;

import java.io.File;
import java.util.*;
import java.util.function.BooleanSupplier;

import fermiumbooter.internal.DiscoveryHandler;
import fermiumbooter.internal.FBConfig;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Layer without codes from Fermium
*/
@Deprecated
@IFMLLoadingPlugin.Name("FermiumBooter")
@IFMLLoadingPlugin.SortingIndex(990)
@IFMLLoadingPlugin.DependsOn("MixinBooter")
public class FermiumPlugin
    implements IFMLLoadingPlugin, zone.rong.mixinbooter.IEarlyMixinLoader {
  static {
    com.cleanroommc.configanytime.ConfigAnytime.register(FBConfig.class);
  }
  
  public static final Logger LOGGER = LogManager.getLogger("FermiumBooterDepoliticization");

  public static File source = null;

  @Override
  public String[] getASMTransformerClass() {
    return null;
  }
  @Override
  public String getModContainerClass() {
    return "fermiumbooter.FermiumBooter";
  }
  @Override
  public String getSetupClass() {
    return null;
  }
  @Override
  public String getAccessTransformerClass() {
    return null;
  }
  @Override
  public void injectData(Map<String, Object> data) {
    source = (File) data.get("coremodLocation");
    if (source != null)
      makeFMLCorePluginContainsFMLMod(source);
    DiscoveryHandler discoveryHandler = new DiscoveryHandler();
    discoveryHandler.build();
    for (DiscoveryHandler.ASMData asmData : discoveryHandler.datas.get("Lnet/minecraftforge/fml/common/Mod;")) {
      if (asmData.values != null && asmData.values.containsKey("modid")) {
        FermiumRegistryAPI.mods.add((String) asmData.values.get("modid"));
      }
    }
    for (DiscoveryHandler.ASMData asmData : discoveryHandler.datas.get("Lfermiumbooter/annotations/MixinConfig;")) {
      if (asmData.values != null && asmData.values.containsKey("name")) {
        try {
          FermiumRegistryAPI.registerAnnotatedMixinConfig(Class.forName(asmData.className.replace('/', '.'), true, Launch.classLoader), null);
        } catch (Throwable t) {
          LOGGER.error(t);
        }
      }
    }
    for (String str : FBConfig.forcedEarlyMixinConfigRemovals) {
      FermiumRegistryAPI.removeMixin(str.trim());
    }
  }

  public static void makeFMLCorePluginContainsFMLMod(File file) {
    String name = file.getName();
    CoreModManager.getIgnoredMods().remove(name);
    CoreModManager.getReparseableCoremods().add(name);
  }

  @Override
  public List<String> getMixinConfigs() {
    return Arrays.asList(
        FermiumRegistryAPI.getEarlyMixins().keySet().toArray(new String[0]));
  }

  @Override
  public boolean shouldMixinConfigQueue(zone.rong.mixinbooter.Context mixinConfig) {
    FermiumRegistryAPI.activeContext = mixinConfig;
    {
      for (BooleanSupplier supplier :
          FermiumRegistryAPI.getEarlyMixins().get(mixinConfig.mixinConfig())) {
        if (supplier.getAsBoolean()) {
          FermiumPlugin.LOGGER.debug("FermiumBooter adding \"" + mixinConfig.mixinConfig()
              + "\" for early mixin application.");
          return true;
        }
      }
      FermiumPlugin.LOGGER.debug(
          "FermiumBooter received null value for suppliers from \""
          + mixinConfig.mixinConfig() + "\" for early mixin application, ignoring.");
      return false;
    }
  }
}
