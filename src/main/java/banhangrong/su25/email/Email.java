package banhangrong.su25.email;

import jakarta.validation.constraints.NotBlank;

public class Email {
    @NotBlank(message = "Email cannot be blank")
    private String toEmail;

    @NotBlank(message = "Subject cannot be blank")
    private String subject;

    @NotBlank(message = "body cannot be blank")
    private String body;

    // No-args constructor
    public Email() {
    }

    // All-args constructor
    public Email(String toEmail, String subject, String body) {
        this.toEmail = toEmail;
        this.subject = subject;
        this.body = body;
    }

    // Getters and Setters
    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
