package ru.linachan.memorymanager;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import ru.linachan.memorymanager.utils.MMUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.sun.jna.platform.win32.WinNT.MEM_COMMIT;
import static com.sun.jna.platform.win32.WinNT.PAGE_READWRITE;

public class MMProcess {

    private final String processName;
    private final int processID;

    public static final int PROCESS_VM_READ = 0x0010;
    public static final int PROCESS_VM_WRITE = 0x0020;
    public static final int PROCESS_VM_OPERATIONS = 0x0008;
    public static final int PROCESS_QUERY_INFO = 0x0400;

    private WinNT.HANDLE processHandle;

    static final Kernel32 kernel32 = Kernel32.INSTANCE;

    public MMProcess(String processName, int processID) {
        this.processName = processName;
        this.processID = processID;
    }

    public void openProcess(int permissions) {
        processHandle = kernel32.OpenProcess(permissions, true, processID);
    }

    public String dumpMemory() {
        StringBuilder memoryDump = new StringBuilder();

        for (WinNT.MEMORY_BASIC_INFORMATION memoryInfo: queryPages()) {
            Memory memoryPage = readMemory(memoryInfo.baseAddress, memoryInfo.regionSize);

            memoryDump.append(MMUtils.dumpMemory(
                memoryPage, memoryInfo.regionSize.longValue(), Pointer.nativeValue(memoryInfo.baseAddress)
            ));
        }

        return memoryDump.toString();
    }

    public String dumpMemory(long address, int bytesToRead) {
        Memory memoryPage = readMemory(address, bytesToRead);

        return MMUtils.dumpMemory(memoryPage, bytesToRead, address);
    }

    public List<WinNT.MEMORY_BASIC_INFORMATION> queryPages() {
        List<WinNT.MEMORY_BASIC_INFORMATION> memoryPages = new ArrayList<>();

        WinNT.MEMORY_BASIC_INFORMATION memoryInfo;
        WinBase.SYSTEM_INFO info =  new WinBase.SYSTEM_INFO();
        kernel32.GetSystemInfo(info);

        Pointer processMinAddressPtr = info.lpMinimumApplicationAddress;
        Pointer processMaxAddressPtr = info.lpMaximumApplicationAddress;

        long processMinAddress = Pointer.nativeValue(processMinAddressPtr);
        long processMaxAddress = Pointer.nativeValue(processMaxAddressPtr);

        while (processMinAddress < processMaxAddress) {
            memoryInfo = new WinNT.MEMORY_BASIC_INFORMATION();
            kernel32.VirtualQueryEx(processHandle, processMinAddressPtr, memoryInfo, new BaseTSD.SIZE_T(memoryInfo.size()));

            if (Objects.equals(memoryInfo.protect, new WinDef.DWORD(PAGE_READWRITE))&&Objects.equals(memoryInfo.state, new WinDef.DWORD(MEM_COMMIT))) {
                memoryPages.add(memoryInfo);
            }

            processMinAddress += memoryInfo.regionSize.longValue();
            processMinAddressPtr = new Pointer(processMinAddress);
        }

        return memoryPages;
    }

    public Memory readMemory(long address, int bytesToRead) {
        IntByReference read = new IntByReference(0);
        Memory output = new Memory(bytesToRead);

        kernel32.ReadProcessMemory(processHandle, new Pointer(address), output, bytesToRead, read);
        return output;
    }

    public Memory readMemory(Pointer address, BaseTSD.SIZE_T bytesToRead) {
        IntByReference read = new IntByReference(0);
        Memory output = new Memory(bytesToRead.intValue());

        kernel32.ReadProcessMemory(processHandle, address, output, bytesToRead.intValue(), read);
        return output;
    }

    public int writeMemory(long address, Memory bytesToWrite) {
        IntByReference write = new IntByReference(0);
        kernel32.WriteProcessMemory(processHandle, new Pointer(address), bytesToWrite, (int) bytesToWrite.size(), write);
        return write.getValue();
    }

    public void kill() {
        kernel32.TerminateProcess(processHandle, 0);
        kernel32.CloseHandle(processHandle);
    }

    public MMReader getMemoryReader() {
        return new MMReader(this);
    }

    public String getProcessName() {
        return processName;
    }

    public int getProcessID() {
        return processID;
    }
}
