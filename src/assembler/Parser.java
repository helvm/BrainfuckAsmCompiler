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
package assembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import utils.SimpleTextFile;
import utils.Utils;

public class Parser {
    
    public enum CmdType {Mov, Add, Sub, Mul, Div, Jmp, Jnz, Jz, Jne, Je, Jle, Jl, Jge, Jg, Out, In, Push, Pop, Call, Ret};
    public enum ArgType {Reg, Const, MemAddr, MemAddrConst, Label};
    
    public final Cmd [] commands;
    public final int [] labelOffsets;
    public final int numberOfRegisters, lastNRegistersReserved;
    public final Map <Integer, Integer> globalMemory;
    public final int stackSize;
    
    public final Map <Integer, Integer> regIndex2RawIndex = new HashMap<>();
    public final Map <Integer, Integer> rawIndex2RegIndex = new HashMap<>();

    public Parser(Cmd[] commands, int[] labelOffsets, int numberOfRegisters, int lastNRegistersReserved, Map<Integer, Integer> globalMemory, int stackSize) {
        this.commands = commands;
        this.labelOffsets = labelOffsets;
        this.numberOfRegisters = numberOfRegisters;
        this.lastNRegistersReserved = lastNRegistersReserved;
        this.globalMemory = globalMemory;
        this.stackSize = stackSize;
    }
    
