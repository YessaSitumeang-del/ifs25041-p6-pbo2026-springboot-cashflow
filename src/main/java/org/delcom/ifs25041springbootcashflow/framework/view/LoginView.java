package org.delcom.ifs25041springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
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

@Route("login")
@PageTitle("Login")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final IUserRepository userRepository;

    public LoginView(AuthUseCase authUseCase, IUserRepository userRepository) {
        this.userRepository = userRepository;
        AuthPage.styleHost(this);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        setInnerInputId(username, "login-username");
        setInnerInputId(password, "login-password");

        ProgressBar loadingBar = AuthPage.loadingBar();
        Button loginButton = AuthPage.primaryButton("Masuk");
        loginButton.setId("login-submit");

        Runnable submit = AuthPage.submitAction(loadingBar, loginButton,
                () -> authUseCase.login(username.getValue(), password.getValue()));

        AuthPage.prepareField(username, submit);
        AuthPage.prepareField(password, submit);

        add(AuthPage.build("Masuk", "Gunakan kredensial akun kamu untuk masuk",
                username, password, loadingBar, loginButton,
                AuthPage.link("Belum punya akun? Daftar", RegisterView.class)));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        AuthPage.redirectIfLoggedIn(event, userRepository);
    }

    private void setInnerInputId(Component component, String id) {
        Runnable apply = () -> component.getElement().executeJs(
                "const applyId = () => {" +
                "  const input = this.inputElement || this.querySelector('input');" +
                "  if (input) {" +
                "    input.id = $0;" +
                "    const label = this.querySelector('label');" +
                "    if (label) label.setAttribute('for', $0);" +
                "    return true;" +
                "  }" +
                "  return false;" +
                "};" +
                "if (!applyId()) {" +
                "  if (this.updateComplete) { this.updateComplete.then(applyId); }" +
                "  const obs = new MutationObserver(() => { if (applyId()) obs.disconnect(); });" +
                "  obs.observe(this, { childList: true, subtree: true });" +
                "}", id);

        apply.run();
        component.addAttachListener(event -> apply.run());
    }
}