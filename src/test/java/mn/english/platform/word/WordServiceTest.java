package mn.english.platform.word;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mn.english.platform.word.dto.WordRequest;

@ExtendWith(MockitoExtension.class)
public class WordServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T08:00:00Z");

    @Mock
    private WordRepository wordRepository;

    private WordService wordService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        wordService = new WordService(wordRepository, fixedClock);
    }

    @Test
    void createNormalizesTheWordAndUsesTheClock() {
        WordRequest request = new WordRequest(
                "  Apple ", "алим", "food", Level.A1, "I eat an apple.");
        when(wordRepository.existsByEn("apple")).thenReturn(false);
        when(wordRepository.saveAndFlush(any(Word.class))).thenAnswer(inv -> inv.getArgument(0));

        Word word = wordService.create(request);

        assertEquals("apple", word.getEn());
        assertEquals(Level.A1, word.getLevel());
        assertEquals(NOW, word.getCreatedAt());
        assertEquals(NOW, word.getUpdatedAt());
    }

    @Test
    void createFailsWhenTheWordExists() {
        WordRequest request = new WordRequest(
                "  Apple ", "алим", "food", Level.A1, "I eat an apple.");
        when(wordRepository.existsByEn("apple")).thenReturn(true);
        assertThrows(WordAlreadyExistsException.class, () -> wordService.create(request));
        verify(wordRepository, never()).saveAndFlush(any(Word.class));

    }

    @Test
    void createFailsWhenTheDatabaseRejectsADuplicate() {
        WordRequest request = new WordRequest(
                "Apple", "алим", "food", Level.A1, "I eat an apple.");
        // existsByEn says no, but another request inserted "apple" before our insert.
        when(wordRepository.existsByEn("apple")).thenReturn(false);
        when(wordRepository.saveAndFlush(any(Word.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        WordAlreadyExistsException ex = assertThrows(
                WordAlreadyExistsException.class, () -> wordService.create(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void updateFailsWhenTheWordDoesNotExist() {
        UUID id = UUID.randomUUID();
        WordRequest request = new WordRequest(
                "Apple", "алим", "food", Level.A1, "I eat an apple.");
        when(wordRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(WordNotFoundException.class, () -> wordService.update(id, request));
    }

    @Test
    void updateKeepsTheSameWordWithoutADuplicateCheck() {
        UUID id = UUID.randomUUID();
        Word existing = existingWord("apple");
        when(wordRepository.findById(id)).thenReturn(Optional.of(existing));
        when(wordRepository.saveAndFlush(existing)).thenReturn(existing);

        // " Apple " normalizes to the same "apple", so no duplicate check is needed.
        Word updated = wordService.update(id, new WordRequest(
                "  Apple ", "алим жимс", "fruit", Level.A2, "An apple a day."));

        verify(wordRepository, never()).existsByEn(anyString());
        assertEquals("apple", updated.getEn());
        assertEquals("алим жимс", updated.getMn());
        assertEquals(Level.A2, updated.getLevel());
    }

    @Test
    void updateFailsWhenChangingToAnotherExistingWord() {
        UUID id = UUID.randomUUID();
        when(wordRepository.findById(id)).thenReturn(Optional.of(existingWord("apple")));
        when(wordRepository.existsByEn("banana")).thenReturn(true);

        WordAlreadyExistsException ex = assertThrows(WordAlreadyExistsException.class,
                () -> wordService.update(id, new WordRequest(
                        "Banana", "гадил", "food", Level.A1, null)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(wordRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateSetsUpdatedAtFromTheClock() {
        UUID id = UUID.randomUUID();
        Word existing = existingWord("apple");
        when(wordRepository.findById(id)).thenReturn(Optional.of(existing));
        when(wordRepository.saveAndFlush(existing)).thenReturn(existing);

        Word updated = wordService.update(id, new WordRequest(
                "apple", "алим", "food", Level.B1, null));

        assertEquals(NOW, updated.getUpdatedAt());
        assertEquals(CREATED_AT, updated.getCreatedAt());
    }

    @Test
    void deleteFailsWhenTheWordDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(wordRepository.findById(id)).thenReturn(Optional.empty());

        WordNotFoundException ex = assertThrows(WordNotFoundException.class,
                () -> wordService.delete(id));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(wordRepository, never()).delete(any(Word.class));
    }

    @Test
    void updateFailsWhenTheDatabaseRejectsADuplicate() {
        UUID id = UUID.randomUUID();
        when(wordRepository.findById(id)).thenReturn(Optional.of(existingWord("apple")));
        // existsByEn says no, but another request inserted "banana" before our update.
        when(wordRepository.existsByEn("banana")).thenReturn(false);
        when(wordRepository.saveAndFlush(any(Word.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        WordAlreadyExistsException ex = assertThrows(WordAlreadyExistsException.class,
                () -> wordService.update(id, new WordRequest(
                        "Banana", "гадил", "food", Level.A1, null)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    /** A word that was saved earlier, so createdAt differs from the fixed clock. */
    private static Word existingWord(String en) {
        return new Word(en, "алим", "food", Level.A1, "I eat an apple.", CREATED_AT);
    }
}
