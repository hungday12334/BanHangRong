USE wap;

-- Test Hash 1
UPDATE users SET password = '$2a$10$e0MYzXyjpJS7Pd0RVvHwHeFtVpVKxKBCl7Cn6FGa.Ll8xKKqqfLq2' WHERE username = 'testadmin';

-- Test Hash 2  
UPDATE users SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi' WHERE username = 'testadmin';

-- Test Hash 3
UPDATE users SET password = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi' WHERE username = 'testadmin';