package org.delcom.ifs25041springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyNotifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouterLink;
import org.delcom.ifs25041springbootcashflow.domain.entity.User;
import org.delcom.ifs25041springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25041springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25041springbootcashflow.framework.auth.AuthSession;

/**
 * Bagian bersama halaman Login dan Register supaya kode tidak terduplikasi.
 */
final class AuthPage {

    private static final String MARGIN = "margin";

    private AuthPage() {
    }

    /** Aksi login/register yang boleh melempar ValidationException (checked). */
    @FunctionalInterface
    interface AuthAction {
        User run() throws ValidationException;
    }

    static void styleHost(VerticalLayout host) {
        host.setSizeFull();
        host.setJustifyContentMode(VerticalLayout.JustifyContentMode.CENTER);
        host.setAlignItems(VerticalLayout.Alignment.CENTER);
        host.getStyle().set("background", "#F3F4F6");
    }

    static ProgressBar loadingBar() {
        ProgressBar bar = new ProgressBar();
        bar.setIndeterminate(true);
        bar.setVisible(false);
        bar.setWidthFull();
        return bar;
    }

    static Button primaryButton(String text) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.setWidthFull();
        button.getStyle().set("margin-top", "0.85rem");
        button.getStyle().set("cursor", "pointer");
        return button;
    }

    static RouterLink link(String text, Class<? extends Component> target) {
        RouterLink link = new RouterLink(text, target);
        link.getStyle()
                .set("align-self", "flex-end")
                .set("text-align", "right")
                .set("cursor", "pointer");
        return link;
    }

    /** Lebar penuh, nilai dikirim ke server saat mengetik, dan Enter memicu submit. */
    static <F extends Component & HasSize & HasValueChangeMode & KeyNotifier> void prepareField(
            F field, Runnable submit) {
        field.setWidthFull();
        // Agar nilai sudah sampai server sebelum Enter memicu submit.
        field.setValueChangeMode(ValueChangeMode.EAGER);
        field.getElement().getStyle().set(MARGIN, "0");
        field.addKeyUpListener(Key.ENTER, event -> submit.run());
    }

    static Runnable submitAction(ProgressBar loadingBar, Button button, AuthAction action) {
        Runnable submit = () -> {
            loadingBar.setVisible(true);
            button.setEnabled(false);
            try {
                AuthSession.login(action.run());
                UI.getCurrent().navigate(FinanceView.class);
            } catch (ValidationException ex) {
                loadingBar.setVisible(false);
                button.setEnabled(true);
                Notification n = Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_START);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        };
        button.addClickListener(event -> submit.run());
        return submit;
    }

    static Main build(String titleText, String subtitleText, Component... formChildren) {
        H1 title = new H1(titleText);
        title.getStyle().set(MARGIN, "0");

        Paragraph subtitle = new Paragraph(subtitleText);
        subtitle.getStyle().set(MARGIN, "0 0 0.15rem 0").set("color", "#4B5563");

        VerticalLayout form = new VerticalLayout(title, subtitle);
        form.add(formChildren);
        form.setPadding(false);
        form.setSpacing(false);
        form.setWidthFull();
        form.getStyle().set("gap", "0.45rem");

        Div card = new Div(form);
        card.getStyle()
                .set("width", "100%")
                .set("max-width", "420px")
                .set("background", "#FFFFFF")
                .set("border", "1px solid #E5E7EB")
                .set("border-radius", "12px")
                .set("padding", "1.35rem 1.35rem")
                .set("box-shadow", "0 8px 24px rgba(15, 23, 42, 0.08)")
                .set("box-sizing", "border-box");

        // Bungkus kartu dengan <main> agar semua konten berada di dalam landmark (aturan axe "region").
        Main main = new Main(card);
        main.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("width", "100%");
        return main;
    }

    static void redirectIfLoggedIn(BeforeEnterEvent event, IUserRepository userRepository) {
        if (AuthSession.currentUser(userRepository).isPresent()) {
            event.forwardTo(FinanceView.class);
        }
    }
}
