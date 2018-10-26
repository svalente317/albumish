/*
 *  Copyright (c) 2017  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * This class use the CLI tooks gvfs-ls, etc. to provide a Java client library to read and write the
 * GVFS filesystem. Obviously, this is a bit hacky. It should be pretty easy to drop-in a real java
 * library, if such a library exists.
 */
public class GvfsClient {
    /**
     * @return list of URIs for MTP devices
     */
    public static String[] getMtpDeviceList() {
        ProcessBuilder pb = new ProcessBuilder("gvfs-ls", "-c", "mtp://");
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
            results.add(line);
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
        return results.toArray(new String[results.size()]);
    }

    public static class FileInfo {
        String pathname;
        long size;
        long timeModified;
    }

    /**
     * Read a directory in GVFS. Return its lists of files and subdirectories.
     */
    public static void listDirectory(String rootUri, String directory, List<String> subdirs,
            List<FileInfo> files) {
        if (rootUri.startsWith("/")) {
            listRegularDirectory(rootUri, directory, subdirs, files);
            return;
        }
        ProcessBuilder pb = new ProcessBuilder(
                "gvfs-ls", "-a", "standard::size,time::modified", rootUri + "/" + directory);
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
            if (values == null || values.length != 4) {
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

    private static void listRegularDirectory(String rootUri, String directory,
            List<String> subdirs, List<FileInfo> fileinfos) {
        File[] files = new File(rootUri, directory).listFiles();
        if (files == null) {
            return;
        }
        if (!directory.isEmpty()) {
            directory += "/";
        }
        for (File file : files) {
            String pathname = directory + file.getName();
            if (file.isDirectory()) {
                subdirs.add(pathname);
            }
            else if (file.isFile()) {
                FileInfo info = new FileInfo();
                info.pathname = pathname;
                info.size = file.length();
                info.timeModified = file.lastModified() / 1000;
                fileinfos.add(info);
            }
        }
    }

    /**
     * Copy a file in GVFS.
     */
    public static boolean copyFile(String srcUri, String dstUri) {
        if (dstUri.startsWith("/")) {
            try {
                Files.copy(Paths.get(srcUri), Paths.get(dstUri),
                        StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (Exception exception) {
                System.err.println("Failed to copy file: " + exception);
                return false;
            }
        }
        return runProcess("gvfs-copy", srcUri, dstUri);
    }

    /**
     * Make a directory and its parent directories in GVFS.
     */
    public static boolean makeDirectory(String srcUri) {
        if (srcUri.startsWith("/")) {
            try {
                Files.createDirectories(Paths.get(srcUri));
                return true;
            } catch (Exception exception) {
                System.err.println("Failed to make directory: " + exception);
                return false;
            }
        }
        return runProcess("gvfs-mkdir", "-p", srcUri);
    }

    /**
     * Remove a file in GVFS.
     */
    public static boolean removeFile(String dstUri) {
        if (dstUri.startsWith("/")) {
            try {
                Files.delete(Paths.get(dstUri));
                return true;
            } catch (Exception exception) {
                System.err.println("Failed to delete file: " + exception);
                return false;
            }
        }
        return runProcess("gvfs-rm", dstUri);
    }

    private static boolean runProcess(String... args) {
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
