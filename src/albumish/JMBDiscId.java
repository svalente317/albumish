/*
 * Copyright (C) 2012 Dietmar Steiner <jmusicbrainz [at] d-steiner.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
/**
 *
 * @author Dietmar Steiner <jmusicbrainz [at] d-steiner.com>
 */
package albumish;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 *
 * @author Dietmar Steiner <jmusicbrainz [at] d-steiner.com>
 */
public class JMBDiscId {

    private static LibDiscId libDiscId = null;
    private Pointer disc = null;

    /**
     * Loads the library only when it is not already loaded and allocates a
     * local disc object;
     *
     * @param path path to libdiscid library
     * @return <code>true</code> if successful else <code>false</code>
     */
    public synchronized boolean init(String path) throws Exception {
        if (libDiscId == null) {
            libDiscId = (LibDiscId) Native.load(path, LibDiscId.class);
        }
        if (libDiscId != null && this.disc == null) {
            this.disc = libDiscId.discid_new();
        }
        return libDiscId != null;
    }

    /**
     * Reads the disc and generates a MusicBrainz DiscId
     *
     * @param drive path to the drive with the audio CD
     * @return the MusicBrainz DiscId or <code>null</code> if unsuccessful
     */
    public String getDiscId(String drive) throws Exception {
        boolean success = libDiscId.discid_read(this.disc, drive);
        if (!success) {
            throw new Exception(libDiscId.discid_get_error_msg(this.disc).getString(0));
        }
        return libDiscId.discid_get_id(this.disc).getString(0);
    }

    /**
     * Reads the disc and generates a FreeDB DiscId
     *
     * @param drive path to the drive with the audio CD
     * @return the FreeDB DiscId or <code>null</code> if unsuccessful
     */
    public String getFreeDBId(String drive) throws Exception {
        boolean success = libDiscId.discid_read(this.disc, drive);
        if (!success) {
            throw new Exception(libDiscId.discid_get_error_msg(this.disc).getString(0));
        }
        return libDiscId.discid_get_freedb_id(this.disc).getString(0);
    }

    /**
     * Reads the drive and generates a MusicBrainz webservice url
     *
     * @param drive path to the drive with the audio CD
     * @return the MusicBrainz webservice
     */
    public String getWebServiceUrl(String drive) throws Exception {
        boolean success = libDiscId.discid_read(this.disc, drive);
        if (!success) {
            throw new Exception(libDiscId.discid_get_error_msg(this.disc).getString(0));
        }
        return libDiscId.discid_get_webservice_url(this.disc).getString(0);
    }

    @Override
    protected void finalize() {
        if (null != this.disc) {
            libDiscId.discid_free(this.disc);
            this.disc = null;
        }
    }
}

/**
 * Library function linking
 */
interface LibDiscId extends Library {

    Pointer discid_new();

    void discid_free(Pointer disc);

    boolean discid_read(Pointer disc, String drive);

    Pointer discid_get_id(Pointer disc);

    Pointer discid_get_freedb_id(Pointer disc);

    Pointer discid_get_webservice_url(Pointer disc);

    Pointer discid_get_default_device();

    Pointer discid_get_error_msg(Pointer disc);
}
