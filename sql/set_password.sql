-- Set password for a user (bcrypt for "123456"). Change the username as needed.
-- Local dev DB name (from application.properties) is `wap` on port 3307.
USE `wap`;

UPDATE `users`
SET `password` = '$2a$10$OW3G2kQ03pPlhuEfQdKZZeUKV6FPZsGiQiV/Lh5JL3SqSJzwpG32m' -- bcrypt("123456")
WHERE `username` = 'seller';
