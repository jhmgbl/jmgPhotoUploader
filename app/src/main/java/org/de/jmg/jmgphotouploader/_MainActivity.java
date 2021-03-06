package org.de.jmg.jmgphotouploader;


import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
//import android.runtime.*;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.*;
import android.widget.*;
import android.widget.ExpandableListView.OnChildClickListener;
import android.provider.*;
import org.de.jmg.jmgphotouploader.Controls.*;
import org.de.jmg.jmgphotouploader.DropBox.DropBoxUserActivity;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.microsoft.live.LiveAuthClient;
import com.microsoft.live.LiveAuthException;
import com.microsoft.live.LiveAuthListener;
import com.microsoft.live.LiveConnectSession;
import com.microsoft.live.LiveOperation;
import com.microsoft.live.LiveOperationException;
import com.microsoft.live.LiveStatus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static org.de.jmg.jmgphotouploader.lib.getFolderItemLock;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [Activity(Label = "JMGPhotoPrinter", MainLauncher = true, Icon = "@drawable/edit")] public class MainActivity : Activity
public class _MainActivity extends Activity
{
	public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 112;
	public ExpandableListView lv = null;
	
	public JMPPPApplication app;
    protected Context Context = this;
	//private Intent LoginLiveIntent;
	//private GestureDetector _gestureDetector;
	//private ScaleGestureDetector _ScaleGestureDetector;
	

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
        try
        {
            AcceptLicenseAndPP(getPreferences(MODE_PRIVATE));
        }
		catch (Throwable e)
		{
            e.printStackTrace();
            finish();
        }
        try
        {
            try
            {
                String[] tmpFiles = lib.getStringArrayFromPrefs(getPreferences(MODE_PRIVATE), "tempFiles");
                if (tmpFiles != null)
                {
                    for (String f : tmpFiles)
                    {
                        try
                        {
                            File F = new File(f);
                            F.delete();
                        }
						catch (Throwable ex)
						{
                            ex.printStackTrace();
                        }
                    }
                    lib.deleteStringArrayFromPrefs(getPreferences(MODE_PRIVATE), "tempFiles");
                }


            }
			catch (Throwable ex)
			{
                ex.printStackTrace();
            }
            app = (JMPPPApplication) getApplication();
			app.MainContext = this.Context;
			initDB(app);
			//requestWindowFeature(Window.FEATURE_PROGRESS);
			//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
			// Set our view from the "main" layout resource
					//_gestureDetector = new GestureDetector(this);
			//_ScaleGestureDetector = new ScaleGestureDetector (this,this);
			//lv.Touch += lv_touch;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
					&& ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
						MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
			}
			else
			{
				initDisplay();
				//lv.setOnChildClickListener(lv_ChildClick);

				loadmedia();
			}


		}
		catch (Throwable ex)
		{
			lib.ShowToast(this, ex.getMessage());
		}

		
		
	}

	private void initDisplay()
	{
		setContentView(R.layout.activity_main);

		lv = new ZoomExpandableListview(this); //FindViewById<ExpandableListView> (Resource.Id.lvItems);
		this.addContentView(lv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

		lv.setChoiceMode(ExpandableListView.CHOICE_MODE_MULTIPLE);
		lv.setClickable(true);
		lv.setFocusable(true);
		lv.setFocusableInTouchMode(true);

	}
	private void initDB(JMPPPApplication app)
	{
		if (app.dbpp != null)
		{
			lib.dbpp = app.dbpp;
			lib.dbpp = new dbpp(this);
			lib.dbpp.createDataBase();
			app.dbpp = lib.dbpp;
		}
		else
		{
			lib.dbpp = new dbpp(this);
			lib.dbpp.createDataBase();
			app.dbpp = lib.dbpp;
		}
		String SQL = "ALTER TABLE Services ADD COLUMN visible BOOL NOT NULL DEFAULT true";
		try
		{
			Cursor c = lib.dbpp.query("Select * FROM Services");
			if (c.getColumnCount() < 5)	lib.dbpp.DataBase.execSQL(SQL);
			boolean first = true;
			lib.setgstatus("enumerate Services");
			while ((first) ? (c.moveToFirst()) : (c.moveToNext()))
			{
				first = false;
				String service = c.getString(c.getColumnIndex("Name"));
				String Package = c.getString(c.getColumnIndex("package"));
				if (service.contains("Facebook")){
					if ( Package == null || Package.equals(""))
					{
						String p = "com.facebook.katana";
						String sql = "Update Services SET package = \"" + p + "\" WHERE _id = " + c.getInt(0);
						lib.dbpp.DataBase.execSQL(sql);
						p = "https://www.facebook.com/mobile/";
						sql = "Update Services SET URL = \"" + p + "\" WHERE _id = " + c.getInt(0);
						lib.dbpp.DataBase.execSQL(sql);
					}
				}
				else if (service.contains("Twitter")){
					if (Package == null|| Package.equals(""))
					{
						String p = "com.twitter." +
								"android,com." +
								"twidroid,com.handmark." +
								"tweetcaster,com.thedeck.android";
						String sql = "Update Services SET package = \"" + p + "\" WHERE _id = " + c.getInt(0);
						lib.dbpp.DataBase.execSQL(sql);

						p = "https://about.twitter.com/de/products/list";
						sql = "Update Services SET URL = \"" + p + "\" WHERE _id = " + c.getInt(0);
						lib.dbpp.DataBase.execSQL(sql);

					}
				}
				else if (service.contains("Instagram")){
					if (Package == null || Package.equals("") )
					{
						String p = "com.instagram.android";
						String sql = "Update Services SET package = \"" + p + "\" WHERE _id = " + c.getInt(0);
						lib.dbpp.DataBase.execSQL(sql);
						p = "http://instagram.de.uptodown.com/android";;
						sql = "Update Services SET URL = \"" + p + "\" WHERE _id = " + c.getInt(0);
						lib.dbpp.DataBase.execSQL(sql);
					}
				}
				else if (service.contains("Pinterest")){
				}
				//c = lib.dbpp.query("Select * FROM Services");

			}
			if (c.getCount() < 6)
			{
				String sql = "INSERT INTO Services ('Name','URL','package') VALUES('Flickr','http://flickr.com','com.yahoo.mobile.client.android.flickr')";
				lib.dbpp.DataBase.execSQL(sql);
				sql = "INSERT INTO Services ('Name','URL','package') VALUES('Tumblr','http://tumblr.com','com.tumblr')";
				lib.dbpp.DataBase.execSQL(sql);
			}
			if (c.getCount() < 7)
			{
				String sql = "INSERT INTO Services ('Name','URL','package') VALUES('Photobucket','http://photobucket.com','com.photobucket.android')";
				lib.dbpp.DataBase.execSQL(sql);
			}
            if (c.getCount() < 8)
            {
                String sql = "INSERT INTO Services ('Name','URL','package') VALUES('Google+','https://plus.google.com','com.google.android.apps.plus')";
                lib.dbpp.DataBase.execSQL(sql);
            }

			lib.getContentProviders(this, lib.getUriToDrawable(this, R.drawable.res));
		}
		catch (Throwable ex)
		{
			System.out.println(ex.getMessage());
		}

	}

	void getLocalFolders()
	{
		boolean blnFolderItemLockInc = false;
		try
		{
			if (getFolderItemLock++ > 1)
			{
				getFolderItemLock--;
				return;
			}
			else
			{
				blnFolderItemLockInc = true;
			}
			String selection = "";
			String sort = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "," + MediaStore.MediaColumns.DATA;
			if (app.blnSortOrderDesc) sort += " DESC";
			String[] selectionArgs = new String[]{};
			String[] projection = new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.BUCKET_ID};
			Cursor mediaCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sort);
            if (mediaCursor != null) lib.GetThumbnails(this, false, mediaCursor, app.BMList, true);

			mediaCursor = getContentResolver().query(MediaStore.Images.Media.INTERNAL_CONTENT_URI, projection, selection, selectionArgs, sort);
            if (mediaCursor != null) lib.GetThumbnails(this, true, mediaCursor, app.BMList, true);

		}
		finally
		{
			getFolderItemLock--;
		}
	}

	private void loadmedia() throws Throwable
	{
		System.out.println(lib.getExternalPicturesDir());

		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		final String lastProvider = prefs.getString("lastProvider", null);
		app.lastProvider = lastProvider;
		final String lastPath = prefs.getString("lastPath", "");
		app.lastPath = lastPath;
		final String lastFileName = prefs.getString("lastFileName", "");
		app.lastFileName = lastFileName;
		//lv.setOverScrollMode(View.OVER_SCROLL_NEVER);
		app.lastFolderfound = false;
		app.lastFilefound = false;
		app.blnSortOrderDesc = prefs.getBoolean("SortOrderDesc", false);

		if (app.ppa == null)
		{
			app.LocalFolder = new ImgFolder(this.getString(R.string.Local), ImgFolder.Type.Local);
			app.BMList.add(app.LocalFolder);
			/*
			try
			{
				//content://com.google.android.apps.photos.contentprovider/0/1/content%3A%2F%2Fmedia%2Fexternal%2Fimages%2Fmedia%2F87/ORIGINAL/NONE/1661484
				Uri ImageUri = Uri.parse("content://com.google.android.apps.photos.contentprovider/images/media");
				mediaCursor = getContentResolver().query(ImageUri, projection, selection, selectionArgs, "");
				if (mediaCursor != null) lib.GetThumbnails(this, true, mediaCursor, app.BMList);
			}
			catch (Throwable ex)
			{
				ex.printStackTrace();
			}
			*/
			if (app.ppa == null) {
				app.BMList.add(new ImgFolder("One Drive",ImgFolder.Type.OneDriveAlbum));
                app.OneDriveFolder = app.BMList.get(app.BMList.size() - 1);
                app.BMList.add(new ImgFolder("Google Drive",ImgFolder.Type.Google));
                app.GoogleFolder = app.BMList.get(app.BMList.size() - 1);
                app.BMList.add(new ImgFolder("Dropbox",ImgFolder.Type.Dropbox));
                app.DropboxFolder = app.BMList.get(app.BMList.size() - 1);
            }
		}
		SetPPA();
		if (lastProvider != null)
		{
			app.latchExpand = new CountDownLatch(1);
            if (lastProvider.equalsIgnoreCase(ImgFolder.Type.OneDriveAlbum.toString()) || lastProvider.equalsIgnoreCase(ImgFolder.Type.OneDriveFolder.toString()))
            {
                lv.expandGroup(app.ppa.rows.indexOf(app.OneDriveFolder));
            }
            else if (lastProvider.equalsIgnoreCase(ImgFolder.Type.Google.toString()))
            {
                lv.expandGroup(app.ppa.rows.indexOf(app.GoogleFolder));
            }
            else if (lastProvider.equalsIgnoreCase(ImgFolder.Type.Dropbox.toString()))
            {
                lv.expandGroup(app.ppa.rows.indexOf(app.DropboxFolder));
            }
            else
            {
                app.latchExpand.countDown();
				lv.expandGroup(app.ppa.rows.indexOf(app.LocalFolder));
				//findPath();
			}
            if (app.latchExpand != null)
            {
                app.latchExpand = null;
            }
            if (app.lastPath.equals("/")) app.lastFolderfound = true;

        }
    }

	private void AcceptLicenseAndPP(SharedPreferences prefs) throws Throwable
	{
        boolean blnLicenseAccepted = prefs.getBoolean("LicenseAccepted", false);
        if (!blnLicenseAccepted)
        {
            InputStream is = this.getAssets().open("LICENSE");
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String strLicense = s.hasNext() ? s.next() : "";
            s.close();
            is.close();
            lib.yesnoundefined res = (lib.ShowMessageYesNo(this,
                    strLicense,
                    getString(R.string.license),
                    true));

			lib.yesnoundefined res2 = lib.yesnoundefined.undefined;
			if (res == lib.yesnoundefined.yes)
			{
				res2 = lib.AcceptPrivacyPolicy(this, Locale.getDefault());
			}

            if (res == lib.yesnoundefined.yes && res2 == lib.yesnoundefined.yes)
            {
                prefs.edit().putBoolean("LicenseAccepted", true).commit();
            }
            else
            {
				prefs.edit().putBoolean("LicenseAccepted", false).commit();
				finish();
            }


        }
    }

	public void xfindPath()
	{
		if (app.lastProvider != null && !app.lastFolderfound && app.lastPath != null)
		{
			boolean found = false;
			int i = 0;
			for (ImgFolder F : app.ppa.rows)
			{
				if (F.type.toString().equals(app.lastProvider) || (F.type.toString().contains("OneDrive") && app.lastProvider.contains("OneDrive")))
					if (app.lastPath.startsWith(F.Name) && F.expanded == false)
					{
						lv.expandGroup(i);
						if (app.lastPath.equals(F.Name))
						{
							app.lastFolderfound = true;
							if (app.lastFilefound == false)
							{
								if (app.lastFilePosition > -1)
								{
									app.ppa.lv.setSelectedChild(i, app.lastFilePosition, true);
									app.lastFilefound = true;
								}
							}
						}
						found = true;
						break;
					}
				i++;
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Permission granted.
				try {
					initDisplay();
					loadmedia();
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}
			} else {
				// User refused to grant permission.
			}
		}
	}

	private void SetPPA()
	{
		if (app.ppa == null)  	app.ppa = new PhotoFolderAdapter(this, app.BMList);
		app.ppa.context = this;
		app.ppa.rows = app.BMList;
		app.ppa.ServiceCursor = null;
		app.ppa.CountVisible = 0;
		lv.setAdapter(app.ppa);
		lv.setOnScrollListener(app.ppa.onScrollListener);
	}
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savePrefs();
		try
		{
			for (DialogInterface dlg : lib.OpenDialogs)
			{
				try
				{
					dlg.dismiss();
				}
				catch (Throwable eex)
				{
					eex.printStackTrace();
				}
			}
		}
		catch (Throwable ex)
		{
			ex.printStackTrace();
		}
	}
	@Override
	public void onResume() {
		super.onResume();
		if (app.dbpp != null && app.dbpp.isClosed) app.dbpp.openDataBase();
	}
	@Override
	public void onPause() {
        //if (app.dbpp != null && app.dbpp.DataBase.isOpen())app.dbpp.close();
        /*
		for(File f: app.tempFiles)
        {
            try
            {
                f.delete();
            }
            catch(Throwable ex)
            {

            }
        }
        app.tempFiles.clear();
        */
        super.onPause();
	}

	@Override
	public void onStop() {

		super.onStop();
	}

	@Override
	protected void onDestroy()
	{
		try
		{
			if (app.dbpp != null && app.dbpp.DataBase.isOpen())app.dbpp.close();

			app.tempFiles.clear();
			app.BMList.clear();
			app.clear();
			app = null;
		}
		catch (Throwable ex)
		{
			ex.printStackTrace();
		}
		super.onDestroy();

		/*
		LiveAuthClient client = app.getAuthClient();
		if (client != null) client.logout(new LiveAuthListener() {
			
			@Override
			public void onAuthError(LiveAuthException arg0, Object arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAuthComplete(LiveStatus arg0, LiveConnectSession arg1,
					Object arg2) {
				// TODO Auto-generated method stub
				
			}
		});
		*/
		
	}

	private void savePrefs()
	{
		try
		{
			if (app.tempFiles.size() > 0)
			{
				String[] tmpFiles = new String[app.tempFiles.size()];
				for (int i = 0; i < app.tempFiles.size(); i++)
				{
					tmpFiles[i] = app.tempFiles.get(i).getPath();
				}
				lib.putStringArrayToPrefs(getPreferences(MODE_PRIVATE), tmpFiles, "tempFiles");
			}
			if (app.lastItem != null)
			{
				SharedPreferences.Editor edit = getPreferences(MODE_PRIVATE).edit();
				edit.putString("lastProvider", app.lastItem.type.toString());
				edit.putString("lastPath", app.lastItem.path);
				edit.putString("lastFileName", app.lastItem.FileName);
				edit.commit();
			}
		}
		catch (Throwable ex)
		{
			ex.printStackTrace();
		}
	}
	@Override
	public void onBackPressed()
	{
		/*
		lib.dbpp.close();
		LiveAuthClient client = app.getAuthClient();
		if (client != null && app.listener != null) client.logout(app.listener);
		*/
		savePrefs();
		super.onBackPressed();
		
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        //case R.id.action_search:
	        //    openSearch();
	         //   return true;
	        case R.id.action_settings:
	            openSettings();
	            return true;
	        case R.id.action_resetdatabase:
	            resetdatabase();
	            return true;
	        case R.id.action_refresh:
				try
				{
					savePrefs();
					app.ppa = null;
					app.BMList.clear();
					loadmedia();
					return true;
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}
			case R.id.action_sortorder:
				app.blnSortOrderDesc = app.blnSortOrderDesc ^ true;
				getPreferences(MODE_PRIVATE).edit().putBoolean("SortOrderDesc", app.blnSortOrderDesc).apply();
				return true;
            case R.id.action_resetDropbox:
                collapseProvider(app.DropboxFolder);
                app.setDropboxClient(null);
                lib.setClientDropbox(null);
                StartLoginDropbox(app.DropboxFolder, true);
                return true;
            case R.id.action_resetGoogle:
                collapseProvider(app.GoogleFolder);
                app.setGoogleDriveClient(null);
                lib.setClientGoogle(null);
                StartLoginGoogle(app.GoogleFolder, true);
                return true;
            case R.id.action_resetLive:
                collapseProvider(app.OneDriveFolder);
                app.setAuthClient(null);
                app.setConnectClient(null);
                lib.setClient(null);
                StartLoginLive(app.OneDriveFolder, true);
                return true;
			case R.id.action_showPrivacyPolicy:
				lib.yesnoundefined dresult = lib.yesnoundefined.undefined;
				try
				{
					dresult = lib.AcceptPrivacyPolicy(this, Locale.getDefault());
					SharedPreferences prefs = this.getPreferences(MODE_PRIVATE);
					if (dresult == lib.yesnoundefined.yes)
					{
						prefs.edit().putBoolean("LicenseAccepted", true).commit();
					}
					else
					{
						prefs.edit().putBoolean("LicenseAccepted", false).commit();
						finish();
					}
				}
				catch (Throwable throwable)
				{
					throwable.printStackTrace();
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
	    }
	}

    private void collapseProvider(ImgFolder Folder)
    {
        if (Folder.fetched && Folder.Name.equalsIgnoreCase("/"))
        {
            int i = app.ppa.rows.indexOf(Folder);
            if (i >= 0) lv.collapseGroup(i);
        }
    }

    private void resetdatabase()
	{
		try
		{
			Cursor c = lib.dbpp.query("Select * FROM Services");
			boolean first = true;
			lib.setgstatus("enumerate Services");
			int i = 0;
			while ((first) ? (c.moveToFirst()) : (c.moveToNext()))
			{
				i++;
				first = false;
				String service = c.getString(c.getColumnIndex("Name"));
				String Package = c.getString(c.getColumnIndex("package"));
				if (service.contains("Facebook")|| i == 1)
				{
					String p = "com.facebook.katana";
					String sql = "Update Services SET package = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					p = "https://www.facebook.com/mobile/";
					sql = "Update Services SET URL = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					p = "Facebook";
					sql = "Update Services SET Name = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
					p = "true";
					sql = "Update Services SET visible = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
				}
				else if (service.contains("Twitter") || i == 2){
					String p = "com.twitter." +
							"android,com." +
							"twidroid,com.handmark." +
							"tweetcaster,com.thedeck.android";
					String sql = "Update Services SET package = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
					p = "https://about.twitter.com/de/products/list";
					sql = "Update Services SET URL = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
					p = "Twitter";
					sql = "Update Services SET Name = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
					p = "true";
					sql = "Update Services SET visible = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
				}
				else if (service.contains("Instagram") || i == 3)
				{
					String p = "com.instagram.android";
					String sql = "Update Services SET package = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
					p = "http://instagram.de.uptodown.com/android";;
					sql = "Update Services SET URL = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
					p = "Instagram";
					sql = "Update Services SET Name = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
					p = "true";
					sql = "Update Services SET visible = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
				}
				else if (service.contains("Pinterest") || i == 4)
				{
					String p = "com.pinterest";
					String sql = "Update Services SET package = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
					p = "http://pinterest.com";;
					sql = "Update Services SET URL = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
					p = "Pinterest";;
					sql = "Update Services SET Name = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
					
					p = "true";
					sql = "Update Services SET visible = \"" + p + "\" WHERE _id = " + c.getInt(0);
					lib.dbpp.DataBase.execSQL(sql);
				}
				//c = lib.dbpp.query("Select * FROM Services");
			
			}
		}
		catch (Throwable ex)
		{
			System.out.println(ex.getMessage());
		}
	}
	
	private void openSearch()
	{
		lib.ShowMessage(this, getString(R.string.search));
	}

	private boolean _liveLock = false;

    public void StartLoginLive(ImgFolder OneDrive, boolean reset)
    {
		if (_liveLock) return;
		_liveLock = true;
		app.LoginClosed=false;
		app.OneDriveFolder = OneDrive;
		Intent LoginLiveIntent = new Intent(this, LoginLiveActivity.class);
		LoginLiveIntent.putExtra("GroupPosition", lib.LastgroupPosition);
        LoginLiveIntent.putExtra("reset", reset);
        this.startActivityForResult(LoginLiveIntent, LoginLiveActivity.requestCode);
	}

	private boolean _GoogleLock = false;

    public void StartLoginGoogle(ImgFolder Google, boolean reset)
    {
		if (_GoogleLock) return;
		_GoogleLock = true;
		app.LoginGoogleClosed=false;
		app.GoogleFolder = Google;
		Intent LoginIntent = new Intent(this, LoginGoogleActivity.class);
		LoginIntent.putExtra("GroupPosition", lib.LastgroupPosition);
        LoginIntent.putExtra("reset", reset);
        this.startActivityForResult(LoginIntent, LoginGoogleActivity.requestCode);
	}

	private boolean _DBLock = false;

    public void StartLoginDropbox(ImgFolder Dropbox, boolean reset)
    {
		if (_DBLock) return;
		_DBLock = true;
		app.LoginDropboxClosed=false;
		app.DropboxFolder = Dropbox;
		Intent LoginIntent = new Intent(this, DropBoxUserActivity.class);
		LoginIntent.putExtra("GroupPosition", lib.LastgroupPosition);
        LoginIntent.putExtra("reset", reset);
        this.startActivityForResult(LoginIntent, DropBoxUserActivity.requestCode);
	}
	
	private void openSettings()
	{
		try
		{
			Intent SettingsIntent = new Intent(this, SettingsActivity.class);
			SettingsIntent.putExtra("GroupPosition", lib.LastgroupPosition);
			this.startActivityForResult(SettingsIntent, SettingsActivity.requestCode); //, LoginLiveActivity.requestCode);
			//this.finish();
		}
		catch (Throwable ex)
		{
			lib.ShowException(this, ex);
		}
			//context.finish();
	}
	
	private OnChildClickListener lv_ChildClick = new OnChildClickListener() {
		
		@Override
		public boolean onChildClick(ExpandableListView parent, View v,
				int groupPosition, int childPosition, long id) {
			if (v.getTag() != null)
			{
				ViewHolder holder = (ViewHolder)(v.getTag());
				ImgListItem ImgListItem = holder.item;
                app.setLastItem(ImgListItem);
                app.lastGroupPosition = groupPosition;
                boolean isOneDrive = ImgListItem.type == ImgFolder.Type.OneDriveAlbum
						|| ImgListItem.type == ImgFolder.Type.OneDriveFolder;
				if (isOneDrive)
				{
					lib.StartViewer(Context , ImgListItem.Uri);
				}
				else if (ImgListItem.type == ImgFolder.Type.Google)
				{
					lib.StartBrowser(Context,Uri.parse(ImgListItem.folder));
				}
				else if (ImgListItem.type == ImgFolder.Type.Dropbox)
				{
					try
					{
						//SharedLinkMetadata M = lib.getClientDropbox(_MainActivity.this).sharing().createSharedLinkWithSettings(ImgListItem.folder);
						lib.StartBrowser(Context , Uri.parse(lib.getClientDropbox(_MainActivity.this).sharing().getFileMetadata(ImgListItem.folder).getPreviewUrl()));
					}
					catch (DbxException e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					lib.StartViewer(Context, Uri.parse("file://" + ImgListItem.folder));
				}
			}
			return false;
		}
	};

	boolean firstStart = true;

	@Override
	protected void onStart() {
        super.onStart();
        if (app.dbpp != null && app.dbpp.isClosed) app.dbpp.openDataBase();
		if (firstStart)
		{
			firstStart = false;
			//lib.SelectImage(this);
		}
	}

        	
    
    private LiveOperation LiveOp;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	Toast.makeText(this, getString(R.string.onActivityResultcalled), Toast.LENGTH_LONG).show();
        lib.setClient(app.getConnectClient());
    	lib.setClientGoogle(app.getGoogleDriveClient());
		lib.setClientDropbox(app.getDropboxClient());

		if (requestCode == LoginLiveActivity.requestCode)
		{
			_liveLock = false;
			if(resultCode == Activity.RESULT_OK && lib.getClient(this) != null) {
				final int GroupPosition = data.getExtras().getInt("GroupPosition");
                final boolean reset = data.getExtras().getBoolean("reset");
                if (!reset)
                {
                    try
                    {
                        lib.GetThumbnailsOneDrive(this, "/", app.OneDriveFolder, GroupPosition, _MainActivity.this.lv);
                        //if (app.OneDriveFolder != null) app.OneDriveFolder.fetched = true;
                    }
                    catch (LiveOperationException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
		}
		else if (requestCode == LoginGoogleActivity.requestCode)
		{
			_GoogleLock = false;
			if (resultCode == Activity.RESULT_OK && lib.getClientGoogle(this) != null) {
				final int GroupPosition = data.getExtras().getInt("GroupPosition");
                final boolean reset = data.getExtras().getBoolean("reset");
                if (!reset)
                {
                    try
                    {
                        lib.GetThumbnailsGoogle(this, "/", app.GoogleFolder, GroupPosition, _MainActivity.this.lv);
                        //if (app.GoogleFolder != null) app.GoogleFolder.fetched = true;
                    }
                    catch (Throwable e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

			}

		}
		else if (requestCode == DropBoxUserActivity.requestCode)
		{
			_DBLock = false;
			if (resultCode == Activity.RESULT_OK && lib.getClientDropbox(this) != null) {
				final int GroupPosition = data.getExtras().getInt("GroupPosition");
                final boolean reset = data.getExtras().getBoolean("reset");

                if (!reset)
                {
                    try
                    {
                        lib.GetThumbnailsDropbox(this, "/", app.DropboxFolder, GroupPosition, _MainActivity.this.lv);
                        //if (app.DropboxFolder != null) app.DropboxFolder.fetched = true;
                    }
                    catch (Throwable e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

		}

		else if (requestCode == SettingsActivity.requestCode && resultCode == Activity.RESULT_OK)
        {
        	boolean changed = data.getExtras().getBoolean("changed");
        	final int GroupPosition = data.getExtras().getInt("GroupPosition");
        	if (changed)
        		{
        			refreshPhotos();
        		}
        	
        }    
    }
    private void refreshPhotos()
    {
    	app.ppa = null;
		app.BMList = new java.util.ArrayList<ImgFolder>();
		try
		{
			loadmedia();
		}
		catch (Throwable throwable)
		{
			throwable.printStackTrace();
		}
		//SetPPA();
	}
	
}