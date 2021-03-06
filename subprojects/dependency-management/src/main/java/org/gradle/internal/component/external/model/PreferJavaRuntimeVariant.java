/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.internal.component.external.model;

import com.google.common.collect.ImmutableSet;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.attributes.DisambiguationRule;
import org.gradle.api.internal.attributes.EmptySchema;
import org.gradle.api.internal.attributes.MultipleCandidatesResult;
import org.gradle.api.internal.model.NamedObjectInstantiator;
import org.gradle.internal.Cast;

import java.util.Collections;
import java.util.Set;

/**
 * When no consumer attributes are provided, prefer the Java runtime variant over the API variant.
 *
 * Gradle has long assumed that, by default, consumers of a maven repository require the _runtime_ variant
 * of the published library.
 * The following disambiguation rule encodes this assumption for the case where a java library is published
 * with variants using Gradle module metadata. This will allow us to migrate to consuming the new module
 * metadata format by default without breaking a bunch of consumers that depend on this assumption,
 * declaring no preference for a particular variant.
 */
public class PreferJavaRuntimeVariant extends EmptySchema {
    private static final Usage RUNTIME_USAGE = NamedObjectInstantiator.INSTANCE.named(Usage.class, Usage.JAVA_RUNTIME);
    private static final Usage API_USAGE = NamedObjectInstantiator.INSTANCE.named(Usage.class, Usage.JAVA_API);
    private static final Set<Usage> DEFAULT_JAVA_USAGES = ImmutableSet.of(API_USAGE, RUNTIME_USAGE);
    private static final Set<Attribute<?>> SUPPORTED_ATTRIBUTES = Collections.<Attribute<?>>singleton(Usage.USAGE_ATTRIBUTE);
    private static final PreferJavaRuntimeVariant SCHEMA_DEFAULT_JAVA_VARIANTS = new PreferJavaRuntimeVariant();

    static PreferJavaRuntimeVariant schema() {
        return SCHEMA_DEFAULT_JAVA_VARIANTS;
    }

    private PreferJavaRuntimeVariant() {
    }

    @Override
    public Set<Attribute<?>> getAttributes() {
        return SUPPORTED_ATTRIBUTES;
    }

    @Override
    public DisambiguationRule<Object> disambiguationRules(Attribute<?> attribute) {
        if (Usage.USAGE_ATTRIBUTE.equals(attribute)) {
            return Cast.uncheckedCast(new DisambiguationRule<Usage>() {
                @Override
                public boolean doesSomething() {
                    return true;
                }

                public void execute(MultipleCandidatesResult<Usage> details) {
                    if (details.getConsumerValue() == null) {
                        Set<Usage> candidates = details.getCandidateValues();
                        if (candidates.equals(DEFAULT_JAVA_USAGES)) {
                            details.closestMatch(RUNTIME_USAGE);
                        } else {
                            // slower path: let's see if the candidates are either null (missing) or one of the standard usages
                            for (Usage candidate : candidates) {
                                if (candidate != null && !DEFAULT_JAVA_USAGES.contains(candidate)) {
                                    return;
                                }
                            }
                            details.closestMatch(RUNTIME_USAGE);
                        }
                    }
                }
            });
        }
        return super.disambiguationRules(attribute);
    }
}
