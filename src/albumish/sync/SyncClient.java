/*
 *  Copyright (c) 2017  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish.sync;

import java.util.List;

public interface SyncClient {
    /**
     * Read a directory on the sync target. Return its lists of files and subdirectories.
     *
     * @param directory relative path from root of sync target
     * @param subdirs output relative paths of subdirectories
     * @param fileinfos output relative paths and info of files in directory
     */
    void listDirectory(String directory, List<String> subdirs, List<FileInfo> fileinfos);

    /**
     * Copy a file to the sync target.
     *
     * @param src absolute path of file in local filesystem
     * @param dst relative path of file location on sync target
     */
    boolean copyFile(String src, String dst);

    /**
     * Make a directory and its parent directories on the sync target.
     */
    boolean makeDirectory(String directory);

    /**
     * Remove a file on the sync target.
     */
    boolean removeFile(String pathname);
}
