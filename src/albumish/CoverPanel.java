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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;

public class CoverPanel extends Canvas implements
        PaintListener, ControlListener, SelectionListener, MouseListener, MouseMoveListener {

    private enum Cmd {
        ADD_ALBUM_TO_PLAYLIST,
        LOAD_ALBUM_ART,
        EDIT_ALBUM_TAGS,
        DELETE_ALBUM_FROM_LIBRARY
    }

    private static final int text_spacing = 12;
    private static final int select_box_thickness = 5;

    private final Jukebox player;
    private final AlbumSorter sorter;
    private int border_width;
    private int image_width;
    private int selected_width;
    private int selected_idx;
    private int[] album_list;
    private int panel_width;
    private int mouse_down_idx;
    private int anchor;

    public CoverPanel(Jukebox player, Composite parent) {
        super(parent, SWT.H_SCROLL | SWT.DOUBLE_BUFFERED);

        this.player = player;
        this.sorter = new AlbumSorter(player.database);
        get_sizes_from_gallery();
        this.album_list = new int[0];
        addPaintListener(this);
        addControlListener(this);
        addMouseListener(this);
        addMouseMoveListener(this);
        getHorizontalBar().addSelectionListener(this);
        this.mouse_down_idx = -1;
    }

    private void get_sizes_from_gallery() {
        int[] sizes = new int[2];
        this.player.gallery.get_sizes(sizes);
        this.image_width = sizes[0];
        this.border_width = this.image_width / 10;
        this.selected_width = sizes[1];
    }

    public int get_height() {
        return 2 * this.border_width + this.selected_width;
    }

    public int get_selected_albumid() {
        if (this.selected_idx >= 0 && this.selected_idx < this.album_list.length) {
            return this.album_list[this.selected_idx];
        }
        return 0;
    }

    public void reset(int[] new_list) {
        int albumid = get_selected_albumid();
        this.album_list = new_list;
        do_sort_and_reset(albumid);
    }

    public void sort(AlbumSorter.SortType sort_type) {
        int albumid = get_selected_albumid();
        this.sorter.set_sort_type(sort_type);
        do_sort_and_reset(albumid);
    }

    private void do_sort_and_reset(int albumid) {
        this.selected_idx = 0;
        this.sorter.sort(this.album_list);

        this.panel_width = (this.border_width + this.image_width) *
                this.album_list.length + this.border_width +
                (this.selected_width - this.image_width);

        this.selected_idx = 0;
        for (int idx = 0; idx < this.album_list.length; idx++) {
            if (albumid == this.album_list[idx]) {
                this.selected_idx = idx;
                break;
            }
        }
        this.player.select_album(get_selected_albumid());
        controlResized(null);
        redraw();
    }

    public boolean select_next_album(int delta) {
        int old_idx = this.selected_idx;
        int new_idx = this.selected_idx + delta;
        if (new_idx < 0 || new_idx >= this.album_list.length) {
            return false;
        }
        this.selected_idx = new_idx;
        if (!center_selected_album()) {
            redraw_albums(old_idx, new_idx);
        }
        this.player.select_album(this.album_list[this.selected_idx]);
        return true;
    }

    @Override
    public void controlMoved(ControlEvent event) {
    }

    @Override
    public void controlResized(ControlEvent event) {
        Rectangle bounds = getClientArea();
        ScrollBar scrollbar = getHorizontalBar();
        scrollbar.setMinimum(0);
        scrollbar.setMaximum(this.panel_width);
        scrollbar.setIncrement(this.image_width);
        scrollbar.setPageIncrement(bounds.width);
        scrollbar.setThumb(bounds.width);
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        if (event.widget instanceof ScrollBar) {
            // The scrollbar was moved.
            redraw();
            return;
        }
        Cmd command = (Cmd) event.widget.getData();
        if (command == null) {
            return;
        }
        if (this.mouse_down_idx < 0) {
            return;
        }
        int albumid = this.album_list[this.mouse_down_idx];
        switch (command) {
        case ADD_ALBUM_TO_PLAYLIST:
            break;
        case LOAD_ALBUM_ART:
            select_nth_album(this.mouse_down_idx);
            this.player.on_load_album_art();
            this.mouse_down_idx = 0;
            break;
        case EDIT_ALBUM_TAGS:
            new EditTagsDialog(this.player, albumid);
            break;
        case DELETE_ALBUM_FROM_LIBRARY:
            new DeleteAlbumDialog(this.player, albumid);
            break;
        }
    }

    @Override
    public void paintControl(PaintEvent event) {
        int base = getHorizontalBar().getSelection();
        int xpos = 0;
        int default_ypos = this.border_width +
                (this.selected_width - this.image_width) / 2;
        for (int idx = 0; idx < this.album_list.length; idx++) {
            xpos += this.border_width;
            if (xpos >= base + event.x + event.width) {
                break;
            }
            int width = this.image_width;
            int ypos = default_ypos;
            boolean is_selected = (idx == this.selected_idx);
            if (is_selected) {
                width = this.selected_width;
                ypos = this.border_width;
            }
            if (xpos + width > base + event.x) {
                int albumid = this.album_list[idx];
                Image image = this.player.gallery.get(albumid, is_selected);
                if (image != null) {
                    Rectangle bounds = image.getBounds();
                    event.gc.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height,
                            xpos - base, ypos, bounds.width, bounds.height);
                } else {
                    event.gc.drawRectangle(xpos - base, ypos, width - 1, width - 1);
                }
                if (idx == this.mouse_down_idx) {
                    GC gc = event.gc;
                    gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
                    gc.drawRectangle(xpos - base, ypos, width - 1, width - 1);
                    int np = select_box_thickness;
                    int minus = 1 + np + np;
                    gc.drawRectangle(xpos - base + np, ypos + np, width - minus, width - minus);
                    gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    for (int n = 1; n < select_box_thickness; n++) {
                        minus = 1 + n + n;
                        gc.drawRectangle(xpos - base + n, ypos + n, width - minus, width - minus);
                    }
                }
                if (!is_selected) {
                    Album album = this.player.database.album_list.get(albumid);
                    Artist artist = this.player.database.artist_list.get(album.artistid);
                    if (artist.name != null) {
                        ypos = default_ypos - text_spacing;
                        paint_line(event.gc, artist.name, xpos - base, ypos, width, true);
                    }
                    if (album.name != null) {
                        ypos = default_ypos + width + text_spacing;
                        paint_line(event.gc, album.name, xpos - base, ypos, width, false);
                    }
                }
            }
            xpos += width;
        }
    }

    private void redraw_albums(int lo, int hi) {
        if (lo > hi) {
            int tmp = lo;
            lo = hi;
            hi = tmp;
        }
        int base = getHorizontalBar().getSelection();
        int xpos = this.border_width + lo *
                (this.border_width + this.image_width);
        if (lo > this.selected_idx) {
            xpos += (this.selected_width - this.image_width);
        }
        int width = (hi - lo) * (this.border_width + this.image_width);
        if (lo <= this.selected_idx && this.selected_idx <= hi) {
            width += this.selected_width;
        } else {
            width += this.image_width;
        }
        redraw(xpos - base, this.border_width, width, this.selected_width, false);
    }

    /**
     * @return true if this issues a redraw request
     */
    private boolean center_selected_album() {
        // Get center of selected album.
        int center = this.selected_idx * (this.border_width + this.image_width) +
                this.border_width + (this.selected_width / 2);
        Point size = getSize();
        int xpos = center - (size.x / 2);
        int base = getHorizontalBar().getSelection();
        getHorizontalBar().setSelection(xpos);
        if (base == getHorizontalBar().getSelection()) {
            return false;
        }
        redraw();
        return true;
    }

    public void invalidate_album(int albumid) {
        int size = this.album_list.length;
        int idx;
        for (idx = 0; idx < size; idx++) {
            if (albumid == this.album_list[idx]) {
                redraw_albums(idx, idx);
                return;
            }
        }
    }

    @Override
    public void mouseDown(MouseEvent event) {
        this.anchor = getHorizontalBar().getSelection() + event.x;
        int idx = get_clicked_idx(event);
        if (idx < 0) {
            return;
        }
        this.mouse_down_idx = idx;
        if (event.button == 3) {
            popup_menu(event);
            return;
        }
        redraw_albums(idx, idx);
    }

    @Override
    public void mouseUp(MouseEvent event) {
        this.anchor = 0;
        int selected = this.mouse_down_idx;
        if (selected < 0) {
            return;
        }
        this.mouse_down_idx = -1;
        int idx = get_clicked_idx(event);
        if (idx == selected && this.selected_idx != selected) {
            select_nth_album(idx);
        } else {
            redraw_albums(selected, selected);
        }
    }

    @Override
    public void mouseDoubleClick(MouseEvent event) {
        int albumid = get_selected_albumid();
        if (albumid < 0) {
            return;
        }
        // TODO update checked database
        Album album = this.player.database.album_list.get(albumid);
        IntList song_list = new IntList();
        for (int songid : album.song_list) {
            if (this.player.check_database.get(songid)) {
                song_list.add(songid);
            }
        }
        int listid = this.player.set_auto_playlist(song_list);
        this.player.play_song(listid, 0);
    }

    private int get_clicked_idx(MouseEvent event) {
        int base = getHorizontalBar().getSelection();
        int xpos = base + event.x;
        int size = this.album_list.length;
        for (int idx = 0; idx < size; idx++) {
            if (xpos < this.border_width) {
                break;
            }
            xpos -= this.border_width;
            int width = (idx == this.selected_idx ?
                    this.selected_width : this.image_width);
            if (xpos >= width) {
                xpos -= width;
                continue;
            }
            // Found the x coordinate.
            int top = this.border_width;
            int bottom = top + this.selected_width;
            if (idx != this.selected_idx) {
                top += (this.selected_width - this.image_width) / 2;
                bottom = top + this.image_width;
            }
            if (event.y < top || event.y >= bottom) {
                break;
            }
            return idx;
        }
        return -1;
    }

    private void select_nth_album(int idx) {
        if (this.selected_idx == idx) {
            return;
        }
        int lo = idx, hi = idx;
        if (this.selected_idx < idx) {
            lo = this.selected_idx;
        } else {
            hi = this.selected_idx;
        }
        this.selected_idx = idx;
        redraw_albums(lo, hi);
        this.player.select_album(this.album_list[this.selected_idx]);
    }

    public void select_album(int albumid) {
        for (int idx = 0; idx < this.album_list.length; idx++) {
            if (albumid == this.album_list[idx]) {
                select_nth_album(idx);
                center_selected_album();
                return;
            }
        }
    }

    private void popup_menu(MouseEvent event) {
        Menu popupMenu = new Menu(this);
        MenuItem item;
        item = new MenuItem(popupMenu, SWT.NONE);
        item.setText("Add Album to Playlist");
        item.setData(Cmd.ADD_ALBUM_TO_PLAYLIST);
        item.addSelectionListener(this);
        item = new MenuItem(popupMenu, SWT.NONE);
        item.setText("Load Album Art...");
        item.setData(Cmd.LOAD_ALBUM_ART);
        item.addSelectionListener(this);
        item = new MenuItem(popupMenu, SWT.NONE);
        item.setText("Edit Album Tags...");
        item.setData(Cmd.EDIT_ALBUM_TAGS);
        item.addSelectionListener(this);
        item = new MenuItem(popupMenu, SWT.NONE);
        item.setText("Delete Album From Library...");
        item.setData(Cmd.DELETE_ALBUM_FROM_LIBRARY);
        item.addSelectionListener(this);
        popupMenu.setVisible(true);
    }

    /**
     * Paint a single line of text centered in a box.
     */
    private void paint_line(GC gc, String text, int xpos, int ypos, int width, boolean above) {
        Point extent = gc.stringExtent(text);
        if (extent.x > width) {
            int length = text.length();
            while ((length > 0) && (extent.x > width)) {
                length--;
                text = text.substring(0, length) + "...";
                extent = gc.stringExtent(text);
            }
        }
        xpos += (width - extent.x) / 2;
        if (above) {
            ypos -= extent.y;
        }
        gc.drawString(text, xpos, ypos);
    }

    @Override
    public void mouseMove(MouseEvent event) {
        if (this.anchor <= 0) {
            return;
        }
        ScrollBar scrollbar = getHorizontalBar();
        int selection = this.anchor - event.x;
        if (selection < 0 || selection > scrollbar.getMaximum()) {
            return;
        }
        scrollbar.setSelection(selection);
        redraw();
    }
}
