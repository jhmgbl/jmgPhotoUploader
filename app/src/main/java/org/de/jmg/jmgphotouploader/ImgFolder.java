package org.de.jmg.jmgphotouploader;


public class ImgFolder extends Object
{
	public enum Type {
		unknown,	    
		Local,
		OneDriveAlbum,
		OneDriveFolder,
		Dropbox, Google
	}
	public ImgFolder(String Name, Type type)
	{
		this.Name = Name;
		this.type = type;
	}
	public ImgFolder(String Name, Type type, String id)
	{
		this.Name = Name;
		this.type = type;
		this.id = id;
	}
	public String Name = "";
	public Type type = Type.Local;
	public String id;
	public boolean fetched;
	public boolean expanded;
	public java.util.ArrayList<ImgListItem> items = new java.util.ArrayList<ImgListItem>();
}