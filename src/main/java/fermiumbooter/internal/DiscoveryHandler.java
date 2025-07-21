package fermiumbooter.internal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import fermiumbooter.FermiumPlugin;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.discovery.asm.ASMModParser;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotationVisitor;
import net.minecraftforge.fml.relauncher.libraries.Artifact;
import net.minecraftforge.fml.relauncher.libraries.LibraryManager;
import net.minecraftforge.fml.relauncher.libraries.Repository;
import org.apache.commons.lang3.SystemUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DiscoveryHandler {

    public static class ASMData {
        public final File source;
        public final String className;
        public final String annName;
        public final Map<String, Object> values;

        public ASMData(File source, String className, String annName, Map<String, Object> values) {
            this.source = source;
            this.className = className;
            this.annName = annName;
            this.values = values;
        }
    }
    public final SetMultimap<String, ASMData> datas = HashMultimap.create();

    public static final Pattern classFile = Pattern.compile("[^\\s\\$]+(\\$[^\\s]+)?\\.class$");

    public static final int ASM = SystemUtils.IS_JAVA_1_8 ? (5 << 16 | 0 << 8) : (9 << 16 | 0 << 8); // ASM5 : ASM9

    public void addFor(File modFile) {
        FileSystem fs = null;
        try
        {
            Path root = null;

            if (modFile.isFile())
            {
                try
                {
                    fs = FileSystems.newFileSystem(modFile.toPath(), (ClassLoader) null);
                    root = fs.getPath("/");
                }
                catch (IOException e)
                {
                    FermiumPlugin.LOGGER.error("Error loading FileSystem from jar: ", e);
                    return;
                }
            }
            else if (modFile.isDirectory())
            {
                root = modFile.toPath();
            }

            if (root == null || !Files.exists(root))
                return;


            Iterator<Path> itr;
            try(Stream<Path> stream = Files.walk(root))
            {
                itr = stream.filter(Files::isReadable).iterator();
                while (itr.hasNext())
                {
                    Path rep = itr.next();
                    String name = root.relativize(rep).toString();
                    if (classFile.matcher(name).find()) {
                        try (InputStream inputStream = Files.newInputStream(rep)) {
                            ClassReader classReader = new ClassReader(inputStream);
                            ClassNode classNode = new ClassNode();
                            classReader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                            if (classNode.interfaces != null) for (String itf : classNode.interfaces) {
                                this.datas.put(itf, new ASMData(modFile, classNode.name, itf, null));
                            }

                            if (classNode.visibleAnnotations != null) for (AnnotationNode annotationNode: classNode.visibleAnnotations) {
                                if (annotationNode.values == null || annotationNode.values.isEmpty()) {
                                    this.datas.put(annotationNode.desc, new ASMData(modFile, classNode.name, annotationNode.desc, null));
                                } else {
                                    HashMap<String, Object> maps = new HashMap<>();
                                    annotationNode.accept(new ModAnnotationVisitor(maps));
                                    this.datas.put(annotationNode.desc, new ASMData(modFile, classNode.name, annotationNode.desc, maps));
                                }
                            }
                        }
                    }
                }
            } catch (IOException e)
            {
                FermiumPlugin.LOGGER.error(e);
            }
        }
        finally
        {
            IOUtils.closeQuietly(fs);
        }
    }

    public static class ModAnnotationVisitor extends AnnotationVisitor {
        public Map<String, Object> map;

        public ModAnnotationVisitor(Map<String, Object> map) {
            super(ASM);
            this.map = map;
        }

        @Override
        public void visit(String s, Object o) {
            map.put(s, o);
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            map.put(name, new ModAnnotation.EnumHolder(desc, value));
        }
    }

    public void build() {
        List<Artifact> maven_canidates = LibraryManager.flattenLists(Launch.minecraftHome);
        List<File> file_canidates = LibraryManager.gatherLegacyCanidates(Launch.minecraftHome);
        // Find from core-locations
        for (Artifact artifact : maven_canidates)
        {
            artifact = Repository.resolveAll(artifact);
            if (artifact != null)
            {
                File target = artifact.getFile();
                if (!file_canidates.contains(target))
                    file_canidates.add(target);
            }
        }
        File mods = new File(Launch.minecraftHome, "mods");
        if (mods.exists() && mods.isDirectory()) {
            File[] list = mods.listFiles();
            if (list != null) {
                Collections.addAll(file_canidates, list);
            }
        }

        Set<String> names = new HashSet<>();
        for (File file : file_canidates) {
            if (!names.contains(file.getAbsolutePath())) {
                this.addFor(file);
                names.add(file.getAbsolutePath());
            }
        }
    }
}
