# Knife
Knife is a tool that reads input grammar in BNF format and converts it to a few Java classes that can parse the given grammar through a simple interface.

Knife doesn't require any external libraries or dependencies. All generation is done ahead-of-time. After generating the parsing classes you can just copy them into your project.

Also, as other good parser generation tools, knife uses itself to read the input grammar.

## Features

* No runtime dependencies, knife generates pure Java code that can easily be ported to other JVM-based languages.
* Parsing is done using push-down automata without recursion.
* Knife uses an explicit API for accepting the token stream. It allows you to easily use knife with any (including your own) lexer. You can pause and resume parsing at any point. Parsing multiple token streams simultaneously is also possible.
* No complete parse-trees are being built during parsing. Reduction of the tree is done on-the-fly for performance. Optimized AST's can be built during parsing with minimal overhead.
* If your grammar is left-recursive without `A =>* A` derivations (aka without cycles), knife will generate an equivalent grammar without left recursion for you.

## Limitations

* Knife generates only **top-down** parsers for **LL(1)** grammars. Please note that many grammars can be converted to LL(1) grammars by eliminating left recursion and left factoring. As already mentioned above, knife will help you with left recursion elimination.

## Getting Started

1. Download the latest `.jar` file from the [releases](https://github.com/ZeroBone/Knife/releases) page.
2. Run `java -jar knife.jar grammar.kn` where `grammar.kn` is your grammar file.
3. Knife will generate the parsing classes in the same directory.

### Grammar file syntax

Knife accepts grammar files in BNF (Backus-Naur-Form) format with productions in the following syntax:

#### Terminals and non-terminals

```
non_terminal = TERMINAL non_terminal TERMINAL_WITH_ARG(argument); { java code }
```

The way knife distinguished between terminals and non-terminals is by looking at the first character. If it is a capital letter, the identifier identifies a terminal symbol, otherwise a non-terminal.

The first non-terminal declared in the grammar file will be the starting symbol of the grammar. Further ordering doesn't matter. It is important, that all declared non-terminals are being used in some production and that they are derivable from the start symbol.

#### Arguments

Argument can be attached to any gramma symbol (terminal or non-terminal) if you need to access it's payload in the code attached to the production.

The production label cannot have an argument. In order to assign a value to it, assign it to `v`. For example:

```
if_stmt = IF LPAREN expr(e) RPAREN stmt(ifbody) ELSE stmt(elsebody); {
	v = new IfStatement(e, ifbody, elseBody);
}
```

#### Type statements

By default the type associated to all grammar symbols is `java.lang.Object`. In order to avoid a lot of unsafe type castings in the production code blocks you can use following syntax to assign a type to a symbol:

```
%type if_stmt IfStatement
```

Everywhere where the typed symbol will be used, the corresponding argument name will have the specified type.

### Example

This example parses prefix arithmetic expression with operations `+`, `-` and `*`.

Grammar (file `prefix.kn`):

```
%type expr Integer
%type NUM Integer

expr = NUM(n); { v = n; }
expr = PLUS expr(op1) expr(op2); { v = op1 + op2; }
expr = MINUS expr(op1) expr(op2); { v = op1 - op2; }
expr = MUL expr(op1) expr(op2); { v = op1 * op2; }
```

After `java -jar knife.jar prefix.kn` 2 files will be generated - `Parser.java` and `ParseNode.java`.

Example usage:

```java
package net.zerobone.knifeexample;

import net.zerobone.knifeexample.parser.Parser;

public class Main {

    public static void main(String[] args) {

        Parser parser = new Parser();

        // parsing expression + * 5 3 * 4 6
        // in infix notation: 5 * 3 + 4 * 6 = 39
        parser.parse(Parser.T_PLUS, "+");
        parser.parse(Parser.T_MUL, "*");
        parser.parse(Parser.T_NUM, 5);
        parser.parse(Parser.T_NUM, 3);
        parser.parse(Parser.T_MUL, "*");
        parser.parse(Parser.T_NUM, 4);
        parser.parse(Parser.T_NUM, 6);
        parser.parse(Parser.T_EOF, null);

        if (parser.successfullyParsed()) {
            System.out.println((int)parser.getValue()); // Output: 39
        }

    }
}
```

## Support

Please [open an issue](https://github.com/ZeroBone/Knife/issues) if you found a bug in Knife.

Any contribution support is very appreciated.

## Copyright

Copyright (c) 2020 Alexander Mayorov.

This software is licensed under the terms of the GNU General Public License, Version 3.

See the LICENCE file for more details.