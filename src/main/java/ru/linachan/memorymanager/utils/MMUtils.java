package ru.linachan.memorymanager.utils;

import com.sun.jna.Memory;
import ru.linachan.memorymanager.MMProcess;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MMUtils {

    public static String dumpMemory(Memory memoryPage, long memorySize, long baseAddress) {
        long memoryOffset = 0x00;
        int bytesPerLine = 0x20;

        StringBuilder memoryDump = new StringBuilder();

        while (memoryOffset < memorySize) {
            byte[] memoryBytes = memoryPage.getByteArray(memoryOffset, bytesPerLine);

            StringBuilder memoryLine = new StringBuilder(String.format(
                "%08X ", baseAddress + memoryOffset
            ));
            StringBuilder memoryRawLine = new StringBuilder("  ");

            for (int i = 0; i < bytesPerLine; i++) {
                memoryLine.append(String.format("%02X ", memoryBytes[i]));
                memoryRawLine.append((memoryBytes[i] == 0x00) ? '.' : (char) memoryBytes[i]);
            }

            memoryLine.append(memoryRawLine);

            memoryDump.append(memoryLine.toString().replaceAll("\\p{Cntrl}", "."));
            memoryDump.append("\r\n");

            memoryOffset += bytesPerLine;
        }

        return memoryDump.toString();
    }

    public static List<Integer> findArray(byte[] sourceArray, byte[] targetArray) {
        List<Integer> searchResult = new ArrayList<>();

        for (int i = 0; i < targetArray.length; i++) {
            if (targetArray[i] == sourceArray[0]) {
                boolean subArrayFound = true;
                for (int j = 0; j < sourceArray.length; j++) {
                    if (targetArray.length <= i+j || sourceArray[j] != targetArray[i+j]) {
                        subArrayFound = false;
                        break;
                    }
                }

                if (subArrayFound) {
                    searchResult.add(i);
                }
            }
        }

        return searchResult;
    }

    public static byte[] prepareString(String data, int bytesPerChar) throws UnsupportedEncodingException {
        byte[] dataBytes = data.getBytes("UTF-8");
        byte[] finalBytes = new byte[data.length() * bytesPerChar];

        if (data.length() * bytesPerChar > dataBytes.length) {
            for (int i = 0; i < data.length(); i++) {
                finalBytes[i * bytesPerChar] = dataBytes[i];
                for (int j = 1; j < bytesPerChar; j++) {
                    finalBytes[i * bytesPerChar + j] = 0x00;
                }
            }
        }

        return finalBytes;
    }

    public static byte[] getNullArray(int size) {
        byte[] nullArray = new byte[size];
        for (int i = 0; i < size; i++) {
            nullArray[i] = 0;
        }
        return nullArray;
    }

    public static Map<Integer, String> processListToMap(List<MMProcess> processList) {
        Map<Integer, String> processMap = new HashMap<>();

        processList.forEach(process -> processMap.put(process.getProcessID(), process.getProcessName()));

        return processMap;
    }
}
