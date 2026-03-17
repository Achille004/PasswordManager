/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras (2004marras@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.html.
 */

package password.manager.lib;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

import javafx.animation.Animation.Status;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;

/**
 * CustomPopup is a utility class for creating customizable popups in JavaFX applications.
 * It provides a builder pattern for easy construction and configuration of the popup, including options for alignment, spacing, fading animations, and stylesheets.
 * The popup can be shown or hidden with optional animations, and its state can be updated to display different messages and colors.
 */
public class CustomPopup extends Popup {

    private final Window owner;
    private final Alignment alignment;
    private final double spacing;

    private final Content content;

    private FadeTransition disappearTransition;

    private CustomPopup(Window owner, Alignment alignment, double spacing) {
        this.owner = owner;
        this.alignment = alignment;
        this.spacing = spacing;

        this.content = new Content();
        setup();
    }

    /**
     * Makes the popup visible immediately, stopping any ongoing disappear animation if necessary.
     * This is a shorthand for {@link #visible(boolean) visible(true)}.
     */
    public void visible() {
        visible(true);
    }

    /**
     * Makes the popup visible. If stopAnimation is true, any ongoing disappear animation will be stopped and the popup will become fully visible immediately.
     * @param stopAnimation Whether to stop any ongoing disappear animation while making the popup visible.
     */
    public void visible(boolean stopAnimation) {
        if (stopAnimation && disappearTransition != null) disappearTransition.stop();
        content.setVisible(owner.isFocused());
        content.setOpacity(1.0);
    }

    /**
     * Hides the popup immediately, playing the disappear animation if it is set.
     * This is a shorthand for {@link #hidden(boolean) hidden(true)}.
     */
    public void hidden() {
        hidden(true);
    }

    /**
     * Hides the popup. If playAnimation is true and a disappear animation is set, the animation will be played before the popup is hidden.
     * @param playAnimation Whether to play the disappear animation (if set) while hiding the popup.
     */
    public void hidden(boolean playAnimation) {
        if (playAnimation && disappearTransition != null) disappearTransition.playFromStart();
        else content.setVisible(false);
    }

    /**
     * Sets the state of the popup, including the displayed text and the color of the bottom bar. This can be used to indicate different states such as success or error.
     * @param text The text to display in the popup.
     * @param bottomBarColor The color to set for the bottom bar, specified as a CSS color value (e.g. "-fx-color-green" or "#00FF00").
     */
    public void setState(String text, String bottomBarColor) {
        content.setState(text, bottomBarColor);
    }

    // Used by constructor
    private void setup() {
        getContent().add(content);
        // DONT EVEN TRY REMOVING THESE PROPERTIES, TWO HOURS OF MY LIFE WASTED!!
        setAutoHide(false);
        setHideOnEscape(false);

        // Set position when height is first computed
        content.heightProperty().addListener((_, oldValue, newValue) -> {
            // Only listen when height is first set
            if(oldValue.doubleValue() != 0 || newValue.doubleValue() <= 0) return;

            // Get dimensions for position calculations
            final double WIDTH = content.getWidth();
            final double HEIGHT = content.getHeight();

            // Define position setters that can be reused for both initial set and listeners
            final BiConsumer<Double, Double> SET_X = (newX, newWidth) -> setX(alignment.calcX(newX, newWidth, WIDTH, spacing));
            final BiConsumer<Double, Double> SET_Y = (newY, newHeight) -> setY(alignment.calcY(newY, newHeight, HEIGHT, spacing));

            // Initial set
            SET_X.accept(owner.getX(), owner.getWidth());
            SET_Y.accept(owner.getY(), owner.getHeight());

            // Update position when owner moves or resizes
            owner.xProperty().addListener((_, _, newX) -> SET_X.accept(newX.doubleValue(), owner.getWidth()));
            owner.yProperty().addListener((_, _, newY) -> SET_Y.accept(newY.doubleValue(), owner.getHeight()));

            // Update position when owner resizes
            owner.widthProperty().addListener((_, _, newWidth) -> SET_X.accept(owner.getX(), newWidth.doubleValue()));
            owner.heightProperty().addListener((_, _, newHeight) -> SET_Y.accept(owner.getY(), newHeight.doubleValue()));
        });

        // Track focused state to prevent unwanted popup visibility changes
        owner.focusedProperty().addListener((_, _, isFocused) -> {
            boolean isVisible = content.isVisible();
            boolean shouldShow = disappearTransition == null || disappearTransition.getStatus() == Status.RUNNING;

            if (isVisible && !isFocused) {
                // Window lost focus while popup is visible - hide it
                content.setVisible(false);
            } else if (!isVisible && isFocused && shouldShow) {
                // Window gained focus while popup is hidden - show it
                content.setVisible(true);
            }
        });
    }

    // Used by builder
    private void setFadingAnimation(Duration fadeDuration) {
        this.disappearTransition = new FadeTransition(fadeDuration, content);
        this.disappearTransition.setFromValue(1.0);
        this.disappearTransition.setToValue(0.0);
        this.disappearTransition.setCycleCount(1);
        this.disappearTransition.setOnFinished(_ -> content.setVisible(false));
    }

    // Used by builder
    private void ready() {
        // This ensures that everything is actually computed
        content.applyCss();
        content.layout();
        super.show(owner);
    }

    /**
     * Builder class for constructing a CustomPopup with a fluent interface.
     * Use {@link Builder#create(Window, Alignment, double) Builder.create(owner, alignment, spacing)} to start building a CustomPopup.
     */
    public static class Builder {
        private CustomPopup popup;

        private Builder(Window owner, Alignment alignment, double spacing) {
            this.popup = new CustomPopup(owner, alignment, spacing);
        }

        /**
         * Starts building a CustomPopup with the specified owner, alignment, and spacing.
         * @param owner The window that the popup will be attached to.
         * @param alignment The alignment of the popup relative to the owner window.
         * @param spacing The spacing in pixels between the popup and the edges of the owner window, as determined by the alignment.
         * @return A new Builder instance.
         */
        public static Builder create(Window owner, Alignment alignment, double spacing) {
            if (owner == null) throw new IllegalArgumentException("Owner window cannot be null");
            if (alignment == null) throw new IllegalArgumentException("Alignment cannot be null");
            if (spacing < 0) throw new IllegalArgumentException("Spacing cannot be negative");
            return new Builder(owner, alignment, spacing);
        }

        /**
         * Sets a fading animation to be played when the popup is hidden. The animation will fade the popup out over the specified duration.
         * @param fadeDuration The duration of the fade-out animation.
         * @return The Builder instance, for chaining.
         */
        public Builder withFadingAnimation(Duration fadeDuration) {
            if (fadeDuration == null) throw new IllegalArgumentException("Fade duration cannot be null");
            if (fadeDuration.lessThanOrEqualTo(Duration.ZERO)) throw new IllegalArgumentException("Fade duration must be greater than zero");
            popup.setFadingAnimation(fadeDuration);
            return this;
        }

        /**
         * Adds the specified stylesheets to the popup's content. These stylesheets will be applied to the popup and can be used to customize its appearance.
         * @param stylesheets One or more stylesheet paths to add to the popup's content. These should be valid paths to CSS files.
         * @return The Builder instance, for chaining.
         */
        public Builder withStylesheets(String... stylesheets) {
            if (stylesheets == null || stylesheets.length == 0) throw new IllegalArgumentException("Stylesheets cannot be null or empty");
            popup.content.getStylesheets().addAll(stylesheets);
            return this;
        }

        /**
         * Finishes building the CustomPopup and returns the constructed instance.
         * This will also prepare the popup for display by computing its layout and showing it to ensure all dimensions are set.
         * @return The constructed CustomPopup instance.
         */
        public CustomPopup build() {
            popup.ready();
            return popup;
        }
    }

    /**
     * Defines the alignment of the popup relative to its owner window.
     * Each alignment option specifies how the popup should be positioned based on the owner's dimensions and the specified spacing.
     */
    @RequiredArgsConstructor
    public static enum Alignment {
        TOP_LEFT(Alignment::forwards, Alignment::forwards),
        TOP_RIGHT(Alignment::backwards, Alignment::forwards),
        BOTTOM_LEFT(Alignment::forwards, Alignment::backwards),
        BOTTOM_RIGHT(Alignment::backwards, Alignment::backwards);

        private final PositionCalculator CALCULATOR_X, CALCULATOR_Y;

        /**
         * Calculates the X coordinate for the popup based on the owner's position and dimensions, the popup's dimensions, and the specified spacing.
         * @param start The starting X coordinate of the owner window.
         * @param offset The width of the owner window.
         * @param itemSize The width of the popup.
         * @param spacing The spacing in pixels between the popup and the edge of the owner window, as determined by the alignment.
         * @return The calculated X coordinate for the popup.
         */
        public double calcX(double start, double offset, double itemSize, double spacing) {
            return CALCULATOR_X.calculate(start, offset, itemSize, spacing);
        }

        /**
         * Calculates the Y coordinate for the popup based on the owner's position and dimensions, the popup's dimensions, and the specified spacing.
         * @param start The starting Y coordinate of the owner window.
         * @param offset The height of the owner window.
         * @param itemSize The height of the popup.
         * @param spacing The spacing in pixels between the popup and the edge of the owner window, as determined by the alignment.
         * @return The calculated Y coordinate for the popup.
         */
        public double calcY(double start, double offset, double itemSize, double spacing) {
            return CALCULATOR_Y.calculate(start, offset, itemSize, spacing);
        }

        // Calculates the coordinate for the popup when aligned forwards (i.e. TOP_LEFT or BOTTOM_LEFT for X, TOP_LEFT or TOP_RIGHT for Y).
        // This is done by adding the specified spacing to the starting coordinate.
        private static double forwards(double start, double offset, double itemSize, double spacing) {
            return start + spacing;
        }

        // Calculates the coordinate for the popup when aligned backwards (i.e. TOP_RIGHT or BOTTOM_RIGHT for X, BOTTOM_LEFT or BOTTOM_RIGHT for Y).
        // This is done by starting from the end of the owner window (start + offset) and subtracting the popup's size and the specified spacing.
        private static double backwards(double start, double offset, double itemSize, double spacing) {
            return start + offset - itemSize - spacing;
        }

        // Used as a functional interface for calculating popup positions
        private interface PositionCalculator {
            double calculate(double start, double offset, double itemSize, double spacing);
        }
    }

    private static class Content extends AnchorPane implements Initializable {
        @FXML
        private Label label;

        @FXML
        private AnchorPane bottomBar;

        @Override
        public void initialize(URL location, ResourceBundle resources) {
            assert label != null : "fx:id=\"label\" was not injected.";
            assert bottomBar != null : "fx:id=\"bottomBar\" was not injected.";
        }

        protected Content() {
            FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/customPopup/index.fxml")));
            fxmlLoader.setRoot(this);
            fxmlLoader.setController(this);
            fxmlLoader.setClassLoader(getClass().getClassLoader());

            try {
                fxmlLoader.load();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        // Sets the text and color of the popup. This is used by the CustomPopup to show or hide the popup.
        protected void setState(String text, String bottomBarColor) {
            label.setText(text);
            bottomBar.setStyle("-fx-background-color: " + bottomBarColor + ";");
        }
    }
}