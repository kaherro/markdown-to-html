package md2html;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.io.IOException;

public class Md2Html {
    private static final Map<String, String> marks = Map.of(
            "++", "u",
            "`", "code",
            "--", "s",
            "*", "em",
            "_", "em",
            "**", "strong",
            "__", "strong"
    );
    private static final Map<Character, String> specials = Map.of(
            '<', "&lt;",
            '>', "&gt;",
            '&', "&amp;",
            '\\', ""
    );

    static int header(String s) {
        int i = 0;
        while (s.charAt(i) == '#') {
            i++;
        }
        if (i >= 1 && i <= 6 && Character.isWhitespace(s.charAt(i))) {
            return i;
        }
        return 0;
    }

    static boolean checkSlash(String s, int i) {
        return !(i > 0 && s.charAt(i - 1) == '\\');
    }

    static String get_tag(String s, int i) {
        StringBuilder tag = new StringBuilder();
        if (i + 1 < s.length() && checkSlash(s, i + 1)) {
            tag.append(s.charAt(i));
            tag.append(s.charAt(i + 1));
        }
        if (!marks.containsKey(tag.toString()) &&
                checkSlash(s, i)) {
            tag.setLength(0);
            tag.append(s.charAt(i));
        }
        return tag.toString();
    }

    private static String parse(String s) {
        StringBuilder x = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '[' && checkSlash(s, i)) {
                boolean flag = false;
                for (int j = i + 1; j + 1 < s.length(); j++) {
                    if (s.charAt(j) == ']' && s.charAt(j + 1) == '<' && checkSlash(s, j)) {
                        for (int z = j + 2; z < s.length(); z++) {
                            if (s.charAt(z) == '>' && checkSlash(s, z)) {
                                x.append("<a href='").append(s, j + 2, z).append("'>").append(parse(s.substring(i + 1, j))).append("</a>");
                                i = z + 1;
                                flag = true;
                            }
                            if (flag) break;
                        }
                    }
                    if (flag) break;
                }
            }
            if (i >= s.length()) break;

            String tag1 = get_tag(s, i);
            boolean tr = false;

            if (tag1.length() > 0 && marks.containsKey(tag1)) {
                int j;
                String tag2 = "";
                for (j = i + tag1.length(); j < s.length(); j++) {
                    tag2 = get_tag(s, j);
                    if (tag1.equals(tag2)) {
                        break;
                    } else {
                        if (marks.containsKey(tag2.toString())) {
                            j += tag2.length() - 1;
                        }
                        tag2 = "";
                    }
                }
                if (tag1.toString().equals(tag2.toString())) {
                    x.append("<" + marks.get(tag1.toString()) + ">");
                    x.append(parse(s.substring(i + tag1.length(), j)));
                    x.append("</" + marks.get(tag1.toString()) + ">");
                    i = j + tag2.length() - 1;
                    tr = true;
                }
            }
            if (!tr) {
                if (specials.containsKey(s.charAt(i))) {
                    x.append(specials.get(s.charAt(i)));
                } else x.append(s.charAt(i));
            }
        }
        return x.toString();
    }

    public static void main(String[] args) {
        List<String> x = new ArrayList<>();
        try {
            FileInputStream fileInputStream = new FileInputStream(args[0]);
            Reader reader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            Scanner in = new Scanner(reader);
            try {
                while (in.hasNext()) {
                    x.add(in.nextLine());
                }
            } finally {
                in.close();
            }
        }
        
        catch (FileNotFoundException e) {
            System.err.println("Input file not found: " + e.getMessage());
            return;
        }

        StringBuilder ans = new StringBuilder();

        String previous = "";
        int header_depth = -1;
        for (int i = 0; i < x.size(); i++) {
            StringBuilder cur = new StringBuilder();
            boolean tr = false;
            while (i < x.size() && x.get(i).length() > 0) {
                if (cur.length() > 0) cur.append("\n");
                cur.append(x.get(i));
                i++;
                tr = true;
            }
            if (tr) i--;
            if (previous.length() == 0 && cur.length() > 0) {
                header_depth = header(cur.toString());
                if (header_depth == 0) {
                    ans.append("<p>");
                    ans.append(parse(cur.toString()));
                } else {
                    ans.append("<h" + header_depth + ">");
                    ans.append(parse(cur.substring(header_depth + 1, cur.length())));
                }
            } else if (cur.length() == 0 && header_depth >= 0) {
                if (header_depth == 0) {
                    ans.append("</p>\n");
                } else {
                    ans.append("</h" + header_depth + ">\n");
                }
                header_depth = -1;
            } else if (header_depth >= 0 && cur.length() > 0) {
                ans.append("\n" + parse(cur.toString()));
            } else if (header_depth < 0 && cur.length() > 0) {
                ans.append(parse(cur.toString())).append("\n");
            }
            previous = cur.toString();
        }
        if (header_depth == 0) {
            ans.append("</p>\n");
        } else {
            ans.append("</h" + header_depth + ">\n");
        }
        try {
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(args[1]),
                    StandardCharsets.UTF_8);
            try {
                out.write(ans.toString());
            } catch (IOException e) {
                System.err.println("Writing file error: " + e.getMessage());
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    System.err.println("Output file closing error: " + e.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Output file not found: " + e.getMessage());
        }
    }

}