package net.komiso.otatar.birplayer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.komiso.otatar.biplayer.R;

/**
 * Created by o.tatar on 08-Dec-16.
 */
public class CustomDrawerListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final String[] textItems;
    private final int[] imageId;

    public CustomDrawerListAdapter(Activity context,
                      String[] textItems, int[] imageId) {
        super(context, R.layout.drawer_list_item_view, textItems);
        this.context = context;
        this.textItems = textItems;
        this.imageId = imageId;

    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {

        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.drawer_list_item_view, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(R.id.header_list_item_text);

        ImageView imageView = (ImageView) rowView.findViewById(R.id.header_list_item_image);
        txtTitle.setText(textItems[position]);

        imageView.setImageResource(imageId[position]);
        return rowView;
    }
}