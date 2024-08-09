package password.manager.controllers.views;

import javafx.application.HostServices;
import password.manager.controllers.AbstractController;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public abstract class AbstractViewController extends AbstractController {
    protected AbstractViewController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices) {
        super(ioManager, langResources, hostServices);
    }

    public abstract void reset();
}
