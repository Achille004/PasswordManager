/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2023  Francesco Marras

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

package main.utils;

import java.util.ArrayList;

import main.security.Account;
import org.jetbrains.annotations.NotNull;

import static main.utils.Utils.*;

public class Exporter {
    /**
     * Exports a list of accounts in HTML.
     * 
     * @param accountList   The list of accounts.
     * @param language      The language of the header.
     * @param loginPassword The password used to decrypt.
     * @return The whole HTML text.
     */
    public static @NotNull String exportHtml(ArrayList<Account> accountList, String language, String loginPassword) {
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

        stb.append(switch (language) {
            case "e" -> "<tr>\n<th>Account</th>\n<th>Software</th>\n<th>Username</th>\n<th>Password</th>\n</tr>";
            case "i" -> "<tr>\n<th>Account</th>\n<th>Software</th>\n<th>Nome Utente</th>\n<th>Password</th>\n</tr>";
            default -> throw new IllegalArgumentException("Invalid language: " + language);
        });

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
    }

    /**
     * Exports a list of accounts in CSV.
     * 
     * @param accountList   The list of accounts.
     * @param loginPassword The password used to decrypt.
     * @return The whole CSV text.
     */
    public static @NotNull String exportCsv(@NotNull ArrayList<Account> accountList, String loginPassword) {
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
    }
}