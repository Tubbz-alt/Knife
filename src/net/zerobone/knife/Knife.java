package net.zerobone.knife;

import net.zerobone.knife.ast.TranslationUnitNode;
import net.zerobone.knife.ast.entities.ProductionSymbol;
import net.zerobone.knife.ast.statements.ProductionStatementNode;
import net.zerobone.knife.ast.statements.StatementNode;
import net.zerobone.knife.generator.Generator;
import net.zerobone.knife.generator.GeneratorContext;
import net.zerobone.knife.grammar.Grammar;
import net.zerobone.knife.grammar.table.ParsingTableConflict;
import net.zerobone.knife.grammar.verification.VerificationError;
import net.zerobone.knife.grammar.table.ParsingTable;
import net.zerobone.knife.grammar.Production;
import net.zerobone.knife.grammar.Symbol;
import net.zerobone.knife.lexer.Lexer;
import net.zerobone.knife.lexer.LexerException;
import net.zerobone.knife.lexer.tokens.Token;
import net.zerobone.knife.parser.KnifeParser;
import net.zerobone.knife.parser.ParseException;
import net.zerobone.knife.parser.Parser;
import net.zerobone.knife.parser.TokenMgrError;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class Knife {

    public static final String VERSION = "1.0.0-beta";

    private Grammar grammar;

    private Knife(Grammar grammar) {
        this.grammar = grammar;
    }

    private void handleErrors(ArrayList<VerificationError> errors) {

        boolean canBeAutoFixed = true;

        for (VerificationError error : errors) {

            System.err.print("Error: ");
            System.err.println(error.getMessage(grammar));

            if (!error.canBeAutoFixed) {
                canBeAutoFixed = false;
            }

        }

        if (!canBeAutoFixed) {
            return;
        }

        System.out.println("Attempting to resolve grammar issues...");

        // TODO: write this information to file

        System.out.println("Before eliminating epsilon productions:");
        System.out.println(grammar.toString());

        grammar.eliminateEpsilonProductions();

        System.out.println("Before eliminating left recursion:");
        System.out.println(grammar.toString());

        grammar.eliminateLeftRecursion();

        System.out.println("Eliminated left recursion:");
        System.out.println(grammar.toString());

    }

    private boolean tableHasErrors(ParsingTable table) {
        return table.conflicts.length != 0;
    }

    private void handleTableErrors(ParsingTable table) {

        for (ParsingTableConflict conflict : table.conflicts) {

            System.err.print("Conflict: ");
            System.err.println(conflict.getMessage());

        }

    }

    private void exportDebugInfo(ParsingTable table) throws IOException {

        BufferedWriter debugLogWriter = new BufferedWriter(new FileWriter("debug.log"));

        debugLogWriter.write("Grammar:");
        debugLogWriter.newLine();
        debugLogWriter.newLine();

        debugLogWriter.write(grammar.toString());

        debugLogWriter.newLine();
        debugLogWriter.newLine();

        debugLogWriter.write("First sets:");
        debugLogWriter.newLine();

        HashMap<Integer, HashSet<Integer>> firstSets = grammar.computeFirstSets();

        for (HashMap.Entry<Integer, HashSet<Integer>> entry : firstSets.entrySet()) {

            debugLogWriter.write("FIRST(");
            debugLogWriter.write(grammar.idToSymbol(entry.getKey()));
            debugLogWriter.write(") = {");

            Iterator<Integer> firstSetIterator = entry.getValue().iterator();

            if (firstSetIterator.hasNext()) {

                while (true) {

                    int id = firstSetIterator.next();

                    if (id != Grammar.FIRST_FOLLOW_SET_EPSILON) {

                        debugLogWriter.write(grammar.idToSymbol(id));

                    }

                    if (!firstSetIterator.hasNext()) {
                        break;
                    }

                    debugLogWriter.write(", ");

                }

            }

            debugLogWriter.write('}');

            debugLogWriter.newLine();

        }

        debugLogWriter.newLine();

        debugLogWriter.write("Follow sets:");
        debugLogWriter.newLine();

        HashMap<Integer, HashSet<Integer>> followSets = grammar.computeFollowSets();

        for (HashMap.Entry<Integer, HashSet<Integer>> entry : followSets.entrySet()) {

            debugLogWriter.write("FOLLOW(");
            debugLogWriter.write(grammar.idToSymbol(entry.getKey()));
            debugLogWriter.write(") = {");

            Iterator<Integer> followSetIterator = entry.getValue().iterator();

            if (followSetIterator.hasNext()) {

                while (true) {

                    int id = followSetIterator.next();

                    if (id == Grammar.FOLLOW_SET_EOF) {
                        debugLogWriter.write("$");
                    }
                    else if (id != Grammar.FIRST_FOLLOW_SET_EPSILON) {

                        debugLogWriter.write(grammar.idToSymbol(id));

                    }

                    if (!followSetIterator.hasNext()) {
                        break;
                    }

                    debugLogWriter.write(", ");

                }

            }

            debugLogWriter.write('}');

            debugLogWriter.newLine();

        }

        if (table.conflicts.length != 0) {

            debugLogWriter.newLine();

            // write conflicts
            debugLogWriter.write("Conflicts");
            debugLogWriter.newLine();

            for (ParsingTableConflict conflict : table.conflicts) {

                debugLogWriter.write(conflict.getMessage());
                debugLogWriter.newLine();

            }

        }

        // table
        debugLogWriter.newLine();

        debugLogWriter.write(table.toString());

        debugLogWriter.close();

    }

    private void run() {

        System.out.println("Knife v" + VERSION + " by Alexander Mayorov (https://github.com/ZeroBone/Knife).");

        ArrayList<VerificationError> verificationErrors = grammar.verify();

        if (!verificationErrors.isEmpty()) {
            handleErrors(verificationErrors);
            return;
        }

        ParsingTable table = grammar.constructParsingTable();

        if (tableHasErrors(table)) {
            handleTableErrors(table);
            return;
        }

        try {
            exportDebugInfo(table);
        }
        catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }

        GeneratorContext context = new GeneratorContext("net.zerobone.knife.parser", table);

        try {
            Generator.generate(context);
        }
        catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            return;
        }

        System.out.println("Parser generated successfully.");

    }

    private static Production convertProduction(ProductionStatementNode statement) {

        Production production = new Production(statement.code);

        for (ProductionSymbol symbol : statement.production) {
            production.append(new Symbol(symbol.id, symbol.terminal, symbol.argument));
        }

        return production;

    }

    public static void main(String[] args) {

        if (args.length == 1) {

            InputStream is;

            try {
                is = new FileInputStream(args[0]);
            }
            catch (FileNotFoundException e) {
                System.out.println("Error: File '" + args[0] + "' was not found!");
                return;
            }

            Lexer lexer = new Lexer(is);

            try {
                Token currentToken;
                do {
                    currentToken = lexer.lex();
                    if (currentToken.id == Parser.T_EOF) {
                        break;
                    }
                    System.out.println(currentToken.id);
                } while (true);
            }
            catch (LexerException | IOException e) {
                e.printStackTrace();
            }

            KnifeParser lang = new KnifeParser(is);
            TranslationUnitNode t;

            try {
                t = lang.translationUnit();
            }
            catch (ParseException | TokenMgrError e) {
                System.out.println(e.toString());
                return;
            }

            Grammar grammar = null;

            for (StatementNode stmt : t.statements) {

                if (stmt instanceof ProductionStatementNode) {

                    ProductionStatementNode production = (ProductionStatementNode)stmt;

                    if (grammar == null) {
                        grammar = new Grammar(production.nonTerminal, convertProduction(production));
                    }
                    else {
                        grammar.addProduction(production.nonTerminal, convertProduction(production));
                    }

                }

            }

            if (grammar == null) {
                System.out.println("Error: could not find start symbol in grammar.");
                return;
            }

            Knife knife = new Knife(grammar);

            knife.run();

            return;

        }

        System.out.println("Invalid arguments!");
        System.out.println("Usage: knife filename.kn");

    }

}