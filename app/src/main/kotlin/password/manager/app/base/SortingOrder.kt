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
import java.text.Collator
import java.util.function.BinaryOperator
import kotlin.Comparator

/**
 * Enum representing the sorting order of accounts.
 * @param i18nKey the key used for internationalization
 * @param converter function to convert two strings into a single one
 * @param comparator comparator to use for sorting
 */
enum class SortingOrder(
    val i18nKey: String,
    val converter: BinaryOperator<String>,
    val comparator: Function1<SupportedLocale, Comparator<Account>>
) {
    SOFTWARE(
        "software",
        { software: String, username: String -> "$software\n$username" },
        AccountComparatorFactory::softwareFirstComparator
    ),
    USERNAME(
        "username",
        { software: String, username: String -> "$username\n$software" },
        AccountComparatorFactory::usernameFirstComparator
    );

    fun convert(software: String, username: String) = converter.apply(software, username)
    fun convert(acc: Account) = convert(acc.software, acc.username)

    /**
     * Creates locale-aware, case-insensitive comparators for accounts.
     */
    private object AccountComparatorFactory {
        /**
         * Creates a case-insensitive collator for the given locale.
         */
        private fun caseInsensitiveCollator(suppLoc: SupportedLocale): Collator =
            Collator.getInstance(suppLoc.locale).apply {
                // Case-insensitive
                strength = Collator.SECONDARY
            }

        fun softwareFirstComparator(locale: SupportedLocale): Comparator<Account> {
            val collator = caseInsensitiveCollator(locale)
            return Comparator
                .comparing(Account::software, collator)
                .thenComparing(Account::username, collator)
        }
        fun usernameFirstComparator(locale: SupportedLocale): Comparator<Account> {
            val collator = caseInsensitiveCollator(locale)
            return Comparator
                .comparing(Account::username, collator)
                .thenComparing(Account::software, collator)
        }

    }
}