    public Parser (String path) {
        globalMemory = new HashMap<>();
        Map <String, Integer> label2Index = new HashMap<>();
        Map <Integer, Integer> label2Offset = new HashMap<>();
        List <Cmd> commandList = new ArrayList<>();
        
        AtomicBoolean inMultiComment = new AtomicBoolean(false);
        SimpleTextFile file = new SimpleTextFile(path);
        if (file.lines.isEmpty())
            error("empty file.", "", 1);
        Integer _stackSize = null;
        for (int i=0; i<file.lines.size(); i++) {
            final String line = file.lines.get(i);
            String lineW = line.trim();
            final int lineCommentIndex = lineW.indexOf("//");
            if (lineCommentIndex != -1)
                lineW = lineW.substring(0, lineCommentIndex);
            lineW = applyMultiComment(lineW, inMultiComment);
            if (lineW.equals("")) {}
            else {
                CmdType foundCmd = null;
                for (CmdType cmd : CmdType.values())
                    if (lineW.startsWith(cmd.name().toLowerCase())) {
                        final String remaining = lineW.substring(cmd.name().length());
                        if (remaining.trim().equals("") || remaining.charAt(0) == ' ' || remaining.charAt(0) == '\t') {
                            foundCmd = cmd;
                            lineW = remaining;
                            break;
                        }
                    }
                
                if (foundCmd != null) {
                    int oldLineLength = lineW.length();
                    lineW = lineW.trim();
                    final int expectedArguments = (foundCmd == CmdType.Ret ? 0 : (foundCmd == CmdType.In || foundCmd == CmdType.Out || foundCmd == CmdType.Jmp || foundCmd == CmdType.Push || foundCmd == CmdType.Pop || foundCmd == CmdType.Call ? 1 : (foundCmd == CmdType.Mul || foundCmd == CmdType.Je || foundCmd == CmdType.Jne || foundCmd == CmdType.Jl || foundCmd == CmdType.Jle || foundCmd == CmdType.Jg || foundCmd == CmdType.Jge ? 3 : (foundCmd == CmdType.Div ? 4 : 2))));
                    if (oldLineLength == lineW.length() && expectedArguments != 0)
                        error("white-space after command expected.", line, i+1);
                    String [] tokens = (expectedArguments > 1 ? lineW.split(",") : new String [] {lineW});
                    if ((expectedArguments == 0 && !tokens[0].trim().equals("")) || (expectedArguments > 0 && tokens.length != expectedArguments))
                        error("invalid number of arguments, expected: " + expectedArguments + ".", line, i+1);
                    Arg [] args = new Arg[expectedArguments];
                    for (int j=0; j<args.length; j++)
                        args[j] = new Arg(tokens[j].trim(), label2Index, rawIndex2RegIndex, regIndex2RawIndex, line, i+1);
                    
                    commandList.add(new Cmd(foundCmd, args, line, i+1));
                } else if (lineW.endsWith(":")) {
                    final String labelName = lineW.substring(0, lineW.length()-1);
                    for (int j=0; j<labelName.length(); j++)
                        if (!((labelName.charAt(j) >= 'A' && labelName.charAt(j) <= 'Z') || labelName.charAt(j) == '_' || (labelName.charAt(j) >= 'a' && labelName.charAt(j) <= 'z') || (j > 0 && labelName.charAt(j) >= '0' && labelName.charAt(j) <= '9')))
                            error("invalid characters in label (just A-Z a-z _ 0-9).", line, i+1);
                    if (!label2Index.containsKey(labelName))
                        label2Index.put(labelName, label2Index.size());
                    final int labelIndex = label2Index.get(labelName);
                    if (label2Offset.containsKey(labelIndex))
                        error("label already exists.", line, i+1);
                    label2Offset.put(labelIndex, commandList.size());
                } else if (lineW.startsWith("global")) {
                    lineW = lineW.substring("global".length());
                    int oldLineLength = lineW.length();
                    lineW = lineW.trim();
                    if (oldLineLength == lineW.length())
                        error("white-space after command expected.", line, i+1);
                    
                    final int firstWhitespace0 = lineW.indexOf(" ");
                    final int firstWhitespace1 = lineW.indexOf("\t");
                    final int firstWhitespace = (firstWhitespace0 != -1 && firstWhitespace1 != -1 ? Math.min(firstWhitespace0, firstWhitespace1) : Math.max(firstWhitespace0, firstWhitespace1));
                    if (firstWhitespace == -1)
                        error("argument-list or string of global-memory expected.", line, i+1);
                    Integer memOffset = parseInt(lineW.substring(0, firstWhitespace));
                    if (memOffset == null)
                        error("could not parse memory offset.", line, i+1);
                    lineW = lineW.substring(firstWhitespace, lineW.length()).trim();
                    int [] memContent = null;
                    if (lineW.length() > 0 && lineW.charAt(0) == '"') {
                        memContent = parseString(lineW);
                        if (memContent == null)
                            error("could not parse string.", line, i+1);
                    } else {
                        String [] tokens = lineW.split(",");
                        memContent = new int [tokens.length];
                        if (tokens.length == 0)
                            error("argument-list or string of global-memory expected.", line, i+1);
                        for (int j=0; j<memContent.length; j++) {
                            Integer intV = parseInt(tokens[j].trim());
                            if (intV == null)
                                error("could not parse an entry of the integer list.", line, i+1);
                            memContent[j] = intV;
                        }
                    }
                    for (int j=0; j<memContent.length; j++) {
                        if (globalMemory.containsKey(memOffset+j))
                            error("global memory at position " + (memOffset+j) + " already set.", line, i+1);
                        globalMemory.put(memOffset+j, memContent[j]);
                    }
                } else if (lineW.startsWith("stacksize")) {
                    lineW = lineW.substring("stacksize".length());
                    int oldLineLength = lineW.length();
                    lineW = lineW.trim();
                    if (oldLineLength == lineW.length())
                        error("white-space after command expected.", line, i+1);
                    
                    Integer _stackSizeParsed = parseInt(lineW);
                    if (_stackSizeParsed == null || _stackSizeParsed < 0)
                        error("could not parse stacksize.", line, i+1);
                    if (_stackSize != null)
                        error("stacksize already set.", line, i+1);
                    _stackSize = _stackSizeParsed;
                } else
                    error("invalid line. (unknown command)", line, i+1);
            }
        }
        if (inMultiComment.get())
            error("end of file reached but still in multi-line comment.", file.lines.get(file.lines.size()-1), file.lines.size()-1);
        
        commands = commandList.toArray(new Cmd[commandList.size()]);
        labelOffsets = new int [label2Index.size()];
        for (int i=0; i<labelOffsets.length; i++) {
            Integer offset = label2Offset.get(i);
            if (offset == null)
                error("label '" + getLabelName(i, label2Index) + "' not found, but referenced.", file.lines.get(file.lines.size()-1), file.lines.size()-1);
            labelOffsets[i] = offset;
        }
        
        numberOfRegisters = rawIndex2RegIndex.size();
        lastNRegistersReserved = 0;
        stackSize = (_stackSize == null ? 16 : _stackSize);
    }
    
    public void debugOutput () {
        for (int i=0; i<commands.length; i++) {
            for (int j=0; j<labelOffsets.length; j++)
                if (labelOffsets[j] == i)
                    System.out.println(" -> {" + j + "}");
            System.out.println(commands[i].toString());
        }
    }
    
    private static String applyMultiComment (String in, AtomicBoolean inMultiComment) {
        boolean [] inComment = new boolean [in.length()];
        for (int i=0; i<in.length(); i++) {
            inComment[i] = inMultiComment.get();
            if (inMultiComment.get() && i > 0 && in.charAt(i-1) == '*' && in.charAt(i) == '/')
                inMultiComment.set(false);
            else if (!inMultiComment.get() && i+1 < in.length() && in.charAt(i) == '/' && in.charAt(i+1) == '*') {
                inMultiComment.set(true);
                inComment[i] = true;
            }
        }
        StringBuilder ret = new StringBuilder();
        for (int i=0; i<inComment.length; i++)
            if (!inComment[i])
                ret.append(in.charAt(i));
        return ret.toString();
    }
    
