package org.de.jmg.jmgphotouploader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore.Images;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ThumbnailFormat;
import com.dropbox.core.v2.files.ThumbnailSize;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.microsoft.live.LiveAuthClient;
import com.microsoft.live.LiveAuthException;
import com.microsoft.live.LiveAuthListener;
import com.microsoft.live.LiveConnectClient;
import com.microsoft.live.LiveConnectSession;
import com.microsoft.live.LiveDownloadOperation;
import com.microsoft.live.LiveDownloadOperationListener;
import com.microsoft.live.LiveOperationException;
import com.microsoft.live.LiveStatus;

import org.de.jmg.jmgphotouploader.Controls.ZoomExpandableListview;
import org.de.jmg.jmgphotouploader.ImgFolder.Type;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

//import android.runtime.*;
//import com.facebook.*;
//import com.facebook.android.Facebook;
//import com.sromku.simple.*;


public class PhotoFolderAdapter extends BaseExpandableListAdapter implements LiveAuthListener {
    public Activity context;
    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3);

    private LiveAuthClient auth;
    public LiveConnectClient client;
    public static int requestCode = 9997;
    public java.util.List<ImgFolder> rows;

    //private SynchronizationContext SyncContext;
    public PhotoFolderAdapter(Activity context, java.util.ArrayList<ImgFolder> List) {
        setThreadPolicy();
        this.context = context;
        this.rows = List;
        ((_MainActivity) context).lv.setOnScrollListener(onScrollListener);
        ((_MainActivity) context).lv.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int i) {
                ImgFolder Folder = rows.get(i);
                String Name = Folder.Name;
                boolean blnRemoved = false;
                if (Name.equalsIgnoreCase("/"))
                {
                 switch (Folder.type)
                    {
                        case Google:
                            Folder.Name = "Google Drive";
                            break;
                        case OneDriveAlbum: case OneDriveFolder:
                            Folder.Name = "One Drive";
                            break;
                        case Dropbox:
                            Folder.Name = "Dropbox";
                            break;
                    }
                }
                for (int ii = i + 1; ii< rows.size(); ii++)
                {
                    ImgFolder Folder2 = rows.get(ii);
                    if (!(Folder2.type == Folder.type
                            || (Folder2.type == Type.OneDriveAlbum && Folder.type == Type.OneDriveFolder)
                            || (Folder.type == Type.OneDriveAlbum && Folder2.type == Type.OneDriveFolder)
                    ))
                    {
                        break;
                    }
                    if(Folder2.Name.startsWith(Name))
                    {
                        rows.remove(ii);
                        ii--;
                        blnRemoved = true;
                    }
                    else
                    {
                        break;
                    }
                }

                if (blnRemoved) {
                    Folder.fetched = false;
                    Folder.items.clear();
                    PhotoFolderAdapter.this.notifyDataSetChanged();
                }

            }
        });
        //((_MainActivity)context).lv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        //SyncContext = (SynchronizationContext.Current != null) ? SynchronizationContext.Current : new SynchronizationContext();
    }

    // Indexes are used for IDs:

    @Override
    protected void finalize() throws Throwable {
        executor.shutdown();
        super.finalize();
    }

    ;

    @Override
    public boolean hasStableIds() {
        // TODO Auto-generated method stub
        return false;
    }
    //---------------------------------------------------------------------------------------
    // Group methods:

    @Override
    public long getGroupId(int groupPosition) {
        // The index of the group is used as its ID:
        return groupPosition;
    }

    // Return the number of produce ("vegetables", "fruitimport com.microsoft.live.LiveOperationException;s", "herbs") objects:
    @Override
    public int getGroupCount() {
        return rows.size();
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        // Recycle a previous view if provided:
        //lib.LastgroupPosition= groupPosition;
        View view = convertView;
        boolean blnNew = false;
        // If no recycled view, inflate a new view as a simple expandable list item 1:
        if (view == null) {
            Object tempVar = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LayoutInflater inflater = (LayoutInflater) ((tempVar instanceof LayoutInflater) ? tempVar : null);
            view = inflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
            blnNew = true;
        }

        // Grab the produce object ("vegetables", "fruits", etc.) at the group position:
        ImgFolder imgFolder = rows.get(groupPosition);

        // Get the built-in first text view and insert the group name ("Vegetables", "Fruits", etc.):
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        if (blnNew) {
            int size = (lib.getScreenSize(context).x < lib.getScreenSize(context).y ? lib.getScreenSize(context).x : lib.getScreenSize(context).y);
            int newSize = (size / 30);
            if (newSize > textView.getTextSize())
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
        }
        if (imgFolder.type == ImgFolder.Type.OneDriveAlbum) {
            textView.setTextColor(Color.CYAN);
            if (isExpanded == false && imgFolder.items != null && imgFolder.items.size() == 0 && imgFolder.Name == "/" && imgFolder.fetched == false) {
                imgFolder.fetched = false;
                imgFolder.Name = "One Drive";
            }
        } else if (imgFolder.type == ImgFolder.Type.OneDriveFolder) {
            textView.setTextColor(Color.GRAY);
        } else if (imgFolder.type == ImgFolder.Type.Google) {
            textView.setTextColor(Color.GREEN);
            if (isExpanded == false && imgFolder.items != null && imgFolder.items.size() == 0 && imgFolder.Name == "/" && imgFolder.fetched == false) {
                imgFolder.fetched = false;
                imgFolder.Name = "Google Drive";
            }
        } else if (imgFolder.type == Type.Dropbox) {
            textView.setTextColor(Color.parseColor("#ffa500"));
            if (isExpanded == false && imgFolder.items != null && imgFolder.items.size() == 0 && imgFolder.Name == "/" && imgFolder.fetched == false) {
                imgFolder.fetched = false;
                imgFolder.Name = "Dropbox";
            }
        } else {
            textView.setTextColor(Color.WHITE);
        }
        textView.setText(imgFolder.Name);

        //if (blnNew) textView.setTextSize(lib.convertFromDp(context.getApplicationContext(), textView.getTextSize()));
        return view;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    //---------------------------------------------------------------------------------------
    // Child methods:

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        // The index of the child is used as its ID:
        return childPosition;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        // Return the number of children (produce item objects) in the group (produce object):
        ImgFolder Folder = rows.get(groupPosition);
        GetFolderItems(Folder, groupPosition);
        return Folder.items.size();
    }

    private static class ItemParams {
        public ImgListItem item;
        public ImageView img;
        public View view;

        public ItemParams(ImgListItem item, ImageView img, View view) {
            this.item = item;
            this.img = img;
            this.view = view;
        }
    }

    private static class ItemParamsSet {
        public ImageView IView;
        public Bitmap img;
        public View view;

        public ItemParamsSet(ImageView IView, Bitmap img, View view) {
            this.IView = IView;
            this.img = img;
            this.view = view;
        }
    }

    public android.database.Cursor ServiceCursor;

    @Override
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        // Recycle a previous view if provided:
        lib.setgstatus("GetChildview Start");
        View view = convertView;
        if (rows.size()<=groupPosition) return view;
        ImgFolder Folder = rows.get(groupPosition);
        lib.LastChildPosition = childPosition;
        //lib.LastgroupPosition = groupPosition;
        lib.LastisLastChild = isLastChild;
        lib.LastgroupPosition = groupPosition;
        GetFolderItems(Folder, groupPosition);
        if(Folder.items.size()<=childPosition)return view;
        final ImgListItem item = Folder.items.get(childPosition);
        // If no recycled view, inflate a new view as a simple expandable list item 2:
        boolean isNewView = false;
        try {
            if (view == null) {
                lib.setgstatus("GetChildview Inflate");
                view = context.getLayoutInflater().inflate(R.layout.customview, null);
                isNewView = true;
            }
            lib.setgstatus("GetChildview ImageView");
            //if (isNewView) view.setMinimumHeight((int) lib.convertFromDp(context.getApplicationContext(), view.getHeight()));
            final ImageView Image = (ImageView) view.findViewById(R.id.Image);
            if (isNewView) {
                int width = lib.getScreenSize(context).x;
                if (lib.getScreenSize(context).y > width) width = lib.getScreenSize(context).y;
                ViewGroup.LayoutParams layoutParams = Image.getLayoutParams();
                int diff = layoutParams.width;
                diff = (width / 10) - diff;
                if (diff > 0) {
                    layoutParams.width = width / 10;
                    layoutParams.height = width / 10;
                    Image.setLayoutParams(layoutParams);
                    //LinearLayout Text = (LinearLayout) view.findViewById (R.id.Text);
                    //RelativeLayout.LayoutParams RL = (RelativeLayout.LayoutParams)Text.getLayoutParams();
                    //RL.setMargins(RL.leftMargin + diff, RL.topMargin,RL.rightMargin + diff, RL.bottomMargin);
                    //Text.setLayoutParams(RL);
                }
            }
            boolean ItemExists = false;
            if (view.getTag() != null) {
                ViewHolder holder = (ViewHolder) (view.getTag());
                ImgListItem ImgListItem = holder.item;
                if (ImgListItem.id != null && ImgListItem.id != "" && ImgListItem.id == item.id)
                    ItemExists = true;
            }

            if (!ItemExists) {
                Image.setImageBitmap(null);
                item.ThumbnailLoaded = false;
                view.setTag(new ViewHolder(item));
            }

            lib.setgstatus("GetChildview Thread");
            JMPPPApplication myApp = (JMPPPApplication) context.getApplication();

            context = (Activity) myApp.MainContext;
            boolean isOneDrive = false;
            boolean isGoogle = false;
            boolean isDropbox = false;
            if (item.type == Type.OneDriveAlbum || item.type == Type.OneDriveFolder) {
                lib.setClient(myApp.getConnectClient());
                LoadThumbnailOneDrive(item, Image);
                isOneDrive = true;
            } else if (item.type == Type.Google) {
                lib.setClientGoogle(myApp.getGoogleDriveClient());
                LoadThumbnailGoogle(item, Image);
                isGoogle = true;
            } else if (item.type == Type.Dropbox) {
                lib.setClientDropbox(myApp.getDropboxClient());
                LoadThumbnailDropbox(item, Image);
                isDropbox = true;
            } else {
                //BitmapWorkerAsyncTask Task = new BitmapWorkerAsyncTask(new ItemParams(item,Image,view), context);
                if (mIsScrolling) {
                } else {
                    Runnable worker = new getBitmapWorkerThread(new ItemParams(item, Image, view), context);
                    executor.submit(worker);
                    //Task.execute();
                }
            }

            Image.setClickable(false);
            Image.setFocusable(false);
            Image.setFocusableInTouchMode(false);
            Image.setLongClickable(false);
            if (isNewView) {
                Image.setMaxHeight((int) lib.convertFromDp(context.getApplicationContext(), Image.getHeight()));
                Image.setMaxWidth((int) lib.convertFromDp(context.getApplicationContext(), Image.getWidth()));
            }
            //Image.Click+= (object sender, EventArgs e) => Console.WriteLine("IMG clicked");
            //view.FindViewById<ImageView> (Android.Resource.Id.Icon).ScaleX = 5;
            //view.FindViewById<ImageView> (Android.Resource.Id.Icon).ScaleY = 5;
            lib.setgstatus("GetChildview Text1");


            TextView Text1 = (TextView) view.findViewById(R.id.Text1);
            Text1.setText(item.FileName);
            Text1.setClickable(false);
            Text1.setFocusable(false);
            Text1.setFocusableInTouchMode(false);
            Text1.setLongClickable(false);
            if (isNewView) {
                Text1.setTextSize(TypedValue.COMPLEX_UNIT_PX, lib.convertFromDp(context.getApplicationContext(), Text1.getTextSize()));
                int newSize = lib.getScreenSize(context).x / 70;
                if (lib.getScreenInches(context).x < 2.5) newSize *= 1.8;
                if (newSize < 12) newSize = 12;
                if (newSize > Text1.getTextSize())
                {
                    Text1.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
                }
            }//Text1.Click+= (object sender, EventArgs e) => Console.WriteLine("Text1 clicked");
            lib.setgstatus("GetChildview Text2");
            TextView Text2 = (TextView) view.findViewById(R.id.Text2);
            if (item.getsize() == null && item.getImg() != null) {
                Text2.setText((item.getsize() != null) ? item.getsize() : item.getImg().getWidth()
                        + "*" + item.getImg().getHeight());
            } else {
                Text2.setText(item.getsize());
            }
            Text2.setClickable(false);
            Text2.setFocusable(false);
            Text2.setFocusableInTouchMode(false);
            Text2.setLongClickable(false);
            if (isNewView)
            {
                Text2.setTextSize(TypedValue.COMPLEX_UNIT_PX, lib.convertFromDp(context.getApplicationContext(), Text2.getTextSize()));
                if (Text2.getTextSize() < 8) Text2.setTextSize(TypedValue.COMPLEX_UNIT_PX,8);
            }

            //Text2.Click += (object sender, EventArgs e) => Console.WriteLine("Text2 clicked");

            //if (item.Img != null) view.FindViewById<TextView> (Resource.Id.Text2).Text = (item.size != null) ? item.size : item.Img.Width + "*" + item.Img.Height;
            //view.FindViewById<TextView> (Android.Resource.Id.Text1).
            lib.setgstatus("GetChildview Tag");


            //view.ScaleX = 2;
            //view.ScaleY = 2;
            view.setClickable(false);
            view.setFocusable(false);
            view.setFocusableInTouchMode(false);
            view.setLongClickable(false);
            /*view.Click += (object sender, EventArgs e) => {
				System.Diagnostics.Debug.Print ("Click");
			};
			*/
            //view.Touch += new EventHandler<View.TouchEvent1Args> (touch);
            //RelativeLayout relLayout = (RelativeLayout)view.FindViewById (Resource.Id.ImageList);
            lib.setgstatus("GetChildview Start Combo");
            LinearLayout layout = (LinearLayout) view.findViewById(R.id.Text); // (context);
            //relLayout.AddView (layout);
            Cursor Cursor;
            lib.setgstatus("GetChildview Services");
            if (ServiceCursor == null) {
                do {
                    Cursor = lib.dbpp.query("Select * FROM Services");
                    ServiceCursor = Cursor;
                    if (Cursor.getCount() == 3) {
                        try {
                            lib.dbpp.DataBase.execSQL("ALTER TABLE Services ADD COLUMN 'package' VARCHAR");
                        } catch (Exception ex) {
                            System.out.print(ex.getMessage());
                        }
                        lib.dbpp.DataBase.execSQL("INSERT INTO Services ('Name','URL','package') VALUES('Pinterest','pinterest.com','com.pinterest')");
                    }
                } while (Cursor.getCount() < 4);
            } else {
                Cursor = ServiceCursor;
            }
            if (Cursor.getCount() > 0) {
                boolean first = true;
                lib.setgstatus("GetChildview enumerate Services");
                while ((first) ? (Cursor.moveToFirst()) : (Cursor.moveToNext())) {
                    first = false;
                    if (Cursor.getString(Cursor.getColumnIndex("visible")).equalsIgnoreCase("true")) {
                        if (item.Uri != null || ((item.type == Type.Google || item.type == Type.Dropbox) && item.id != null)) {
                            lib.setgstatus("GetChildview Select Files");
                            try {
                                android.database.Cursor CursorItem;
                                if (item.type == Type.OneDriveAlbum) {
                                    CursorItem = lib.dbpp.DataBase.query("Files", null, "FileName=?", new String[]{item.id}, null, null, null);
                                } else if (item.type == Type.Google) {
                                    CursorItem = lib.dbpp.DataBase.query("Files", null, "URI=?", new String[]{item.id}, null, null, null);
                                    //CursorItem = null;
                                } else if (item.type == Type.Dropbox) {
                                    CursorItem = lib.dbpp.DataBase.query("Files", null, "URI=?", new String[]{item.id}, null, null, null);
                                    //CursorItem = null;
                                } else {
                                    String uri = item.folder; //item.Uri.getPath();
                                    CursorItem = lib.dbpp.DataBase.query("Files", null, "URI=?", new String[]{uri}, null, null, null);
                                }
                                String Service = Cursor.getString(Cursor.getColumnIndex("Name"));
                                CheckBox cb;
                                if (isNewView) {
                                    lib.setgstatus("GetChildview Create CheckBox");
                                    cb = new CheckBox(context);
                                    cb.setId(Cursor.getInt(Cursor.getColumnIndex("_id")));
                                    cb.setText(Service);
                                    cb.setTextColor(Color.LTGRAY);
                                    //(cb.setScaleX(lib.convertFromDp(context.getApplicationContext(), 1.0f));
                                    //cb.setScaleY(lib.convertFromDp(context.getApplicationContext(), 1.0f));
                                    //cb.forceLayout();
                                    cb.setTextSize(TypedValue.COMPLEX_UNIT_PX, lib.convertFromDp(context.getApplicationContext(), 14.0f));
                                    //cb.SetPadding (1,1,1,1);//(5, 26, 5, 5);
                                    cb.setGravity(Gravity.CENTER_VERTICAL);
                                    float Weight = 0.49f / Cursor.getCount();
                                    int size = (int) ((lib.getScreenSize(context).x * 0.35f) / Cursor.getCount());
                                    LinearLayout.LayoutParams L = new LinearLayout.LayoutParams(size, ViewGroup.LayoutParams.WRAP_CONTENT, Weight);
                                    //noinspection WrongConstant
                                    L.gravity = cb.getGravity();
									/*
									if (cb.getScaleX() != 1.0f){
										L.setMargins(0,20,20,20);
										L.setMargins((int)(L.leftMargin / cb.getScaleX()),(int)( L.topMargin / cb.getScaleY()),(int) (L.rightMargin / cb.getScaleX()), (int)(L.bottomMargin / cb.getScaleY()));
										//L.leftMargin = - Math.abs(L.leftMargin);
										L.rightMargin = - Math.abs(L.rightMargin);
										L.topMargin = - Math.abs(L.topMargin);
										L.bottomMargin = - Math.abs(L.bottomMargin);
									}
									*/
                                    lib.setgstatus("GetChildview AddCheckbox to Layout");
                                    //L.width = lib.getScreenSize(context).x / (2 * Cursor.getCount());
                                    //cb.findViewById(android.R.id.checkbox);
                                    layout.addView(cb, L);
                                    //cb.setWidth((int)(cb.getWidth() / cb.getScaleX()));
                                    //cb.setHeight((int)(cb.getHeight() / cb.getScaleY()));
                                    //Console.WriteLine ("CB: Width: " + cb.Width + " Height: " + cb.Height + " Gravity: " + cb.Gravity.ToString());
                                    //C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C#-style event wireups:
                                    cb.setOnCheckedChangeListener(onCheckedChangedListener);
                                    cb.setOnLongClickListener(onLongClickListener);
                                    cb.setChecked(false);
                                } else {
                                    lib.setgstatus("GetChildview Find Checkbox");
                                    cb = (CheckBox) view.findViewById(Cursor.getInt(Cursor.getColumnIndex("_id")));
                                    cb.setTag(null);
                                    cb.setChecked(false);
                                }
                                lib.setgstatus("GetChildview Set Checked");
                                if (CursorItem != null && CursorItem.moveToFirst()) {
                                    try {
                                        lib.setgstatus("GetChildview Query Uploads");
                                        try {
                                            String ServiceID = Cursor.getString(Cursor.getColumnIndex("_id"));
                                            String FileID = CursorItem.getString(CursorItem.getColumnIndex("_id"));
                                            android.database.Cursor CursorUploads = lib.dbpp.DataBase.query
                                                    ("Uploads", null, "ServiceID=? AND FileID=?",
                                                            (new String[]{ServiceID,
                                                                    FileID}), null, null, null);
                                            boolean Checked = (CursorUploads.getCount() > 0);
                                            cb.setChecked(Checked);
                                        } finally {

                                        }
                                    } catch (Exception e) {
                                    }
                                }
                                lib.setgstatus("GetChildview Set cb.Tag");
                                cb.setTag(new cbItemHolder(item, Cursor.getInt(Cursor.getColumnIndex("_id"))));
                            } finally {

                            }
                        }
                    }
                }
            }
			/*
			Text1.LayoutParameters.Width = 0;
			((LinearLayout.LayoutParams)Text1.LayoutParameters).Weight= 0.3f;
			Text2.LayoutParameters.Width = 0;
			((LinearLayout.LayoutParams)Text2.LayoutParameters).Weight= 0.2f;
			*/
            lib.setgstatus("GetChildview Finished");
            return view;
        } catch (RuntimeException ex) {
            lib.ShowException(context, ex);
            return view;
        } catch (Exception e) {
            lib.ShowException(context, e);
            return view;
        }
    }

    //List<imgListViewLiveDownloadListener> list = new ArrayList<imgListViewLiveDownloadListener>();
    private void LoadThumbnailOneDrive(ImgListItem pItem, ImageView pImage) throws Exception {
        final ZoomExpandableListview lv = (ZoomExpandableListview) ((_MainActivity) context).lv;
		/*
        try
		{
			for (int i = 0; i < list.size(); i++)
			{
				imgListViewLiveDownloadListener ldl = list.get(i);
				if (!ItemExists(ldl.Image, ldl.item))
				{
					try
					{
						ldl.cancel();
						list.remove((ldl));
						i--;
						//lib.ShowToast(context, "Item " + ldl.item.FileName + " download cancelled!");
					}
					catch (Exception ex)
					{
						lib.ShowException(context, ex);
					}

				}
			}
		}
		catch (Exception ex)
		{
			lib.ShowException(context,ex);
		}

	    */

        String file = pItem.id + "/picture?type=thumbnail";
        if (pItem.ThumbnailLoaded) {
            return;
        }
        if (!ItemExists(pImage, pItem)) {
            return;
        }
        try {

            try {
				/*
				mProgress = new ProgressDialog(context);
ZoomExpandableListview lv = (ZoomExpandableListview) ((_MainActivity) context).lv;				mProgress.setTitle("Download");
				mProgress.setMessage("Downloading Image");
				mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mProgress.setMax(100);
				mProgress.show();
				*/
            } catch (Exception ex) {
                lib.ShowException(context, ex);
            }

            if (mIsScrolling || lv.getIsScaled() || !ItemExists(pImage, pItem)) {
        		/*
        		lib.ShowToast(context, "Item " + item.FileName
        				+ " can not be displayed!"
        				+ " IsScrolling " + mIsScrolling
        				+ " IsScaled " + lv.getIsScaled()
        				+ " ItemExists " + ItemExists(Image,item));
        		return;
        		*/
            } else {
                imgListViewLiveDownloadListener LDL;
                LDL = new imgListViewLiveDownloadListener(pImage, pItem) {

                    public void onDownloadCompleted(LiveDownloadOperation operation) {
                        //list.remove(this);
                        //File f = (File)operation.getUserState();
                        InputStream input = null;
                        if (mIsScrolling || lv.getIsScaled() || !ItemExists(Image, item)) {
			        		/*
			        		lib.ShowToast(context, "Download interrupted " + item.FileName
			        				+ " can not be displayed!"
			        				+ " IsScrolling " + mIsScrolling
			        				+ " IsScaled " + lv.getIsScaled()
			        				+ " ItemExists " + ItemExists(Image,item));
			        		operation.cancel();
			        		return;
			        		*/
                        }
                        try {
                            //resultTextView.setText("Picture downloaded.");
                            input = operation.getStream(); //new FileInputStream(f);
                            Bitmap bMap = null;
                            try {
                                int i = 0;
                                while (bMap == null) {
                                    i++;
                                    bMap = BitmapFactory.decodeStream(input);
                                    if (i > 0) break;
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                lib.ShowToast(context, context.getString(R.string.Couldnotload) + " " + item.FileName + context.getString(R.string.Error) + ex.getClass().getName() + " " + ex.getMessage());
                            }
                            if (bMap != null) {
                                if (ItemExists(Image, item)) {
                                    Image.setImageBitmap(bMap);
                                    item.ThumbnailLoaded = true;
                                } else {
                                    lib.ShowToast(context, getS(R.string.Item) + item.FileName + getS(R.string.isnomorevisible));
                                }
                                //SetImageViewBitmap(new ItemParamsSet(p.img, bMap));
                                //p.item.setsize(bMap.getWidth() + "*" + bMap.getHeight());
                            } else {
                                lib.ShowToast(context, getS(R.string.Couldnotload) + " " + item.FileName);
                                item.ThumbnailLoaded = true;
                            }
                            if (input != null) {
                                try {
                                    input.close();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception ex) {
                            //resultTextView.setText("Error downloading picture: " + ex.getMessage());
                            lib.ShowToast(context, context.getString(R.string.Couldnotload) + item.FileName + context.getString(R.string.Error) + ex.getClass().getName() + " " + ex.getMessage() + (lib.getCauses(ex)));
                        } finally {
                            //f.delete();
                        }
                    }


                    public void onDownloadFailed(LiveOperationException exception, LiveDownloadOperation operation) {
                        //resultTextView.setText(exception.getMessage());
                        //lib.ShowToast(context, "Failure! Could not load " + item.FileName);
                        lib.ShowToast(context, context.getString(R.string.Couldnotload) + item.FileName + context.getString(R.string.Error) + exception.getClass().getName() + " " + exception.getMessage() + (lib.getCauses(exception)));
                        //File f = (File)operation.getUserState();
                        //f.delete();
                        //mProgress.dismiss();
			        	/*
			        	if (exception.getClass() == LiveOperationException.class && exception.getMessage().contains("request_token_missing"))
			        	{
			        		Intent LoginLiveIntent = new Intent(context, LoginLiveActivity.class);
							LoginLiveIntent.putExtra("GroupPosition", 0);
							context.startActivity(LoginLiveIntent); //, LoginLiveActivity.requestCode);
							context.finish();
			        	}
			        	*/

                        //list.remove(this);
                    }

                    public void onDownloadProgress(int totalBytes, int bytesRemaining, LiveDownloadOperation operation) {
                        //resultTextView.setText("Downloading picture... " + bytesRemaining + " bytes downloaded " +
                        //  "(" + (bytesRemaining / totalBytes) * 100 + "%)");
                        int percentCompleted = (int) ((bytesRemaining / totalBytes) * 100);
                        //ZoomExpandableListview lv = (ZoomExpandableListview) ((_MainActivity) context).lv;
                        if (mIsScrolling || lv.getIsScaled() || !ItemExists(Image, item)) {
                            lib.ShowToast(context, getS(R.string.Operationcanceled) + item.FileName);
			        				/*+ " can not be displayed!"
			        				+ " IsScrolling " + mIsScrolling
			        				+ " IsScaled " + lv.getIsScaled()
			        				+ " ItemExists " + ItemExists(Image,item));*/
                            operation.cancel();
                            //list.remove(this);
                            //File f = (File)operation.getUserState();
                            //f.delete();
                        }

                        //mProgress.setProgress(percentCompleted);
                    }
                };
                //File tmpFile = File.createTempFile("Live",".tmp");
                LDL.operation = lib.getClient(context).downloadAsync(file, LDL);
                //list.add(LDL);

            }
        } catch (Exception ex) {
            lib.ShowException(context, ex);
        }
    }

    private void LoadThumbnailGoogle(final ImgListItem pItem, final ImageView pImage) throws Exception {
        final ZoomExpandableListview lv = (ZoomExpandableListview) ((_MainActivity) context).lv;


        if (pItem.ThumbnailLoaded) {
            return;
        }
        if (!ItemExists(pImage, pItem)) {
            return;
        }
        try {
            final Drive drive = lib.getClientGoogle(context);
            if (drive != null) {

                if (mIsScrolling || lv.getIsScaled() || !ItemExists(pImage, pItem)) {
        		/*
        		lib.ShowToast(context, "Item " + item.FileName
        				+ " can not be displayed!"
        				+ " IsScrolling " + mIsScrolling
        				+ " IsScaled " + lv.getIsScaled()
        				+ " ItemExists " + ItemExists(Image,item));
        		return;
        		*/
                } else {
                    //resultTextView.setText("Picture downloaded.");
                    LinearLayout.LayoutParams LP = (LinearLayout.LayoutParams) pImage.getLayoutParams();
                    final int width = LP.width;// pImage.getWidth();
                    final int height = LP.height; //pImage.getHeight();
                    AsyncTask<Void, Void, Bitmap> Task = new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            try {
                                String thumbnailLink = pItem.ThumbNailLink;
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                InputStream input = null;
                                if (thumbnailLink == null) {
                                    final com.google.api.services.drive.model.File file = drive.files().get(pItem.id)
                                            .setFields("thumbnailLink")
                                            .execute();
                                    thumbnailLink = file.getThumbnailLink();
                                }
                                if (thumbnailLink == null) {
                                    try {
                                        thumbnailLink = " https://drive.google.com/thumbnail?sz=w" + width + "-h" + height + "&id=" + pItem.id + "";
                                        input = new BufferedInputStream(new URL(thumbnailLink).openStream());
                                    } catch (IOException eex) {
                                        return null;
                                    }
                                } else {
                                    HttpRequest request = drive.getRequestFactory().buildGetRequest(new GenericUrl(thumbnailLink));
                                    Future<HttpResponse> response = request.executeAsync(executor);
                                    response.get(20, TimeUnit.SECONDS).download(byteArrayOutputStream);
                                    input = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                                }
                                Bitmap bMap = null;
                                try {
                                    int i = 0;
                                    while (bMap == null) {
                                        i++;
                                        bMap = BitmapFactory.decodeStream(input);
                                        if (i > 0) break;
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    lib.ShowToast(context, context.getString(R.string.Couldnotload) + pItem.FileName + context.getString(R.string.Error) + ex.getClass().getName() + " " + ex.getMessage());
                                }
                                if (bMap != null) {
                                    return bMap;
                                } else {
                                    lib.ShowToast(context, getS(R.string.Couldnotload) + pItem.FileName);
                                }
                                if (input != null) {
                                    try {
                                        input.close();
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            } catch (Exception ex) {
                                //resultTextView.setText("Error downloading picture: " + ex.getMessage());
                                lib.ShowToast(context, context.getString(R.string.Couldnotload) + pItem.FileName + context.getString(R.string.Error) + ex.getClass().getName() + " " + ex.getMessage() + (lib.getCauses(ex)));
                            } finally {
                                //f.delete();
                            }
                            return null;
                        }


                        @Override
                        protected void onPostExecute(Bitmap bMap) {

                            if (bMap != null) {
                                if (ItemExists(pImage, pItem)) {
                                    pImage.setImageBitmap(bMap);
                                    pItem.ThumbnailLoaded = true;
                                } else {
                                    lib.ShowToast(context, getS(R.string.Item) + pItem.FileName + getS(R.string.isnomorevisible));
                                }
                                //SetImageViewBitmap(new ItemParamsSet(p.img, bMap));
                                //p.item.setsize(bMap.getWidth() + "*" + bMap.getHeight());
                            } else {
                                lib.ShowToast(context, getS(R.string.Couldnotload) + pItem.FileName);
                                pItem.ThumbnailLoaded = true;
                            }
                        }
                    };
                    Task.executeOnExecutor(executor);
                }
            }
        } catch (Exception ex) {
            //resultTextView.setText("Error downloading picture: " + ex.getMessage());
            lib.ShowToast(context, context.getString(R.string.Couldnotload) + pItem.FileName + context.getString(R.string.Error) + ex.getClass().getName() + " " + ex.getMessage() + (lib.getCauses(ex)));
        }
    }

    private void LoadThumbnailDropbox(final ImgListItem pItem, final ImageView pImage) throws Exception {
        final ZoomExpandableListview lv = (ZoomExpandableListview) ((_MainActivity) context).lv;


        if (pItem.ThumbnailLoaded) {
            return;
        }
        if (!ItemExists(pImage, pItem)) {
            return;
        }
        try {
            final DbxClientV2 client = lib.getClientDropbox(context);
            if (client != null) {

                if (mIsScrolling || lv.getIsScaled() || !ItemExists(pImage, pItem)) {
        		/*
        		lib.ShowToast(context, "Item " + item.FileName
        				+ " can not be displayed!"
        				+ " IsScrolling " + mIsScrolling
        				+ " IsScaled " + lv.getIsScaled()
        				+ " ItemExists " + ItemExists(Image,item));
        		return;
        		*/
                } else {
                    //resultTextView.setText("Picture downloaded.");
                    LinearLayout.LayoutParams LP = (LinearLayout.LayoutParams) pImage.getLayoutParams();
                    final int width = LP.width;// pImage.getWidth();
                    final int height = LP.height; //pImage.getHeight();
                    AsyncTask<Void, Void, Bitmap> Task = new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... params) {
                            try {
                                ThumbnailSize ts = ThumbnailSize.W64H64;
                                if (width <= 64)
                                {

                                }
                                else if (width <= 128)
                                {
                                    ts = ThumbnailSize.W128H128;
                                }
                                else if (width <= 640)
                                {
                                    ts = ThumbnailSize.W640H480;
                                }
                                else
                                {
                                    ts = ThumbnailSize.W1024H768;
                                }
                                DbxDownloader<FileMetadata> downloader = client.files().getThumbnailBuilder(pItem.folder)
                                        .withFormat(ThumbnailFormat.JPEG)
                                        .withSize(ts)
                                        .start();
                                //ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                InputStream input = null;
                                if (downloader != null) {
                                    try {
                                        input = new BufferedInputStream(downloader.getInputStream());
                                    } catch (Exception eex) {
                                        return null;
                                    }
                                }
                                Bitmap bMap = null;
                                try {
                                    int i = 0;
                                    while (bMap == null) {
                                        i++;
                                        bMap = BitmapFactory.decodeStream(input);
                                        if (i > 0) break;
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    lib.ShowToast(context, context.getString(R.string.Couldnotload) + pItem.FileName + context.getString(R.string.Error) + ex.getClass().getName() + " " + ex.getMessage());
                                }
                                if (bMap != null) {
                                    return bMap;
                                } else {
                                    lib.ShowToast(context, getS(R.string.Couldnotload) + pItem.FileName);
                                }
                                if (input != null) {
                                    try {
                                        input.close();
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            } catch (Exception ex) {
                                //resultTextView.setText("Error downloading picture: " + ex.getMessage());
                                lib.ShowToast(context, context.getString(R.string.Couldnotload) + pItem.FileName + context.getString(R.string.Error) + ex.getClass().getName() + " " + ex.getMessage() + (lib.getCauses(ex)));
                            } finally {
                                //f.delete();
                            }
                            return null;
                        }


                        @Override
                        protected void onPostExecute(Bitmap bMap) {

                            if (bMap != null) {
                                if (ItemExists(pImage, pItem)) {
                                    pImage.setImageBitmap(bMap);
                                    pItem.ThumbnailLoaded = true;
                                } else {
                                    lib.ShowToast(context, getS(R.string.Item) + pItem.FileName + getS(R.string.isnomorevisible));
                                }
                                //SetImageViewBitmap(new ItemParamsSet(p.img, bMap));
                                //p.item.setsize(bMap.getWidth() + "*" + bMap.getHeight());
                            } else {
                                lib.ShowToast(context, getS(R.string.Couldnotload) + pItem.FileName);
                                pItem.ThumbnailLoaded = true;
                            }
                        }
                    };
                    Task.executeOnExecutor(executor);
                }
            }
        } catch (Exception ex) {
            //resultTextView.setText("Error downloading picture: " + ex.getMessage());
            lib.ShowToast(context, context.getString(R.string.Couldnotload) + pItem.FileName + context.getString(R.string.Error) + ex.getClass().getName() + " " + ex.getMessage() + (lib.getCauses(ex)));
        }
    }


    private boolean ItemExists(ImageView Image, ImgListItem item) {
        View view = (View) Image.getParent().getParent();
        Boolean ItemExists = false;
        if (view.getTag() != null) {
            ViewHolder holder = (ViewHolder) (view.getTag());
            ImgListItem ImgListItem = holder.item;
            if (ImgListItem.id != null && ImgListItem.id != "" && ImgListItem.id == item.id)
                ItemExists = true;
        }
        return ItemExists;
    }

    public String getS(int resid) {
        return context.getString(resid);
    }


    private void GetFolderItems(ImgFolder Folder, int GroupPosition) {
        if ((Folder.type == ImgFolder.Type.OneDriveAlbum
                || Folder.type == ImgFolder.Type.OneDriveFolder
                || Folder.type == ImgFolder.Type.Google
                || Folder.type == ImgFolder.Type.Dropbox)
                && (Folder.Name != "/") && (Folder.items.size() == 0)) {
            if (Folder.Name == "One Drive" || Folder.Name == "Google Drive" || Folder.Name == "Dropbox") {
                Folder.Name = "/";
            }
            if (Folder.items.size() == 0 && Folder.fetched == false) {
                lib.LastgroupPosition = GroupPosition;
                if (Folder.type == ImgFolder.Type.Google) {
                    if (lib.getClientGoogle(context) == null) {

                        Folder.Name = "Google Drive";
                        lib.BMList = new java.util.ArrayList<ImgListItem>();
                        ((_MainActivity) context).StartLoginGoogle(Folder);
                    } else {
                        try {
                            if (Folder.fetched == false) {
                                lib.BMList = new java.util.ArrayList<ImgListItem>();
                                Folder.items = lib.BMList;
                                lib.GetThumbnailsGoogle(context, Folder.Name, Folder, GroupPosition, ((_MainActivity) context).lv);
                                Folder.fetched = true;
                            }
                        } catch (LiveOperationException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Folder.items = lib.BMList;
                } else if (Folder.type == Type.Dropbox) {
                    if (lib.getClientDropbox(context) == null) {

                        Folder.Name = "Dropbox";
                        lib.BMList = new java.util.ArrayList<ImgListItem>();
                        ((_MainActivity) context).StartLoginDropbox(Folder);
                    } else {
                        try {
                            if (Folder.fetched == false) {
                                lib.BMList = new java.util.ArrayList<ImgListItem>();
                                Folder.items = lib.BMList;
                                lib.GetThumbnailsDropbox(context, Folder.Name, Folder, GroupPosition, ((_MainActivity) context).lv);
                                Folder.fetched = true;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Folder.items = lib.BMList;
                }else {

                    if (lib.getClient(context) == null) {
                        Folder.Name = "One Drive";
                        //CountDownLatch Latch = new CountDownLatch(1);
                        //lib.Latch= Latch;
                        lib.BMList = new java.util.ArrayList<ImgListItem>();
                        ((_MainActivity) context).StartLoginLive(Folder);
                        //context.finish();
                    } else {
                        try {
                            if (Folder.fetched == false) {
                                lib.BMList = new java.util.ArrayList<ImgListItem>();
                                Folder.items = lib.BMList;
                                lib.GetThumbnailsOneDrive(context, Folder.Name, Folder, GroupPosition, ((_MainActivity) context).lv);
                                Folder.fetched = true;
                            }
                        } catch (LiveOperationException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
					/* 
					CountDownLatch LatchClient = new CountDownLatch(1);
					lib.LatchClient = LatchClient;
					this.auth = new LiveAuthClient(context, "0000000048135143");
			        final Iterable<String> scopes = Arrays.asList("wl.signin", "wl.basic", "wl.skydrive");
			        auth.login(context, scopes, (LiveAuthListener) this);
			        try {
						LatchClient.await(15, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				*/
				/*
		         try {
					//lib.GetThumbnailsOneDrive(context, lib.BMList);
					Latch.await(30, TimeUnit.SECONDS);
		         } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
		         }
		         */
                    Folder.items = lib.BMList;
                }
            }
        }
    }


    public void onAuthComplete(LiveStatus status, LiveConnectSession session, Object userState) {
        if (status == LiveStatus.CONNECTED) {
            //lib.ShowMessage(this,"Signed in.");
            client = new LiveConnectClient(session);
            lib.setClient(client);
            //lib.LatchClient.countDown();
            //GetThumbnailsOneDriveAndSetLV(true);
            //this.setResult(1, this.getIntent());
            //this.finishActivity(requestCode);
            //this.finish();
        } else {
            lib.ShowMessage(context, getS(R.string.Notsignedin));
            client = null;
            //lib.LatchClient.countDown();
            //GetThumbnailsOneDriveAndSetLV(false);
            //this.setResult(0, getIntent());
            //this.finishActivity(requestCode);
        }
    }

    public void onAuthError(LiveAuthException exception, Object userState) {
        lib.ShowMessage(context, getS(R.string.Errorsigningin) + exception.getMessage());
        client = null;
        //lib.LatchClient.countDown();
        //GetThumbnailsOneDriveAndSetLV(false);
    }


    public class getBitmapWorkerThread implements Runnable {

        private String command;
        private ItemParams p;
        private Context Context;

        public getBitmapWorkerThread(ItemParams p, Context Context) {
            this.p = p;
            this.Context = Context;
        }

        @Override
        public void run() {
            Bitmap img = p.item.getImg();
            SetImageViewBitmap(new ItemParamsSet(p.img, img, p.view));
        }

        private synchronized void SetImageViewBitmap(ItemParamsSet p) {
            p.IView.setImageBitmap(p.img);
            //p.IView.invalidate();
            this.p.item.ThumbnailLoaded = true;
        }

        @Override
        public String toString() {
            return this.command;
        }
    }

    public class BitmapWorkerAsyncTask extends AsyncTask {

        private String command;
        private ItemParams p;
        private Context Context;

        public BitmapWorkerAsyncTask(ItemParams p, Context Context) {
            this.p = p;
            this.Context = Context;
        }

        @Override
        protected Object doInBackground(Object... arg0) {
            // TODO Auto-generated method stub
            Bitmap img = p.item.getImg();
            SetImageViewBitmap(new ItemParamsSet(p.img, img, p.view));
            return null;
        }

        private synchronized void SetImageViewBitmap(ItemParamsSet p) {
            p.IView.setImageBitmap(p.img);
            //p.IView.invalidate();
            p.view.invalidate();
        }

        @Override
        public String toString() {
            return this.command;
        }


    }

    private static class cbItemHolder extends java.lang.Object {
        public ImgListItem item;
        public int ServiceID;

        public cbItemHolder(ImgListItem item, int ServiceID) {
            this.item = item;
            this.ServiceID = ServiceID;
        }
    }

    public OnCheckedChangeListener onCheckedChangedListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            // TODO Auto-generated method stub
            lib.setgstatus("cb_checkedChanged Start");
            CheckBox cb = (CheckBox) buttonView;
            if (cb.getTag() == null) {
                return;
            }
            cbItemHolder holder = (cbItemHolder) cb.getTag();
            ImgListItem item = holder.item;
            android.database.Cursor CursorItem;
            do {
                lib.setgstatus("cb_checkedChanged Query Files");
                //CursorItem = lib.dbpp.DataBase.query("Files", null, "URI=?", new String[]{item.Uri.getPath()}, null, null, null);
                boolean isOneDrive = false;
                boolean isGoogle = false;
                boolean isDropbox = false;
                if (item.type == Type.OneDriveAlbum) {
                    CursorItem = lib.dbpp.DataBase.query("Files", null, "FileName=?", new String[]{item.id}, null, null, null);
                    isOneDrive = true;
                } else if (item.type == Type.Google) {
                    CursorItem = lib.dbpp.DataBase.query("Files", null, "URI=?", new String[]{item.id}, null, null, null);
                    isGoogle = true;
                } else if (item.type == Type.Dropbox) {
                    CursorItem = lib.dbpp.DataBase.query("Files", null, "URI=?", new String[]{item.id}, null, null, null);
                    isDropbox = true;
                }else {
                    CursorItem = lib.dbpp.DataBase.query("Files", null, "URI=?", new String[]{item.folder}, null, null, null);
                }
                if (CursorItem != null && CursorItem.getCount() == 0) {
                    ContentValues values = new ContentValues();
                    if (isOneDrive) {
                        values.put("URI", item.Uri.getPath());
                    }
                    else if (isGoogle) {
                        values.put("URI", item.id);
                    } else if (isDropbox) {
                        values.put("URI", item.id);
                    }else {
                        values.put("URI", item.folder);
                    }
                    if (item.type == Type.OneDriveAlbum) {
                        values.put("FileName", item.id);
                    } else if (item.type == Type.Google) {
                        values.put("FileName", item.FileName);
                    } else if (item.type == Type.Dropbox) {
                        values.put("FileName", item.FileName);
                    }else {
                        values.put("FileName", item.FileName);
                    }
                    lib.setgstatus("cb_checkedChanged Insert Files");
                    lib.dbpp.DataBase.insert("Files", null, values);
                    //CursorItem = lib.dbpp.DataBase.Query ("Files", null, "URI=?", new string[]{ item.Uri.Path }, null, null, null);
                }
            } while (CursorItem != null && CursorItem.getCount() == 0);
            if (CursorItem != null) CursorItem.moveToFirst();
            lib.setgstatus("cb_checkedChanged GetFileID");
            int FileID = CursorItem.getInt(CursorItem.getColumnIndex("_id"));

            if (isChecked) {
                try {
                    lib.setgstatus("cb_checkedChanged InsertUploads");
                    ContentValues values = new ContentValues();
                    values.put("ServiceID", holder.ServiceID);
                    values.put("FileID", FileID);
                    lib.dbpp.DataBase.insert("Uploads", "", values);
                } catch (java.lang.Exception e) {
                }
            } else {
                try {
                    lib.setgstatus("cb_checkedChanged Delete Uploads " + holder.ServiceID + " " + FileID);
                    //lib.ShowMessage(context, lib.getgstatus());
                    //String[] WhereArgs = new String[]{"" + (holder.ServiceID), "" + (FileID)};
                    lib.dbpp.DataBase.execSQL("DELETE FROM Uploads WHERE ServiceID = "
                            + holder.ServiceID + " AND FileID = " + FileID);

                    //lib.dbpp.DataBase.delete("Uploads", "ServiceID=?, FileID=?",
                    //		WhereArgs);
                } catch (java.lang.Exception e2) {
                    lib.ShowException(context, e2);
                }
            }


        }
    };
    private int mFirstVisibleItem = -1;
    private int mVisibleItemCount = -1;
    private boolean mIsScrolling = false;

    public OnScrollListener onScrollListener = new OnScrollListener() {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            // TODO Auto-generated method stub
            mFirstVisibleItem = firstVisibleItem;
            mVisibleItemCount = visibleItemCount;
            //if (totalItemCount > 0) mIsScrolling = true;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            // TODO Auto-generated method stub
			/*
			int 	SCROLL_STATE_FLING 	The user had previously been scrolling using touch and had performed a fling.
			int 	SCROLL_STATE_IDLE 	The view is not scrolling.
			int 	SCROLL_STATE_TOUCH_SCROLL 	The user is scrolling using touch, and their finger is still on the screen 
			*/
            if (scrollState == SCROLL_STATE_IDLE) {
                mIsScrolling = false;
                ExpandableListView lv = (ExpandableListView) view;
                //PhotoFolderAdapter ppa = (PhotoFolderAdapter)(lv.getAdapter());
                int firstVis = lv.getFirstVisiblePosition();
                int lastVis = lv.getLastVisiblePosition();
                int count = firstVis;

                while (count <= lastVis) {
                    long longposition = lv.getExpandableListPosition(count);
                    int type = ExpandableListView.getPackedPositionType(longposition);
                    int groupPosition = ExpandableListView.getPackedPositionGroup(longposition);
                    int childPosition = ExpandableListView.getPackedPositionChild(longposition);
                    if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                        boolean isLastChild = count == lastVis;
                        View ChildView = lv.getChildAt(count - firstVis);
                        if ((ChildView != null) && (ChildView.getTag() != null)) {
                            ViewHolder holder = (ViewHolder) (ChildView.getTag());
                            if (holder != null) {
                                ImgListItem item = holder.item;
                                ImageView Image = (ImageView) (ChildView.findViewById(R.id.Image));
                                //Bitmap bitmap = ((BitmapDrawable)Image.getDrawable()).getBitmap();
                                if (item.type == ImgFolder.Type.OneDriveAlbum || item.type == ImgFolder.Type.OneDriveFolder) {
                                    try {
                                        LoadThumbnailOneDrive(item, Image);
                                    } catch (Exception e) {
                                        lib.ShowException(context, e);
                                    }
                                } else if (item.type == Type.Google) {
                                    try {
                                        LoadThumbnailGoogle(item, Image);
                                    } catch (Exception e) {
                                        lib.ShowException(context, e);
                                    }
                                } else if (item.type == Type.Dropbox) {
                                    try {
                                        LoadThumbnailDropbox(item, Image);
                                    } catch (Exception e) {
                                        lib.ShowException(context, e);
                                    }
                                }else {
                                    //BitmapWorkerAsyncTask Task = new BitmapWorkerAsyncTask(new ItemParams(item,Image,view), context);
                                    Runnable worker = new getBitmapWorkerThread(new ItemParams(item, Image, view), context);
                                    executor.submit(worker);
                                    //Task.execute();
                                }
                            }
                        }
                    } else {

                    }
                    count++;
                }
            } else {
                mIsScrolling = true;
            }
        }

    };
    public OnLongClickListener onLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View arg0) {
            // TODO Auto-generated method stub
            try {
                CheckBox cb = (CheckBox) arg0;
                final String service = (String) cb.getText();
                if (cb.isChecked()) {
                    final int id = cb.getId();
                    View v = (View) cb.getParent().getParent();
                    if (v.getTag() != null) {
                        ViewHolder holder = (ViewHolder) (v.getTag());
                        final ImgListItem ImgListItem = holder.item;
                        final Uri uri = ImgListItem.Uri;
                        if (ImgListItem.getDownImg() == null && ImgListItem.getDownUri() == null)
                        {
                            if (ImgListItem.type == Type.OneDriveAlbum) {
                                final String file = ImgListItem.id + "/picture?type=full";
                                if (lib.getClient(context) != null) {
                                } else {
                                    JMPPPApplication myApp = (JMPPPApplication) context.getApplication();
                                    lib.setClient(myApp.getConnectClient());
                                }

                                final ProgressDialog mProgress = new ProgressDialog(context);
                                mProgress.setTitle(getS(R.string.Download));
                                mProgress.setMessage(getS(R.string.DownloadingImage));
                                mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                mProgress.setMax(100);
                                mProgress.setIndeterminate(true);
                                mProgress.show();

                                lib.getClient(context).downloadAsync(file, new LiveDownloadOperationListener() {

                                    @Override
                                    public void onDownloadProgress(int arg0, int arg1,
                                                                   LiveDownloadOperation arg2) {
                                        // TODO Auto-generated method stub
                                        int percentCompleted = (int) ((arg1 / arg0) * 100);
                                        mProgress.setProgress(percentCompleted);
                                    }

                                    @Override
                                    public void onDownloadFailed(LiveOperationException arg0,
                                                                 LiveDownloadOperation arg1) {
                                        // TODO Auto-generated method stub
                                        mProgress.dismiss();
                                        lib.ShowToast(context, arg0.getMessage());
                                    }

                                    @Override
                                    public void onDownloadCompleted(LiveDownloadOperation arg0) {
                                        lib.ShowToast(context, getS(R.string.File) + " " + file + " " + getS(R.string.downloaded));
                                        InputStream s = null;
                                        Bitmap mBitmap = null;
                                        try {
                                            s = arg0.getStream();
                                            mBitmap = BitmapFactory.decodeStream(s);
                                            if (mBitmap != null)
                                            {
                                                ImgListItem.setDownImg((mBitmap));
                                                ImgListItem.setDownUri(ShareBitmap(mBitmap,ServiceCursor,id));
                                            }
                                            s.close();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            lib.ShowToast(context, e.getMessage());
                                        }
                                        mProgress.dismiss();

                                    }
                                });


                            } else if (ImgListItem.type == Type.Google) {
                                try {
                                    final Drive drive = lib.getClientGoogle(context);
                                    if (drive != null) {
                                        final ProgressDialog mProgress = new ProgressDialog(context);
                                        mProgress.setTitle(getS(R.string.Download));
                                        mProgress.setMessage(getS(R.string.DownloadingImage));
                                        mProgress.setIndeterminate(true);
                                        mProgress.show();
                                        AsyncTask<Void, Void, Bitmap> Task = new AsyncTask<Void, Void, Bitmap>() {
                                            Exception eex;

                                            @Override
                                            protected Bitmap doInBackground(Void... params) {
                                                try {
                                                    if (ImgListItem.getImg() != null)
                                                        return ImgListItem.getImg();
                                                    Uri Link = null;//ImgListItem.Uri;
                                                    String sLink = null;
                                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                                    InputStream input = null;
                                                    if (Link == null) {
                                                        drive.files().get(ImgListItem.id)
                                                                .executeMediaAndDownloadTo(byteArrayOutputStream);
                                                        input = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                                                        //sLink = file.getWebContentLink();
                                                        //if (sLink != null) Link = Uri.parse((sLink));
                                                    } else if (Link == null) {
                                                        try {
                                                            sLink = " https://drive.google.com/thumbnail?sz=w" + 1000 + "-h" + 1000 + "&id=" + ImgListItem.id + "";
                                                            input = new BufferedInputStream(new URL(sLink).openStream());
                                                        } catch (IOException eex) {
                                                            return null;
                                                        }
                                                    } else {
                                                        HttpRequest request = drive.getRequestFactory().buildGetRequest(new GenericUrl(sLink));
                                                        Future<HttpResponse> response = request.executeAsync(executor);
                                                        response.get(200, TimeUnit.SECONDS).download(byteArrayOutputStream);
                                                        input = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                                                    }
                                                    Bitmap bMap = null;
                                                    try {
                                                        int i = 0;
                                                        while (bMap == null) {
                                                            i++;
                                                            bMap = BitmapFactory.decodeStream(input);
                                                            if (i > 0) break;
                                                        }
                                                    } catch (Exception ex) {
                                                        eex = ex;
                                                        cancel(true);
                                                    }
                                                    if (bMap != null) {
                                                        return bMap;
                                                    } else {
                                                        eex = null;
                                                        cancel(true);
                                                        //lib.ShowToast(context, getS(R.string.Couldnotload) + ImgListItem.FileName);
                                                    }
                                                    if (input != null) {
                                                        try {
                                                            input.close();
                                                        } catch (IOException e) {
                                                            // TODO Auto-generated catch block
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                } catch (Exception ex) {
                                                    //resultTextView.setText("Error downloading picture: " + ex.getMessage());
                                                    cancel(true);
                                                    eex = ex;
                                                } finally {
                                                }
                                                return null;
                                            }

                                            @Override
                                            protected void onCancelled() {
                                                String msg = context.getString(R.string.Couldnotload) +" " + ImgListItem.FileName;
                                                if (mProgress != null) mProgress.hide();
                                                if (eex != null) {
                                                    msg += context.getString(R.string.Error) + eex.getClass().getName() + " " + eex.getMessage() + (lib.getCauses(eex));
                                                }
                                                lib.ShowToast(context, msg);

                                            }


                                            @Override
                                            protected void onPostExecute(Bitmap bMap) {
                                                if (mProgress != null) mProgress.hide();
                                                if (bMap != null) {
                                                    try {
                                                        if (bMap != null) {
                                                            ImgListItem.setDownImg(bMap);
                                                            ImgListItem.setDownUri(ShareBitmap(bMap,ServiceCursor,id));
                                                            /*
                                                            String path = Images.Media.insertImage(context.getContentResolver(),
                                                                    bMap, "Image", "Image" + id);
                                                            Uri newUri = Uri.parse(path);
                                                            ShareUri(ServiceCursor, id, newUri);
                                                            */
                                                        }
                                                    } catch (Exception e) {
                                                        // TODO Auto-generated catch block
                                                        e.printStackTrace();
                                                        lib.ShowToast(context, e.getMessage());
                                                    }
                                                } else {
                                                    lib.ShowToast(context, getS(R.string.Couldnotload) + " " + ImgListItem.FileName);
                                                }
                                            }
                                        };
                                        Task.executeOnExecutor(executor);
                                    }
                                } catch (Exception ex) {
                                    //resultTextView.setText("Error downloading picture: " + ex.getMessage());
                                    lib.ShowToast(context, context.getString(R.string.Couldnotload) + " " + ImgListItem.FileName + context.getString(R.string.Error) + ex.getClass().getName() + " " + ex.getMessage() + (lib.getCauses(ex)));
                                }
                            } else {
                                ShareUri(ServiceCursor, id, uri);
                            }
                        }
                        else
                        {
                            if (ImgListItem.getDownUri() != null)
                            {
                                ShareUri(ServiceCursor,id,ImgListItem.getDownUri());
                            }
                            else if (ImgListItem.getDownImg() != null) {
                               ImgListItem.setDownUri(ShareBitmap(ImgListItem.getDownImg(),ServiceCursor,id));
                                /*String path = Images.Media.insertImage(context.getContentResolver(),
                                        ImgListItem.getImg(), "Image Description", null);
                                Uri newUri = Uri.parse(path);
                                ShareUri(ServiceCursor, id, newUri);*/
                            }
                        }
                    }
                }
                else
                {
                    lib.ShowToast(context,service);
                }
                return true;
            } catch (Exception ex) {
                lib.ShowToast(context, ex.getMessage());
                return false;
            }

        }

    };
    private Uri ShareBitmap(Bitmap mBitmap, Cursor c, int id) throws IOException {

        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) cacheDir = context.getCacheDir();
        File sfile = File.createTempFile("SharePic", ".jpg", cacheDir);
        JMPPPApplication myApp = (JMPPPApplication) context.getApplication();
        myApp.tempFiles.add (sfile);
        FileOutputStream filecon = new FileOutputStream(sfile);
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, filecon);
        if (filecon != null) filecon.close();
        //file.delete();
        //String path = Images.Media.insertImage(context.getContentResolver(),
        //        mBitmap, "Image Description", null);
        Uri newUri = Uri.fromFile(sfile);//Uri.parse(path);
        ShareUri(c,id,newUri);
        return newUri;
    }
    private void ShareUri(Cursor c, int id, Uri uri) {
		/*
		if (service.contains("Facebook")){
			lib.SharePictureOnFacebook(context , uri);
		}
		else if (service.contains("Twitter")){
			lib.SharePictureOnTwitter(context , uri);
		}
		else if (service.contains("Instagram")){
			lib.SharePictureOnInstagram(context , uri);
		}
		else if (service.contains("Pinterest")){
			lib.SharePictureOnPinterest(context , uri);
		}
		*/
        lib.SharePicture(context, uri, c, id);
    }

    public boolean onTouchEvent(MotionEvent event) {
        //MainActivity a = (MainActivity)((context instanceof MainActivity) ? context : null);
        //a.lv.OnTouchEvent(e.Event);
        return false;
    }


    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void setThreadPolicy() {
        if (android.os.Build.VERSION.SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

}