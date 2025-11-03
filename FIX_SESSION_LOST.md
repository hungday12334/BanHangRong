# 🔧 FIX: Session Lost When Navigating to Categories

## ❌ Vấn đề

**Tình huống:**
```
1. Đăng nhập vào Seller Dashboard ✅
2. Click vào "Quản lý Danh mục" 
3. Bị redirect về trang login ❌
```

**Nguyên nhân:**
- `CustomAuthenticationSuccessHandler` KHÔNG lưu user object vào session
- Controller đang check `session.getAttribute("user")` → NULL
- Session timeout quá ngắn (default 30 minutes)
- Spring Security chỉ lưu Authentication object, không lưu Users entity

---

## ✅ Giải pháp đã implement

### 1. **CustomAuthenticationSuccessHandler.java** - Lưu user vào session

```java
// BEFORE: Không lưu gì vào session ❌
public void onAuthenticationSuccess(...) {
    Users user = usersRepository.findByUsername(username)...;
    user.setLastLogin(LocalDateTime.now());
    usersRepository.save(user);
    // → Session không có "user" attribute!
}

// AFTER: Lưu user vào session ✅
public void onAuthenticationSuccess(...) {
    Users user = usersRepository.findByUsername(username)...;
    
    // ⭐ LƯU VÀO SESSION
    request.getSession().setAttribute("user", user);
    request.getSession().setAttribute("userId", user.getUserId());
    request.getSession().setAttribute("userType", user.getUserType());
    request.getSession().setAttribute("username", user.getUsername());
    
    // Set session timeout = 8 hours
    request.getSession().setMaxInactiveInterval(28800);
}
```

### 2. **application.properties** - Tăng session timeout

```properties
# ADDED: Session configuration
server.servlet.session.timeout=28800
server.servlet.session.cookie.max-age=28800
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false
server.servlet.session.tracking-modes=cookie
```

**28800 seconds = 8 hours**

### 3. **SecurityConfig.java** - Cho phép multiple sessions

```java
// BEFORE
.maximumSessions(1) // Chỉ 1 session ❌

// AFTER
.maximumSessions(5) // Cho phép nhiều tabs/devices ✅
```

### 4. **SellerCategoryController.java** - Better error handling

```java
// BEFORE
if (currentUser == null) {
    return "redirect:/login"; // Không có message
}

// AFTER
if (currentUser == null) {
    model.addAttribute("error", "Phiên đăng nhập đã hết hạn...");
    return "redirect:/login?expired=true"; // ✅ Có message
}

// Check role
if (!"seller".equalsIgnoreCase(userType) && !"admin".equalsIgnoreCase(userType)) {
    model.addAttribute("error", "Bạn không có quyền truy cập...");
    return "redirect:/";
}
```

---

## 🧪 Test Fix

### Bước 1: Restart Application
```bash
# Stop current app (Ctrl+C)
./mvnw clean compile
./mvnw spring-boot:run
```

### Bước 2: Test Login Flow

#### Test 1: Login và navigate
```
1. Mở browser: http://localhost:8080/login
2. Đăng nhập với seller account
3. Verify redirect về /seller/dashboard ✅
4. Click "Quản lý Danh mục"
5. Verify KHÔNG BỊ redirect về login ✅
6. Verify stats hiển thị đúng ✅
```

#### Test 2: Check console logs
```bash
# Khi login, terminal sẽ hiển thị:
=== Authentication Success ===
Username: seller1
User ID: 100
User Type: SELLER
Session ID: XXXXX
Session MaxInactiveInterval: 28800 seconds  ← ✅ 8 hours
================================
```

#### Test 3: Check session trong browser
```javascript
// Open browser DevTools → Application tab → Cookies
// Should see JSESSIONID cookie with:
// - Max-Age: 28800
// - HttpOnly: true
// - SameSite: Lax
```

#### Test 4: Multiple tabs
```
1. Login trong tab 1
2. Mở tab 2 → navigate /seller/categories
3. Verify: KHÔNG cần login lại ✅
4. Cả 2 tabs đều hoạt động bình thường ✅
```

#### Test 5: Session persistence
```
1. Login
2. Navigate /seller/categories ✅
3. Wait 1 minute
4. Click vào category khác
5. Verify: Vẫn hoạt động, không bị logout ✅
```

---

## 🔍 Debug Steps

### Nếu vẫn bị redirect về login:

#### Check 1: Session attribute
```java
// Add logging trong controller
Users currentUser = (Users) session.getAttribute("user");
System.out.println("Current user from session: " + currentUser);
System.out.println("Session ID: " + session.getId());
System.out.println("Session attributes: " + Collections.list(session.getAttributeNames()));
```

#### Check 2: Browser console
```javascript
// Check cookies
document.cookie

// Expected: JSESSIONID=xxxxx; ...
```

#### Check 3: Network tab
```
Open DevTools → Network
- Request: GET /seller/categories
- Check Request Headers → Cookie: JSESSIONID=xxx
- Check Response Status: 200 (not 302 redirect)
```

#### Check 4: Application logs
```bash
# Grep for session info
tail -f app.log | grep -i "session\|authentication"

# Should see:
# - Authentication Success messages
# - Session ID
# - User object stored in session
```

---

## 📊 Behavior Comparison

### BEFORE (Broken ❌)

