package cc.twittertools.scraper;

import java.util.HashMap;
import java.util.Map;

public class SimpleMultiSet<T> {

  protected HashMap<T, Integer> map;
  protected int max_count = 0;
  protected T max_thing = null;

  public SimpleMultiSet() {
    map = new HashMap<T, Integer>();
  }

  public void add(T thing) {
    int thing_count = 0;
    if (map.containsKey(thing)) {
      thing_count = map.get(thing);
    }
    thing_count++;
    map.put(thing, thing_count);
    if (thing_count > max_count) {
      max_count = thing_count;
      max_thing = thing;
    }
  }

  public int get_count(T thing) {
    if (map.containsKey(thing))
      return map.get(thing);
    else
      return 0;
  }

  public int get_max_count() {
    return max_count;
  }

  public T get_max() {
    return max_thing;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (Map.Entry<T, Integer> entry : map.entrySet()) {
      buf.append("[" + entry.getKey() + "]->" + entry.getValue() + "\n");
    }
    return buf.toString();
  }
}
