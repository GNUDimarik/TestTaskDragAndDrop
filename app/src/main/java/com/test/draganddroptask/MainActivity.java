package com.test.draganddroptask;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        // Setup D&D feature and RecyclerView
        RecyclerViewDragDropManager dragMgr = new RecyclerViewDragDropManager();
        dragMgr.setInitiateOnMove(false);
        dragMgr.setInitiateOnLongPress(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Cursor cursor = getContentResolver().query(Constants.DATA_CONTENT_URI, null, null,
                null, null);
        startManagingCursor(cursor);
        recyclerView.setAdapter(dragMgr.createWrappedAdapter(new MyAdapter(this, cursor)));
        dragMgr.attachRecyclerView(recyclerView);
    }

    static class MyViewHolder extends AbstractDraggableItemViewHolder {
        TextView textView;

        public MyViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    static class MyAdapter extends RecyclerView.Adapter<MyViewHolder>
            implements DraggableItemAdapter<MyViewHolder> {
        private static final String TAG = "MyAdapter";
        private Context mContext;
        private Cursor mCursor;
        private int mRowIdColumn;
        private int mTitleColumn;
        private int mNextColumn;
        private int mPrevColumn;
        private ArrayList<Item> mList;

        private class Item {
            public long id = 0;
            public String title = "";
            public long next = 0;
            public long prev = 0;

            public Item(Cursor c) {
                id = c.getLong(mRowIdColumn);
                title = c.getString(mTitleColumn);
                next = c.getLong(mNextColumn);
                prev = c.getLong(mPrevColumn);
            }
        }

        ;

        public MyAdapter(Context context, Cursor cursor) {
            mList = new ArrayList<>();
            setHasStableIds(true); // this is required for D&D feature.
            mContext = context;
            mCursor = cursor;

            if (mCursor != null) {
                mRowIdColumn = mCursor.getColumnIndex(Constants.DATA_ID);
                mTitleColumn = mCursor.getColumnIndex(Constants.DATA_TITLE);
                mNextColumn = mCursor.getColumnIndex(Constants.DATA_NEXT);
                mPrevColumn = mCursor.getColumnIndex(Constants.DATA_PREV);
                fillFromCursor();
            }
        }

        @Override
        public long getItemId(int position) {
            if (!mList.isEmpty() && position < mList.size()) {
                return mList.get(position).id;
            }

            return 0;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_minimal,
                    parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            if (!mList.isEmpty() && position < mList.size()) {
                holder.textView.setText(mList.get(position).title);
            }
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        @Override
        public void onMoveItem(int fromPosition, int toPosition) {
            String dbg = cursorToString(mCursor);
            Log.d(TAG, dbg);

            if (mCursor.moveToPosition(fromPosition)) {
                Item fromItem = mList.get(fromPosition);
                ArrayList ops = new ArrayList();
                Uri uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, fromItem.prev);
                Cursor cursor = mContext.getContentResolver().query(uri, null, null,
                        null, null);

                if (cursor.moveToNext()) {
                    try {
                        ContentValues cv = new ContentValues();
                        cv.put(Constants.DATA_NEXT, fromItem.next);
                        uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, fromItem.prev);
                        ops.add(ContentProviderOperation.newUpdate(uri).withValues(cv).build());
                    } finally {
                        cursor.close();
                    }
                }

                uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, fromItem.next);
                cursor = mContext.getContentResolver().query(uri, null, null,
                        null, null);

                if (cursor.moveToNext()) {
                    try {
                        ContentValues cv = new ContentValues();
                        cv.put(Constants.DATA_PREV, fromItem.prev);
                        uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, fromItem.next);
                        ops.add(ContentProviderOperation.newUpdate(uri).withValues(cv).build());
                    } finally {
                        cursor.close();
                    }
                }

                Item toItem = mList.get(toPosition);

                uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, toItem.id);
                cursor = mContext.getContentResolver().query(uri, null, null,
                        null, null);

                if (cursor.moveToNext()) {
                    try {
                        ContentValues cv = new ContentValues();

                        if (fromPosition < toPosition) {
                            cv.put(Constants.DATA_NEXT, fromItem.id);
                        } else {
                            cv.put(Constants.DATA_PREV, fromItem.id);
                        }

                        uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, toItem.id);
                        ops.add(ContentProviderOperation.newUpdate(uri).withValues(cv).build());
                    } finally {
                        cursor.close();
                    }
                }

                ContentValues cv = new ContentValues();

                if (fromPosition < toPosition) {
                    cv.put(Constants.DATA_PREV, toItem.id);
                    cv.put(Constants.DATA_NEXT, toItem.next);
                } else {
                    cv.put(Constants.DATA_PREV, toItem.prev);
                    cv.put(Constants.DATA_NEXT, toItem.id);
                }

                uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, fromItem.id);
                ops.add(ContentProviderOperation.newUpdate(uri).withValues(cv).build());

                if (fromPosition > toPosition) {
                    uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, toItem.prev);
                    cursor = mContext.getContentResolver().query(uri, null, null,
                            null, null);

                    if (cursor.moveToNext()) {
                        try {
                            cv = new ContentValues();
                            cv.put(Constants.DATA_NEXT, fromItem.id);
                            uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, toItem.prev);
                            ops.add(ContentProviderOperation.newUpdate(uri).withValues(cv).build());

                        } finally {
                            cursor.close();
                        }
                    }
                } else if (fromPosition < toPosition) {
                    uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, toItem.next);
                    cursor = mContext.getContentResolver().query(uri, null, null,
                            null, null);

                    if (cursor.moveToNext()) {
                        try {
                            cv = new ContentValues();
                            cv.put(Constants.DATA_PREV, fromItem.id);
                            uri = ContentUris.withAppendedId(Constants.DATA_CONTENT_URI, toItem.next);
                            ops.add(ContentProviderOperation.newUpdate(uri).withValues(cv).build());
                        } finally {
                            cursor.close();
                        }
                    }
                }

                try {
                    mContext.getContentResolver().applyBatch(Constants.AUTHORITY, ops);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                notifyItemMoved(fromPosition, toPosition);
                mCursor.requery();
                dbg = cursorToString(mCursor);
                fillFromCursor();
                Log.d(TAG, dbg);
            } else {
                Log.d(TAG, "Could not to move cursor");
            }
        }

        @Override
        public boolean onCheckCanStartDrag(MyViewHolder holder, int position, int x, int y) {
            return true;
        }

        @Override
        public ItemDraggableRange onGetItemDraggableRange(MyViewHolder holder, int position) {
            return null;
        }

        @Override
        public boolean onCheckCanDrop(int draggingPosition, int dropPosition) {
            return true;
        }

        private void fillFromCursor() {
            mList.clear();
            long currentId = 0;
            int number = mCursor.getCount();
            Map<Long, Integer> map = new HashMap<>();

            for (int i = 0; i < number; ++i) {
                mCursor.moveToPosition(i);
                Item item = new Item(mCursor);

                if (item.prev == 0) {
                    currentId = item.id;
                    Log.d(TAG, "currentId " + currentId);
                    mList.add(item);
                } else {
                    Log.d(TAG, "put " + item.prev + " with index " + i);
                    map.put(item.prev, i);
                }
            }

            while (mList.size() < number) {
                Integer index = map.get(currentId);
                Log.d(TAG, "index " + index);

                if (index != null) {
                    mCursor.moveToPosition(index);
                    Item nextItem = new Item(mCursor);
                    mList.add(nextItem);
                    currentId = nextItem.id;
                }
            }
        }

        private String cursorToString(Cursor cursor) {
            String cursorString = "";
            if (cursor.moveToFirst()) {
                String[] columnNames = cursor.getColumnNames();
                for (String name : columnNames)
                    cursorString += String.format("%s ][ ", name);
                cursorString += "\n";
                do {
                    for (String name : columnNames) {
                        cursorString += String.format("%s ][ ",
                                cursor.getString(cursor.getColumnIndex(name)));
                    }
                    cursorString += "\n";
                } while (cursor.moveToNext());
            }
            return cursorString;
        }
    }
}
