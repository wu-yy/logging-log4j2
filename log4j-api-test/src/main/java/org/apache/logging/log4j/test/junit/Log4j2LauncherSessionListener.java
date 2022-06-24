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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Global Log4j2 test setup.
 */
public class Log4j2LauncherSessionListener implements LauncherSessionListener {

    private static final String DISABLE_CONSOLE_STATUS_LISTENER = "log4j2.junit.disableConsoleStatusListener";

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        // Prevents `PropertiesUtil` from initializing (and caching the results)
        // in the middle of a test.
        final PropertiesUtil properties = PropertiesUtil.getProperties();
        if (properties.getBooleanProperty(DISABLE_CONSOLE_STATUS_LISTENER)) {
            replaceStatusConsoleListener();
        }
    }

    private static void replaceStatusConsoleListener() {
        final StatusLogger logger = StatusLogger.getLogger();
        for (final StatusListener listener : logger.getListeners()) {
            if (listener instanceof StatusConsoleListener) {
                logger.removeListener(listener);
            }
        }
        logger.registerListener(new NoOpStatusConsoleListener());
    }

    private static class NoOpStatusConsoleListener extends StatusConsoleListener {

        public NoOpStatusConsoleListener() {
            super(Level.OFF);
        }

        public void log(final StatusData data) {
            // NOP
        }
    }

}