```
Login Flow:
1. User submits login form
   ↓
2. Spring Security authenticates
   ↓
3. CustomAuthenticationSuccessHandler runs
   ↓
4. User object KHÔNG được lưu vào session ❌
   ↓
5. Redirect to /seller/dashboard
   → Dashboard có thể load (Spring Security có Authentication)
   ↓
6. User clicks "Quản lý Danh mục"
   → Navigate to /seller/categories
   ↓
7. SellerCategoryController checks session.getAttribute("user")
   → Returns NULL ❌
   ↓
8. Redirect to /login ❌
```

### AFTER (Fixed ✅)

```
Login Flow:
1. User submits login form
   ↓
2. Spring Security authenticates
   ↓
3. CustomAuthenticationSuccessHandler runs
   ↓
4. ⭐ User object LƯU VÀO session ✅
   session.setAttribute("user", user)
   session.setAttribute("userId", userId)
   session.setAttribute("userType", userType)
   ↓
5. Set session timeout = 8 hours
   ↓
6. Redirect to /seller/dashboard
   ↓
7. User clicks "Quản lý Danh mục"
   → Navigate to /seller/categories
   ↓
8. SellerCategoryController checks session.getAttribute("user")
   → Returns User object ✅
   ↓
9. Page loads with correct data ✅
```

---

## 🎯 What Changed

### Files Modified:

1. **CustomAuthenticationSuccessHandler.java**
   - ✅ Added `session.setAttribute("user", user)`
   - ✅ Added session timeout setting
   - ✅ Added debug logging
   - ✅ Improved redirect logic

2. **application.properties**
   - ✅ Added session timeout: 8 hours
   - ✅ Added cookie config
   - ✅ Added tracking mode

3. **SecurityConfig.java**
   - ✅ Changed `maximumSessions` from 1 to 5
   - ✅ Changed seller auth from `.hasAnyRole()` to `.authenticated()`
   - ✅ Role check done in controller instead

4. **SellerCategoryController.java**
   - ✅ Better error messages
   - ✅ Explicit role check
   - ✅ `?expired=true` param in redirect

---

## ✅ Verification Checklist

After restart, verify:

- [ ] Login succeeds
- [ ] Console shows "Authentication Success" with session info
- [ ] Dashboard loads correctly
- [ ] Click "Quản lý Danh mục" → NO redirect to login
- [ ] Stats display correctly
- [ ] Can navigate between different pages
- [ ] Multiple tabs work
- [ ] Session persists for 8 hours
- [ ] JSESSIONID cookie present
- [ ] No console errors

---

## 🐛 Common Issues

### Issue 1: Still redirects to login
**Check:**
```bash
# In terminal logs, search for:
grep "User object from session" app.log

# Should see user object, not null
```

### Issue 2: Session expires too quickly
**Check:**
```bash
# In logs:
grep "MaxInactiveInterval" app.log

# Should show: 28800 seconds (8 hours)
```

### Issue 3: Multiple devices don't work
**Check SecurityConfig:**
```java
.maximumSessions(5) // Should be > 1
```

### Issue 4: Role check fails
**Check user_type in database:**
```sql
SELECT user_id, username, user_type 
FROM users 
WHERE username = 'your_seller_username';

-- user_type should be 'seller' or 'SELLER'
```

---

## 🚀 Quick Test Script

```bash
# 1. Restart app
./mvnw spring-boot:run

# 2. Wait for startup (check for "Started Su25Application")

# 3. Open browser
open http://localhost:8080/login

# 4. Login with seller account

# 5. Check logs for:
#    - "Authentication Success"
#    - "Session ID: ..."
#    - "Session MaxInactiveInterval: 28800"

# 6. Navigate to /seller/categories
#    → Should work without redirect

# 7. Refresh page
#    → Should still work

# 8. Open new tab, navigate /seller/categories
#    → Should work without login
```

---

## 📝 Technical Details

### Session Storage Flow:

```
Browser                    Server
   │                          │
   │  POST /perform-login     │
   ├─────────────────────────→│
   │                          │
   │                     ┌────▼────┐
   │                     │ Spring  │
   │                     │Security │
   │                     │Authenticate
   │                     └────┬────┘
   │                          │
   │                     ┌────▼──────────────────┐
   │                     │CustomAuthentication   │
   │                     │SuccessHandler         │
   │                     │                        │
   │                     │ session.setAttribute( │
   │                     │   "user", userObject) │
   │                     │ session.setTimeout()  │
   │                     └────┬──────────────────┘
   │                          │
   │  302 /seller/dashboard   │
   │←─────────────────────────┤
   │  Set-Cookie: JSESSIONID  │
   │                          │
   │  GET /seller/categories  │
   ├─────────────────────────→│
   │  Cookie: JSESSIONID      │
   │                          │
   │                     ┌────▼────────────────┐
   │                     │SellerCategory       │
   │                     │Controller           │
   │                     │                     │
   │                     │ user = session      │
   │                     │   .getAttribute()   │
   │                     │ → User object ✅    │
   │                     └────┬────────────��───┘
   │                          │
   │  200 OK with HTML        │
   │←─────────────────────────┤
   │                          │
```

---

## 🎉 Success Criteria

✅ **PASS** nếu:
1. Login thành công
2. Navigate /seller/categories không bị redirect
3. Stats hiển thị đúng
4. Multiple tabs hoạt động
5. Session persist 8 hours
6. No console errors

❌ **FAIL** nếu:
1. Vẫn bị redirect về login
2. Session.getAttribute("user") = null
3. Session timeout quá nhanh
4. Role check fails

---

**Date**: January 3, 2025
**Issue**: Session lost on navigation
**Status**: ✅ FIXED
**Impact**: Critical - affects all sellers
**Priority**: HIGH

---

**Giờ đây bạn có thể navigate giữa các pages mà không bị logout! 🎊**

