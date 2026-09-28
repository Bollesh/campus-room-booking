package com.campusbooking.model;

import java.io.Serializable;
import java.util.Objects;

public class ClubMembershipId implements Serializable {

    private String stuEmail;
    private String clubName;

    public ClubMembershipId() {}

    public ClubMembershipId(String stuEmail, String clubName) {
        this.stuEmail = stuEmail;
        this.clubName = clubName;
    }

    // Override equals and hashCode methods for composite key comparison

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClubMembershipId)) return false;
        ClubMembershipId that = (ClubMembershipId) o;
        return Objects.equals(stuEmail, that.stuEmail) &&
               Objects.equals(clubName, that.clubName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stuEmail, clubName);
    }
}
