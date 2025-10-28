# Standard Customer Header Template

## CSS Styles (thêm vào `<style>` section)

```css
:root {
    --bg: #f5f7fb;
    --card: #ffffff;
    --ink: #0f172a;
    --muted: #6b7280;
    --brand: #0ea5e9;
    --chip: #e5e7eb;
    --line: #eaeef4;
}

* {
    box-sizing: border-box;
}

html, body {
    height: 100%;
}

body {
    margin: 0;
    font-family: 'Inter', system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif;
    color: var(--ink);
    background: var(--bg);
}

.wrapper {
    min-height: 100%;
    display: flex;
    flex-direction: column;
}

.topbar {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 10px 16px;
    border-bottom: 1px solid var(--line);
    background: var(--card);
    flex-wrap: nowrap;
}

.brand {
    font-weight: 800;
    letter-spacing: .2px;
}

.nav {
    display: flex;
    gap: 16px;
    margin: 0 12px;
    flex: 1;
    overflow: auto;
    white-space: nowrap;
}

.nav a {
    text-decoration: none;
    color: #111827;
    font-weight: 500;
    padding: 8px 10px;
    border-radius: 8px;
}

.nav a:hover {
    background: #f3f4f6;
}

.top-actions {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 12px;
    position: relative;
}

.support-link {
    font-size: 14px;
    color: var(--muted);
    text-decoration: none;
    padding: 6px 10px;
    border-radius: 8px;
    transition: all 0.2s;
}

.support-link:hover {
    background: #f3f4f6;
    color: #111827;
}

.user-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    color: #111827;
    border: 1px solid var(--line);
    padding: 6px 10px;
    border-radius: 8px;
    background: #fff;
    white-space: nowrap;
}

.user-btn .wallet-badge {
    margin-left: 8px;
    padding: 4px 8px;
    background: #f0f9ff;
    border: 1px solid #bae6fd;
    border-radius: 6px;
    font-weight: 600;
    color: #0369a1;
    font-size: 12px;
}

@media (max-width: 768px) {
    .user-btn .wallet-badge {
        display: none;
    }
}

.user-menu {
    position: absolute;
    right: 0;
    top: 48px;
    background: #fff;
    border: 1px solid var(--line);
    border-radius: 10px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, .08);
    width: 200px;
    display: none;
    z-index: 5;
}

.user-menu .item {
    display: block;
    padding: 10px 12px;
    color: #111827;
    text-decoration: none;
}

.user-menu .item:hover {
    background: #f3f4f6;
}

.search {
    position: relative;
}

.search input {
    height: 34px;
    width: 220px;
    padding: 0 12px;
    border: 1px solid var(--line);
    border-radius: 8px;
    outline: none;
}

.cart {
    position: relative;
    width: 28px;
    height: 28px;
    display: grid;
    place-items: center;
    border: 1px solid var(--line);
    border-radius: 8px;
    text-decoration: none;
    color: #111827;
}

.cart .badge {
    position: absolute;
    top: -6px;
    right: -6px;
    background: #ef4444;
    color: #fff;
    min-width: 18px;
    height: 18px;
    border-radius: 9px;
    font-size: 11px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 5px;
}
```

## HTML Structure (thay thế header cũ)

