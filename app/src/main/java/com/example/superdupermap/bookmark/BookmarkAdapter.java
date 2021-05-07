package com.example.superdupermap.bookmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.superdupermap.R;
import com.example.superdupermap.database.Bookmark;

import java.lang.ref.WeakReference;
import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {

    private final List<Bookmark> localDataSet;
    private final WeakReference<BookmarkActivity> target;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final ImageView imageView;
        private final BookmarkAdapter bookmarkAdapter;

        public ViewHolder(View view, List<Bookmark> localDataSet, BookmarkAdapter bookmarkAdapter) {
            super(view);
            this.bookmarkAdapter = bookmarkAdapter;
            name = (TextView) view.findViewById(R.id.name);
            name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bookmarkAdapter.target.get().gotoBookmark(localDataSet.get(getLayoutPosition()));
                }
            });
            imageView = (ImageView) view.findViewById(R.id.delete);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("deleteing");
                    System.out.println(getLayoutPosition());
                    System.out.println(localDataSet.get(getLayoutPosition()).id);
                    System.out.println(localDataSet.get(getLayoutPosition()).name);
                    bookmarkAdapter.target.get().deleteBookmark(localDataSet.get(getAdapterPosition()));
                }
            });
        }

        public TextView getTextView() {
            return name;
        }

        public void updateFields(Bookmark data) {
            System.out.println("UpdateFields called");
            System.out.println(data.name);
            name.setText(data.name);
        }

    }


    public BookmarkAdapter(List<Bookmark> dataSet, BookmarkActivity bookmarkActivity) {
        target = new WeakReference<>(bookmarkActivity);
        localDataSet = dataSet;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.text_row_item, viewGroup, false);

        return new ViewHolder(view, localDataSet, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        System.out.println("onBindViewHolder called");
        System.out.println(position);
        viewHolder.updateFields(localDataSet.get(position));
        // viewHolder.getTextView().setText(localDataSet.get(position).name);
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }
}