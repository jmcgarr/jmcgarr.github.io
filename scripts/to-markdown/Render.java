// Renders Markdown from stdin to HTML on stdout exactly as JBake 2.7's MarkdownEngine does
// (PegdownOptionsAdapter + the site's markdown.extensions). A probe for convert.py; not needed to run it.
// Usage: tools/render.sh < file.md
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.PegdownExtensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.data.DataHolder;

public class Render {
    public static void main(String[] a) throws Exception {
        String exts = a.length > 0 ? a[0]
                : "AUTOLINKS,FENCED_CODE_BLOCKS,DEFINITIONS,TABLES,STRIKETHROUGH,TASKLISTITEMS,FOOTNOTES";
        int e = PegdownExtensions.NONE;
        for (String x : exts.split(",")) e |= PegdownExtensions.class.getDeclaredField(x.trim()).getInt(null);
        DataHolder o = PegdownOptionsAdapter.flexmarkOptions(e);
        String in = new String(System.in.readAllBytes(), "UTF-8");
        System.out.print(HtmlRenderer.builder(o).build().render(Parser.builder(o).build().parse(in)));
    }
}
