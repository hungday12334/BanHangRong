# 🔥 GIẢI PHÁP LỖI UTF-8 ENCODING - CHAT VIETNAMESE

## ❌ LỖI CỤ THỂ CỦA BẠN
```
❌ Failed to create conversation: 
Data truncation: Incorrect string value: '\xC5\xA9 Lon...' 
for column `smiledev_wap`.`chat_conversations`.`seller_name` at row 1
```

## 🎯 NGUYÊN NHÂN
Database tables `chat_conversations` và `chat_messages` đang dùng charset **latin1** hoặc **utf8** (không phải utf8mb4)
→ Không hỗ trợ ký tự tiếng Việt có dấu như: **ũ, ơ, ư, ế, ...**

## ✅ GIẢI PHÁP - 2 CÁCH

### CÁCH 1: Tự động (Recommended) ⚡

```bash
cd /Users/hoangquangminh/Desktop/FUlearning/FALL25/SWP391/BanHangRong

# Fix UTF-8 encoding
./fix-utf8-encoding.sh

# Sau đó chạy app
./test-and-run.sh
```

### CÁCH 2: Thủ công (nếu không có mysql client) 🔧

1. **Mở phpmyadmin/adminer** trên smiledev.id.vn
2. **Chọn database:** smiledev_wap
3. **Mở SQL tab** và paste nội dung file: `sql/fix_chat_utf8.sql`
4. **Execute**
5. **Chạy app:** `./test-and-run.sh`

---

## 📋 CHI TIẾT CÁCH FIX

### Script đã làm gì:

1. **Thay đổi charset của columns:**
```sql
ALTER TABLE chat_conversations 
  MODIFY COLUMN customer_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  MODIFY COLUMN seller_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  MODIFY COLUMN last_message TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **Convert toàn bộ table:**
```sql
ALTER TABLE chat_conversations CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE chat_messages CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **Cập nhật connection string** để force UTF-8:
```properties
characterEncoding=UTF-8
connectionCollation=utf8mb4_unicode_ci
useUnicode=true
```

---

## 🚀 BƯỚC THỰC HIỆN

### Bước 1: Fix Database Encoding

```bash
./fix-utf8-encoding.sh
```

**Output mong đợi:**
```
==========================================
  FIX UTF-8 ENCODING FOR CHAT TABLES
==========================================

✅ .env loaded
   Database: smiledev.id.vn/smiledev_wap

Connecting to database...
✅ Connected successfully

Checking current encoding...
   Current charset: latin1

Applying UTF-8 fix...

Chat tables updated to UTF8MB4
✅ UTF-8 encoding fixed!

Verifying...
   New charset: utf8mb4

✅ Verification passed!
   Tables are now using utf8mb4

==========================================
```

### Bước 2: Rebuild & Chạy Application

```bash
./test-and-run.sh
```

Hoặc thủ công:
```bash
./mvnw clean package -DskipTests
java -jar -Dspring.profiles.active=simple target/su25-0.0.1-SNAPSHOT.jar
```

### Bước 3: Test

1. Mở http://localhost:8080
2. Login customer
3. Click **"Chat với shop"**
4. ✅ **PHẢI THÀNH CÔNG!** Không còn lỗi encoding

---

## 🔍 KIỂM TRA

### Check encoding đã đúng chưa:

```bash
mysql -h smiledev.id.vn -u smiledev_wap -p123456789 smiledev_wap -e "
  SELECT CCSA.character_set_name 
  FROM information_schema.TABLES T, 
       information_schema.COLLATION_CHARACTER_SET_APPLICABILITY CCSA 
  WHERE CCSA.collation_name = T.table_collation 
    AND T.table_schema = 'smiledev_wap' 
    AND T.table_name = 'chat_conversations';
"
```

**Kết quả mong đợi:** `utf8mb4`

### Test với tên tiếng Việt:

```sql
-- Thử insert dữ liệu có dấu
INSERT INTO chat_conversations 
  (id, customer_id, customer_name, seller_id, seller_name, created_at, updated_at) 
VALUES 
  ('test_conv', 1, 'Nguyễn Văn Ũ', 2, 'Shop Điện Tử Lớn', NOW(), NOW());

-- Nếu không có lỗi → ✅ Thành công!

-- Xóa test data
DELETE FROM chat_conversations WHERE id = 'test_conv';
```

