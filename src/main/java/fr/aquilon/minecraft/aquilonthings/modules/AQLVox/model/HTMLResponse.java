package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import com.google.common.base.Strings;
import fi.iki.elonen.NanoHTTPD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An helper to construct an HTML response
 * @author Billi
 */
public class HTMLResponse {
    private NanoHTTPD.Response.Status code;
    private String title;
    private List<String> headers;
    private StringBuilder body;
    private final Template template;

    public HTMLResponse(String title) {
        this(null, title);
    }

    public HTMLResponse(Template template, String title) {
        this.code = NanoHTTPD.Response.Status.OK;
        this.template = template;
        this.title = title;
        this.headers = new ArrayList<>();
        this.body = new StringBuilder();
    }

    public Template getTemplate() {
        return template;
    }

    public String getHead() {
        return "\t<head>\n\t\t<meta charset=\"utf-8\"/>\n\t\t" +
                "<title>"+getTitle()+(template!=null?template.titleSuffix:"")+"</title>\n\t\t"+
                "<!-- Static headers -->\n\t\t" +
                (template!=null && template.headers!=null ? String.join("\n\t\t", template.headers) : "") +
                "<!-- Custom headers -->\n\t\t" +
                String.join("\n\t\t", headers) +
                "\n\t</head>\n";
    }

    public NanoHTTPD.Response.Status getCode() {
        return code;
    }

    public HTMLResponse setCode(NanoHTTPD.Response.Status code) {
        this.code = code;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void addHeader(String header) {
        this.headers.add(header);
    }

    public void addAllHeaders(Collection<String> headers) {
        this.headers.addAll(headers);
    }

    public String getBody() {
        return body.toString();
    }

    public void setBody(String body) {
        this.body = new StringBuilder(body);
    }

    public HTMLResponse append(String html) {
        return append(0, html);
    }

    public HTMLResponse append(int indentLevel, String html) {
        if (indentLevel>0) body.append(Strings.repeat("\t", indentLevel));
        body.append(html);
        return this;
    }

    public String getFullBody() {
        return "\t<body>\n\t"+
                (template!=null ? template.bodyPrefix : "") +
                getBody() +
                (template!=null ? template.bodySuffix : "") +
                "</body>\n";
    }

    public String getHTML() {
        return "<!DOCTYPE html>\n<html lang=\"fr\">\n" +
                getHead() +
                getFullBody() +
                "</html>";
    }

    public static class Template {
        public static final Template ABOUT = new Template(
                " • AQLVox - Aquilon",
                "<style>body { font-family: Calibri, sans-serif; }</style>\n",
                "", null);

        public final String titleSuffix;
        public final String bodyPrefix;
        public final String bodySuffix;
        public final String[] headers;

        public Template(String titleSuffix, String bodyPrefix, String bodySuffix, String[] headers) {
            this.titleSuffix = titleSuffix;
            this.bodyPrefix = bodyPrefix;
            this.bodySuffix = bodySuffix;
            this.headers = headers;
        }
    }
}
