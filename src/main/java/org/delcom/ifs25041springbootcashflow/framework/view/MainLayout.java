package org.delcom.ifs25041springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import org.delcom.ifs25041springbootcashflow.domain.entity.User;
import org.delcom.ifs25041springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25041springbootcashflow.framework.auth.AuthSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout utama untuk halaman yang memerlukan login.
 */
public class MainLayout extends AppLayout implements BeforeEnterObserver, AfterNavigationObserver {

    private static final String BACKGROUND = "background";
    private static final String BORDER_LIGHT = "1px solid #E5E7EB";
    private static final String DISPLAY = "display";
    private static final String BLOCK = "block";
    private static final String BORDER_RADIUS = "border-radius";
    private static final String FLEX_SHRINK = "flex-shrink";
    private static final String FONT_SIZE = "font-size";
    private static final String FONT_WEIGHT = "font-weight";
    private static final String MARGIN = "margin";
    private static final String COLOR = "color";
    private static final String WIDTH = "width";
    private static final String BLUE_700 = "#1D4ED8";
    private static final String FONT_SIZE_095REM = "0.95rem";
    private static final String CURSOR = "cursor";
    private static final String POINTER = "pointer";
    private static final String BORDER = "border";
    private static final String GRAY_600 = "#4B5563";
    private static final String GRAY_800 = "#1F2937";
    private static final String PADDING = "padding";
    private static final String DATA_ACTIVE = "data-active";

    private final IUserRepository userRepository;
    private final List<NavEntry> navEntries = new ArrayList<>();
    private final Image headerAvatarImage = new Image();
    private final Span headerAvatarInitial = new Span();
    private final Div headerAvatar = new Div();
    // Landmark <main> untuk isi halaman (aturan axe: landmark-one-main dan region).
    private final Main mainContent = new Main();

    private record NavEntry(RouterLink link, String pathPrefix) {
    }

