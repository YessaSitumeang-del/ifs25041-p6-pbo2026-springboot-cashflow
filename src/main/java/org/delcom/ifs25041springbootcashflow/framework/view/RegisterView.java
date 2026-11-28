package org.delcom.ifs25041springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25041springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25041springbootcashflow.usecase.AuthUseCase;

@Route("register")
@PageTitle("Registrasi")
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {

    private final IUserRepository userRepository;

    public RegisterView(AuthUseCase authUseCase, IUserRepository userRepository) {
        this.userRepository = userRepository;
        AuthPage.styleHost(this);

        TextField fullName = new TextField("Nama Lengkap");
        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");

        ProgressBar loadingBar = AuthPage.loadingBar();
        Button registerButton = AuthPage.primaryButton("Daftar");

        Runnable submit = AuthPage.submitAction(loadingBar, registerButton,
                () -> authUseCase.register(username.getValue(), password.getValue(), fullName.getValue()));

        AuthPage.prepareField(fullName, submit);
        AuthPage.prepareField(username, submit);
        AuthPage.prepareField(password, submit);

        add(AuthPage.build("Registrasi", "Buat akun baru untuk mencatat keuangan pribadi.",
                fullName, username, password, loadingBar, registerButton,
                AuthPage.link("Sudah punya akun? Masuk", LoginView.class)));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        AuthPage.redirectIfLoggedIn(event, userRepository);
    }
}
