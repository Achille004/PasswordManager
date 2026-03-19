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

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Cursor;
import javafx.scene.control.PasswordField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ReadablePasswordFieldSkin extends TextFieldSkin {

    public static final char BULLET = '\u25cf';

    private final ImageView actionIcon = new ImageView();
    private final BooleanProperty readable = new SimpleBooleanProperty(false);

    private final ChangeListener<Boolean> readableListener;

    public ReadablePasswordFieldSkin(CustomPasswordField control) {
        this(control, false);
    }

    public ReadablePasswordFieldSkin(CustomPasswordField control, boolean initialReadable) {
        super(control);
        readable.set(initialReadable);

        actionIcon.setPreserveRatio(true);
        actionIcon.setManaged(false);
        actionIcon.setFocusTraversable(false);
        actionIcon.setPickOnBounds(true);

        actionIcon.setCursor(Cursor.HAND);
        actionIcon.setVisible(true);
        actionIcon.toFront();

        actionIcon.setImage(readable.get() ? Icons.HIDDEN.getImage() : Icons.SHOWING.getImage());
        actionIcon.setOnMouseClicked(_ -> toggleReadable());
        getChildren().add(actionIcon);

        this.readableListener = (_, _, isReadable) ->{
            actionIcon.setImage(isReadable
                ? Icons.HIDDEN.getImage()
                : Icons.SHOWING.getImage()
            );

            control.setText(control.getText());
            control.end();
        };
        readable.addListener(readableListener);
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);

        double iconSize = snapSizeX(getSkinnable().getHeight());
        double spacing = (h - iconSize) / 2;

        double buttonX = snapPositionX(x + w - iconSize - spacing / 2);
        double buttonY = snapPositionY(y + spacing);

        actionIcon.resizeRelocate(buttonX, buttonY, iconSize, iconSize);
        actionIcon.setFitWidth(iconSize);
        actionIcon.setFitHeight(iconSize);
    }

    @Override
    protected String maskText(String txt) {
        boolean isReadable = readable != null && readable.get();
        return (getSkinnable() instanceof PasswordField && !isReadable)
                ? super.maskText(txt)
                : txt;
    }

    @Override
    public void dispose() {
        readable.removeListener(readableListener);
        actionIcon.setOnMouseClicked(null);
        super.dispose();
    }

    private enum Icons {
        SHOWING("/icons/open-eye.png"),
        HIDDEN("/icons/closed-eye.png");

        private final Image image;

        Icons(String path) {
            this.image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
        }

        public Image getImage() {
            return image;
        }
    }

    ///// PASSWORD INPUT CONTROL METHODS /////

    public BooleanProperty readableProperty() {
        return readable;
    }

    public void setReadable(boolean readable) {
        // Use XOR to avoid unnecessary updates
        if (readable ^ isReadable()) {
            this.readable.set(readable);
        }
    }

    public boolean isReadable() {
        return readable.get();
    }

    public void toggleReadable() {
        setReadable(!isReadable());
    }
}
