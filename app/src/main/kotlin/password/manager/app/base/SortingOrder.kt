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
package password.manager.app.base

import password.manager.app.security.Account
import java.util.function.BinaryOperator
import java.util.function.Function
import kotlin.Comparator
import kotlin.plus

/**
 * Enum representing the sorting order of accounts.
 * @param i18nKey the key used for internationalization
 * @param converter function to convert two strings into a single one
 * @param comparator comparator to use for sorting
 */
enum class SortingOrder(
    val i18nKey: String,
    val converter: BinaryOperator<String>,
    val comparator: Comparator<Account?>
) {
    SOFTWARE(
        "software", BinaryOperator { software: String?, username: String? -> software + "\n" + username },
        CachedComparators.SOFTWARE_COMPARATOR.thenComparing(CachedComparators.USERNAME_COMPARATOR)
    ),
    USERNAME(
        "username", BinaryOperator { software: String?, username: String? -> username + "\n" + software },
        CachedComparators.USERNAME_COMPARATOR.thenComparing(CachedComparators.SOFTWARE_COMPARATOR)
    );

    fun convert(software: String, username: String) = converter.apply(software, username)
    fun convert(acc: Account) = convert(acc.software, acc.username)

    /**
     * Inner class to hold comparators to avoid creating them multiple times.
     */
    private object CachedComparators {
        val SOFTWARE_COMPARATOR: Comparator<Account> = Comparator.comparing(
            Account::getSoftware ,
            String.CASE_INSENSITIVE_ORDER
        )
        val USERNAME_COMPARATOR: Comparator<Account> = Comparator.comparing(
            Account::getUsername,
            String.CASE_INSENSITIVE_ORDER
        )
    }
}
