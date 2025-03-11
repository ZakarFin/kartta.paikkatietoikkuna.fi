package fi.nls.paikkis.control;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@OskariActionRoute("GetArticlesByTag")
public class GetArticlesByTagHandler extends ActionHandler {

    private Logger log = LogFactory.getLogger(GetArticlesByTagHandler.class);

    private static final String KEY_TAGS = "tags";
    private static final String KEY_ID = "id";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_ARTICLES = "articles";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DUMMY = "dummy";
    private static final String KEY_BODY = "body";
    private static final String KEY_URL = "url";
    private static final String KEY_ELEMENT = "element";
    private static final String HTML_TAG_IMG = "img";
    private static final String HTML_TAG_A = "a";
    private static final String HTML_PARAM_SRC = "src";
    private static final String HTML_PARAM_HREF = "href";
    private static final String HTML_PARAM_TARGET = "target";

    private String fileLocation = null;
    private String articlesByTagSetupFile = null;
    private int timeout = 6000;
    private int cacheTimeout = JedisManager.EXPIRY_TIME_DAY;

    private Map<String, String> errorMsg = new HashMap<>();

    @Override
    public void init() {
        super.init();
        fileLocation = PropertyUtil.get("actionhandler.GetArticlesByTag.dir", "/fi/nls/oskari/control");
        articlesByTagSetupFile = PropertyUtil.get("actionhandler.GetArticlesByTag.setupFile", "/articles-by-tag-setup-file.json");
        timeout = PropertyUtil.getOptional("actionhandler.GetArticlesByTag.timeout", 6000);
        cacheTimeout = PropertyUtil.getOptional("actionhandler.GetArticlesByTag.cache.timeout", JedisManager.EXPIRY_TIME_DAY);

        if(!fileLocation.endsWith(File.separator)) {
            fileLocation = fileLocation + File.separator;
        }
        errorMsg.put("fi", "Virhe: Käyttöohjetta ei voitu ladata.");
        errorMsg.put("sv", "Bruksanvisningarna kunde inte laddas.");
        errorMsg.put("en", "Failed to load user guide.");
    }

    @Override
    public void handleAction(ActionParameters params) throws ActionException {
        final String lang = params.getLocale().getLanguage();
        final String tags = params.getHttpParam(KEY_TAGS, "");

        log.debug("Getting articles for language:", lang, "with tags:", tags);

        // First try to find from cache
        final String cacheKey = "getarticlebytag:" + tags + "_" + lang;
        JSONArray articles = getFromCache(cacheKey);

        if (articles == null || articles.length() == 0) {
            // If nothing in cache try scraping as specified in setup file
            articles = articlesFromSetupFile(lang + "_" + tags);
            if (articles != null && articles.length() > 0) {
                // Only cache successfully scraped stuff
                JedisManager.setex(cacheKey, cacheTimeout, articles.toString());
            } else {
                // If scraping fails try static resources
                JSONObject articleContent = getContent(tags);
                if (articleContent == null) {
                    articleContent = tryContentWithLessTags(tags, tags);
                }
                if (articleContent != null && !articleContent.has(KEY_DUMMY)){
                    JSONObject articleJson = new JSONObject();
                    JSONHelper.putValue(articleJson, KEY_ID, "none");
                    JSONHelper.putValue(articleJson, KEY_CONTENT, articleContent);
                    articles = new JSONArray();
                    articles.put(articleJson);
                }
            }
        }

        if (articles == null || articles.length() == 0) {
            // If everything fails create dummy content
            JSONObject articleContent = getMissingContentNote(tags);
            articles = new JSONArray();
            JSONObject articleJson = new JSONObject();
            JSONHelper.putValue(articleJson, KEY_ID, "none");
            JSONHelper.putValue(articleJson, KEY_CONTENT, articleContent);
            articles.put(articleJson);
        }
        // Change any dummy article to a localized error message:
        for (int i = 0; i < articles.length(); i++) {
            JSONObject art = articles.optJSONObject(i);
            if (art == null) {
                continue;
            }
            JSONObject content = art.optJSONObject(KEY_CONTENT);
            if (content == null || !content.optBoolean(KEY_DUMMY)) {
                continue;
            }
            String debugInfo = content.optString(KEY_BODY);
            JSONHelper.putValue(content, KEY_BODY, errorMsg.getOrDefault(params.getLocale().getLanguage(), ""));
            JSONHelper.putValue(content, "debugInfo", debugInfo);
        }

        final JSONObject response = new JSONObject();
        JSONHelper.putValue(response, KEY_ARTICLES, articles);
        ResponseHelper.writeResponse(params, response);
    }

    private JSONArray getFromCache(String cacheKey) {
        String articleFromCache = JedisManager.get(cacheKey, false);
        if (articleFromCache != null) {
            try {
                return new JSONArray(articleFromCache);
            } catch (JSONException e) {
                // This really shouldn't happen, since we only cache
                // valid stuff, but better safe than sorry
                log.warn(e, "Failed to create JSONArray from cached article");
            }
        }
        return null;
    }

