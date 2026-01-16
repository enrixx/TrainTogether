package de.othr.traintogether.dto;

import lombok.Data;

@Data
public class ReviewDto {
    private String authorName;
    private String profilePhotoUrl;
    private int rating;
    private String text;
    private String relativeTimeDescription;
}
