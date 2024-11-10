/*
 *  Copyright (c) 2017  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish.sync;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class FileSystemClient implements SyncClient {
    private String top;

    public FileSystemClient(String top) {
        this.top = top;
    }

    public void listDirectory(String directory, List<String> subdirs, List<FileInfo> fileinfos) {
        File[] files = new File(this.top, directory).listFiles();
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

    public boolean copyFile(String src, String dst) {
        try {
            Files.copy(Paths.get(src), Paths.get(this.top, dst),
                    StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception exception) {
            System.err.println("Failed to copy file: " + exception);
            return false;
        }
    }

    public boolean makeDirectory(String directory) {
        try {
            Files.createDirectories(Paths.get(this.top, directory));
            return true;
        } catch (Exception exception) {
            System.err.println("Failed to make directory: " + exception);
            return false;
        }
    }

    public boolean removeFile(String pathname) {
        try {
            Files.delete(Paths.get(this.top, pathname));
            return true;
        } catch (Exception exception) {
            System.err.println("Failed to delete file: " + exception);
            return false;
        }
    }
}
