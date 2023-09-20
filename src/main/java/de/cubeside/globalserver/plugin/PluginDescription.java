package de.cubeside.globalserver.plugin;

import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import com.vdurmont.semver4j.SemverException;
import de.cubeside.globalserver.plugin.PluginDependency.LoadOrder;
import de.cubeside.globalserver.plugin.PluginDependency.Type;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public class PluginDescription {
    private final File jarFile;

    private final String name;
    private final String mainClass;
    private final Semver version;

    private final List<PluginDependency> dependencies;

    public PluginDescription(File jarFile) throws PluginDescriptionException {
        this.jarFile = jarFile;
        Object root = null;
        try (JarFile jarJarFile = new JarFile(jarFile)) {
            JarEntry pluginYmlEntry = jarJarFile.getJarEntry("plugin.yml");
            if (pluginYmlEntry == null) {
                throw new PluginDescriptionException("The plugin jar " + jarFile.getName() + " does not contain a plugin.yml");
            }
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
            loaderOptions.setNestingDepthLimit(Integer.MAX_VALUE);
            root = new Yaml(new SafeConstructor(loaderOptions)).load(new InputStreamReader(new BufferedInputStream(jarJarFile.getInputStream(pluginYmlEntry)), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PluginDescriptionException("Could not load plugin jar " + jarFile.getName() + ": " + e.getMessage(), e);
        } catch (YAMLException e) {
            throw new PluginDescriptionException("Could not load the plugin.yml in the plugin jar " + jarFile.getName() + ": " + e.getMessage(), e);
        }
        if (!(root instanceof Map<?, ?> map)) {
            throw new PluginDescriptionException("The plugin.yml in the plugin jar " + jarFile.getName() + " has no root map");
        }
        if (!(map.get("name") instanceof String pluginName)) {
            throw new PluginDescriptionException("The plugin.yml in the plugin jar " + jarFile.getName() + " is missing the required field 'name'");
        }
        if (!(map.get("main") instanceof String mainClass)) {
            throw new PluginDescriptionException("The plugin.yml in the plugin jar " + jarFile.getName() + " is missing the required field 'main'");
        }
        if (!isValidClassName(mainClass)) {
            throw new PluginDescriptionException("The plugin.yml in the plugin jar " + jarFile.getName() + " contains an invalid main class name: " + mainClass);
        }
        Object versionObject = map.get("version");
        if (!(versionObject instanceof String) && !(versionObject instanceof Number)) {
            throw new PluginDescriptionException("The plugin.yml in the plugin jar " + jarFile.getName() + " is missing the required field 'version'");
        }
        this.name = pluginName.trim();
        if (this.name.isEmpty()) {
            throw new PluginDescriptionException("The plugin.yml in the plugin jar " + jarFile.getName() + " contains an invalid plugin name");
        }
        this.mainClass = mainClass.trim();
        try {
            this.version = new Semver(versionObject.toString().trim(), SemverType.NPM);
        } catch (SemverException e) {
            throw new PluginDescriptionException("The plugin.yml in the plugin jar " + jarFile.getName() + " contains an invalid version: " + e.getMessage(), e);
        }
        ArrayList<PluginDependency> dependencies = new ArrayList<>();
        // TODO properly load dependencies
        Object depends = map.get("depend");
        if (depends instanceof String) {
            depends = List.of(depends);
        }
        if (depends instanceof List<?> l) {
            for (Object e : l) {
                if (e instanceof String s) {
                    dependencies.add(new PluginDependency(s, new Requirement[] { Requirement.buildNPM("*") }, LoadOrder.BEFORE, Type.REQUIRED));
                }
            }
        }
        Object softdepends = map.get("softdepend");
        if (softdepends instanceof String) {
            softdepends = List.of(depends);
        }
        if (softdepends instanceof List<?> l) {
            for (Object e : l) {
                if (e instanceof String s) {
                    dependencies.add(new PluginDependency(s, new Requirement[] { Requirement.buildNPM("*") }, LoadOrder.BEFORE, Type.OPTIONAL));
                }
            }
        }
        Object loadbefore = map.get("load-before");
        if (loadbefore instanceof String) {
            loadbefore = List.of(depends);
        }
        if (loadbefore instanceof List<?> l) {
            for (Object e : l) {
                if (e instanceof String s) {
                    dependencies.add(new PluginDependency(s, new Requirement[] { Requirement.buildNPM("*") }, LoadOrder.AFTER, Type.OPTIONAL));
                }
            }
        }
        this.dependencies = Collections.unmodifiableList(dependencies);
    }

    public String getName() {
        return name;
    }

    public Semver getVersion() {
        return version;
    }

    public String getMainClass() {
        return mainClass;
    }

    public File getJarFile() {
        return jarFile;
    }

    public List<PluginDependency> getDependencies() {
        return dependencies;
    }

    private static final String IDENTIFIER = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    private static final Pattern CLASS_NAME = Pattern.compile(IDENTIFIER + "(\\." + IDENTIFIER + ")*");

    private static boolean isValidClassName(String className) {
        return CLASS_NAME.matcher(className).matches();
    }
}
