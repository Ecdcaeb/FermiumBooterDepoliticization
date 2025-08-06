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
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DiscoveryHandler {

    public static class ASMData {
        public final File source;
        public final String className;
        public final ClassNode classNode;
        public final String annName;
        public final Map<String, Object> values;

        public ASMData(File source, String className, ClassNode classNode, String annName, Map<String, Object> values) {
            this.source = source;
            this.className = className;
            this.classNode = classNode;
            this.annName = annName;
            this.values = values;
        }
    }
    public final SetMultimap<String, ASMData> datas = HashMultimap.create();

    public static final Pattern classFile = Pattern.compile("[^\\s\\$]+(\\$[^\\s]+)?\\.class$");

    public static final int ASM = SystemUtils.IS_JAVA_1_8 ? (5 << 16 | 0 << 8) : (9 << 16 | 0 << 8); // ASM5 : ASM9

    public void addFor(File modFile) {
        if (!modFile.exists()) {
            FermiumPlugin.LOGGER.warn("Skipping non-existent file: {}", modFile);
            return;
        }

        try {
            if (modFile.isDirectory()) {
                processDirectory(modFile, modFile.toPath());
            } else if (modFile.getName().endsWith(".jar")) {
                processJarFile(modFile);
            }
        } catch (IOException e) {
            FermiumPlugin.LOGGER.error("Failed to process mod file: {}", modFile, e);
        }
    }

    private void processDirectory(File modFile, Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(this::isClassFile)
                  .forEach((file)->this.processClassFile(modFile, file));
        }
    }

    private void processJarFile(File jarFile) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(jarFile.toPath(), (ClassLoader) null)) {
            final Path root = fs.getPath("/");
            Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (isClassFile(file)) {
                        try (InputStream is = Files.newInputStream(file)) {
                            processClassStream(jarFile, is);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            FermiumPlugin.LOGGER.error("Error processing JAR: {}", jarFile, e);
            throw e;
        }
    }

    private boolean isClassFile(Path path) {
        return path.toString().endsWith(".class");
    }


    private void processClassStream(File modFile, InputStream is) {
        try {
            ClassReader classReader = new ClassReader(is);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (classNode.interfaces != null) for (String itf : classNode.interfaces) {
                this.datas.put(itf, new ASMData(modFile, classNode.name, classNode, itf, null));
            }

              if (classNode.visibleAnnotations != null) for (AnnotationNode annotationNode: classNode.visibleAnnotations) {
                if (annotationNode.values == null || annotationNode.values.isEmpty()) {
                       this.datas.put(annotationNode.desc, new ASMData(modFile, classNode.name, classNode, annotationNode.desc, null));
                } else {
                    HashMap<String, Object> maps = new HashMap<>();
                    annotationNode.accept(new ModAnnotationVisitor(maps));
                    this.datas.put(annotationNode.desc, new ASMData(modFile, classNode.name, classNode, annotationNode.desc, maps));
                }
            }
        } catch (Exception e) {
            FermiumPlugin.LOGGER.error("Invalid class file", e);
        }
    }

    private void processClassFile(File modFile, Path classPath) {
        try (InputStream is = Files.newInputStream(classPath)) {
            processClassStream(modFile, is);
        } catch (IOException e) {
            FermiumPlugin.LOGGER.error("Error reading class: {}", classPath, e);
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
        Set<File> allFiles = new LinkedHashSet<>();
        
        // // Add library files
        // LibraryManager.flattenLists(Launch.minecraftHome).stream()
        //     .map(Repository::resolveAll)
        //     .filter(Objects::nonNull)
        //     .map(Artifact::getFile)
        //     .forEach(allFiles::add);
        
        // // Add legacy candidates
        // allFiles.addAll(LibraryManager.gatherLegacyCanidates(Launch.minecraftHome));
        
        // Add mods directory
        File modsDir = new File(Launch.minecraftHome, "mods");
        if (modsDir.isDirectory()) {
            File[] files = modsDir.listFiles();
            if (files != null) {
                Collections.addAll(allFiles, files);
            }
        }

        // Process unique files
        allFiles.forEach(this::addFor);
    }
}
