package com.projetsnap.orane.projetsnap;


public class SnapClass {
    private String image, loc, title,date;
    public SnapClass() {
    }

    public SnapClass(String image, String loc, String title, String date) {
        this.image = image;
        this.loc = loc;
        this.title=title;
        this.date=date;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String image) {
        this.title = image;
    }

    public String getLoc() {
        return loc;
    }

    public void setLocation(String loc) {
        this.loc = loc;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }


}
