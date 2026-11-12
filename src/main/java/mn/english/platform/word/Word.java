package mn.english.platform.word;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "words")
public class Word {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "en", nullable = false)
    private String en;

    @Column(name = "mn", nullable = false)
    private String mn;

    @Column(name = "category", nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private Level level;
    @Column(name = "example")
    private String example;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Word() {}

    public Word(String en, String mn, String category, Level level, String example, Instant now) {
        this.en = en;
        this.mn = mn;
        this.category = category;
        this.level = level;
        this.example = example;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String en, String mn, String category, Level level, String example, Instant now) {
        this.en = en;
        this.mn = mn;
        this.category = category;
        this.level = level;
        this.example = example;
        this.updatedAt = now;
    }

    public UUID getId() {return id;}
    public String getEn() {return en;}
    public String getMn() {return mn;}
    public String getCategory() {return category;}
    public Level getLevel() {return level;}
    public String getExample() {return example;}
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    
}
