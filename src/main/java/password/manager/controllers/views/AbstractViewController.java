package password.manager.controllers.views;

import password.manager.controllers.AbstractController;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public abstract class AbstractViewController extends AbstractController {
    protected AbstractViewController(IOManager ioManager, ObservableResourceFactory langResources) {
        super(ioManager, langResources);
    }

    public abstract void reset();
}
