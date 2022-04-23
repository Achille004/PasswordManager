/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022  Francesco Marras

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package main;

import mareas.account.octabit.OctabitEncriptedAccount;

/**
 * @author FrA
 */
public class Account extends OctabitEncriptedAccount {

    private String software;

    /**
     * Constructor that directly sets account iformations.
     *
     * @param software The software.
     * @param username The username.
     * @param password The password.
     */
    public Account(String software, String username, String password) {
        super(username, password);
        this.software = software;
    }

    /**
     * Gets the Account's software.
     *
     * @return The software.
     */
    public String getSoftware() {
        return software;
    }

    /**
     * Sets the Account's software.
     *
     * @param software The new software.
     */
    public void setSoftware(String software) {
        this.software = software;
    }
}
