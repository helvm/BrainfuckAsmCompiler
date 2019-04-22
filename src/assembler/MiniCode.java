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

public class MiniCode {
    
    //global positions are: [registers], [src main-loop], [dst main-loop], [tmp main-loop], [tmp main-loop2], [stack-ptr], [tmp reserved 0-3], [tmp reserved 4 (0 for memlookup)] [reserved mem-lookup]
    
    public final int numberOfRegisters;
    
    public MiniCode (int numberOfRegisters) {
        this.numberOfRegisters = numberOfRegisters;
    }
    
    public int getReservedMainLoopPosition () {
        return numberOfRegisters+0;
    }
    
    public int getReservedStackPtr () {
        return numberOfRegisters+4;
    }
    
    public int getReservedTmpReserved () {
        return numberOfRegisters+5;
    }
    
    public int getReservedMemLookupPosition () {
        return numberOfRegisters+10;
    }
    
    public String move (int positionSrc, int positionDst) {
        StringBuilder ret = new StringBuilder();
        int moveRight = positionDst-positionSrc;
        while (moveRight > 0) {
            ret.append(">");
            moveRight--;
        }
        while (moveRight < 0) {
            ret.append("<");
            moveRight++;
        }
        return ret.toString();
    }
    
    public String constantAdd (int addConstant) {
        StringBuilder ret = new StringBuilder();
        while (addConstant > 0) {
            ret.append("+");
            addConstant--;
        }
        while (addConstant < 0) {
            ret.append("-");
            addConstant++;
        }
        return ret.toString();
    }
    
    //if the zeroCell is not zero, the value is added to it
    public String moveContentToZeroCell (int currentPosition, int srcPosition, int zeroCellPosition, int dstPosition) {
        return move(currentPosition, srcPosition) + "[-" + move(srcPosition, zeroCellPosition) + "+" + move(zeroCellPosition, srcPosition) + "]" + move(srcPosition, dstPosition);
    }
    
    //tmpPosition AND dstPosition has to be 0, tmp is 0 again after this instruction
    public String copyContent (int currentPosition, int srcPosition, int tmpPosition, int copyToPosition, int dstPosition) {
        return move(currentPosition, srcPosition) + "[-" + move(srcPosition, tmpPosition) + "+" + move(tmpPosition, copyToPosition) + "+" + move(copyToPosition, srcPosition) + "]" + move(srcPosition, tmpPosition) + "[-" + move(tmpPosition, srcPosition) + "+" + move(srcPosition, tmpPosition) + "]" + move(tmpPosition, dstPosition);
    }
    
    //mem-addr is in getReservedMemLookupPosition(), result is written in tmpReserved+0 (has to be 0 before)
    public String moveFromMemAddr (int currentPosition, int dstPosition) {
        return move(currentPosition, getReservedMemLookupPosition()) + "+[-[->>+>+<<<]>>>]<<[-<<[<<<[>>>[-]<<<<<<]]>>>[-<<+>>]<<" + move(getReservedMemLookupPosition(), getReservedTmpReserved()) + "+" + move(getReservedTmpReserved(), getReservedMemLookupPosition()) + "+[-[->>+>+<<<]>>>]<<]<<[<<<[>>>[-]<<<<<<]]>>>[-<<+>>]<<" + move(getReservedMemLookupPosition(), dstPosition);
    }
    
    //mem-addr is in getReservedMemLookupPosition(), result is read from tmpReserved+0, then written to memory-cell (will be overwritten)
    public String moveToMemAddr (int currentPosition, int dstPosition) {
        return move(currentPosition, getReservedMemLookupPosition()) + "+[-[->>+>+<<<]>>>]<<[-]<<[<<<[>>>[-]<<<<<<]]>>>[-<<+>>]<<" + move(getReservedMemLookupPosition(), getReservedTmpReserved()) + "[-" + move(getReservedTmpReserved(), getReservedMemLookupPosition()) + "+[-[->>+>+<<<]>>>]<<+<<[<<<[>>>[-]<<<<<<]]>>>[-<<+>>]<<" + move(getReservedMemLookupPosition(), getReservedTmpReserved()) + "]" + move(getReservedTmpReserved(), dstPosition);
    }
    
