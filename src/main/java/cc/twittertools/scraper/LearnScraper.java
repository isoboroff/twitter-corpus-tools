package cc.twittertools.scraper;

import cc.twittertools.corpus.data.Status;
import cc.twittertools.corpus.data.JsonStatusBlockReader;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import com.google.common.collect.TreeMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.ImmutableMultiset;
import com.google.gson.JsonObject;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.Response;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.apache.lucene.queryparser.classic.*;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.index.Term;
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


public class LearnScraper {

    protected AsyncHttpClient client;
    protected LinkedList<Exemplar> training;
    protected LinkedList<Exemplar> testing;

    public LearnScraper() {
	Builder bdr = new Builder();
	bdr.setRequestTimeoutInMs(10000)
	    .setFollowRedirects(true);
	client = new AsyncHttpClient(bdr.build());

	training = new LinkedList<E
    }

    public void train_on(Exemplar ex)

    public Response fetch_tweet(String screen_name, String id_str) {
	String url_string = "https://twitter.com/"+screen_name+"/status/"+id_str;
	String html = null;

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
	if (suffend > html.length())
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

	LearnScraper foo = new LearnScraper();

	JsonStatusBlockReader in = 
	    new JsonStatusBlockReader(new File(json_input));

	SimpleMultiSet<Pat> pats = new SimpleMultiSet<Pat>();

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

	    int i = 0;
	    while (i != -1) {
		i = html.indexOf(screen_name, i+1);
		if (i >= 0) {
		    Pat p = foo.build_pat(html, screen_name, i);
		    if (p != null) {
			String test = p.extract(html);
			if (test.equals(screen_name)) {
			    pats.add(p);
			}
		    }
		}
	    }
	    System.out.println("++++ Max is (" + pats.get_max() + "): "
			       + pats.get_max_count());
	}

	in.close();
	
	// write model
   }

}
