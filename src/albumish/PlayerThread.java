/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.FileInputStream;
import java.io.InputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;

public class PlayerThread extends Thread {

    // Display the playing time every 333 milliseconds.
    private static final int UPDATE_PERIOD = 333;

    private Jukebox jukebox;
    private int playing_listid;
    private int playing_song_pid;
    private int next_listid;
    private int next_song_pid;
    private boolean is_task_scheduled;
    private AudioDevice audio;
    private Bitstream bitstream;
    private Decoder decoder;
    private int interruption_count;
    private float audio_position;
    private boolean is_paused;
    private Float target_position;

    public PlayerThread(Jukebox jukebox) {
        this.jukebox = jukebox;
        this.playing_listid = -1;
        this.next_listid = -1;
    }

    @Override
    public void run() {
        while (true) {
            InputStream istream = null;
            synchronized (this) {
                if (this.next_listid < 0) {
                    // Wait for the main thread to call play_song().
                    try {
                        wait();
                    } catch (Exception exception) {
                    }
                    continue;
                }
                Playlist playlist = this.jukebox.playlists.get(this.next_listid);
                if (playlist == null) {
                    this.playing_listid = -1;
                    this.next_listid = -1;
                    update_top_panel(null);
                    continue;
                }
                if (this.next_song_pid == 0) {
                    this.next_song_pid = playlist.get_next_pid(this.playing_song_pid, +1);
                }
                int songid = playlist.get_songid_of_pid(this.next_song_pid);
                if (songid <= 0) {
                    this.playing_listid = -1;
                    this.next_listid = -1;
                    update_top_panel(null);
                    continue;
                }
                Song song = this.jukebox.database.song_list.get(songid);
                this.playing_listid = this.next_listid;
                this.playing_song_pid = this.next_song_pid;
                this.next_song_pid = 0;
                try {
                    FactoryRegistry registry = FactoryRegistry.systemRegistry();
                    this.audio = registry.createAudioDevice();
                    istream = new FileInputStream(song.filename);
                    this.bitstream = new Bitstream(istream);
                    this.decoder = new Decoder();
                    this.audio.open(this.decoder);
                } catch (Exception exception) {
                    System.err.println(exception.toString());
                    Utils.quietClose(istream);
                    continue;
                }
                update_top_panel(song);
                this.interruption_count = 0;
            }
            this.audio_position = 0;
            try {
                boolean value = true;
                while (value && this.interruption_count == 0) {
                    if (this.is_paused) {
                        synchronized (this) {
                            if (this.is_paused) {
                                wait();
                            }
                        }
                        continue;
                    }
                    if (this.target_position != null) {
                        jump_in_audio_thread(this.target_position);
                        this.target_position = null;
                    }
                    value = decodeFrame();
                }
            } catch (Exception exception) {
                System.err.println(exception.toString());
            }
            this.audio.flush();
            this.audio.close();
            Utils.quietClose(this.bitstream);
            Utils.quietClose(istream);
        }
    }

    private boolean decodeFrame() throws Exception {
        Header header = this.bitstream.readFrame();
        if (header == null) {
            return false;
        }
        SampleBuffer output = (SampleBuffer) this.decoder.decodeFrame(header, this.bitstream);
        this.audio.write(output.getBuffer(), 0, output.getBufferLength());
        this.bitstream.closeFrame();
        this.audio_position += header.ms_per_frame();
        return true;
    }

    /**
     * Jump forward or backwards in the audio stream.
     */
    private void jump_in_audio_thread(float target_position) throws Exception {
        this.audio.flush();
        // TODO jump backwards
        while (this.audio_position < target_position) {
            Header header = this.bitstream.readFrame();
            if (header == null) {
                return;
            }
            this.bitstream.closeFrame();
            this.audio_position += header.ms_per_frame();
        }
    }

    /**
     * This is called by the audio thread when it starts playing a new song, and when it reaches the
     * end of the playlist.
     */
    private void update_top_panel(final Song song) {
        this.jukebox.main_window.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                PlayerThread.this.jukebox.update_top_panel(song);
            }
        });
    }

    /**
     * This is called by the GUI thread to start playing a specific song. It's synchronized so that
     * it can safely communicate with the audio thread.
     */
    public void play_song_pid(int listid, int song_pid) {
        synchronized (this) {
            this.next_listid = listid;
            this.next_song_pid = song_pid;
            if (this.playing_listid < 0) {
                notify();
            } else {
                this.interruption_count = 1;
                pause(false);
            }
            if (!this.is_task_scheduled) {
                schedule_task();
                this.is_task_scheduled = true;
            }
        }
    }

    /**
     * This is called by the GUI thread to start playing the previous or next song in the playlist.
     */
    public void play_next_song(int delta) {
        synchronized (this) {
            if (this.playing_listid < 0) {
                return;
            }
            Playlist playlist = this.jukebox.playlists.get(this.playing_listid);
            if (playlist == null) {
                return;
            }
            int pid = playlist.get_next_pid(this.playing_song_pid, delta);
            play_song_pid(this.playing_listid, pid);
        }
    }

    /**
     * This is called by the GUI thread when it starts playing a new song, and then it continuously
     * calls itself, to remain in the GUI thread.
     */
    private void schedule_task() {
        this.jukebox.main_window.getDisplay().timerExec(UPDATE_PERIOD, new Runnable() {
            @Override
            public void run() {
                get_and_display_time();
            }
        });
    }

    /**
     * This runs in the GUI thread while a song is playing. It's synchronized so that it can
     * communicate with the audio thread to find out when the player is paused or stopped.
     */
    private void get_and_display_time() {
        boolean is_jumping;
        int position;
        boolean reschedule = true;
        synchronized (this) {
            if (this.playing_listid < 0) {
                this.is_task_scheduled = false;
                return;
            }
            if (this.is_paused) {
                this.is_task_scheduled = false;
                reschedule = false;
            }
            is_jumping = this.target_position != null;
            position = (int) this.audio_position;
        }
        if (!is_jumping) {
            this.jukebox.display_time(position);
        }
        if (reschedule) {
            schedule_task();
        }
    }

    /**
     * This is called by the GUI thread to pause and unpause the player. When this unpauses the
     * player, it restarts the scheduled task process, just like play_song() does.
     *
     * @return true if the player is now paused
     */
    public boolean pause(boolean do_pause) {
        synchronized (this) {
            if (this.playing_listid < 0) {
                return false;
            }
            if (this.is_paused == do_pause) {
                return this.is_paused;
            }
            this.is_paused = do_pause;
            if (!this.is_paused) {
                if (!this.is_task_scheduled) {
                    schedule_task();
                    this.is_task_scheduled = true;
                }
                notify();
            }
            return this.is_paused;
        }
    }

    /**
     * This is called by the GUI thread. If it returns true, then the player will remain paused
     * until the GUI thread unpauses it.
     */
    public boolean is_paused() {
        return this.is_paused;
    }

    /**
     * Tell the player to jump forwards or backwards in the audio stream.
     */
    public void do_audio_jump(float value) {
        this.target_position = new Float(value);
    }

    /**
     * @return the length of the audio stream that has been played.
     */
    public float get_audio_position_in_ms() {
        return this.audio_position;
    }

    public int get_playing_listid() {
        return this.playing_listid;
    }

    public int get_playing_song_pid() {
        return this.playing_song_pid;
    }
}
