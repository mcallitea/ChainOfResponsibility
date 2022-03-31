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

import com.github.mcallitea.cor.builder.exception.PackageNotFoundException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class PackageReader {

    private PackageReader() {
    }

    /**
     * Searches for the given <i>packageName</i> in the set of packages defined in the callers classloader. If the
     * package was not found, a runtime exception {@link PackageNotFoundException} will be fired.
     *
     * @param packageName Name of the searched package
     * @see Package#getPackages()
     */
    public static Package getPackage(final String packageName) throws IOException {
        loadPackage(packageName);
        Optional<Package> optionalPackage = Arrays.stream(Package.getPackages())
                .filter(aPackage -> aPackage.getName().equals(packageName))
                .findFirst();

        if (optionalPackage.isEmpty()) {
            throw new PackageNotFoundException("Package not found: " + packageName);
        }

        return optionalPackage.get();
    }

    /**
     * Read the classes of a package, so the package will be present in classpath.
     *
     * @param packageName Name of the package to load
     * @throws IOException If reading from stream fails
     */
    private static void loadPackage(String packageName) throws IOException {
        try {
            ClassesReader.readClasses(packageName, Object.class);
        } catch (ClassNotFoundException e) {
            throw new PackageNotFoundException("Package is empty: " + packageName);
        }
    }

}
