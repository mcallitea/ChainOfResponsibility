/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mcallitea.cor.builder.reader;

import com.github.mcallitea.cor.api.CorCommand;
import com.github.mcallitea.cor.api.annotation.CorElement;
import com.github.mcallitea.cor.api.annotation.CorPackage;
import com.github.mcallitea.cor.api.annotation.CorType;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CorAnnotationReader reads the Cor-Annotations of {@link com.github.mcallitea.cor.api.annotation} package in a given
 * package or package-name. Die Configuration will be returned, containing the classes that implement the
 * {@link CorCommand} interface.
 *
 * @since 1.0.0
 */
public class CorAnnotationReader {

    private CorAnnotationReader() {
    }

    /**
     * Reads the {@link CorPackage} annotation of <i>package-info.java</i> of the given <i>packageName</i> and returns
     * the list of {@link CorType} annotations.
     *
     * @param packageName Name of the package in which the configuration is searched for.
     * @return List of {@link CorType} annotations.
     * @throws IOException If reading of the package fails.
     */
    public static Optional<List<CorType>> readPackage(String packageName) throws IOException {
        return CorAnnotationReader.readPackage(PackageReader.getPackage(packageName));
    }

    /**
     * Reads the {@link CorPackage} annotation of <i>package-info.java</i> of the given <i>corPackage</i> and returns
     * the list of {@link CorType} annotations.
     *
     * @param corPackage Package in which the configuration is searched for.
     * @return List of {@link CorType} annotations.
     */
    public static Optional<List<CorType>> readPackage(Package corPackage) {
        CorPackage corPackageAnnotation = corPackage.getAnnotation(CorPackage.class);

        if (corPackageAnnotation != null) {
            return Optional.of(Arrays.asList(corPackageAnnotation.value()));
        }

        return Optional.empty();
    }

    /**
     * Reads the classes annotated with {@link CorElement} of the given <i>packageName</i> and returns a map containing
     * the class as key and die annotation as value. Classes will be filtered by implementing die CorCommand interface.
     *
     * @param packageName Package name in which the configuration is searched for.
     * @return Class-CorElement map containing the configuration.
     * @throws IOException If reading of the package fails.
     */
    public static Map<Class<? extends CorCommand<?, ?>>, CorElement> readClassesByClassAnnotation(String packageName) throws IOException, ClassNotFoundException {
        return CorAnnotationReader.readClassesByClassAnnotation(PackageReader.getPackage(packageName));
    }

    /**
     * Reads the classes annotated with {@link CorElement} of the given <i>package</i> and returns a map containing the
     * class as key and die annotation as value. Classes will be filtered by implementing die CorCommand interface.
     *
     * @param corPackage Package in which the configuration is searched for.
     * @return Class-CorElement map containing the configuration.
     * @throws IOException If reading of the package fails.
     */
    public static Map<Class<? extends CorCommand<?, ?>>, CorElement> readClassesByClassAnnotation(Package corPackage) throws IOException, ClassNotFoundException {
        Map<Class<? extends CorCommand<?, ?>>, CorElement> elementMap = new HashMap<>();

        for (Class<? extends CorCommand> corCommandClass : ClassesReader.readClasses(corPackage.getName(), CorCommand.class)) {
            CorElement corElementAnnotation = corCommandClass.getAnnotation(CorElement.class);

            if (corElementAnnotation != null) {
                elementMap.put((Class<? extends CorCommand<?, ?>>) corCommandClass, corElementAnnotation);
            }
        }

        return elementMap;
    }

}
