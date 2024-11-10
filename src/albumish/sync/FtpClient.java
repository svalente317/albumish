/*
 *  Copyright (c) 2017  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish.sync;

import java.io.IOException;
import java.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;

public class FtpClient implements SyncClient {
    private final FTPClient ftp;
    private final String top;

    public FtpClient(String hostname, int port, String username, String password, String top)
            throws IOException {
        this.ftp = new FTPClient();
        FTPClientConfig config = new FTPClientConfig();
        this.ftp.configure(config);
        this.ftp.connect(hostname, port);
        System.out.println(ftp.getReplyString());
        int reply = this.ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            this.ftp.disconnect();
            throw new IOException("connection reply " + reply);
        }
       if (!this.ftp.login(username, password)) {
           this.ftp.disconnect();
           throw new IOException("login failed");
       }
       if (!top.endsWith("/")) {
           top += "/";
       }
        this.top = top;
    }

    public void listDirectory(String directory, List<String> subdirs, List<FileInfo> fileinfos) {
        try {
            var files = this.ftp.listFiles(this.top + directory);
            if (!directory.isEmpty()) {
                directory += "/";
            }
            for (var file : files) {
                String pathname = directory + file.getName();
                if (file.isDirectory()) {
                    subdirs.add(pathname);
                }
                else if (file.isFile()) {
                    FileInfo info = new FileInfo();
                    info.pathname = pathname;
                    info.size = file.getSize();
                    // TODO info.timeModified = file.lastModified() / 1000;
                    fileinfos.add(info);
                }
            }
        } catch (Exception exception) {
            System.err.println("Failed to list directory: " + exception);
        }
    }

    public boolean copyFile(String src, String dst) {
        return false;
    }

    public boolean makeDirectory(String directory) {
       return false;
    }

    public boolean removeFile(String pathname) {
        return false;
    }
}
