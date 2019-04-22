/*
 * The MIT License
 *
 * Copyright 2019 Hilmar Ackermann.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import assembler.CodeCreation;
import assembler.ControlFlowSegments;
import assembler.Optimizer;
import assembler.Parser;
import interpreter.SimpleInterpreter;
import java.io.File;
import utils.SimpleTextFile;

public class Main {
    
    public static void main (String [] args) {
        final String pathInput = (args.length > 0 && new File(args[0]).exists() ? args[0] : null);
        String pathOutput = null;
        Boolean interpretResult = null;
        Boolean showResult = null;
        boolean containsInvalidArgs = false;
        for (int i=1; i<args.length; i++) {
            if (args[i].equals("-o") && i+1<args.length && pathOutput == null) {
                pathOutput = args[i+1];
                i++;
            } else if (args[i].equals("-e") && interpretResult == null)
                interpretResult = true;
            else if (args[i].equals("-p") && showResult == null)
                showResult = true;
            else
                containsInvalidArgs = true;
        }
        if (showResult == null)
            showResult = false;
        if (interpretResult == null)
            interpretResult = false;
        
        if (containsInvalidArgs || pathInput == null || (!interpretResult && !showResult && pathOutput == null)) {
            System.out.println("Usage: [input-file] [options]");
            System.out.println("");
            System.out.println("Options:");
            System.out.println("   -o [FILE]   save the translated brainfuck-code in a file");
            System.out.println("   -p          print the translated brainfuck-code on the console");
            System.out.println("   -e          interpret the translated brainfuck-code in a built-in brainfuck-interpreter");
            System.out.println("               the cells of this interpreter are from 0 to Inf, containing bytes");
            System.out.println("");
            System.out.println("Example usage:");
            System.out.println("   java Main samples/fibonacci.asm -o fibonacci.bf");
            System.out.println("   java Main samples/fibonacci.asm -e");
        } else {
            Parser parser = new Parser(pathInput);
            CodeCreation code = new CodeCreation(parser);
            String bfCode = Optimizer.optimizeSimple(new ControlFlowSegments(code).createMainLoop(), true);
            
            if (showResult)
                System.out.println(bfCode);
            if (pathOutput != null) {
                SimpleTextFile file = new SimpleTextFile();
                file.lines.add(bfCode);
                file.write(pathOutput);
            }
            
            if (interpretResult)
                new SimpleInterpreter().execute(bfCode);
        }
    }
    
}