    public String copyFromMemAddr (int currentPosition, int registerPositionReadMemAddr, int registerPositionWrite, int dstPosition) {
        return move(currentPosition, registerPositionWrite) + "[-]" + moveContentToZeroCell(registerPositionWrite, registerPositionReadMemAddr, getReservedMemLookupPosition(), getReservedMemLookupPosition()) + moveFromMemAddr(getReservedMemLookupPosition(), getReservedMemLookupPosition()) + copyContent(getReservedMemLookupPosition(), getReservedTmpReserved(), getReservedTmpReserved()+1, registerPositionWrite, getReservedMemLookupPosition()) + moveToMemAddr(getReservedMemLookupPosition(), getReservedMemLookupPosition()) + moveContentToZeroCell(getReservedMemLookupPosition(), getReservedMemLookupPosition(), registerPositionReadMemAddr, dstPosition);
    }
    
    public String copyToMemAddr (int currentPosition, int registerPositionWriteMemAddr, int registerPositionRead, int dstPosition) {
        return moveContentToZeroCell(currentPosition, registerPositionWriteMemAddr, getReservedMemLookupPosition(), getReservedMemLookupPosition()) + copyContent(getReservedMemLookupPosition(), registerPositionRead, getReservedTmpReserved()+1, getReservedTmpReserved(), getReservedMemLookupPosition()) + moveToMemAddr(getReservedMemLookupPosition(), getReservedMemLookupPosition()) + moveContentToZeroCell(getReservedMemLookupPosition(), getReservedMemLookupPosition(), registerPositionWriteMemAddr, dstPosition);
    }
    
    public String constantToMemAddr (int currentPosition, int registerPositionWriteMemAddr, int constantValue, int dstPosition) {
        return moveContentToZeroCell(currentPosition, registerPositionWriteMemAddr, getReservedMemLookupPosition(), getReservedMemLookupPosition()) + "+[-[->>+>+<<<]>>>]<<[-]" + constantAdd(constantValue) + "<<[<<<[>>>[-]<<<<<<]]>>>[-<<+>>]<<" + moveContentToZeroCell(getReservedMemLookupPosition(), getReservedMemLookupPosition(), registerPositionWriteMemAddr, dstPosition);
    }
    
    public String copyFromMemAddrConst (int currentPosition, int constantMemAddr, int registerPositionWrite, int dstPosition) {
        return move(currentPosition, getReservedMemLookupPosition()) + constantAdd(constantMemAddr) + moveFromMemAddr(getReservedMemLookupPosition(), getReservedMemLookupPosition()) + copyContent(getReservedMemLookupPosition(), getReservedTmpReserved(), getReservedTmpReserved()+1, registerPositionWrite, getReservedMemLookupPosition()) + moveToMemAddr(getReservedMemLookupPosition(), getReservedMemLookupPosition()) + "[-]" + move(getReservedMemLookupPosition(), dstPosition);
    }
    
    public String copyToMemAddrConst (int currentPosition, int constantMemAddr, int registerPositionRead, int dstPosition) {
        return move(currentPosition, getReservedMemLookupPosition()) + constantAdd(constantMemAddr) + copyContent(getReservedMemLookupPosition(), registerPositionRead, getReservedTmpReserved()+1, getReservedTmpReserved(), getReservedMemLookupPosition()) + moveToMemAddr(getReservedMemLookupPosition(), getReservedMemLookupPosition()) + "[-]" + move(getReservedMemLookupPosition(), dstPosition);
    }
    
    public String constantToMemAddrConst (int currentPosition, int constantMemAddr, int constantValue, int dstPosition) {
        return move(currentPosition, getReservedMemLookupPosition()) + constantAdd(constantMemAddr) + "+[-[->>+>+<<<]>>>]<<[-]" + constantAdd(constantValue) + "<<[<<<[>>>[-]<<<<<<]]>>>[-<<+>>]<<[-]" + move(getReservedMemLookupPosition(), dstPosition);
    }
    
}