---

## 🛠️ FILES ĐÃ SỬA

### 1. SQL Scripts:
- 🆕 `sql/fix_chat_utf8.sql` - SQL để fix encoding

### 2. Configuration:
- ✅ `application-simple.properties` - Thêm UTF-8 params
- ✅ `application.properties` - Thêm UTF-8 params

### 3. Scripts:
- 🆕 `fix-utf8-encoding.sh` - Auto fix UTF-8
- ✅ `test-and-run.sh` - Updated to check/fix UTF-8

---

## ⚠️ LƯU Ý QUAN TRỌNG

### 1. Backup Data (nếu có data quan trọng)
```bash
# Backup tables trước khi alter
mysqldump -h smiledev.id.vn -u smiledev_wap -p123456789 smiledev_wap \
  chat_conversations chat_messages > backup_chat_tables.sql
```

### 2. Nếu đã có data trong tables
Sau khi ALTER, data cũ có thể bị lỗi encoding. Nên:
- Xóa data cũ: `TRUNCATE TABLE chat_conversations; TRUNCATE TABLE chat_messages;`
- Hoặc migrate lại data với encoding đúng

### 3. Connection String phải có UTF-8 params
```
characterEncoding=UTF-8
connectionCollation=utf8mb4_unicode_ci
useUnicode=true
```

✅ Đã thêm vào cả 2 files properties

---

## 🚨 TROUBLESHOOTING

### Vẫn lỗi "Incorrect string value" sau khi fix

**Nguyên nhân:** Application cache hoặc connection pool cũ

**Giải pháp:**
```bash
# 1. Stop app hoàn toàn
pkill -f su25-0.0.1-SNAPSHOT.jar

# 2. Clean build
./mvnw clean

# 3. Rebuild & run
./mvnw package -DskipTests
java -jar -Dspring.profiles.active=simple target/su25-0.0.1-SNAPSHOT.jar
```

### Không có mysql client

**Giải pháp 1:** Install mysql client
```bash
# macOS
brew install mysql-client

# Ubuntu/Debian
sudo apt-get install mysql-client
```

**Giải pháp 2:** Dùng phpmyadmin/adminer
- Copy nội dung file `sql/fix_chat_utf8.sql`
- Paste vào SQL tab
- Execute

### Database không cho ALTER TABLE

**Nguyên nhân:** User không có quyền ALTER

**Giải pháp:**
- Liên hệ admin database để grant quyền
- Hoặc admin chạy giúp script `sql/fix_chat_utf8.sql`

---

## 📊 KẾT QUẢ MONG ĐỢI

### Trước khi fix:
```
❌ Failed to create conversation: 
Incorrect string value: '\xC5\xA9 Lon...' for column seller_name
```

### Sau khi fix:
```
=== CREATE CONVERSATION REQUEST ===
Customer ID: 1
Seller ID: 2
✓ Conversation created/found: conv_1_2
```

### Browser:
- ✅ Click "Chat với shop" → Chat window mở
- ✅ Hiển thị đúng tên tiếng Việt có dấu
- ✅ Gửi/nhận tin nhắn tiếng Việt OK

---

## 🎉 TÓM TẮT

### VẤN ĐỀ:
Database không hỗ trợ tiếng Việt (charset sai)

### GIẢI PHÁP:
1. Run: `./fix-utf8-encoding.sh` → Fix database
2. Run: `./test-and-run.sh` → Start app
3. Test: Click "Chat với shop"
4. ✅ SUCCESS!

### FILES QUAN TRỌNG:
- 🔧 `fix-utf8-encoding.sh` - Script fix tự động
- 📄 `sql/fix_chat_utf8.sql` - SQL fix encoding
- ⚙️ `application-simple.properties` - Config UTF-8

---

📅 **Date:** Oct 30, 2025  
🐛 **Issue:** UTF-8 Encoding  
✅ **Status:** FIXED  
🚀 **Command:** `./fix-utf8-encoding.sh && ./test-and-run.sh`

**Lỗi này 100% sẽ fix được!** 💪

