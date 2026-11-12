package mn.english.platform.word.dto;

import java.time.Instant;
import java.util.UUID;

import mn.english.platform.word.Level;
import mn.english.platform.word.Word;

public record WordResponse(
        UUID id,
        String en,
        String mn,
        String category,
        Level level,
        String example,
        Instant createdAt,
        Instant updatedAt) {

    public static WordResponse from(Word word) {
        return new WordResponse(
                word.getId(),
                word.getEn(),
                word.getMn(),
                word.getCategory(),
                word.getLevel(),
                word.getExample(),
                word.getCreatedAt(),
                word.getUpdatedAt());
    }
}
