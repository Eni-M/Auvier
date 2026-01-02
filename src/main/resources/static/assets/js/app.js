(() => {
    const burger = document.querySelector(".burger");
    const menu = document.querySelector(".menu");
    const panel = document.querySelector(".menu__panel");
    const backdrop = document.querySelector(".menu__backdrop");
    const closeBtn = document.querySelector(".menu__close");

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

    closeBtn.addEventListener("click", closeMenu);
    backdrop.addEventListener("click", closeMenu);

    // Close on Escape
    window.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && menu.classList.contains("is-open")) closeMenu();
    });

    // Close after clicking a link
    menu.addEventListener("click", (e) => {
        const a = e.target.closest("a");
        if (!a) return;
        closeMenu();
    });

    // Prevent backdrop clicks when clicking inside panel
    panel.addEventListener("click", (e) => e.stopPropagation());
})();
