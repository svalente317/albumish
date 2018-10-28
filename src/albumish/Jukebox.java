/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.File;
import java.io.InputStream;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

public class Jukebox implements SelectionListener {

    private enum Cmd {
        ADD_FOLDER,
        RIP_CD,
        SYNC_TO_DEVICE,
        QUIT,
        PREV_ALBUM,
        NEXT_ALBUM,
        JUMP_ALBUM,
        LOAD_ALBUM_ART,
        NEW_PLAYLIST,
        DELETE_PLAYLIST,
        ADD_SELECTION,
        DELETE_SELECTION,
        RANDOM_PLAYLIST,
        EXPORT_SONGS
    };

    // Draw unselected albums in a square of 225x225.
    // Draw the selected album in a square of 300x300.
    static int[] COVER_SIZES = { 225, 300 };

    private static final String RANDOM_PLAYLIST_PREFIX = "Random Playlist ";
    private static final int RANDOM_PLAYLIST_SIZE = 100;

    public Database database;
    public CheckDatabase check_database;
    public PlaylistCollection playlists;
    public Gallery gallery;
    public Shell main_window;

    private final FilterWorker filter_worker;
    private final TopPanel top_panel;
    private final CoverPanel cover_panel;
    private final SortPanel sort_panel;
    private final PlaylistPanel playlist_panel;
    private final SongPanel playlist_song_panel;
    private final SongPanel album_song_panel;
    private final PlayerThread player_thread;
    private int num_random_playlists;

    public static void main(String[] argv) throws Exception {
        new Jukebox().run();
        System.exit(0);
    }

    public Jukebox() throws Exception {
        // Initialize the non-graphical objects:
        // database, check_database, playlists, gallery, and filters.
        Display display = new Display();
        Rectangle bounds = display.getBounds();
        // If display width is 1920 or less, then use the normal album sizes.
        // If display width is 3840 or more, then double the album sizes.
        // If the display width is in between 1920 and 3840, then that's weird.
        if (bounds.width >= 3840) {
            COVER_SIZES[0] *= 2;
            COVER_SIZES[1] *= 2;
        }
        String home = System.getProperty("user.home");
        File directory = new File(home, ".albumish");
        this.database = new Database();
        this.database.load(directory, "database.json");
        this.check_database = new CheckDatabase(this.database, directory, "checked_songs.list");
        this.playlists = new PlaylistCollection(this.database, directory);
        File gallery_dir = new File(home, "Pictures/covers");
        this.gallery = new Gallery(this, display, gallery_dir, COVER_SIZES);
        this.filter_worker = new FilterWorker(this.database, this.check_database);

        // Create the main window.
        GridData data;
        this.main_window = new Shell(display);
        this.main_window.setImage(get_icon("CD.png"));
        this.main_window.setText("Albumish");
        this.main_window.setLayout(new GridLayout(3, false));
        makeMenuBar(this.main_window);
        this.top_panel = new TopPanel(this, this.main_window);
        data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
        this.top_panel.setLayoutData(data);
        this.cover_panel = new CoverPanel(this, this.main_window);
        data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
        data.heightHint = this.cover_panel.get_height();
        data.widthHint = bounds.width;
        this.cover_panel.setLayoutData(data);
        this.sort_panel = new SortPanel(this, this.main_window);
        data = new GridData(SWT.CENTER, SWT.BEGINNING, false, false);
        this.sort_panel.getControl().setLayoutData(data);
        this.playlist_song_panel = new SongPanel(this, this.main_window, false);
        data = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
        this.playlist_song_panel.getControl().setLayoutData(data);
        this.album_song_panel = new SongPanel(this, this.main_window, true);
        data = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
        this.album_song_panel.getControl().setLayoutData(data);
        this.playlist_panel = new PlaylistPanel(this, this.playlists);
        data = new GridData(SWT.FILL, SWT.FILL, false, true);
        data.heightHint = bounds.height / 5;
        this.playlist_panel.getControl().setLayoutData(data);
        this.main_window.pack();
        this.main_window.open();

        this.cover_panel.sort(this.sort_panel.get_selected_sort());
        set_filter(null);
        this.top_panel.grab_focus();
        this.player_thread = new PlayerThread(this);
        this.player_thread.start();
    }

