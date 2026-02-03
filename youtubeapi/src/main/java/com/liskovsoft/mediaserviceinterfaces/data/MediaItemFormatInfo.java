package com.liskovsoft.mediaserviceinterfaces.data;

import java.util.List;

public interface MediaItemFormatInfo {
    List<MediaFormat> getAdaptiveFormats();
    List<MediaFormat> getRegularFormats();
    List<MediaSubtitle> getSubtitles();
    MediaItemStoryboard getStoryboard();
    String getHlsManifestUrl();
    String getDashManifestUrl();
    String getPlayabilityStatus();
    String getPlayabilityReason();
    String getPlayabilityDescription();
    boolean isPlayableInEmbed();
    String getTrailerVideoId();
    String getUploadDate();
    long getStartTimestamp();
    String getWatchTimeUrl();
    String getEventId();
    String getVisitorMonitoringData();
    boolean isLive();
    boolean isUpcoming();
    boolean isPostLiveDvr();
    String getVideoPlaybackUstreamerConfig();
    float getLoudnessDb();
}
