package banhangrong.su25.email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Email {

    @NotBlank(message = "Email cannot be blank")
    private String toEmail;

    @NotBlank(message = "Subject cannot be blank")
    private String subject;

    @NotBlank(message = "body cannot be blank")
    private String body;
}
