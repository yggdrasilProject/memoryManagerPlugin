package ru.linachan.memorymanager;

import ru.linachan.memorymanager.utils.MMUtils;
import ru.linachan.yggdrasil.common.console.tables.Table;
import ru.linachan.yggdrasil.plugin.YggdrasilPluginManager;
import ru.linachan.yggdrasil.shell.YggdrasilShellCommand;
import ru.linachan.yggdrasil.shell.helpers.CommandAction;
import ru.linachan.yggdrasil.shell.helpers.ShellCommand;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ShellCommand(command = "mm", description = "Manipulate process memory")
public class MMCommand extends YggdrasilShellCommand {

    private MMPlugin mmEngine;

    @Override
    protected void init() throws IOException {
        mmEngine = core.getManager(YggdrasilPluginManager.class).get(MMPlugin.class);
    }

    @CommandAction("List processes")
    public void ps() throws IOException {
        Map<Integer, String> processList;

        if (kwargs.containsKey("name")) {
            processList = MMUtils.processListToMap(mmEngine.getProcessesByName(kwargs.get("name")));
        } else {
            processList = MMUtils.processListToMap(mmEngine.getAllProcesses());
        }

        console.writeTable(new Table(processList, "PID", "Process Name"));
    }

    @CommandAction("Attach to process")
    public void attach() throws IOException {
        if (kwargs.containsKey("pid")) {
            Optional<MMProcess> processOptional = mmEngine.getAllProcesses().stream()
                    .filter(process -> String.valueOf(process.getProcessID()).equals(kwargs.get("pid")))
                    .findFirst();

            if (processOptional.isPresent()) {
                if (mmEngine.isAttached()) {
                    if (!console.readYesNo(String.format("MMPlugin is already attached to '%s'. Do you want to detach it?", mmEngine.getAttachedProcess().getProcessName()))) {
                        console.writeLine("Canceled");
                        return;
                    }
                }

                MMProcess process = processOptional.get();
                mmEngine.attachProcess(process);

                console.writeLine("Attached to '%s' at PID%d", process.getProcessName(), process.getProcessID());
            }
        } else if (kwargs.containsKey("name")) {
            List<MMProcess> processes = mmEngine.getProcessesByName(kwargs.get("name"));

            if (processes.size() > 1) {
                console.writeLine("Multiple processes found. Provide --pid instead.");
                console.writeTable(new Table(
                    MMUtils.processListToMap(processes), "PID", "Process Name"
                ));
            } else if (processes.size() == 1) {
                if (mmEngine.isAttached()) {
                    if (!console.readYesNo(String.format("MMPlugin is already attached to '%s'. Do you want to detach it?", mmEngine.getAttachedProcess().getProcessName()))) {
                        console.writeLine("Canceled");
                        return;
                    }
                }

                MMProcess process = processes.get(0);
                mmEngine.attachProcess(process);

                console.writeLine("Attached to '%s' at PID%d", process.getProcessName(), process.getProcessID());
            }
        } else {
            if (mmEngine.isAttached()) {
                console.writeLine(
                        "MMPlugin is attached to '%s' at PID%d",
                        mmEngine.getAttachedProcess().getProcessName(),
                        mmEngine.getAttachedProcess().getProcessID()
                );
            } else {
                console.writeLine("No attached process");
            }
        }
    }

    @CommandAction("Detach from process")
    public void detach() throws IOException {
        if (mmEngine.isAttached()) {
            if (kwargs.containsKey("kill")) {
                console.writeLine("Killing process PID%d", mmEngine.getAttachedProcess().getProcessID());
                mmEngine.getAttachedProcess().kill();
            }

            mmEngine.detachProcess();
            console.writeLine("Detached");
        } else {
            console.writeLine("No process attached");
        }
    }

    @CommandAction("Dump process memory")
    public void dump() throws IOException {
        if (mmEngine.isAttached()) {
            if (kwargs.containsKey("address")&&kwargs.containsKey("length")) {
                long address = Long.decode(kwargs.get("address"));
                int length = Integer.parseInt(kwargs.get("length"));

                for (String line: mmEngine.getAttachedProcess().dumpMemory(address, length).split("\r\n")) {
                    console.writeLine(line);
                }
            }
        } else {
            console.writeLine("No process attached");
        }
    }

    @CommandAction("Search value in process memory")
    public void search() throws IOException {
        if (mmEngine.isAttached()) {
            String value = kwargs.getOrDefault("value", null);
            Integer bytesPerChar = Integer.parseInt(kwargs.getOrDefault("bytes", "2"));
            String type = kwargs.getOrDefault("type", "string");

            MMReader memoryReader = mmEngine.getAttachedProcess()
                .getMemoryReader();

            if (value != null) {
                Table searchResults = new Table("Address", "Value");

                switch (type) {
                    case "string":
                        memoryReader.findString(value, bytesPerChar).forEach(result -> searchResults.addRow(
                                String.format("%08X", result),
                                memoryReader.readString(result, bytesPerChar))
                        );
                        break;
                    case "byte":
                        memoryReader.findByte(Byte.parseByte(value)).forEach(result -> searchResults.addRow(
                                String.format("%08X", result),
                                String.valueOf(memoryReader.readByte(result))
                        ));
                        break;
                    case "short":
                        memoryReader.findShort(Short.parseShort(value)).forEach(result -> searchResults.addRow(
                                String.format("%08X", result),
                                String.valueOf(memoryReader.readShort(result))
                        ));
                        break;
                    case "int":
                        memoryReader.findInt(Integer.parseInt(value)).forEach(result -> searchResults.addRow(
                                String.format("%08X", result),
                                String.valueOf(memoryReader.readInt(result))
                        ));
                        break;
                    case "long":
                        memoryReader.findLong(Long.parseLong(value)).forEach(result -> searchResults.addRow(
                                String.format("%08X", result),
                                String.valueOf(memoryReader.readLong(result))
                        ));
                        break;
                    case "float":
                        memoryReader.findFloat(Float.parseFloat(value)).forEach(result -> searchResults.addRow(
                                String.format("%08X", result),
                                String.valueOf(memoryReader.readFloat(result))
                        ));
                        break;
                    case "double":
                        memoryReader.findDouble(Double.parseDouble(value)).forEach(result -> searchResults.addRow(
                                String.format("%08X", result),
                                String.valueOf(memoryReader.readDouble(result))
                        ));
                        break;
                    default:
                        console.writeLine("Unknown type: '%s'", type);
                        break;
                }

                console.writeTable(searchResults);
            } else {
                console.writeLine("No pattern provided");
            }
        } else {
            console.writeLine("No process attached");
        }
    }

    @Override
    protected void onInterrupt() {

    }
}
