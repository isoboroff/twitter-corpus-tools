package cc.twittertools.scraper;

import com.google.gson.JsonObject;

public class Exemplar {
  protected String html;
  protected JsonObject json;

  public Exemplar(String _html, JsonObject _json) {
    html = _html;
    json = _json;
  }

  public String get_html() { return html; }
  public JsonObject get_json() { return json; }
}
