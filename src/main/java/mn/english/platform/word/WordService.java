package mn.english.platform.word;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mn.english.platform.word.dto.WordRequest;

@Service
public class WordService {

    private final WordRepository wordRepository;
    private final Clock clock;

    public WordService(WordRepository wordRepository, Clock clock) {
        this.wordRepository = wordRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Word> findAll() {
        return wordRepository.findAll();
    }

    @Transactional
    public Word create(WordRequest request) {
        String en = normalizeEn(request.en());
        if (wordRepository.existsByEn(en)) {
            throw new WordAlreadyExistsException();
        }
        try {
            Instant now = clock.instant();
            return wordRepository
                    .saveAndFlush(new Word(en, request.mn(), request.category(), request.level(),
                            request.example(), now));
        } catch (DataIntegrityViolationException e) {
            // Another request inserted the same word between existsByEn and the insert:
            // the UNIQUE constraint on en is the only one a valid request can hit.
            throw new WordAlreadyExistsException();
        }
    }

    @Transactional
    public void delete(UUID id) {
        Word word = wordRepository.findById(id)
                .orElseThrow(WordNotFoundException::new);

        wordRepository.delete(word);

    }

    @Transactional
    public Word update(UUID id, WordRequest request) {
        String en = normalizeEn(request.en());
        Word word = wordRepository.findById(id)
                .orElseThrow(WordNotFoundException::new);
        if (!en.equals(word.getEn()) && wordRepository.existsByEn(en)) {
            throw new WordAlreadyExistsException();
        }
        Instant now = clock.instant();
        word.update(en, request.mn(), request.category(), request.level(), request.example(), now);
        try {
            return wordRepository.saveAndFlush(word);
        } catch (DataIntegrityViolationException e) {
            throw new WordAlreadyExistsException();
        }
    }

    private static String normalizeEn(String en) {
        return en.trim().toLowerCase(Locale.ROOT);
    }

}
