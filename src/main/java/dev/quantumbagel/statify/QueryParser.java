package dev.quantumbagel.statify;


import org.checkerframework.checker.units.qual.A;

import java.util.*;
public class QueryParser {
    static final String validChars = "abcdefghijklmnopqrstuvwyxz:_";
    public static void main(String[] args) {}
    public static HashMap<String, String> replaceCalculatedStats(HashMap<String, List<Integer>> statMap, String instruction) {
        HashMap<String, String> doTheMathReady = new HashMap<>();
        List<String> statsToFind = obtainStatsFromInstruction(instruction);
        for (Map.Entry<String, List<Integer>> entry: statMap.entrySet()) {
            List<Integer> statList = entry.getValue();
            String instructionCopy = instruction;
            int index = 0;
            boolean nullFound = false;
            for (Integer val: statList) {
                if (val == null) {
                    nullFound = true;
                    break;
                } else {
                    instructionCopy = instructionCopy.replace(statsToFind.get(index), statList.get(index).toString());
                }
                index ++;
            }
            if (!nullFound) {
                doTheMathReady.put(entry.getKey(), instructionCopy);
            }

        }
        return doTheMathReady;
    }
    public static List<String> obtainStatsFromInstruction(String instruction) {
        StringBuilder currentParse = new StringBuilder();
        List<String> requiredStats = new ArrayList<>();
        if (instruction == null) {
            return new ArrayList<>();
        }
        for (Character character: instruction.toCharArray()) {
            if (validChars.contains(character.toString().toLowerCase())) {
                currentParse.append(character);
            } else {
                if (!currentParse.isEmpty() && currentParse.toString().contains(":")) {
                    requiredStats.add(currentParse.toString());
                    currentParse = new StringBuilder();
                } else if (!currentParse.isEmpty() && !currentParse.toString().contains(":")) {
                    currentParse = new StringBuilder();
                }
            }
        }
        if (!currentParse.isEmpty()) {
            requiredStats.add(currentParse.toString());
        }
        return requiredStats;
    }

    public static HashMap<String, List<Integer>> calculateStatsToReplace(String instruction) {

        int currentCounter = 0;
        if (instruction == null) {
            return new HashMap<>();
        }
        List<String> requiredStats = obtainStatsFromInstruction(instruction);
        HashMap<String, List<Integer>> johnWick = new HashMap<>();
        HashMap<String, String> uToU = UserCacheReader.getUUIDtoUsernameDict();
        List<String> usernames = new ArrayList<>();
        List<String> legitUsernames = new ArrayList<>();
        for (Map.Entry<String, String> thing: uToU.entrySet()) {
            usernames.add(thing.getValue().toLowerCase());
            legitUsernames.add(thing.getValue());
        }
        for (String item: requiredStats) {
            String[] splitItem = item.split(":", -1);
            LinkedHashMap<String, Integer> ranking;
            if (usernames.contains(splitItem[0].toLowerCase())) {
                HashMap<String, String> hash = CustomFavorites.getAll(legitUsernames.get(usernames.indexOf(splitItem[0].toLowerCase())));
                String toParse = hash.get(splitItem[1]);
                HashMap<String, List<Integer>> iAm;
                try {
                    iAm = calculateStatsToReplace(toParse);
                } catch (StackOverflowError ignored) {
                    return new HashMap<>();
                }
                HashMap<String, String> doneWithCalc = replaceCalculatedStats(iAm, toParse);
                ranking = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry: doneWithCalc.entrySet()) {
                    Integer mathNum = (int) doTheMath(entry.getValue().replace("\"", ""));
                    ranking.put(entry.getKey(), mathNum);
                }
            } else {
                ranking = GetRanking.getRanking(splitItem[0], splitItem[1], true);
            }
            if (ranking.isEmpty()) {
                return new HashMap<>(); // there is NO SHOT of this working for ANY player, so just return now
            }
            for (Map.Entry<String, Integer> entry: ranking.entrySet()) {
                if (!johnWick.containsKey(entry.getKey())) {
                    johnWick.put(entry.getKey(), new ArrayList<>(){});
                    for (int i = 0; i < currentCounter; i++) {
                        johnWick.get(entry.getKey()).add(null);
                    }
                    johnWick.get(entry.getKey()).add(entry.getValue());
                } else {
                    johnWick.get(entry.getKey()).add(entry.getValue());
                }
            }
            for (Map.Entry<String, List<Integer>> thing: johnWick.entrySet()) {
                if (thing.getValue().size() < currentCounter + 1) {
                    for (int i = 0; i < (currentCounter + 1 - thing.getValue().size()); i++) {
                        johnWick.get(thing.getKey()).add(null);
                    }
                }
            }
            currentCounter++;
        }
        return johnWick;
    }
    public static double doTheMath(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            Double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                if (x == x) {// NaN check
                    return x;
                } else {
                    return null;
                }
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)` | number
            //        | functionName `(` expression `)` | functionName factor
            //        | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    if (!eat(')')) throw new RuntimeException("Missing ')'");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    if (eat('(')) {
                        x = parseExpression();
                        if (!eat(')')) throw new RuntimeException("Missing ')' after argument to " + func);
                    } else {
                        x = parseFactor();
                    }
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else if (func.equals("abs")) x = Math.abs(x);
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
}