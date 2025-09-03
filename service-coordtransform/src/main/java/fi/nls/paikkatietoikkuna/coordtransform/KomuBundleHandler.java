package fi.nls.paikkatietoikkuna.coordtransform;

import fi.nls.oskari.annotation.OskariViewModifier;
import fi.nls.oskari.control.view.modifier.bundle.BundleHandler;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.view.modifier.ModifierException;
import fi.nls.oskari.view.modifier.ModifierParams;
import org.json.JSONObject;

/**
 * Injects possible url for frontend in komu config

 {
 "url": "/action/KomuProj"
 }
 */
@OskariViewModifier("coordinatetransformation")
public class KomuBundleHandler extends BundleHandler {

    public boolean modifyBundle(final ModifierParams params) throws ModifierException {
        final JSONObject config = getBundleConfig(params.getConfig());
        JSONHelper.putValue(config, "url", isFrontendUrl());
        return false;
    }

    /**
     * If users are managed in external source any changes to them are usually overwritten when they login.
     * So we can disable the fields that are and updates that can happen to users to make the admin UI more user-friendly.
     * @return
     */
    public static String isFrontendUrl() {
        return PropertyUtil.getOptional("pti.komu.frontendUrl");
    }
}
