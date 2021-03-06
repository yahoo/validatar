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

import static java.util.Arrays.asList;

/**
 * This formatter renders the report as an HTML report and mails it to specified recipients.
 */
@Slf4j
public class EmailFormatter implements Formatter {
    public static final String EMAIL_FORMATTER = "email";

    public static final String EMAIL_RECIPIENT = "email-recipient";
    public static final String EMAIL_RECIPIENTS = "email-recipients";
    public static final String EMAIL_SENDER_NAME = "email-sender-name";
    public static final String EMAIL_SUBJECT_PREFIX = "email-subject-prefix";
    public static final String EMAIL_FROM = "email-from";
    public static final String EMAIL_REPLY_TO = "email-reply-to";
    public static final String EMAIL_SMTP_HOST = "email-smtp-host";
    public static final String EMAIL_SMTP_PORT = "email-smtp-port";
    public static final String EMAIL_SMTP_STRATEGY = "email-smtp-strategy";

    private static final String DEFAULT_STRATEGY = "SMTP_TLS";

    private static final OptionParser PARSER = new OptionParser() {
        {
            acceptsAll(asList(EMAIL_RECIPIENT, EMAIL_RECIPIENTS), "Comma-separated or multi-option emails to send reports")
                    .withRequiredArg()
                    .required()
                    .describedAs("Report recipients' emails")
                    .withValuesSeparatedBy(COMMA);
            accepts(EMAIL_SENDER_NAME, "Name of sender displayed to report recipients")
                    .withRequiredArg()
                    .defaultsTo("Validatar");
            accepts(EMAIL_SUBJECT_PREFIX, "Prefix for the subject of the email")
                    .withRequiredArg()
                    .defaultsTo("[VALIDATAR] Test Status - ");
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
            accepts(EMAIL_SMTP_STRATEGY, "Email SMTP transport strategy - SMTP_PLAIN, SMTP_TLS, SMTP_SSL")
                    .withRequiredArg()
                    .defaultsTo(DEFAULT_STRATEGY);
            allowsUnrecognizedOptions();
        }
    };

    private static final String TWIG_TEMPLATE = "templates/email.twig";
    private static final String TWIG_ERROR_PARAM = "error";
    private static final String TWIG_TEST_LIST_PARAM = "testList";

    private static final String COMMA = ",";

    private static final String SUBJECT_FAILURE_SUFFIX = "Errored";
    private static final String SUBJECT_SUCCESS_SUFFIX = "Passed";

    private static final String X_PRIORITY = "X-Priority";
    private static final int PRIORITY = 2;

    private List<String> recipientEmails;
    private String senderName;
    private String subjectPrefix;
    private String fromEmail;
    private String replyTo;
    private String smtpHost;
    private int smtpPort;
    private TransportStrategy strategy;

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
        subjectPrefix = (String) options.valueOf(EMAIL_SUBJECT_PREFIX);
        fromEmail = (String) options.valueOf(EMAIL_FROM);
        replyTo = (String) options.valueOf(EMAIL_REPLY_TO);
        smtpHost = (String) options.valueOf(EMAIL_SMTP_HOST);
        smtpPort = Integer.parseInt((String) options.valueOf(EMAIL_SMTP_PORT));
        recipientEmails = (List<String>) options.valuesOf(EMAIL_RECIPIENTS);
        strategy = TransportStrategy.valueOf((String) options.valueOf(EMAIL_SMTP_STRATEGY));
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
        JtwigTemplate template = JtwigTemplate.classpathTemplate(TWIG_TEMPLATE);
        JtwigModel model = JtwigModel.newModel()
                                     .with(TWIG_ERROR_PARAM, hasError)
                                     .with(TWIG_TEST_LIST_PARAM, testList);
        String reportHtml = template.render(model);
        EmailBuilder emailBuilder =
            new EmailBuilder().from(senderName, fromEmail)
                    .replyTo(senderName, replyTo)
                    .subject(subjectPrefix + (hasError ? SUBJECT_FAILURE_SUFFIX : SUBJECT_SUCCESS_SUFFIX))
                    .addHeader(X_PRIORITY, PRIORITY) .textHTML(reportHtml);
        for (String recipientEmail : recipientEmails) {
            log.info("Emailing {}", recipientEmail);
            emailBuilder.to(recipientEmail);
        }
        Email reportEmail = emailBuilder.build();
        ServerConfig mailServerConfig = new ServerConfig(smtpHost, smtpPort);
        Mailer reportMailer = new Mailer(mailServerConfig, strategy);
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
