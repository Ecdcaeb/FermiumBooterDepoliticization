package fermiumbooter;

import java.io.File;
import java.util.*;
import java.util.function.BooleanSupplier;

import fermiumbooter.internal.ConfigDiscover;
import fermiumbooter.internal.DiscoveryHandler;
import fermiumbooter.internal.FBConfig;
import fermiumbooter.internal.UpdateHelper;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cleanroommc.configanytime.ConfigAnytime;

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
    try {
        Launch.classLoader.findClass("fermiumbooter.FermiumRegistryAPI"); // force classLoader include
    } catch (Throwable ignored) { }
    ConfigAnytime.register(FBConfig.class);
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
  }

  public static void init() {
    // init special mod checks
    try {
        Class.forName("net.optifine.Log");
        FermiumRegistryAPI.mods.add("optifine");
      } catch (ClassNotFoundException ignored) {
    }
    //
    
    DiscoveryHandler discoveryHandler = new DiscoveryHandler();
    discoveryHandler.build();
    for (String str : FBConfig.forcedEarlyMixinConfigRemovals) {
      FermiumRegistryAPI.removeMixin(str.trim());
    }

    HashSet<String> usingFermiumBooterMods = new HashSet<>();

    for (DiscoveryHandler.ASMData asmData : discoveryHandler.datas.get("Lnet/minecraftforge/fml/common/Mod;")) {
      if (asmData.values != null) {
        if (asmData.values.containsKey("modid")) {
          String modId = (String) asmData.values.get("modid");
          FermiumRegistryAPI.mods.add(modId);
          LOGGER.debug("FOUND MODID {}", modId);
        }
        if (asmData.values.containsKey("dependencies")) {
          if (((String)asmData.values.get("dependencies")).contains(":fermiumbooter")) {
            if (asmData.values.containsKey("modid")) {
              String modId = (String)asmData.values.get("modid");
              usingFermiumBooterMods.add(modId);
              UpdateHelper.onModUsingFermiumbooter(modId);
            }
          }
        }
      }
    }

    ConfigDiscover.load(discoveryHandler);
  }

  public static void makeFMLCorePluginContainsFMLMod(File file) {
    String name = file.getName();
    CoreModManager.getIgnoredMods().remove(name);
    CoreModManager.getReparseableCoremods().add(name);
  }

  @Override
  public List<String> getMixinConfigs() {
    init();
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
      return false;
    }
  }
}
