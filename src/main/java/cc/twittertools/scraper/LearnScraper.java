package cc.twittertools.scraper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import cc.twittertools.corpus.data.JsonStatusBlockReader;
import cc.twittertools.corpus.data.Status;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.Response;

// read a json object
// fetch the HTML page for the same entity
// for field in json:
//   identify all spans containing the field value
//   adjust weights on those spans for the field

// KISS
// Each JSON field gets a model
// the model is a heap of (span, weight), so getting the MLE span is easy.
// store the models using Java's serialization


public class LearnScraper {

  protected AsyncHttpClient client;
  protected HashMap<String, SimpleMultiSet<Pat>> model;
  protected LinkedList<Exemplar> training;
  protected LinkedList<Exemplar> testing;

  public LearnScraper() {
    Builder bdr = new Builder();
    bdr.setRequestTimeoutInMs(10000)
    .setFollowRedirects(true);
    client = new AsyncHttpClient(bdr.build());

    training = new LinkedList<Exemplar>();
    testing = new LinkedList<Exemplar>();
    model = new HashMap<String, SimpleMultiSet<Pat>>();
  }

  public void train_single(String key, JsonElement val, SimpleMultiSet<Pat> model, String html) {
    String valstr = val.getAsString();
    int i = html.indexOf(valstr);
    while (i != -1) {
      Pat p = build_pat(html, valstr, i);
      if (p != null) {
        String test = p.extract(html);
        if (test != null && test.equals(valstr)) {
          model.add(p);
        } 
      }
      i = html.indexOf(valstr, i + valstr.length());
    }
  }

  public void train_on(Exemplar ex) {
    // for each JSON field:
    // find matching patterns in HTML and confirm them, add them to model

    training.add(ex);
    JsonObject truth = ex.get_json();

    for (Map.Entry<String, JsonElement> slot: truth.entrySet()) {
      String key = slot.getKey();
      JsonElement value = slot.getValue();
      if (value.isJsonPrimitive()) {        
        SimpleMultiSet<Pat> slot_model;
        if (model.containsKey(key)) {
          slot_model = model.get(key);          
        } else {
          slot_model = new SimpleMultiSet<Pat>();
          model.put(key, slot_model);
        }
        train_single(key, value, slot_model, ex.get_html());
        if (slot_model.get_max() != null) {
          System.out.println(key + ": " + slot_model.get_max());
        }
      }
    }
  }

  public LinkedList<Exemplar> training_examples() {
    return training;
  }
  
  public void add_test(Exemplar ex) {
    testing.add(ex);
  }
  
  public void run_test() {
    int ex_count = 0;
    int num_correct = 0;
    int num_wrong = 0;
    
    for (Exemplar ex: testing) {
      JsonObject truth = ex.get_json();
      String html = ex.get_html();
      
      // This should really flip around and try to extract every slot
      // for which we have a model, then compute recall over the
      // json slots.
      for (Map.Entry<String, JsonElement> slot: truth.entrySet()) {
        String key = slot.getKey();
        JsonElement value = slot.getValue();
        if (value.isJsonPrimitive()) {
          ex_count++;
          String valstr = value.getAsString();
          System.out.print(key + ": ");
          if (model.containsKey(key)) {
            Pat p = model.get(key).get_max();
            if (p != null) {
              String extval = p.extract(html);
              if (extval != null && extval.equals(valstr)) {
                num_correct++;
                System.out.println("Correct: " + extval);
              } else {
                num_wrong++;
                System.out.println("Wrong: " + extval + ", should be " + valstr);
              }
            } else {
              System.out.println("no optimal pattern");
            }
          } else {
            System.out.println("no matching model");
          }
        }
      }
    }
    
    System.out.println(num_correct + " correct slots, " + num_wrong + " wrong, P% = " +
        (((float)num_correct / ex_count) * 100.0));
  }

  public Response fetch_tweet(String screen_name, String id_str) {
    String url_string = "https://twitter.com/"+screen_name+"/status/"+id_str;

    try {
      Response resp = client.prepareGet(url_string).execute().get();
      if (resp.getStatusCode() == 200) {
        return resp;
      }
    } catch (Exception e) {}
    return null;
  }

  public Pat build_pat(String html, String substr, int start) {
    int hlen = html.length();
    int slen = substr.length();
    int prestart = start - 10;
    int suffend = start + slen + 10;

    if (prestart < 0)
      return null;
    if (suffend > hlen)
      return null;

    String pre = html.substring(prestart, start);
    String suf = html.substring(start + slen, suffend);
    return new Pat(pre, suf);
  }

  public static void main(String args[]) throws IOException {
    if (args.length != 2) {
      System.err.println("Usage: LearnScraper <json> <model-output>");
      System.exit(-1);
    }

    String json_input = args[0];
    String model_file = args[1];
    Random rnd = new Random();
    LearnScraper foo = new LearnScraper();

    JsonStatusBlockReader in = 
        new JsonStatusBlockReader(new File(json_input));

    int ok_count = 0;
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
      Response r = foo.fetch_tweet(screen_name, id_str);
      if (r == null) {
        System.out.println(" fetch fail.");
        continue;
      }

      String html = r.getResponseBody();
      if (html == null) {
        System.out.println(" nope.");
        continue;
      }
      System.out.println(" got it.");

      ok_count++;
      Exemplar ex = new Exemplar(html, json);
      if (rnd.nextBoolean()) {
        System.out.println("training");
        foo.train_on(ex);
      } else {
        System.out.println("saving for testing");
        foo.add_test(ex);
      }
      
      if ((ok_count % 30) == 0) {
        foo.run_test();
      }

    }

    in.close();

    // write model
  }

}
