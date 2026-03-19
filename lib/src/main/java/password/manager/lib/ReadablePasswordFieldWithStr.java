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

import javafx.beans.property.BooleanProperty;

public class ReadablePasswordFieldWithStr extends CustomPasswordField implements AnimationAwareControl {

    private final ReadablePasswordFieldWithStrSkin skin;

    public ReadablePasswordFieldWithStr() {
        skin = new ReadablePasswordFieldWithStrSkin(this);
        setSkin(skin);
    }

    ///// PASSWORD INPUT CONTROL METHODS /////

    public BooleanProperty readableProperty() {
        return skin.readableProperty();
    }

    public void setReadable(boolean readable) {
        skin.setReadable(readable);
    }

    public void toggleReadable() {
        skin.toggleReadable();
    }

    public boolean isReadable() {
        return skin.isReadable();
    }

    ///// ANIMATION AWARE CONTROL METHODS /////

    @Override
    public AnimationController<?> getAnimationController() {
        return skin.getAnimationController();
    }

    @Override
    public void onListenerDetached() {
        skin.onListenerDetached();
    }

    @Override
    public void onListenerAttached() {
        skin.onListenerAttached();
    }
}
