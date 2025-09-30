package fi.nls.paikkatietoikkuna.coordtransform;

import fi.nls.oskari.control.ActionCommonException;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.RestActionHandler;
import fi.nls.oskari.util.PropertyUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.Iterator;


/**
 * Proxies KomuProj action_route requests
 */
@OskariActionRoute("KomuProj")
public class KomuProjActionHandler extends RestActionHandler {

    private static final String PROP_END_POINT = "coordtransform.endpoint";
    private static final Logger LOG = LogFactory.getLogger(KomuProjActionHandler.class);


    private String endPoint;

    public KomuProjActionHandler() {
        this(null);
    }

    protected KomuProjActionHandler(String endPoint) {
        this.endPoint = endPoint;
    }

    @Override
    public void init() {
        if (endPoint == null) {
            endPoint = PropertyUtil.getNecessary(PROP_END_POINT);
        }
    }

    @Override
    public void handlePost(ActionParameters params) throws ActionException {
        try {
            HttpURLConnection conn = getConnection(params);

            IOHelper.copy(params.getRequest().getInputStream(), conn.getOutputStream());
            if (conn.getResponseCode() != 200) {
                if (LOG.isDebugEnabled()) {
                    // don't read if debug is not enabled
                    LOG.debug("Response was:", IOHelper.readString(conn.getInputStream()));
                }
                throw new ActionCommonException("Got non-OK response");
            }
            IOHelper.readBytesTo(conn, params.getResponse().getOutputStream());

        } catch (IOException e) {
            throw new ActionCommonException("Unable to proxy to proj", e);
        }
    }

    private HttpURLConnection getConnection(ActionParameters params) throws IOException {
        String queryParams = IOHelper.getParamsMultiValue(params.getRequest().getParameterMap());
        String url = IOHelper.addQueryString(endPoint, queryParams);
        HttpURLConnection conn = IOHelper.getConnection(url);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        IOHelper.setContentType(conn, "text/plain");

        return conn;
    }
}
