package fermiumbooter.internal;

import fermiumbooter.FermiumPlugin;
import fermiumbooter.FermiumRegistryAPI;
import fermiumbooter.annotations.MixinConfig;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Config;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.util.Annotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigDiscover {

    private static final Logger LOGGER = FermiumPlugin.LOGGER;

    public static void load(DiscoveryHandler discoveryHandler) {
        HashMap<String, HashMap<String, Boolean>> configMap = new HashMap<>();
        for (DiscoveryHandler.ASMData asmData : discoveryHandler.datas.get("Lfermiumbooter/annotations/MixinConfig;")) {
            if (asmData.values != null && asmData.values.containsKey("name")) {
                String name = (String) asmData.values.get("name");
                if (!configMap.containsKey(name)) {
                    configMap.computeIfAbsent(name, (k)->new HashMap<>());
                    try (Stream<String> lines = Files.lines(new File(Launch.minecraftHome, "config/" + name + ".cfg").toPath())) {
                        for (String line : lines.map(String::trim).filter((s) -> s.startsWith("B:")).collect(Collectors.toList())) {
                            String name = line.substring(2, line.lastIndexOf('='));
                            String value = line.substring(line.lastIndexOf('=') + 1);
                            configMap.computeIfAbsent(name, (k)->new HashMap<>())
                                    .put(name, Boolean.parseBoolean(value));
                        }
                    } catch (IOException e) {
                        LOGGER.error("Could not read config {}.cfg for {}" ,name, asmData.className, e);
                    }
                }
                for (FieldNode fn : asmData.classNode.fields) {
                    AnnotationNode mixinToggle = Annotations.getVisible(fn, MixinConfig.MixinToggle.class);
                    if (mixinToggle != null) {
                        final String fname;
                        {
                            AnnotationNode nameNode = Annotations.getVisible(fn, Config.Name.class);
                            if (nameNode != null) {
                                fname = Annotations.getValue(nameNode);
                            } else {
                                fname = fn.name;
                            }
                        }

                        final String earlyMixin = Annotations.getValue(mixinToggle, "earlyMixin", (String) null);
                        final String lateMixin = Annotations.getValue(mixinToggle, "lateMixin", (String) null);
                        final boolean defaultValue = Annotations.getValue(mixinToggle, "defaultValue", Boolean.FALSE);
                        final HashSet<CompatHandingRecord> compatHandingRecords = new HashSet<>();
                        {
                            AnnotationNode compatHandlings = Annotations.getVisible(fn, MixinConfig.CompatHandlings.class);
                            if (compatHandlings != null) {
                                AnnotationNode[] compatHandlingArray = Annotations.getValue(compatHandlings, "value", (AnnotationNode[]) null);
                                if (compatHandlingArray != null) {
                                    for (AnnotationNode node : compatHandlingArray) {
                                        compatHandingRecords.add(new CompatHandingRecord(node));
                                    }
                                }
                            }
                        }

                        final boolean configValue;
                        {
                            if (configMap.containsKey(name)) {
                                if (configMap.get(name).containsKey(fname)) {
                                    configValue = configMap.get(name).get(fname);
                                } else configValue = configMap.get(name).getOrDefault('"' + fname + '"', defaultValue);
                            } else configValue = defaultValue;
                        }

                        if (earlyMixin != null) {
                            if (compatHandingRecords.isEmpty()) {
                                FermiumRegistryAPI.enqueueMixin(false, earlyMixin, configValue);
                            } else {
                                FermiumRegistryAPI.enqueueMixin(false, earlyMixin, ()->
                                {
                                    if(FBConfig.overrideMixinCompatibilityChecks) {
                                        for (CompatHandingRecord compat : compatHandingRecords) {
                                            if (compat.desired != FermiumRegistryAPI.isModPresent(compat.modid)) {
                                                LOGGER.error(
                                                        "FermiumBooterDepoliticization annotated mixin config {} from {} {} {} {}: {}.",
                                                        fname, lateMixin,
                                                        compat.disableMixin ? "disabled as incompatible" : "may have issues",
                                                        compat.desired ? "without" : "with",
                                                        compat.modid, compat.reason);
                                                if (compat.disableMixin) {
                                                    return false;
                                                }
                                            }
                                        }
                                    }
                                    return configValue;
                                });
                            }
                        }
                        if (lateMixin != null) {
                            if (compatHandingRecords.isEmpty()) {
                                FermiumRegistryAPI.enqueueMixin(true, lateMixin, configValue);
                            } else {
                                FermiumRegistryAPI.enqueueMixin(true, lateMixin, ()->
                                {
                                    if(FBConfig.overrideMixinCompatibilityChecks) {
                                        for (CompatHandingRecord compat : compatHandingRecords) {
                                            if (compat.desired != FermiumRegistryAPI.isModPresent(compat.modid)) {
                                                LOGGER.error(
                                                        "FermiumBooterDepoliticization annotated mixin config {} from {} {} {} {}: {}.",
                                                        fname, lateMixin,
                                                        compat.disableMixin ? "disabled as incompatible" : "may have issues",
                                                        compat.desired ? "without" : "with",
                                                        compat.modid, compat.reason);
                                                if (compat.disableMixin) {
                                                    return false;
                                                }
                                            }
                                        }
                                    }
                                    return configValue;
                                });
                            }
                        }
                    }
                }

            }
        }
    }

    public static class CompatHandingRecord{
        public final String modid;
        public final boolean desired;
        public final boolean disableMixin;
        public final String reason;

        public CompatHandingRecord(AnnotationNode annotationNode) {
            this.modid = Annotations.getValue(annotationNode, "modid");
            this.desired = Annotations.getValue(annotationNode, "desired", Boolean.TRUE);
            this.disableMixin = Annotations.getValue(annotationNode, "disableMixin", Boolean.TRUE);
            this.reason = Annotations.getValue(annotationNode, "reason", "Undefined");
        }

        @Override
        public String toString() {
            return "CompatHandingRecord{" +
                    "modid='" + modid + '\'' +
                    ", desired=" + desired +
                    ", disableMixin=" + disableMixin +
                    ", reason='" + reason + '\'' +
                    '}';
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof CompatHandingRecord)) return false;

            CompatHandingRecord that = (CompatHandingRecord) o;
            return desired == that.desired && disableMixin == that.disableMixin && Objects.equals(modid, that.modid) && Objects.equals(reason, that.reason);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(modid);
            result = 31 * result + Boolean.hashCode(desired);
            result = 31 * result + Boolean.hashCode(disableMixin);
            result = 31 * result + Objects.hashCode(reason);
            return result;
        }
    }
}
