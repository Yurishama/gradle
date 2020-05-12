/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.initialization;

import org.gradle.api.Project;
import org.gradle.api.internal.properties.GradleProperties;
import org.gradle.initialization.layout.BuildLayout;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

import static java.util.Collections.emptyMap;


public class DefaultGradlePropertiesController implements GradlePropertiesController {

    private final GradleProperties sharedGradleProperties = new SharedGradleProperties();
    private State state;

    public DefaultGradlePropertiesController(
        BuildLayout buildLayout,
        IGradlePropertiesLoader propertiesLoader
    ) {
        this.state = new Loaded(buildLayout, propertiesLoader);
    }

    @Override
    public GradleProperties getGradleProperties() {
        return sharedGradleProperties;
    }

    @Override
    public void loadGradlePropertiesFrom(File settingsDir) {
        state = state.loadGradleProperties(settingsDir);
    }

    private class SharedGradleProperties implements GradleProperties {

        @Nullable
        @Override
        public String find(String propertyName) {
            return gradleProperties().find(propertyName);
        }

        @Override
        public Map<String, String> mergeProperties(Map<String, String> properties) {
            return gradleProperties().mergeProperties(properties);
        }

        private GradleProperties gradleProperties() {
            return state.gradleProperties();
        }
    }

    private interface State {

        GradleProperties gradleProperties();

        State loadGradleProperties(File settingsDir);
    }

    private static class Loaded implements State {

        private final BuildLayout buildLayout;
        private final IGradlePropertiesLoader propertiesLoader;

        private Loaded(BuildLayout buildLayout, IGradlePropertiesLoader propertiesLoader) {
            this.buildLayout = buildLayout;
            this.propertiesLoader = propertiesLoader;
        }

        @Override
        public GradleProperties gradleProperties() {
            throw new IllegalStateException("GradleProperties has not been loaded yet.");
        }

        @Override
        public State loadGradleProperties(File settingsDir) {
            GradleProperties loadedProperties = propertiesLoader.loadGradleProperties(settingsDir);
            addSystemPropertiesFromGradleProperties(loadedProperties.mergeProperties(emptyMap()));
            return new Applied(loadedProperties);
        }

        private void addSystemPropertiesFromGradleProperties(Map<String, String> properties) {
            String prefix = Project.SYSTEM_PROP_PREFIX + '.';
            for (String key : properties.keySet()) {
                if (key.startsWith(prefix)) {
                    System.setProperty(key.substring(prefix.length()), properties.get(key));
                }
            }
        }
    }

    private static class Applied implements State {

        private final GradleProperties gradleProperties;

        private Applied(GradleProperties gradleProperties) {
            this.gradleProperties = gradleProperties;
        }

        @Override
        public GradleProperties gradleProperties() {
            return gradleProperties;
        }

        @Override
        public State loadGradleProperties(File settingsDir) {
            return this;
        }
    }
}
