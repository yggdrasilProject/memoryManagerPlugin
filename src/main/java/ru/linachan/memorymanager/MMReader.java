package ru.linachan.memorymanager;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import org.bouncycastle.util.Arrays;
import ru.linachan.memorymanager.utils.MMUtils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MMReader {

    private final MMProcess process;

    public MMReader(MMProcess process) {
        this.process = process;
    }

    // byte[]

    public List<Long> findBytes(byte[] pattern) {
        List<Long> searchResult = new ArrayList<>();

        for (WinNT.MEMORY_BASIC_INFORMATION memoryInfo: process.queryPages()) {
            Memory memoryPage = process.readMemory(memoryInfo.baseAddress, memoryInfo.regionSize);
            byte[] memoryBytes = memoryPage.getByteArray(0x00, memoryInfo.regionSize.intValue());

            List<Integer> memoryOffsets = MMUtils.findArray(pattern, memoryBytes);

            searchResult.addAll(
                memoryOffsets.stream()
                    .map(memoryOffset -> Pointer.nativeValue(memoryInfo.baseAddress) + memoryOffset)
                    .collect(Collectors.toList())
            );
        }

        return searchResult;
    }

    public byte[] readBytes(long address, int blockSize) {
        Memory memoryBlock = process.readMemory(address, blockSize);
        return memoryBlock.getByteArray(0, (int) memoryBlock.size());
    }

    public int writeBytes(long address, byte[] bytesToWrite) {
        Memory memoryToWrite = new Memory(bytesToWrite.length);
        memoryToWrite.write(0, bytesToWrite, 0, bytesToWrite.length);
        return process.writeMemory(address, memoryToWrite);
    }

    // String

    private long findStringBaseAddress(long stringAddress, int bytesPerChar) {
        byte[] nullArray = MMUtils.getNullArray(bytesPerChar);

        while (true) {
            Memory memoryPage = process.readMemory(stringAddress - bytesPerChar, bytesPerChar);
            byte[] memoryBytes = memoryPage.getByteArray(0, bytesPerChar);

            if (Arrays.areEqual(memoryBytes, nullArray)) {
                break;
            } else {
                stringAddress -= bytesPerChar;
            }
        }

        return stringAddress;
    }

    public List<Long> findString(String pattern, int bytesPerChar) throws UnsupportedEncodingException {
        return findBytes(MMUtils.prepareString(pattern, bytesPerChar)).stream()
            .map(address -> findStringBaseAddress(address, bytesPerChar))
            .collect(Collectors.toList());
    }

    public String readString(long address, int bytesPerChar) {
        StringBuilder resultString = new StringBuilder();

        byte[] nullArray = MMUtils.getNullArray(bytesPerChar);

        address = findStringBaseAddress(address, bytesPerChar);

        while (true) {
            Memory memoryPage = process.readMemory(address, bytesPerChar);
            byte[] memoryBytes = memoryPage.getByteArray(0, bytesPerChar);

            if (Arrays.areEqual(memoryBytes, nullArray)) {
                break;
            } else {
                resultString.append(new String(memoryBytes));
                address += bytesPerChar;
            }
        }

        return resultString.toString();
    }

    public void writeString(String data, long address, int bytesPerChar) throws UnsupportedEncodingException {
        address = findStringBaseAddress(address, bytesPerChar);

        byte[] dataBytes = MMUtils.prepareString(data, bytesPerChar);
        byte[] nullArray = MMUtils.getNullArray(Math.max(readString(address, bytesPerChar).length() * bytesPerChar, dataBytes.length));

        System.arraycopy(dataBytes, 0, nullArray, 0, dataBytes.length);

        writeBytes(address, nullArray);
    }

    // byte

    public List<Long> findByte(byte value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(1);
        valueBytes.put(value);
        return findBytes(valueBytes.array());
    }

    public byte readByte(long address) {
        ByteBuffer valueBytes = ByteBuffer.wrap(readBytes(address, 1));
        return valueBytes.get();
    }

    public void writeByte(long address, byte value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(1);
        valueBytes.put(value);
        writeBytes(address, valueBytes.array());
    }

    // short

    public List<Long> findShort(short value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(2);
        valueBytes.putShort(value);
        return findBytes(valueBytes.array());
    }

    public short readShort(long address) {
        ByteBuffer valueBytes = ByteBuffer.wrap(readBytes(address, 2));
        return valueBytes.getShort();
    }

    public void writeShort(long address, short value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(2);
        valueBytes.putShort(value);
        writeBytes(address, valueBytes.array());
    }

    // int

    public List<Long> findInt(int value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(4);
        valueBytes.putInt(value);
        return findBytes(valueBytes.array());
    }

    public int readInt(long address) {
        ByteBuffer valueBytes = ByteBuffer.wrap(readBytes(address, 4));
        return valueBytes.getInt();
    }

    public void writeInt(long address, int value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(4);
        valueBytes.putInt(value);
        writeBytes(address, valueBytes.array());
    }

    // long

    public List<Long> findLong(long value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(8);
        valueBytes.putLong(value);
        return findBytes(valueBytes.array());
    }

    public long readLong(long address) {
        ByteBuffer valueBytes = ByteBuffer.wrap(readBytes(address, 8));
        return valueBytes.getLong();
    }

    public void writeLong(long address, long value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(8);
        valueBytes.putLong(value);
        writeBytes(address, valueBytes.array());
    }

    // float

    public List<Long> findFloat(float value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(4);
        valueBytes.putFloat(value);
        return findBytes(valueBytes.array());
    }

    public float readFloat(long address) {
        ByteBuffer valueBytes = ByteBuffer.wrap(readBytes(address, 4));
        return valueBytes.getFloat();
    }

    public void writeFloat(long address, float value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(4);
        valueBytes.putFloat(value);
        writeBytes(address, valueBytes.array());
    }

    // double

    public List<Long> findDouble(double value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(8);
        valueBytes.putDouble(value);
        return findBytes(valueBytes.array());
    }

    public double readDouble(long address) {
        ByteBuffer valueBytes = ByteBuffer.wrap(readBytes(address, 8));
        return valueBytes.getDouble();
    }

    public void writeDouble(long address, double value) {
        ByteBuffer valueBytes = ByteBuffer.allocate(8);
        valueBytes.putDouble(value);
        writeBytes(address, valueBytes.array());
    }
}
