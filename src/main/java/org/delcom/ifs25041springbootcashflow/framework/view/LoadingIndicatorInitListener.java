package org.delcom.ifs25041springbootcashflow.framework.view;

import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * Konfigurasi indikator loading global untuk tindakan yang memerlukan waktu tunggu.
 */
@Component
public class LoadingIndicatorInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            LoadingIndicatorConfiguration conf = uiEvent.getUI().getLoadingIndicatorConfiguration();
            conf.setApplyDefaultTheme(true);
            conf.setFirstDelay(150);
            conf.setSecondDelay(600);
            conf.setThirdDelay(1500);

            // Beri landmark pada indikator koneksi bawaan Vaadin (teks "Online").
            uiEvent.getUI().getPage().executeJs("""
                    const apply = () => {
                        const el = document.querySelector('vaadin-connection-indicator');
                        if (!el) return false;
                        el.setAttribute('role', 'region');
                        el.setAttribute('aria-label', 'Status koneksi');
                        return true;
                    };
                    if (!apply()) {
                        let n = 0;
                        const t = setInterval(() => { if (apply() || ++n > 20) clearInterval(t); }, 250);
                    }
                    """);
        });
    }
}