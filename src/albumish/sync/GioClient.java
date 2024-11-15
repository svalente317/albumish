/*
 *  Copyright (c) 2017  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class use the "gio" CLI tool to provide a Java client library to read and write the GIO
 * filesystem. Obviously, this is a bit hacky. It should be pretty easy to drop-in a real java
 * library, if such a library exists.
 */
public class GioClient implements SyncClient {
    private boolean use_gio;
    private boolean use_gvfs;

    public GioClient() {
        if (new File("/usr/bin/gio").exists()) {
            this.use_gio = true;
        } else if (new File("/usr/bin/gvfs-ls").exists()) {
            this.use_gvfs = true;
        }
    }

    /**
     * @return list of URIs for MTP devices
     */
    public String[] getMtpDeviceList() {
        String[] command;
        if (this.use_gio) {
            command = new String[] {"gio", "mount", "-l"};
        } else if (this.use_gvfs) {
            command = new String[] {"gvfs-ls", "-c", "mtp://"};
        } else {
            return null;
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process;
        try {
            process = pb.start();
        } catch (Exception exception) {
            System.err.println("Failed to start process: " + exception);
            return null;
        }
        InputStream istream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
        List<String> results = new ArrayList<>();
        while (true) {
            String line = null;
            try {
                line = reader.readLine();
            } catch (Exception exception) {
                System.err.println("Failed reading from process: " + exception);
            }
            if (line == null) {
                break;
            }
            if (this.use_gio) {
                String[] words = line.split(" ");
                if (words.length == 4 && words[0].startsWith("Mount(") &&
                        words[1].equals("mtp") && words[2].equals("->")) {
                    try {
                        results.add(URLDecoder.decode(words[3], StandardCharsets.UTF_8));
                    } catch (Exception dummy) {}
                }
            } else {
                results.add(line);
            }
        }
        try {
            reader.close();
        } catch (Exception exception) {
            System.err.println("Failed to close stdout: " + exception);
        }
        try {
            process.waitFor();
        } catch (Exception exception) {
            System.err.println("Failed to clean up process: " + exception);
        }
        return results.toArray(new String[0]);
    }

    /**
     * Read a directory in GIO. Return its lists of files and subdirectories.
     */
    public void listDirectory(String directory, List<String> subdirs, List<FileInfo> files) {
        String rootUri = "TODO FIXME";
        ProcessBuilder pb = new ProcessBuilder(makeCommand("list",
                "-a", "standard::size,time::modified", rootUri + "/" + directory));
        Process process;
        try {
            process = pb.start();
        } catch (Exception exception) {
            System.err.println("Failed to start process: " + exception);
            return;
        }
        if (!directory.isEmpty()) {
            directory += "/";
        }
        InputStream istream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
        while (true) {
            String line = null;
            try {
                line = reader.readLine();
            } catch (Exception exception) {
                System.err.println("Failed reading from process: " + exception);
            }
            if (line == null) {
                break;
            }
            String[] values = line.split("\t");
            if (values.length != 4) {
                System.out.println("invalid line: " + line);
                continue;
            }
            FileInfo info = new FileInfo();
            String name = values[0];
            if (name.equals("")) {
                System.out.println("invalid line (empty name): " + line);
                continue;
            }
            info.pathname = directory + name;
            try {
                info.size = Long.parseLong(values[1]);
            } catch (Exception exception) {
                System.out.println("invalid line (bad size): " + line);
                continue;
            }
            String type = values[2];
            boolean isDirectory = type.equals("(directory)");
            if (!(isDirectory || type.equals("(regular)"))) {
                System.out.println("invalid line (bad type): " + line);
                continue;
            }
            String modified = values[3];
            String prefix = "time::modified=";
            if (!modified.startsWith(prefix)) {
                System.out.println("invalid line (bad time modified): " + line);
                continue;
            }
            try {
                info.timeModified = Long.parseLong(modified.substring(prefix.length()));
            } catch (Exception exception) {
                System.out.println("invalid line (bad time modified): " + line);
                continue;
            }
            if (isDirectory) {
                subdirs.add(info.pathname);
            } else {
                files.add(info);
            }
        }
        try {
            reader.close();
        } catch (Exception exception) {
            System.err.println("Failed to close stdout: " + exception);
        }
        try {
            process.waitFor();
        } catch (Exception exception) {
            System.err.println("Failed to clean up process: " + exception);
        }
    }

    /**
     * Copy a file in GIO.
     */
    public boolean copyFile(String srcUri, String dstUri) {
        return runProcess(makeCommand("copy", srcUri, dstUri));
    }

    /**
     * Make a directory and its parent directories in GIO.
     */
    public boolean makeDirectory(String srcUri) {
        return runProcess(makeCommand("mkdir", "-p", srcUri));
    }

    /**
     * Remove a file in GIO.
     */
    public boolean removeFile(String dstUri) {
        return runProcess(makeCommand("remove", dstUri));
    }

    private String[] makeCommand(String...args) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, args);
        if (this.use_gio) {
            list.add(0, "gio");
        } else if (this.use_gvfs) {
            String c = args[0];
            list.set(0, c.equals("list") ? "gvfs-ls" : c.equals("copy") ? "gvfs-cp" :
                c.equals("mkdir") ? "gvfs-mkdir" : c.equals("remove") ? "gvfs-rm" : null);
        } else {
            return null;
        }
        return list.toArray(new String[0]);
    }

    private boolean runProcess(String[] args) {
        ProcessBuilder pb = new ProcessBuilder(args);
        Process process;
        try {
            process = pb.start();
        } catch (Exception exception) {
            System.err.println("Failed to start process: " + exception);
            return false;
        }
        int status;
        try {
            status = process.waitFor();
        } catch (Exception exception) {
            System.err.println("Failed to clean up process: " + exception);
            return false;
        }
        if (status != 0) {
            System.out.println("status=" + status + " for " + args[0] + " " +
                    args[args.length - 1]);
        }
        return status == 0;
    }
}
