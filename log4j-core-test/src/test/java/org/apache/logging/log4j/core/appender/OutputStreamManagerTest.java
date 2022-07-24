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

package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.test.StatusMessages;
import org.apache.logging.log4j.test.junit.UsingStatusLogger;
import org.junit.jupiter.api.Test;

/**
 * OutputStreamManager Tests.
 */
public class OutputStreamManagerTest {

    @Test
    @LoggerContextSource("multipleIncompatibleAppendersTest.xml")
    @UsingStatusLogger
    public void narrow(final LoggerContext context, final StatusMessages messages) {
        final Logger logger = context.getLogger(OutputStreamManagerTest.class);
        logger.info("test");
        final Optional<StatusData> error = messages.findStatusMessages(Level.ERROR, "RollingRandomAccessFile")
                .findFirst();
        assertTrue(error.isPresent(), "Missing error message");
        assertEquals("Could not configure plugin element RollingRandomAccessFile: org.apache.logging.log4j.core.config.ConfigurationException: Configuration has multiple incompatible Appenders pointing to the same resource 'target/multiIncompatibleAppender.log'",
                error.get().getMessage().getFormattedMessage());
    }

    @Test
    public void testOutputStreamAppenderFlushClearsBufferOnException() {
        IOException exception = new IOException();
        final OutputStream throwingOutputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw exception;
            }
        };

        final int bufferSize = 3;
        OutputStreamManager outputStreamManager = new OutputStreamManager(throwingOutputStream, "test", null, false, bufferSize);

        for (int i = 0; i < bufferSize - 1; i++) {
            outputStreamManager.getByteBuffer().put((byte) 0);
        }

        assertEquals(outputStreamManager.getByteBuffer().remaining(), 1);

        AppenderLoggingException appenderLoggingException = assertThrows(AppenderLoggingException.class, () -> outputStreamManager.flushBuffer(outputStreamManager.getByteBuffer()));
        assertEquals(appenderLoggingException.getCause(), exception);

        assertEquals(outputStreamManager.getByteBuffer().limit(), outputStreamManager.getByteBuffer().capacity());
    }
}
