package mn.english.platform.word;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WordRepository extends JpaRepository<Word, UUID> {
    
    Optional<Word> findByEn(String en);
    boolean existsByEn(String en);

}
