package ru.linachan.memorymanager;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import ru.linachan.yggdrasil.common.SystemInfo;
import ru.linachan.yggdrasil.plugin.YggdrasilPlugin;
import ru.linachan.yggdrasil.plugin.helpers.OSSupport;
import ru.linachan.yggdrasil.plugin.helpers.Plugin;

import java.util.ArrayList;
import java.util.List;

@OSSupport(SystemInfo.OSType.WINDOWS)
@Plugin(name = "MemoryManager", description = "Provides ability to manage process memory")
public class MMPlugin implements YggdrasilPlugin {

    static final Kernel32 kernel32 = Kernel32.INSTANCE;

    private MMProcess attachedProcess = null;

    @Override
    public void onInit() {

    }

    @Override
    public void onShutdown() {

    }

    public List<MMProcess> getProcessesByName(String processName) {
        List<MMProcess> processes = new ArrayList<>();

        WinNT.HANDLE snapshot = null;

        try {
            snapshot = kernel32.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));
            Tlhelp32.PROCESSENTRY32 entry = new Tlhelp32.PROCESSENTRY32();
            kernel32.Process32First(snapshot, entry);

            do {
                String processEXEName = Native.toString(entry.szExeFile);
                if (processEXEName.equals(processName)) {
                    processes.add(new MMProcess(Native.toString(entry.szExeFile), entry.th32ProcessID.intValue()));
                }
            } while(kernel32.Process32Next(snapshot, entry));
        } finally {
            kernel32.CloseHandle(snapshot);
        }

        return processes;
    }

    public List<MMProcess> getAllProcesses() throws LastErrorException {
        List<MMProcess> processes = new ArrayList<>();
        WinNT.HANDLE snapshot = null;

        try {
            snapshot = kernel32.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));
            Tlhelp32.PROCESSENTRY32 entry = new Tlhelp32.PROCESSENTRY32();
            kernel32.Process32First(snapshot, entry);

            do {
                processes.add(new MMProcess(Native.toString(entry.szExeFile), entry.th32ProcessID.intValue()));
            } while(kernel32.Process32Next(snapshot, entry));
        } finally {
            kernel32.CloseHandle(snapshot);
        }

        return processes;
    }

    public void attachProcess(MMProcess process) {
        process.openProcess(
            MMProcess.PROCESS_VM_READ | MMProcess.PROCESS_VM_WRITE | MMProcess.PROCESS_VM_OPERATIONS | MMProcess.PROCESS_QUERY_INFO
        );

        attachedProcess = process;
    }

    public void detachProcess() {
        attachedProcess = null;
    }

    public MMProcess getAttachedProcess() {
        return attachedProcess;
    }

    public boolean isAttached() {
        return attachedProcess != null;
    }
}