    public MainLayout(IUserRepository userRepository) {
        this.userRepository = userRepository;
        setPrimarySection(Section.DRAWER);
        mainContent.setSizeFull();
        setContent(mainContent);
        AuthSession.currentUser(userRepository);
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        HorizontalLayout brandTitle = appBrandTitle("1.2rem", "28px");

        configureHeaderAvatar();
        refreshHeaderAvatar(AuthSession.currentUser().orElse(null));

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), brandTitle, headerAvatar);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(brandTitle);
        header.setWidthFull();
        header.setPadding(true);
        header.getStyle()
                .set(BACKGROUND, "#FFFFFF")
                .set("border-bottom", BORDER_LIGHT);
        // <header> = landmark banner.
        Header banner = new Header(header);
        banner.setWidthFull();
        addToNavbar(banner);
    }

    private HorizontalLayout appBrandTitle(String fontSize, String iconSize) {
        Image logo = new Image("icons/icon.png", "Cashflow");
        logo.setWidth(iconSize);
        logo.setHeight(iconSize);
        logo.getStyle()
                .set(DISPLAY, BLOCK)
                .set(BORDER_RADIUS, "6px")
                .set("object-fit", "cover")
                .set(FLEX_SHRINK, "0");

        Span title = new Span("Cashflow");
        title.getStyle()
                .set(FONT_SIZE, fontSize)
                .set(FONT_WEIGHT, "700")
                .set(MARGIN, "0")
                .set(COLOR, "#111827")
                .set("line-height", "1.2");

        HorizontalLayout brand = new HorizontalLayout(logo, title);
        brand.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        brand.setSpacing(false);
        brand.setPadding(false);
        brand.getStyle().set("gap", "0.65rem");
        return brand;
    }

    private void configureHeaderAvatar() {
        headerAvatarImage.setWidth("40px");
        headerAvatarImage.setHeight("40px");
        headerAvatarImage.getStyle()
                .set(BORDER_RADIUS, "50%")
                .set("object-fit", "cover")
                .set(DISPLAY, BLOCK);

        headerAvatarInitial.getStyle()
                .set(WIDTH, "40px")
                .set("height", "40px")
                .set(BORDER_RADIUS, "50%")
                .set(DISPLAY, "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set(BACKGROUND, "#DBEAFE")
                .set(COLOR, BLUE_700)
                .set(FONT_WEIGHT, "700")
                .set(FONT_SIZE, FONT_SIZE_095REM);

        headerAvatar.removeAll();
        headerAvatar.add(headerAvatarInitial);
        headerAvatar.getStyle()
                .set(CURSOR, POINTER)
                .set(BORDER, "2px solid #93C5FD")
                .set(BORDER_RADIUS, "50%")
                .set("overflow", "hidden")
                .set(WIDTH, "40px")
                .set("height", "40px")
                .set(FLEX_SHRINK, "0");
        headerAvatar.getElement().setAttribute("title", "Menu akun");

        ContextMenu accountMenu = new ContextMenu(headerAvatar);
        accountMenu.setOpenOnClick(true);

        AuthSession.currentUser().ifPresent(user -> {
            var identityItem = accountMenu.addItem(accountIdentity(user));
            identityItem.setEnabled(false);
            identityItem.getStyle().set(CURSOR, "default");
            var profileItem = accountMenu.addItem(menuItemContent(VaadinIcon.USER, "Profil", GRAY_600, GRAY_800), event ->
                    UI.getCurrent().navigate(ProfileView.class));
            profileItem.getStyle().set(CURSOR, POINTER);
            var logoutItem = accountMenu.addItem(menuItemContent(VaadinIcon.SIGN_OUT, "Keluar", "#DC2626", "#DC2626"), event -> {
                AuthSession.logout();
                // Full reload agar UI tidak blank setelah keluar dari MainLayout.
                UI.getCurrent().getPage().setLocation("login");
            });
            logoutItem.getStyle().set(CURSOR, POINTER);
        });
    }

    private VerticalLayout accountIdentity(User user) {
        Span username = new Span(user.getUsername());
        username.getStyle()
                .set(DISPLAY, BLOCK)
                .set(FONT_WEIGHT, "700")
                .set(FONT_SIZE, FONT_SIZE_095REM)
                .set(COLOR, "#111827")
                .set(MARGIN, "0");

        Span role = new Span(user.getRole().name());
        role.getStyle()
                .set(DISPLAY, BLOCK)
                .set("margin-top", "0.15rem")
                .set(FONT_SIZE, "0.75rem")
                .set(FONT_WEIGHT, "600")
                .set("letter-spacing", "0.02em")
                .set(COLOR, BLUE_700);

        VerticalLayout identity = new VerticalLayout(username, role);
        identity.setPadding(false);
        identity.setSpacing(false);
        identity.setMargin(false);
        identity.getStyle()
                .set("gap", "0")
                .set(PADDING, "0.15rem 0")
                .set("min-width", "8rem");
        return identity;
    }

    private HorizontalLayout menuItemContent(VaadinIcon iconType, String label, String iconColor, String textColor) {
        Icon icon = iconType.create();
        icon.setSize("1rem");
        icon.getStyle()
                .set(COLOR, iconColor)
                .set(FLEX_SHRINK, "0")
                .set(MARGIN, "0");

        Span text = new Span(label);
        text.getStyle()
                .set(COLOR, textColor)
                .set(FONT_WEIGHT, "600")
                .set(MARGIN, "0");

        HorizontalLayout content = new HorizontalLayout(icon, text);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        content.setSpacing(false);
        content.setPadding(false);
        content.setMargin(false);
        content.setWidth(null);
        content.getStyle()
                .set("gap", "0.5rem")
                .set(MARGIN, "0")
                .set(PADDING, "0")
                .set("justify-content", "flex-start")
                .set(DISPLAY, "inline-flex");
        return content;
    }

    private void refreshHeaderAvatar(User user) {
        if (user == null) {
            headerAvatarInitial.setText("?");
            headerAvatar.removeAll();
            headerAvatar.add(headerAvatarInitial);
            return;
        }

        String initial = user.getFullName() == null || user.getFullName().isBlank()
                ? user.getUsername().substring(0, 1).toUpperCase()
                : user.getFullName().substring(0, 1).toUpperCase();
        headerAvatarInitial.setText(initial);

        headerAvatar.removeAll();
        if (user.getPhotoPath() != null && !user.getPhotoPath().isBlank()) {
            headerAvatarImage.setSrc("/uploads/" + user.getPhotoPath() + "?t=" + System.currentTimeMillis());
            headerAvatarImage.setAlt(user.getFullName());
            headerAvatar.add(headerAvatarImage);
        } else {
            headerAvatar.add(headerAvatarInitial);
        }
    }

    private void createDrawer() {
        VerticalLayout drawer = new VerticalLayout();
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.getStyle()
                .set(BACKGROUND, "#FFFFFF")
                .set("border-right", BORDER_LIGHT);

        Div brand = new Div();
        brand.getStyle()
                .set(PADDING, "1.25rem 1rem 1rem")
                .set("border-bottom", BORDER_LIGHT);
        brand.add(appBrandTitle("1.05rem", "30px"));

        VerticalLayout menu = new VerticalLayout();
        menu.setPadding(true);
        menu.setSpacing(false);
        menu.getStyle().set("gap", "0.35rem").set("padding-top", "1rem");

        menu.add(navItem("Keuangan Saya", FinanceView.class, "", VaadinIcon.WALLET));
        menu.add(navItem("Profil", ProfileView.class, "profile", VaadinIcon.USER));

        AuthSession.currentUser()
                .filter(User::isAdmin)
                .ifPresent(user -> menu.add(navItem("Daftar Pengguna", UsersView.class, "users", VaadinIcon.USERS)));

        drawer.add(brand, menu);
        drawer.expand(menu);
        // <nav> = landmark navigation.
        Nav nav = new Nav(drawer);
        nav.getElement().setAttribute("aria-label", "Menu utama");
        nav.setSizeFull();
        addToDrawer(nav);
    }

    private RouterLink navItem(
            String label,
            Class<? extends Component> view,
            String pathPrefix,
            VaadinIcon iconType
    ) {
        Icon icon = iconType.create();
        icon.setSize("1.05rem");
        icon.getStyle().set(COLOR, GRAY_600);

        Span text = new Span(label);
        text.getStyle()
                .set(FONT_SIZE, FONT_SIZE_095REM)
                .set(FONT_WEIGHT, "500")
                .set(COLOR, GRAY_800);

        HorizontalLayout content = new HorizontalLayout(icon, text);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setSpacing(true);
        content.setPadding(false);
        content.getStyle().set("gap", "0.65rem");

        RouterLink link = new RouterLink();
        link.setRoute(view);
        link.add(content);
        link.getStyle()
                .set(DISPLAY, BLOCK)
                .set(WIDTH, "100%")
                .set("text-decoration", "none")
                .set(PADDING, "0.7rem 0.85rem")
                .set(BORDER_RADIUS, "10px")
                .set("box-sizing", "border-box")
                .set(BORDER, "1px solid transparent")
                .set("transition", "background 0.15s ease")
                .set(CURSOR, POINTER);

        link.getElement().addEventListener("mouseover", e -> {
            if (!"true".equals(link.getElement().getAttribute(DATA_ACTIVE))) {
                link.getStyle().set(BACKGROUND, "#F3F4F6");
            }
        });
        link.getElement().addEventListener("mouseout", e -> {
            if (!"true".equals(link.getElement().getAttribute(DATA_ACTIVE))) {
                link.getStyle().set(BACKGROUND, "transparent");
            }
        });

        navEntries.add(new NavEntry(link, pathPrefix));
        return link;
    }

    private void highlightActive(String path) {
        String normalized = path == null ? "" : path;
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        for (NavEntry entry : navEntries) {
            boolean active;
            if (entry.pathPrefix().isEmpty()) {
                active = normalized.isEmpty();
            } else {
                active = normalized.equals(entry.pathPrefix())
                        || normalized.startsWith(entry.pathPrefix() + "/");
            }
            // Highlight Daftar Pengguna juga saat melihat transaksi user lain.
            if ("users".equals(entry.pathPrefix()) && normalized.startsWith("user-transactions")) {
                active = true;
            }

            RouterLink link = entry.link();
            if (active) {
                link.getElement().setAttribute(DATA_ACTIVE, "true");
                link.getStyle()
                        .set(BACKGROUND, "#DBEAFE")
                        .set(BORDER, "1px solid #93C5FD");
                styleNavChildren(link, BLUE_700, "#1E40AF", "600");
            } else {
                link.getElement().setAttribute(DATA_ACTIVE, "false");
                link.getStyle()
                        .set(BACKGROUND, "transparent")
                        .set(BORDER, "1px solid transparent");
                styleNavChildren(link, GRAY_600, GRAY_800, "500");
            }
        }
    }

    private void styleNavChildren(RouterLink link, String iconColor, String textColor, String weight) {
        link.getChildren()
                .filter(HorizontalLayout.class::isInstance)
                .map(HorizontalLayout.class::cast)
                .findFirst()
                .ifPresent(row -> row.getChildren().forEach(child -> {
                    if (child instanceof Icon icon) {
                        icon.getStyle().set(COLOR, iconColor);
                    }
                    if (child instanceof Span span) {
                        span.getStyle().set(COLOR, textColor).set(FONT_WEIGHT, weight);
                    }
                }));
    }

    /** Taruh view hasil routing di dalam <main>, bukan langsung di slot content AppLayout. */
    @Override
    public void showRouterLayoutContent(HasElement content) {
        mainContent.removeAll();
        if (content != null) {
            mainContent.getElement().appendChild(content.getElement());
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        highlightActive(event.getLocation().getPath());
        refreshHeaderAvatar(AuthSession.currentUser(userRepository).orElse(null));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (AuthSession.currentUser(userRepository).isEmpty()) {
            event.forwardTo(LoginView.class);
        }
    }
}