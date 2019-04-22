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
import assembler.Parser.CmdType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeCreation {
     
    public final Parser parsed;
    public final MiniCode miniCode;
    
    public CodeCreation (Parser parsed) {
        this.parsed = replaceAllMacros(parsed);
        miniCode = new MiniCode(this.parsed.numberOfRegisters);
    }
    
    public static Parser replaceAllMacros (Parser parsed) {
        Parser ret = parsed;
        for (int i=0; i<ret.commands.length; i++)
            if (ret.commands[i].cmdType == CmdType.Div) {
                ret = Macro.divMacro.replace(ret, i);
                i--;
            }
        
        return ret;
    }
    
    //position before is 0 and has to be at the end 0 again
    public String createGlobalInit () {
        List <Integer> globalMemoryKeys = new ArrayList<>();
        globalMemoryKeys.addAll(parsed.globalMemory.keySet());
        Collections.sort(globalMemoryKeys);
        StringBuilder ret = new StringBuilder();
        for (int i=0; i<globalMemoryKeys.size(); i++) {
            final int key = globalMemoryKeys.get(i), val = parsed.globalMemory.get(key);
            final int offset = miniCode.getReservedMemLookupPosition()+1 + (key+parsed.stackSize)*3;
            
            final int multiplicator = Math.max(1, (int)Math.round(Math.sqrt(val)));
            final int val0 = val/multiplicator, val1 = val%multiplicator;
            ret.append(miniCode.move(0, offset));
           if (val0 >= 4)
                ret.append(">").append(miniCode.constantAdd(val0)).append("[-<").append(miniCode.constantAdd(multiplicator)).append(">]<").append(miniCode.constantAdd(val1));
            else
                ret.append(miniCode.constantAdd(val));
            ret.append(miniCode.move(offset, 0));
        }
        
        return ret.toString();
    }
    
    //position before is 0 and has to be at the end 0 again
    public String create (ControlFlowSegments parent, int fromIndexIncl, int toIndexExcl) {
        StringBuilder ret = new StringBuilder();
        final boolean containsFinalJump = (toIndexExcl <= parsed.commands.length && fromIndexIncl <= toIndexExcl-1 && parsed.commands[toIndexExcl-1].isJumpInstruction());
        final boolean isFinalSegment = (toIndexExcl >= parsed.commands.length);
        
        for (int i=fromIndexIncl; i<toIndexExcl-(containsFinalJump ? 1 : 0); i++) {
            Cmd command = parsed.commands[i];
            if (command.isJumpInstruction())
                throw new RuntimeException("jump instruction unexpected. (internal error)");
            ret.append(createInstruction(command));
        }
        
        if (containsFinalJump)
            ret.append(createJumpInstruction(parent, parsed.commands[toIndexExcl-1], toIndexExcl));
        else if (isFinalSegment)
            ret.append(miniCode.move(0, miniCode.getReservedMainLoopPosition()+1)).append(miniCode.constantAdd(parent.getSegmentIndex(parsed.commands.length))).append(miniCode.move(miniCode.getReservedMainLoopPosition()+1, 0));
        else
            ret.append(createJumpInstruction(parent, null, toIndexExcl));
        return ret.toString();
    }
    
    //position before is 0 and has to be at the end 0 again
    private String createInstruction (Cmd command) {
        StringBuilder ret = new StringBuilder();
        switch (command.cmdType) {
            case Mov: {
                if (command.args[0].argType == ArgType.Reg && command.args[1].argType == ArgType.Reg) {
                    ret.append(miniCode.move(0, command.args[0].regIndex)).append("[-]");
                    ret.append(miniCode.copyContent(command.args[0].regIndex, command.args[1].regIndex, miniCode.getReservedTmpReserved(), command.args[0].regIndex, 0));
                } else if (command.args[0].argType == ArgType.Reg && command.args[1].argType == ArgType.Const) {
                    ret.append(miniCode.move(0, command.args[0].regIndex)).append("[-]").append(miniCode.constantAdd(command.args[1].constValue));
                    ret.append(miniCode.move(command.args[0].regIndex, 0));
                } else if (command.args[0].argType == ArgType.Reg && command.args[1].argType == ArgType.MemAddr)
                    ret.append(miniCode.move(0, command.args[1].regIndex)).append(miniCode.constantAdd(parsed.stackSize)).append(miniCode.copyFromMemAddr(command.args[1].regIndex, command.args[1].regIndex, command.args[0].regIndex, command.args[1].regIndex)).append(miniCode.constantAdd(-parsed.stackSize)).append(miniCode.move(command.args[1].regIndex, 0));
                else if (command.args[0].argType == ArgType.MemAddr && command.args[1].argType == ArgType.Reg)
                   ret.append(miniCode.move(0, command.args[0].regIndex)).append(miniCode.constantAdd(parsed.stackSize)).append(miniCode.copyToMemAddr(command.args[0].regIndex, command.args[0].regIndex, command.args[1].regIndex, command.args[0].regIndex)).append(miniCode.constantAdd(-parsed.stackSize)).append(miniCode.move(command.args[0].regIndex, 0));
                else if (command.args[0].argType == ArgType.MemAddr && command.args[1].argType == ArgType.Const)
                   ret.append(miniCode.move(0, command.args[0].regIndex)).append(miniCode.constantAdd(parsed.stackSize)).append(miniCode.constantToMemAddr(command.args[0].regIndex, command.args[0].regIndex, command.args[1].constValue, command.args[0].regIndex)).append(miniCode.constantAdd(-parsed.stackSize)).append(miniCode.move(command.args[0].regIndex, 0));
                else if (command.args[0].argType == ArgType.Reg && command.args[1].argType == ArgType.MemAddrConst)
                    ret.append(miniCode.copyFromMemAddrConst(0, command.args[1].constValue+parsed.stackSize, command.args[0].regIndex, 0));
                else if (command.args[0].argType == ArgType.MemAddrConst && command.args[1].argType == ArgType.Reg)
                   ret.append(miniCode.copyToMemAddrConst(0, command.args[0].constValue+parsed.stackSize, command.args[1].regIndex, 0));
                else if (command.args[0].argType == ArgType.MemAddrConst && command.args[1].argType == ArgType.Const)
                   ret.append(miniCode.constantToMemAddrConst(0, command.args[0].constValue+parsed.stackSize, command.args[1].constValue, 0));
                else
                    throw new RuntimeException("unknown mov-command. (internal error)");
                break;
            }
            case Add:
            case Sub:
            {
                final boolean isAdd = command.cmdType == CmdType.Add;
                if (command.args[0].argType == ArgType.Reg && command.args[1].argType == ArgType.Reg)
                    ret.append(miniCode.copyContent(0, command.args[1].regIndex, miniCode.getReservedTmpReserved()+1, miniCode.getReservedTmpReserved(), miniCode.getReservedTmpReserved())).append("[-").append(miniCode.move(miniCode.getReservedTmpReserved(), command.args[0].regIndex)).append(isAdd ? "+" : "-").append(miniCode.move(command.args[0].regIndex, miniCode.getReservedTmpReserved())).append("]").append(miniCode.move(miniCode.getReservedTmpReserved(), 0));
                else {
                    int constVal = (isAdd ? 1 : -1)*command.args[1].constValue;
                    boolean constDoAdd = (constVal > 0);
                    constVal = Math.abs(constVal);
                    if (constVal != 0)
                        ret.append(miniCode.move(0, miniCode.getReservedTmpReserved())).append(miniCode.constantAdd(constVal)).append("[-").append(miniCode.move(miniCode.getReservedTmpReserved(), command.args[0].regIndex)).append(constDoAdd ? "+" : "-").append(miniCode.move(command.args[0].regIndex, miniCode.getReservedTmpReserved())).append("]").append(miniCode.move(miniCode.getReservedTmpReserved(), 0));
                }
                break;
            }
            case Mul: {
                ret.append(miniCode.move(0, command.args[0].regIndex)).append("[-]");
                ret.append(miniCode.copyContent(command.args[0].regIndex, command.args[2].regIndex, miniCode.getReservedTmpReserved()+3, miniCode.getReservedTmpReserved(), miniCode.getReservedTmpReserved()));
                ret.append(miniCode.copyContent(miniCode.getReservedTmpReserved(), command.args[1].regIndex, miniCode.getReservedTmpReserved()+3, miniCode.getReservedTmpReserved()+1, miniCode.getReservedTmpReserved()));
                ret.append("[").append(miniCode.copyContent(miniCode.getReservedTmpReserved(), miniCode.getReservedTmpReserved()+1, miniCode.getReservedTmpReserved()+3, miniCode.getReservedTmpReserved()+2, miniCode.getReservedTmpReserved())).append(miniCode.moveContentToZeroCell(miniCode.getReservedTmpReserved(), miniCode.getReservedTmpReserved()+2, command.args[0].regIndex, miniCode.getReservedTmpReserved())).append("-]>[-]").append(miniCode.move(miniCode.getReservedTmpReserved()+1, 0));
                break;
            }
            case Div:
                throw new RuntimeException("div should have been replaced with a macro. (internal error)");
            
            case Jmp:
            case Jz:
            case Jnz:
            case Je:
            case Jne:
            case Jl:
            case Jle:
            case Jg:
            case Jge:
            case Call:
            case Ret:
                throw new RuntimeException("jump instruction not expected here. (internal error)");
                
            case In: {
                ret.append(miniCode.move(0, command.args[0].regIndex) + "," + miniCode.move(command.args[0].regIndex, 0));
                break;
            }
                
            case Push:
            case Pop:
            {
                final boolean isPush = command.cmdType == CmdType.Push;
                if (!isPush)
                    ret.append(miniCode.move(0, miniCode.getReservedStackPtr())).append("-").append(miniCode.copyFromMemAddr(miniCode.getReservedStackPtr(), miniCode.getReservedStackPtr(), command.args[0].regIndex, 0));
                else if (isPush && command.args[0].argType == ArgType.Reg)
                   ret.append(miniCode.copyToMemAddr(0, miniCode.getReservedStackPtr(), command.args[0].regIndex, miniCode.getReservedStackPtr())).append(miniCode.constantAdd(1)).append(miniCode.move(miniCode.getReservedStackPtr(), 0));
                else if (isPush && command.args[0].argType == ArgType.Const)
                   ret.append(miniCode.constantToMemAddr(0, miniCode.getReservedStackPtr(), command.args[0].constValue, miniCode.getReservedStackPtr())).append(miniCode.constantAdd(1)).append(miniCode.move(miniCode.getReservedStackPtr(), 0));
                else
                    throw new RuntimeException("unknown push/pop-command. (internal error)");
                break;
            }
            
            case Out: {
                if (command.args[0].argType == ArgType.Const)
                    ret.append(miniCode.move(0, miniCode.getReservedTmpReserved())).append(miniCode.constantAdd(command.args[0].constValue)).append(".[-]").append(miniCode.move(miniCode.getReservedTmpReserved(), 0));
                else
                    ret.append(miniCode.move(0, command.args[0].regIndex)).append(".").append(miniCode.move(command.args[0].regIndex, 0));
                break;
            }
            default:
                throw new RuntimeException("unknown command. (internal error)");
        }
        return ret.toString();
    }
    
    //position before is 0 and has to be at the end 0 again
    private String createJumpInstruction (ControlFlowSegments parent, Cmd command, int offsetSuccessor) {
        StringBuilder ret = new StringBuilder();
        if (command != null && command.cmdType == CmdType.Jmp)
            ret.append(miniCode.move(0, miniCode.getReservedMainLoopPosition()+1)).append(miniCode.constantAdd(parent.getSegmentIndex(parsed.labelOffsets[command.args[0].labelIndex]))).append(miniCode.move(miniCode.getReservedMainLoopPosition()+1, 0));
        else if (command != null && (command.cmdType == CmdType.Jz || command.cmdType == CmdType.Jnz)) {
            final int jmpDstNonZero = parent.getSegmentIndex(offsetSuccessor);
            final int jmpDstZero = parent.getSegmentIndex(parsed.labelOffsets[command.args[1].labelIndex]);
            ret.append(miniCode.copyContent(0, command.args[0].regIndex, miniCode.getReservedTmpReserved()+1, miniCode.getReservedTmpReserved(), miniCode.getReservedTmpReserved())).append(">+<[").append(miniCode.move(miniCode.getReservedTmpReserved(), miniCode.getReservedMainLoopPosition()+1)).append(miniCode.constantAdd((command.cmdType == CmdType.Jz ? jmpDstNonZero : jmpDstZero))).append(miniCode.move(miniCode.getReservedMainLoopPosition()+1, miniCode.getReservedTmpReserved())).append("[-]>-<]>[").append(miniCode.move(miniCode.getReservedTmpReserved()+1, miniCode.getReservedMainLoopPosition()+1)).append(miniCode.constantAdd((command.cmdType == CmdType.Jz ? jmpDstZero : jmpDstNonZero))).append(miniCode.move(miniCode.getReservedMainLoopPosition()+1, miniCode.getReservedTmpReserved()+1)).append("-]").append(miniCode.move(miniCode.getReservedTmpReserved()+1, 0));
        } else if (command != null && (command.cmdType == CmdType.Je || command.cmdType == CmdType.Jne || command.cmdType == CmdType.Jl || command.cmdType == CmdType.Jle || command.cmdType == CmdType.Jg || command.cmdType == CmdType.Jge)) {
            final int jmpDstNonZero = parent.getSegmentIndex(offsetSuccessor);
            final int jmpDstZero = parent.getSegmentIndex(parsed.labelOffsets[command.args[2].labelIndex]);
            final boolean jumpIfZero = (command.cmdType == CmdType.Je);
            ret.append(miniCode.copyContent(0, command.args[(command.cmdType == CmdType.Jg || command.cmdType == CmdType.Jge ? 1 : 0)].regIndex, miniCode.getReservedTmpReserved()+2, miniCode.getReservedTmpReserved(), miniCode.getReservedTmpReserved()));
            ret.append(miniCode.copyContent(miniCode.getReservedTmpReserved(), command.args[(command.cmdType == CmdType.Jg || command.cmdType == CmdType.Jge ? 0 : 1)].regIndex, miniCode.getReservedTmpReserved()+2, miniCode.getReservedTmpReserved()+1, miniCode.getReservedTmpReserved()+1));
            if (command.cmdType == CmdType.Je || command.cmdType == CmdType.Jne)
                ret.append("[-<->]<");
            else if (command.cmdType == CmdType.Jg || command.cmdType == CmdType.Jge || command.cmdType == CmdType.Jl || command.cmdType == CmdType.Jle) {
                if (command.cmdType == CmdType.Jge || command.cmdType == CmdType.Jle)
                    ret.append("+");
                ret.append("[").append(miniCode.copyContent(miniCode.getReservedTmpReserved()+1, miniCode.getReservedTmpReserved(), miniCode.getReservedTmpReserved()+4, miniCode.getReservedTmpReserved()+3, miniCode.getReservedTmpReserved()+1)).append(">+>[<->[-]]<<-<->]<[-]>>[-<<+>>]<<");
            }
            ret.append(">+<[").append(miniCode.move(miniCode.getReservedTmpReserved(), miniCode.getReservedMainLoopPosition()+1)).append(miniCode.constantAdd((jumpIfZero ? jmpDstNonZero : jmpDstZero))).append(miniCode.move(miniCode.getReservedMainLoopPosition()+1, miniCode.getReservedTmpReserved())).append("[-]>-<]>[").append(miniCode.move(miniCode.getReservedTmpReserved()+1, miniCode.getReservedMainLoopPosition()+1)).append(miniCode.constantAdd((jumpIfZero ? jmpDstZero : jmpDstNonZero))).append(miniCode.move(miniCode.getReservedMainLoopPosition()+1, miniCode.getReservedTmpReserved()+1)).append("-]").append(miniCode.move(miniCode.getReservedTmpReserved()+1, 0));
        } else if (command != null && command.cmdType == CmdType.Call) {
            final int jmpBack = parent.getSegmentIndex(offsetSuccessor);
            ret.append(createInstruction(new Cmd(CmdType.Push, new Arg [] {new Arg(ArgType.Const, jmpBack, -1, -1)})));
            ret.append(miniCode.move(0, miniCode.getReservedMainLoopPosition()+1)).append(miniCode.constantAdd(parent.getSegmentIndex(parsed.labelOffsets[command.args[0].labelIndex]))).append(miniCode.move(miniCode.getReservedMainLoopPosition()+1, 0));
        } else if (command != null && command.cmdType == CmdType.Ret)
            ret.append(miniCode.move(0, miniCode.getReservedStackPtr())).append("-").append(miniCode.copyFromMemAddr(miniCode.getReservedStackPtr(), miniCode.getReservedStackPtr(), miniCode.getReservedMainLoopPosition()+1, 0));
        else if (command == null)
            ret.append(miniCode.move(0, miniCode.getReservedMainLoopPosition()+1)).append(miniCode.constantAdd(parent.getSegmentIndex(offsetSuccessor))).append(miniCode.move(miniCode.getReservedMainLoopPosition()+1, 0));
        else
            throw new RuntimeException("unknown jump instruction. (internal error)");
        return ret.toString();
    }
    
}
