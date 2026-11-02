package org.example.generator;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

final class ImplementationScanner {
    private final Set<String> extraPackages;
    private final Map<Class<?>, List<Class<?>>> cache = new ConcurrentHashMap<>();

    ImplementationScanner(Set<String> extraPackages) {
        this.extraPackages = extraPackages;
    }

    List<Class<?>> findImplementations(Class<?> base) {
        return cache.computeIfAbsent(base, this::scan);
    }

    Set<String> packagesFor(Class<?> base) {
        Set<String> packages = new LinkedHashSet<>();
        packages.add(base.getPackageName());
        packages.addAll(extraPackages);
        return packages;
    }

    private List<Class<?>> scan(Class<?> base) {
        ClassLoader loader = base.getClassLoader() != null
                ? base.getClassLoader()
                : Thread.currentThread().getContextClassLoader();
        Set<Class<?>> found = new LinkedHashSet<>();
        for (String pkg : packagesFor(base)) {
            for (String className : classNames(loader, pkg)) {
                Class<?> candidate = load(className, loader);
                if (candidate != null && isImplementation(base, candidate)) {
                    found.add(candidate);
                }
            }
        }
        List<Class<?>> result = new ArrayList<>(found);
        // Детерминированный порядок, чтобы генератор с фиксированным seed был воспроизводим
        result.sort(Comparator.comparing(Class::getName));
        return Collections.unmodifiableList(result);
    }

    private static boolean isImplementation(Class<?> base, Class<?> candidate) {
        int modifiers = candidate.getModifiers();
        return candidate != base
                && base.isAssignableFrom(candidate)
                && !candidate.isInterface()
                && !Modifier.isAbstract(modifiers)
                && !candidate.isAnonymousClass()
                && !candidate.isLocalClass()
                && (!candidate.isMemberClass() || Modifier.isStatic(modifiers))
                && candidate.isAnnotationPresent(Generatable.class);
    }

    private static Class<?> load(String className, ClassLoader loader) {
        try {
            return Class.forName(className, false, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static Set<String> classNames(ClassLoader loader, String pkg) {
        String path = pkg.replace('.', '/');
        Set<String> names = new LinkedHashSet<>();
        try {
            Enumeration<URL> resources = loader.getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                switch (url.getProtocol()) {
                    case "file" -> scanDirectory(Path.of(url.toURI()), pkg, names);
                    case "jar" -> scanJar(url, path, names);
                    default -> { /* другие источники классов не поддерживаются */ }
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new GenerationException("Failed to scan package " + pkg, e);
        }
        return names;
    }

    private static void scanDirectory(Path root, String pkg, Set<String> names) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(f -> f.toString().endsWith(".class")).forEach(f -> {
                String relative = root.relativize(f).toString().replace('\\', '.').replace('/', '.');
                String simple = relative.substring(0, relative.length() - ".class".length());
                addClassName(pkg.isEmpty() ? simple : pkg + "." + simple, names);
            });
        }
    }

    private static void scanJar(URL url, String path, Set<String> names) throws IOException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        connection.setUseCaches(false);
        try (JarFile jar = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().getName();
                if (entry.startsWith(path + "/") && entry.endsWith(".class")) {
                    addClassName(entry.substring(0, entry.length() - ".class".length()).replace('/', '.'), names);
                }
            }
        }
    }

    private static void addClassName(String name, Set<String> names) {
        if (!name.endsWith("package-info") && !name.endsWith("module-info")) {
            names.add(name);
        }
    }
}
