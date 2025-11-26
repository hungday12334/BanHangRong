document.addEventListener('DOMContentLoaded', function () {
    const hiddenInput = document.getElementById('selected-product-ids');
    const checkboxes = Array.from(document.querySelectorAll('.item-checkbox'));
    const checkoutForm = document.getElementById('checkout-form');
    const deleteSelectedBtn = document.getElementById('delete-selected-btn');
    let isSubmitting = false; // prevent re-entry

    // Read CSRF token/header from meta tags (Thymeleaf provides these in the template)
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]')?.content;

    // Fallback: look for any hidden input that looks like a CSRF token in the checkout form or page
    const csrfInputEl = document.querySelector('input[type="hidden"][name*="csrf"]');
    const csrfTokenFallback = csrfInputEl ? csrfInputEl.value : null;
    const csrfNameFallback = csrfInputEl ? csrfInputEl.getAttribute('name') : null;

    const csrfToken = csrfTokenMeta || csrfTokenFallback;
    // Determine header name: prefer meta header, else use common header name
    const csrfHeader = csrfHeaderMeta || 'X-CSRF-TOKEN';

    function makeHeaders() {
        const h = { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' };
        if (csrfToken) h[csrfHeader] = csrfToken;
        return h;
    }

    function updateHiddenInput() {
        const selected = checkboxes
            .filter(chk => chk.checked)
            .map(chk => chk.getAttribute('data-product-id'))
            .filter(Boolean);
        hiddenInput.value = selected.join(',');
        // Show/hide delete button
        if (deleteSelectedBtn) deleteSelectedBtn.style.display = selected.length ? 'inline-block' : 'none';
    }

    // initialize
    updateHiddenInput();

    // wire change events
    checkboxes.forEach(chk => {
        chk.addEventListener('change', updateHiddenInput);
    });

    // ensure hidden input is updated right before submit (in case of dynamic changes)
    if (checkoutForm) {
        checkoutForm.addEventListener('submit', function (e) {
            // If this is the re-submission after we applied updates, allow it
            if (isSubmitting) return true;

            e.preventDefault();
            updateHiddenInput();
            // If no items selected, prevent submit and show a toast
            if (!hiddenInput.value || hiddenInput.value.trim() === '') {
                showToast('Please select at least one item to checkout');
                return;
            }

            // Before final submit, ensure server-side cart quantities reflect latest input values.
            const selectedIds = hiddenInput.value.split(',').map(s => s.trim()).filter(Boolean);
            const updates = [];
            selectedIds.forEach(id => {
                const qtyInput = document.querySelector('.qty-input[data-product-id="' + id + '"]');
                if (qtyInput) {
                    let qty = parseInt(qtyInput.value, 10);
                    if (isNaN(qty) || qty < 1) qty = 1;
                    const max = parseInt(qtyInput.getAttribute('max'), 10);
                    if (!isNaN(max) && qty > max) qty = max;
                    // send update for this product
                    updates.push(fetch('/cart/update', {
                        method: 'POST',
                        headers: makeHeaders(),
                        credentials: 'same-origin',
                        body: 'productId=' + encodeURIComponent(id) + '&quantity=' + encodeURIComponent(qty)
                    }).then(r => r.json()).catch(() => ({ ok: false })));
                }
            });

            // Wait for all updates to finish, then submit form
            Promise.all(updates).then(results => {
                // Optionally check for failures in results and show a toast, but proceed to submit so user can see server error
                isSubmitting = true;
                // submit the form programmatically (this bypasses the preventDefault above because isSubmitting=true)
                checkoutForm.submit();
            }).catch(() => {
                // still submit to let server handle final checks
                isSubmitting = true;
                checkoutForm.submit();
            });
        });
    }

    // Delete selected items by calling backend remove endpoint for each
    window.deleteSelected = function () {
        updateHiddenInput();
        const ids = hiddenInput.value ? hiddenInput.value.split(',').map(s => s.trim()).filter(Boolean) : [];
        if (!ids.length) {
            showToast('No items selected');
            return;
        }

        if (!confirm('Are you sure you want to remove the selected items from your cart?')) return;

        Promise.all(ids.map(id => {
            return fetch('/cart/remove', {
                method: 'POST',
                headers: makeHeaders(),
                credentials: 'same-origin',
                body: 'productId=' + encodeURIComponent(id)
            }).then(res => {
                if (!res.ok) return Promise.resolve({ ok: false });
                return res.json().catch(() => ({ ok: false }));
            }).catch(() => ({ ok: false }));
        })).then(results => {
            // If at least one success, reload
            const anyOk = results.some(r => r && r.ok);
            if (anyOk) location.reload();
            else showToast('Failed to remove selected items');
        }).catch(() => showToast('Failed to remove selected items'));
    };

    // Apply voucher for a product (used by template onclick)
    window.applyVoucher = function (productId) {
        const input = document.getElementById('voucher-input-' + productId);
        if (!input) return;
        const code = input.value ? input.value.trim() : '';
        if (!code) {
            showToast('Please enter a voucher code');
            return;
        }

        fetch('/cart/apply-voucher', {
            method: 'POST',
            headers: makeHeaders(),
            credentials: 'same-origin',
            body: 'productId=' + encodeURIComponent(productId) + '&code=' + encodeURIComponent(code)
        }).then(res => {
            if (!res.ok) return res.text().then(t => { console.error('Apply voucher failed', res.status, t); showToast('Failed to apply voucher'); throw new Error('apply_failed'); });
            return res.json().catch(() => ({ ok: false }));
        }).then(data => {
            if (data && data.ok) {
                showToast('Voucher applied: ' + data.code);
                setTimeout(() => location.reload(), 800);
            } else {
                console.warn('Apply voucher response', data);
                showToast(data && data.error ? data.error : 'Failed to apply voucher');
            }
        }).catch(err => console.error(err));
    };

    // handle individual remove buttons
    const removeBtns = Array.from(document.querySelectorAll('.remove-btn'));
    removeBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            const pid = this.getAttribute('data-product-id');
            if (!pid) return;
            if (!confirm('Remove this item from cart?')) return;
            fetch('/cart/remove', {
                method: 'POST',
                headers: makeHeaders(),
                credentials: 'same-origin',
                body: 'productId=' + encodeURIComponent(pid)
            }).then(res => {
                if (!res.ok) {
                    return res.text().then(text => {
                        console.error('Remove failed, status:', res.status, text);
                        showToast('Failed to remove item');
                        throw new Error('remove_failed');
                    });
                }
                return res.json().catch(() => ({ ok: false }));
            }).then(data => {
                if (data && data.ok) {
                    location.reload();
                } else {
                    console.warn('Remove response:', data);
                    showToast(data && data.error ? data.error : 'Failed to remove item');
                }
            }).catch(err => {
                console.error('Remove request error', err);
            });
        });
    });

    // handle quantity updates: call POST /cart/update and reload to reflect server-side totals
    const qtyInputs = Array.from(document.querySelectorAll('.qty-input'));
    qtyInputs.forEach(input => {
        // use change event (fires when user commits new value)
        input.addEventListener('change', function () {
            const pid = this.getAttribute('data-product-id');
            let qty = parseInt(this.value, 10);
            if (!pid) return;
            if (isNaN(qty) || qty < 1) qty = 1;
            // optional max check
            const max = parseInt(this.getAttribute('max'), 10);
            if (!isNaN(max) && qty > max) qty = max;

            // disable checkout while updating
            const checkoutBtn = document.getElementById('checkout-btn');
            if (checkoutBtn) checkoutBtn.disabled = true;

            fetch('/cart/update', {
                method: 'POST',
                headers: makeHeaders(),
                credentials: 'same-origin',
                body: 'productId=' + encodeURIComponent(pid) + '&quantity=' + encodeURIComponent(qty)
            }).then(r => r.json()).then(data => {
                if (data && data.ok) {
                    // after successful update, reload page so totals and hidden input reflect new quantities
                    location.reload();
                } else {
                    // show error and set applied qty if provided
                    if (data && typeof data.appliedQty !== 'undefined') {
                        input.value = data.appliedQty;
                    }
                    showToast(data && data.error ? data.error : 'Failed to update quantity');
                }
            }).catch(() => {
                showToast('Failed to update quantity');
            }).finally(() => {
                if (checkoutBtn) checkoutBtn.disabled = false;
            });
        });
    });

    // simple toast helper
    function showToast(msg) {
        const toast = document.getElementById('toast');
        if (!toast) return;
        toast.textContent = msg;
        toast.style.display = 'block';
        setTimeout(() => { toast.style.display = 'none'; }, 2500);
    }
});
