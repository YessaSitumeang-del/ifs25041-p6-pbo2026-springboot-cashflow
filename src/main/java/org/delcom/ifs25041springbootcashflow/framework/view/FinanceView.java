package org.delcom.ifs25041springbootcashflow.framework.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25041springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25041springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25041springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.FinanceSummary;
import org.delcom.ifs25041springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25041springbootcashflow.domain.entity.User;
import org.delcom.ifs25041springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25041springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25041springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25041springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25041springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25041springbootcashflow.usecase.FinanceUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dashboard keuangan pribadi: kartu ringkasan, toolbar, daftar transaksi, dan semua dialognya.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Keuangan Saya")
public class FinanceView extends VerticalLayout {

    private static final Logger LOG = LoggerFactory.getLogger(FinanceView.class);
    private static final Executor AI_EXECUTOR = task -> Thread.ofVirtual().start(task);
    private static final String COLOR_INCOME = "#166534";
    private static final String COLOR_EXPENSE = "#B91C1C";
    private static final long MAX_PROOF_BYTES = 5L * 1024 * 1024;

    private final FinanceUseCase financeUseCase;
    private final FinanceAiUseCase financeAiUseCase;
    private final FinancePresenter presenter;
    private final FileStorage fileStorage;

    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final TextField searchField = new TextField();
    private final Select<TransactionTypeFilter> typeFilter = new Select<>();
    private final Select<SortOption> sortSelect = new Select<>();
    private final Paragraph statusLabel = new Paragraph();
    private final Span emptyState = new Span("- Belum ada transaksi yang dicatat!");
    private final Span countValue = new Span("0");
    private final Span incomeValue = new Span();
    private final Span expenseValue = new Span();
    private final Span balanceValue = new Span();

    public FinanceView(
            FinanceUseCase financeUseCase,
            FinanceAiUseCase financeAiUseCase,
            FinancePresenter presenter,
            FileStorage fileStorage
    ) {
        this.financeUseCase = financeUseCase;
        this.financeAiUseCase = financeAiUseCase;
        this.presenter = presenter;
        this.fileStorage = fileStorage;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#F3F4F6");

        H1 title = new H1("Catatan Keuangan Saya");
        title.getStyle().set("margin", "0");
        Paragraph subtitle = new Paragraph("Catat pemasukan dan pengeluaran, pantau saldo kamu.");
        subtitle.getStyle().set("margin", "0").set("color", "#4B5563");

        statusLabel.getStyle().set("margin", "0.5rem 0 0").set("font-weight", "600").set("color", "#1F2937");
        emptyState.getStyle().set("color", "#6B7280").set("padding", "0.5rem 0");

        configureToolbar();
        configureGrid();

        Div card = PageCard.wrap(title, subtitle, buildSummaryCards(), buildToolbar(), statusLabel, emptyState, grid);
        add(card);
        expand(card);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Data dimuat setelah view benar-benar ditampilkan (guard rute di MainLayout sudah lolos).
        refreshAll();
    }

    private User requireUser() {
        return AuthSession.currentUser().orElseThrow();
    }

    // ------------------------------------------------------------------ ringkasan

    private HorizontalLayout buildSummaryCards() {
        incomeValue.getStyle().set("color", COLOR_INCOME);
        expenseValue.getStyle().set("color", COLOR_EXPENSE);
        HorizontalLayout cards = new HorizontalLayout(
                summaryCard("Jumlah Transaksi", countValue, "#2563EB"),
                summaryCard("Total Pemasukan", incomeValue, "#16A34A"),
                summaryCard("Total Pengeluaran", expenseValue, "#DC2626"),
                summaryCard("Saldo", balanceValue, "#7C3AED")
        );
        cards.setWidthFull();
        cards.setSpacing(true);
        cards.getStyle().set("flex-wrap", "wrap").set("margin", "0.75rem 0");
        return cards;
    }

