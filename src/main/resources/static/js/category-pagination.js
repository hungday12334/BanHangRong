// Pagination functions for category management (Review Style)

// ===== PAGINATION FUNCTIONS (REVIEW STYLE) =====

// Initialize pagination
function initPagination() {
    updatePagination();
}

// Get all visible rows (after filtering)
function getVisibleRows() {
    const rows = Array.from(document.querySelectorAll('.category-row'));
    return rows.filter(row => row.style.display !== 'none');
}

// Update pagination based on visible rows (Review Style)
function updatePagination() {
    const visibleRows = getVisibleRows();
    const totalItems = visibleRows.length;

    // Calculate total pages
    totalPages = Math.ceil(totalItems / pageSize);

    // Reset to page 1 if current page is out of bounds
    if (currentPage > totalPages && totalPages > 0) {
        currentPage = totalPages;
    }
    if (currentPage < 1) {
        currentPage = 1;
    }

    // Show/hide rows based on current page
    visibleRows.forEach((row, index) => {
        const startIndex = (currentPage - 1) * pageSize;
        const endIndex = startIndex + pageSize;

        if (index >= startIndex && index < endIndex) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });

    // Update pagination UI
    updatePaginationUI(totalItems);

    // Update select all state
    if (typeof updateSelectAllState === 'function') {
        updateSelectAllState();
    }
}

// Update pagination UI (Review Style)
function updatePaginationUI(totalItems) {
    const paginationWrapper = document.getElementById('paginationWrapper');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const pageNumbers = document.getElementById('pageNumbers');
    const currentPageNum = document.getElementById('currentPageNum');
    const totalPagesNum = document.getElementById('totalPagesNum');
    const totalItemsNum = document.getElementById('totalItemsNum');

    if (!paginationWrapper) return;

    // Hide pagination if only 1 page or no items
    if (totalPages <= 1 || totalItems === 0) {
        paginationWrapper.style.display = 'none';
        return;
    } else {
        paginationWrapper.style.display = 'flex';
    }

    // Update page info
    if (currentPageNum) currentPageNum.textContent = currentPage;
    if (totalPagesNum) totalPagesNum.textContent = totalPages;
    if (totalItemsNum) totalItemsNum.textContent = totalItems;

    // Update prev/next buttons
    if (prevBtn) {
        if (currentPage === 1) {
            prevBtn.classList.add('disabled');
        } else {
            prevBtn.classList.remove('disabled');
        }
    }

    if (nextBtn) {
        if (currentPage === totalPages) {
            nextBtn.classList.add('disabled');
        } else {
            nextBtn.classList.remove('disabled');
        }
    }

    // Render page numbers (Review Style - EXACTLY like reviews.html)
    if (pageNumbers) {
        pageNumbers.innerHTML = '';

        for (let i = 1; i <= totalPages; i++) {
            // Show: current page, pages near current (±2), first page, last page
            // Matches: i == currentPage || (i >= currentPage - 2 && i <= currentPage + 2) || i == 1 || i == totalPages
            const showPage = i === currentPage ||
                            (i >= currentPage - 2 && i <= currentPage + 2) ||
                            i === 1 ||
                            i === totalPages;

            if (showPage) {
                const pageBtn = document.createElement('a');
                pageBtn.className = 'pagination-btn page-num-btn';
                pageBtn.textContent = i;
                pageBtn.setAttribute('data-page', i - 1); // For consistency with reviews (0-based)
                pageBtn.onclick = function() { goToPage(i); };

                if (i === currentPage) {
                    pageBtn.classList.add('active');
                }

                pageNumbers.appendChild(pageBtn);
            }

            // Add ellipsis EXACTLY like reviews: at position i == 2 when currentPage > 4
            // Reviews code: th:if="${i == 1 && reviewsPage.number > 3}"
            // reviewsPage.number is 0-based, so reviewsPage.number > 3 means currentPage (1-based) > 4
            if (i === 2 && currentPage > 4) {
                const ellipsis = document.createElement('span');
                ellipsis.style.cssText = 'padding: 8px; color: var(--muted);';
                ellipsis.textContent = '...';
                pageNumbers.appendChild(ellipsis);
            }

            // Add ellipsis EXACTLY like reviews: at position i == totalPages - 1 when currentPage < totalPages - 3
            // Reviews code: th:if="${i == reviewsPage.totalPages - 2 && reviewsPage.number < reviewsPage.totalPages - 4}"
            if (i === totalPages - 1 && currentPage < totalPages - 3) {
                const ellipsis = document.createElement('span');
                ellipsis.style.cssText = 'padding: 8px; color: var(--muted);';
                ellipsis.textContent = '...';
                pageNumbers.appendChild(ellipsis);
            }
        }
    }
}

// Go to specific page
function goToPage(page) {
    if (page < 1 || page > totalPages) return;
    if (page === currentPage) return;

    currentPage = page;
    updatePagination();

    // Scroll to top of table
    const categoryList = document.querySelector('.category-list-section');
    if (categoryList) {
        categoryList.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

// Change page size
function changePageSize() {
    const select = document.getElementById('pageSizeSelect');
    pageSize = parseInt(select.value);
    currentPage = 1; // Reset to first page
    updatePagination();
}

// Call initPagination after DOM is ready
window.addEventListener('load', function() {
    initPagination();
});

