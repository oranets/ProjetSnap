package com.projetsnap.orane.projetsnap;



public class SnapClass {
<<<<<<< HEAD
    private String image, loc;

=======
    private String image, loc, title;
>>>>>>> origin/master
    public SnapClass() {
    }

    public SnapClass(String image, String loc, String title) {
        this.image = image;
        this.loc = loc;
        this.title=title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }public String getTitle() {
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




}
