CREATE DATABASE IF NOT EXISTS productdb;
USE productdb;
CREATE TABLE IF NOT EXISTS products (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(255) NOT NULL,
 price DOUBLE NOT NULL
);
INSERT INTO products(name,price)
SELECT 'Chatpata Makhana',149.00
WHERE NOT EXISTS (SELECT 1 FROM products);
INSERT INTO products(name,price)
SELECT 'Peri Peri Makhana',159.00
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Peri Peri Makhana');
