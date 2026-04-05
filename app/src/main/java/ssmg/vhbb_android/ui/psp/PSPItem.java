package ssmg.vhbb_android.ui.psp;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ssmg.vhbb_android.BaseItem;
import ssmg.vhbb_android.Constants.PSP;

public class PSPItem extends BaseItem {

    private final String IconUrl;
    private final String LongDescription;
    private final String SourceUrl;
    private final String ReleaseUrl;
    private final String TitleID;
    private final String TrailerUrl;
    private final int Type;
    private final int ID;
    private final int Downloads;
    private final long Size;
    private final int AI;
    private Date Date;
    private String[] ScreenshotsUrl;

    public PSPItem(String name, String iconUrl, String version, String author, String desc, String longDesc, String date, String srcUrl, String relUrl, String url, String screenshots, int type, int id, int downloads, long size, int ai, String titleId, String trailer) {
        super(name, "", version, author, desc, url);

        this.IconUrl = PSP.ICONS_PARENT_URL + iconUrl;
        this.LongDescription = longDesc;
        this.SourceUrl = srcUrl;
        this.ReleaseUrl = relUrl;
        this.Type = type;
        this.ID = id;
        this.Downloads = downloads;
        this.Size = size;
        this.AI = ai;
        this.TitleID = titleId;

        if (!trailer.isEmpty() && !trailer.equals("0")) {
            this.TrailerUrl = PSP.TRAILER_PARENT_URL + trailer + ".mp4";
        } else {
            this.TrailerUrl = "";
        }

        this.setDate(date);

        if (!screenshots.equals("")) {
            String[] scArray = screenshots.split(";");
            for (int i = 0; i < scArray.length; i++)
                scArray[i] = PSP.PARENT_URL + "/" + scArray[i];
            this.ScreenshotsUrl = scArray;
        }
    }

    public String getIconUrl() { return IconUrl; }
    public String getLongDescription() { return LongDescription; }
    public String getSourceUrl() { return SourceUrl; }
    public String getReleaseUrl() { return ReleaseUrl; }
    public String getTitleID() { return TitleID; }
    public String getTrailerUrl() { return TrailerUrl; }
    public int getType() { return Type; }

    public String getTypeString() {
        switch (Type) {
            case 11: return "Original Game";
            case 12: return "Game Port";
            case 14: return "Utility";
            case 15: return "Emulator";
            default: return "";
        }
    }

    public int getID() { return ID; }
    public int getDownloads() { return Downloads; }
    public long getSize() { return Size; }
    public int getAI() { return AI; }
    public Date getDate() { return Date; }

    public String getDateString() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return dateFormat.format(this.Date);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "1970-01-01";
    }

    private void setDate(String date) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            this.Date = dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String[] getScreenshotsUrl() { return ScreenshotsUrl; }

}
