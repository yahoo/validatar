package com.yahoo.validatar.report.email;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Test;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.report.Formatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.config.ServerConfig;
import org.simplejavamail.mailer.config.TransportStrategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This formatter renders the report as an HTML report
 * and mails it to specified recipients.
 */
@Slf4j
public class EmailFormatter implements Formatter {

    /**
     * This class holds the necessary information for rendering
     * the Validatar report using Jtwig.
     */
    protected static class TestSuiteModel {
        /**
         * Test suite name.
         */
        public final String name;
        /**
         * Number of passed queries.
         */
        public final int queryPassed;
        /**
         * Total number of queries.
         */
        public final int queryTotal;
        /**
         * Number of passed tests.
         */
        public final int testPassed;
        /**
         * Total number of tests.
         */
        public final int testTotal;

        /**
         * List of failed queries.
         */
        public final List<Query> failedQueries;
        /**
         * List of failed tests.
         */
        public final List<Test> failedTests;

        /**
         * Create a {@code TestSuiteModel} from a {@code TestSuite}.
         * The constructor will pull the required information from
         * the given test suite.
         *
         * @param testSuite the test suite object to model
         */
        protected TestSuiteModel(TestSuite testSuite) {
            failedQueries = new LinkedList<>();
            failedTests = new LinkedList<>();
            this.name = testSuite.name;
            int passCount = 0;
            for (Query query : testSuite.queries) {
                if (query.failed()) {
                    failedQueries.add(query);
                } else {
                    passCount++;
                }
            }
            this.queryPassed = passCount;
            this.queryTotal = testSuite.queries.size();
            passCount = 0;
            for (Test test : testSuite.tests) {
                if (test.failed()) {
                    failedTests.add(test);
                } else {
                    passCount++;
                }
            }
            this.testPassed = passCount;
            this.testTotal = testSuite.tests.size();
        }

        /**
         * @return true if all queries and tests passed
         */
        protected boolean allPassed() {
            return queryPassed == queryTotal && testPassed == testTotal;
        }
    }

    private static final String EMAIL_FORMATTER = "email";

    private static final String RECIPIENT_EMAILS = "recipient-emails";
    private static final String SENDER_NAME = "sender-name";
    private static final String FROM_EMAIL = "from-email";
    private static final String REPLY_TO = "reply-to";
    private static final String SMTP_HOST = "smtp-host";
    private static final String SMTP_PORT = "smtp-port";

    /**
     * Option parser for this class. Caller should provide
     * the following arguments:
     * <ul>
     *     <li>--recipient-emails | list of emails to send reports</li>
     *     <li>--sender-name | name of the report email sender (default 'Validatar'</li>
     *     <li>--from-email | email to show as the sender</li>
     *     <li>--reply-to | email to which replies are sent</li>
     *     <li>--smtp-host | SMTP server host</li>
     *     <li>--smtp-port | SMTP server port</li>
     * </ul>
     */
    private static final OptionParser PARSER = new OptionParser() {
        {
            accepts(RECIPIENT_EMAILS, "Comma-separated list of emails to send reports")
                    .withRequiredArg()
                    .describedAs("Report recipients' emails");
            accepts(SENDER_NAME, "Name of sender displayed to report recipients")
                    .withRequiredArg()
                    .defaultsTo("Validatar");
            accepts(FROM_EMAIL, "Email shown to recipients as 'from'")
                    .withRequiredArg();
            accepts(REPLY_TO, "Email to which replies will be sent")
                    .withRequiredArg();
            accepts(SMTP_HOST, "Email SMTP host name")
                    .withRequiredArg();
            accepts(SMTP_PORT, "Email SMTP port")
                    .withRequiredArg();
            allowsUnrecognizedOptions();
        }
    };

    /**
     * List of recipient emails for Validatar reports.
     */
    private List<String> recipientEmails;
    /**
     * Sender name to display to report recipients.
     */
    private String senderName;
    /**
     * From email to display to report recipients.
     */
    private String fromEmail;
    /**
     * Email to which report email replies are sent.
     */
    private String replyTo;
    /**
     * SMTP server host.
     */
    private String smtpHost;
    /**
     * SMTP server port.
     */
    private int smtpPort;

    @Override
    @SuppressWarnings("unchecked")
    public boolean setup(String[] arguments) {
        OptionSet options = PARSER.parse(arguments);
        senderName = (String) options.valueOf(SENDER_NAME);
        fromEmail = (String) options.valueOf(FROM_EMAIL);
        replyTo = (String) options.valueOf(REPLY_TO);
        smtpHost = (String) options.valueOf(SMTP_HOST);
        smtpPort = Integer.parseInt((String) options.valueOf(SMTP_PORT));
        recipientEmails = (List<String>) options.valuesOf(RECIPIENT_EMAILS);
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Render the report HTML using Jtwig and send the result
     * to the recipient emails.
     */
    @Override
    public void writeReport(List<TestSuite> testSuites) throws IOException {
        if (testSuites == null) {
            testSuites = Collections.emptyList();
        }
        log.info("Sending report email for {} test suites", testSuites.size());
        List<TestSuiteModel> testList = new ArrayList<>(testSuites.size());
        boolean hasError = false;
        for (TestSuite testSuite : testSuites) {
            TestSuiteModel testSuiteModel = new TestSuiteModel(testSuite);
            hasError = hasError || !testSuiteModel.allPassed();
            testList.add(testSuiteModel);
        }
        JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/email.twig");
        JtwigModel model = JtwigModel.newModel()
                .with("error", hasError)
                .with("testList", testList);
        String reportHtml = template.render(model);
        EmailBuilder reportEmailBuilder = new EmailBuilder()
                .from(senderName, fromEmail)
                .replyTo(senderName, replyTo)
                .subject("Validatar Report – " + (hasError ? "Test Errors" : "Tests Passed"))
                .addHeader("X-Priority", 2)
                .textHTML(reportHtml);
        for (String recipientEmail : recipientEmails) {
            reportEmailBuilder.to(recipientEmail);
        }
        Email reportEmail = reportEmailBuilder.build();
        ServerConfig mailServerConfig = new ServerConfig(smtpHost, smtpPort);
        Mailer reportMailer = new Mailer(mailServerConfig, TransportStrategy.SMTP_TLS);
        sendEmail(reportMailer, reportEmail);
        log.info("Finishing sending report to recipients");
    }

    /**
     * Method uses the provided mailer to
     * send the email report.
     *
     * @param mailer mailer to use
     * @param email  report email
     */
    protected void sendEmail(Mailer mailer, Email email) {
        mailer.sendMail(email);
    }

    @Override
    public String getName() {
        return EMAIL_FORMATTER;
    }

    @Override
    public void printHelp() {
        Helpable.printHelp("Email report options", PARSER);
    }
}
