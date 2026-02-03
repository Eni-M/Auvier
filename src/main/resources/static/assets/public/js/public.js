/**
 * ==========================================================================
 * AUVIER - Public Store JavaScript
 * Handles store interactions: navigation, search, mobile menu, etc.
 * ==========================================================================
 */

(function() {
    'use strict';

    // ========================================================================
    // DOM Ready
    // ========================================================================
    document.addEventListener('DOMContentLoaded', function() {
        initMobileMenu();
        initSearch();
        initSidebarToggles();
        initColorSwatches();
        initViewToggle();
        initSortSelect();
        initQuantitySelectors();
        initSmoothScroll();
        initCartCount();
        initAccountDropdown();
    });

    // ========================================================================
    // Cart Count
    // ========================================================================
    function initCartCount() {
        updateCartCount();
    }

    function updateCartCount() {
        const cartCountEl = document.getElementById('cartCount');
        if (!cartCountEl) return;

        const cart = JSON.parse(localStorage.getItem('auvier_cart') || '[]');
        const count = cart.reduce(function(sum, item) {
            return sum + (item.quantity || 1);
        }, 0);

        if (count > 0) {
            cartCountEl.textContent = count > 99 ? '99+' : count;
            cartCountEl.style.display = 'flex';
        } else {
            cartCountEl.style.display = 'none';
        }
    }

    // Make globally accessible
    window.updateCartCount = updateCartCount;

    // ========================================================================
    // Account Dropdown
    // ========================================================================
    function initAccountDropdown() {
        const dropdown = document.getElementById('accountDropdown');
        if (!dropdown) return;

        // Close dropdown when clicking outside
        document.addEventListener('click', function(e) {
            if (!e.target.closest('.au-nav__dropdown')) {
                dropdown.classList.remove('is-open');
            }
        });

        // Close on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                dropdown.classList.remove('is-open');
            }
        });
    }

    window.toggleAccountMenu = function() {
        const dropdown = document.getElementById('accountDropdown');
        if (dropdown) {
            dropdown.classList.toggle('is-open');
        }
    };

    // ========================================================================
    // Mobile Menu
    // ========================================================================
    function initMobileMenu() {
        const toggle = document.querySelector('.au-nav__toggle');
        const mobileMenu = document.getElementById('mobileMenu');

        if (!toggle || !mobileMenu) return;

        toggle.addEventListener('click', function() {
            const isOpen = mobileMenu.classList.contains('is-open');

            if (isOpen) {
                closeMobileMenu();
            } else {
                openMobileMenu();
            }
        });

        // Close on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && mobileMenu.classList.contains('is-open')) {
                closeMobileMenu();
            }
        });

        // Close when clicking a link
        mobileMenu.querySelectorAll('a').forEach(function(link) {
            link.addEventListener('click', closeMobileMenu);
        });
    }

    function openMobileMenu() {
        const mobileMenu = document.getElementById('mobileMenu');
        const toggle = document.querySelector('.au-nav__toggle');

        if (!mobileMenu) return;

        mobileMenu.classList.add('is-open');
        document.body.style.overflow = 'hidden';
        toggle?.setAttribute('aria-expanded', 'true');

        // Animate burger to X
        const spans = toggle?.querySelectorAll('span');
        if (spans && spans.length >= 2) {
            spans[0].style.transform = 'rotate(45deg) translate(4px, 4px)';
            spans[1].style.transform = 'rotate(-45deg) translate(0px, 0px)';
        }
    }

    function closeMobileMenu() {
        const mobileMenu = document.getElementById('mobileMenu');
        const toggle = document.querySelector('.au-nav__toggle');

        if (!mobileMenu) return;

        mobileMenu.classList.remove('is-open');
        document.body.style.overflow = '';
        toggle?.setAttribute('aria-expanded', 'false');

        // Reset burger
        const spans = toggle?.querySelectorAll('span');
        if (spans) {
            spans.forEach(function(span) {
                span.style.transform = '';
            });
        }
    }

    // Make functions globally accessible
    window.toggleMobileMenu = function() {
        const mobileMenu = document.getElementById('mobileMenu');
        if (mobileMenu?.classList.contains('is-open')) {
            closeMobileMenu();
        } else {
            openMobileMenu();
        }
    };

    // ========================================================================
    // Search
    // ========================================================================
    function initSearch() {
        const searchOverlay = document.getElementById('searchOverlay');
        const searchInput = searchOverlay?.querySelector('.au-search__input');

        if (!searchOverlay) return;

        // Focus input when search opens
        if (searchInput) {
            const observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    if (mutation.target.classList.contains('is-open')) {
                        setTimeout(function() {
                            searchInput.focus();
                        }, 100);
                    }
                });
            });

            observer.observe(searchOverlay, {
                attributes: true,
                attributeFilter: ['class']
            });
        }

        // Close on escape
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && searchOverlay.classList.contains('is-open')) {
                closeSearch();
            }
        });
    }

    function openSearch() {
        const searchOverlay = document.getElementById('searchOverlay');
        if (!searchOverlay) return;

        searchOverlay.classList.add('is-open');
    }

    function closeSearch() {
        const searchOverlay = document.getElementById('searchOverlay');
        if (!searchOverlay) return;

        searchOverlay.classList.remove('is-open');
    }

    // Make functions globally accessible
    window.toggleSearch = function() {
        const searchOverlay = document.getElementById('searchOverlay');
        if (searchOverlay?.classList.contains('is-open')) {
            closeSearch();
        } else {
            openSearch();
        }
    };

    // ========================================================================
    // Sidebar Filter Toggles
    // ========================================================================
    function initSidebarToggles() {
        const titles = document.querySelectorAll('.au-sidebar__title');

        titles.forEach(function(title) {
            title.addEventListener('click', function() {
                this.classList.toggle('is-open');

                // Find the next sibling (the list)
                const list = this.nextElementSibling;
                if (list) {
                    if (this.classList.contains('is-open')) {
                        list.style.display = '';
                    } else {
                        list.style.display = 'none';
                    }
                }
            });
        });
    }

    // ========================================================================
    // Color Swatches
    // ========================================================================
    function initColorSwatches() {
        const swatches = document.querySelectorAll('.au-color-swatch');

        swatches.forEach(function(swatch) {
            swatch.addEventListener('click', function() {
                // Toggle active state
                this.classList.toggle('is-active');

                // Update hidden input or trigger filter
                const color = this.dataset.color;
                if (color) {
                    updateColorFilter(color, this.classList.contains('is-active'));
                }
            });
        });
    }

    function updateColorFilter(color, isActive) {
        // This can be extended to work with form submission or AJAX filtering
        console.log('Color filter:', color, isActive ? 'added' : 'removed');
    }

    // ========================================================================
    // View Toggle (Grid/List)
    // ========================================================================
    function initViewToggle() {
        const toggles = document.querySelectorAll('.au-view-toggle button');
        const productsContainer = document.querySelector('.au-products');

        if (!toggles.length || !productsContainer) return;

        toggles.forEach(function(toggle) {
            toggle.addEventListener('click', function() {
                // Update active state
                toggles.forEach(function(t) {
                    t.classList.remove('is-active');
                });
                this.classList.add('is-active');

                // Update view
                const view = this.dataset.view;
                if (view === 'list') {
                    productsContainer.classList.add('au-products--list');
                } else {
                    productsContainer.classList.remove('au-products--list');
                }
            });
        });
    }

    // ========================================================================
    // Sort Select
    // ========================================================================
    function initSortSelect() {
        // Function for sort select change
        window.updateSort = function(value) {
            const url = new URL(window.location.href);
            url.searchParams.set('sort', value);
            window.location.href = url.toString();
        };
    }

    // ========================================================================
    // Quantity Selectors
    // ========================================================================
    function initQuantitySelectors() {
        const selectors = document.querySelectorAll('.au-quantity');

        selectors.forEach(function(selector) {
            const minus = selector.querySelector('[data-action="minus"]');
            const plus = selector.querySelector('[data-action="plus"]');
            const input = selector.querySelector('input');

            if (!input) return;

            minus?.addEventListener('click', function() {
                const currentValue = parseInt(input.value) || 1;
                if (currentValue > 1) {
                    input.value = currentValue - 1;
                    input.dispatchEvent(new Event('change'));
                }
            });

            plus?.addEventListener('click', function() {
                const currentValue = parseInt(input.value) || 1;
                const max = parseInt(input.max) || 99;
                if (currentValue < max) {
                    input.value = currentValue + 1;
                    input.dispatchEvent(new Event('change'));
                }
            });
        });
    }

    // ========================================================================
    // Smooth Scroll for Anchor Links
    // ========================================================================
    function initSmoothScroll() {
        document.querySelectorAll('a[href^="#"]').forEach(function(anchor) {
            anchor.addEventListener('click', function(e) {
                const href = this.getAttribute('href');
                if (href === '#') return;

                const target = document.querySelector(href);
                if (target) {
                    e.preventDefault();
                    target.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            });
        });
    }

    // ========================================================================
    // Quick Add to Cart (placeholder for AJAX functionality)
    // ========================================================================
    window.quickAddToCart = function(productId) {
        console.log('Quick add to cart:', productId);

        // Show loading state
        // Make AJAX request
        // Update cart count
        // Show confirmation

        // For now, just show an alert
        alert('Product added to cart! (Demo)');
    };

    // ========================================================================
    // Newsletter Form
    // ========================================================================
    document.querySelectorAll('.au-newsletter-form').forEach(function(form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();

            const email = this.querySelector('input[type="email"]')?.value;
            if (!email) return;

            // Here you would typically send this to your backend
            console.log('Newsletter signup:', email);
            alert('Thank you for subscribing!');
            this.reset();
        });
    });

})();
