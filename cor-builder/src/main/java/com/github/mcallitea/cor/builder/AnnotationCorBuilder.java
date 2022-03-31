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

package com.github.mcallitea.cor.builder;

import com.github.mcallitea.cor.api.CorCommand;
import com.github.mcallitea.cor.api.CorCommandInit;
import com.github.mcallitea.cor.api.annotation.CorElement;
import com.github.mcallitea.cor.api.annotation.CorType;
import com.github.mcallitea.cor.builder.exception.BuildException;
import com.github.mcallitea.cor.builder.priority.PriorityCorElement;
import com.github.mcallitea.cor.builder.priority.PriorityCorType;
import com.github.mcallitea.cor.builder.reader.CorAnnotationReader;
import com.github.mcallitea.cor.builder.reader.PackageReader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * The implementation make use of the {@link CorCommand} interface to link the elements of the chain of responsibility.
 * This is done by {@link CorCommand#nextCommand(CorCommand)} method, so it is expected that each element knows the
 * following elements. It is not necessary to know the previous element, so this builder only make use of the
 * <i>nextCommand</i> method.
 * <p>
 * This builder supports the initialization of the chain by providing only the package-name or the package itself. The
 * builder searches the package for a package-info.java first, and only if no {@link CorType} elements are defined, the
 * package will be searched for classes annotated with {@link CorElement}.
 * <p>
 *
 * @since 1.0.0
 */
public class AnnotationCorBuilder {

    private AnnotationCorBuilder() {
    }

    /**
     * Builds the chain of responsibility based on the {@link com.github.mcallitea.cor.api.annotation.CorPackage},
     * {@link CorType} or {@link CorElement} annotations.
     *
     * @param packageName Name of the package that will be browsed.
     * @param <T>         Generic type that should extend the {@link CorCommand} interface.
     * @return The first element of the chain
     * @throws BuildException If classes from package could not be read or the package does not exist.
     */
    public static <T extends CorCommand<?, ?>> T buildChainOfResponsibility(String packageName) throws BuildException {
        try {
            return buildChainOfResponsibility(PackageReader.getPackage(packageName));
        } catch (IOException e) {
            throw new BuildException("Build of the chain failed.", e);
        }
    }

    /**
     * Builds the chain of responsibility based on the {@link com.github.mcallitea.cor.api.annotation.CorPackage},
     * {@link CorType} or {@link CorElement} annotations.
     *
     * @param corPackage Package that will be browsed
     * @param <T>        Generic type that should extend the {@link CorCommand} interface.
     * @return The first element of the chain
     * @throws BuildException If classes from package could not be read or the package does not exist.
     */
    public static <T extends CorCommand<?, ?>> T buildChainOfResponsibility(Package corPackage) throws BuildException {
        Optional<List<CorType>> corTypeElements = CorAnnotationReader.readPackage(corPackage);
        try {
            if (corTypeElements.isPresent()) {
                return buildChainOfResponsibilityByAnnotations(corTypeElements.get());
            } else {
                return buildChainOfResponsibilityByAnnotations(CorAnnotationReader.readClassesByClassAnnotation(corPackage));
            }
        } catch (IOException | ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new BuildException("Build of the chain failed.", e);
        }
    }

    /**
     * Builds the chain of responsibility.
     * Using {@link CorType} is type-safe in compile time, because {@link CorType} already defines that a given class
     * has to implement {@link CorCommand}.
     *
     * @param corTypes List of {@link CorType} defined in {@link com.github.mcallitea.cor.api.annotation.CorPackage}.
     * @param <T>      Generic type that should extend the {@link CorCommand} interface.
     * @return The first element of the chain.
     */
    private static <T extends CorCommand<?, ?>> T buildChainOfResponsibilityByAnnotations(List<CorType> corTypes) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        T command = null;
        CorCommand<?, ?> currentCommand = null;
        PriorityQueue<PriorityCorType> priorityCorTypes = createPriorityQueue(corTypes);

        while (!priorityCorTypes.isEmpty()) {
            PriorityCorType priorityCorType = priorityCorTypes.poll();
            CorCommand corElement = initCorElement(priorityCorType.corType());
            if (command == null) {
                command = (T) corElement;
            } else {
                currentCommand.nextCommand(corElement);
            }

            currentCommand = corElement;
        }

        return command;
    }

    /**
     * Builds the chain of responsibility.
     * The classes used for <i>elementMap</i> are gathered by {@link CorAnnotationReader} which uses the
     * {@link com.github.mcallitea.cor.builder.reader.ClassesReader} with {@link CorCommand} to identify classes that
     * implement {@link CorCommand} at first and check for {@link CorElement} at second. So elements of elementMap are
     * prefiltered.
     *
     * @param elementMap Prefiltered map of classes that are configured by {@link CorElement} annotation.
     * @param <T>        Generic type that should extend the {@link CorCommand} interface.
     * @return The first element of the chain.
     */
    private static <T extends CorCommand<?, ?>> T buildChainOfResponsibilityByAnnotations(Map<Class<? extends CorCommand<?, ?>>, CorElement> elementMap) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        T command = null;
        CorCommand<?, ?> currentCommand = null;
        PriorityQueue<PriorityCorElement> priorityCorElements = createPriorityQueue(elementMap);

        while (!priorityCorElements.isEmpty()) {
            PriorityCorElement priorityCorElement = priorityCorElements.poll();
            CorCommand corElement = initCorElement(priorityCorElement.clazz(), priorityCorElement.corElement());
            if (command == null) {
                command = (T) corElement;
            } else {
                currentCommand.nextCommand(corElement);
            }

            currentCommand = corElement;
        }

        return command;
    }

    private static PriorityQueue<PriorityCorType> createPriorityQueue(List<CorType> corTypes) {
        PriorityQueue<PriorityCorType> priorityCorTypes = new PriorityQueue<>(corTypes.size(), Comparator.comparingInt(PriorityCorType::priority));
        for (CorType corType : corTypes) {
            priorityCorTypes.add(new PriorityCorType(corType.order(), corType));
        }

        return priorityCorTypes;
    }

    private static PriorityQueue<PriorityCorElement> createPriorityQueue(Map<Class<? extends CorCommand<?, ?>>, CorElement> elementMap) {
        PriorityQueue<PriorityCorElement> priorityCorTypes = new PriorityQueue<>(elementMap.size(), Comparator.comparingInt(PriorityCorElement::priority));
        for (Map.Entry<Class<? extends CorCommand<?, ?>>, CorElement> elementEntry : elementMap.entrySet()) {
            priorityCorTypes.add(new PriorityCorElement(elementEntry.getValue().order(), elementEntry.getKey(), elementEntry.getValue()));
        }

        return priorityCorTypes;
    }

    private static CorCommand<?, ?> initCorElement(CorType corType) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        if (corType.staticInitializerMethod().isEmpty()) {
            return corType.value().getDeclaredConstructor((Class<?>[]) null).newInstance();
        }

        Method method = corType.value().getMethod(corType.staticInitializerMethod());
        CorCommandInit corCommandInit = (CorCommandInit) method.invoke(null);

        return corCommandInit.init(corType.value());
    }

    /**
     * Initializes an instance of type <i>clazz</i> by the given {@link CorElement}, which points to a static
     * initializer-method in the same class.
     *
     * @param clazz      Any class that implements the CorCommand interface.
     * @param corElement Annotation that contains further information for initialization.
     * @return An instance of <i>clazz</i>.
     */
    private static CorCommand<?, ?> initCorElement(Class<? extends CorCommand<?, ?>> clazz, CorElement corElement) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        if (corElement.staticInitializerMethod().isEmpty()) {
            return clazz.getDeclaredConstructor((Class<?>[]) null).newInstance();
        }

        Method method = clazz.getMethod(corElement.staticInitializerMethod());
        CorCommandInit corCommandInit = (CorCommandInit) method.invoke(null);

        return corCommandInit.init(clazz);
    }

}
