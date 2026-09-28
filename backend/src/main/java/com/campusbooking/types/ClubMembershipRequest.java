package com.campusbooking.types;

public class ClubMembershipRequest {
    private String stuEmail;
    private String clubName;

    public ClubMembershipRequest() {}

    public String getStuEmail() {
        return stuEmail;
    }

    public void setStuEmail(String stuEmail) {
        this.stuEmail = stuEmail;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }
}
