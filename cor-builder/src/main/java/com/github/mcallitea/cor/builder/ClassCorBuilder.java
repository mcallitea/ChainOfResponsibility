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
import com.github.mcallitea.cor.builder.exception.BuildException;
import com.github.mcallitea.cor.builder.exception.ElementInstantiationException;
import com.github.mcallitea.cor.builder.reader.ClassesReader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * The implementation make use of the {@link CorCommand} interface to link the elements of the chain of responsibility.
 * This is done by {@link CorCommand#nextCommand(CorCommand)} method, so it is expected that each element knows the
 * following elements. It is not necessary to know the previous element, so this builder only make use of the
 * <i>nextCommand</i> method.
 * <p>
 * This builder supports the initialization of the chain by providing only the package-name and the interface all
 * classes should implement. Therefore, the {@link ClassesReader} is used.
 * <p>
 * To give convenient ways of usage, a list of classes can also be used to let the builder do the rest - with
 * parameterless constructors as well, as with a custom initializer or with specific initializers for each type.
 *
 * @since 1.0.0
 */
public class ClassCorBuilder {

    private ClassCorBuilder() {
    }

    /**
     * Builds the chain of responsibility based on the classes that are present in the desired package and implements
     * the desired interface <i>interfaceToImplement</i>.
     * <p>
     * The first element of the chain will be returned.
     *
     * @param corPackage           Package that will be browsed
     * @param interfaceToImplement Interface that should be implemented by the searched classes
     * @param <T>                  Generic type - should be the interface that extends the {@link com.github.mcallitea.cor.api.Command}
     *                             and is also the generic type of the {@link CorCommand} interface
     * @return The first element of the chain
     */
    public static <T extends CorCommand<?, ?>> T buildChainOfResponsibility(Package corPackage, Class<T> interfaceToImplement) throws ElementInstantiationException, BuildException {
        return buildChainOfResponsibility(corPackage.getName(), interfaceToImplement);
    }

    /**
     * Builds the chain of responsibility based on the classes that are present in the desired package and implements
     * the desired interface <i>interfaceToImplement</i>.
     * <p>
     * The first element of the chain will be returned.
     *
     * @param packageName          Name of the package that will be browsed
     * @param interfaceToImplement Interface that should be implemented by the searched classes
     * @param <T>                  Generic type - should be the interface that extends the {@link com.github.mcallitea.cor.api.Command}
     *                             and is also the generic type of the {@link CorCommand} interface
     * @return The first element of the chain
     * @throws BuildException                If classes from package could not be read or the package does not exist.
     * @throws ElementInstantiationException When searching or calling the constructor of a class that instance should become part of the chain.
     */
    public static <T extends CorCommand<?, ?>> T buildChainOfResponsibility(String packageName, Class<T> interfaceToImplement) throws ElementInstantiationException, BuildException {
        List<Class<T>> classesList = null;
        try {
            classesList = ClassesReader.readClasses(packageName, interfaceToImplement);
        } catch (IOException | ClassNotFoundException e) {
            throw new BuildException("Reading of the package failed.", e);
        }
        return buildChainOfResponsibility(classesList);
    }

    /**
     * Builds the chain of responsibility based on the given classes. Therefore, the classes should implement or
     * implement an extension of the {@link com.github.mcallitea.cor.api.CorCommand} interface - otherwise
     * the class will be ignored.
     * The classes should also contain a parameter-less public constructor, so instances can be created.
     * <p>
     * The first element of the chain will be returned.
     *
     * @param classesList List of classes that implements the {@link CorCommand} interface
     * @param <T>         Generic type - should be the interface that extends the {@link CorCommand}
     * @return The first element of the chain
     */
    public static <T extends CorCommand<?, ?>> T buildChainOfResponsibility(List<Class<T>> classesList) throws ElementInstantiationException {
        return buildChainOfResponsibility(classesList, clazz -> clazz.getDeclaredConstructor((Class<?>[]) null).newInstance());
    }

    /**
     * @param classesList    List of classes that implements the {@link CorCommand} interface
     * @param corCommandInit Lambda to initialize an instance of <i>T</i>.
     * @param <T>            Generic type - should be the interface that extends the {@link CorCommand}
     * @return The first element of the chain
     * @throws ElementInstantiationException When searching or calling the constructor of a class that instance should become part of the chain.
     */
    public static <T extends CorCommand<?, ?>> T buildChainOfResponsibility(List<Class<T>> classesList, CorCommandInit corCommandInit) throws ElementInstantiationException {
        T command = null;
        CorCommand<?, ?> currentCommand = null;

        for (Class<T> clazz : classesList) {
            CorCommand corElement = null;
            corElement = initCorElement(clazz, corCommandInit);
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
     * Builds the chain of responsibility by a given map, that contains a specific initializer for each class.
     * Returns the first element of the chain.
     *
     * @param classesAndInitializers Map that contains a specific initializer for each class.
     * @param <T>                    Generic type - should be the interface that extends the {@link CorCommand}
     * @return The first element of the chain
     */
    public static <T extends CorCommand<?, ?>> T buildChainOfResponsibility(Map<Class<? extends CorCommand<?, ?>>, CorCommandInit> classesAndInitializers) throws ElementInstantiationException {
        T command = null;
        CorCommand<?, ?> currentCommand = null;

        for (Map.Entry<Class<? extends CorCommand<?, ?>>, CorCommandInit> entry : classesAndInitializers.entrySet()) {
            CorCommand corElement = initCorElement(entry.getKey(), entry.getValue());
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
     * Initializes an instance of type <i>clazz</i> by a Lambda of {@link CorCommandInit#init(Class)}.
     *
     * @param clazz          Any class that implements the CorCommand Interface.
     * @param corCommandInit Lambda to initialize an instance of <i>clazz</i>.
     * @return An instance of <i>clazz</i>.
     */
    private static CorCommand<?, ?> initCorElement(Class<? extends CorCommand<?, ?>> clazz, CorCommandInit corCommandInit) throws ElementInstantiationException {
        try {
            return corCommandInit.init(clazz);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new ElementInstantiationException("Instantiation and initialization of a chain element failed.", e);
        }
    }

}
