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

import com.github.mcallitea.cor.builder.exception.ResourceStreamException;
import org.jboss.vfs.VirtualJarInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;

/**
 * ClassesReader is a simple implementation to read classes from a package filtered by an interface that classes should
 * implement.
 * <p>
 * This implementation can be used in a JavaEE respective Jakarta EE context, as well as in testing contexts. As
 * important hint: The usage of ClassesReader leads to the fact, that unit-test-cases and also the production release
 * work properly, but different code will be executed. So if it is important, that the exact same code is executed, a
 * component-test of a Jakarta EE application is recommended.
 *
 * @since 1.0.0
 */
public class ClassesReader {

    private ClassesReader() {
    }

    /**
     * Reads classes of a defined package that implements a specific interface. Class-search will not search recursively.
     * The <i>interfaceToImplement</i> will be checked by isAssignableFrom. Only instantiable classes that
     * implements the desired interface will be part of the result-set, so interfaces and abstract classes will be
     * filtered.
     *
     * @param corPackage           Package that will be browsed
     * @param interfaceToImplement Interface that should be implemented by the searched classes
     * @return List of classes that implements the desired interface
     * @throws IOException If reading from stream fails
     */
    public static <T> List<Class<T>> readClasses(final Package corPackage, final Class<T> interfaceToImplement) throws IOException, ClassNotFoundException {
        return ClassesReader.readClasses(corPackage.getName(), interfaceToImplement);
    }

    /**
     * Reads classes of a defined package that implements a specific interface. Class-search will not search recursively.
     * The <i>interfaceToImplement</i> will be checked by isAssignableFrom. Only instantiable classes that
     * implements the desired interface will be part of the result-set, so interfaces and abstract classes will be
     * filtered.
     *
     * @param packageName          Name of the package that will be browsed
     * @param interfaceToImplement Interface that should be implemented by the searched classes
     * @return List of classes that implements the desired interface
     * @throws IOException If reading from stream fails
     */
    public static <T> List<Class<T>> readClasses(final String packageName, final Class<T> interfaceToImplement) throws IOException, ClassNotFoundException {
        List<Class<T>> classesList = new LinkedList<>();

        for (String classFileName : getResourceFileNames(packageName)) {
            String separator = "";
            if (!packageName.endsWith(".")) {
                separator = ".";
            }
            // classFileName.length() - 6 : length of ".class" = 6
            Class<?> clazz = Class.forName(packageName + separator + classFileName.substring(0, classFileName.length() - 6));

            if (interfaceToImplement.isAssignableFrom(clazz) && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                // Cast to Class<T> can be performed, because the type-check is done by interfaceToImplement.isAssignableFrom
                classesList.add((Class<T>) clazz);
            }
        }

        return classesList;
    }

    /**
     * Returns a list of .class-filenames that are part of the desired package.
     *
     * @param packageName Name of the package that will be browsed
     * @return List of .class-filenames
     * @throws IOException If reading from stream fails
     */
    public static List<String> getResourceFileNames(final String packageName) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String packagePath = packageName.replace(".", "/");

        try (InputStream inputStream = classLoader.getResourceAsStream(packagePath)) {
            if (inputStream == null) {
                throw new ResourceStreamException("Resource-Stream is null. Maybe given package-name is not present.");
            }

            /* In Java EE context there will only be the vfs available, so VirtualJarInputStream should be used.
             * Otherwise, a BufferedReader can be used, so tests will also work properly.
             */
            if (VirtualJarInputStream.class.equals(inputStream.getClass())) {
                return readFromJarInputStream((VirtualJarInputStream) inputStream);
            } else {
                return readFromInputStream(inputStream);
            }
        }
    }

    private static List<String> readFromJarInputStream(VirtualJarInputStream virtualJarInputStream) throws IOException {
        List<String> fileNames = new LinkedList<>();
        JarEntry jarEntry;

        while ((jarEntry = virtualJarInputStream.getNextJarEntry()) != null) {
            fileNames.add(jarEntry.getName());
        }

        return fileNames;
    }

    private static List<String> readFromInputStream(InputStream inputStream) throws IOException {
        List<String> fileNames = new LinkedList<>();

        try (BufferedReader bufferedInputStream = new BufferedReader(new InputStreamReader(inputStream))) {
            String resource;

            while ((resource = bufferedInputStream.readLine()) != null) {
                fileNames.add(resource);
            }
        }

        return fileNames;
    }

}
