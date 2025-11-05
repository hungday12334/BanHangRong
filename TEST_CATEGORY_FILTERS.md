# Test Guide: Category Management Advanced Filters

## 🧪 Test Scenarios

### Prerequisites
1. Login as a **seller** account
2. Navigate to Category Management page (`/seller/categories`)
3. Ensure you have some categories with products

---

## Test Case 1: Toggle Advanced Filters

### Steps:
1. Look for "Advanced Filters" section below statistics cards
2. Click the "Show" button on the right

### Expected Result:
- ✅ Filter form slides down with smooth animation
- ✅ Button text changes to "Hide"
- ✅ Icon changes from chevron-down to chevron-up
- ✅ Form shows 6 input fields:
  - Category ID (number input)
  - Category Name (text input)
  - Products From (number input)
  - Products To (number input)
  - From Date (date picker)
  - To Date (date picker)

### Steps (Hide):
3. Click "Hide" button

### Expected Result:
- ✅ Filter form slides up and hides
- ✅ Button text changes back to "Show"
- ✅ Icon changes back to chevron-down

---

## Test Case 2: Filter by Category ID

### Steps:
1. Open Advanced Filters
2. Enter a valid category ID (e.g., `1`)
3. Click "Apply Filters"

### Expected Result:
- ✅ Page reloads with URL: `/seller/categories?categoryId=1`
- ✅ Only category with ID=1 is shown in the table
- ✅ Filter badge appears below: "ID: 1"
- ✅ Filter form auto-expands showing the active filter
- ✅ "Clear filters" button is visible

---

## Test Case 3: Filter by Category Name

### Steps:
1. Open Advanced Filters
2. Enter partial category name (e.g., `elec`)
3. Click "Apply Filters"

### Expected Result:
- ✅ Page reloads with URL: `/seller/categories?categoryName=elec`
- ✅ Only categories containing "elec" in name are shown (case-insensitive)
- ✅ Filter badge appears: "Name: elec"
- ✅ Examples: "Electronics", "electronic devices", "ELECTRONICS" all match

---

## Test Case 4: Filter by Product Count Range

### Scenario A: Only Min Products
1. Enter `5` in "Products From"
2. Leave "Products To" empty
3. Click "Apply Filters"

### Expected Result:
- ✅ URL: `/seller/categories?productCountFrom=5`
- ✅ Only categories with ≥5 products shown
- ✅ Badge: "Products: 5 - ∞"

### Scenario B: Only Max Products
1. Clear "Products From"
2. Enter `10` in "Products To"
3. Click "Apply Filters"

### Expected Result:
- ✅ URL: `/seller/categories?productCountTo=10`
- ✅ Only categories with ≤10 products shown
- ✅ Badge: "Products: 0 - 10"

### Scenario C: Range
1. Enter `5` in "Products From"
2. Enter `15` in "Products To"
3. Click "Apply Filters"

### Expected Result:
- ✅ URL: `/seller/categories?productCountFrom=5&productCountTo=15`
- ✅ Only categories with 5-15 products shown
- ✅ Badge: "Products: 5 - 15"

---

## Test Case 5: Filter by Date Range

### Scenario A: From Date Only
1. Select a date in "From Date" (e.g., 2025-01-01)
2. Click "Apply Filters"

### Expected Result:
- ✅ URL: `/seller/categories?fromDate=2025-01-01`
- ✅ Only categories created after 2025-01-01 00:00:00 shown
- ✅ Badge: "From: 2025-01-01"

### Scenario B: To Date Only
1. Clear "From Date"
2. Select date in "To Date" (e.g., 2025-12-31)
3. Click "Apply Filters"

### Expected Result:
- ✅ URL: `/seller/categories?toDate=2025-12-31`
- ✅ Only categories created before 2025-12-31 23:59:59 shown
- ✅ Badge: "To: 2025-12-31"

### Scenario C: Date Range
1. Select "From Date": 2025-01-01
2. Select "To Date": 2025-03-31
3. Click "Apply Filters"

### Expected Result:
- ✅ URL: `/seller/categories?fromDate=2025-01-01&toDate=2025-03-31`
- ✅ Only categories created in Q1 2025 shown
- ✅ Two badges: "From: 2025-01-01" and "To: 2025-03-31"

---

## Test Case 6: Multiple Filters Combined

### Steps:
1. Enter category name: `tech`
2. Enter Products From: `3`
3. Enter From Date: 2025-01-01
4. Click "Apply Filters"

### Expected Result:
- ✅ URL: `/seller/categories?categoryName=tech&productCountFrom=3&fromDate=2025-01-01`
- ✅ Results match ALL conditions:
  - Name contains "tech" AND
  - Has ≥3 products AND
  - Created after 2025-01-01
- ✅ Three badges shown
- ✅ All filters are visible in the form

---

## Test Case 7: Clear Filters

### Method 1: Clear Filters Button (in form)
1. Apply some filters
2. Click "Clear Filters" button in the filter form

### Expected Result:
- ✅ All input fields are cleared
- ✅ Redirects to `/seller/categories` (no parameters)
- ✅ All categories shown again
- ✅ No filter badges displayed

### Method 2: Clear Button (in status display)
1. Apply some filters
2. Click "Clear filters" button in the active filters section

### Expected Result:
- ✅ Same as Method 1
- ✅ Redirects to base URL
- ✅ All filters removed

