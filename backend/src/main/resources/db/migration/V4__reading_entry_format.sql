-- Edition the reader used (paper / e-book / audiobook); NULL = not specified (every existing entry)
ALTER TABLE reading_entries ADD COLUMN format VARCHAR(20) CHECK (format IN ('PAPER', 'EBOOK', 'AUDIOBOOK'));
