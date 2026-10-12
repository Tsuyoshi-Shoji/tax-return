(() => {
    "use strict";

    const MAX_AMOUNT = 9999999999999n;
    const FORBIDDEN_DETAILS = /[<>"'`\\]/;
    const numberFormatter = new Intl.NumberFormat("ja-JP");

    const byId = (id) => document.getElementById(id);

    const countCodePoints = (value) => Array.from(value || "").length;

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

        const fieldOrder = [typeFieldName, "category", "date", "amount", methodFieldName];
        if (!isIncome) fieldOrder.push("homeApportionment");
        fieldOrder.push("details");

        const fieldElements = {
            [typeFieldName]: typeRadios,
            category: categorySelect ? [categorySelect] : [],
            date: dateInput ? [dateInput] : [],
            amount: amountInput ? [amountInput] : [],
            [methodFieldName]: methodRadios,
            homeApportionment: homeApportionmentCheckbox ? [homeApportionmentCheckbox] : [],
            details: detailsInput ? [detailsInput] : []
        };

        const sourceOptions = optionSource
            ? Array.from(optionSource.options).map((option) => option.cloneNode(true))
            : [];

        let confirmationFlowRequired = form.dataset.confirmationRequired === "true" && !!(confirmationToken && confirmationToken.value.trim());
        let allowConfirmedSubmit = false;
        let isSubmitting = false;
        let lastDialogOrigin = submitButton || form;

        const setSubmittingState = (submitting) => {
            isSubmitting = submitting;
            if (submitButton) submitButton.disabled = submitting;
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
            elements.forEach((element) => {
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
        };

        const showValidationDialog = (errors) => {
            if (!window.AppDialog) return;
            lastDialogOrigin = document.activeElement instanceof HTMLElement ? document.activeElement : submitButton || form;
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
            lastDialogOrigin = document.activeElement instanceof HTMLElement ? document.activeElement : submitButton || form;
            openDialog(confirmationDialog, lastDialogOrigin);
        };

        const submitAfterConfirmation = () => {
            allowConfirmedSubmit = true;
            closeDialog(confirmationDialog, "confirm");
            if (typeof form.requestSubmit === "function") {
                form.requestSubmit(submitButton || undefined);
            } else {
                setSubmittingState(true);
                form.submit();
            }
        };

        updateSubmitLabel();
        renderCategoryOptions(false);
        updateCounter();
        updateApportionmentPreview();

        typeRadios.forEach((radio) => {
            radio.addEventListener("change", () => {
                renderCategoryOptions(true);
                clearFieldError(typeFieldName);
                clearFieldError("category");
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
                clearFieldError("category");
                clearConfirmationState();
            });
        }

        if (dateInput) {
            dateInput.addEventListener("change", () => {
                clearFieldError("date");
                clearConfirmationState();
            });
        }

        if (amountInput) {
            amountInput.addEventListener("input", () => {
                clearFieldError("amount");
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
                renderCategoryOptions(true);
                updateCounter();
                updateApportionmentPreview();
                fieldOrder.forEach((fieldName) => clearFieldError(fieldName));
                clearConfirmationState();
                const firstType = typeRadios.find((radio) => radio.checked) || typeRadios[0];
                if (firstType) firstType.focus();
            });
        }

        form.addEventListener("submit", (event) => {
            if (isSubmitting) {
                event.preventDefault();
                return;
            }

            const { errors } = validateForm();
            applyValidationResult(errors);
            if (Object.keys(errors).length > 0) {
                event.preventDefault();
                showValidationDialog(errors);
                return;
            }

            if (confirmationFlowRequired && confirmationToken && confirmationToken.value.trim() && !allowConfirmedSubmit) {
                event.preventDefault();
                showConfirmationDialog();
                return;
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
    });
})();
