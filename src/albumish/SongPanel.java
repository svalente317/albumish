/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class SongPanel implements SelectionListener {

    private final Jukebox player;
    private final Table table;
    private boolean show_bitrate;
    private boolean show_bpm;
    private int playlistid;

    public SongPanel(Jukebox player, Composite parent, boolean with_checks) {
        this.playlistid = -1;
        this.player = player;
        int style = (with_checks ? SWT.CHECK : 0);
        this.table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | style);
        TableColumn column;
        new TableColumn(this.table, SWT.RIGHT);
        column = new TableColumn(this.table, SWT.LEFT);
        column.setText("Title");
        column = new TableColumn(this.table, SWT.RIGHT);
        column.setText("Time");
        column = new TableColumn(this.table, SWT.LEFT);
        column.setText("Artist");
        column = new TableColumn(this.table, SWT.LEFT);
        column.setText("Album");
        column = new TableColumn(this.table, SWT.LEFT);
        column.setText("Year");
        this.show_bitrate = this.player.config.flag("bitrate");
        this.show_bpm = this.player.config.flag("bpm");
        if (this.show_bitrate) {
            column = new TableColumn(this.table, SWT.LEFT);
            column.setText("bitrate");
        }
        if (this.show_bpm) {
            column = new TableColumn(this.table, SWT.LEFT);
            column.setText("BPM");
        }
        this.table.setHeaderVisible(true);
        this.table.setLinesVisible(true);
        for (TableColumn pcolumn : this.table.getColumns()) {
            pcolumn.pack();
        }
        this.table.addSelectionListener(this);
    }

    public Control getControl() {
        return this.table;
    }

    public void reset(IntList songs, int selected_idx) {
        this.table.setRedraw(false);
        Database database = this.player.database;
        CheckDatabase check_database = this.player.check_database;
        int count = this.table.getItemCount();
        int num_songs = (songs == null ? 0 : songs.size());
        for (int idx = 0; idx < num_songs; idx++) {
            TableItem item;
            if (idx < count) {
                item = this.table.getItem(idx);
            } else {
                item = new TableItem(this.table, 0);
            }
            Song song = database.song_list.get(songs.get(idx));
            item.setData(song);
            item.setChecked(check_database.get(song.id));
            String duration = null;
            if (song.duration > 0) {
                int min = song.duration / 60;
                int sec = song.duration % 60;
                duration = min + ":" + (sec < 10 ? "0" : "") + sec;
            }
            String[] row = new String[]{
                    song.track_number > 0 ? Integer.toString(song.track_number) : null,
                    song.title, duration,
                    song.artistid > 0 ? database.artist_list.get(song.artistid).name : null,
                    song.albumid > 0 ? database.album_list.get(song.albumid).name : null,
                    song.year > 0 ? Integer.toString(song.year) : null
            };
            if (this.show_bitrate) {
                if (this.show_bpm) {
                    row = Utils.addAll(row, song.bitrate, song.bpm);
                } else {
                    row = Utils.addAll(row, song.bitrate);
                }
            } else if (this.show_bpm) {
                row = Utils.addAll(row, song.bpm);
            }
            for (int cnum = 0; cnum < row.length; cnum++) {
                item.setText(cnum, row[cnum] == null ? "" : row[cnum]);
            }
        }
        while (count > num_songs) {
            this.table.remove(count - 1);
            count--;
        }
        for (TableColumn column : this.table.getColumns()) {
            column.pack();
        }
        for (TableColumn column : this.table.getColumns()) {
            int width = column.getWidth();
            column.setWidth(width+5);
        }
        this.table.deselectAll();
        if (selected_idx >= 0 && selected_idx < num_songs) {
            this.table.select(selected_idx);
        }
        this.table.setRedraw(true);
        this.table.redraw();
    }

    public void update_database() {
        CheckDatabase check_database = this.player.check_database;
        for (TableItem item : this.table.getItems()) {
            int songid = ((Song) (item.getData())).id;
            boolean is_checked = item.getChecked();
            if (is_checked != check_database.get(songid)) {
                check_database.set(songid, is_checked);
            }
        }
    }

    public void set_playlistid(int playlistid) {
        this.playlistid = playlistid;
    }

    public int get_playlistid() {
        return this.playlistid;
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
        int active_idx = this.table.getSelectionIndex();
        if (active_idx < 0) {
            return;
        }
        if (this.playlistid >= 0) {
            this.player.play_song(this.playlistid, active_idx);
            return;
        }
        IntList song_list = new IntList();
        int song_idx = -1;
        int count = this.table.getItemCount();
        for (int idx = 0; idx < count; idx++) {
            if (idx == active_idx) {
                song_idx = song_list.size();
            }
            TableItem item = this.table.getItem(idx);
            int songid = ((Song) (item.getData())).id;
            boolean is_checked = item.getChecked();
            if (is_checked || idx == active_idx) {
                song_list.add(songid);
            }
        }
        int listid = this.player.set_auto_playlist(song_list);
        this.player.play_song(listid, song_idx);
    }

    public IntList get_selection() {
        IntList selection = new IntList();
        TableItem[] items = this.table.getSelection();
        if (items != null) {
            for (TableItem item : items) {
                int songid = ((Song) (item.getData())).id;
                selection.add(songid);
            }
        }
        return selection;
    }

    public int[] get_selection_indices() {
        return this.table.getSelectionIndices();
    }

    public int get_selected_idx() {
        return this.table.getSelectionIndex();
    }

    public void select_nth_row(int idx) {
        this.table.setSelection(idx);
    }
}
