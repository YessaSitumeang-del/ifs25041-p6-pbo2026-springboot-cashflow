package org.delcom.ifs25041springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25041springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25041springbootcashflow.domain.entity.FinanceSummary;
import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.User;
import org.delcom.ifs25041springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25041springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25041springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25041springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25041springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25041springbootcashflow.usecase.UserUseCase;

import java.util.List;

/**
 * Halaman admin: transaksi milik pengguna lain, hanya baca (tanpa tombol tambah/hapus).
 * Parameter ID bersifat opsional agar {@code /user-transactions} tanpa ID dialihkan ke daftar pengguna.
 */
@Route(value = "user-transactions", layout = MainLayout.class)
@PageTitle("Transaksi Pengguna")
public class UserTransactionsView extends VerticalLayout
        implements HasUrlParameter<Integer>, BeforeEnterObserver {

    private static final String MARGIN = "margin";
    private static final String COLOR = "color";

    private final UserUseCase userUseCase;
    private final FinanceUseCase financeUseCase;
    private final FinancePresenter presenter;
    private final IUserRepository userRepository;

    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final H1 title = new H1("Transaksi Pengguna");
    private final Paragraph subtitle = new Paragraph();
    private final Span emptyState = new Span("- Pengguna ini belum memiliki transaksi.");
    private final Span countValue = new Span("0");
    private final Span incomeValue = new Span();
    private final Span expenseValue = new Span();
    private final Span balanceValue = new Span();

    public UserTransactionsView(
            UserUseCase userUseCase,
            FinanceUseCase financeUseCase,
            FinancePresenter presenter,
            IUserRepository userRepository
    ) {
        this.userUseCase = userUseCase;
        this.financeUseCase = financeUseCase;
        this.presenter = presenter;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#F3F4F6");

        title.getStyle().set(MARGIN, "0");
        subtitle.getStyle().set(MARGIN, "0").set(COLOR, "#4B5563");
        emptyState.getStyle().set(COLOR, "#6B7280").set("padding", "0.5rem 0");
        incomeValue.getStyle().set(COLOR, "#166534");
        expenseValue.getStyle().set(COLOR, "#B91C1C");

        Button back = new Button("Kembali", VaadinIcon.ARROW_LEFT.create(),
                event -> UI.getCurrent().navigate(UsersView.class));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.getStyle().set("cursor", "pointer");

        HorizontalLayout cards = new HorizontalLayout(
                FinanceView.summaryCard("Jumlah Transaksi", countValue, "#2563EB"),
                FinanceView.summaryCard("Total Pemasukan", incomeValue, "#16A34A"),
                FinanceView.summaryCard("Total Pengeluaran", expenseValue, "#DC2626"),
                FinanceView.summaryCard("Saldo", balanceValue, "#7C3AED")
        );
        cards.setWidthFull();
        cards.getStyle().set("flex-wrap", "wrap").set(MARGIN, "0.75rem 0");

        FinanceView.addTransactionColumns(grid, presenter);
        grid.setSizeFull();
        GridStyles.apply(grid);

        Div card = PageCard.wrap(back, title, subtitle, cards, emptyState, grid);
        add(card);
        expand(card);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Integer userId) {
        User viewer = AuthSession.currentUser(userRepository).orElse(null);
        if (viewer == null || !viewer.isAdmin()) {
            // Pengalihan ditangani beforeEnter (login / dashboard).
            return;
        }
        if (userId == null) {
            event.forwardTo(UsersView.class);
            return;
        }
        try {
            User owner = userUseCase.getUser(userId);
            List<Transaction> transactions = financeUseCase.getTransactionsOf(viewer, userId);
            FinanceSummary summary = financeUseCase.getSummary(userId);

            title.setText("Transaksi " + owner.getFullName());
            subtitle.setText("@" + owner.getUsername() + " - tampilan hanya baca.");
            FinanceView.updateSummaryValues(presenter, summary, countValue, incomeValue, expenseValue, balanceValue);
            grid.setItems(transactions);
            emptyState.setVisible(transactions.isEmpty());
            grid.setVisible(!transactions.isEmpty());
        } catch (EntityNotFoundException ex) {
            event.forwardTo(UsersView.class);
        } catch (ValidationException ex) {
            event.forwardTo(FinanceView.class);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User current = AuthSession.currentUser(userRepository).orElse(null);
        if (current == null) {
            event.forwardTo(LoginView.class);
        } else if (!current.isAdmin()) {
            event.forwardTo(FinanceView.class);
        }
    }
}
