package com.yahoo.validatar.report.email;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.report.Formatter;
import joptsimple.OptionException;
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
import java.util.List;

/**
 * This formatter renders the report as an HTML report
 * and mails it to specified recipients.
 */
@Slf4j
public class EmailFormatter implements Formatter {
    public static final String EMAIL_FORMATTER = "email";

    protected static final String EMAIL_RECIPIENTS = "email-recipients";
    protected static final String EMAIL_SENDER_NAME = "email-sender-name";
    protected static final String EMAIL_FROM = "email-from";
    protected static final String EMAIL_REPLY_TO = "email-reply-to";
    protected static final String EMAIL_SMTP_HOST = "email-smtp-host";
    protected static final String EMAIL_SMTP_PORT = "email-smtp-port";

    /**
     * Option parser for this class. Caller should provide the following arguments:
     * <ul>
     * <li>--email-recipients | list of emails to send reports</li>
     * <li>--email-sender-name | name of the report email sender (default 'Validatar'</li>
     * <li>--email-from | email to show as the sender</li>
     * <li>--email-reply-to | email to which replies are sent</li>
     * <li>--email-smtp-host | SMTP server host</li>
     * <li>--email-smtp-port | SMTP server port</li>
     * </ul>
     */
    private static final OptionParser PARSER = new OptionParser() {
        {
            accepts(EMAIL_RECIPIENTS, "Comma-separated list of emails to send reports")
                    .withRequiredArg()
                    .describedAs("Report recipients' emails")
                    .required();
            accepts(EMAIL_SENDER_NAME, "Name of sender displayed to report recipients")
                    .withRequiredArg()
                    .defaultsTo("Validatar");
            accepts(EMAIL_FROM, "Email shown to recipients as 'from'")
                    .withRequiredArg()
                    .required();
            accepts(EMAIL_REPLY_TO, "Email to which replies will be sent")
                    .withRequiredArg()
                    .required();
            accepts(EMAIL_SMTP_HOST, "Email SMTP host name")
                    .withRequiredArg()
                    .required();
            accepts(EMAIL_SMTP_PORT, "Email SMTP port")
                    .withRequiredArg()
                    .required();
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
        OptionSet options;
        try {
            options = PARSER.parse(arguments);
        } catch (OptionException e) {
            log.error("EmailFormatter is missing required arguments", e);
            return false;
        }
        senderName = (String) options.valueOf(EMAIL_SENDER_NAME);
        fromEmail = (String) options.valueOf(EMAIL_FROM);
        replyTo = (String) options.valueOf(EMAIL_REPLY_TO);
        smtpHost = (String) options.valueOf(EMAIL_SMTP_HOST);
        smtpPort = Integer.parseInt((String) options.valueOf(EMAIL_SMTP_PORT));
        recipientEmails = (List<String>) options.valuesOf(EMAIL_RECIPIENTS);
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Render the report HTML using Jtwig and send the result to the recipient emails.
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
        EmailBuilder emailBuilder = new EmailBuilder().from(senderName, fromEmail)
                                                      .replyTo(senderName, replyTo)
                                                      .subject("Validatar Report – " + (hasError ? "Test Errors" : "Tests Passed"))
                                                      .addHeader("X-Priority", 2)
                                                      .textHTML(reportHtml);
        for (String recipientEmail : recipientEmails) {
            emailBuilder.to(recipientEmail);
        }
        Email reportEmail = emailBuilder.build();
        ServerConfig mailServerConfig = new ServerConfig(smtpHost, smtpPort);
        Mailer reportMailer = new Mailer(mailServerConfig, TransportStrategy.SMTP_TLS);
        sendEmail(reportMailer, reportEmail);
        log.info("Finished sending report to recipients");
    }

    /**
     * Method uses the provided mailer to send the email report.
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
