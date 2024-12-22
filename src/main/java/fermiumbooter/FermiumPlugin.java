package fermiumbooter;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import java.util.function.Supplier;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Deprecated
@IFMLLoadingPlugin.Name("FermiumBooter")
@IFMLLoadingPlugin.SortingIndex(990)
public class FermiumPlugin implements IFMLLoadingPlugin, zone.rong.mixinbooter.IEarlyMixinLoader {

	public static final Logger LOGGER = LogManager.getLogger("FermiumBooterDepoliticization");

	public static File source = null;
	
	@Override public String[] getASMTransformerClass(){return null;}
	@Override public String getModContainerClass() {return "fermiumbooter.FermiumBooter"; }
	@Override public String getSetupClass(){return null;}
	@Override public String getAccessTransformerClass() {return null;}
	@Override public void injectData(Map<String, Object> data) {
		source = (File) data.get("coremodLocation");
	}

	@Override
    public List<String> getMixinConfigs(){
		return Arrays.asList(FermiumRegistryAPI.getEarlyMixins().keySet().toArray(new String[0]));
	}

	@Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        if (FermiumRegistryAPI.getRejectMixins().contains(mixinConfig)) {
			LOGGER.warn("FermiumBooter received removal of \"" + mixinConfig + "\" for early mixin application, rejecting.");
			return false;
		} else {
			List<Supplier<Boolean>> list = FermiumRegistryAPI.getEarlyMixins().get(mixinConfig);
			if (list != null) {
				Boolean enabled = null;
				for(Supplier<Boolean> supplier : list) {
					Boolean supplied = supplier.get();
					if (supplied == Boolean.TRUE) {
						LOGGER.info("FermiumBooter adding \"" + mixinConfig + "\" for early mixin application.");
						return true;
					}
					else if (supplied == null) LOGGER.warn("FermiumBooter received null value for individual supplier from \"" + mixinConfig + "\" for early mixin application.");
					else enabled = Boolean.FALSE;
				}
				if(enabled == null) {
					LOGGER.warn("FermiumBooter received null value for suppliers from \"" + mixinConfig + "\" for early mixin application, ignoring.");
				}
				return false;
			} else return true;
		}
    }
}