```html
<div class="topbar">
    <div class="brand">BanHangRong</div>
    <nav class="nav" aria-label="Main">
        <a href="/customer/dashboard">Home</a>
        <a href="/categories">Category</a>
        <a href="#">License Software</a>
        <a href="#">Game Keys</a>
        <a href="#">Voucher</a>
        <a href="/customer/support">Support</a>
    </nav>
    <div class="top-actions">
        <a href="/customer/support" class="support-link">Support</a>
        <div class="user" style="position:relative">
            <button id="userMenuBtn" class="user-btn" type="button" aria-haspopup="menu" aria-expanded="false">
                <span th:text="${user != null ? user.username : 'User123456'}">User123456</span>
                <span class="wallet-badge" th:if="${user != null}">
                    💰 <span th:text="${user != null and user.balance != null ? #numbers.formatDecimal(user.balance, 0, 'COMMA', 0, 'POINT') + '₫' : '0₫'}">0₫</span>
                </span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M7 10l5 5 5-5" stroke="#6b7280" stroke-width="2" stroke-linecap="round"
                        stroke-linejoin="round" />
                </svg>
            </button>
            <div id="userDropdown" class="user-menu" role="menu" aria-label="User menu">
                <a class="item" th:if="${user != null}" th:href="@{'/customer/profile/' + ${user.username}}" role="menuitem">My Account</a>
                <a class="item" href="/customer/notifications" role="menuitem">🔔 Notifications</a>
                <a class="item" href="/customer/wishlist" role="menuitem">💝 Wishlist</a>
                <a class="item" href="/customer/reviews" role="menuitem">⭐ My Reviews</a>
                <a class="item" href="/customer/settings" role="menuitem">⚙️ Settings</a>
                <form class="item" method="post" action="/wallet/vnpay/create" role="menuitem"
                    style="display:flex;gap:8px;align-items:center" onsubmit="return topupPrompt(event)">
                    <input type="hidden" th:if="${_csrf != null}" th:name="${_csrf.parameterName}"
                        th:value="${_csrf.token}" />
                    <input type="hidden" name="amount" id="topupAmountInput" />
                    <button type="submit" style="all:unset;cursor:pointer;color:#0ea5e9">💳 Top Up
                        (VNPay)</button>
                </form>
                <a class="item" href="/orderhistory" role="menuitem">My Orders</a>
                <a class="item seller-apply" href="/seller/dashboard" role="menuitem"
                    style="background:#f8fafc;border-top:1px solid #e2e8f0;color:#0ea5e9;font-weight:600;">📈
                    Become a Seller</a>
                <a class="item" href="#" role="menuitem"
                    onclick="event.preventDefault(); document.getElementById('logoutForm').submit();">Logout</a>
            </div>
            <form id="logoutForm" method="post" action="/logout" style="display:none;">
            </form>
        </div>
        <div class="search">
            <form method="GET" action="/customer/dashboard" style="display:flex;gap:4px;">
                <input type="search" name="search" placeholder="search" aria-label="Search" th:value="${search}"
                    style="height:34px;width:220px;padding:0 12px;border:1px solid var(--line);border-radius:8px;outline:none;">
                <button type="submit"
                    style="padding:8px 12px;background:var(--brand);color:#fff;border:none;border-radius:8px;cursor:pointer;">🔍</button>
                <a th:if="${search != null and !search.trim().isEmpty()}" th:href="@{/customer/dashboard}"
                    style="padding:8px 12px;background:#6b7280;color:#fff;text-decoration:none;border-radius:8px;display:flex;align-items:center;">✕</a>
            </form>
        </div>
        <a href="/cart" class="cart" title="Shopping Cart" aria-label="Shopping Cart">🛒<span class="badge"
                th:text="${cartCount}" th:if="${cartCount != null and cartCount > 0}">0</span></a>
    </div>
</div>
```

## JavaScript (thêm trước `</body>`)

```javascript
<script>
    // User menu toggle
    document.addEventListener('DOMContentLoaded', function() {
        const userMenuBtn = document.getElementById('userMenuBtn');
        const userDropdown = document.getElementById('userDropdown');
        
        if (userMenuBtn && userDropdown) {
            userMenuBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                userDropdown.classList.toggle('show');
            });
            
            // Close menu when clicking outside
            document.addEventListener('click', function(e) {
                if (!userMenuBtn.contains(e.target) && !userDropdown.contains(e.target)) {
                    userDropdown.classList.remove('show');
                }
            });
        }
    });

    // VNPay topup function
    function topupPrompt(e) {
        e.preventDefault();
        const v = prompt('Enter amount to top up (VND):', '200000');
        if (!v) return false;
        const clean = v.replace(/[^0-9]/g, '');
        if (!clean) {
            alert('Please enter a valid amount');
            return false;
        }
        document.getElementById('topupAmountInput').value = clean;
        e.target.submit();
        return true;
    }
</script>
```

## Files đã cập nhật:

✅ dashboard.html - Standard reference
✅ product_detail.html - Full header
✅ seller-profile.html - Full header with wallet badge
✅ orderhistory.html - Full header with wallet badge

## Files cần cập nhật tiếp:

- cart.html
- profile.html
- categories.html  
- category-products.html
- reviews.html
- settings.html
- wishlist.html
- support.html
- profile-edit.html
- notification.html

