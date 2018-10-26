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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

public class SortPanel implements SelectionListener {

    // The order that the sort types appear in this list.
    // This is basically redundant with the definition of SortType.
    private static final AlbumSorter.SortType[] TYPE_LIST = {
            AlbumSorter.SortType.YEAR_NEWEST,
            AlbumSorter.SortType.YEAR_OLDEST,
            AlbumSorter.SortType.ALBUM,
            AlbumSorter.SortType.ARTIST
    };

    // The sort type descriptions, in the same order.
    private static final String[] TEXT_LIST = {
            "  Year - New First  ",
            "  Year - Old First  ",
            "  Album  ",
            "  Artist  "
    };

    private Jukebox player;
    private Group frame;
    private Combo sort_box;

    public SortPanel(Jukebox player, Composite parent) {
        this.player = player;

        FillLayout layout = new FillLayout();
        layout.marginHeight = layout.marginWidth = 12;

        this.frame = new Group(parent, SWT.SHADOW_IN);
        this.frame.setText("Sort Order");
        this.frame.setLayout(layout);

        this.sort_box = new Combo(this.frame, SWT.DROP_DOWN | SWT.READ_ONLY);
        this.sort_box.setItems(TEXT_LIST);
        this.sort_box.select(0);
        this.sort_box.addSelectionListener(this);
    }

    public Control getControl() {
        return this.frame;
    }

    public AlbumSorter.SortType get_selected_sort() {
        int idx = this.sort_box.getSelectionIndex();
        return TYPE_LIST[idx];
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        this.player.sort_albums(get_selected_sort());
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }
}
