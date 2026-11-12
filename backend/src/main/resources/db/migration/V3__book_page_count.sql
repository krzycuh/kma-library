-- Number of pages of a book (approximate: the catalog is shared, so one value per title, not per edition)
ALTER TABLE books ADD COLUMN page_count INTEGER;

-- One row per book the page-count backfill has already tried, so each book is looked up once
-- (UNAVAILABLE rows are retried at the next start)
CREATE TABLE book_page_count_lookups (
    book_id UUID PRIMARY KEY REFERENCES books(id) ON DELETE CASCADE,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    outcome VARCHAR(20) NOT NULL,
    query_kind VARCHAR(20),
    provider VARCHAR(30),
    page_count INTEGER
);
