import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMailSocketLabs {

  public static void main(String[] args) {

    final String username = "SMTP_USERNAME";
    final String password = "SMTP_PASSWORD";

    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.socketFactory.port", "465");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.host", "smtp.socketlabs.com");
    props.put("mail.smtp.port", "465");

    Session session = Session.getInstance(props,
    new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    });

    try {

      Message message = new MimeMessage(session);
      message.setHeader("X-xsMailingId", "YOUR_MAILING_ID");
      message.setFrom(new InternetAddress("YOUR_FROM_ADDRESS", "YOUR_PERSONAL_NAME"));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("RECIPIENT_EMAIL_ADDRESS"));
      message.setSubject("Test Email");
      message.setText("This is a test message.");

      Transport.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }
}
