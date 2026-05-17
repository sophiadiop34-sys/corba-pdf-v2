/* ================================================================
   CORBA PDF Service - JavaScript Utilitaires Partagés
   ================================================================ */

// ── Loader ────────────────────────────────────────────────────
function showLoader(msg) {
    let overlay = document.getElementById('loaderOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'loaderOverlay';
        overlay.innerHTML = `
            <div class="loader-box">
                <div class="spinner-corba"></div>
                <h5 class="mb-1" id="loaderMsg">Traitement CORBA en cours...</h5>
                <small class="text-muted">PDFBox est en train de travailler</small>
            </div>`;
        document.body.appendChild(overlay);
    }
    if (msg) {
        const m = document.getElementById('loaderMsg');
        if (m) m.textContent = msg;
    }
    overlay.classList.add('active');
}

function hideLoader() {
    const overlay = document.getElementById('loaderOverlay');
    if (overlay) overlay.classList.remove('active');
}

// ── Alerts ────────────────────────────────────────────────────
function showAlert(message, type = 'info') {
    const zone = document.getElementById('alertZone');
    if (!zone) return;
    const icons = {
        success: 'check-circle', danger: 'exclamation-circle',
        warning: 'exclamation-triangle', info: 'info-circle'
    };
    zone.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show d-flex align-items-center gap-2" role="alert">
            <i class="fas fa-${icons[type] || 'info-circle'}"></i>
            <span>${message}</span>
            <button type="button" class="btn-close ms-auto" data-bs-dismiss="alert"></button>
        </div>`;
    zone.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    // Auto-dismiss after 6s for success
    if (type === 'success') {
        setTimeout(() => {
            const a = zone.querySelector('.alert');
            if (a) a.classList.remove('show');
        }, 6000);
    }
}

// ── File Display ──────────────────────────────────────────────
function showSelectedFile(input, targetId) {
    const target = document.getElementById(targetId);
    if (!target || !input.files || input.files.length === 0) return;
    const file = input.files[0];
    target.innerHTML = `
        <div class="file-item">
            <i class="fas fa-file-pdf"></i>
            <span class="fw-semibold">${file.name}</span>
            <span class="badge bg-secondary ms-auto">${formatBytes(file.size)}</span>
        </div>`;
}

function handleFileSelect(input, listId) {
    const list = document.getElementById(listId);
    if (!list || !input.files || input.files.length === 0) return;
    list.innerHTML = '';
    Array.from(input.files).forEach((f, i) => {
        list.innerHTML += `
            <div class="file-item">
                <i class="fas fa-file-pdf"></i>
                <span>${i + 1}. ${f.name}</span>
                <span class="badge bg-secondary ms-auto">${formatBytes(f.size)}</span>
            </div>`;
    });
}

// ── General API Call ──────────────────────────────────────────
function callApi(url, formData, loaderMsg) {
    showLoader(loaderMsg || 'Traitement en cours...');
    fetch(url, { method: 'POST', body: formData })
        .then(r => r.json())
        .then(data => {
            hideLoader();
            if (data.success) {
                showAlert(data.message, 'success');
                renderResult(data.data, data.message);
            } else {
                showAlert(data.message, 'danger');
            }
        })
        .catch(err => {
            hideLoader();
            showAlert('Erreur réseau : ' + err.message, 'danger');
        });
}

// ── Render Result ─────────────────────────────────────────────
function renderResult(data, message) {
    const zone    = document.getElementById('resultZone');
    const content = document.getElementById('resultContent');
    if (!zone || !content) return;

    zone.classList.remove('d-none');

    // Array = multiple files (split)
    if (Array.isArray(data)) {
        let html = `<p class="fw-semibold mb-3">${message}</p>
                    <div class="row g-2">`;
        data.forEach((item, i) => {
            html += `
                <div class="col-md-4">
                    <div class="card text-center p-3 h-100">
                        <i class="fas fa-file-pdf fa-2x text-danger mb-2"></i>
                        <div class="small fw-semibold">${item.fileName}</div>
                        <div class="small text-muted">${formatBytes(item.sizeBytes)}</div>
                        <a href="${item.downloadUrl}" class="btn btn-sm btn-outline-primary mt-2" download="${item.fileName}">
                            <i class="fas fa-download me-1"></i>Télécharger
                        </a>
                    </div>
                </div>`;
        });
        html += '</div>';
        content.innerHTML = html;
    } else {
        // Single file
        content.innerHTML = `
            <div class="d-flex align-items-center gap-3">
                <i class="fas fa-file-pdf fa-3x text-danger"></i>
                <div>
                    <div class="fw-bold">${data.fileName}</div>
                    <div class="text-muted small">${data.message}</div>
                </div>
                <a href="${data.downloadUrl}" class="btn btn-success ms-auto px-4" download="${data.fileName}">
                    <i class="fas fa-download me-2"></i>Télécharger
                </a>
            </div>`;
    }

    zone.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// ── Drag & Drop ───────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.drop-zone').forEach(zone => {
        zone.addEventListener('dragover', e => {
            e.preventDefault();
            zone.classList.add('drag-over');
        });
        zone.addEventListener('dragleave', () => {
            zone.classList.remove('drag-over');
        });
        zone.addEventListener('drop', e => {
            e.preventDefault();
            zone.classList.remove('drag-over');
            const input = zone.querySelector('input[type="file"]');
            if (input) {
                input.files = e.dataTransfer.files;
                input.dispatchEvent(new Event('change'));
            }
        });
    });
});

// ── Utilities ─────────────────────────────────────────────────
function formatBytes(bytes) {
    if (!bytes) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
}
