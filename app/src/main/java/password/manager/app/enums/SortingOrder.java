/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2025  Francesco Marras (2004marras@gmail.com)

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

package password.manager.app.enums;

import java.util.Comparator;
import java.util.function.BinaryOperator;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import password.manager.app.security.Account;

@Getter
@RequiredArgsConstructor
public enum SortingOrder {
    SOFTWARE(
        "software", (software, username) -> software + "\n" + username,
        CachedComparators.SOFTWARE_COMPARATOR.thenComparing(CachedComparators.USERNAME_COMPARATOR)
    ),
    USERNAME(
        "username", (software, username) -> username + "\n" + software,
        CachedComparators.USERNAME_COMPARATOR.thenComparing(CachedComparators.SOFTWARE_COMPARATOR)
    );

    private final String i18nKey;
    private final BinaryOperator<String> converter;
    private final Comparator<Account> comparator;

    public String convert(String software, String username) {
        return converter.apply(software, username);
    }

    public String convert(@NotNull Account account) {
        return convert(account.getSoftware(), account.getUsername());
    }

    /**
     * Inner class to hold comparators to avoid creating them multiple times.
     */
    private static class CachedComparators {
        private static final Comparator<Account> SOFTWARE_COMPARATOR = Comparator.comparing(Account::getSoftware, String.CASE_INSENSITIVE_ORDER);
        private static final Comparator<Account> USERNAME_COMPARATOR = Comparator.comparing(Account::getUsername, String.CASE_INSENSITIVE_ORDER);
    }
}
