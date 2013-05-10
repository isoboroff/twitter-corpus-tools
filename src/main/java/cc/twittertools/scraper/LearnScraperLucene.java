package cc.twittertools.scraper;

import cc.twittertools.corpus.data.Status;
import cc.twittertools.corpus.data.JsonStatusBlockReader;
import java.io.File;
import java.io.IOException;
import com.google.gson.JsonObject;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.Response;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.spans.*;

// read a json object
// fetch the HTML page for the same entity
// for field in json:
//   identify all spans containing the field value
//   adjust weights on those spans for the field

// KISS
// Each JSON field gets a model
// the model is a heap of (span, weight), so getting the MLE span is easy.
// store the models using Java's serialization


public class LearnScraperLucene {

  protected AsyncHttpClient client;

  public LearnScraperLucene() {
    Builder bdr = new Builder();
    bdr.setRequestTimeoutInMs(10000)
    .setFollowRedirects(true);
    client = new AsyncHttpClient(bdr.build());
  }

  public String get_html_for(String screen_name, String id_str) {
    String url_string = "http://twitter.com/"+screen_name+"/status/"+id_str;
    String html = null;

    try {
      Response resp = client.prepareGet(url_string).execute().get();
      if (resp.getStatusCode() == 200) {
        html = resp.getResponseBody();
      }
    } catch (Exception e) {}
    return html;
  }

  public static void main(String args[]) throws IOException {
    if (args.length != 2) {
      System.err.println("Usage: LearnScraper <json> <model-output>");
      System.exit(-1);
    }

    String json_input = args[0];
    String model_file = args[1];

    LearnScraperLucene foo = new LearnScraperLucene();

    JsonStatusBlockReader in = 
        new JsonStatusBlockReader(new File(json_input));
    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);


    while (true) {
      Status st = in.next();
      if (st == null)
        break;

      JsonObject json = st.getJsonObject();
      if (json.get("text") == null)
        continue;

      String screen_name = json.getAsJsonObject("user").get("screen_name").getAsString();
      String id_str = json.get("id_str").getAsString();
      System.out.print("----------- Fetching " + screen_name +"/"+ id_str);
      String html = foo.get_html_for(screen_name, id_str);
      if (html == null) {
        System.out.println(" nope.");
        continue;
      }
      System.out.println(" got it.");

      MemoryIndex index = new MemoryIndex();
      index.addField("html", html, analyzer);
      SpanTermQuery q = new SpanTermQuery(new Term("html", screen_name));
      /*
      Spans spans = q.getSpans(index.createSearcher().getIndexReader());
      while (spans.next() == true) {
        System.out.println("Doc: " + spans.doc() + " from " + spans.start() + " to " + spans.end());
      }*/

      // do search for multiple ranked passages
    }

    in.close();

    // write model
  }

}
