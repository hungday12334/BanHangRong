# TỔNG HỢP TEST CASES - SELLER REVIEW FEATURE

## 📋 OVERVIEW
Module: **SellerReviewController**
Endpoints:
- `GET /seller/reviews` - Xem dashboard đánh giá
- `POST /seller/reviews/respond/{reviewId}` - Phản hồi đánh giá
- `GET /seller/reviews/api/unanswered-count` - Lấy số lượng chưa trả lời

---

## ✅ HAPPY CASES (Luồng thành công)

### HC-01: Xem Dashboard Reviews Thành Công
**Endpoint:** `GET /seller/reviews`
- **Precondition:** 
  - Seller ID = 1 tồn tại trong database
  - Có ít nhất 1 review cho sản phẩm của seller
- **Steps:**
  1. Truy cập `/seller/reviews`
  2. System lấy tất cả reviews của seller ID = 1
  3. System lấy reviews chưa trả lời
  4. System đếm số lượng reviews chưa trả lời
- **Expected Result:**
  - Status: 200 OK
  - Model chứa: `allReviews`, `unansweredReviews`, `unansweredCount`, `sellerId`
  - Hiển thị trang `seller/reviews.html`

### HC-02: Phản Hồi Review Thành Công
**Endpoint:** `POST /seller/reviews/respond/{reviewId}`
- **Precondition:**
  - Review ID tồn tại (VD: reviewId = 5)
  - Review chưa có seller response
  - Request body hợp lệ: `{"response": "Cảm ơn bạn đã đánh giá!"}`
- **Steps:**
  1. POST request với body JSON
  2. System tìm review theo ID
  3. Cập nhật `sellerResponse` = "Cảm ơn bạn đánh giá!"
  4. Tự động set `sellerResponseAt` = now()
  5. Save vào database
- **Expected Result:**
  ```json
  {
    "success": true,
    "message": "Đã gửi phản hồi thành công",
    "review": {
      "reviewId": 5,
      "sellerResponse": "Cảm ơn bạn đã đánh giá!",
      "sellerResponseAt": "2025-10-23T10:30:00"
    }
  }
  ```

### HC-03: Lấy Số Lượng Chưa Trả Lời Thành Công
**Endpoint:** `GET /seller/reviews/api/unanswered-count?sellerId=1`
- **Precondition:** Seller ID = 1 có 3 reviews chưa trả lời
- **Expected Result:**
  ```json
  {
    "unansweredCount": 3
  }
  ```

### HC-04: Cập Nhật Lại Response (Update Response)
**Endpoint:** `POST /seller/reviews/respond/{reviewId}`
- **Precondition:** Review đã có response trước đó
- **Steps:** Gửi response mới
- **Expected Result:** Response cũ bị ghi đè, `sellerResponseAt` được cập nhật

---

## ❌ UNHAPPY CASES (Luồng lỗi)

### UC-01: Review ID Không Tồn Tại
**Endpoint:** `POST /seller/reviews/respond/999999`
- **Input:** `{"response": "Test"}`
- **Expected Result:**
  ```json
  {
    "success": false,
    "message": "Lỗi: Review not found with id: 999999"
  }
  ```

### UC-02: Request Body Thiếu Field "response"
**Endpoint:** `POST /seller/reviews/respond/5`
- **Input:** `{}` hoặc `{"text": "Hello"}`
- **Current Behavior:** NullPointerException → Response = null được lưu
- **Expected Result:** Validation error
  ```json
  {
    "success": false,
    "message": "Response không được để trống"
  }
  ```

### UC-03: Response Rỗng
**Endpoint:** `POST /seller/reviews/respond/5`
- **Input:** `{"response": ""}` hoặc `{"response": "   "}`
- **Current Behavior:** Lưu response rỗng thành công
- **Expected Result:** Validation error

