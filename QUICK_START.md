# 🚀 QUICK START - License Management

## ⚡ 3 bước để bắt đầu:

### 1️⃣ Setup Database (1 phút)
```bash
mysql -u root -p banhangrong_db < sql/create_category_licenses_table.sql
```

### 2️⃣ Chạy App (30 giây)
```bash
./mvnw spring-boot:run
```

### 3️⃣ Test Feature (2 phút)
1. Mở: http://localhost:8080/seller/categories
2. Click "Quản lý Licenses"
3. Thêm license → Gán vào category → Done! ✅

---

## 📝 Cheat Sheet

### Thêm License mới
```
Modal → "Thêm License mới" 
→ Điền form → Click "Thêm License"
```

### Gán License vào Category
```
Click 🎫 icon bên cạnh category 
→ Tìm license → Click "Thêm"
```

### Xóa License khỏi Category
```
Có 2 cách:
1. Click X trên badge trong section "Đã gán"
2. Click "Xóa" trong danh sách licenses
```

---

## 🎯 Key URLs

| URL | Mô tả |
|-----|-------|
| `/seller/categories` | Category Management page |
| `/seller/categories/api/licenses` | Get all licenses |
| `/seller/categories/api/licenses/assign` | Assign license |
| `/seller/categories/api/licenses/remove` | Remove license |

---

## 📦 Files quan trọng

**Backend:**
- `CategoryLicenses.java` - Entity
- `CategoryLicensesRepository.java` - Repository  
- `LicenseManagementService.java` - Service
- `SellerCategoryController.java` - Controller

**Frontend:**
- `category-management.html` - UI & JavaScript
- `category-management.css` - Styles

**Database:**
- `create_category_licenses_table.sql` - Migration

**Docs:**
- `FEATURE_COMPLETE.md` - Overview
- `SETUP_INSTRUCTIONS.md` - Detailed guide
- `LICENSE_MANAGEMENT_README.md` - Technical docs

---

## 🐛 Quick Troubleshooting

| Vấn đề | Fix |
|--------|-----|
| Modal không hiển thị | Hard refresh (Cmd+Shift+R) |
| 401 Unauthorized | Đăng nhập lại |
| Table not found | Chạy SQL migration |
| Compile error | `./mvnw clean compile` |

---

## ✅ Feature checklist

- [x] Tạo license mới → Lưu vào DB
- [x] Xem danh sách licenses
- [x] Gán license vào category
- [x] Xem licenses đã gán
- [x] Xóa license khỏi category
- [x] License không bị xóa khỏi DB
- [x] UI đẹp, responsive
- [x] Security: data isolation per seller

---

## 💡 Pro Tips

1. **Reuse licenses**: Gán cùng một license cho nhiều categories
2. **No data loss**: Xóa khỏi category ≠ xóa khỏi database
3. **Visual feedback**: Assigned licenses có border xanh và badge
4. **Quick access**: Có 2 cách mở modal (global + per category)
5. **Bulk management**: Có thể gán nhiều licenses cùng lúc

---

## 🎨 UI Components

```
┌─────────────────────────┐
│ 🎫 License Manager      │
├─────────────────────────┤
│ [Form thêm mới]        │ ← Tạo license
├─────────────────────────┤
│ [Licenses đã gán]      │ ← Show khi có assignments
├─────────────────────────┤
│ [Danh sách licenses]   │ ← Tất cả licenses
└─────────────────────────┘
```

---

## 📞 Need Help?

**Read these files:**
1. `SETUP_INSTRUCTIONS.md` - Step-by-step guide
2. `LICENSE_MANAGEMENT_README.md` - Technical details
3. `FEATURE_COMPLETE.md` - Full overview

**Still stuck?**
- Check browser console for errors
- Verify database connection
- Review network requests in DevTools

---

## 🎉 You're ready!

All set! Start managing licenses now. Good luck! 🚀

---

**Version**: 1.0.0 | **Date**: 2025-01-03 | **Status**: ✅ Ready

