package com.yahoo.validatar.common;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;

@Slf4j
public class Pluggable<T> {
    @Getter
    private OptionParser parser;
    private List<Class<? extends T>> defaults;
    private String optionsKey;

    public Pluggable(List<Class<? extends T>> defaults, String key, String description) {
        Objects.requireNonNull(defaults);
        parser = new OptionParser() {
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

    public List<T> getPlugins(String[] arguments) {
        OptionSet options = parser.parse(arguments);
        List<Class<? extends T>> pluginClasses = new ArrayList<>(defaults);
        for (String pluggable : (List<String>) options.valuesOf(optionsKey)) {
            try {
                Class<? extends T> plugin = (Class<? extends T>) Class.forName(pluggable);
                pluginClasses.add(plugin);
            } catch (ClassNotFoundException e) {
                log.error("Requested plugin class not found: {}", pluggable, e);
            }
        }

        List<T> plugins = new ArrayList<>();
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
