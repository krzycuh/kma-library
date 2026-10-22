-- KMA Library – initial schema
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ============================================
-- Identity & Access
-- ============================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    avatar_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_role ON users(role);

-- ============================================
-- Catalog (shared book dictionary)
-- ============================================
CREATE TABLE books (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    authors TEXT NOT NULL,
    normalized_key VARCHAR(600) NOT NULL,
    isbn13 VARCHAR(13),
    published_year INTEGER,
    cover_url VARCHAR(500),
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    external_id VARCHAR(100),
    created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_books_isbn13 ON books(isbn13) WHERE isbn13 IS NOT NULL;
CREATE INDEX idx_books_normalized_key ON books(normalized_key);
CREATE INDEX idx_books_title_trgm ON books USING gin (title gin_trgm_ops);
CREATE INDEX idx_books_authors_trgm ON books USING gin (authors gin_trgm_ops);

-- ============================================
-- Reading (personal reading log)
-- ============================================
CREATE TABLE reading_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reader_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id),
    status VARCHAR(20) NOT NULL DEFAULT 'FINISHED',
    finished_year SMALLINT,
    finished_month SMALLINT CHECK (finished_month BETWEEN 1 AND 12),
    rating SMALLINT CHECK (rating BETWEEN 1 AND 5),
    review TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (finished_month IS NULL OR finished_year IS NOT NULL)
);

CREATE INDEX idx_reading_entries_reader ON reading_entries(reader_id, finished_year DESC, finished_month DESC);
CREATE INDEX idx_reading_entries_book ON reading_entries(book_id);

-- ============================================
-- Shelf (libraries, copies, loans)
-- ============================================
CREATE TABLE libraries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_libraries_owner ON libraries(owner_id);

CREATE TABLE library_members (
    library_id UUID NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (library_id, user_id)
);

CREATE INDEX idx_library_members_user ON library_members(user_id);

CREATE TABLE shelf_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_id UUID NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SHELF' CHECK (status IN ('ON_SHELF', 'LENT', 'LOST', 'GIVEN_AWAY')),
    acquired_on DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shelf_items_library ON shelf_items(library_id, status);
CREATE INDEX idx_shelf_items_book ON shelf_items(book_id);

CREATE TABLE loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shelf_item_id UUID NOT NULL REFERENCES shelf_items(id) ON DELETE CASCADE,
    borrower_first_name VARCHAR(100) NOT NULL,
    borrower_last_name VARCHAR(100) NOT NULL,
    lent_on DATE NOT NULL,
    due_on DATE,
    returned_on DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (returned_on IS NULL OR returned_on >= lent_on)
);

CREATE UNIQUE INDEX ux_loans_active ON loans(shelf_item_id) WHERE returned_on IS NULL;
CREATE INDEX idx_loans_shelf_item ON loans(shelf_item_id, lent_on DESC);

-- ============================================
-- updated_at trigger
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_books_updated_at BEFORE UPDATE ON books
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_reading_entries_updated_at BEFORE UPDATE ON reading_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_libraries_updated_at BEFORE UPDATE ON libraries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_shelf_items_updated_at BEFORE UPDATE ON shelf_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_loans_updated_at BEFORE UPDATE ON loans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
