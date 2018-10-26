/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

/**
 * Playlist is simply a list of song IDs. Each song in a playlist also has a Playlist ID (pid),
 * which makes it possible to "mark" songs in a playlist.
 *
 * You are playing the Nth song in the playlist, which has PID X. Then, you delete the first song in
 * the playlist. Now, you are playing the (N-1)th song in the playlist, but it is still PID X.
 */
public class Playlist {
    public String name;
    public int id;

    private IntList song_list;
    private IntList pid_list;
    private int next_pid;
    public boolean is_changed;

    public Playlist() {
        this.song_list = new IntList();
        this.pid_list = new IntList();
        this.next_pid = 1;
    }

    /**
     * Get the list of songids in the playlist for read-only access.
     */
    public IntList get_song_list() {
        return this.song_list;
    }

    /**
     * Add a song to the end of the playlist.
     */
    public void add(int songid) {
        this.song_list.add(songid);
        this.pid_list.add(this.next_pid);
        this.next_pid++;
        this.is_changed = true;
    }

    /**
     * Get the nth pid in the playlist.
     */
    public int get_nth_pid(int song_idx) {
        if (song_idx >= this.pid_list.size()) {
            return 0;
        }
        return this.pid_list.get(song_idx);
    }

    /**
     * Given a pid, find its current index in the playlist.
     */
    public int get_idx_of_pid(int pid) {
        for (int idx = 0; idx < this.pid_list.size(); idx++) {
            if (this.pid_list.get(idx) == pid) {
                return idx;
            }
        }
        return -1;
    }

    /**
     * Get the pid of the song in a location relative to another song.
     */
    public int get_next_pid(int pid, int delta) {
        int idx = get_idx_of_pid(pid);
        if (idx < 0) {
            return 0;
        }
        idx += delta;
        if (idx < 0 || idx >= this.pid_list.size()) {
            return 0;
        }
        return this.pid_list.get(idx);
    }

    /**
     * Given a pid, find the corresponding songid in the playlist.
     */
    public int get_songid_of_pid(int pid) {
        int idx = get_idx_of_pid(pid);
        if (idx < 0) {
            return 0;
        }
        return this.song_list.get(idx);
    }

    /**
     * Remove the nth song from the playlist.
     */
    public void remove_song_by_idx(int idx) {
        this.song_list.remove(idx);
        this.pid_list.remove(idx);
        this.is_changed = true;
    }

    /**
     * Replace the playlist contents.
     */
    public void reset(IntList new_song_list) {
        this.song_list.truncate(0);
        this.pid_list.truncate(0);
        if (new_song_list != null) {
            for (int idx = 0; idx < new_song_list.size(); idx++) {
                add(new_song_list.get(idx));
            }
        }
    }
}
