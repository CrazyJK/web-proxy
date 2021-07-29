import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternTest {

	protected static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{(.+?)\\}");

	public static void main(String[] args) {

		LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
		params.put("_subHost", "namenode");
		params.put("_port", "8080");
		params.put("_path", "monitor");

		StringBuffer urlBuf = new StringBuffer();
		Matcher matcher = TEMPLATE_PATTERN.matcher("http://{_subHost}.behindfirewall.mycompany.com:{_port}/{_path}?KEY=aasdasd");
		while (matcher.find()) {
			System.out.println();
			System.out.println("found");
			String arg = matcher.group(1);
			System.out.println("arg=" + arg);
			String replacement = params.remove(arg);// note we remove
			System.out.println("replacement=" + replacement);
			if (replacement == null) {
				System.out.println("Missing HTTP parameter " + arg + " to fill the template");
			}
			matcher.appendReplacement(urlBuf, replacement);
			System.out.println("urlBuf=" + urlBuf);
		}
		matcher.appendTail(urlBuf);
		String newTargetUri = urlBuf.toString();

		System.out.println(newTargetUri);
	}

}
