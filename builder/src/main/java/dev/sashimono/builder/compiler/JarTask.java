package dev.sashimono.builder.compiler;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import dev.sashimono.builder.config.Dependency;
import dev.sashimono.builder.config.GAV;
import dev.sashimono.builder.dependencies.ResolvedDependency;
import dev.sashimono.builder.util.TaskMap;

public class JarTask implements Function<TaskMap, JarResult> {

    private final Path outputDir;
    private final GAV gav;

    public JarTask(Path outputDir, GAV gav) {
        this.outputDir = outputDir;
        this.gav = gav;
    }

    @Override
    public JarResult apply(TaskMap taskMap) {
        var deps = taskMap.results(CompileResult.class).get(0);
        Path parentDir = outputDir;
        var groupParts = gav.group().split("\\.");
        for (var i : groupParts) {
            parentDir = parentDir.resolve(i);
        }
        parentDir = parentDir.resolve(gav.artifact());
        parentDir = parentDir.resolve(gav.version());
        try {
            Files.createDirectories(parentDir);
            Path target = parentDir.resolve(gav.artifact() + "-" + gav.version() + ".jar");
            try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(target))) {
                Files.walkFileTree(deps.classesDirectory(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        System.out.println(file.toString());
                        ZipEntry entry = new ZipEntry(file.toString());
                        entry.setCreationTime(FileTime.fromMillis(0));
                        entry.setSize(Files.size(file));
                        out.putNextEntry(entry);
                        out.write(Files.readAllBytes(file));
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            return new JarResult(new ResolvedDependency(new Dependency(gav, "jar"), target, Optional.empty()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
