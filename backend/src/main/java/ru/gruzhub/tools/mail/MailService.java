package ru.gruzhub.tools.mail;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.springframework.stereotype.Component;
import ru.gruzhub.tools.env.EnvVariables;
import ru.gruzhub.tools.env.enums.AppMode;

@Component
public class MailService {
    private final Session session;
    private final String emailLogin;
    private final EnvVariables envVariables;

    public MailService(EnvVariables envVariables) {
        // Setup mail server
        Properties props = new Properties();
        props.put("mail.smtp.host", envVariables.EMAIL_HOST);
        props.put("mail.smtp.socketFactory.port", String.valueOf(envVariables.EMAIL_PORT));
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", String.valueOf(envVariables.EMAIL_PORT));

        this.session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(envVariables.EMAIL_LOGIN,
                                                  envVariables.EMAIL_PASSWORD);
            }
        });

        this.emailLogin = envVariables.EMAIL_LOGIN;

        this.envVariables = envVariables;
    }

    public void sendEmail(String to, String subject, String message) {
        if (this.envVariables.APP_MODE != AppMode.PRODUCTION) {
            return;
        }

        try {
            Message mimeMessage = new MimeMessage(this.session);
            mimeMessage.setFrom(new InternetAddress(this.emailLogin));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            mimeMessage.setSubject(subject);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(message, "text/html; charset=utf-8");
            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            mimeMessage.setContent(multipart);

            Transport.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