### UC-04: Seller Không Phải Chủ Sở Hữu Review
**Endpoint:** `POST /seller/reviews/respond/5`
- **Scenario:** Review ID = 5 thuộc về Seller ID = 2, nhưng request từ Seller ID = 1
- **Current Behavior:** Vẫn cập nhật được (BẢO MẬT YẾU)
- **Expected Result:** 
  ```json
  {
    "success": false,
    "message": "Bạn không có quyền phản hồi review này"
  }
  ```

### UC-05: Seller ID Không Tồn Tại (Dashboard)
**Endpoint:** `GET /seller/reviews`
- **Scenario:** sellerId = 999 không tồn tại
- **Current Behavior:** Trả về lists rỗng
- **Expected Result:** Redirect hoặc error message

### UC-06: Invalid Seller ID (Count API)
**Endpoint:** `GET /seller/reviews/api/unanswered-count?sellerId=-1`
- **Input:** sellerId âm hoặc null
- **Expected Result:** 400 Bad Request

---

## 🔒 SECURITY CASES (Vấn đề bảo mật)

### SEC-01: Hardcoded Seller ID
- **Issue:** `Long sellerId = 1L;` hardcode trong code
- **Risk:** Luôn lấy reviews của seller ID = 1
- **Fix:** Lấy từ session/authentication
  ```java
  Long sellerId = (Long) session.getAttribute("userId");
  ```

### SEC-02: Không Có Authorization Check
- **Issue:** Không verify xem user có phải seller không
- **Risk:** Customer cũng có thể truy cập `/seller/reviews`
- **Fix:** Thêm `@PreAuthorize("hasRole('SELLER')")`

### SEC-03: Cross-Seller Access
- **Issue:** Seller A có thể respond review của Seller B
- **Risk:** 
  1. GET `/seller/reviews/api/unanswered-count?sellerId=2`
  2. POST `/seller/reviews/respond/99` (review của seller khác)
- **Fix:** Validate ownership trước khi response

### SEC-04: SQL Injection Risk (Gián tiếp)
- **Issue:** Dùng `@Query` với `@Param` → An toàn
- **Status:** ✅ OK (Parameterized query)

### SEC-05: No Rate Limiting
- **Issue:** Có thể spam responses
- **Risk:** DOS attack, spam database
- **Fix:** Implement rate limiting

---

## 🎯 EDGE CASES (Trường hợp biên)

### EDGE-01: Seller Không Có Review Nào
- **Scenario:** Seller mới, chưa có sản phẩm nào được review
- **Expected:** Dashboard hiển thị empty state

### EDGE-02: Review Có Comment Null
- **Scenario:** Review chỉ có rating, không có comment
- **Expected:** UI không bị lỗi, hiển thị "Không có nhận xét"

### EDGE-03: Response Rất Dài (> 1000 ký tự)
- **Input:** Response 5000 ký tự
- **Current:** Không có giới hạn (columnDefinition = TEXT)
- **Expected:** Validate max length

### EDGE-04: Concurrent Response Updates
- **Scenario:** 2 sellers cùng lúc respond 1 review (race condition)
- **Expected:** Last write wins, hoặc lock mechanism

### EDGE-05: Deleted Product Still Has Reviews
- **Scenario:** Sản phẩm bị xóa nhưng review vẫn còn
- **Expected:** Query JOIN có thể fail hoặc return null

### EDGE-06: Review Từ User Đã Xóa
- **Scenario:** userId không còn tồn tại
- **Expected:** Vẫn hiển thị review, user = "Người dùng đã xóa"

### EDGE-07: Response Có Ký Tự Đặc Biệt
- **Input:** `{"response": "<script>alert('XSS')</script>"}`
- **Expected:** HTML encoding để tránh XSS

### EDGE-08: Multiple Sellers Cùng Product (Marketplace)
- **Scenario:** Nếu hệ thống cho phép nhiều seller bán cùng 1 product
- **Expected:** Ai cũng thấy review?

---

## 📊 PERFORMANCE CASES

### PERF-01: Seller Có 10,000+ Reviews
- **Issue:** Query `findBySellerId` lấy tất cả reviews → slow
- **Fix:** Implement pagination

