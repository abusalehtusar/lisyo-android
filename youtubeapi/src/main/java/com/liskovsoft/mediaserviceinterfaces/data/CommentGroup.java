package com.liskovsoft.mediaserviceinterfaces.data;

import java.util.List;

public interface CommentGroup {
    List<CommentItem> getComments();
    String getNextPageKey();
}
