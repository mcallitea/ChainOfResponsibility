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
 * @param <I>
 * @param <R>
 * @since 1.0.0
 */
public abstract class CommandEngine<I, R> implements Command<I, R> {

    private Command<I, R> nextCommand;

    /**
     * Executes the current element in the chain.
     *
     * @param injected The given object the client submits to the chain.
     * @return An <i>Optional</i> that may contain an object or is empty, if the chain should not return any object.
     */
    protected abstract Optional<R> execute(I injected);

    @Override
    public Optional<R> processCommand(I injected) {
        if (this.isResponsible(injected)) {
            return this.execute(injected);
        }

        return this.nextCommand.processCommand(injected);
    }

    @Override
    public void nextCommand(CorCommand<I, R> command) {
        this.nextCommand = (Command<I, R>) command;
    }

}
