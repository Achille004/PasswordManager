/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras

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

package main.enums;

import java.util.Comparator;
import java.util.function.BiFunction;

import main.security.Account;

public enum SavingOrder {
    Software("software", (software, username) -> software + " / " + username, (acc1, acc2) -> {
        int software = acc1.getSoftware().compareTo(acc2.getSoftware());
        return (software == 0) ? acc1.getUsername().compareTo(acc2.getUsername()) : software;
    }),
    Username("username", (software, username) -> username + " / " + software, (acc1, acc2) -> {
        int username = acc1.getUsername().compareTo(acc2.getUsername());
        return (username == 0) ? acc1.getSoftware().compareTo(acc2.getSoftware()) : username;
    });

    private final String i18nKey;
    private final BiFunction<String, String, String> converter;
    private final Comparator<Account> comparator;

    private SavingOrder(String i18nKey, BiFunction<String, String, String> converter, Comparator<Account> comparator) {
        this.i18nKey = i18nKey;
        this.converter = converter;
        this.comparator = comparator;
    }

    public String i18nKey() {
        return i18nKey;
    }

    public BiFunction<String, String, String> getConverter() {
        return converter;
    }

    public String convert(String software, String username) {
        return converter.apply(software, username);
    }

    public String convert(Account account) {
        return this.convert(account.getSoftware(), account.getUsername());
    }

    public Comparator<Account> getComparator() {
        return comparator;
    }
}
