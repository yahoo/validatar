package com.yahoo.validatar.report.email;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import org.simplejavamail.email.Email;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

public class EmailFormatterTest {

    private static <T> T get(Object tgt, String name, Class<T> cls) {
        try {
            Field f = tgt.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object o = f.get(tgt);
            return cls.cast(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void set(EmailFormatter tgt, String name, Object val) {
        Class<?> cls = EmailFormatter.class;
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            f.set(tgt, val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSetup() {
        String[] args = {
            "--recipient-emails", "email@email.com",
            "--sender-name", "Validatar",
            "--from-email", "validatar@validatar.com",
            "--reply-to", "validatar@validatar.com",
            "--smtp-host", "host.host.com",
            "--smtp-port", "25"
        };
        EmailFormatter formatter = new EmailFormatter();
        formatter.setup(args);
        List recipientEmails = get(formatter, "recipientEmails", List.class);
        assertEquals(recipientEmails.size(), 1);
        assertEquals(recipientEmails.get(0), "email@email.com");
        assertEquals("Validatar", get(formatter, "senderName", String.class));
        assertEquals("validatar@validatar.com", get(formatter, "fromEmail", String.class));
        assertEquals("validatar@validatar.com", get(formatter, "replyTo", String.class));
        assertEquals("host.host.com", get(formatter, "smtpHost", String.class));
        assertEquals((Integer) 25, get(formatter, "smtpPort", Integer.class));
    }

    @Test
    public void testWriteReport() throws IOException {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        Query query = new Query();
        TestSuite ts = new TestSuite();
        ts.name = "testSuiteName1";
        test.name = "testName1";
        query.name = "queryName1";
        test.addMessage("testMessage1");
        test.addMessage("testMessage2");
        test.addMessage("testMessage3");
        test.setFailed();
        query.addMessage("queryMessage");
        query.setFailed();
        ts.queries = Collections.singletonList(query);
        ts.tests = Collections.singletonList(test);
        EmailFormatter formatter = mock(EmailFormatter.class);
        doCallRealMethod().when(formatter).writeReport(any());
        set(formatter, "recipientEmails", Collections.singletonList("email@email.com"));
        set(formatter, "senderName", "Validatar");
        set(formatter, "fromEmail", "from@mail.com");
        set(formatter, "replyTo", "reply@mail.com");
        set(formatter, "smtpHost", "host.host.com");
        set(formatter, "smtpPort", 25);
        doAnswer(iom -> {
                Email email = (Email) iom.getArguments()[1];
                String html = email.getTextHTML();
                String[] containsAllOf = {
                    "testSuiteName1", "testName1", "queryName1", "testMessage1",
                    "testMessage2", "testMessage3", "queryMessage"
                };
                for (String str : containsAllOf) {
                    assertTrue(html.contains(str));
                }
                return null;
            }
        ).when(formatter).sendEmail(any(), any());
        formatter.writeReport(Collections.singletonList(ts));
        verify(formatter).sendEmail(any(), any());
    }

}
