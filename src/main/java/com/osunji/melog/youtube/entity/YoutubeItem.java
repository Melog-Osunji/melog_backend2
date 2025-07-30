package com.osunji.melog.youtube.entity;

public class YoutubeItem {

	private String url;
	private String title;
	private String thumbnail;
	private String description;

	public YoutubeItem(String url, String title, String thumbnail, String description) {
		this.url = url;
		this.title = title;
		this.thumbnail = thumbnail;
		this.description = description;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}