    static Div summaryCard(String label, Span value, String accent) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-size", "0.8rem").set("color", "#6B7280");
        value.getStyle().set("font-size", "1.35rem").set("font-weight", "700");
        Div card = new Div(labelSpan, value);
        card.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("flex", "1 1 200px")
                .set("gap", "0.25rem")
                .set("padding", "0.75rem 1rem")
                .set("background", "#FFFFFF")
                .set("border", "1px solid #E5E7EB")
                .set("border-left", "4px solid " + accent)
                .set("border-radius", "8px");
        return card;
    }

    static void updateSummaryValues(
            FinancePresenter presenter,
            FinanceSummary summary,
            Span count,
            Span income,
            Span expense,
            Span balance
    ) {
        count.setText(String.valueOf(summary.count()));
        income.setText(presenter.formatMoney(summary.income()));
        expense.setText(presenter.formatMoney(summary.expense()));
        balance.setText(presenter.formatMoney(summary.balance()));
        balance.getStyle().set("color", summary.balance() < 0 ? COLOR_EXPENSE : "#1F2937");
    }

    // ------------------------------------------------------------------ grid

    /** Kolom bersama untuk dashboard sendiri dan tampilan admin (read-only). */
    static void addTransactionColumns(Grid<Transaction> grid, FinancePresenter presenter) {
        grid.addColumn(Transaction::getId)
                .setHeader("ID").setKey("id").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Comparator.comparingInt(Transaction::getId));

        grid.addColumn(Transaction::getDescription)
                .setHeader("Keterangan").setKey("description").setFlexGrow(1)
                .setComparator(Comparator.comparing(Transaction::getDescription, String.CASE_INSENSITIVE_ORDER));

        grid.addComponentColumn(transaction -> {
                    Span amount = new Span(presenter.formatMoney(transaction.getAmount()));
                    amount.getStyle()
                            .set("font-weight", "600")
                            .set("white-space", "nowrap")
                            .set("color", transaction.getType() == TransactionType.INCOME ? COLOR_INCOME : COLOR_EXPENSE);
                    return amount;
                })
                .setHeader("Jumlah").setKey("amount").setAutoWidth(true)
                .setComparator(Comparator.comparingLong(Transaction::getAmount));

        grid.addComponentColumn(transaction -> {
                    boolean income = transaction.getType() == TransactionType.INCOME;
                    Span badge = new Span(transaction.getType().label());
                    badge.getStyle()
                            .set("padding", "2px 10px")
                            .set("border-radius", "999px")
                            .set("font-size", "0.8rem")
                            .set("font-weight", "600")
                            .set("background", income ? "#DCFCE7" : "#FEE2E2")
                            .set("color", income ? COLOR_INCOME : COLOR_EXPENSE);
                    return badge;
                })
                .setHeader("Tipe").setKey("type").setAutoWidth(true)
                .setComparator(Comparator.comparing((Transaction transaction) -> transaction.getType().label()));

        grid.addComponentColumn(transaction -> {
                    if (!transaction.hasProof()) {
                        return new Span("-");
                    }
                    return ImagePreviewDialog.clickableThumbnail(
                            "/uploads/" + transaction.getProofPath(), "Bukti transaksi", "40px");
                })
                .setHeader("Bukti").setKey("proof").setAutoWidth(true).setFlexGrow(0).setSortable(false);

        grid.addColumn(presenter::formatCreatedAt)
                .setHeader("Waktu").setKey("createdAt").setAutoWidth(true)
                .setComparator(Comparator.comparing(Transaction::getCreatedAt));
    }

    private void configureGrid() {
        addTransactionColumns(grid, presenter);
        grid.addComponentColumn(transaction -> {
                    Button delete = new Button("Hapus", VaadinIcon.TRASH.create(),
                            event -> openDeleteDialog(transaction));
                    delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
                    delete.getStyle().set("cursor", "pointer");
                    return delete;
                })
                .setHeader("Tindakan").setKey("actions").setAutoWidth(true).setFlexGrow(0).setSortable(false);

        grid.setSizeFull();
        GridStyles.apply(grid);
    }

    // ------------------------------------------------------------------ toolbar

    private void configureToolbar() {
        searchField.setPlaceholder("Cari keterangan...");
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(400);
        searchField.setWidth("240px");
        searchField.addValueChangeListener(event -> applyFilters());
        searchField.addKeyDownListener(Key.ENTER, event -> applyFilters());

        typeFilter.setItems(TransactionTypeFilter.values());
        typeFilter.setItemLabelGenerator(TransactionTypeFilter::label);
        typeFilter.setValue(TransactionTypeFilter.ALL);
        typeFilter.setWidth("170px");
        typeFilter.addValueChangeListener(event -> applyFilters());

        sortSelect.setItems(SortOption.values());
        sortSelect.setItemLabelGenerator(SortOption::label);
        sortSelect.setValue(SortOption.NEWEST_FIRST);
        sortSelect.setWidth("190px");
        sortSelect.addValueChangeListener(event -> applyFilters());
    }

    private HorizontalLayout buildToolbar() {
        Button search = new Button("Cari", VaadinIcon.SEARCH.create(), event -> applyFilters());
        search.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button reload = new Button("Muat Ulang", VaadinIcon.REFRESH.create(), event -> {
            searchField.clear();
            typeFilter.setValue(TransactionTypeFilter.ALL);
            sortSelect.setValue(SortOption.NEWEST_FIRST);
            refreshAll();
        });

        Button generateAi = new Button("Generate AI", VaadinIcon.MAGIC.create(), event -> openAiDialog());
        generateAi.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        Button addIncome = new Button("Tambah Pemasukan", VaadinIcon.ARROW_UP.create(),
                event -> openAddDialog(TransactionType.INCOME));
        addIncome.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        Button addExpense = new Button("Tambah Pengeluaran", VaadinIcon.ARROW_DOWN.create(),
                event -> openAddDialog(TransactionType.EXPENSE));
        addExpense.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        for (Button button : List.of(search, reload, generateAi, addIncome, addExpense)) {
            button.getStyle().set("cursor", "pointer");
        }

        HorizontalLayout toolbar = new HorizontalLayout(
                searchField, typeFilter, sortSelect, search, reload, generateAi, addIncome, addExpense);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle().set("flex-wrap", "wrap");
        return toolbar;
    }

    // ------------------------------------------------------------------ dialog tambah

    private void openAddDialog(TransactionType type) {
        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        TextField descriptionField = new TextField("Keterangan");
        descriptionField.setWidthFull();
        descriptionField.setPlaceholder(type == TransactionType.INCOME ? "Contoh: Gaji bulanan" : "Contoh: Makan siang");

        TextField amountField = new TextField("Jumlah (Rp)");
        amountField.setWidthFull();
        amountField.setPrefixComponent(new Span("Rp"));
        amountField.setHelperText("Angka bulat tanpa titik/koma, contoh: 150000");
        amountField.setAllowedCharPattern("[0-9]");

        AtomicReference<String> pendingName = new AtomicReference<>();
        AtomicReference<byte[]> pendingData = new AtomicReference<>();
        Span proofLabel = new Span("Bukti transaksi (opsional)");
        proofLabel.getStyle().set("font-size", "0.875rem").set("font-weight", "500");
        Upload upload = proofUpload(pendingName, pendingData);

        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());
        Button saveButton = new Button("Simpan", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        saveButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            saveButton.setEnabled(false);
            String storedProof = null;
            try {
                User user = requireUser();
                // Validasi dulu sebelum menyentuh file agar input salah tidak meninggalkan sisa upload.
                long amount = financeUseCase.validateInput(descriptionField.getValue(), amountField.getValue());
                if (pendingName.get() != null && pendingData.get() != null) {
                    storedProof = fileStorage.store("proofs", pendingName.get(),
                            new ByteArrayInputStream(pendingData.get()));
                }
                Transaction created = financeUseCase.addTransaction(
                        user.getId(), descriptionField.getValue(), amount, type, storedProof);
                showSuccess(presenter.showAddSuccess(created) + " | "
                        + presenter.formatBalance(financeUseCase.getBalance(user.getId())));
                dialog.close();
                refreshAll();
            } catch (ValidationException | IOException ex) {
                fileStorage.delete(storedProof);
                showError(ex instanceof ValidationException
                        ? presenter.showValidationError(ex.getMessage())
                        : ex.getMessage());
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
            } catch (IllegalStateException ex) {
                LOG.error("Gagal menyimpan transaksi", ex);
                fileStorage.delete(storedProof);
                showError("Terjadi kesalahan saat menyimpan transaksi. Coba lagi.");
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
            }
        });

        VerticalLayout body = new VerticalLayout(descriptionField, amountField, proofLabel, upload);
        body.setPadding(false);
        body.setSpacing(true);

        buildDialog(dialog, "Tambah " + type.label(), loadingBar, body, cancelButton, saveButton);
        dialog.open();
        descriptionField.focus();
    }

    // ------------------------------------------------------------------ dialog AI

    private void openAiDialog() {
        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        Span hint = new Span("Tuliskan catatan keuangan kamu dalam bahasa bebas, AI akan menguraikannya menjadi transaksi:");
        hint.getStyle().set("font-size", "0.875rem").set("color", "#4B5563");

        TextArea promptArea = new TextArea("Catatan Keuangan");
        promptArea.setPlaceholder("Contoh: Hari ini gajian 5 juta, bayar kos 1,2 juta, makan siang 25 ribu...");
        promptArea.setWidthFull();
        promptArea.setMinHeight("100px");

        Grid<AiGeneratedTransaction> previewGrid = new Grid<>(AiGeneratedTransaction.class, false);
        previewGrid.addColumn(AiGeneratedTransaction::description).setHeader("Keterangan").setFlexGrow(2);
        previewGrid.addColumn(item -> presenter.formatMoney(item.amount())).setHeader("Jumlah").setAutoWidth(true);
        previewGrid.addColumn(item -> item.type().label()).setHeader("Tipe").setAutoWidth(true);
        previewGrid.setWidthFull();
        previewGrid.setHeight("200px");
        previewGrid.setVisible(false);
        GridStyles.apply(previewGrid);

        List<AiGeneratedTransaction> generatedItems = new ArrayList<>();

        Button applyButton = new Button("Tambahkan ke Catatan", VaadinIcon.PLUS_CIRCLE.create());
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyButton.setEnabled(false);

        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());

        Button generateButton = new Button("Proses AI", VaadinIcon.MAGIC.create());
        generateButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        generateButton.addClickListener(event -> {
            String promptText = promptArea.getValue();
            if (promptText == null || promptText.trim().isEmpty()) {
                showError("Catatan keuangan tidak boleh kosong");
                return;
            }

            UI ui = UI.getCurrent();
            setAiBusy(true, loadingBar, generateButton, applyButton, cancelButton, promptArea);
            ui.setPollInterval(400);

            String prompt = promptText.trim();
            // Panggilan jaringan berjalan di thread terpisah supaya ProgressBar sempat tampil.
            CompletableFuture.supplyAsync(() -> {
                try {
                    return financeAiUseCase.generateTransactions(prompt);
                } catch (DomainException ex) {
                    throw new IllegalStateException(ex.getMessage(), ex);
                }
            }, AI_EXECUTOR).whenComplete((result, error) -> ui.access(() -> {
                try {
                    if (error != null) {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        showError("Gagal menghasilkan transaksi AI: " + cause.getMessage());
                        return;
                    }
                    generatedItems.clear();
                    generatedItems.addAll(result);
                    previewGrid.setItems(generatedItems);
                    previewGrid.setVisible(true);
                    showSuccess("AI menghasilkan " + generatedItems.size() + " transaksi. Periksa dulu sebelum disimpan.");
                } finally {
                    setAiBusy(false, loadingBar, generateButton, applyButton, cancelButton, promptArea);
                    applyButton.setEnabled(!generatedItems.isEmpty());
                    ui.setPollInterval(-1);
                }
            }));
        });

        applyButton.addClickListener(event -> {
            if (generatedItems.isEmpty()) {
                showError("Belum ada transaksi yang dapat ditambahkan");
                return;
            }
            loadingBar.setVisible(true);
            applyButton.setEnabled(false);
            generateButton.setEnabled(false);
            cancelButton.setEnabled(false);
            try {
                User user = requireUser();
                financeAiUseCase.saveGeneratedTransactions(user.getId(), generatedItems);
                showSuccess("Berhasil menambahkan " + generatedItems.size() + " transaksi dari AI | "
                        + presenter.formatBalance(financeUseCase.getBalance(user.getId())));
                dialog.close();
                refreshAll();
            } catch (ValidationException | IllegalStateException ex) {
                if (ex instanceof IllegalStateException) {
                    LOG.error("Gagal menyimpan transaksi hasil AI", ex);
                }
                showError(ex instanceof ValidationException
                        ? presenter.showValidationError(ex.getMessage())
                        : "Terjadi kesalahan saat menyimpan transaksi. Coba lagi.");
                loadingBar.setVisible(false);
                applyButton.setEnabled(true);
                generateButton.setEnabled(true);
                cancelButton.setEnabled(true);
            }
        });

        HorizontalLayout promptActionRow = new HorizontalLayout(generateButton);
        promptActionRow.setWidthFull();
        promptActionRow.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout body = new VerticalLayout(hint, promptArea, promptActionRow, previewGrid);
        body.setPadding(false);
        body.setSpacing(true);

        buildDialog(dialog, "Generate Transaksi dengan AI", loadingBar, body, cancelButton, applyButton);
        dialog.setWidth("640px");
        dialog.open();
    }

    // ------------------------------------------------------------------ dialog hapus

    private void openDeleteDialog(Transaction transaction) {
        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        Span message = new Span("Hapus transaksi " + presenter.format(transaction) + "? Tindakan ini tidak dapat dibatalkan.");

        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());
        Button confirmButton = new Button("Hapus", VaadinIcon.TRASH.create());
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        confirmButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            confirmButton.setEnabled(false);
            try {
                Transaction removed = financeUseCase.removeTransactionForUser(requireUser().getId(), transaction.getId());
                fileStorage.delete(removed.getProofPath());
                showSuccess(presenter.showRemoveSuccess(removed) + " | "
                        + presenter.formatBalance(financeUseCase.getBalance(requireUser().getId())));
                dialog.close();
                refreshAll();
            } catch (EntityNotFoundException ex) {
                showError(presenter.showRemoveFailed(transaction.getId()));
                dialog.close();
                refreshAll();
            } catch (ValidationException ex) {
                showError(presenter.showValidationError(ex.getMessage()));
                loadingBar.setVisible(false);
                confirmButton.setEnabled(true);
            }
        });

        buildDialog(dialog, "Hapus Transaksi", loadingBar, message, cancelButton, confirmButton);
        dialog.open();
    }

    // ------------------------------------------------------------------ dialog shell

    private void buildDialog(
            Dialog dialog,
            String title,
            ProgressBar loadingBar,
            Component bodyContent,
            Button... footerButtons
    ) {
        dialog.setWidth("500px");
        dialog.setMaxWidth("95vw");
        dialog.setResizable(false);
        dialog.addClassName("finance-form-dialog");

        Span headerTitle = new Span(title);
        headerTitle.getStyle()
                .set("margin", "0")
                .set("color", "#FFFFFF")
                .set("font-size", "1.125rem")
                .set("font-weight", "600")
                .set("line-height", "1.4");

        // Semua dialog memakai header primer #2563EB.
        Div header = new Div(headerTitle);
        header.getStyle()
                .set("background", "#2563EB")
                .set("padding", "1rem 1.5rem")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        loadingBar.setIndeterminate(true);
        loadingBar.getStyle().set("margin", "0").set("height", "4px");

        Div body = new Div(bodyContent);
        body.getStyle()
                .set("background", "#FFFFFF")
                .set("padding", "1.25rem 1.5rem")
                .set("overflow", "hidden")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        // Footer abu-abu #F3F4F6, tombol aksi di sisi kanan.
        Div footer = new Div();
        footer.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-end")
                .set("align-items", "center")
                .set("gap", "0.75rem")
                .set("background", "#F3F4F6")
                .set("border-top", "1px solid #E5E7EB")
                .set("padding", "0.85rem 1.5rem 1.1rem")
                .set("box-sizing", "border-box")
                .set("width", "100%");
        for (Button button : footerButtons) {
            button.getStyle().set("cursor", "pointer");
        }
        footer.add(footerButtons);

        Div shell = new Div(header, loadingBar, body, footer);
        shell.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("width", "100%")
                .set("overflow", "hidden")
                .set("border-radius", "12px")
                .set("background", "#FFFFFF");

        dialog.removeAll();
        dialog.add(shell);
        removeOverlayPadding(dialog);
    }

    private void removeOverlayPadding(Dialog dialog) {
        dialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                dialog.getElement().executeJs("""
                        const overlay = this.$.overlay;
                        if (!overlay || !overlay.shadowRoot) {
                          return;
                        }
                        const content = overlay.shadowRoot.querySelector('[part="content"]');
                        if (content) {
                          content.style.padding = '0';
                          content.style.overflow = 'visible';
                          content.style.maxHeight = 'none';
                        }
                        const overlayPart = overlay.shadowRoot.querySelector('[part="overlay"]');
                        if (overlayPart) {
                          overlayPart.style.padding = '0';
                          overlayPart.style.overflow = 'hidden';
                        }
                        """);
            }
        });
    }

    // ------------------------------------------------------------------ data

    private void applyFilters() {
        int userId = requireUser().getId();
        List<Transaction> transactions = financeUseCase.listTransactions(
                userId, searchField.getValue(), typeFilter.getValue(), sortSelect.getValue());

        StringBuilder title = new StringBuilder("Daftar Transaksi");
        String keyword = searchField.getValue();
        if (keyword != null && !keyword.isBlank()) {
            title.append(" - cari \"").append(keyword.trim()).append("\"");
        }
        if (typeFilter.getValue() != null && typeFilter.getValue() != TransactionTypeFilter.ALL) {
            title.append(" - ").append(typeFilter.getValue().label());
        }
        title.append(" (").append(transactions.size()).append(")");
        refreshGrid(transactions, title.toString());
    }

    private void refreshAll() {
        applyFilters();
        updateSummary();
    }

    private void updateSummary() {
        FinanceSummary summary = financeUseCase.getSummary(requireUser().getId());
        updateSummaryValues(presenter, summary, countValue, incomeValue, expenseValue, balanceValue);
    }

    private void refreshGrid(List<Transaction> transactions, String title) {
        grid.setItems(transactions);
        statusLabel.setText(title);
        emptyState.setVisible(transactions.isEmpty());
        grid.setVisible(!transactions.isEmpty());
    }

    private Upload proofUpload(AtomicReference<String> pendingName, AtomicReference<byte[]> pendingData) {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            pendingName.set(metadata.fileName());
            pendingData.set(data);
        }));
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setAcceptedFileExtensions(".png", ".jpg", ".jpeg", ".webp");
        upload.setMaxFiles(1);
        upload.setMaxFileSize((int) MAX_PROOF_BYTES);
        upload.addFileRejectedListener(event -> showError(event.getErrorMessage()));
        return upload;
    }

    private void setAiBusy(
            boolean busy,
            ProgressBar loadingBar,
            Button generateButton,
            Button applyButton,
            Button cancelButton,
            TextArea promptArea
    ) {
        loadingBar.setVisible(busy);
        generateButton.setEnabled(!busy);
        generateButton.setText(busy ? "Memproses..." : "Proses AI");
        applyButton.setEnabled(false);
        cancelButton.setEnabled(!busy);
        promptArea.setReadOnly(busy);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 4500, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
