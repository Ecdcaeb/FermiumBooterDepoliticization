package fermiumbooter.internal;

import java.util.*;
import org.apache.logging.log4j.Logger;
import fermiumbooter.FermiumPlugin;

public class UpdateHelper {
    private static final Logger LOGGER = FermiumPlugin.LOGGER;

    public static void onModUsingFermiumbooter(String modid) {
        if (FBConfig.displayUpdateHelperAtLog) {
            LOGGER.warn("discover mod {} is using Fermiumbooter");
        }
    }

    public static void onModRegisterV1_2Config(String modid) {
        if (FBConfig.displayUpdateHelperAtLog) {
            LOGGER.warn("discover mod {} is using Fermiumbooter 1.2, it is incapable with fermiumbooter 1.3. However, FermiumBooterDepoliticization make it capable.");
            LOGGER.warn("You need to update this mod.");
        }
    }
}