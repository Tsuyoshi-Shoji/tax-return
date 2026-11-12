(() => {
    "use strict";

    const MAX_AMOUNT = 9999999999999n;
    const DEFAULT_MONTHLY_ROWS = 5;
    const MAX_MONTHLY_ROWS = 50;
    const FORBIDDEN_DETAILS = /[<>"'`\\]/;
    const CONTROL_CHARACTERS = /[\u0000-\u001f\u007f-\u009f\u2028\u2029]/;
    const OTHER_CATEGORY = "other";
    const OUT_OF_SCOPE_TAX_CATEGORY = "OUT_OF_SCOPE";
    const numberFormatter = new Intl.NumberFormat("ja-JP");

    const byId = (id) => document.getElementById(id);

    const countCodePoints = (value) => Array.from(value || "").length;

    const normalizeIntegerInput = (value) => (value || "")
        .replace(/[０-９]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 0xFEE0))
        .replace(/[,，]/g, "")
        .trim();

    const parseAmount = (value) => {
        const normalized = (value || "").trim();
        if (!/^[0-9]{1,13}$/.test(normalized)) return null;
        try {
            return BigInt(normalized);
        } catch (error) {
            return null;
        }
    };

    const parseRateHundredths = (value) => {
        const normalized = (value || "").trim();
        if (!normalized || !/^\d{1,3}(?:\.\d{1,2})?$/.test(normalized)) return null;
        const parts = normalized.split(".");
        const integerPart = BigInt(parts[0]);
        const decimalPart = BigInt(((parts[1] || "") + "00").slice(0, 2));
        const result = integerPart * 100n + decimalPart;
        return result <= 10000n ? result : null;
    };

    const formatBigIntYen = (value) => `${numberFormatter.format(value.toString())}円`;

    const formatRate = (hundredths) => {
        if (hundredths === null) return "未設定";
        const integerPart = hundredths / 100n;
        const decimalPart = (hundredths % 100n).toString().padStart(2, "0").replace(/0+$/, "");
        return decimalPart ? `${integerPart.toString()}.${decimalPart}%` : `${integerPart.toString()}%`;
    };

    const getSelectedRadioValue = (nodes) => {
        const checked = nodes.find((node) => node.checked);
        return checked ? checked.value : "";
    };

    const parseIsoDate = (value) => {
        const normalized = (value || "").trim();
        const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(normalized);
        if (!match) return null;
        const year = Number(match[1]);
        const month = Number(match[2]);
        const day = Number(match[3]);
        const date = new Date(Date.UTC(year, month - 1, day));
        if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) {
            return null;
        }
        return { normalized, stamp: Date.UTC(year, month - 1, day) };
    };

    const parseTargetMonth = (value) => {
        const normalized = (value || "").trim();
        const match = /^(\d{4})-(\d{2})$/.exec(normalized);
        if (!match) return null;
        const year = Number(match[1]);
        const month = Number(match[2]);
        if (month < 1 || month > 12) return null;
        return { normalized, year, month };
    };

    const focusFirstInvalid = (fieldOrder, fieldElements) => {
        const invalidField = fieldOrder.find((field) => {
            const elements = fieldElements[field] || [];
            return elements.some((element) => element.getAttribute("aria-invalid") === "true");
        });
        if (!invalidField) return false;
        const elements = fieldElements[invalidField] || [];
        const target = elements.find((element) => !element.disabled) || elements[0];
        if (!target) return false;
        target.focus();
        return true;
    };

    const openDialog = (dialog, origin) => {
        if (!dialog) return;
        dialog.dataset.originId = origin && origin.id ? origin.id : "";
        if (typeof dialog.showModal === "function") {
            dialog.showModal();
        } else {
            dialog.setAttribute("open", "open");
        }
    };

    const closeDialog = (dialog, value) => {
        if (!dialog) return;
        if (typeof dialog.close === "function") {
            dialog.close(value);
        } else {
            dialog.removeAttribute("open");
        }
    };

    document.addEventListener("DOMContentLoaded", () => {
        const form = document.querySelector(".entry-form");
        if (!form) return;

        const entryKind = form.dataset.entryKind === "expense" ? "expense" : "income";
        const isIncome = entryKind === "income";
        const typeFieldName = isIncome ? "incomeType" : "expenseType";
        const methodFieldName = isIncome ? "receiveMethodId" : "paymentMethodId";
        const parsedToday = parseIsoDate(form.dataset.today || "");
        const todayStamp = parsedToday ? parsedToday.stamp : null;
        const apportionmentRateHundredths = parseRateHundredths(form.dataset.apportionmentRate || "");
        const defaultType = form.dataset.defaultType || "";
        const defaultMethod = form.dataset.defaultMethod || "";

        const typeRadios = Array.from(form.querySelectorAll(`input[name="${typeFieldName}"]`));
        const methodRadios = Array.from(form.querySelectorAll(`input[name="${methodFieldName}"]`));
        const categorySelect = byId("category");
        const optionSource = byId("category-option-source");
        const dateInput = byId("date");
        const amountInput = byId("amount");
        const detailsInput = byId("details");
        const withholdingInput = byId("withholding-tax");
        const taxCategorySelect = byId("tax-category");
        const counterpartyNameInput = byId("counterparty-name");
        const counterpartyAddressInput = byId("counterparty-address");
        const excludeCheckbox = byId("exclude-from-return");
        const homeApportionmentCheckbox = byId("home-apportionment");
        const confirmationToken = byId("confirmation-token");
        const clearButton = byId("button-clear");
        const submitButton = byId("button-register");
        const detailsCount = byId("details-count");
        const deductibleAmount = byId("display-deductible-amount");
        const deductibleFormula = byId("display-deductible-formula");
        const rateDisplay = byId("display-apportionment-rate");
        const rateText = byId("display-apportionment-rate-text");
        const validationDialog = byId("validation-dialog");
        const validationDialogList = byId("validation-dialog-list");
        const confirmationDialog = byId("confirmation-dialog");
        const confirmationSubmitButton = byId("button-confirm-submit");
        const modeRadios = isIncome ? [] : Array.from(document.querySelectorAll('input[name="entryMode"][form="entry-form"]'));
        const normalModeSection = byId("normal-mode-section");
        const normalModeActions = byId("normal-mode-actions");
        const monthlyModeSection = byId("monthly-mode-section");
        const monthlyRowsContainer = byId("monthly-rows");
        const targetMonthInput = byId("target-month");
        const add5RowsButton = byId("button-add-5-rows");
        const clearMonthlyButton = byId("button-clear-monthly");
        const submitMonthlyButton = byId("button-register-monthly");
        const monthlyModeBadge = byId("monthly-mode-badge");
        const pageDescription = byId("page-description");
        const monthlyModeDescription = byId("monthly-mode-description");
        const csvModeSection = byId("csv-mode-section");
        const csvModeDescription = byId("csv-mode-description");
        const monthlyRowCount = byId("monthly-row-count");

        const fieldOrder = [typeFieldName, "category", "date", "amount"];
        if (isIncome) fieldOrder.push("withholdingTax", "taxCategory");
        fieldOrder.push(methodFieldName, "counterpartyName", "counterpartyAddress");
        if (!isIncome) fieldOrder.push("homeApportionment");
        fieldOrder.push("details");

        const fieldElements = {
            [typeFieldName]: typeRadios,
            category: categorySelect ? [categorySelect] : [],
            date: dateInput ? [dateInput] : [],
            amount: amountInput ? [amountInput] : [],
            withholdingTax: withholdingInput ? [withholdingInput] : [],
            taxCategory: taxCategorySelect ? [taxCategorySelect] : [],
            [methodFieldName]: methodRadios,
            counterpartyName: counterpartyNameInput ? [counterpartyNameInput] : [],
            counterpartyAddress: counterpartyAddressInput ? [counterpartyAddressInput] : [],
            homeApportionment: homeApportionmentCheckbox ? [homeApportionmentCheckbox] : [],
            details: detailsInput ? [detailsInput] : []
        };

        const sourceOptions = optionSource
            ? Array.from(optionSource.options).map((option) => option.cloneNode(true))
            : [];

        let confirmationFlowRequired = form.dataset.confirmationRequired === "true" && !!(confirmationToken && confirmationToken.value.trim());
        let allowConfirmedSubmit = false;
        let isSubmitting = false;
        let lastDialogOrigin = submitButton || submitMonthlyButton || form;

        const isMonthlyModeSelected = () => {
            if (isIncome) return false;
            const selected = modeRadios.find((radio) => radio.checked);
            return !!selected && selected.value === "monthly";
        };

        const isCsvModeSelected = () => {
            if (isIncome) return false;
            const selected = modeRadios.find((radio) => radio.checked);
            return !!selected && selected.value === "csv";
        };

        const getCurrentSubmitButton = () => (isMonthlyModeSelected() ? submitMonthlyButton : submitButton);

        const setSubmittingState = (submitting) => {
            isSubmitting = submitting;
            if (submitButton) submitButton.disabled = submitting;
            if (submitMonthlyButton) submitMonthlyButton.disabled = submitting;
            if (confirmationSubmitButton) confirmationSubmitButton.disabled = submitting;
        };

        const updateSubmitLabel = () => {
            if (!submitButton) return;
            submitButton.textContent = confirmationFlowRequired && confirmationToken && confirmationToken.value.trim()
                ? submitButton.dataset.confirmLabel
                : submitButton.dataset.defaultLabel;
        };

        const setError = (fieldName, message) => {
            const errorNode = form.querySelector(`[data-error-for="${fieldName}"]`);
            if (errorNode) errorNode.textContent = message || "";
            const elements = fieldElements[fieldName] || [];
            const dynamicTargets = Array.from(form.querySelectorAll(`[data-error-field="${fieldName}"]`));
            [...elements, ...dynamicTargets].forEach((element) => {
                if (!element) return;
                if (message) {
                    element.setAttribute("aria-invalid", "true");
                } else {
                    element.removeAttribute("aria-invalid");
                }
            });
        };

        const clearFieldError = (fieldName) => setError(fieldName, "");

        const clearConfirmationState = () => {
            if (confirmationToken) confirmationToken.value = "";
            confirmationFlowRequired = false;
            allowConfirmedSubmit = false;
            form.dataset.confirmationRequired = "false";
            updateSubmitLabel();
        };

        const getLabelTextForRadio = (nodes) => {
            const checked = nodes.find((node) => node.checked);
            if (!checked) return "未選択";
            const wrapper = checked.closest("label");
            return wrapper ? wrapper.textContent.trim() : "未選択";
        };

        const renderCategoryOptions = (resetSelection) => {
            if (!categorySelect) return;
            const selectedType = getSelectedRadioValue(typeRadios);
            const previousValue = resetSelection ? "" : categorySelect.value;
            const fragment = document.createDocumentFragment();
            const placeholder = document.createElement("option");
            placeholder.value = "";
            placeholder.textContent = "項目を選択してください";
            fragment.appendChild(placeholder);

            let matched = false;
            sourceOptions.forEach((option) => {
                if ((option.dataset.group || "") !== selectedType) return;
                const clone = option.cloneNode(true);
                if (!resetSelection && previousValue && previousValue === clone.value) {
                    clone.selected = true;
                    matched = true;
                }
                fragment.appendChild(clone);
            });

            categorySelect.replaceChildren(fragment);
            if (!matched) categorySelect.value = "";
            categorySelect.disabled = !selectedType;
        };

        const syncIncomeTaxControls = (applyItemDefault) => {
            if (!isIncome) return;
            const isOtherCategory = getSelectedRadioValue(typeRadios) === OTHER_CATEGORY;
            if (withholdingInput) {
                if (isOtherCategory) withholdingInput.value = "0";
                withholdingInput.readOnly = isOtherCategory;
            }
            if (!taxCategorySelect) return;
            if (isOtherCategory) {
                // 「その他」区分はサーバー側で不課税に固定するため、選択不可として表示のみ合わせる。
                taxCategorySelect.value = OUT_OF_SCOPE_TAX_CATEGORY;
                taxCategorySelect.disabled = true;
                return;
            }
            taxCategorySelect.disabled = false;
            if (!applyItemDefault) return;
            const sourceOption = sourceOptions.find((option) => categorySelect && categorySelect.value && option.value === categorySelect.value);
            taxCategorySelect.value = sourceOption ? sourceOption.dataset.taxCategory || "" : "";
        };

        const updateCounter = () => {
            if (!detailsCount || !detailsInput) return;
            detailsCount.textContent = `${countCodePoints(detailsInput.value)} / 255文字`;
        };

        const updateApportionmentPreview = () => {
            if (!deductibleAmount || !deductibleFormula || !rateDisplay) return;
            const amount = parseAmount(amountInput ? amountInput.value : "");
            const rate = apportionmentRateHundredths;
            const exclude = !!(excludeCheckbox && excludeCheckbox.checked);
            const homeApportionment = !!(homeApportionmentCheckbox && homeApportionmentCheckbox.checked);

            rateDisplay.textContent = formatRate(rate);
            if (rateText) rateText.textContent = rate === null ? "未設定" : formatRate(rate).replace("%", "");

            if (amount === null) {
                deductibleAmount.textContent = "—";
                deductibleFormula.textContent = "—";
                return;
            }
            if (exclude) {
                deductibleAmount.textContent = "0円";
                deductibleFormula.textContent = "確定申告対象外のため 0円";
                return;
            }
            if (!homeApportionment) {
                deductibleAmount.textContent = formatBigIntYen(amount);
                deductibleFormula.textContent = `(${formatBigIntYen(amount)} × 100%)`;
                return;
            }
            if (rate === null) {
                deductibleAmount.textContent = "—";
                deductibleFormula.textContent = "按分率が未設定です";
                return;
            }
            const deductible = (amount * rate) / 10000n;
            deductibleAmount.textContent = formatBigIntYen(deductible);
            deductibleFormula.textContent = `(${formatBigIntYen(amount)} × ${formatRate(rate)})`;
        };

        const validateForm = () => {
            const errors = {};
            const selectedType = getSelectedRadioValue(typeRadios).trim();
            const selectedMethod = getSelectedRadioValue(methodRadios).trim();
            const selectedCategory = categorySelect ? categorySelect.value.trim() : "";
            const parsedDate = parseIsoDate(dateInput ? dateInput.value : "");
            const amount = parseAmount(amountInput ? amountInput.value : "");
            const details = detailsInput ? detailsInput.value.trim() : "";

            if (!selectedType) {
                errors[typeFieldName] = "区分を選択してください。";
            }

            const validCategory = sourceOptions.some((option) => option.value === selectedCategory && option.dataset.group === selectedType);
            if (!selectedCategory) {
                errors.category = "項目を選択してください。";
            } else if (!validCategory) {
                errors.category = "選択された項目がマスタに存在しません。";
            }

            if (!(dateInput && dateInput.value.trim())) {
                errors.date = isIncome ? "収益発生日を選択してください。" : "支出発生日を選択してください。";
            } else if (!parsedDate) {
                errors.date = "存在する日付を選択してください。";
            } else if (todayStamp !== null && parsedDate.stamp > todayStamp) {
                errors.date = "登録日より未来の日付は選択できません。";
            }

            if (!(amountInput && amountInput.value.trim())) {
                errors.amount = "金額を入力してください。";
            } else if (amount === null) {
                errors.amount = "金額は数値のみで入力してください。";
            } else if (amount < 1n) {
                errors.amount = "金額は1円以上で入力してください。";
            } else if (amount > MAX_AMOUNT) {
                errors.amount = "金額は9999999999999円以下で入力してください。";
            }

            if (!selectedMethod) {
                errors[methodFieldName] = isIncome ? "受取方法を選択してください。" : "支払方法を選択してください。";
            }

            if (isIncome && withholdingInput) {
                const withholdingRaw = normalizeIntegerInput(withholdingInput.value);
                const withholding = withholdingRaw ? parseAmount(withholdingRaw) : 0n;
                if (withholding === null) {
                    errors.withholdingTax = "源泉徴収税額は0以上の整数（円）で入力してください。";
                } else if (withholding > 0n && selectedType === OTHER_CATEGORY) {
                    errors.withholdingTax = "源泉徴収税額は売上・雑収入の場合のみ入力できます。";
                } else if (withholding > 0n && amount !== null && withholding >= amount) {
                    errors.withholdingTax = "源泉徴収税額は金額未満で入力してください。";
                }
            }

            const counterpartyLabel = isIncome ? "取引先" : "支払先";
            const counterpartyName = counterpartyNameInput ? counterpartyNameInput.value.trim() : "";
            const counterpartyAddress = counterpartyAddressInput ? counterpartyAddressInput.value.trim() : "";
            if (countCodePoints(counterpartyName) > 100) {
                errors.counterpartyName = `${counterpartyLabel}名は100文字以内で入力してください。`;
            } else if (FORBIDDEN_DETAILS.test(counterpartyName) || CONTROL_CHARACTERS.test(counterpartyName)) {
                errors.counterpartyName = `${counterpartyLabel}名に使用できない文字が含まれています。`;
            }
            if (countCodePoints(counterpartyAddress) > 255) {
                errors.counterpartyAddress = `${counterpartyLabel}の所在地は255文字以内で入力してください。`;
            } else if (FORBIDDEN_DETAILS.test(counterpartyAddress) || CONTROL_CHARACTERS.test(counterpartyAddress)) {
                errors.counterpartyAddress = `${counterpartyLabel}の所在地に使用できない文字が含まれています。`;
            } else if (counterpartyAddress && !counterpartyName) {
                errors.counterpartyAddress = `${counterpartyLabel}の所在地を入力する場合は${counterpartyLabel}名も入力してください。`;
            }

            if (!isIncome && homeApportionmentCheckbox && homeApportionmentCheckbox.checked) {
                if (apportionmentRateHundredths === null) {
                    errors.homeApportionment = "設定されている家事按分率が不正です。設定画面で0〜100%の範囲に修正してください。";
                }
            }

            if (!details) {
                errors.details = isIncome ? "収益詳細を入力してください。" : "支出詳細を入力してください。";
            } else if (countCodePoints(details) > 255) {
                errors.details = isIncome ? "収益詳細は1〜255文字で入力してください。" : "支出詳細は1〜255文字で入力してください。";
            } else if (FORBIDDEN_DETAILS.test(details)) {
                errors.details = "詳細に使用できない文字が含まれています。";
            }

            return { errors };
        };

        const applyValidationResult = (errors) => {
            fieldOrder.forEach((fieldName) => setError(fieldName, errors[fieldName] || ""));
            if (monthlyRowsContainer) {
                monthlyRowsContainer.querySelectorAll("[data-error-for]").forEach((node) => {
                    const fieldName = node.getAttribute("data-error-for");
                    if (!fieldName) return;
                    setError(fieldName, errors[fieldName] || "");
                });
            }
            setError("targetMonth", errors.targetMonth || "");
            setError("monthlyRows", errors.monthlyRows || "");
            setError("entryMode", errors.entryMode || "");
        };

        const showValidationDialog = (errors) => {
            if (!window.AppDialog) return;
            lastDialogOrigin = document.activeElement instanceof HTMLElement ? document.activeElement : getCurrentSubmitButton() || form;
            window.AppDialog.alert({
                message: "未入力または修正が必要な項目があります。内容をご確認のうえ、再度お進みください。",
                details: Object.values(errors),
                buttons: [{ label: "内容を確認", primary: true }],
                onClose: () => {
                    if (lastDialogOrigin && document.contains(lastDialogOrigin)) lastDialogOrigin.focus();
                }
            });
        };

        const populateConfirmationDialog = () => {
            const setText = (id, value) => {
                const element = byId(id);
                if (element) element.textContent = value;
            };

            setText("confirm-type", getLabelTextForRadio(typeRadios));
            setText("confirm-category", categorySelect && categorySelect.selectedOptions[0] ? categorySelect.selectedOptions[0].textContent.trim() : "未選択");
            setText("confirm-date", dateInput && dateInput.value ? dateInput.value : "未入力");
            const amount = parseAmount(amountInput ? amountInput.value : "");
            setText("confirm-amount", amount === null ? "未入力" : formatBigIntYen(amount));
            setText("confirm-method", getLabelTextForRadio(methodRadios));
            if (isIncome) {
                const withholdingRaw = normalizeIntegerInput(withholdingInput ? withholdingInput.value : "");
                const withholding = withholdingRaw ? parseAmount(withholdingRaw) : 0n;
                setText("confirm-withholding", withholding === null ? "—" : formatBigIntYen(withholding));
                setText("confirm-tax-category", taxCategorySelect && taxCategorySelect.selectedOptions[0]
                    ? taxCategorySelect.selectedOptions[0].textContent.trim()
                    : "—");
            }
            setText("confirm-counterparty", counterpartyNameInput && counterpartyNameInput.value.trim() ? counterpartyNameInput.value.trim() : "未入力");
            setText("confirm-counterparty-address", counterpartyAddressInput && counterpartyAddressInput.value.trim() ? counterpartyAddressInput.value.trim() : "未入力");
            setText("confirm-exclude", excludeCheckbox && excludeCheckbox.checked ? "対象外にする" : "対象に含める");
            setText("confirm-details", detailsInput && detailsInput.value.trim() ? detailsInput.value.trim() : "未入力");
            if (!isIncome) {
                setText("confirm-apportionment", homeApportionmentCheckbox && homeApportionmentCheckbox.checked
                    ? `適用する（${formatRate(apportionmentRateHundredths)}）`
                    : "適用しない");
                setText("confirm-deductible", deductibleAmount ? deductibleAmount.textContent : "—");
            }
        };

        const showConfirmationDialog = () => {
            if (!confirmationDialog) return;
            populateConfirmationDialog();
            lastDialogOrigin = document.activeElement instanceof HTMLElement ? document.activeElement : getCurrentSubmitButton() || form;
            openDialog(confirmationDialog, lastDialogOrigin);
        };

        const submitAfterConfirmation = () => {
            allowConfirmedSubmit = true;
            closeDialog(confirmationDialog, "confirm");
            if (typeof form.requestSubmit === "function") {
                form.requestSubmit(getCurrentSubmitButton() || undefined);
            } else {
                setSubmittingState(true);
                form.submit();
            }
        };

        const toggleModePanels = () => {
            if (isIncome) return;
            const monthly = isMonthlyModeSelected();
            const csv = isCsvModeSelected();
            if (normalModeSection) normalModeSection.hidden = monthly || csv;
            if (normalModeActions) normalModeActions.hidden = monthly || csv;
            if (monthlyModeSection) monthlyModeSection.hidden = !monthly;
            if (csvModeSection) csvModeSection.hidden = !csv;
            if (add5RowsButton) add5RowsButton.hidden = !monthly;
            if (monthlyModeBadge) monthlyModeBadge.hidden = !monthly;
            if (pageDescription) pageDescription.hidden = monthly || csv;
            if (monthlyModeDescription) monthlyModeDescription.hidden = !monthly;
            if (csvModeDescription) csvModeDescription.hidden = !csv;
        };

        const getCheckedValue = (row, selector) => {
            const checked = row ? row.querySelector(`${selector}:checked`) : null;
            return checked ? (checked.value || "").trim() : "";
        };

        const resetMonthlyRowInputs = (row) => {
            row.querySelectorAll("input, select").forEach((input) => {
                if (input.type === "checkbox") {
                    input.checked = false;
                } else if (input.type === "radio") {
                    if (input.classList.contains("monthly-expense-type")) {
                        input.checked = input.value === defaultType;
                    } else if (input.classList.contains("monthly-payment-method")) {
                        input.checked = input.value === defaultMethod;
                    } else {
                        input.checked = false;
                    }
                } else {
                    input.value = "";
                }
                input.removeAttribute("aria-invalid");
            });
            const category = row.querySelector(".monthly-category");
            if (category) category.dataset.selected = "";
            row.querySelectorAll("[data-error-for]").forEach((node) => {
                node.textContent = "";
            });
        };

        const reindexMonthlyRow = (row, index) => {
            const replaceIndex = (value) => value.replace(/monthlyRows\[\d+]/, `monthlyRows[${index}]`);
            row.dataset.rowIndex = String(index);
            row.setAttribute("aria-label", `明細${index + 1}`);
            const removeButton = row.querySelector(".button-remove-row");
            if (removeButton) {
                removeButton.id = `button-remove-row-${index}`;
                removeButton.setAttribute("aria-label", `明細${index + 1}を削除`);
            }
            row.querySelectorAll("[name], [data-error-for], [data-error-field]").forEach((element) => {
                if (element.name) element.name = replaceIndex(element.name);
                const errorFor = element.getAttribute("data-error-for");
                if (errorFor) element.setAttribute("data-error-for", replaceIndex(errorFor));
                const errorField = element.getAttribute("data-error-field");
                if (errorField) element.setAttribute("data-error-field", replaceIndex(errorField));
            });
        };

        const getMonthlyRows = () => (monthlyRowsContainer
            ? Array.from(monthlyRowsContainer.querySelectorAll(".entry-monthly-row"))
            : []);

        const renderMonthlyCategoryOptions = (row, resetSelection) => {
            if (!row) return;
            const typeSelect = row.querySelector(".monthly-expense-type");
            const categorySelect = row.querySelector(".monthly-category");
            if (!typeSelect || !categorySelect) return;
            const selectedType = getCheckedValue(row, ".monthly-expense-type");
            const previousValue = resetSelection ? "" : (categorySelect.value || categorySelect.dataset.selected || "").trim();
            const fragment = document.createDocumentFragment();
            const placeholder = document.createElement("option");
            placeholder.value = "";
            placeholder.textContent = monthlyRowsContainer?.dataset.itemsPlaceholder || "項目を選択してください";
            fragment.appendChild(placeholder);
            let matched = false;
            sourceOptions.forEach((option) => {
                if ((option.dataset.group || "") !== selectedType) return;
                const clone = option.cloneNode(true);
                if (previousValue && previousValue === clone.value) {
                    clone.selected = true;
                    matched = true;
                }
                fragment.appendChild(clone);
            });
            categorySelect.replaceChildren(fragment);
            if (!matched) categorySelect.value = "";
            categorySelect.disabled = !selectedType;
            categorySelect.dataset.selected = categorySelect.value;
        };

        const renumberMonthlyRows = () => {
            const rows = getMonthlyRows();
            rows.forEach((row, index) => reindexMonthlyRow(row, index));
            rows.forEach((row) => {
                const button = row.querySelector(".button-remove-row");
                if (button) button.disabled = rows.length <= 1;
            });
            if (monthlyRowCount) monthlyRowCount.textContent = String(rows.length);
        };

        const createMonthlyRow = (index) => {
            const rows = getMonthlyRows();
            const template = rows[0];
            if (!template) return null;
            const clone = template.cloneNode(true);
            // 同名ラジオの選択状態が既存行へ波及しないよう、DOM挿入前に添字を振り直す
            reindexMonthlyRow(clone, index);
            resetMonthlyRowInputs(clone);
            return clone;
        };

        const addMonthlyRows = (count) => {
            if (!monthlyRowsContainer) return;
            const currentCount = getMonthlyRows().length;
            if (currentCount + count > MAX_MONTHLY_ROWS) {
                if (window.AppDialog) {
                    window.AppDialog.alert({
                        message: `月間登録モードの入力行は最大${MAX_MONTHLY_ROWS}件です。`,
                        details: [`現在 ${currentCount} 件です。これ以上追加できません。`],
                        buttons: [{ label: "内容を確認", primary: true }]
                    });
                } else {
                    window.alert(`月間登録モードの入力行は最大${MAX_MONTHLY_ROWS}件です。`);
                }
                return;
            }
            for (let i = 0; i < count; i += 1) {
                const row = createMonthlyRow(currentCount + i);
                if (!row) break;
                monthlyRowsContainer.appendChild(row);
                renderMonthlyCategoryOptions(row, true);
            }
            renumberMonthlyRows();
        };

        const clearMonthlyRows = () => {
            const rows = getMonthlyRows();
            if (rows.length === 0 || !monthlyRowsContainer) return;
            const first = rows[0];
            monthlyRowsContainer.replaceChildren(first);
            reindexMonthlyRow(first, 0);
            resetMonthlyRowInputs(first);
            renderMonthlyCategoryOptions(first, true);
            for (let i = 1; i < DEFAULT_MONTHLY_ROWS; i += 1) {
                const row = createMonthlyRow(i);
                if (!row) break;
                monthlyRowsContainer.appendChild(row);
                renderMonthlyCategoryOptions(row, true);
            }
            if (targetMonthInput) targetMonthInput.value = "";
            renumberMonthlyRows();
            clearConfirmationState();
        };

        const validateMonthlyForm = () => {
            const errors = {};
            const targetMonth = parseTargetMonth(targetMonthInput ? targetMonthInput.value : "");
            if (!(targetMonthInput && targetMonthInput.value.trim())) {
                errors.targetMonth = "登録対象月を選択してください。";
            } else if (!targetMonth) {
                errors.targetMonth = "登録対象月の形式が不正です。";
            }
            const rows = getMonthlyRows();
            if (rows.length < 1) {
                errors.monthlyRows = "登録する支出行を1行以上入力してください。";
                return { errors, highAmountFound: false };
            }
            let highAmountFound = false;
            rows.forEach((row, index) => {
                const path = (field) => `monthlyRows[${index}].${field}`;
                const categorySelect = row.querySelector('.monthly-category');
                const dateInputRow = row.querySelector('input[name$=".date"]');
                const amountInputRow = row.querySelector('input[name$=".amount"]');
                const detailsInputRow = row.querySelector('input[name$=".details"]');
                const apportionCheckbox = row.querySelector('input[name$=".homeApportionment"]');

                const selectedType = getCheckedValue(row, ".monthly-expense-type");
                const selectedCategory = categorySelect ? categorySelect.value.trim() : "";
                const selectedMethod = getCheckedValue(row, ".monthly-payment-method");
                const parsedDate = parseIsoDate(dateInputRow ? dateInputRow.value : "");
                const amount = parseAmount(amountInputRow ? amountInputRow.value : "");
                const details = detailsInputRow ? detailsInputRow.value.trim() : "";

                if (!selectedType) {
                    errors[path("expenseType")] = "区分を選択してください。";
                }
                const validCategory = sourceOptions.some((option) => option.value === selectedCategory && option.dataset.group === selectedType);
                if (!selectedCategory) {
                    errors[path("category")] = "項目を選択してください。";
                } else if (!validCategory) {
                    errors[path("category")] = "選択された項目がマスタに存在しません。";
                }
                if (!(dateInputRow && dateInputRow.value.trim())) {
                    errors[path("date")] = "支出発生日を選択してください。";
                } else if (!parsedDate) {
                    errors[path("date")] = "存在する日付を選択してください。";
                } else if (todayStamp !== null && parsedDate.stamp > todayStamp) {
                    errors[path("date")] = "登録日より未来の日付は選択できません。";
                } else if (targetMonth && (new Date(parsedDate.stamp).getUTCFullYear() !== targetMonth.year
                    || new Date(parsedDate.stamp).getUTCMonth() + 1 !== targetMonth.month)) {
                    errors[path("date")] = "支出発生日は対象月内の日付を入力してください。";
                }

                if (!(amountInputRow && amountInputRow.value.trim())) {
                    errors[path("amount")] = "金額を入力してください。";
                } else if (amount === null) {
                    errors[path("amount")] = "金額は数値のみで入力してください。";
                } else if (amount < 1n) {
                    errors[path("amount")] = "金額は1円以上で入力してください。";
                } else if (amount > MAX_AMOUNT) {
                    errors[path("amount")] = "金額は9999999999999円以下で入力してください。";
                }

                if (!selectedMethod) {
                    errors[path("paymentMethodId")] = "支払方法を選択してください。";
                }
                if (!details) {
                    errors[path("details")] = "支出詳細を入力してください。";
                } else if (countCodePoints(details) > 255) {
                    errors[path("details")] = "支出詳細は1〜255文字で入力してください。";
                } else if (FORBIDDEN_DETAILS.test(details)) {
                    errors[path("details")] = "支出詳細に使用できない文字が含まれています。";
                }
                if (apportionCheckbox && apportionCheckbox.checked && apportionmentRateHundredths === null) {
                    errors[path("homeApportionment")] = "設定されている家事按分率が不正です。設定画面で0〜100%の範囲に修正してください。";
                }
                if (amount !== null && amount >= 100000000n) {
                    highAmountFound = true;
                }
            });
            return { errors, highAmountFound };
        };

        updateSubmitLabel();
        renderCategoryOptions(false);
        syncIncomeTaxControls(false);
        updateCounter();
        updateApportionmentPreview();
        if (!isIncome) {
            toggleModePanels();
            getMonthlyRows().forEach((row) => renderMonthlyCategoryOptions(row, false));
            renumberMonthlyRows();
        }

        modeRadios.forEach((radio) => {
            radio.addEventListener("change", () => {
                clearConfirmationState();
                toggleModePanels();
            });
        });

        typeRadios.forEach((radio) => {
            radio.addEventListener("change", () => {
                renderCategoryOptions(true);
                syncIncomeTaxControls(true);
                clearFieldError(typeFieldName);
                clearFieldError("category");
                clearFieldError("withholdingTax");
                clearFieldError("taxCategory");
                clearConfirmationState();
            });
        });

        methodRadios.forEach((radio) => {
            radio.addEventListener("change", () => {
                clearFieldError(methodFieldName);
                clearConfirmationState();
            });
        });

        if (categorySelect) {
            categorySelect.addEventListener("change", () => {
                syncIncomeTaxControls(true);
                clearFieldError("category");
                clearFieldError("taxCategory");
                clearConfirmationState();
            });
        }

        if (withholdingInput) {
            withholdingInput.addEventListener("input", () => {
                clearFieldError("withholdingTax");
                clearConfirmationState();
            });
            withholdingInput.addEventListener("blur", () => {
                const normalized = normalizeIntegerInput(withholdingInput.value);
                if (/^[0-9]{1,13}$/.test(normalized)) {
                    withholdingInput.value = BigInt(normalized).toString();
                }
            });
        }

        if (taxCategorySelect) {
            taxCategorySelect.addEventListener("change", () => {
                clearFieldError("taxCategory");
                clearConfirmationState();
            });
        }

        [counterpartyNameInput, counterpartyAddressInput].forEach((input) => {
            if (!input) return;
            input.addEventListener("input", () => {
                clearFieldError(input === counterpartyNameInput ? "counterpartyName" : "counterpartyAddress");
                if (input === counterpartyNameInput) clearFieldError("counterpartyAddress");
                clearConfirmationState();
            });
        });

        if (dateInput) {
            dateInput.addEventListener("change", () => {
                clearFieldError("date");
                clearConfirmationState();
            });
        }

        if (amountInput) {
            amountInput.addEventListener("input", () => {
                clearFieldError("amount");
                clearFieldError("withholdingTax");
                clearConfirmationState();
                updateApportionmentPreview();
            });
        }

        if (excludeCheckbox) {
            excludeCheckbox.addEventListener("change", () => {
                clearConfirmationState();
                updateApportionmentPreview();
            });
        }

        if (homeApportionmentCheckbox) {
            homeApportionmentCheckbox.addEventListener("change", () => {
                clearFieldError("homeApportionment");
                clearConfirmationState();
                updateApportionmentPreview();
            });
        }

        if (detailsInput) {
            detailsInput.addEventListener("input", () => {
                updateCounter();
                clearFieldError("details");
                clearConfirmationState();
            });
        }

        if (monthlyRowsContainer) {
            monthlyRowsContainer.addEventListener("change", (event) => {
                const target = event.target;
                if (!(target instanceof HTMLElement)) return;
                if (target.classList.contains("monthly-expense-type")) {
                    const row = target.closest(".entry-monthly-row");
                    renderMonthlyCategoryOptions(row, true);
                    const category = row ? row.querySelector(".monthly-category") : null;
                    const categoryField = category ? category.getAttribute("data-error-field") : "";
                    if (categoryField) clearFieldError(categoryField);
                }
                const errorField = target.getAttribute("data-error-field");
                if (errorField) clearFieldError(errorField);
                clearConfirmationState();
            });
            monthlyRowsContainer.addEventListener("input", (event) => {
                const target = event.target;
                if (!(target instanceof HTMLElement)) return;
                const errorField = target.getAttribute("data-error-field");
                if (errorField) clearFieldError(errorField);
                clearConfirmationState();
            });
            monthlyRowsContainer.addEventListener("click", (event) => {
                const target = event.target;
                if (!(target instanceof Element)) return;
                const removeButton = target.closest(".button-remove-row");
                if (!removeButton) return;
                event.preventDefault();
                const row = removeButton.closest(".entry-monthly-row");
                if (!row) return;
                const rows = getMonthlyRows();
                if (rows.length <= 1) return;
                const removedIndex = rows.indexOf(row);
                row.remove();
                renumberMonthlyRows();
                clearConfirmationState();
                const remaining = getMonthlyRows();
                const nextRow = remaining[Math.min(removedIndex, remaining.length - 1)];
                const nextFocus = nextRow ? nextRow.querySelector(".button-remove-row:not(:disabled)") || nextRow.querySelector("input, select") : null;
                if (nextFocus) nextFocus.focus();
            });
        }

        if (add5RowsButton) {
            add5RowsButton.addEventListener("click", () => {
                addMonthlyRows(5);
                clearConfirmationState();
            });
        }

        if (clearMonthlyButton) {
            clearMonthlyButton.addEventListener("click", () => {
                clearMonthlyRows();
                clearFieldError("targetMonth");
                clearFieldError("monthlyRows");
                if (targetMonthInput) targetMonthInput.focus();
            });
        }

        if (clearButton) {
            clearButton.addEventListener("click", () => {
                typeRadios.forEach((radio, index) => {
                    radio.checked = (radio.value === defaultType) || (!defaultType && index === 0);
                });
                methodRadios.forEach((radio, index) => {
                    radio.checked = (radio.value === defaultMethod) || (!defaultMethod && index === 0);
                });
                if (categorySelect) categorySelect.value = "";
                if (dateInput) dateInput.value = "";
                if (amountInput) amountInput.value = "";
                if (excludeCheckbox) excludeCheckbox.checked = false;
                if (homeApportionmentCheckbox) homeApportionmentCheckbox.checked = false;
                if (detailsInput) detailsInput.value = "";
                if (withholdingInput) withholdingInput.value = "0";
                if (taxCategorySelect) taxCategorySelect.value = "";
                if (counterpartyNameInput) counterpartyNameInput.value = "";
                if (counterpartyAddressInput) counterpartyAddressInput.value = "";
                renderCategoryOptions(true);
                syncIncomeTaxControls(true);
                updateCounter();
                updateApportionmentPreview();
                fieldOrder.forEach((fieldName) => clearFieldError(fieldName));
                clearConfirmationState();
                const firstType = typeRadios.find((radio) => radio.checked) || typeRadios[0];
                if (firstType) firstType.focus();
            });
        }

        form.addEventListener("submit", (event) => {
            if (isSubmitting || isCsvModeSelected()) {
                event.preventDefault();
                return;
            }

            let errors;
            let highAmountFound = false;
            if (isMonthlyModeSelected()) {
                const monthlyResult = validateMonthlyForm();
                errors = monthlyResult.errors;
                highAmountFound = monthlyResult.highAmountFound;
            } else {
                errors = validateForm().errors;
            }
            applyValidationResult(errors);
            if (Object.keys(errors).length > 0) {
                event.preventDefault();
                showValidationDialog(errors);
                return;
            }

            if (isMonthlyModeSelected() && confirmationFlowRequired && confirmationToken && confirmationToken.value.trim() && !allowConfirmedSubmit) {
                event.preventDefault();
                const confirmed = window.confirm("高額支出を含むため確認して送信します。よろしいですか？");
                if (!confirmed) {
                    clearConfirmationState();
                    return;
                }
                allowConfirmedSubmit = true;
                if (typeof form.requestSubmit === "function") {
                    form.requestSubmit(getCurrentSubmitButton() || undefined);
                } else {
                    setSubmittingState(true);
                    form.submit();
                }
                return;
            }

            if (!isMonthlyModeSelected() && confirmationFlowRequired && confirmationToken && confirmationToken.value.trim() && !allowConfirmedSubmit) {
                event.preventDefault();
                showConfirmationDialog();
                return;
            }

            if (isMonthlyModeSelected() && highAmountFound && !confirmationFlowRequired) {
                const confirmed = window.confirm("100,000,000円以上の支出が含まれます。このまま送信しますか？");
                if (!confirmed) {
                    event.preventDefault();
                    return;
                }
            }

            allowConfirmedSubmit = false;
            setSubmittingState(true);
        });

        if (validationDialog) {
            validationDialog.addEventListener("close", () => {
                if (!focusFirstInvalid(fieldOrder, fieldElements)) {
                    const origin = (validationDialog.dataset.originId && byId(validationDialog.dataset.originId)) || lastDialogOrigin;
                    if (origin && typeof origin.focus === "function") origin.focus();
                }
            });
            validationDialog.addEventListener("click", (event) => {
                const rect = validationDialog.getBoundingClientRect();
                const inDialog = rect.top <= event.clientY && event.clientY <= rect.top + rect.height
                    && rect.left <= event.clientX && event.clientX <= rect.left + rect.width;
                if (!inDialog) closeDialog(validationDialog, "close");
            });
        }

        if (confirmationDialog) {
            confirmationDialog.addEventListener("click", (event) => {
                const rect = confirmationDialog.getBoundingClientRect();
                const inDialog = rect.top <= event.clientY && event.clientY <= rect.top + rect.height
                    && rect.left <= event.clientX && event.clientX <= rect.left + rect.width;
                if (!inDialog) closeDialog(confirmationDialog, "cancel");
            });
            confirmationDialog.addEventListener("cancel", () => {
                clearConfirmationState();
            });
            confirmationDialog.addEventListener("close", () => {
                if (confirmationDialog.returnValue === "confirm") {
                    return;
                }
                if (confirmationDialog.returnValue !== "confirm") {
                    clearConfirmationState();
                }
                if (!focusFirstInvalid(fieldOrder, fieldElements)) {
                    const origin = (confirmationDialog.dataset.originId && byId(confirmationDialog.dataset.originId)) || lastDialogOrigin || submitButton;
                    if (origin && typeof origin.focus === "function") origin.focus();
                }
            });
        }

        if (confirmationSubmitButton) {
            confirmationSubmitButton.addEventListener("click", () => {
                submitAfterConfirmation();
            });
        }

        if (confirmationFlowRequired) {
            window.setTimeout(() => {
                updateApportionmentPreview();
                showConfirmationDialog();
            }, 0);
        }

        setupCsvImport();
    });

    function setupCsvImport() {
        const csvForm = document.getElementById("csv-import-form");
        const dropzone = document.getElementById("csv-dropzone");
        const fileInput = document.getElementById("csv-file");
        if (!csvForm || !dropzone || !fileInput) return;
        const selectButton = document.getElementById("button-csv-select");
        const clearButton = document.getElementById("button-csv-clear");
        const importButton = document.getElementById("button-csv-import");
        const fileNameLabel = document.getElementById("csv-file-name");
        const errorLabel = document.getElementById("csv-file-error");
        const maxBytes = Number(csvForm.dataset.maxBytes) || 1048576;
        const emptyLabel = fileNameLabel ? fileNameLabel.textContent : "";
        let submitting = false;

        const validateFile = (file) => {
            if (!file) return "CSVファイルを選択してください。";
            if (!/\.csv$/i.test(file.name)) return "拡張子が .csv のファイルを選択してください。";
            if (file.size === 0) return "空のファイルは取り込めません。";
            if (file.size > maxBytes) return "CSVファイルは1MB以下にしてください。";
            return "";
        };

        const render = () => {
            const file = fileInput.files && fileInput.files[0];
            const error = file ? validateFile(file) : "";
            if (fileNameLabel) fileNameLabel.textContent = file ? file.name : emptyLabel;
            if (errorLabel) errorLabel.textContent = error;
            dropzone.classList.toggle("has-file", !!file && !error);
            if (importButton) importButton.disabled = submitting || !file || !!error;
        };

        const assignFiles = (files) => {
            if (!files || files.length === 0) return;
            if (files.length > 1) {
                if (errorLabel) errorLabel.textContent = "CSVファイルは1つだけ選択してください。";
                return;
            }
            try {
                const transfer = new DataTransfer();
                transfer.items.add(files[0]);
                fileInput.files = transfer.files;
            } catch (e) {
                if (errorLabel) errorLabel.textContent = "このブラウザーではドラッグ＆ドロップを利用できません。「ファイルを選択」から選択してください。";
                return;
            }
            render();
        };

        if (selectButton) selectButton.addEventListener("click", () => fileInput.click());
        fileInput.addEventListener("change", render);

        ["dragenter", "dragover"].forEach((type) => {
            dropzone.addEventListener(type, (event) => {
                event.preventDefault();
                if (event.dataTransfer) event.dataTransfer.dropEffect = "copy";
                dropzone.classList.add("is-dragover");
            });
        });
        ["dragleave", "dragend"].forEach((type) => {
            dropzone.addEventListener(type, (event) => {
                if (event.type === "dragleave" && dropzone.contains(event.relatedTarget)) return;
                dropzone.classList.remove("is-dragover");
            });
        });
        dropzone.addEventListener("drop", (event) => {
            event.preventDefault();
            dropzone.classList.remove("is-dragover");
            assignFiles(event.dataTransfer ? event.dataTransfer.files : null);
        });
        // ドロップ位置がずれてもブラウザーがファイルを開いて画面遷移しないようにする。
        ["dragover", "drop"].forEach((type) => {
            window.addEventListener(type, (event) => {
                if (!dropzone.contains(event.target)) event.preventDefault();
            });
        });

        if (clearButton) {
            clearButton.addEventListener("click", () => {
                fileInput.value = "";
                render();
            });
        }

        csvForm.addEventListener("submit", (event) => {
            const file = fileInput.files && fileInput.files[0];
            const error = validateFile(file);
            if (submitting || error) {
                event.preventDefault();
                if (errorLabel && error) errorLabel.textContent = error;
                return;
            }
            submitting = true;
            if (importButton) {
                importButton.disabled = true;
                importButton.textContent = "インポート中…";
            }
            if (clearButton) clearButton.disabled = true;
        });

        render();
    }
})();
