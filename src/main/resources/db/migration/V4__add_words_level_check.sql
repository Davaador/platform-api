ALTER TABLE words
    ADD CONSTRAINT chk_words_level
    CHECK (level IN ('A1', 'A2', 'B1', 'B2'));