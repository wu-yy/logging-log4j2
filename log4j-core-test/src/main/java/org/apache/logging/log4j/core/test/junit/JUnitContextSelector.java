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
import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.LoggerContextAccessor;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.test.junit.ExtensionContextAnchor;
import org.junit.jupiter.api.extension.ExtensionContext;

public class JUnitContextSelector implements ContextSelector {

    static LoggerContext getContext(ExtensionContext context, URI configLocation) {
        LoggerContextAccessor accessor = ExtensionContextAnchor.getAttribute(LoggerContextAccessor.class,
                LoggerContextAccessor.class, context);
        assertNotNull(accessor, "Missing LoggerContext. Check if the @LoggerContextSource annotation is present.");
        return accessor.getLoggerContext();
    }

    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
        return getContext(fqcn, loader, currentContext, null);
    }

    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation) {
        if (currentContext) {
            final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
            if (context != null) {
                return context;
            }
        }
        final LoggerContext context = getContext(null, configLocation);
        if (context.getConfigLocation() == null && configLocation != null) {
            context.setConfigLocation(configLocation);
        }
        return context;
    }

    @Override
    public List<LoggerContext> getLoggerContexts() {
        LoggerContextAccessor accessor = ExtensionContextAnchor.getAttribute(LoggerContextAccessor.class,
                LoggerContextAccessor.class, null);
        return accessor != null ? List.of(accessor.getLoggerContext()) : List.of();
    }

    @Override
    public void removeContext(LoggerContext context) {
        ExtensionContextAnchor.removeAttribute(LoggerContext.class, null);
    }

}
