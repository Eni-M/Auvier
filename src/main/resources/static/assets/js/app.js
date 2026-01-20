(() => {
    // Select elements
    const burger = document.querySelector(".burger");
    const menu = document.querySelector(".menu");
    const panel = document.querySelector(".menu__panel");
    const backdrop = document.querySelector(".menu__backdrop");
    const closeBtn = document.querySelector(".menu__close");

    // Only run menu logic if burger and menu actually exist on this page
    if (burger && menu) {
        const openMenu = () => {
            menu.classList.add("is-open");
            menu.setAttribute("aria-hidden", "false");
            burger.setAttribute("aria-expanded", "true");
            document.body.style.overflow = "hidden";
        };

        const closeMenu = () => {
            menu.classList.remove("is-open");
            menu.setAttribute("aria-hidden", "true");
            burger.setAttribute("aria-expanded", "false");
            document.body.style.overflow = "";
        };

        burger.addEventListener("click", () => {
            const isOpen = menu.classList.contains("is-open");
            if (isOpen) closeMenu();
            else openMenu();
        });

        if (closeBtn) closeBtn.addEventListener("click", closeMenu);
        if (backdrop) backdrop.addEventListener("click", closeMenu);

        window.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && menu.classList.contains("is-open")) closeMenu();
        });

        menu.addEventListener("click", (e) => {
            if (e.target.closest("a")) closeMenu();
        });

        if (panel) panel.addEventListener("click", (e) => e.stopPropagation());
    }

    // Modal Logic (Outside the burger check so it works on all pages)
    document.addEventListener('DOMContentLoaded', function() {
        // Inside your app.js (safely outside the burger menu check)
        document.addEventListener('click', function (e) {
            // Find the closest element with the .modal-trigger class
            const btn = e.target.closest('.modal-trigger');

            if (btn) {
                // Prevent default behavior (especially if it's an <a> tag)
                e.preventDefault();

                const id = btn.getAttribute('data-id');
                const name = btn.getAttribute('data-name');

                // Check if the fragment's function is available globally
                if (typeof openDeleteModal === "function") {
                    openDeleteModal(id, name);
                } else {
                    console.error("openDeleteModal is not defined. Is the fragment included?");
                }
            }
        });
    });


})();