    public Image get_icon(String iname) {
        InputStream istream = null;
        Image image = null;
        try {
            istream = getClass().getResourceAsStream("icons/" + iname);
            image = new Image(this.main_window.getDisplay(), istream);
        } catch (Exception exception) {
            System.err.println(exception.toString());
        }
        Utils.quietClose(istream);
        return image;
    }

    private void makeMenuBar(Shell shell) {
        String platform = SWT.getPlatform().toLowerCase();
        Menu menubar = new Menu(shell, SWT.BAR);
        MenuItem cascade;
        Menu menu;

        cascade = new MenuItem(menubar, SWT.CASCADE);
        cascade.setText("&Library");
        menu = new Menu(shell, SWT.DROP_DOWN);
        cascade.setMenu(menu);
        addMenuItem(menu, "&Add Folder to Library...", "A", Cmd.ADD_FOLDER, null);
        addMenuItem(menu, "&Rip CD to Library...", null, Cmd.RIP_CD, null);
        addMenuItem(menu, "&Sync to Device...", null, Cmd.SYNC_TO_DEVICE, null);
        String label = (platform.startsWith("win") ? "E&xit" : "&Quit");
        String altkey = (platform.equalsIgnoreCase("carbon") ? "Q" : null);
        addMenuItem(menu, label, altkey, Cmd.QUIT, null);

        cascade = new MenuItem(menubar, SWT.CASCADE);
        cascade.setText("&Albums");
        menu = new Menu(shell, SWT.DROP_DOWN);
        cascade.setMenu(menu);
        addMenuItem(menu, "&Previous Album", "F11", Cmd.PREV_ALBUM, null);
        addMenuItem(menu, "&Next Album", "F12", Cmd.NEXT_ALBUM, null);
        addMenuItem(menu, "&Jump to Playing Album", "J", Cmd.JUMP_ALBUM, null);
        addMenuItem(menu, "&Load Album Art...", null, Cmd.LOAD_ALBUM_ART, null);

        cascade = new MenuItem(menubar, SWT.CASCADE);
        cascade.setText("&Playlist");
        menu = new Menu(shell, SWT.DROP_DOWN);
        cascade.setMenu(menu);
        addMenuItem(menu, "&New Playlist...", "N", Cmd.NEW_PLAYLIST, null);
        addMenuItem(menu, "&Delete Playlist...", null, Cmd.DELETE_PLAYLIST, null);
        addMenuItem(menu, "Add Selection to &Playlist", "Y", Cmd.ADD_SELECTION, null);
        addMenuItem(menu, "Delete Selection from Playlist", "X", Cmd.DELETE_SELECTION, null);
        addMenuItem(menu, "New Random Playlist", null, Cmd.RANDOM_PLAYLIST, null);
        addMenuItem(menu, "Export Checked Songs...", null, Cmd.EXPORT_SONGS, null);

        shell.setMenuBar(menubar);
    }

    private void addMenuItem(Menu menu, String label, String key, Cmd cmd,
            String iname) {
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        if (key != null && key.length() == 1) {
            label += "\tCtrl+" + key;
            item.setAccelerator(SWT.MOD1 | key.charAt(0));
        }
        if (key != null && key.length() > 1) {
            label += "\t" + key;
            item.setAccelerator(key.equals("F11") ? SWT.F11 : key.equals("F12") ? SWT.F12 : 0);
        }
        item.setText(label);
        item.setData(cmd);
        item.addSelectionListener(this);
    }

    /**
     * Called by top_panel. Update cover_panel.
     */
    public void set_filter(String text) {
        int[] album_list;
        if (text == null || text.isEmpty()) {
            album_list = this.filter_worker.remove_filter(0);
        } else {
            Filter filter = new Filter(0, text);
            album_list = this.filter_worker.add_filter(filter);
        }
        this.cover_panel.reset(album_list);
    }