### PERF-02: Dashboard Load Time
- **Issue:** 3 queries riêng biệt (allReviews, unanswered, count)
- **Fix:** Optimize thành 1-2 queries

### PERF-03: N+1 Query Problem
- **Scenario:** Nếu cần load thêm Product/User info cho mỗi review
- **Fix:** Use JOIN FETCH

---

## 🔄 INTEGRATION CASES

### INT-01: Database Connection Lost
- **Scenario:** DB down khi đang load reviews
- **Expected:** Graceful error message, không crash app

### INT-02: Transaction Rollback
- **Scenario:** Exception khi save response
- **Expected:** Database rollback, không lưu partial data

### INT-03: Thymeleaf Template Missing
- **Scenario:** File `seller/reviews.html` không tồn tại
- **Expected:** 500 error với clear message

---

## 🧪 DATA VALIDATION CASES

### VAL-01: Response Length
- **Min:** 1 ký tự
- **Max:** 1000 ký tự (recommend)
- **Pattern:** Không chỉ toàn khoảng trắng

### VAL-02: Review ID Format
- **Valid:** Số nguyên dương > 0
- **Invalid:** -1, 0, "abc", null

### VAL-03: Seller ID Format
- **Valid:** Long > 0
- **Invalid:** null, -1, 0, "text"

### VAL-04: JSON Request Malformed
- **Input:** `{response: "test"}` (thiếu quotes)
- **Expected:** 400 Bad Request

---

## 🎨 UI/UX CASES

### UI-01: Empty State
- **Scenario:** Không có reviews
- **Expected:** Message "Chưa có đánh giá nào"

### UI-02: Loading State
- **Scenario:** Đang gửi response
- **Expected:** Button disabled, spinner

### UI-03: Success Feedback
- **Scenario:** Response thành công
- **Expected:** Toast notification "Đã gửi phản hồi"

### UI-04: Error Display
- **Scenario:** Response fail
- **Expected:** Error message hiển thị rõ ràng

---

## 🔧 RECOMMENDATIONS

### Critical Fixes Needed:
1. ✅ **Add Authentication/Authorization**
   - Lấy sellerId từ session, không hardcode
   - Check role SELLER
   
2. ✅ **Validate Ownership**
   - Verify review thuộc về seller trước khi respond
   
3. ✅ **Input Validation**
   - Response không rỗng, không quá dài
   - Sanitize HTML/XSS
   
4. ✅ **Error Handling**
   - Không catch Exception chung chung
   - Return proper HTTP status codes
   
5. ✅ **Pagination**
   - Thêm pagination cho dashboard

### Nice to Have:
- Rate limiting
- Audit log (ai response gì lúc nào)
- Email notification cho customer khi seller response
- Rich text editor cho response

---

## 📝 SAMPLE TEST DATA

```sql
-- Seller với nhiều reviews
INSERT INTO users (user_id, role) VALUES (1, 'SELLER');
INSERT INTO products (product_id, seller_id) VALUES (100, 1);

-- Reviews chưa response
INSERT INTO product_reviews (review_id, product_id, user_id, rating, comment, seller_response) 
VALUES 
(1, 100, 10, 5, 'Sản phẩm tốt!', NULL),
(2, 100, 11, 4, 'OK', NULL),
(3, 100, 12, 3, 'Tạm được', NULL);

-- Reviews đã response
INSERT INTO product_reviews (review_id, product_id, user_id, rating, comment, seller_response, seller_response_at) 
VALUES 
(4, 100, 13, 5, 'Tuyệt vời!', 'Cảm ơn bạn!', NOW());

-- Edge case: Review không có comment
INSERT INTO product_reviews (review_id, product_id, user_id, rating, comment, seller_response) 
VALUES (5, 100, 14, 5, NULL, NULL);
```

---

**Tổng số test cases:** 40+
**Priority High:** SEC-01, SEC-02, SEC-03, VAL-01, UC-04
**Priority Medium:** EDGE-01 đến EDGE-08, PERF-01
**Priority Low:** UI/UX cases

