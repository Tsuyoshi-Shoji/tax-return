(function () {
    const form = document.getElementById("profit-loss-search-form");
    if (!form) {
        return;
    }

    const incomePage = document.getElementById("income-page");
    const expensePage = document.getElementById("expense-page");
    const clearButton = document.getElementById("button-clear-conditions");
    const errorPanel = document.getElementById("search-error-panel");
    const types = Array.from(form.querySelectorAll('input[name="types"]'));

    const itemToggle = document.getElementById("button-items-toggle");
    const itemDropdown = document.getElementById("items-dropdown");
    const itemSearch = document.getElementById("items-search");
    const itemRows = Array.from(document.querySelectorAll(".items-option-row"));
    const hiddenItemsContainer = document.getElementById("items-hidden-container");
    const selectedLabel = document.getElementById("items-selected-label");

    function parseDate(input) {
        if (!input || !/^\d{4}-\d{2}-\d{2}$/.test(input)) {
            return null;
        }
        const date = new Date(`${input}T00:00:00`);
        if (Number.isNaN(date.getTime())) {
            return null;
        }
        const [year, month, day] = input.split("-").map(Number);
        if (date.getFullYear() !== year || date.getMonth() + 1 !== month || date.getDate() !== day) {
            return null;
        }
        return date;
    }

    function parseAmount(input) {
        if (!input) {
            return null;
        }
        if (!/^\d+$/.test(input)) {
            return null;
        }
        return Number.parseInt(input, 10);
    }

    function selectedTypes() {
        const selected = new Set(types.filter((input) => input.checked).map((input) => input.value));
        if (selected.size === 0) {
            selected.add("income");
            selected.add("expense");
        }
        return selected;
    }

    function selectedItemValues() {
        return Array.from(hiddenItemsContainer.querySelectorAll('input[name="items"]')).map((input) => input.value);
    }

    function setHiddenSelectedItems(values) {
        hiddenItemsContainer.innerHTML = "";
        values.forEach((value) => {
            const hidden = document.createElement("input");
            hidden.type = "hidden";
            hidden.name = "items";
            hidden.value = value;
            hiddenItemsContainer.appendChild(hidden);
        });
    }

    function updateSelectedLabel() {
        const values = selectedItemValues();
        if (values.length === 0) {
            selectedLabel.textContent = "すべて";
            return;
        }
        const labels = values.slice(0, 2).map((value) => {
            const row = itemRows.find((candidate) => candidate.dataset.value === value);
            return row ? row.querySelector("span")?.textContent || value : value;
        });
        selectedLabel.textContent = values.length > 2 ? `${labels.join("、")} ほか${values.length - 2}件` : labels.join("、");
    }

    function syncCheckboxesFromHidden() {
        const selected = new Set(selectedItemValues());
        itemRows.forEach((row) => {
            const checkbox = row.querySelector(".items-option-checkbox");
            checkbox.checked = selected.has(row.dataset.value);
        });
    }

    function renderItemRows() {
        const activeTypes = selectedTypes();
        const keyword = (itemSearch?.value || "").trim().toLowerCase();
        const selected = new Set(selectedItemValues());
        const nextSelected = [];
        itemRows.forEach((row) => {
            const type = row.dataset.group;
            const label = (row.querySelector("span")?.textContent || "").toLowerCase();
            const inType = activeTypes.has(type);
            const matchesSearch = !keyword || label.includes(keyword);
            row.hidden = !(inType && matchesSearch);
            if (!inType) {
                const checkbox = row.querySelector(".items-option-checkbox");
                checkbox.checked = false;
                selected.delete(row.dataset.value);
            }
        });
        itemRows.forEach((row) => {
            const checkbox = row.querySelector(".items-option-checkbox");
            if (checkbox.checked) {
                nextSelected.push(row.dataset.value);
            }
        });
        setHiddenSelectedItems(nextSelected);
        updateSelectedLabel();
    }

    function openDropdown() {
        if (!itemDropdown || !itemToggle) {
            return;
        }
        itemDropdown.hidden = false;
        itemToggle.setAttribute("aria-expanded", "true");
        if (itemSearch) {
            itemSearch.focus();
        }
    }

    function closeDropdown() {
        if (!itemDropdown || !itemToggle) {
            return;
        }
        itemDropdown.hidden = true;
        itemToggle.setAttribute("aria-expanded", "false");
    }

    function toggleDropdown() {
        if (!itemDropdown) {
            return;
        }
        if (itemDropdown.hidden) {
            openDropdown();
        } else {
            closeDropdown();
        }
    }

    function renderErrors(errors) {
        if (!errors.length) {
            if (errorPanel) {
                errorPanel.remove();
            }
            return;
        }
        let panel = errorPanel;
        if (!panel) {
            panel = document.createElement("div");
            panel.id = "search-error-panel";
            panel.className = "search-errors";
            panel.setAttribute("role", "alert");
            const heading = form.querySelector("#search-heading");
            heading.insertAdjacentElement("afterend", panel);
        }
        const list = document.createElement("ul");
        errors.forEach((message) => {
            const li = document.createElement("li");
            li.textContent = message;
            list.appendChild(li);
        });
        panel.innerHTML = "";
        panel.appendChild(list);
    }

    itemToggle?.addEventListener("click", () => {
        toggleDropdown();
    });

    itemSearch?.addEventListener("input", () => {
        renderItemRows();
    });

    itemRows.forEach((row) => {
        const checkbox = row.querySelector(".items-option-checkbox");
        checkbox.addEventListener("change", () => {
            const selected = itemRows
                .map((candidate) => ({
                    value: candidate.dataset.value,
                    checked: candidate.querySelector(".items-option-checkbox").checked
                }))
                .filter((candidate) => candidate.checked)
                .map((candidate) => candidate.value);
            setHiddenSelectedItems(selected);
            updateSelectedLabel();
        });
    });

    types.forEach((input) => {
        input.addEventListener("change", () => {
            renderItemRows();
        });
    });

    document.addEventListener("click", (event) => {
        if (!itemDropdown || itemDropdown.hidden) {
            return;
        }
        const root = document.getElementById("item-selector");
        if (root && !root.contains(event.target)) {
            closeDropdown();
        }
    });

    form.addEventListener("submit", (event) => {
        const errors = [];
        const dateFrom = form.querySelector("#date-from")?.value.trim() || "";
        const dateTo = form.querySelector("#date-to")?.value.trim() || "";
        const amountFrom = form.querySelector("#amount-from")?.value.trim() || "";
        const amountTo = form.querySelector("#amount-to")?.value.trim() || "";

        const parsedDateFrom = dateFrom ? parseDate(dateFrom) : null;
        const parsedDateTo = dateTo ? parseDate(dateTo) : null;
        if (dateFrom && !parsedDateFrom) {
            errors.push("発生日Fromには存在する日付を指定してください。");
        }
        if (dateTo && !parsedDateTo) {
            errors.push("発生日Toには存在する日付を指定してください。");
        }
        if (parsedDateFrom && parsedDateTo && parsedDateTo < parsedDateFrom) {
            errors.push("発生日Toは発生日From以降の日付を指定してください。");
        }

        const parsedAmountFrom = amountFrom ? parseAmount(amountFrom) : null;
        const parsedAmountTo = amountTo ? parseAmount(amountTo) : null;
        if (amountFrom && parsedAmountFrom === null) {
            errors.push("金額Fromは数値で入力してください。");
        }
        if (amountTo && parsedAmountTo === null) {
            errors.push("金額Toは数値で入力してください。");
        }
        if (parsedAmountFrom !== null && parsedAmountTo !== null && parsedAmountTo < parsedAmountFrom) {
            errors.push("金額Toは金額From以上を指定してください。");
        }

        if (errors.length > 0) {
            event.preventDefault();
            renderErrors(errors);
            return;
        }
        closeDropdown();
        renderErrors([]);
    });

    clearButton?.addEventListener("click", () => {
        const action = form.getAttribute("action") || window.location.pathname;
        window.location.href = action;
    });

    document.querySelectorAll("[data-target-page]").forEach((button) => {
        button.addEventListener("click", () => {
            const pageValue = Number.parseInt(button.getAttribute("data-page") || "1", 10);
            const targetId = button.getAttribute("data-target-page");
            const target = targetId === "expense-page" ? expensePage : incomePage;
            if (!target) {
                return;
            }
            target.value = Number.isNaN(pageValue) || pageValue < 1 ? "1" : String(pageValue);
            form.requestSubmit();
        });
    });

    syncCheckboxesFromHidden();
    renderItemRows();
})();
