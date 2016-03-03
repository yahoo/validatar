package com.yahoo.validatar.common;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.singletonList;

/**
 * A class that can be extended to load or plugin additional classes to a type. For example, extending this
 * class in an package that loads engines could let it allow loading additional engines at runtime from arguments.
 * It only works with classes that can be instantiated with the default constructor.
 *
 * @param <T> The super type of the pluggable classes.
 */
@Slf4j
public class Pluggable<T> {
    @Getter
    private OptionParser pluginOptionsParser;
    private List<Class<? extends T>> defaults;
    private String optionsKey;

    /**
     * The constructor.
     *
     * @param defaults The List of default classes to use as plugins.
     * @param key The key to use to load the plugin class from command line arguments.
     * @param description A helpful description to provide for what these plugins are.
     */
    public Pluggable(List<Class<? extends T>> defaults, String key, String description) {
        Objects.requireNonNull(defaults);
        pluginOptionsParser = new OptionParser() {
            {
                acceptsAll(singletonList(key), description)
                        .withRequiredArg()
                        .describedAs("Additional custom fully qualified classes to plug in");
                allowsUnrecognizedOptions();
            }
        };
        this.defaults = defaults;
        this.optionsKey = key;
    }

    /**
     * Returns a set view of the instantiated plugins that could be created.
     * @param arguments The commandline arguments containing the optional plugin arguments and class names.
     * @return A Set of all the instantiated plugin classes.
     */
    public Set<T> getPlugins(String[] arguments) {
        OptionSet options = pluginOptionsParser.parse(arguments);
        Set<Class<? extends T>> pluginClasses = new HashSet<>(defaults);
        for (String pluggable : (List<String>) options.valuesOf(optionsKey)) {
            try {
                Class<? extends T> plugin = (Class<? extends T>) Class.forName(pluggable);
                pluginClasses.add(plugin);
            } catch (ClassNotFoundException e) {
                log.error("Requested plugin class not found: {}", pluggable, e);
            }
        }

        Set<T> plugins = new HashSet<>();
        for (Class<? extends T> pluginClass : pluginClasses) {
            try {
                plugins.add(pluginClass.newInstance());
            } catch (InstantiationException ie) {
                log.error("Error instantiating {} plugin.\n{}", pluginClass, ie);
            } catch (IllegalAccessException iae) {
                log.error("Illegal access while loading {} plugin.\n{}", pluginClass, iae);
            }
        }
        return plugins;
    }
}
