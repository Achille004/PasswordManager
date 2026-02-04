package password.manager.app.controllers;

import static password.manager.app.Utils.*;
import static password.manager.lib.Utils.*;

import java.util.IdentityHashMap;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

/**
 * Manages tabs within a {@link TabPane}, allowing dynamic creation, selection, and closure of tabs associated with specific items.
 * <p>_This class does not handle the tabs directly, so you can still manage special tabs (like a "+" tab) externally._
 * @param <T> the type of the items associated with the tabs
 * @param <U> the type of the controllers associated with the tabs
 */
public class TabManager<T, U extends AbstractController> {

    private final TabPane TAB_PANE;
    private final IdentityHashMap<T, Tab> TABS_MAP = new IdentityHashMap<>(); // Map for two-way association and caching

    private final Callback<T, U> CONTROLLER_CONSTRUCTOR;
    private final BiConsumer<Tab, T> TAB_INITIALIZER;

    public TabManager(@NotNull TabPane tabPane, @NotNull Callback<T, U> controllerConstructor, @NotNull BiConsumer<Tab, T> tabInitializer) {
        this.TAB_PANE = tabPane;
        this.CONTROLLER_CONSTRUCTOR = controllerConstructor;
        this.TAB_INITIALIZER = tabInitializer;

        final ChangeListener<Tab> TAB_FOCUS_HANDLER = (_, _, newTab) -> {
            if (newTab != null) getController(newTab).reset();
        };
        TAB_PANE.getSelectionModel().selectedItemProperty().addListener(TAB_FOCUS_HANDLER);
    }

    public void openTab(@NotNull T item) {
        Tab tab = TABS_MAP.computeIfAbsent(item, this::createTab);
        selectTab(tab, true);
    }

    public Tab createTab(@NotNull T item) {
        U controller = CONTROLLER_CONSTRUCTOR.call(item);
        Tab tab = new Tab();

        TabManager.loadTab(tab, controller);
        TAB_INITIALIZER.accept(tab, item);

        return tab;
    }

    public void selectTab(@NotNull Tab tab, boolean addIfMissing) {
        Platform.runLater(() -> {
            if (addIfMissing && !TAB_PANE.getTabs().contains(tab)) {
                TAB_PANE.getTabs().add(TAB_PANE.getTabs().size() - 1, tab);
            }
            TAB_PANE.getSelectionModel().select(tab);
        });
    }

    public void selectTab(@NotNull Tab tab) {
        selectTab(tab, false);
    }

    public void selectAdjacentTab(int direction) {
        int currentIndex = TAB_PANE.getSelectionModel().getSelectedIndex();
        int newIndex = intSquash(0, currentIndex + direction, TAB_PANE.getTabs().size() - 1);
        TAB_PANE.getSelectionModel().select(newIndex);
    }

    public void closeTab(@NotNull Tab tab) {
        Platform.runLater(() -> TAB_PANE.getTabs().remove(tab));
    }

    public void closeTab(@NotNull T item) {
        Tab tab = TABS_MAP.remove(item);
        if (tab != null) closeTab(tab);
    }

    // Utility methods for tab management

    /**
     * Load a tab with the given controller.
     * @param <U> the type of the controller
     * @param tab the tab to load into
     * @param controller the controller to associate with the tab
     */
    public static <U extends AbstractController> void loadTab(@NotNull Tab tab, @NotNull U controller) {
        Pane pane = (Pane) loadFxml(controller);
        tab.setContent(pane);
        tab.getProperties().put("controller", controller);
    }

    /**
     * Get the controller associated with a tab.
     * @param <U> the type of the controller
     * @param tab the tab
     * @return the controller associated with the tab
     */
    @SuppressWarnings("unchecked")
    public static <U extends AbstractController> U getController(@NotNull Tab tab) {
        return (U) tab.getProperties().get("controller");
    }
}
