/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class PlaylistPanel implements SelectionListener, ControlListener {

    private Jukebox player;
    private PlaylistCollection collection;
    private Table table;

    public PlaylistPanel(Jukebox player, PlaylistCollection collection) {
        this.player = player;
        this.collection = collection;
        this.table = new Table(player.main_window, SWT.BORDER | SWT.SINGLE);
        new TableColumn(this.table, SWT.LEFT);
        for (Playlist playlist : collection) {
            TableItem item = new TableItem(this.table, 0);
            item.setText(playlist.name);
        }
        this.table.select(PlaylistCollection.AUTO_PLAYLIST);
        this.table.addSelectionListener(this);
        this.table.addControlListener(this);
    }

    public Control getControl() {
        return this.table;
    }

    public int set_auto_playlist(IntList song_list) {
        int playlistid = this.collection.set_auto_playlist(song_list);
        this.table.select(PlaylistCollection.AUTO_PLAYLIST);
        this.player.select_playlist(playlistid);
        return playlistid;
    }

    public void new_playlist(String name, IntList song_list) {
        Playlist playlist = this.collection.new_playlist();
        playlist.name = name;
        playlist.reset(song_list);
        TableItem item = new TableItem(this.table, 0);
        item.setText(playlist.name);
        this.table.select(playlist.id);
        this.player.select_playlist(playlist.id);
    }

    public int get_selected_playlist() {
        return this.table.getSelectionIndex();
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        int playlistid = get_selected_playlist();
        if (playlistid < 0) {
            return;
        }
        this.player.select_playlist(playlistid);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
    }

    @Override
    public void controlMoved(ControlEvent event) {
    }

    @Override
    public void controlResized(ControlEvent event) {
        Rectangle bounds = this.table.getClientArea();
        TableColumn column = this.table.getColumn(0);
        column.setWidth(bounds.width);
    }
}