---

## Test Case 8: Invalid Input Handling

### Scenario A: Negative Numbers
1. Enter `-5` in "Products From"
2. Try to apply filters

### Expected Result:
- ✅ HTML5 validation prevents negative numbers (min="0")
- ✅ Browser shows validation error

### Scenario B: Invalid Date
1. Try to enter invalid date (most browsers prevent this)

### Expected Result:
- ✅ Date picker only allows valid dates
- ✅ No error occurs

### Scenario C: From > To (Products)
1. Enter Products From: `20`
2. Enter Products To: `10`
3. Click "Apply Filters"

### Expected Result:
- ✅ Backend logic filters correctly
- ✅ No results shown (empty state)
- ✅ Message: "No categories found"

---

## Test Case 9: Edge Cases

### Scenario A: Filter with No Results
1. Enter Category ID: `99999` (non-existent)
2. Apply filters

### Expected Result:
- ✅ "No categories yet" empty state shown
- ✅ Filter badge still displays
- ✅ Can clear filters to return

### Scenario B: All Categories Filtered Out
1. Enter impossible condition (e.g., Products From: 10000)
2. Apply filters

### Expected Result:
- ✅ Empty state with icon
- ✅ Filter status shows active
- ✅ Clear button available

---

## Test Case 10: UI/UX Checks

### Check 1: Responsive Design
1. Resize browser to mobile width (<768px)

### Expected Result:
- ✅ Filter inputs stack vertically (1 column)
- ✅ Badges wrap to multiple lines if needed
- ✅ Buttons remain clickable

### Check 2: Auto-Expand on Page Load
1. Apply some filters
2. Refresh the page

### Expected Result:
- ✅ Filter form is auto-expanded
- ✅ Filter values are preserved
- ✅ "Hide" button shown instead of "Show"

### Check 3: Visual Consistency
1. Compare with Review Management page

### Expected Result:
- ✅ Similar design and layout
- ✅ Same filter card styling
- ✅ Same badge colors
- ✅ Same button styles

---

## Test Case 11: Integration with Existing Features

### Test A: Search Box
1. Apply filter: categoryName=tech
2. Use the search box: type "device"

### Expected Result:
- ✅ Search box filters client-side
- ✅ Backend filter still active
- ✅ Results match both filters

### Test B: Sort Dropdown
1. Apply some filters
2. Change sort order (e.g., Name A-Z)

### Expected Result:
- ✅ Filters remain active
- ✅ Results are sorted correctly
- ✅ URL parameters preserved

### Test C: Statistics Cards
1. Apply filters to show only 2 categories
2. Check statistics cards

### Expected Result:
- ✅ Statistics show TOTAL counts (not filtered)
- ✅ Example: "Total Categories: 10" even if only 2 shown
- ✅ This is intentional - stats are global

---

## Test Case 12: URL Bookmarking/Sharing

### Steps:
1. Apply filters: `/seller/categories?categoryName=tech&productCountFrom=5`
2. Copy the URL
3. Open in new tab/window

### Expected Result:
- ✅ Same filters applied automatically
- ✅ Same results shown
- ✅ URL can be bookmarked
- ✅ URL can be shared with other sellers

---

## 🎯 Performance Checks

### Check 1: Loading Speed
- ✅ Filter form loads instantly
- ✅ No lag when toggling show/hide
- ✅ Page loads in <2 seconds with filters

### Check 2: Animation Smoothness
- ✅ Slide down/up animation is smooth (300ms)
- ✅ No jitter or jumping
- ✅ No layout shift

### Check 3: Large Dataset
- Test with 100+ categories
- Apply various filters

### Expected Result:
- ✅ No performance degradation
- ✅ Filtering happens server-side efficiently

---

## ✅ Checklist

Complete this checklist after testing:

- [ ] Filter form can be toggled
- [ ] All 6 filter types work correctly
- [ ] Multiple filters can be combined
- [ ] Clear filters works (both methods)
- [ ] Filter badges display correctly
- [ ] Auto-expand works when filters active
- [ ] Responsive on mobile
- [ ] URL parameters work correctly
- [ ] Integration with search/sort works
- [ ] No console errors
- [ ] Smooth animations
- [ ] Visual consistency with Review page

---

## 🐛 Bug Report Template

If you find issues, report using this format:

```
### Bug: [Short Description]

**Steps to Reproduce:**
1. 
2. 
3. 

**Expected Result:**
- 

**Actual Result:**
- 

**Browser:** Chrome/Firefox/Safari
**Screenshot:** [if applicable]
**Console Errors:** [if any]
```

---

## 📊 Test Results Summary

| Test Case | Status | Notes |
|-----------|--------|-------|
| Toggle Filters | ⏳ | |
| Filter by ID | ⏳ | |
| Filter by Name | ⏳ | |
| Filter by Product Count | ⏳ | |
| Filter by Date Range | ⏳ | |
| Multiple Filters | ⏳ | |
| Clear Filters | ⏳ | |
| Invalid Input | ⏳ | |
| Edge Cases | ⏳ | |
| UI/UX | ⏳ | |
| Integration | ⏳ | |
| URL Bookmarking | ⏳ | |

**Legend:** ⏳ Pending | ✅ Pass | ❌ Fail

---

**Happy Testing! 🎉**

