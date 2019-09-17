package in.utkal.moredio;

import java.util.ArrayList;

public class RadioStation {

    private String name, tag, lowUrl, highUrl, logoUrl, webUrl;
    private ArrayList<String> m_languages;
    private ArrayList<String> m_genres;

    public RadioStation(String name, String tag, String lowUrl, String highUrl, String logoUrl, String webUrl, ArrayList<String> languages, ArrayList<String> genres) {
        this.name = name;
        this.tag = tag;
        this.lowUrl = lowUrl;
        this.highUrl = highUrl;
        this.logoUrl = logoUrl;
        this.webUrl = webUrl;
        this.m_languages = new ArrayList<String>();
        this.m_genres = new ArrayList<String>();
        this.m_languages.addAll(languages);
        this.m_genres.addAll(genres);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getLowUrl() {
        return lowUrl;
    }

    public void setLowUrl(String lowUrl) {
        this.lowUrl = lowUrl;
    }

    public String getHighUrl() {
        return highUrl;
    }

    public void setHighUrl(String highUrl) {
        this.highUrl = highUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public ArrayList<String> getLanguages() {
        return m_languages;
    }

    public void setLanguages(ArrayList<String> langs) {
        this.m_languages = langs;
    }

    public ArrayList<String> getGenres() {
        return m_genres;
    }

    public void setGenres(ArrayList<String> genres) {
        this.m_genres = genres;
    }
}
