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

import assembler.Parser.Arg;
import assembler.Parser.ArgType;
import assembler.Parser.Cmd;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Macro {
    
    public static final Macro divMacro = new Macro(new Parser("macros/div.asm"));
    
    public final Parser macroCode;
    
    public Macro (Parser macroCode) {
        this.macroCode = macroCode;
    }
    
    //every reg-argument 0..n will replace $0-$n
    public Parser replace (Parser parsed, int cmdIndex) {
        AtomicInteger reservedRegisterOffset = new AtomicInteger(parsed.numberOfRegisters - parsed.lastNRegistersReserved);
        Map <Integer, Integer> replaceRegIndices = new HashMap<>();
        for (int i=0; i<parsed.commands[cmdIndex].args.length; i++) {
            if (parsed.commands[cmdIndex].args[i].argType == ArgType.Reg) {
                Integer regIndexMacro = macroCode.rawIndex2RegIndex.get(replaceRegIndices.size());
                if (regIndexMacro == null)
                    throw new RuntimeException("argument not used in macro-code. (internal error)");
                replaceRegIndices.put(regIndexMacro, parsed.commands[cmdIndex].args[i].regIndex);
            } else
                throw new RuntimeException("invalid argument. (internal error)");
        }
        final int additionalReservedRegisters = Math.max(parsed.lastNRegistersReserved, macroCode.numberOfRegisters-replaceRegIndices.size());
        
        int [] mergedLabelOffsets = new int [parsed.labelOffsets.length + macroCode.labelOffsets.length];
        for (int i=0; i<parsed.labelOffsets.length; i++)
            mergedLabelOffsets[i] = (parsed.labelOffsets[i] <= cmdIndex ? parsed.labelOffsets[i] : (parsed.labelOffsets[i]+macroCode.commands.length-1));
        for (int i=0; i<macroCode.labelOffsets.length; i++)
            mergedLabelOffsets[i+parsed.labelOffsets.length] = macroCode.labelOffsets[i] + cmdIndex;
        
        Cmd [] mergedCommands = new Cmd[parsed.commands.length-1 + macroCode.commands.length];
        System.arraycopy(parsed.commands, 0, mergedCommands, 0, cmdIndex);
        for (int i=0; i<macroCode.commands.length; i++) {
            Arg [] args = new Arg [macroCode.commands[i].args.length];
            for (int j=0; j<args.length; j++) {
                switch (macroCode.commands[i].args[j].argType) {
                    case Reg:
                    case MemAddr:
                        args[j] = new Arg(macroCode.commands[i].args[j].argType, -1, getRegTranslation(replaceRegIndices, i, j, reservedRegisterOffset), -1);
                        break;
                    case Const:
                    case MemAddrConst:
                        args[j] = new Arg(macroCode.commands[i].args[j].argType, macroCode.commands[i].args[j].constValue, -1, -1); 
                        break;
                    case Label:
                        args[j] = new Arg(macroCode.commands[i].args[j].argType, -1, -1, macroCode.commands[i].args[j].labelIndex + parsed.labelOffsets.length); 
                        break;
                    default:
                        throw new RuntimeException("internal error.");
                }
            }
            mergedCommands[i + cmdIndex] = new Cmd(macroCode.commands[i].cmdType, args);
        }
        System.arraycopy(parsed.commands, cmdIndex+1, mergedCommands, cmdIndex+macroCode.commands.length, parsed.commands.length-cmdIndex-1);
        
        return new Parser (mergedCommands, mergedLabelOffsets, parsed.numberOfRegisters+additionalReservedRegisters, parsed.lastNRegistersReserved+additionalReservedRegisters, parsed.globalMemory, parsed.stackSize);
    }
    
    private int getRegTranslation (Map <Integer, Integer> replaceRegIndices, int cmdIndex, int argIndex, AtomicInteger reservedRegisterOffset) {
        Integer ret = replaceRegIndices.get(macroCode.commands[cmdIndex].args[argIndex].regIndex);
        if (ret == null) {
            replaceRegIndices.put(macroCode.commands[cmdIndex].args[argIndex].regIndex, reservedRegisterOffset.getAndIncrement());
            return getRegTranslation(replaceRegIndices, cmdIndex, argIndex, reservedRegisterOffset);
        } else
            return ret;
    }
    
}
