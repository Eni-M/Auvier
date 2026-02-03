/**
 * ==========================================================================
 * AUVIER - Admin Panel JavaScript
 * Handles admin interactions: modals, alerts, tables, etc.
 * ==========================================================================
 */

(function() {
    'use strict';

    // ========================================================================
    // DOM Ready
    // ========================================================================
    document.addEventListener('DOMContentLoaded', function() {
        initModalTriggers();
        initAlertDismiss();
        initTableRowActions();
        initFormValidation();
        highlightCurrentNav();
    });

    // ========================================================================
    // Modal System
    // ========================================================================
    function initModalTriggers() {
        // Delegate clicks for modal triggers
        document.addEventListener('click', function(e) {
            const btn = e.target.closest('.modal-trigger');

            if (btn) {
                e.preventDefault();

                const id = btn.getAttribute('data-id');
                const name = btn.getAttribute('data-name');

                if (typeof openDeleteModal === 'function') {
                    openDeleteModal(id, name);
                } else {
                    console.error('openDeleteModal is not defined. Is the fragment included?');
                }
            }
        });
    }

    // Global function to open delete confirmation modal
    window.openDeleteModal = function(id, name) {
        const modal = document.getElementById('deleteModal');
        if (!modal) return;

        // Update modal content
        const nameEl = modal.querySelector('[data-modal-name]');
        const idInput = modal.querySelector('[data-modal-id]');

        if (nameEl) nameEl.textContent = name || 'this item';
        if (idInput) idInput.value = id;

        // Show modal
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    };

    // Global function to close modal
    window.closeModal = function(modalId) {
        const modal = document.getElementById(modalId || 'deleteModal');
        if (!modal) return;

        modal.style.display = 'none';
        document.body.style.overflow = '';
    };

    // Close modal on backdrop click
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('v-modal')) {
            closeModal(e.target.id);
        }
    });

    // Close modal on Escape key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            const openModal = document.querySelector('.v-modal[style*="flex"]');
            if (openModal) {
                closeModal(openModal.id);
            }
        }
    });

    // ========================================================================
    // Alert Dismiss
    // ========================================================================
    function initAlertDismiss() {
        document.querySelectorAll('.v-alert__close').forEach(function(btn) {
            btn.addEventListener('click', function() {
                const alert = this.closest('.v-alert');
                if (alert) {
                    alert.style.opacity = '0';
                    alert.style.transform = 'translateY(-10px)';
                    setTimeout(function() {
                        alert.remove();
                    }, 200);
                }
            });
        });

        // Auto-dismiss success alerts after 5 seconds
        document.querySelectorAll('.v-alert--success').forEach(function(alert) {
            setTimeout(function() {
                const closeBtn = alert.querySelector('.v-alert__close');
                if (closeBtn) closeBtn.click();
            }, 5000);
        });
    }

    // ========================================================================
    // Table Row Actions
    // ========================================================================
    function initTableRowActions() {
        // Make entire table row clickable for view action
        document.querySelectorAll('.v-table tbody tr[data-href]').forEach(function(row) {
            row.style.cursor = 'pointer';
            row.addEventListener('click', function(e) {
                // Don't navigate if clicking on an action button
                if (e.target.closest('.v-btngroup, .v-btn, a, button')) return;

                const href = this.getAttribute('data-href');
                if (href) window.location.href = href;
            });
        });
    }

    // ========================================================================
    // Form Validation
    // ========================================================================
    function initFormValidation() {
        document.querySelectorAll('form.v-admin-form').forEach(function(form) {
            form.addEventListener('submit', function(e) {
                const requiredFields = form.querySelectorAll('[required]');
                let isValid = true;

                requiredFields.forEach(function(field) {
                    // Remove previous error states
                    field.classList.remove('is-invalid');
                    const errorEl = field.parentNode.querySelector('.v-error');
                    if (errorEl) errorEl.remove();

                    // Check validity
                    if (!field.value.trim()) {
                        isValid = false;
                        field.classList.add('is-invalid');

                        // Add error message
                        const error = document.createElement('span');
                        error.className = 'v-error';
                        error.textContent = 'This field is required';
                        field.parentNode.appendChild(error);
                    }
                });

                if (!isValid) {
                    e.preventDefault();

                    // Scroll to first error
                    const firstError = form.querySelector('.is-invalid');
                    if (firstError) {
                        firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        firstError.focus();
                    }
                }
            });
        });
    }

    // ========================================================================
    // Highlight Current Nav
    // ========================================================================
    function highlightCurrentNav() {
        const currentPath = window.location.pathname;

        document.querySelectorAll('.v-topnav__link').forEach(function(link) {
            const href = link.getAttribute('href');

            if (href === currentPath ||
                (href !== '/admin' && currentPath.startsWith(href))) {
                link.setAttribute('aria-current', 'page');
            }
        });
    }

    // ========================================================================
    // Utility Functions
    // ========================================================================

    // Format currency
    window.formatCurrency = function(amount, currency) {
        currency = currency || 'USD';
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: currency
        }).format(amount);
    };

    // Format date
    window.formatDate = function(dateString) {
        const date = new Date(dateString);
        return new Intl.DateTimeFormat('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        }).format(date);
    };

    // Confirm action
    window.confirmAction = function(message, callback) {
        if (confirm(message)) {
            callback();
        }
    };

    // Copy to clipboard
    window.copyToClipboard = function(text) {
        navigator.clipboard.writeText(text).then(function() {
            // Show toast notification
            showToast('Copied to clipboard');
        }).catch(function(err) {
            console.error('Failed to copy:', err);
        });
    };

    // Toast notification
    window.showToast = function(message, type) {
        type = type || 'info';

        const toast = document.createElement('div');
        toast.className = 'v-toast v-toast--' + type;
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            bottom: 20px;
            right: 20px;
            padding: 12px 20px;
            background: rgba(255,255,255,0.95);
            color: #0b0b0b;
            border-radius: 10px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.2);
            z-index: 10000;
            animation: slideIn 0.3s ease;
        `;

        document.body.appendChild(toast);

        setTimeout(function() {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            setTimeout(function() {
                toast.remove();
            }, 200);
        }, 3000);
    };

    // ========================================================================
    // Admin Profile Dropdown
    // ========================================================================
    let activityLoaded = false;

    window.toggleAdminProfile = function() {
        const profile = document.querySelector('.v-admin-profile');
        if (profile) {
            profile.classList.toggle('active');
        }
    };

    window.toggleActivityLog = function() {
        const panel = document.getElementById('activityLogPanel');
        const header = document.querySelector('.v-admin-profile__section-header');
        const chevron = document.getElementById('activityChevron');

        if (panel) {
            const isHidden = panel.style.display === 'none';
            panel.style.display = isHidden ? 'block' : 'none';

            if (header) {
                header.classList.toggle('expanded', isHidden);
            }

            // Load activity on first expand
            if (isHidden && !activityLoaded) {
                loadMyActivity();
            }
        }
    };

    function loadMyActivity() {
        const loading = document.getElementById('activityLoading');
        const list = document.getElementById('activityList');

        fetch('/api/admin/my-activity')
            .then(response => response.json())
            .then(data => {
                if (loading) loading.style.display = 'none';

                if (data && data.length > 0) {
                    let html = '';
                    data.forEach(item => {
                        const iconClass = item.action.toLowerCase();
                        const icon = item.action === 'CREATE' ? 'bi-plus-lg' :
                                     item.action === 'UPDATE' ? 'bi-pencil-fill' : 'bi-trash-fill';
                        html += `
                            <div class="v-admin-profile__activity-item">
                                <div class="v-admin-profile__activity-icon ${iconClass}">
                                    <i class="bi ${icon}"></i>
                                </div>
                                <div class="v-admin-profile__activity-content">
                                    <div class="v-admin-profile__activity-text">
                                        ${item.description || (item.action + ' ' + item.entityType + ': ' + (item.entityName || 'N/A'))}
                                    </div>
                                    <div class="v-admin-profile__activity-time">${item.timeAgo}</div>
                                </div>
                            </div>
                        `;
                    });
                    if (list) list.innerHTML = html;
                } else {
                    if (list) list.innerHTML = '<div class="v-admin-profile__activity-empty">No recent activity</div>';
                }
                activityLoaded = true;
            })
            .catch(err => {
                console.error('Failed to load activity:', err);
                if (loading) loading.style.display = 'none';
                if (list) list.innerHTML = '<div class="v-admin-profile__activity-empty">Failed to load activity</div>';
            });
    }

    // Close dropdown when clicking outside
    document.addEventListener('click', function(e) {
        const profile = document.querySelector('.v-admin-profile');
        if (profile && !profile.contains(e.target)) {
            profile.classList.remove('active');
        }
    });

})();
