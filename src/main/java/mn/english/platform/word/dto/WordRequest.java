package mn.english.platform.word.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mn.english.platform.word.Level;

public record WordRequest(
                @NotBlank @Size(max = 100) String en,
                @NotBlank @Size(max = 200) String mn,
                @NotBlank @Size(max = 50) String category,
                @NotNull Level level,
                @Size(max = 500) String example) {
}
