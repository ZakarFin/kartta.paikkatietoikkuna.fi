package fi.nls.oskari.spring;

import org.oskari.spring.controllers.MapController;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import org.oskari.spring.extension.OskariParam;
import fi.nls.oskari.util.PropertyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by SMAKINEN on 2.6.2017.
 */
@Controller
public class MapViewUrlHandler {

    private final static Logger LOG = LogFactory.getLogger(MapViewUrlHandler.class);

    @Autowired
    private MapController controller;

    @RequestMapping(value={"/view/{lang}/{mapId}", "/published/{lang}/{mapId}"})
    public String redirectToMapView(@PathVariable("lang") String lang,
                                    @PathVariable("mapId") String mapId,
                                    Model model,
                                    @OskariParam ActionParameters params) throws Exception {
        if(!isSupported(lang)) {
            lang = PropertyUtil.getDefaultLanguage();
        }
        String url = "/?lang=" + lang + "&";
        if(mapId.matches("\\d+")) {
            // digits -> use viewId
            url = url + "viewId=" + mapId;
        } else {
            // default to uuid
            url = url + "uuid=" + mapId;
        }
        return "redirect:" + attachQuery(url, params.getRequest().getQueryString());
    }

    private boolean isSupported(String lang) {
        for(String l: PropertyUtil.getSupportedLanguages()) {
            if(lang.equalsIgnoreCase(l)) {
                return true;
            }
        }
        return false;
    }

    private String attachQuery(String path, String query) {
        if(query == null) {
            return path;
        }
        if(path.indexOf('?') == -1) {
            return path + "?" + query;
        }
        if(path.endsWith("?")) {
            return path + query;
        }
        return path + "&" + query;

    }

}