package edu.umd.mindlab.androidservicetest;

/**
 * Created by User on 10/2/2017.
 */

import java.security.Security;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

/**
 * Created by User on 10/1/2017.
 */

public class EmailSender extends javax.mail.Authenticator {
    private String mailhost = "smtp.gmail.com";
    private String user;
    private String password;
    private Session session;

    static {
        Security.addProvider(new JSSEProvider());
    }

    public EmailSender(String user, String password) {
        this.user = user;
        this.password = password;

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", mailhost);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.quitwait", "false");

        session = Session.getDefaultInstance(props, this);
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
    }

    // method for sending the email
    public synchronized void sendMailAttach(String subject, String body, String sender, String recipients,
                                            byte[] pdfBytes) throws Exception {

        DataHandler pdfHandler = new DataHandler(new ByteArrayDataSource(pdfBytes, "application/pdf"));

        MimeMessage message = new MimeMessage(session);
        message.setSender(new InternetAddress(sender));
        message.setSubject(subject);
        // Set the email message text.
        //
        MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setText(body);
        //
        // Set the email attachment file
        //
        MimeBodyPart attachmentPart = new MimeBodyPart();

        attachmentPart.setDataHandler(pdfHandler);

        //
        attachmentPart.setFileName("Consent PDF");
        //

        // add the body and attachment to the email
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messagePart);
        multipart.addBodyPart(attachmentPart);

        message.setContent(multipart);

        // if there are multiple recipients, in our case there are
        if (recipients.indexOf(',') > 0)
        {message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));}
        else
        {message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));}

        Transport.send(message);

    }
}
