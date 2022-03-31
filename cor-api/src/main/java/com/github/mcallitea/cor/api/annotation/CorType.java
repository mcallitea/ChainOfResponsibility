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

package com.github.mcallitea.cor.api.annotation;

import com.github.mcallitea.cor.api.CorCommand;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that is used by the {@link CorPackage} annotation.
 * <p>
 * If the <i>CorPackage</i> and the <i>CorType</i> annotations are used, no type-annotation is necessary.
 *
 * @since 1.0.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CorType {

    Class<? extends CorCommand<?, ?>> value();

    /**
     * Order priority of the <i>CorElement</i>.
     * If the <i>order</i> value is not specified, the order of the chain-elements is unimportant for processing of the
     * chain.
     *
     * @return Priority
     */
    int order() default 0;

    /**
     * The string points to a method that returns the lambda of
     * {@link com.github.mcallitea.cor.api.CorCommandInit#init(Class)}.
     *
     * @return Name of the static method that will be called to get the initializer lambda.
     */
    String staticInitializerMethod() default "";

}