package fi.nls.paikkatietoikkuna.coordtransform;

import fi.nls.oskari.annotation.OskariViewModifier;
import fi.nls.oskari.control.view.modifier.bundle.BundleHandler;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.view.modifier.ModifierException;
import fi.nls.oskari.view.modifier.ModifierParams;
import org.json.JSONObject;

/**
 * Injects an optional alternative url for frontend to use through bundle config

 {
 "url": "/action/KomuProj"
 }
 */
@OskariViewModifier("coordinatetransformation")
public class KomuBundleHandler extends BundleHandler {

    public boolean modifyBundle(final ModifierParams params) throws ModifierException {
        final JSONObject config = getBundleConfig(params.getConfig());
        JSONHelper.putValue(config, "url", getAltFrontendUrl());
        return false;
    }

    /**
     * Allows configuring an alternative frontend URL for the frontend to use when calling the server.
     * Either a path to a new route on this server (/action/KomuProj) or a whole URL to another service (komu backend)
     * @return
     */
    public static String getAltFrontendUrl() {
        return PropertyUtil.getOptional("pti.komu.frontendUrl");
    }
}
