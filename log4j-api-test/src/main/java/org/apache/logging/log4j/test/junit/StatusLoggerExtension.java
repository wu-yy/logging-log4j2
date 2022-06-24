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

package org.apache.logging.log4j.test.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.StatusMessages;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

public class StatusLoggerExtension extends TypeBasedParameterResolver<StatusMessages>
        implements BeforeEachCallback, TestExecutionExceptionHandler {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private static final StatusConsoleListener CONSOLE_LISTENER = new StatusConsoleListener(Level.ALL);
    private static final Object KEY = StatusMessages.class;

    public StatusLoggerExtension() {
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        ExtensionContextAnchor.bind(context);
        final StatusMessagesHolder holder = new StatusMessagesHolder(context);
        ExtensionContextAnchor.setAttribute(KEY, holder, context);
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        printStatusMessages(context);
        throw throwable;
    }

    public void printStatusMessages(ExtensionContext context) {
        final StatusMessages statusListener = getStatusMessages(context);
        statusListener.getMessages().forEach(CONSOLE_LISTENER::log);
    }

    @Override
    public StatusMessages resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return getStatusMessages(extensionContext);
    }

    private static StatusMessages getStatusMessages(ExtensionContext extensionContext) {
        final StatusMessagesHolder holder = ExtensionContextAnchor.getAttribute(KEY, StatusMessagesHolder.class,
                extensionContext);
        return holder.get();
    }

    private static class StatusMessagesHolder implements CloseableResource, Supplier<StatusMessages> {

        private final JUnitStatusMessages statusListener;

        public StatusMessagesHolder(ExtensionContext context) {
            this.statusListener = new JUnitStatusMessages(context);
            LOGGER.registerListener(statusListener);
        }

        @Override
        public StatusMessages get() {
            return statusListener;
        }

        @Override
        public void close() throws Throwable {
            LOGGER.removeListener(statusListener);
        }

    }

    private static class JUnitStatusMessages implements StatusMessages, StatusListener {

        private final ExtensionContext context;
        private final List<StatusData> statusData = Collections.synchronizedList(new ArrayList<StatusData>());

        public JUnitStatusMessages(ExtensionContext context) {
            this.context = context;
        }

        @Override
        public void log(StatusData data) {
            if (context.equals(ExtensionContextAnchor.getContext())) {
                statusData.add(data);
            }
        }

        @Override
        public Level getStatusLevel() {
            return Level.DEBUG;
        }

        @Override
        public void close() throws IOException {
            // NOP
        }

        @Override
        public Stream<StatusData> getMessages() {
            return statusData.stream();
        }

    }
}
