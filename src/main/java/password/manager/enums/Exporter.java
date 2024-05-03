/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras (2004marras@gmail.com)

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

package password.manager.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import password.manager.inerfaces.TriFunction;
import password.manager.security.Account;
import password.manager.utils.ObservableResourceFactory;

import static password.manager.utils.Utils.addZerosToIndex;

import org.jetbrains.annotations.NotNull;

import javafx.collections.ObservableList;

@RequiredArgsConstructor
public enum Exporter {

    HTML((accountList, langResources, loginPassword) -> {
        StringBuilder stb = new StringBuilder();

        stb.append("<!DOCTYPE html>\n<html>\n<style>\n");

        // css
        stb.append("""
                body {
                    background-color: rgb(51,51,51);
                    color: rgb(204,204,204);
                    margin: 1em;
                }

                table, th, td {
                    border: 0.1em solid rgb(204,204,204);
                    border-collapse: collapse;
                }

                th, td {
                    padding: 1em;
                }
                """);

        stb.append("\n</style>\n\n<body>\n<table style=\"width:100%\">");
        stb.append("<tr>\n<th>").append(langResources.getValue("account"))
                .append("</th>\n<th>").append(langResources.getValue("software"))
                .append("</th>\n<th>").append(langResources.getValue("username"))
                .append("</th>\n<th>").append(langResources.getValue("password"))
                .append("</th>\n</tr>");

        final int listSize = accountList.size();
        int counter = 0;
        for (Account currentAccount : accountList) {
            counter++;
            stb
                    .append("<tr>\n<td>")
                    .append(addZerosToIndex(listSize, counter)).append("</td>\n<td>")
                    .append(currentAccount.getSoftware()).append("</td>\n<td>")
                    .append(currentAccount.getUsername()).append("</td>\n<td>")
                    .append(currentAccount.getPassword(loginPassword)).append("</td>\n</tr>");
        }

        stb.append("</table>\n</body>\n</html>");

        return stb.toString();
    }),
    CSV((accountList, langResources, loginPassword) -> {
        StringBuilder stb = new StringBuilder();

        final int listSize = accountList.size();
        int counter = 0;
        for (Account currentAccount : accountList) {
            counter++;
            stb
                    .append(addZerosToIndex(listSize, counter)).append(",")
                    .append(currentAccount.getSoftware()).append(",")
                    .append(currentAccount.getUsername()).append(",")
                    .append(currentAccount.getPassword(loginPassword)).append("\n");
        }

        return stb.toString();
    });

    private final @Getter TriFunction<@NotNull ObservableList<Account>, ObservableResourceFactory, String, String> exporter;
}