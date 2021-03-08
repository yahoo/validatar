package com.yahoo.validatar.report.email;

import com.yahoo.validatar.OutputCaptor;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.config.TransportStrategy;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class EmailFormatterTest {
    private static <T> T get(Object target, String name, Class<T> clazz) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object o = f.get(target);
            return clazz.cast(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void set(EmailFormatter target, String name, Object value) {
        Class<?> cls = EmailFormatter.class;
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSetup() {
        String[] args = {
            "--" + EmailFormatter.EMAIL_RECIPIENTS, "email@email.com",
            "--" + EmailFormatter.EMAIL_SENDER_NAME, "Validatar",
            "--" + EmailFormatter.EMAIL_FROM, "validatar@validatar.com",
            "--" + EmailFormatter.EMAIL_REPLY_TO, "validatar@validatar.com",
            "--" + EmailFormatter.EMAIL_SMTP_HOST, "host.host.com",
            "--" + EmailFormatter.EMAIL_SMTP_PORT, "25",
            "--" + EmailFormatter.EMAIL_SMTP_STRATEGY, "SMTP_PLAIN"
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
        assertEquals(TransportStrategy.SMTP_PLAIN, get(formatter, "strategy", TransportStrategy.class));
    }

    @Test
    public void testWriteReportShowsFailures() throws IOException {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        com.yahoo.validatar.common.Test skipped = new com.yahoo.validatar.common.Test();
        skipped.name = "SkippedTest";
        skipped.warnOnly = true;
        skipped.addMessage("SkippedTestMessage");
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
        ts.tests = Arrays.asList(test, skipped);
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
                    "testSuiteName1", "testName1", "queryName1", "testMessage1", "SkippedTestMessage",
                    "testMessage2", "testMessage3", "queryMessage", "SKIPPED", "SkippedTest"
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

    @Test
    public void testWriteReportPassesAndShowsMessagesWhenOnlyWarnings() throws IOException {
        com.yahoo.validatar.common.Test test = new com.yahoo.validatar.common.Test();
        com.yahoo.validatar.common.Test skipped = new com.yahoo.validatar.common.Test();
        skipped.name = "SkippedTest";
        skipped.warnOnly = true;
        skipped.addMessage("SkippedTestMessage");
        Query query = new Query();
        TestSuite ts = new TestSuite();
        ts.name = "testSuiteName1";
        test.name = "testName1";
        query.name = "queryName1";
        test.addMessage("testMessage1");
        test.addMessage("testMessage2");
        test.addMessage("testMessage3");
        query.addMessage("queryMessage");
        ts.queries = Collections.singletonList(query);
        ts.tests = Arrays.asList(test, skipped);
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
                    "SkippedTest", "SkippedTestMessage", "testSuiteName1"
                };
                String[] containsNoneOf = {
                    "testMessage1", "testMessage2", "testMessage3", "queryMessage",
                    "testName1", "queryName1"
                };
                for (String str : containsAllOf) {
                    assertTrue(html.contains(str));
                }
                for (String str : containsNoneOf) {
                    assertFalse(html.contains(str));
                }
                return null;
            }
        ).when(formatter).sendEmail(any(), any());
        formatter.writeReport(Collections.singletonList(ts));
        verify(formatter).sendEmail(any(), any());
    }

    @Test
    public void testSetupReturnsFailMissingParams() {
        EmailFormatter formatter = new EmailFormatter();
        assertFalse(formatter.setup(new String[]{}));
    }

    @Test
    public void testWriteReportEmptyTestSuites() throws IOException {
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
                assertTrue(html.contains("Nice!"));
                return null;
            }
        ).when(formatter).sendEmail(any(), any());
        formatter.writeReport(null);
    }

    @Test
    public void testSendEmail() {
        Mailer mailer = mock(Mailer.class);
        Email email = mock(Email.class);
        EmailFormatter formatter = new EmailFormatter();
        formatter.sendEmail(mailer, email);
        verify(mailer).sendMail(email);
    }

    @Test
    public void testGetName() {
        EmailFormatter formatter = new EmailFormatter();
        assertEquals(EmailFormatter.EMAIL_FORMATTER, formatter.getName());
        OutputCaptor.redirectToDevNull();
        try {
            formatter.printHelp();
        } catch (Exception e) {
            OutputCaptor.redirectToStandard();
            fail();
        }
        OutputCaptor.redirectToStandard();
    }
}
