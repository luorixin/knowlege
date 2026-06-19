ALTER TABLE kb_user ADD COLUMN password VARCHAR(256);

-- Set default password '123456' for all existing users (BCrypt encoded)
-- '$2a$10$X8Ocdb730wXo//P2vTz0mOCB7Nq/U/6y2p7a/B9F5i2LzD0H1JmQO' is '123456'
UPDATE kb_user SET password = '$2a$10$X8Ocdb730wXo//P2vTz0mOCB7Nq/U/6y2p7a/B9F5i2LzD0H1JmQO' WHERE password IS NULL;

-- Make it NOT NULL after setting the defaults
ALTER TABLE kb_user ALTER COLUMN password SET NOT NULL;
