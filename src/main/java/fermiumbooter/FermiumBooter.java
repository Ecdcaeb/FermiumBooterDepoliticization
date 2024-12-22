package fermiumbooter;

import java.util.*;
import net.minecraftforge.fml.common.*;
import com.google.common.eventbus.EventBus;

import java.util.function.Supplier;

@Deprecated
public class FermiumBooter extends net.minecraftforge.fml.common.DummyModContainer implements zone.rong.mixinbooter.ILateMixinLoader
{
    public static final String MODID = "fermiumbooter";
    public static final String VERSION = "1.1.1";
    public static final String NAME = "FermiumBooterDepoliticization";
	
	public FermiumBooter() {
		super(new ModMetadata());
        ModMetadata metadata = this.getMetadata();
        metadata.modId = MODID;
        metadata.name = NAME;
        metadata.authorList.add("Hileb");
        metadata.version = VERSION;
        metadata.credits = "\n" +
            "       MixinBooter - it is a true mixin lib with tech.\n";
        metadata.description = "Defenders against destroying communities";
        metadata.url = "https://github.com/Ecdcaeb/FermiumBooterDepoliticization";
        metadata.logoFile ="assets/icon.png";
	}

	@Override
    public boolean registerBus(EventBus bus, LoadController controller) {  
        return true;
    }

    @Override
    public Set<net.minecraftforge.fml.common.versioning.ArtifactVersion> getRequirements() {
        return Collections.singleton(new net.minecraftforge.fml.common.versioning.DefaultArtifactVersion("mixinbooter"));
    }
    
    @Override
    public List<String> getMixinConfigs(){
		return Arrays.asList(FermiumRegistryAPI.getLateMixins().entrySet().toArray(new String[0]));
	}

	@Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        if (FermiumRegistryAPI.getRejectMixins().contains(mixinConfig)) {
			FermiumPlugin.LOGGER.warn("FermiumBooter received removal of \"" + mixinConfig + "\" for early mixin application, rejecting.");
			return false;
		} else {
			List<Supplier<Boolean>> list = FermiumRegistryAPI.getLateMixins().get(mixinConfig);
			if (list != null) {
				Boolean enabled = null;
				for(Supplier<Boolean> supplier : list) {
					Boolean supplied = supplier.get();
					if (supplied == Boolean.TRUE) {
						FermiumPlugin.LOGGER.info("FermiumBooter adding \"" + mixinConfig + "\" for early mixin application.");
						return true;
					}
					else if (supplied == null) FermiumPlugin.LOGGER.warn("FermiumBooter received null value for individual supplier from \"" + mixinConfig + "\" for early mixin application.");
					else enabled = Boolean.FALSE;
				}
				if(enabled == null) {
					FermiumPlugin.LOGGER.warn("FermiumBooter received null value for suppliers from \"" + mixinConfig + "\" for early mixin application, ignoring.");
				}
				return false;
			} else return true;
		}
    }
}