/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.core.test.junit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.LoggerContextAccessor;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.test.junit.ExtensionContextAnchor;
import org.apache.logging.log4j.test.junit.TestPropertySource;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import org.junit.platform.commons.util.AnnotationUtils;

class LoggerContextResolver extends TypeBasedParameterResolver<LoggerContext> implements BeforeAllCallback,
        BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        AnnotationUtils.findAnnotation(testClass, LoggerContextSource.class)
                .ifPresent(testSource -> setUpLoggerContext(testSource, context));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        if (AnnotationUtils.isAnnotated(testClass, LoggerContextSource.class)) {
            final LoggerContextAccessor accessor = ExtensionContextAnchor.getAttribute(LoggerContextAccessor.class, LoggerContextAccessor.class, context);
            if (accessor == null) {
                throw new IllegalStateException(
                        "Specified @LoggerContextSource but no LoggerContext found for test class " +
                                testClass.getCanonicalName());
            }
            if (ExtensionContextAnchor.getAttribute(ReconfigurationPolicy.class, ReconfigurationPolicy.class, context) == ReconfigurationPolicy.BEFORE_EACH) {
                accessor.getLoggerContext().reconfigure();
            }
        }
        AnnotationUtils.findAnnotation(context.getRequiredTestMethod(), LoggerContextSource.class)
                .ifPresent(source -> {
                    final LoggerContext loggerContext = setUpLoggerContext(source, context);
                    if (source.reconfigure() == ReconfigurationPolicy.BEFORE_EACH) {
                        loggerContext.reconfigure();
                    }
                });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        if (AnnotationUtils.isAnnotated(testClass, LoggerContextSource.class)) {
            final ReconfigurationPolicy policy = ExtensionContextAnchor.getAttribute(ReconfigurationPolicy.class,
                    ReconfigurationPolicy.class, context);
            if (policy == ReconfigurationPolicy.AFTER_EACH) {
                ExtensionContextAnchor.getAttribute(LoggerContextAccessor.class, LoggerContextAccessor.class, context)
                        .getLoggerContext().reconfigure();
            }
        }
    }

    @Override
    public LoggerContext resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return getLoggerContext(extensionContext);
    }

    static LoggerContext getLoggerContext(ExtensionContext context) {
        final LoggerContextAccessor accessor = ExtensionContextAnchor.getAttribute(LoggerContextAccessor.class,
                LoggerContextAccessor.class, context);
        assertNotNull(accessor);
        return accessor.getLoggerContext();
    }

    private static LoggerContext setUpLoggerContext(final LoggerContextSource source,
            final ExtensionContext extensionContext) {
        final String displayName = extensionContext.getDisplayName();
        final Injector injector = extensionContext.getTestInstance().map(DI::createInjector)
                .orElseGet(DI::createInjector);
        injector.init();
        final String configLocation = source.value();
        final URI configUri;
        if (source.v1config()) {
            TestPropertySource.createProperties(extensionContext)
                    .setProperty(ConfigurationFactory.LOG4J1_CONFIGURATION_FILE_PROPERTY, configLocation);
            configUri = null; // handled by system property
        } else {
            configUri = configLocation.isEmpty() ? null : NetUtils.toURI(configLocation);
        }
        final LoggerContext context = new LoggerContext(displayName, extensionContext, configUri, injector);
        context.putObject(Injector.class.getName(), injector);
        ExtensionContextAnchor.setAttribute(ReconfigurationPolicy.class, source.reconfigure(), extensionContext);
        ExtensionContextAnchor.setAttribute(LoggerContextAccessor.class,
                new ContextHolder(context, source.timeout(), source.unit()), extensionContext);
        ContextAnchor.THREAD_CONTEXT.set(context);
        try {
            context.start();
        } finally {
            ContextAnchor.THREAD_CONTEXT.remove();
        }
        return context;
    }

    private static class ContextHolder implements Store.CloseableResource, LoggerContextAccessor {
        private final LoggerContext context;
        private final long shutdownTimeout;
        private final TimeUnit unit;

        private ContextHolder(final LoggerContext context, final long shutdownTimeout, final TimeUnit unit) {
            this.context = context;
            this.shutdownTimeout = shutdownTimeout;
            this.unit = unit;
        }

        @Override
        public LoggerContext getLoggerContext() {
            return context;
        }

        @Override
        public void close() throws Throwable {
            context.stop(shutdownTimeout, unit);
        }
    }

}
