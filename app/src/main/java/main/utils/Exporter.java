package main.utils;

import java.util.ArrayList;

import main.security.Account;

public class Exporter {
    /**
     * Exports a list of accounts in HTML.
     * 
     * @param accountList   The list of accounts.
     * @param language      The language of the header.
     * @param loginPassword The password used to decrypt.
     * @return The whole HTML text.
     */
    public static String exportHtml(ArrayList<Account> accountList, String language, String loginPassword) {
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

        switch (language) {
            case "e" -> {
                stb.append(
                        "<tr>\n<th>Account</th>\n<th>Software</th>\n<th>Username</th>\n<th>Password</th>\n</tr>");
            }

            case "i" -> {
                stb.append(
                        "<tr>\n<th>Account</th>\n<th>Software</th>\n<th>Nome Utente</th>\n<th>Password</th>\n</tr>");
            }

            default -> throw new IllegalArgumentException("Invalid language: " + language);
        }

        final int listSize = accountList.size();
        int counter = 0;
        for (Account currentAccount : accountList) {
            counter++;
            stb.append("<tr>\n<td>" + Utils.addZerosToIndex(listSize, counter) +
                    "</td>\n<td>" + currentAccount.getSoftware() +
                    "</td>\n<td>" + currentAccount.getUsername() +
                    "</td>\n<td>" + currentAccount.getPassword(loginPassword) +
                    "</td>\n</tr>");
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
    public static String exportCsv(ArrayList<Account> accountList, String loginPassword) {
        StringBuilder stb = new StringBuilder();

        final int listSize = accountList.size();
        int counter = 0;
        for (Account currentAccount : accountList) {
            counter++;
            stb.append(Utils.addZerosToIndex(listSize, counter) + "," +
                    currentAccount.getSoftware() + "," +
                    currentAccount.getUsername() + "," +
                    currentAccount.getPassword(loginPassword) + "\n");
        }

        return stb.toString();
    }
}