package com.liskovsoft.mediaserviceinterfaces.data;

public interface MediaFormat {
    int FORMAT_TYPE_REGULAR = 0;
    int FORMAT_TYPE_DASH = 1;
    int FORMAT_TYPE_SABR = 2;

    String getUrl();
    String getSignatureCipher();
    String getType();
    String getITag();
    int getBitrate();
    long getContentLength();
    String getProjectionType();
    int getFps();
    String getQualityLabel();
    String getQuality();
    long getLmt();
    int getWidth();
    int getHeight();
    String getSize();
    String getInit();
    String getIndex();
    String getClen();
    String getSignature();
    String getS();
    String getSp();
    int getFormatType();
    String getMimeType();
    String getCodecs();
}
