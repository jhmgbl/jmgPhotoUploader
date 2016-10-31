// ------------------------------------------------------------------------------
// Copyright (c) 2014 Microsoft Corporation
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
// ------------------------------------------------------------------------------

package org.de.jmg.jmgphotouploader;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.dropbox.core.v2.DbxClientV2;
import com.google.api.services.drive.Drive;
import com.microsoft.live.LiveAuthClient;
import com.microsoft.live.LiveAuthListener;
import com.microsoft.live.LiveConnectClient;
import com.microsoft.live.LiveConnectSession;

import java.io.File;
import java.util.LinkedList;

public class JMPPPApplication extends Application {

    private LiveAuthClient mAuthClient;
    private LiveConnectClient mConnectClient;
    private com.google.api.services.drive.Drive mGoogleDriveClient;
    private DbxClientV2 mDropboxClient;
    private LiveConnectSession mSession;
    public java.util.ArrayList<ImgFolder> BMList = new java.util.ArrayList<ImgFolder>();
    public PhotoFolderAdapter ppa;
    public dbpp dbpp;
    public Context MainContext;
    public boolean LoginClosed = false;
    public ImgFolder OneDriveFolder;
    public ImgFolder GoogleFolder;
    public boolean LoginGoogleClosed = false;
    public LinkedList<File> tempFiles = new LinkedList<>();
    public boolean LoginDropboxClosed;
    public ImgFolder DropboxFolder;

    public LiveAuthClient getAuthClient() {
        return mAuthClient;
    }

    public LiveConnectClient getConnectClient() {
        if (mConnectClient != null)
        {
        	LiveConnectSession session = mConnectClient.getSession();
        	if (session != null && session.isExpired())
        	{
        		Intent LoginLiveIntent = new Intent(MainContext, LoginLiveActivity.class);
				LoginLiveIntent.putExtra("GroupPosition", 0);
				this.ppa = null;
				this.BMList = new java.util.ArrayList<ImgFolder>();
				MainContext.startActivity(LoginLiveIntent);
				((Activity) MainContext).finish();
        	}
        }
    	return mConnectClient;
    }

    public com.google.api.services.drive.Drive getGoogleDriveClient()
    {

        if (mGoogleDriveClient != null && mGoogleDriveClient == null )
        {
            Intent LoginGoogleIntent = new Intent(MainContext, LoginGoogleActivity.class);
            //LoginLiveIntent.putExtra("GroupPosition", 0);
            this.ppa = null;
            this.BMList = new java.util.ArrayList<ImgFolder>();
            MainContext.startActivity(LoginGoogleIntent);
            ((Activity) MainContext).finish();
        }
        return mGoogleDriveClient;
    }

    public LiveConnectSession getSession() {
        return mSession;
    }

    public void setAuthClient(LiveAuthClient authClient) {
        mAuthClient = authClient;
    }

    public void setConnectClient(LiveConnectClient connectClient) {
        mConnectClient = connectClient;
    }
    public void setGoobleDriveClient(Drive connectClient) {
        mGoogleDriveClient = connectClient;
    }

    public void setSession(LiveConnectSession session) {
        mSession = session;
    }

    public void setDropboxClient(DbxClientV2 dropboxClient) {
        mDropboxClient = dropboxClient;
    }
    public DbxClientV2 getDropboxClient()
    {
        return  mDropboxClient;
    }
}
