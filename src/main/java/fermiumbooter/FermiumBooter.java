package fermiumbooter;

import com.google.common.eventbus.Subscribe;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.versioning.VersionRange;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;

/**
 * Layer without codes from Fermium
*/
@Deprecated
public class FermiumBooter
    extends net.minecraftforge.fml.common.DummyModContainer
    implements zone.rong.mixinbooter.ILateMixinLoader {
  public static final String MODID = Tags.MOD_ID;
  public static final String VERSION = Tags.VERSION;
  public static final String NAME = Tags.MOD_NAME;

  public FermiumBooter() {
    super(new ModMetadata());
    ModMetadata metadata = this.getMetadata();
    metadata.modId = MODID;
    metadata.name = NAME;
    metadata.authorList.add("Hileb");
    metadata.version = VERSION;
    metadata.credits = "\n"
        + "       MixinBooter - it is a true mixin lib with tech.\n";
    metadata.description = "Defenders against destroying communities";
    metadata.url = "https://github.com/Ecdcaeb/FermiumBooterDepoliticization";
    metadata.logoFile = "assets/fermiumbooter/icon.png";
  }

  @Override
  public boolean registerBus(com.google.common.eventbus.EventBus bus,
      net.minecraftforge.fml.common.LoadController controller) {
    bus.register(this);
    return true;
  }

  @Subscribe
  @SuppressWarnings("unused")
  public void onConstructed(FMLConstructionEvent event) {
    FermiumRegistryAPI.clear();
  }

  @Override
  public Set<ArtifactVersion>
  getRequirements() {
      try {
    return Collections.singleton(
        new DefaultArtifactVersion("mixinbooter", VersionRange.createFromVersionSpec("[10,)")));
      } catch (Throwable t) {
          return Collections.singleton(
        new DefaultArtifactVersion("mixinbooter", true));
      }
  }

  @Override
  public List<String> getMixinConfigs() {
    return Arrays.asList(
        FermiumRegistryAPI.getLateMixins().keySet().toArray(new String[0]));
  }

  @Override
  public boolean shouldMixinConfigQueue(zone.rong.mixinbooter.Context mixinConfig) {
    FermiumRegistryAPI.activeContext = mixinConfig;
    {
      for (BooleanSupplier supplier :
          FermiumRegistryAPI.getLateMixins().get(mixinConfig.mixinConfig())) {
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