    /**
     * Run the player until the window is closed.
     */
    public void run() {
        Display display = this.main_window.getDisplay();
        while (!this.main_window.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        // TODO this.album_song_panel.update_database();
        this.playlists.save(this.database);
        this.database.save();
        this.check_database.save();
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        Cmd command = (Cmd) event.widget.getData();
        if (command == null) {
            return;
        }
        switch (command) {
        case ADD_FOLDER:
            new AddToLibraryManager().add_to_library(this);
            break;
        case RIP_CD:
            new RipManager().rip_cd(this);
            break;
        case SYNC_TO_DEVICE:
            new SyncManager().sync_to_device(this);
            break;
        case QUIT:
            this.main_window.close();
            break;
        case PREV_ALBUM:
            this.cover_panel.select_next_album(-1);
            break;
        case NEXT_ALBUM:
            this.cover_panel.select_next_album(+1);
            break;
        case JUMP_ALBUM:
            int playing_listid = this.player_thread.get_playing_listid();
            int playing_song_pid = this.player_thread.get_playing_song_pid();
            if (playing_listid >= 0 && playing_song_pid > 0) {
                Playlist playlist = this.playlists.get(playing_listid);
                if (playlist != null) {
                    int songid = playlist.get_songid_of_pid(playing_song_pid);
                    if (songid > 0) {
                        Song song = this.database.song_list.get(songid);
                        this.cover_panel.select_album(song.albumid);
                        if (this.playlist_song_panel.get_playlistid() == playing_listid) {
                            int idx = playlist.get_idx_of_pid(playing_song_pid);
                            this.playlist_song_panel.select_nth_row(idx);
                        }
                    }
                }
            }
            break;
        case LOAD_ALBUM_ART:
            on_load_album_art();
            break;
        case NEW_PLAYLIST:
            InputDialog.InputRunnable runnable = new InputDialog.InputRunnable() {
                @Override
                public void run(String input) {
                    Jukebox.this.playlist_panel.new_playlist(input, null);
                }
            };
            new InputDialog(this.main_window, "Create New Playlist",
                    "Enter new playlist name.", null, runnable);
            break;
        case DELETE_PLAYLIST:
            // TODO delete playlist
            break;
        case ADD_SELECTION:
            add_selection_to_playlist();
            break;
        case DELETE_SELECTION:
            delete_selection_from_playlist();
            break;
        case RANDOM_PLAYLIST:
            new_random_playlist();
            break;
        case EXPORT_SONGS:
            FileDialog fd = new FileDialog(this.main_window, SWT.SAVE);
            fd.setText("Export Checked Songs to iTunes Playlist");
            String pathname = fd.open();
            if (pathname != null) {
                this.check_database.export_playlist(pathname);
            }
            break;
        default:
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
    }

    /**
     * Called by cover_panel. Update song_panel and album_song_panel.
     */
    public void select_album(int albumid) {
        if (albumid >= 0) {
            this.album_song_panel.update_database();
            IntList song_list = this.filter_worker.generate_song_list(albumid);
            this.album_song_panel.reset(song_list);
        }
    }

    /**
     * Called by gallery. Update cover_panel.
     */
    public void invalidate_album(int albumid) {
        this.cover_panel.invalidate_album(albumid);
    }

    /**
     * Called after updating library. Update cover_panel.
     */
    public void reset_albums() {
        int[] album_list = this.filter_worker.generate_album_list();
        this.cover_panel.reset(album_list);
    }

    /**
     * Called by sort_panel. Update cover_panel.
     */
    public void sort_albums(AlbumSorter.SortType sort_type) {
        this.cover_panel.sort(sort_type);
    }

    /**
     * Called by playlist_panel. Update playlist_song_panel.
     */
    public void select_playlist(int playlistid) {
        Playlist playlist = this.playlists.get(playlistid);
        if (playlist == null) {
            return;
        }
        this.playlist_song_panel.set_playlistid(playlistid);
        this.playlist_song_panel.reset(playlist.get_song_list());
    }

    /**
     * Called by song_panel. Update playlist panels.
     */
    public int set_auto_playlist(IntList song_list) {
        return this.playlist_panel.set_auto_playlist(song_list);
    }

    /**
     * Called by top_panel.
     */
    public void do_previous() {
        // If you press Previous in the first 3 seconds of a song, then go to the previous song.
        // Otherwise, restart the current song.
        int delta = this.player_thread.get_audio_position_in_ms() < 3000.0 ? -1 : 0;
        this.player_thread.play_next_song(delta);
    }

    /**
     * Called by top_panel.
     */
    public void do_next() {
        this.player_thread.play_next_song(+1);
    }

    /**
     * Called by top_panel.
     */
    public void do_play() {
        if (this.player_thread.is_paused()) {
            this.player_thread.pause(false);
            this.top_panel.display_pause_button();
            return;
        }
        int playlistid = this.playlist_song_panel.get_playlistid();
        if (playlistid >= 0) {
            int song_idx = this.playlist_song_panel.get_selected_idx();
            if (song_idx >= 0) {
                play_song(playlistid, song_idx);
            }
        }
    }

    /**
     * Called by top_panel.
     */
    public void do_pause() {
        if (this.player_thread.pause(true)) {
            this.top_panel.display_play_button();
        }
    }

    /**
     * Called by top_panel.
     */
    public void do_audio_jump(float position) {
        this.player_thread.do_audio_jump(position);
    }

    /**
     * Called by playlist_song_panel.
     */
    public void play_song(int listid, int song_idx) {
        Playlist playlist = this.playlists.get(listid);
        if (playlist != null) {
            int pid = playlist.get_nth_pid(song_idx);
            this.player_thread.play_song_pid(listid, pid);
        }
    }

    /**
     * Called by player_thread.
     */
    public void update_top_panel(Song song) {
        if (song == null) {
            this.top_panel.display(null, null, null, 0);
            return;
        }
        String artist_name = null;
        if (song.artistid > 0) {
            artist_name = this.database.artist_list.get(song.artistid).name;
        }
        String album_name = null;
        if (song.albumid > 0) {
            album_name = this.database.album_list.get(song.albumid).name;
        }
        this.top_panel.display(song.title, artist_name, album_name, song.duration);
    }

    /**
     * Called by player_thread.
     */
    public void display_time(int position) {
        this.top_panel.display_time(position);
    }

    /**
     * Called from the main menu.
     */
    public void on_load_album_art() {
        int albumid = this.cover_panel.get_selected_albumid();
        Album album = this.database.album_list.get(albumid);
        Artist artist = this.database.artist_list.get(album.artistid);
        String title = "";
        if (artist.name != null) {
            title = artist.name + " / ";
        }
        if (album.name != null) {
            title += album.name;
        }
        FileDialog dialog = new FileDialog(this.main_window, SWT.OPEN);
        dialog.setText(title);
        String filename = dialog.open();
        if (filename != null) {
            this.gallery.copy_file(albumid, filename);
            this.cover_panel.invalidate_album(albumid);
        }
    }

    private void add_selection_to_playlist() {
        IntList new_songs = this.album_song_panel.get_selection();
        int playlistid = this.playlist_panel.get_selected_playlist();
        if (new_songs.size() == 0 || playlistid < 0) {
            return;
        }
        Playlist playlist = this.playlists.get(playlistid);
        for (int idx = 0; idx < new_songs.size(); idx++) {
            playlist.add(new_songs.get(idx));
        }
        select_playlist(playlistid);
    }

    private void delete_selection_from_playlist() {
        int playlistid = this.playlist_panel.get_selected_playlist();
        int[] indices = this.playlist_song_panel.get_selection_indices();
        if (playlistid < 0 || indices == null || indices.length == 0) {
            return;
        }
        Playlist playlist = this.playlists.get(playlistid);
        for (int idx = indices.length - 1; idx >= 0; idx--) {
            playlist.remove_song_by_idx(indices[idx]);
        }
        select_playlist(playlistid);
    }

    private void new_random_playlist() {
        IntList song_list = this.check_database.get_checked_songids();
        int num_songs = song_list.size();
        if (num_songs == 0) {
            return;
        }
        Random random = new Random();
        for (int idx = 0; idx < num_songs; idx++) {
            int other_idx = idx + random.nextInt(num_songs - idx);
            song_list.swap(idx, other_idx);
        }
        song_list.truncate(RANDOM_PLAYLIST_SIZE);
        this.num_random_playlists++;
        String name = RANDOM_PLAYLIST_PREFIX + this.num_random_playlists;
        this.playlist_panel.new_playlist(name, song_list);
    }
}
