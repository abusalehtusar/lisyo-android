package com.liskovsoft.mediaserviceinterfaces.data;

public interface CommentItem {
    String getAuthor();
    String getMessage();
    String getPublishedDate();
    String getLikeCount();
    boolean isLiked();
    String getAuthorThumbnailUrl();
    String getAuthorChannelId();
    String getReplyKey();
}
