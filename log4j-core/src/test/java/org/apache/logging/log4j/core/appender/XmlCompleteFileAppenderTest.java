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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests a "complete" XML file a.k.a. a well-formed XML file.
 */
public class XmlCompleteFileAppenderTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "XmlCompleteFileAppenderTest.xml");
    }

    @Test
    public void testFlushAtEndOfBatch() throws Exception {
        final File file = new File("target", "XmlCompleteFileAppenderTest.log");
        // System.out.println(f.getAbsolutePath());
        file.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String logMsg = "Message flushed with immediate flush=false";
        log.info(logMsg);
        CoreLoggerContexts.stopLoggerContext(false, file); // stop async thread

        String line1;
        String line2;
        String line3;
        String line4;
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            line1 = reader.readLine();
            line2 = reader.readLine();
            reader.readLine(); // ignore the empty line after the <Events> root
            line3 = reader.readLine();
            line4 = reader.readLine();
        } finally {
            file.delete();
        }
        assertNotNull("line1", line1);
        final String msg1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        assertTrue("line1 incorrect: [" + line1 + "], does not contain: [" + msg1 + ']', line1.equals(msg1));

        assertNotNull("line2", line2);
        final String msg2 = "<Events xmlns=\"http://logging.apache.org/log4j/2.0/events\">";
        assertTrue("line2 incorrect: [" + line2 + "], does not contain: [" + msg2 + ']', line2.equals(msg2));

        assertNotNull("line3", line3);
        final String msg3 = "<Event ";
        assertTrue("line3 incorrect: [" + line3 + "], does not contain: [" + msg3 + ']', line3.contains(msg3));

        assertNotNull("line4", line4);
        final String msg4 = logMsg;
        assertTrue("line4 incorrect: [" + line4 + "], does not contain: [" + msg4 + ']', line4.contains(msg4));

        final String location = "testFlushAtEndOfBatch";
        assertTrue("no location", !line1.contains(location));
    }

    /**
     * Test the indentation of the Events XML.
     * <p>Expected Events XML is as below.</p>
     * <pre>
&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
&lt;Events xmlns=&quot;http://logging.apache.org/log4j/2.0/events&quot;&gt;

  &lt;Event xmlns=&quot;http://logging.apache.org/log4j/2.0/events&quot; timeMillis=&quot;1460974522088&quot; thread=&quot;main&quot; level=&quot;INFO&quot; loggerName=&quot;com.foo.Bar&quot; endOfBatch=&quot;false&quot; loggerFqcn=&quot;org.apache.logging.log4j.spi.AbstractLogger&quot; threadId=&quot;11&quot; threadPriority=&quot;5&quot;&gt;
    &lt;Message&gt;First Msg tag must be in level 2 after correct indentation&lt;/Message&gt;
  &lt;/Event&gt;

  &lt;Event xmlns=&quot;http://logging.apache.org/log4j/2.0/events&quot; timeMillis=&quot;1460974522089&quot; thread=&quot;main&quot; level=&quot;INFO&quot; loggerName=&quot;com.foo.Bar&quot; endOfBatch=&quot;true&quot; loggerFqcn=&quot;org.apache.logging.log4j.spi.AbstractLogger&quot; threadId=&quot;11&quot; threadPriority=&quot;5&quot;&gt;
    &lt;Message&gt;Second Msg tag must also be in level 2 after correct indentation&lt;/Message&gt;
  &lt;/Event&gt;
&lt;/Events&gt;
     * </pre>
     * @throws Exception
     */
    @Test
    public void testChildElementsAreCorrectlyIndented() throws Exception {
        final File file = new File("target", "XmlCompleteFileAppenderTest.log");
        file.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String firstLogMsg = "First Msg tag must be in level 2 after correct indentation";
        log.info(firstLogMsg);
        final String secondLogMsg = "Second Msg tag must also be in level 2 after correct indentation";
        log.info(secondLogMsg);
        CoreLoggerContexts.stopLoggerContext(false, file); // stop async thread

        final String[] lines = new String[9];

        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {

            int usefulLinesIndex = 0;
            String readLine;
            while((readLine = reader.readLine()) != null) {

                if (!"".equals(readLine.trim())) {
                    lines[usefulLinesIndex] = readLine;
                    usefulLinesIndex++;
                }
            }
        } finally {
            file.delete();
        }

        String currentLine = lines[0];
        assertFalse("line1 incorrect: [" + currentLine + "], must have no indentation", currentLine.startsWith(" "));
        // <EVENTS
        currentLine = lines[1];
        assertFalse("line2 incorrect: [" + currentLine + "], must have no indentation", currentLine.startsWith(" "));
        // <EVENT
        currentLine = lines[2];
        assertTrue("line3 incorrect: [" + currentLine + "], must have two-space indentation", currentLine.startsWith("  "));
        assertFalse("line3 incorrect: [" + currentLine + "], must not have more than two-space indentation", currentLine.startsWith("   "));
        // <MSG
        currentLine = lines[3];
        assertTrue("line4 incorrect: [" + currentLine + "], must have four-space indentation", currentLine.startsWith("    "));
        assertFalse("line4 incorrect: [" + currentLine + "], must not have more than four-space indentation", currentLine.startsWith("     "));
        // </EVENT
        currentLine = lines[4];
        assertTrue("line5 incorrect: [" + currentLine + "], must have two-space indentation", currentLine.startsWith("  "));
        assertFalse("line5 incorrect: [" + currentLine + "], must not have more than two-space indentation", currentLine.startsWith("   "));

        // <EVENT
        currentLine = lines[5];
        assertTrue("line6 incorrect: [" + currentLine + "], must have two-space indentation", currentLine.startsWith("  "));
        assertFalse("line6 incorrect: [" + currentLine + "], must not have more than two-space indentation", currentLine.startsWith("   "));
        // <MSG
        currentLine = lines[6];
        assertTrue("line7 incorrect: [" + currentLine + "], must have four-space indentation", currentLine.startsWith("    "));
        assertFalse("line7 incorrect: [" + currentLine + "], must not have more than four-space indentation", currentLine.startsWith("     "));
        // </EVENT
        currentLine = lines[7];
        assertTrue("line8 incorrect: [" + currentLine + "], must have two-space indentation", currentLine.startsWith("  "));
        assertFalse("line8 incorrect: [" + currentLine + "], must not have more than two-space indentation", currentLine.startsWith("   "));
        // </EVENTS
        currentLine = lines[8];
        assertFalse("line9 incorrect: [" + currentLine + "], must have no indentation", currentLine.startsWith(" "));
    }
}
