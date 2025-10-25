function hideElement(element) {
    if (element) element.classList.add('hidden');
}

function showElement(element) {
    if (element) element.classList.remove('hidden');
}

function createPagination(currentPage, totalPages, onPageChange, containerSelector) {
    const container = document.querySelector(containerSelector);
    if (!container || totalPages <= 1) {
        hideElement(container);
        return;
    }

    showElement(container);

    const prevBtn = container.querySelector('[id$="prev-btn"]');
    const nextBtn = container.querySelector('[id$="next-btn"]');
    const pageNumbers = container.querySelector('[id$="page-numbers"]');

    if (prevBtn) {
        prevBtn.disabled = currentPage === 1;
        prevBtn.onclick = () => {
            if (currentPage > 1) {
                onPageChange(currentPage - 1);
            }
        };
    }

    if (nextBtn) {
        nextBtn.disabled = currentPage === totalPages;
        nextBtn.onclick = () => {
            if (currentPage < totalPages) {
                onPageChange(currentPage + 1);
            }
        };
    }

    if (pageNumbers) {
        pageNumbers.innerHTML = generatePageNumbers(currentPage, totalPages, onPageChange);

         pageNumbers.querySelectorAll('.page-number').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const page = parseInt(e.target.dataset.page);
                onPageChange(page);
            });
        });
    }
}

function generatePageNumbers(currentPage, totalPages, onPageChange) {
    const maxVisiblePages = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

    if (endPage - startPage + 1 < maxVisiblePages) {
        startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    let html = '';

    if (startPage > 1) {
        html += `<a href="#" class="page-number" data-page="1">1</a>`;
        if (startPage > 2) {
            html += `<span class="page-ellipsis">...</span>`;
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        const isActive = i === currentPage ? 'active' : '';
        html += `<a href="#" class="page-number ${isActive}" data-page="${i}">${i}</a>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) {
            html += `<span class="page-ellipsis">...</span>`;
        }
        html += `<a href="#" class="page-number" data-page="${totalPages}">${totalPages}</a>`;
    }
    
    return html; 
}