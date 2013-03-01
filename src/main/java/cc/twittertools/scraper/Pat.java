package cc.twittertools.scraper;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Pat implements Comparable<Pat> {
  protected String prefix;
  protected String suffix;
  protected Pattern re;

  public Pat(String pre, String suf) { 
    prefix = pre;
    suffix = suf;
    this.compile();
  }

  public String toString() {
    return "/" + prefix + "(.*?)" + suffix + "/";
  }

  public void compile() {
    if (prefix != null && suffix != null)
      re = Pattern.compile(Pattern.quote(prefix) + "(.+?)" 
          + Pattern.quote(suffix));
  }

  public String extract(String src) {
    Matcher m = re.matcher(src);
    if (m.find())
      return m.group(1);
    else
      return null;
  }

  public int compareTo(Pat other) {
    int x = prefix.compareTo(other.prefix);
    if (x == 0)
      x = suffix.compareTo(other.suffix);
    return x;
  }

  public boolean equals(Object other) {
    return (other instanceof Pat) && this.compareTo((Pat)other) == 0;
  }

  public int hashCode() {
    return prefix.hashCode() * 31 + suffix.hashCode();
  }
}
