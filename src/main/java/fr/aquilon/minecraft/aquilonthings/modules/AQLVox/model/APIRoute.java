package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.RouteRegistrationException;
import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class APIRoute implements JSONExportable {
    public static final String METHOD_WEBSOCKET = "WEBSOCKET";
    public static final String PERM_PREFIX_ROUTE = "nav.";
    public static final APIRoute ROOT_ROUTE = new APIRoute("root", "GET", null, "/");

    private String name;
    private String method;
    private String module;
    private String uri;
    private Pattern regexURI;
    private String[] argNames;
    private Map<String, RouteArg> args;

    public APIRoute(String templateName, String method, String module, String uri) throws RouteRegistrationException {
        this.name = Objects.requireNonNull(templateName);
        this.method = Objects.requireNonNull(method).toUpperCase();
        this.module = module;
        String validatedUri = Objects.requireNonNull(uri);
        if (validatedUri.length()>1 && validatedUri.endsWith("/"))
            validatedUri = validatedUri.substring(0, validatedUri.length()-1);
        this.uri = validatedUri;
        try {
            this.regexURI = Pattern.compile(ArgumentType.translateArgs(getURI()), Pattern.UNICODE_CASE);
        } catch (PatternSyntaxException ex) {
            throw new RouteRegistrationException(module, templateName, "Invalid argument syntax", ex);
        }
        if (getPermName().contains("{"))
            throw new RouteRegistrationException(module, templateName, "Invalid argument syntax");
        parseArgs();
    }

    private void parseArgs() {
        String[] urlParts = getURI().substring(1).split("/");
        String argRegex = "^"+ArgumentType.getArgumentsRegex()+"$";
        Map<String, RouteArg> argList = new HashMap<>();
        List<String> argNameList = new ArrayList<>();
        int argIndex = 0;
        for (int i=0; i<urlParts.length; i++) {
            String urlPart = urlParts[i];
            if (!urlPart.matches(argRegex)) continue;
            RouteArg arg = RouteArg.fromTemplate(urlPart, argIndex, i);
            if (arg==null) continue;
            argIndex++;
            argNameList.add(arg.getName());
            argList.put(arg.getName(), arg);
        }
        argNames = argNameList.toArray(new String[0]);
        args = argList;
    }

    // --- Public methods ---

    public String getName() {
        return name;
    }

    public String getMethod() {
        return method;
    }

    public String getModule() {
        return module;
    }

    public String getURI() {
        return uri;
    }

    public boolean isRoot() {
        return module==null && uri.equals("/");
    }

    public String getPermName() {
        if (isRoot()) return PERM_PREFIX_ROUTE + ".root.get";
        return PERM_PREFIX_ROUTE + getModule() +
                getURI().replace('/','.')
                        .replaceAll(ArgumentType.getArgumentsRegex(), "#") +
                (getURI().equals("/") ? "root." : ".") +
                getMethod().toLowerCase();
    }

    public JSONObject toJSON() {
        JSONObject route = new JSONObject();
        route.put("method", method);
        route.put("module", module);
        route.put("uri", uri);
        route.put("perm", getPermName());
        return route;
    }

    public Pattern getRegexURI() {
        return regexURI;
    }

    public int getArgCount() {
        return args.size();
    }

    public RouteArg getArg(String name) {
        return args.get(name);
    }

    public RouteArg[] getArgs() {
        List<RouteArg> values = new ArrayList<>(args.values());
        values.sort(Comparator.comparing(RouteArg::getIndex));
        return values.toArray(new RouteArg[0]);
    }

    @Override
    public String toString() {
        return method+": "+ getFullURI();
    }

    public String getFullURI() {
        if (isRoot()) return "/";
        return '/'+module+uri;
    }

    public String getSwaggerURI() {
        String uri = getFullURI();
        return uri.replaceAll(ArgumentType.getArgumentsRegex(), "{$2}");
    }

    /**
     * Get an argument template by ID.
     * @param index The id of the argument in the uri, starts at 1.
     * @return The argument template or null
     */
    public RouteArg getArg(int index) {
        return args.get(argNames[index-1]);
    }

    /**
     * Get an argument template by position.
     * @param position The position of the argument in the uri, starts at 1.
     * @return The argument template or null
     */
    public RouteArg getArgFromPosition(int position) {
        for (RouteArg arg : args.values()) {
            if (arg.position!=position) continue;
            return arg;
        }
        return null;
    }

    public boolean matches(String method, String module, String subUri) {
        if (!method.equals(this.getMethod())) return false;
        if (!module.equals(this.getModule())) return false;
        return getRegexURI().matcher(subUri).matches();
    }

    public static String getString(NanoHTTPD.IHTTPSession req) {
        String method = req.getMethod().name();
        String uri = req.getUri();
        return method+": "+uri;
    }

    public enum ArgumentType {
        STRING("string", "string", "([\\p{L}0-9 \\-_\\(\\)\\[\\]']+)"),
        INT("int", "integer", "(\\d+)"),
        LONG("long", "integer", "(\\d+)"),
        UUID("uuid", "string", "([0-9a-f]{32})");
        private final String type;
        private final String schemaType;
        private final String valueRegex;

        public static final String REGEX_ARG_TEMPLATE = "\\{(<TYPE>):([\\w-]+)\\}";

        ArgumentType(String type, String schemaType, String valueRegex) {
            this.type=Objects.requireNonNull(type);
            this.schemaType=Objects.requireNonNull(schemaType);
            this.valueRegex=Objects.requireNonNull(valueRegex);
        }

        // --- Accessors ---

        public String getType() {
            return type;
        }

        public String getSchemaType() {
            return schemaType;
        }

        public String getValueRegex() {
            return valueRegex;
        }

        // --- Methods ---

        /**
         * @return A JSONObject representing an open api schema
         */
        public JSONObject getSchema() {
            JSONObject res = new JSONObject();
            res.put("type", getSchemaType());
            switch (this) {
                case STRING:
                    res.put("regex", STRING.valueRegex);
                    break;
                case INT:
                    res.put("format", "int32");
                    break;
                case LONG:
                    res.put("format", "int64");
                    break;
                case UUID:
                    res.put("regex", UUID.valueRegex);
                    break;
            }
            return res;
        }

        public String getArgRegex() {
            return REGEX_ARG_TEMPLATE.replace("<TYPE>", getType());
        }

        public String getEscapedValueRegex() {
            return getValueRegex().replaceAll("\\\\", "\\\\\\\\");
        }

        // --- Static helpers ---

        public static ArgumentType fromType(String typeIn) {
            for (ArgumentType t : values()) {
                if (t.getType().equals(typeIn)) return t;
            }
            return null;
        }

        public static String getArgumentsRegex() {
            return REGEX_ARG_TEMPLATE.replace("<TYPE>", "\\w+");
        }

        public static String translateArgs(String uri) {
            String res = "^"+uri;
            for (ArgumentType t : values()) {
                res = res.replaceAll(t.getArgRegex(), t.getEscapedValueRegex());
            }
            res+='$';
            return res;
        }
    }

    public static class RouteArg {
        private int index;
        private int position;
        private ArgumentType type;
        private String name;

        public RouteArg(int argIndex, int argPos, ArgumentType type, String name) {
            this.index = argIndex;
            this.position = argPos;
            this.type = type;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public ArgumentType getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return type.getType()+": "+getName();
        }

        public static RouteArg fromTemplate(String template, int index, int pos) {
            String[] parts = template.substring(1, template.length()-1).split(":");
            ArgumentType argType = ArgumentType.fromType(parts[0]);
            if (argType==null) return null;
            return new RouteArg(index, pos, argType, parts[1]);
        }
    }
}
