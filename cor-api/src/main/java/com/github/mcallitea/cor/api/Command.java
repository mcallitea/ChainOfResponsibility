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

package com.github.mcallitea.cor.api;

import java.util.Optional;

/**
 * A class of <i>Command</i> is an element of the cain of responsibility.
 * <p>
 * Mostly this Interface might not be implemented directly and to extend @see {@link CommandEngine} is the more
 * convenient way.
 *
 * @since 1.0.0
 */
public interface Command<I, R> extends CorCommand<I, R> {

    /**
     * Checks if the current object (element of the chain) is responsible for the given parameter <i>I</i>.
     *
     * @param injected Generic object that will be processed by the caller of the chain.
     * @return true if the current object (element of the chain) is responsible for the given parameter <i>I</i>, false otherwise.
     */
    boolean isResponsible(I injected);

    /**
     * Processes the current element of the chain.
     * Processing the element means, that it must be checked if the current element is responsible for the given
     * parameter <i>I</i>, and if true, executes the current element.
     *
     * @param injected Generic object that will be processed by the caller of the chain.
     * @return An <i>Optional</i> that may contain an object or is empty, if the chain should not return any object.
     */
    Optional<R> processCommand(I injected);

}
