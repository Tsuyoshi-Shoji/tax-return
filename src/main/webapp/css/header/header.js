(() => {
    "use strict";

    const menuButton = document.getElementById("hamburgerBtn");
    const navigation = document.getElementById("menuPanel");
    const mobile = window.matchMedia("(max-width: 760px)");

    // Create backdrop overlay for mobile
    const createBackdrop = () => {
        let backdrop = document.getElementById("sidebarBackdrop");
        if (!backdrop) {
            backdrop = document.createElement("div");
            backdrop.id = "sidebarBackdrop";
            backdrop.className = "sidebar-backdrop";
            document.body.appendChild(backdrop);
        }
        return backdrop;
    };

    if (menuButton && navigation) {
        const backdrop = createBackdrop();

        let closeTimer = null;
        const finishClose = () => {
            clearTimeout(closeTimer);
            closeTimer = null;
            navigation.classList.remove("is-closing");
            backdrop.classList.remove("is-closing", "is-visible");
        };

        const setNavigation = (open, animate = false) => {
            const shouldShow = !mobile.matches || open;
            menuButton.setAttribute("aria-expanded", String(shouldShow));
            menuButton.setAttribute("aria-label", shouldShow ? "メニューを閉じる" : "メニューを開く");

            if (shouldShow) {
                finishClose();
                navigation.hidden = false;
                if (mobile.matches) backdrop.classList.add("is-visible");
                return;
            }
            if (animate && !navigation.hidden) {
                // スライドアップ完了後に非表示にする
                navigation.classList.add("is-closing");
                backdrop.classList.add("is-closing");
                closeTimer = setTimeout(() => {
                    navigation.hidden = true;
                    finishClose();
                }, 200);
                return;
            }
            navigation.hidden = true;
            finishClose();
        };

        const updateViewport = () => {
            const navigationHadFocus = navigation.contains(document.activeElement);
            const buttonHadFocus = document.activeElement === menuButton;
            menuButton.hidden = !mobile.matches;
            setNavigation(!mobile.matches);
            if (mobile.matches && navigationHadFocus) menuButton.focus();
            if (!mobile.matches && buttonHadFocus) navigation.querySelector("a").focus();
        };

        const isOpen = () => !navigation.hidden && !navigation.classList.contains("is-closing");
        menuButton.addEventListener("click", () => setNavigation(!isOpen(), true));
        menuButton.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && mobile.matches) setNavigation(false, true);
        });
        navigation.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && mobile.matches) {
                setNavigation(false, true);
                menuButton.focus();
            }
        });
        
        // Close menu when backdrop is clicked
        backdrop.addEventListener("click", () => {
            if (mobile.matches && isOpen()) {
                setNavigation(false, true);
            }
        });

        mobile.addEventListener("change", updateViewport);
        updateViewport();
    }

    const dropdowns = [...document.querySelectorAll(".header-dropdown")];
    dropdowns.forEach((dropdown) => {
        dropdown.addEventListener("toggle", () => {
            if (dropdown.open) dropdowns.forEach((other) => {
                if (other !== dropdown) other.open = false;
            });
        });
        dropdown.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                dropdown.open = false;
                dropdown.querySelector("summary").focus();
            }
        });
        dropdown.addEventListener("focusout", (event) => {
            if (!dropdown.contains(event.relatedTarget)) dropdown.open = false;
        });
    });
    document.addEventListener("click", (event) => {
        dropdowns.forEach((dropdown) => {
            if (!dropdown.contains(event.target)) dropdown.open = false;
        });
    });
})();
