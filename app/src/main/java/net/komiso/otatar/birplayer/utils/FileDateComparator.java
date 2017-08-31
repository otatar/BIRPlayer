package net.komiso.otatar.birplayer.utils;

import java.io.File;
import java.util.Comparator;

/**
 * Created by o.tatar on 28-Aug-17.
 */
public class FileDateComparator implements Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {

        return (int) (rhs.lastModified() - lhs.lastModified());
    }
}