    private static String getLabelName (int labelIndex, Map <String, Integer> label2Index) {
        for (String label : label2Index.keySet())
            if (label2Index.get(label).equals(labelIndex))
                return label;
        throw new RuntimeException("internal error.");
    }
    
    public static void error (String msg, String line, int lineNumber) {
        if (line == null)
            throw new RuntimeException("internal error.");
        
        throw new RuntimeException(msg + "  (in line: " + lineNumber + " -- '" + line + "')");
    }
    
    public static class Cmd {
        
        public final CmdType cmdType;
        public final Arg [] args;

        public Cmd(CmdType cmdType, Arg[] args) {
            this(cmdType, args, null, -1);
        }
        
        public Cmd(CmdType cmdType, Arg[] args, String line, int lineNumber) {
            this.cmdType = cmdType;
            this.args = args;
            
            if (!isValid())
                error("invalid type of arguments. (not compatible with the given instruction)", line, lineNumber);
            
            Set <Integer> regIndicesUsed = new HashSet<>();
            for (int i=0; i<args.length; i++)
                if ((args[i].argType == ArgType.Reg || args[i].argType == ArgType.MemAddr) && !regIndicesUsed.add(args[i].regIndex))
                    error("multiple use of the same register in one instruction.", line, lineNumber);
        }
        
        public boolean isJumpInstruction () {
            return cmdType == CmdType.Jmp || cmdType == CmdType.Jz || cmdType == CmdType.Jnz || cmdType == CmdType.Je || cmdType == CmdType.Jne || cmdType == CmdType.Jl || cmdType == CmdType.Jle || cmdType == CmdType.Jg || cmdType == CmdType.Jge || cmdType == CmdType.Call || cmdType == CmdType.Ret;
        }
        
        private boolean isValid () {
            switch (cmdType) {
                case Mov:
                    return (args[0].argType == ArgType.Reg ? (args[1].argType != ArgType.Label) : (args[0].argType == ArgType.MemAddr || args[0].argType == ArgType.MemAddrConst ? (args[1].argType != ArgType.Label && args[1].argType != ArgType.MemAddr && args[1].argType != ArgType.MemAddrConst) : false));
                case Add:
                case Sub:
                    return (args[0].argType == ArgType.Reg && (args[1].argType == ArgType.Reg || args[1].argType == ArgType.Const));
                case Mul:
                    return (args[0].argType == ArgType.Reg && args[1].argType == ArgType.Reg && args[2].argType == ArgType.Reg);
                case Div:
                    return (args[0].argType == ArgType.Reg && args[1].argType == ArgType.Reg && args[2].argType == ArgType.Reg && args[3].argType == ArgType.Reg);
                case Jmp:
                    return (args[0].argType == ArgType.Label);
                case Jz:
                case Jnz:
                    return (args[0].argType == ArgType.Reg && args[1].argType == ArgType.Label);
                case Je:
                case Jne:
                case Jl:
                case Jle:
                case Jg:
                case Jge:
                    return (args[0].argType == ArgType.Reg && args[1].argType == ArgType.Reg && args[2].argType == ArgType.Label);
                case Out:
                    return (args[0].argType == ArgType.Reg || args[0].argType == ArgType.Const);
                case In:
                    return (args[0].argType == ArgType.Reg);
                case Push:
                    return (args[0].argType == ArgType.Reg || args[0].argType == ArgType.Const);
                case Pop:
                    return (args[0].argType == ArgType.Reg);
                case Call:
                    return (args[0].argType == ArgType.Label);
                case Ret:
                    return true;
                default:
                    throw new RuntimeException("internal error.");
            }
        }
        
        @Override
        public String toString () {
            final String argStr = Arrays.toString(args);
            return cmdType.name().toLowerCase() + " " + argStr.substring(1, argStr.length()-1);
        }
        
    }
    
    private static Integer parseInt (String str) {
        Integer ret = null;
        try {
            ret = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            ret = null;
        }
        return ret;
    }
    
