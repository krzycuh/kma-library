-- Initial admin user
-- Default password: "admin123" (BCrypt). Change it after first login!
INSERT INTO users (id, email, password_hash, name, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin@example.com',
    '$2a$10$R8JmlnGD9G05IfJYZtZp.elgSgck/iKyc22kjJDyNYoQWbUqaeZNS',
    'Admin',
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

-- Default library for the admin user
INSERT INTO libraries (id, name, owner_id)
SELECT gen_random_uuid(), 'Biblioteczka Admin', u.id
FROM users u
WHERE u.email = 'admin@example.com'
  AND NOT EXISTS (SELECT 1 FROM libraries l WHERE l.owner_id = u.id);

INSERT INTO library_members (library_id, user_id, role)
SELECT l.id, l.owner_id, 'OWNER'
FROM libraries l
JOIN users u ON u.id = l.owner_id
WHERE u.email = 'admin@example.com'
ON CONFLICT DO NOTHING;