    private JSONArray articlesFromSetupFile(final String commaSeparatedTags) {
        JSONArray articles = new JSONArray();
        try {
            String setupJSON = IOHelper.readString(GetArticlesByTagHandler.class.getResourceAsStream(articlesByTagSetupFile));
            if (setupJSON == null || setupJSON.isEmpty()) {
                log.warn("Not found article setup file: " + articlesByTagSetupFile);
                return null;
            }
            final JSONObject setup = JSONHelper.createJSONObject(setupJSON);
            if (setup.has(KEY_ARTICLES)) {
                JSONArray articlesArray = setup.getJSONArray(KEY_ARTICLES);
                JSONObject articleContent = JSONHelper.createJSONObject("static", "[no cms, dummy content]");
                for (int i = 0; i < articlesArray.length(); i++) {
                    JSONObject article = articlesArray.getJSONObject(i);
                    if (article.get(KEY_TAGS).equals(commaSeparatedTags)) {
                        log.debug("Reading article from web page: " + article.getString(KEY_URL));

                        Document doc = Jsoup.connect(article.getString(KEY_URL)).timeout(timeout).get();
                        Element divcontent = doc.select(article.getString(KEY_ELEMENT)).first();

                        // convert img urls
                        for (Element e : divcontent.getAllElements()) {
                            if (HTML_TAG_IMG.equalsIgnoreCase(e.tagName())) {
                                String src = e.attr(HTML_PARAM_SRC);
                                if (src != null && src.startsWith("//")) {
                                    e.attr(HTML_PARAM_SRC, "https:" + src);
                                }
                            } else if (HTML_TAG_A.equalsIgnoreCase(e.tagName())) {
                                if (e.hasAttr(HTML_PARAM_HREF)) {
                                    if (!e.attr(HTML_PARAM_HREF).startsWith("#")) {
                                        // open links to external documents in a new window/tab
                                        // use named target ("info") instead of "_blank" to always use
                                        // the same window/tab even if the link is clicked multiple times
                                        e.attr(HTML_PARAM_TARGET, "info");
                                    }
                                }
                            }
                        }

                        JSONHelper.putValue(articleContent, KEY_BODY, divcontent.html());
                        JSONHelper.putValue(article, KEY_ID, "none");
                        JSONHelper.putValue(article, KEY_CONTENT, articleContent);
                        articles.put(article);
                    }
                }

                log.debug("Founded ", articles.length(), " articles from article setup file with tags:", commaSeparatedTags);
                if(articles.length() > 0) {
                    return articles;
                }
            }
        } catch (IOException e) {
            log.error("Not found article: ", e);
        } catch (JSONException e) {
            log.error("Cannot parse article setup file: " + articlesByTagSetupFile, e);
        }
        return null;
    }

    public JSONObject tryContentWithLessTags(String originalTags, String commaSeparatedTags) {
        if(commaSeparatedTags == null) {
            return getMissingContentNote(originalTags);
        }
        String[] tags = commaSeparatedTags.split(",");
        if(tags.length == 1) {
            return getMissingContentNote(originalTags);
        }
        // remove the last tag
        final String newTags = StringUtils.join(Arrays.copyOf(tags, tags.length-1), ",");
        JSONObject articleContent = getContent(newTags);
        while (articleContent == null) {
            articleContent = tryContentWithLessTags(originalTags, newTags);
        }
        return articleContent;
    }

    // create dummy content since content files are not provided for these tags
    private JSONObject getMissingContentNote(String tags) {
        JSONObject articleContent = JSONHelper.createJSONObject("static", "[no cms, dummy content]");
        JSONHelper.putValue(articleContent, KEY_TITLE, "[title]");
        JSONHelper.putValue(articleContent, KEY_DUMMY, true);
        JSONHelper.putValue(articleContent, KEY_BODY, "[body from GetArticlesByTag action route with tags: '" + tags + "']");
        return articleContent;
    }

    protected JSONObject getContent(final String commaSeparatedTags) {
        final String fileName = commaSeparatedTags
                .replace(',', '_')
                .replace(' ', '_')
                .replace('/', '_')
                .replace('.', '_')
                .replace('\\', '_');
        final String htmlContent = readInputFile(fileName + ".html");
        if(htmlContent != null) {
            log.debug("Found HTML-file with tags", commaSeparatedTags);
            return JSONHelper.createJSONObject("body", htmlContent);
        }
        final String jsonContent = readInputFile(fileName + ".json");
        if(jsonContent != null) {
            log.debug("Found JSON-file with tags", commaSeparatedTags);
            return JSONHelper.createJSONObject(jsonContent);
        }
        log.debug("Didn't find content for tags:", commaSeparatedTags);
        return null;
    }

    protected String readInputFile(final String filename) {
        InputStream in = getClass().getResourceAsStream(fileLocation + filename);
        if(in != null) {
            try {
                return IOHelper.readString(in);
            } catch (Exception ignore) {
                log.info("Unable to read file from classpath:", fileLocation + filename);
            }
            finally {
                IOHelper.close(in);
            }
        }
        return null;
    }
}