    private static int [] parseString (String str) {
        if (str.length() >= 2 && str.charAt(0) == '"' && str.charAt(str.length()-1) == '"') {
            List <Integer> ret = new ArrayList<>();
            boolean escapeActive = false;
            for (int i=1; i<str.length()-1; i++) {
                if (str.charAt(i) == '\\') {
                    if (escapeActive) {
                        ret.add((int)'\\');
                        escapeActive = false;
                    } else
                        escapeActive = true;
                } else if (escapeActive) {
                    if (str.charAt(i) == 'n')
                        ret.add(10);
                    else if (str.charAt(i) == 't')
                        ret.add((int)'\t');
                    else if (str.charAt(i) == '0')
                        ret.add(0);
                    else
                        return null;
                    escapeActive = false;
                } else {
                    int toAdd = (int)str.charAt(i);
                    ret.add(toAdd);
                    if (toAdd < 0 || toAdd > 255)
                        return null;
                }
            }
            
            if (escapeActive)
                return null;
            else
                return Utils.list2ArrayInt(ret);
        } else
            return null;
    }
    
    private static int doRegIndexTranslation (int rawIndex, Map <Integer, Integer> regIndexTranslation, Map <Integer, Integer> regIndex2RawIndex) {
        if (regIndexTranslation.containsKey(rawIndex))
            return regIndexTranslation.get(rawIndex);
        else {
            regIndex2RawIndex.put(regIndexTranslation.size(), rawIndex);
            regIndexTranslation.put(rawIndex, regIndexTranslation.size());
            return regIndexTranslation.size()-1;
        }
    }
    
    public static class Arg {
        
        public final ArgType argType;
        public final int constValue, regIndex, labelIndex;

        public Arg(ArgType argType, int constValue, int regIndex, int labelIndex) {
            this.argType = argType;
            this.constValue = constValue;
            this.regIndex = regIndex;
            this.labelIndex = labelIndex;
        }
        
        public Arg (String rawArg, Map <String, Integer> label2Index, Map <Integer, Integer> regIndexTranslation, Map <Integer, Integer> regIndex2RawIndex, String line, int lineNumber) {
            if (rawArg.isEmpty())
                error("argument expected.", line, lineNumber);
            if (rawArg.charAt(0) == '$') {
                Integer index = parseInt(rawArg.substring(1));
                if (index == null || index < 0)
                    error("could not parse index of register.", line, lineNumber);
                argType = ArgType.Reg;
                constValue = -1;
                regIndex = doRegIndexTranslation(index, regIndexTranslation, regIndex2RawIndex);
                labelIndex = -1;
            } else if (rawArg.charAt(0) == '[') {
                if (rawArg.length() < 3 || rawArg.charAt(rawArg.length()-1) != ']')
                    error("invalid [$..] format.", line, lineNumber);
                
                if (rawArg.charAt(1) == '$') {
                    Integer index = parseInt(rawArg.substring(2, rawArg.length()-1));
                    if (index == null || index < 0)
                        error("could not parse index of register.", line, lineNumber);
                    argType = ArgType.MemAddr;
                    constValue = -1;
                    regIndex = doRegIndexTranslation(index, regIndexTranslation, regIndex2RawIndex);
                    labelIndex = -1;
                } else {
                    Integer index = parseInt(rawArg.substring(1, rawArg.length()-1));
                    if (index == null || index < 0)
                        error("could not parse index of constant memory-adress.", line, lineNumber);
                    argType = ArgType.MemAddrConst;
                    constValue = index;
                    regIndex = -1;
                    labelIndex = -1;
                }
            } else if (rawArg.charAt(0) >= '0' && rawArg.charAt(0) <= '9') {
                Integer constant = parseInt(rawArg);
                if (constant == null)
                    error("could not parse index of register.", line, lineNumber);
                argType = ArgType.Const;
                constValue = constant;
                regIndex = -1;
                labelIndex = -1;
            } else {
                for (int j=0; j<rawArg.length(); j++)
                    if (!((rawArg.charAt(j) >= 'A' && rawArg.charAt(j) <= 'Z') || rawArg.charAt(j) == '_' || (rawArg.charAt(j) >= 'a' && rawArg.charAt(j) <= 'z') || (j > 0 && rawArg.charAt(j) >= '0' && rawArg.charAt(j) <= '9')))
                        error("invalid characters in label (just A-Z a-z _ 0-9).", line, lineNumber);
                if (!label2Index.containsKey(rawArg))
                    label2Index.put(rawArg, label2Index.size());
                argType = ArgType.Label;
                constValue = -1;
                regIndex = -1;
                labelIndex = label2Index.get(rawArg);
            }
        }
        
        @Override
        public String toString () {
            switch (argType) {
                case Reg:
                    return "$" + regIndex;
                case Const:
                    return "" + constValue;
                case MemAddr:
                    return "[$" + regIndex + "]";
                case MemAddrConst:
                    return "[" + constValue + "]";
                case Label:
                    return "{" + labelIndex + "}";
                default:
                    throw new RuntimeException("internal error.");
            }
        }
        
    }
    
}
