package com.osunji.melog.youtube.dto;

public class YoutubeItemDTO {

	private String url;
	private String title;
	private String thumbnail;
	private String description;

	public YoutubeItemDTO(String url, String title, String thumbnail, String description) {
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