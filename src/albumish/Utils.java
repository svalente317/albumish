/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javazoom.jl.decoder.Bitstream;

public class Utils {
    public static void quietClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception exception) {
            }
        }
    }

    public static void quietClose(Bitstream closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception exception) {
            }
        }
    }

    public static List<String> read_directory_tree(File topdir, Set<String> known_files) {
        List<String> results = new ArrayList<>();
        Queue<File> workqueue = new LinkedList<>();
        workqueue.add(topdir);
        while (true) {
            File directory = workqueue.poll();
            if (directory == null) {
                break;
            }
            File[] files = directory.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    workqueue.add(file);
                } else if (file.isFile()) {
                    String pathname = file.getAbsolutePath();
                    if (!known_files.contains(pathname)) {
                        results.add(pathname);
                    }
                } else {
                    System.err.println(file.toString() + ": not a file or directory");
                }
            }
        }
        return results;
    }

    public static int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception ignore) {
            return 0;
        }
    }

    public static boolean isEmpty(String text) {
        return text == null || text.equals("");
    }

    public static final String BAD_CHARS = "/:?\"";

    /**
     * For each bad character --
     * If it appears with no space on either side of it, then replace it with a space.
     * If it appears with a space on one side of it, then remove it.
     * If it appears with a space on both sides of it, then remove it and the next space.
     */
    public static void name_to_filename(StringBuilder builder, String name) {
        boolean haveSpace = true;
        boolean needSpace = false;
        boolean skipSpace = false;
        for (int idx = 0; idx < name.length(); idx++) {
            char ch = name.charAt(idx);
            if (BAD_CHARS.indexOf(ch) >= 0) {
                needSpace = !haveSpace;
                haveSpace = false;
                skipSpace = true;
            } else {
                if (needSpace && ch != '.') {
                    builder.append(' ');
                    needSpace = false;
                }
                if (skipSpace) {
                    skipSpace = false;
                    if (ch == ' ') {
                        continue;
                    }
                }
                builder.append(ch);
                haveSpace = (ch == ' ');
            }
        }
    }

    /**
     * In vfat, a directory name can not end with a dot.
     */
    public static void name_to_dirname(StringBuilder output, String name) {
        name_to_filename(output, name);
        int idx = output.length() - 1;
        if (output.charAt(idx) == '.') {
            output.setCharAt(idx, '_');
        }
    }

    /**
     * Copy a file in the local file system.
     */
    public static void copy_file(File srcfile, File dstfile) {
        FileInputStream istream = null;
        FileOutputStream ostream = null;
        try {
            istream = new FileInputStream(srcfile);
            ostream = new FileOutputStream(dstfile);
            FileChannel inputChannel = istream.getChannel();
            FileChannel outputChannel = ostream.getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            ostream.close();
            istream.close();
        } catch (Exception exception) {
            System.err.println(exception.toString());
            quietClose(ostream);
            quietClose(istream);
        }
    }

    public static String basename(String filename) {
        int idx = filename.lastIndexOf('/');
        return idx >= 0 ? filename.substring(idx + 1) : filename;
    }

    public static String[] addAll(String[] array1, String... array2) {
        String[] joinedArray = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }
}
